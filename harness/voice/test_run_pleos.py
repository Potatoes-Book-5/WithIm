"""Launcher lifecycle and result freshness, without starting an emulator."""
import argparse
import importlib.util
import json
from pathlib import Path
import signal
import tempfile
import unittest
from unittest.mock import Mock, patch

spec = importlib.util.spec_from_file_location('run_pleos', Path(__file__).with_name('run-pleos.py'))
launcher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(launcher)


class LauncherTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.args = argparse.Namespace(out=self.root, sdk=self.root / 'sdk', avd='Pleos_Connect_v2.1.0',
                                       port=5556, grpc_port=8554, skip_build=True, instrumentation='gleo')

    def test_instrumentation_requires_all_ten_tests(self):
        self.assertTrue(launcher.instrumentation_passed('INSTRUMENTATION_RESULT: stream=\nOK (10 tests)\n'))
        self.assertTrue(launcher.instrumentation_passed('OK (17 tests)', expected=17))
        self.assertFalse(launcher.instrumentation_passed('OK (10 tests)', expected=17))
        for output in ['OK (0 tests)', 'OK (9 tests)', 'OK (10 tests)\nFAILURES!!!',
                       'OK (10 tests)\nINSTRUMENTATION_FAILED', 'Process crashed', '']:
            self.assertFalse(launcher.instrumentation_passed(output))

    def test_manifest_failure_still_closes_owned_emulator(self):
        process = Mock(pid=123)
        process.wait.return_value = 0
        def command(args, *rest):
            if args[-1] == '-list-avds':
                return 'Pleos_Connect_v2.1.0\n'
            return 'List of devices attached\n'
        with patch.object(launcher, 'available'), patch.object(Path, 'is_file', return_value=True), \
             patch.object(launcher, 'command', side_effect=command) as cmd, \
             patch.object(launcher.subprocess, 'Popen', return_value=process), \
             patch.object(Path, 'write_text', side_effect=OSError('synthetic disk failure')):
            with self.assertRaises(OSError):
                launcher.execute(self.args)
        commands = [call.args[0] for call in cmd.call_args_list]
        self.assertTrue(any(c[-2:] == ['emu', 'kill'] for c in commands))
        self.assertTrue(any(c[-3:] == ['emu', 'avd', 'hostmicoff'] for c in commands))
        process.wait.assert_called_once_with(timeout=10)

    def test_setup_failure_gets_a_new_report_each_time(self):
        previous_handler = signal.getsignal(signal.SIGTERM)
        self.addCleanup(signal.signal, signal.SIGTERM, previous_handler)
        for _ in range(2):
            args = argparse.Namespace(**{**vars(self.args), 'out': self.root})
            with patch.object(launcher, 'parse_args', return_value=args), \
                 patch.object(launcher, 'execute', side_effect=ValueError('synthetic setup failure')), \
                 self.assertRaises(ValueError):
                launcher.main()
        reports = list(self.root.glob('*/run.json'))
        self.assertEqual(2, len(reports))
        for path in reports:
            report = json.loads(path.read_text())
            self.assertEqual('BLOCKED', report['status'])
            self.assertFalse(report['speech_e2e_verified'])

    def test_phone_name_is_rejected_before_any_launch(self):
        self.args.avd = 'Medium_Phone_API_37.0'
        with patch.object(launcher.subprocess, 'Popen') as process, self.assertRaises(ValueError):
            launcher.execute(self.args)
        process.assert_not_called()


if __name__ == '__main__':
    unittest.main()
