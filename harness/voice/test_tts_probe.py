"""Host-side harness tests. No Android device, real audio or network service required."""
import json
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import wave

import grpc
from google.protobuf.empty_pb2 import Empty
import tts_probe as probe


class ProbeTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.wav = self.root / 'synthetic.wav'
        self.data = b'\x01\x02' * 1600
        self.write_wav()
        self.case = {'action': 'SCRAP_APARTMENT', 'status': 'asking'}
        self.received = 'I/GleoE2E(100): event=received action=SCRAP_APARTMENT '
        self.callback = 'I/GleoE2E(100): event=callback_sent action=SCRAP_APARTMENT status=asking persisted=false '

    def write_wav(self, channels=1, rate=16000, data=None):
        with wave.open(str(self.wav), 'wb') as wav:
            wav.setnchannels(channels)
            wav.setsampwidth(2)
            wav.setframerate(rate)
            wav.writeframes(self.data if data is None else data)

    def test_pcm_and_padding_are_exact_and_bounded(self):
        self.assertEqual(self.data, probe.read_pcm(self.wav))
        chunks = list(probe.pcm_chunks(self.data))
        self.assertTrue(all(0 < len(c) <= 640 and len(c) % 2 == 0 for c in chunks))
        self.assertEqual(b'\0' * 16000 + self.data + b'\0' * 16000, b''.join(chunks))

    def test_rejects_stereo_wrong_rate_short_and_long_files(self):
        for kw in [dict(channels=2), dict(rate=44100), dict(data=b'00'), dict(data=b'00'*320001)]:
            with self.subTest(kw=list(kw)):
                self.write_wav(**kw)
                with self.assertRaises(ValueError):
                    probe.read_pcm(self.wav)

    def test_rejects_truncated_wav(self):
        self.wav.write_bytes(self.wav.read_bytes()[:-2])
        with self.assertRaises(ValueError):
            probe.read_pcm(self.wav)

    def test_endpoint_rejects_nonlocal_and_invalid_ports(self):
        for endpoint in ['localhost:8554', '0.0.0.0:8554', '192.168.1.2:8554', '127.0.0.1:65536', '127.0.0.1:22']:
            with self.subTest(endpoint=endpoint), self.assertRaises(ValueError):
                probe.validate_endpoint(endpoint)
        probe.validate_endpoint('127.0.0.1:8554')

    def test_session_must_match_readonly_owner(self):
        path = self.root / 'session.json'
        session = dict(serial='emulator-5556', endpoint='127.0.0.1:8554', pid=123, active=True,
                       read_only=True, avd='Pleos_Connect_v2.1.0')
        path.write_text(json.dumps(session))
        with patch.object(probe, 'command', return_value='123\n'):
            probe.validate_session(path, session['serial'], session['endpoint'])
        for key, value in [('read_only', False), ('active', False), ('serial', 'emulator-5554'), ('avd', 'Phone')]:
            path.write_text(json.dumps({**session, key: value}))
            with self.subTest(key=key), self.assertRaises(ValueError):
                probe.validate_session(path, session['serial'], session['endpoint'])
        path.write_text(json.dumps(session))
        with patch.object(probe, 'command', return_value='999\n'), self.assertRaises(ValueError):
            probe.validate_session(path, session['serial'], session['endpoint'])

    def test_protocol_fields_and_enums_are_checked_before_injection(self):
        proto = self.root / 'sdk.proto'
        source = '''message AudioFormat {
            enum Channels { Mono=0; } enum SampleFormat { AUD_FMT_S16=1; }
            enum DeliveryMode { MODE_UNSPECIFIED=0; }
            uint64 samplingRate=1; Channels channels=2; SampleFormat format=3; DeliveryMode mode=4;
        } message AudioPacket { AudioFormat format=1; uint64 timestamp=2; bytes audio=3; }
          message MicrophoneState { bool realAudioEnabled=1; }'''
        proto.write_text(source)
        probe.validate_protocol(proto)
        for old, new in [('audio=3', 'audio=4'), ('Mono=0', 'Mono=1'), ('uint64 samplingRate', 'uint32 samplingRate')]:
            proto.write_text(source.replace(old, new))
            with self.subTest(old=old), self.assertRaises(ValueError):
                probe.validate_protocol(proto)

    def classify(self, lines, before=None):
        return probe.classify(before or [], (before or []) + lines, self.case)[0]

    def test_only_one_ordered_matching_request_and_callback_matches(self):
        self.assertEqual('ACTION_CALLBACK_MATCHED', self.classify([self.received, self.callback]))
        self.assertEqual('INCONCLUSIVE', self.classify([]))
        for lines in [[self.callback], [self.received], [self.callback, self.received],
                      [self.received, self.callback, self.callback],
                      [self.received, self.callback.replace('asking', 'success')],
                      [self.received, self.callback.replace('false', 'true')],
                      [self.received.replace('SCRAP_APARTMENT', 'GET_AREA_PRICE_SUMMARY'), self.callback]]:
            with self.subTest(lines=lines):
                self.assertEqual('ACTION_CALLBACK_MISMATCH', self.classify(lines))

    def test_log_rollover_cannot_replay_an_old_success(self):
        self.assertEqual('OBSERVATION_LOST', probe.classify(['old marker'], [self.received, self.callback], self.case)[0])

    def test_report_does_not_capture_unstructured_log_payloads(self):
        _, events = probe.classify([], ['arbitrary private text', self.received, self.callback], self.case)
        self.assertEqual(2, len(events))
        self.assertNotIn('private', json.dumps(events))

    def test_case_paths_and_expectations_are_validated(self):
        path = self.root / 'cases.json'
        case = dict(id='test-case', text='합성 문장', action='SCRAP_APARTMENT', status='asking')
        path.write_text(json.dumps([case]))
        self.assertEqual([case], probe.load_cases(path))
        for cases in [[{**case, 'id': '../escape'}], [case, case], [{**case, 'action': 'UNKNOWN'}], []]:
            path.write_text(json.dumps(cases))
            with self.assertRaises(ValueError):
                probe.load_cases(path)

    def serve_audio(self, fail=False, mic_stays_on=False):
        F, P, M = probe.messages()
        calls, audio = [], []
        def setmic(request, context):
            calls.append(('mic', request.realAudioEnabled))
            return Empty()
        def getmic(request, context):
            return M(realAudioEnabled=mic_stays_on)
        def inject(requests, context):
            calls.append(('inject', None))
            for packet in requests:
                audio.append(packet)
            if fail:
                context.abort(grpc.StatusCode.FAILED_PRECONDITION, 'synthetic failure')
            return Empty()
        handlers = {
            'setMicrophoneState': grpc.unary_unary_rpc_method_handler(setmic, request_deserializer=M.FromString, response_serializer=Empty.SerializeToString),
            'getMicrophoneState': grpc.unary_unary_rpc_method_handler(getmic, request_deserializer=Empty.FromString, response_serializer=M.SerializeToString),
            'injectAudio': grpc.stream_unary_rpc_method_handler(inject, request_deserializer=P.FromString, response_serializer=Empty.SerializeToString),
        }
        server = grpc.server(ThreadPoolExecutor(max_workers=2))
        server.add_generic_rpc_handlers((grpc.method_handlers_generic_handler('android.emulation.control.EmulatorController', handlers),))
        port = server.add_insecure_port('127.0.0.1:0')
        server.start()
        self.addCleanup(lambda: server.stop(0).wait())
        return f'127.0.0.1:{port}', calls, audio

    def test_grpc_stream_preserves_pcm_and_keeps_real_mic_off(self):
        endpoint, calls, audio = self.serve_audio()
        with patch.object(probe.time, 'sleep'):
            result = probe.inject(self.wav, endpoint)
        self.assertEqual('OK', result['rpc'])
        self.assertEqual([('mic', False), ('inject', None), ('mic', False)], calls)
        self.assertEqual(b'\0'*16000+self.data+b'\0'*16000, b''.join(x.audio for x in audio))
        self.assertTrue(all(x.format.samplingRate == 16000 and x.format.format == 1 and x.format.channels == 0 for x in audio))

    def test_rpc_failure_still_disables_mic(self):
        endpoint, calls, _ = self.serve_audio(fail=True)
        with patch.object(probe.time, 'sleep'), self.assertRaises(grpc.RpcError):
            probe.inject(self.wav, endpoint)
        self.assertEqual(('mic', False), calls[-1])

    def test_refuses_stream_when_real_mic_remains_enabled(self):
        endpoint, calls, _ = self.serve_audio(mic_stays_on=True)
        with self.assertRaises(RuntimeError):
            probe.inject(self.wav, endpoint)
        self.assertEqual([('mic', False)], calls)


if __name__ == '__main__':
    unittest.main()
