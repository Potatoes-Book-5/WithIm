# 합성 음성 검증 도구

실 마이크 없이 macOS의 한국어 Yuna TTS로 WAV를 생성하고, **폐기 가능한 Pleos Connect**의 공식 가상 마이크 RPC로 주입한다. 앱의 debug 진단 이벤트만 수집한다. 일반 Phone AVD는 실행하지 않는다.

## 준비

- 기존 Pleos Connect AVD `Pleos_Connect_v2.1.0`, 기존 프로젝트의 Java/Android SDK 환경
- 로컬에 설치된 Yuna 음성, `/usr/bin/say`, `/usr/bin/afconvert`, `/usr/sbin/lsof`
- Python 3.9 이상과 [requirements.txt](requirements.txt)의 패키지. 앱 SDK 의존성은 바꾸지 않는다.

필요하면 도구 전용 가상 환경에만 설치한다.

```sh
python3 -m venv /tmp/gleo-voice-venv
/tmp/gleo-voice-venv/bin/python -m pip install -r harness/voice/requirements.txt
```

이후 아래 `python3`를 해당 가상 환경의 Python 경로로 바꾼다.

## 실행

저장소 루트에서:

```sh
python3 -m unittest discover -s harness/voice -p 'test_*.py' -v
python3 harness/voice/run-pleos.py --out /tmp/gleo-voice --instrumentation all
```

- `--instrumentation all`: 앱의 전체 Android 계측 18개. `--instrumentation`만 주면 Gleo 경계 11개. 생략하면 음성 경로만 실행한다.
- `--live-provider-probe`: 기존 앱 DI와 실제 원천으로 가격 callback을 점검한다. 가격을 주입하거나 동의를 변경하지 않는다. 결과는 `live-provider.json`의 제한된 상태/건수이며, 환경이 미준비이면 BLOCKED다.
- `--skip-build`: 이미 빌드된 debug 앱·테스트 APK를 사용한다.
- 기존 실행 환경에서 Java 17 설정이 필요하면 `JAVA_HOME`을 기존 JDK 경로로 지정한다.
- 다른 테스트 포트가 필요하면 `--port 5558 --grpc-port 8556`. 이미 사용 중인 기기·포트는 교체하지 않고 중단한다.
- 실행마다 `--out` 하위에 고유 폴더를 만들고 경로를 출력한다. 지난 결과를 덮어 쓰지 않는다.
- `-read-only -no-snapshot`으로 시작하고 종료 시 해당 테스트 인스턴스와 오디오 포트를 닫는다. 강제 종료 후 문제가 있으면 **이번 실행의 session.json에 기록된 테스트 인스턴스만** 확인한다.

음원 생성만 필요하면 Android 실행 없이:

```sh
python3 harness/voice/tts_probe.py generate --out /tmp/gleo-wavs
```

## 판정

| 결과 | 의미 |
|---|---|
| `app-boundary.json`의 PASS | 지정한 계측 테스트 수와 assertion 통과. 실제 음성 인식 검증은 아님 |
| `injection.rpc=OK` | PCM을 emulator RPC가 수락. Gleo가 들었다는 증거는 아님 |
| `ACTION_CALLBACK_MATCHED` | 관찰 구간에 기대한 action 수신 1개와 callback 1개가 순서대로 일치 |
| `ACTION_CALLBACK_MISMATCH` | 다른 action/상태, 중복, 순서 또는 저장 결과 불일치 |
| `INCONCLUSIVE` | 음성 주입 후 앱 이벤트 없음. STT·함수 선택 성공 여부 미확인 |
| `OBSERVATION_LOST` | 로그 교체/삭제로 관찰 구간이 사라짐. 이전 로그로 성공 판정하지 않음 |
| `BLOCKED` | 준비/기기/RPC 오류 또는 `ASSISTANT_NOT_BOUND` 등 선행 조건 미충족 |

`run.json`은 전체 실행 상태, `results.json`은 발화별 기대 action·실제 이벤트, `app-boundary.json`/`instrumentation.txt`는 음성과 분리한 앱 검증 기록이다. `speech_e2e_verified`와 `stt_transcript_verified`는 현재 도구에서 항상 false다. transcript·최종 Gleo 음성은 아직 검증하지 않는다. 종료 코드 0도 action/callback 일치 범위만 의미하며 제품 TC 90개의 PASS가 아니다. 오류·불일치·미확인은 코드 2로 끝난다.

## 입력과 보호 조건

[cases.json](cases.json)은 명시한 시군구 가격, 브리핑 미지원, 대상 없는 스크랩의 세 발화다. 가격은 해당 시군구의 fresh prepared 데이터와 foreground/동의가 선행 조건이며 실제 값으로 응답한다. 브리핑은 fail/UNSUPPORTED를 기대한다. 스크랩은 Home 화면에 선택 상세와 활성 Nudge가 없는 조건에서 asking을 기대한다. 실제 대상 문맥이 생기면 불일치로 처리한다. 영구 사용자 DB를 변경하지 않으며, 성공 저장 경로는 별도 in-memory Room 계측으로 검사한다. 생성하는 발화 텍스트만 fixture이며 가격 응답을 합성하지 않는다.

RPC는 loopback에만 열고 JWT를 유지한다. [emulator-access.json](emulator-access.json)은 오디오 주입과 마이크 상태 제어 3개 RPC만 테스트 인스턴스에 허용한다. 실 마이크는 끄며 호스트 녹음·스피커 재생을 하지 않는다. 관리 도구가 발급한 활성 read-only 세션과 RPC 소유 PID가 일치해야 주입한다. SDK에 배포된 proto 필드·enum이 바뀌면 중단한다. 원문 발화는 고정 합성 fixture이며 로그에는 사용자 위치·식별자·응답 본문을 남기지 않는다. emulator.log 등 로컬 파일은 공개 결과로 커밋하지 않는다.

Gleo가 알려진 null 연결 상태이면 기다린 뒤 BLOCKED 처리한다. non-null 연결도 STT listening을 증명하지 않는다. Gleo UI/로그인/등록된 함수 스키마가 정상인지 별도 확인해야 하며, 이 도구는 vendor private DB나 인증 정보를 고치지 않는다.

공식 근거(확인 2026-11-15): 설치 SDK의 `emulator/lib/emulator_controller.proto`와 [AOSP inject_audio.py 예제](https://android.googlesource.com/platform/external/qemu/+/08aefd1f57c653d17e9ef972df9d82c978f7695c/android/android-grpc/python/samples/audio/inject_audio.py). 상세 결과·TC 범위는 [TTS 검증 기록](../../docs/testing/gleo-tour/synthesized-speech.md)을 참고한다.
