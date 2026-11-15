#!/usr/bin/env python3
"""Offline TTS -> Pleos virtual microphone -> app events. Never use the real mic."""
import argparse
import json
import os
import re
import subprocess
import sys
import time
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SERVICE = '/android.emulation.control.EmulatorController/'
ACTIONS = {'GET_AREA_PRICE_SUMMARY', 'GET_REAL_ESTATE_BRIEFING', 'SCRAP_APARTMENT'}
EVENT = re.compile(r'event=(received|callback_sent) action=([A-Z_]+) '
                   r'(?:(?:status=(success|asking|fail|unknown) persisted=(true|false)) )?$')


def command(args, timeout=12):
    return subprocess.run(args, capture_output=True, text=True, check=True, timeout=timeout).stdout


def validate_endpoint(endpoint):
    match = re.fullmatch(r'127\.0\.0\.1:(\d{1,5})', endpoint)
    if not match or not 1024 <= int(match[1]) <= 65535:
        raise ValueError('Expected a loopback endpoint with an unprivileged port')


def validate_session(path, serial, endpoint):
    """Only the read-only instance owned by run-pleos.py may receive speech."""
    validate_endpoint(endpoint)
    session = json.loads(path.read_text())
    if (session.get('serial') != serial or session.get('endpoint') != endpoint
            or session.get('read_only') is not True or session.get('active') is not True
            or not session.get('avd', '').startswith('Pleos_Connect_')):
        raise ValueError('An active, matching read-only Pleos session is required')
    pid = session.get('pid')
    if type(pid) is not int or pid <= 1:
        raise ValueError('Invalid session process')
    # Inspect socket ownership, never process environments, tokens or emulator key files.
    owners = command(['/usr/sbin/lsof', '-t', '-nP', '-iTCP:' + endpoint.split(':')[1], '-sTCP:LISTEN']).split()
    if set(owners) != {str(pid)}:
        raise ValueError('RPC listener does not belong to the test session')


def validate_protocol(path):
    """Fail closed if the installed SDK no longer matches this small wire subset."""
    source = re.sub(r'//[^\n]*', '', path.read_text())
    for message, fields in {
        'AudioFormat': [('uint64', 'samplingRate', 1), ('Channels', 'channels', 2),
                        ('SampleFormat', 'format', 3), ('DeliveryMode', 'mode', 4)],
        'AudioPacket': [('AudioFormat', 'format', 1), ('uint64', 'timestamp', 2), ('bytes', 'audio', 3)],
        'MicrophoneState': [('bool', 'realAudioEnabled', 1)],
    }.items():
        start = re.search(r'\bmessage\s+' + message + r'\s*\{', source)
        if not start:
            raise ValueError('SDK audio protocol is incompatible')
        depth = 1
        end = start.end()
        while end < len(source) and depth:
            depth += (source[end] == '{') - (source[end] == '}')
            end += 1
        body = source[start.end():end-1]
        for kind, name, number in fields:
            if not re.search(r'\b' + kind + r'\s+' + name + r'\s*=\s*' + str(number) + r'\s*;', body):
                raise ValueError('SDK audio field is incompatible')
        if message == 'AudioFormat':
            for name, number in [('Mono', 0), ('AUD_FMT_S16', 1), ('MODE_UNSPECIFIED', 0)]:
                if not re.search(r'\b' + name + r'\s*=\s*' + str(number) + r'\s*;', body):
                    raise ValueError('SDK audio enum is incompatible')


def messages():
    from google.protobuf import descriptor_pb2, descriptor_pool, message_factory
    f = descriptor_pb2.FileDescriptorProto(name='voice_probe.proto', package='android.emulation.control', syntax='proto3')
    def msg(name, fields):
        m = f.message_type.add(name=name)
        for name, number, kind, target in fields:
            x = m.field.add(name=name, number=number, type=kind)
            if target:
                x.type_name = '.android.emulation.control.' + target
    # int32 encodes these nonnegative SDK enum values identically; checked against SDK above.
    msg('AudioFormat', [('samplingRate', 1, 4, None), ('channels', 2, 5, None), ('format', 3, 5, None), ('mode', 4, 5, None)])
    msg('AudioPacket', [('format', 1, 11, 'AudioFormat'), ('timestamp', 2, 4, None), ('audio', 3, 12, None)])
    msg('MicrophoneState', [('realAudioEnabled', 1, 8, None)])
    pool = descriptor_pool.DescriptorPool()
    pool.Add(f)
    return tuple(message_factory.GetMessageClass(pool.FindMessageTypeByName('android.emulation.control.' + n))
                 for n in ['AudioFormat', 'AudioPacket', 'MicrophoneState'])


def read_pcm(path):
    with wave.open(str(path), 'rb') as wav:
        if (wav.getnchannels(), wav.getsampwidth(), wav.getframerate(), wav.getcomptype()) != (1, 2, 16000, 'NONE'):
            raise ValueError('Expected uncompressed mono PCM16 WAV at 16000 Hz')
        if not 1600 <= wav.getnframes() <= 20 * 16000:
            raise ValueError('WAV must be between 0.1 and 20 seconds')
        data = wav.readframes(wav.getnframes())
        if len(data) != wav.getnframes() * 2:
            raise ValueError('Truncated WAV')
        return data


def pcm_chunks(data):
    pcm = b'\0' * 16000 + data + b'\0' * 16000
    return (pcm[start:start + 640] for start in range(0, len(pcm), 640))


def inject(path, endpoint):
    import grpc
    from google.protobuf.empty_pb2 import Empty
    validate_endpoint(endpoint)
    F, P, M = messages()
    data = read_pcm(path)
    with grpc.insecure_channel(endpoint) as channel:
        grpc.channel_ready_future(channel).result(timeout=5)
        setmic = channel.unary_unary(SERVICE + 'setMicrophoneState', request_serializer=M.SerializeToString, response_deserializer=Empty.FromString)
        getmic = channel.unary_unary(SERVICE + 'getMicrophoneState', request_serializer=Empty.SerializeToString, response_deserializer=M.FromString)
        setmic(M(realAudioEnabled=False), timeout=5)
        if getmic(Empty(), timeout=5).realAudioEnabled:
            raise RuntimeError('Physical microphone access must stay disabled')
        def packets():
            for chunk in pcm_chunks(data):
                yield P(format=F(samplingRate=16000, channels=0, format=1, mode=0), audio=chunk)
                # MODE_UNSPECIFIED already blocks on the emulator's audio queue.
                # Client sleeps add a second clock and can starve the virtual mic
                # while the guest is busy rendering or synthesizing speech.
        try:
            channel.stream_unary(SERVICE + 'injectAudio', request_serializer=P.SerializeToString, response_deserializer=Empty.FromString)(
                packets(), timeout=len(data) / 32000 + 15)
        finally:
            setmic(M(realAudioEnabled=False), timeout=5)
    return {'bytes': len(data), 'seconds': len(data) / 32000, 'rpc': 'OK', 'host_microphone': False}


def load_cases(path):
    cases = json.loads(path.read_text())
    if not isinstance(cases, list) or not 1 <= len(cases) <= 30:
        raise ValueError('Expected 1..30 synthetic cases')
    ids = set()
    for case in cases:
        name = case.get('id', '')
        if not re.fullmatch(r'[a-z0-9-]+', name) or name in ids:
            raise ValueError('Invalid or duplicate case id')
        ids.add(name)
        if (case.get('action') not in ACTIONS or case.get('status') not in {'success', 'asking', 'fail'}
                or not isinstance(case.get('text'), str) or not 1 <= len(case['text']) <= 200):
            raise ValueError('Invalid speech expectation')
    return cases


def generate(cases, out):
    for case in cases:
        aiff, wav = (out / (case['id'] + ext) for ext in ['.aiff', '.wav'])
        command(['/usr/bin/say', '-v', 'Yuna', '-o', str(aiff), case['text']], 60)
        command(['/usr/bin/afconvert', '-f', 'WAVE', '-d', 'LEI16@16000', '-c', '1', str(aiff), str(wav)])
        read_pcm(wav)
    return len(cases)


def parse_events(lines):
    events = []
    for line in lines:
        match = EVENT.search(line)
        if match:
            events.append({'event': match[1], 'action': match[2],
                           **({'status': match[3], 'persisted': match[4] == 'true'} if match[3] else {})})
    return events


def classify(before, after, case):
    # Rollover/clearing invalidates the observation window. Never re-use stale log entries.
    if after[:len(before)] != before:
        return 'OBSERVATION_LOST', []
    events = parse_events(after[len(before):])
    if not events:
        return 'INCONCLUSIVE', events
    expected = [{'event': 'received', 'action': case['action']},
                {'event': 'callback_sent', 'action': case['action'], 'status': case['status'],
                 'persisted': case.get('persisted', False)}]
    return ('ACTION_CALLBACK_MATCHED' if events == expected else 'ACTION_CALLBACK_MISMATCH'), events


def error_info(exc):
    result = {'error_type': type(exc).__name__}
    try:
        import grpc
        if isinstance(exc, grpc.RpcError):
            result['rpc_code'] = exc.code().name
    except ImportError:
        pass
    return result


def run_cases(cases, args):
    adb = [args.adb, '-s', args.serial]
    def events():
        return command(adb + ['logcat', '-d', '-v', 'brief', '-s', 'GleoE2E:I', '*:S'], 6).splitlines()
    results = []
    try:
        command(adb + ['emu', 'avd', 'hostmicoff'], 5)
        for case in cases:
            result = {'id': case['id'], 'text': case['text'], 'expected_action': case['action'],
                      'expected_status': case['status'], 'stt_transcript_verified': False, 'status': 'INCONCLUSIVE'}
            try:
                command(adb + ['shell', 'am', 'start', '-n', 'com.potatoes.book5.realestate/.MainActivity'])
                before = events()
                command(adb + ['shell', 'cmd', 'voiceinteraction', 'show'], 8)
                time.sleep(1)
                dump = command(adb + ['shell', 'dumpsys', 'voiceinteraction'], 6)
                result['assistant_binding_null'] = 'mBound=true mService=null' in dump
                # Gleo 4.2.0.15 can recognize speech and dispatch app actions while
                # the standard VoiceInteractionService binder is null. Record
                # that observation without turning it into a false input gate.
                result['injection'] = inject(args.out / (case['id'] + '.wav'), args.endpoint)
                time.sleep(7)
                result['status'], result['events'] = classify(before, events(), case)
            except Exception as exc:
                result.update(status='BLOCKED', **error_info(exc))
            finally:
                try:
                    command(adb + ['shell', 'cmd', 'voiceinteraction', 'hide'], 5)
                except Exception:
                    pass
            results.append(result)
            (args.out / 'results.json').write_text(json.dumps(results, ensure_ascii=False, indent=2) + '\n')
            print(case['id'], result['status'], flush=True)
            time.sleep(2)
    finally:
        try:
            command(adb + ['emu', 'avd', 'hostmicoff'], 5)
        except Exception:
            pass
    return 0 if all(r['status'] == 'ACTION_CALLBACK_MATCHED' for r in results) else 2


def main():
    sdk = Path(os.environ.get('ANDROID_SDK_ROOT', str(Path.home() / 'Library/Android/sdk')))
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('mode', choices=['generate', 'run', 'inject'])
    ap.add_argument('--cases', type=Path, default=Path(__file__).with_name('cases.json'))
    ap.add_argument('--out', type=Path, required=True)
    ap.add_argument('--serial', default=os.environ.get('ANDROID_SERIAL', 'emulator-5556'))
    ap.add_argument('--endpoint', default='127.0.0.1:8554')
    ap.add_argument('--session', type=Path)
    ap.add_argument('--proto', type=Path, default=sdk / 'emulator/lib/emulator_controller.proto')
    ap.add_argument('--adb', default=str(sdk / 'platform-tools/adb'))
    ap.add_argument('--wav', type=Path)
    args = ap.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    if args.mode == 'run':
        (args.out / 'results.json').write_text('[]\n')
    cases = load_cases(args.cases)
    if args.mode == 'generate':
        print(json.dumps({'generated': generate(cases, args.out)}))
        return 0
    if args.session is None or (args.mode == 'inject' and args.wav is None):
        raise ValueError('Injection requires --session; inject also requires --wav')
    validate_session(args.session, args.serial, args.endpoint)
    validate_protocol(args.proto)
    env = dict(os.environ, ANDROID_SERIAL=args.serial)
    subprocess.run([sys.executable, str(ROOT / 'harness/check-pleos-device.py')], env=env, check=True, timeout=25)
    if args.mode == 'inject':
        print(json.dumps(inject(args.wav, args.endpoint)))
        return 0
    return run_cases(cases, args)


if __name__ == '__main__':
    try:
        sys.exit(main())
    except Exception as exc:
        # No raw grpc/vendor error details or unrelated log contents in reports.
        print(json.dumps({'status': 'BLOCKED', **error_info(exc)}), file=sys.stderr)
        sys.exit(2)
