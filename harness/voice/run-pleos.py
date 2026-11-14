#!/usr/bin/env python3
"""Own a disposable Pleos Connect emulator for offline TTS verification."""
import argparse
import json
import os
import re
import socket
import signal
import uuid
from datetime import datetime, timezone
import subprocess
import sys
import time
from pathlib import Path

from tts_probe import ROOT, command, error_info, parse_events


def available(port):
    with socket.socket() as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(('127.0.0.1', port))


def parse_args():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--out', type=Path, required=True)
    ap.add_argument('--sdk', type=Path, default=Path(os.environ.get('ANDROID_SDK_ROOT', str(Path.home() / 'Library/Android/sdk'))))
    ap.add_argument('--avd', default='Pleos_Connect_v2.1.0')
    ap.add_argument('--port', type=int, default=5556)
    ap.add_argument('--grpc-port', type=int, default=8554)
    ap.add_argument('--skip-build', action='store_true', help='Use the already built debug APK')
    ap.add_argument('--instrumentation', nargs='?', const='gleo', choices=['gleo', 'all'],
                    help='Run 10 Gleo boundary tests or all 17 app tests (neither is speech E2E)')
    return ap.parse_args()


def instrumentation_passed(output, expected=10):
    return bool(re.search(r'OK \(' + str(expected) + r' tests\)', output)) and not any(
        word in output for word in ["FAILURES!!!", "INSTRUMENTATION_FAILED", "Process crashed"])


def execute(args):
    if not args.avd.startswith('Pleos_Connect_'):
        raise ValueError('Only an explicitly named Pleos Connect AVD may be launched')
    if not 5554 <= args.port <= 5682 or args.port % 2 or not 1024 <= args.grpc_port <= 65535:
        raise ValueError('Invalid emulator ports')
    emulator, adb = str(args.sdk / 'emulator/emulator'), str(args.sdk / 'platform-tools/adb')
    if args.avd not in command([emulator, '-list-avds']).splitlines():
        raise ValueError('Requested Pleos Connect AVD is not installed')
    serial = 'emulator-' + str(args.port)
    if any(line.split()[0] == serial for line in command([adb, 'devices']).splitlines()[1:] if line.split()):
        raise ValueError('Test serial is already in use; leave existing emulators untouched')
    for port in [args.port, args.port + 1, args.grpc_port]:
        available(port)
    if not args.skip_build:
        # Keep the existing SDK/build environment. Never read local.properties here.
        tasks = [':app:assembleDebug'] + ([':app:assembleDebugAndroidTest'] if args.instrumentation else [])
        subprocess.run([str(ROOT / 'gradlew')] + tasks, cwd=ROOT, check=True, timeout=300)
    apk = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
    if not apk.is_file():
        raise ValueError('Build the debug APK first')
    print('Starting disposable Pleos Connect (real microphone disabled before speech)', flush=True)
    session = {'serial': serial, 'endpoint': '127.0.0.1:' + str(args.grpc_port),
               'avd': args.avd, 'read_only': True, 'active': True}
    manifest = args.out / 'session.json'
    with (args.out / 'emulator.log').open('w') as log:
        process = subprocess.Popen([emulator, '-avd', args.avd, '-read-only', '-port', str(args.port),
            '-no-snapshot', '-no-boot-anim', '-no-window', '-gpu', 'swiftshader', '-audio', 'coreaudio',
            '-grpc', str(args.grpc_port), '-grpc-use-jwt', '-grpc-allowlist',
            str(Path(__file__).with_name('emulator-access.json'))], stdout=log, stderr=subprocess.STDOUT)
        session['pid'] = process.pid
        selected = [adb, '-s', serial]
        try:
            manifest.write_text(json.dumps(session, indent=2) + '\n')
            deadline, last_update = time.monotonic() + 180, 0
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    raise RuntimeError('Test emulator exited during boot')
                try:
                    if command(selected + ['shell', 'getprop', 'sys.boot_completed'], 4).strip() == '1':
                        break
                except (subprocess.SubprocessError, OSError):
                    pass
                if time.monotonic() - last_update > 30:
                    print('Waiting for Pleos boot…', flush=True)
                    last_update = time.monotonic()
                time.sleep(2)
            else:
                raise TimeoutError('Pleos boot deadline exceeded')
            env = dict(os.environ, ANDROID_SERIAL=serial)
            subprocess.run([sys.executable, str(ROOT / 'harness/check-pleos-device.py')], env=env, check=True, timeout=25)
            command(selected + ['emu', 'avd', 'hostmicoff'], 5)
            print('Installing debug app…', flush=True)
            command(selected + ['install', '--no-streaming', '-r', str(apk)], 60)
            instrumentation_ok = True
            if args.instrumentation:
                print('Running app-boundary instrumentation (not a speech test)…', flush=True)
                test_apk = ROOT / 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
                command(selected + ['install', '--no-streaming', '-r', '-t', str(test_apk)], 60)
                classes = ','.join('com.potatoes.book5.realestate.vehicle.gleo.' + name for name in
                    ['GleoWireTest', 'VoiceScrapE2ETest', 'PreparedMarketE2ETest'])
                filters = ['-e', 'class', classes] if args.instrumentation == 'gleo' else []
                expected = 10 if args.instrumentation == 'gleo' else 17
                log_args = ['logcat', '-d', '-v', 'brief', '-s', 'GleoE2E:I', 'PreparedMarketPerf:I', '*:S']
                before = command(selected + log_args, 6).splitlines()
                output = command(selected + ['shell', 'am', 'instrument', '-w', '-r'] + filters + [
                    'com.potatoes.book5.realestate.test/androidx.test.runner.AndroidJUnitRunner'], 120)
                (args.out / 'instrumentation.txt').write_text(output)
                instrumentation_ok = instrumentation_passed(output, expected)
                after = command(selected + log_args, 6).splitlines()
                fresh = after[len(before):] if after[:len(before)] == before else []
                metrics = [int(m[1]) for line in fresh if (m := re.search(r'1000 cache reads: (\d+)ms while collector blocked', line))]
                evidence = {'status': 'PASS' if instrumentation_ok else 'FAIL', 'expected_tests': expected,
                            'speech_test': False, 'events': parse_events(fresh), 'prepared_1000_reads_ms': metrics}
                (args.out / 'app-boundary.json').write_text(json.dumps(evidence, indent=2) + '\n')
                print('App-boundary instrumentation: ' + ('PASS' if instrumentation_ok else 'FAIL'), flush=True)
            probe = [sys.executable, str(Path(__file__).with_name('tts_probe.py'))]
            subprocess.run(probe + ['generate', '--out', str(args.out)], check=True, timeout=210)
            speech_code = subprocess.run(probe + ['run', '--out', str(args.out), '--serial', serial,
                '--endpoint', session['endpoint'], '--session', str(manifest), '--adb', adb,
                '--proto', str(args.sdk / 'emulator/lib/emulator_controller.proto')], timeout=300).returncode
            return speech_code if instrumentation_ok else 2
        finally:
            try:
                command(selected + ['emu', 'avd', 'hostmicoff'], 5)
                command(selected + ['emu', 'kill'], 5)
            except Exception:
                pass
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.terminate()
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=5)
            session['active'] = False
            manifest.write_text(json.dumps(session, indent=2) + '\n')
            print('Closed test emulator and audio endpoint', flush=True)


def main():
    args = parse_args()
    # Every invocation has a new directory; setup failures cannot inherit an older PASS.
    run_id = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ-') + uuid.uuid4().hex[:8]
    args.out = args.out.resolve() / run_id
    args.out.mkdir(parents=True, exist_ok=False)
    print('Run artifacts: ' + str(args.out), flush=True)
    summary = {'run_id': run_id, 'status': 'STARTED', 'speech_e2e_verified': False}
    report = args.out / 'run.json'
    report.write_text(json.dumps(summary, indent=2) + '\n')
    def interrupted(signum, frame):
        raise KeyboardInterrupt()
    signal.signal(signal.SIGTERM, interrupted)
    try:
        code = execute(args)
        summary['status'] = 'ACTION_CALLBACK_MATCHED' if code == 0 else 'INCOMPLETE'
        return code
    except BaseException as exc:
        summary.update(status='BLOCKED', **error_info(exc))
        raise
    finally:
        report.write_text(json.dumps(summary, indent=2) + '\n')


if __name__ == '__main__':
    try:
        sys.exit(main())
    except Exception as exc:
        print(json.dumps({'status': 'BLOCKED', **error_info(exc)}), file=sys.stderr)
        sys.exit(2)
