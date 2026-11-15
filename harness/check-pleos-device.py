#!/usr/bin/env python3
"""Reject unsupported Android runtimes before installing or running tests."""
import os
import shutil
import subprocess
import sys
from pathlib import Path


def main():
    candidates = [shutil.which("adb")]
    candidates += [str(Path(os.environ[key]) / "platform-tools/adb")
                   for key in ("ANDROID_HOME", "ANDROID_SDK_ROOT") if os.environ.get(key)]
    candidates += [str(Path.home() / "Library/Android/sdk/platform-tools/adb")]
    adb = next((p for p in candidates if p and Path(p).is_file()), None)
    if not adb:
        print("adb를 찾을 수 없습니다. Android SDK 경로를 확인하세요.", file=sys.stderr)
        return 1

    def run(*args):
        return subprocess.run([adb, *args], capture_output=True, text=True,
                              check=True, timeout=8).stdout.strip()

    try:
        devices = [line.split()[0] for line in run("devices").splitlines()[1:]
                   if len(line.split()) >= 2 and line.split()[1] == "device"]
        selected = os.environ.get("ANDROID_SERIAL")
        if selected:
            devices = [selected] if selected in devices else []
        if not devices:
            print("온라인 Pleos Connect 기기가 필요합니다. adb devices와 ANDROID_SERIAL을 확인하세요.", file=sys.stderr)
            return 1
        for serial in devices:
            automotive = run("-s", serial, "shell", "pm", "has-feature", "android.hardware.type.automotive")
            service = run("-s", serial, "shell", "pm", "path", "ai.pleos.service.car")
            if automotive != "true" or not service.startswith("package:"):
                print(f"차단: {serial}는 Pleos Connect 실행 환경으로 확인되지 않습니다. 일반 Phone 검증은 금지합니다.", file=sys.stderr)
                return 1
            print(f"Pleos Connect 확인: {serial}")
    except (subprocess.SubprocessError, OSError) as exc:
        print(f"Pleos Connect 기기 확인 실패: {type(exc).__name__}. 기기 부팅·서비스 상태를 확인하세요.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
