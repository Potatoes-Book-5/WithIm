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

- `--instrumentation all`: 앱의 전체 Android 계측 23개. `--instrumentation`만 주면 Gleo 경계 11개. 생략하면 음성 경로만 실행한다.
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

[cases.json](cases.json)은 명시한 시군구 가격, 브리핑 미지원, 대상 없는 스크랩의 세 발화다. 가격은 해당 시군구의 fresh prepared 데이터와 foreground/동의가 선행 조건이며 실제 값으로 응답한다. 브리핑은 fail/UNSUPPORTED를 기대한다. 대상 없는 스크랩은 asking을 기대한다. 공개 debug 스크랩은 현재 보이는 유효 추천 배너만 저장하며 상세 화면·외부 전달 ID는 사용하지 않는다. 따라서 이 기본 세 케이스는 영구 저장을 하지 않는다. 성공 저장은 별도 in-memory Room 계측 또는 명시적인 추천 배너 음성 수동 시험으로 구분한다. 생성하는 발화 텍스트만 fixture이며 가격 응답을 합성하지 않는다.

RPC는 loopback에만 열고 JWT를 유지한다. [emulator-access.json](emulator-access.json)은 오디오 주입과 마이크 상태 제어 3개 RPC만 테스트 인스턴스에 허용한다. 실 마이크는 끄며 호스트 녹음·스피커 재생을 하지 않는다. 관리 도구가 발급한 활성 read-only 세션과 RPC 소유 PID가 일치해야 주입한다. SDK에 배포된 proto 필드·enum이 바뀌면 중단한다. 원문 발화는 고정 합성 fixture이며 로그에는 사용자 위치·식별자·응답 본문을 남기지 않는다. emulator.log 등 로컬 파일은 공개 결과로 커밋하지 않는다.

표준 VoiceInteractionService binder의 null은 진단 정보로만 기록한다. Gleo 4.2.0.15에서 이 값이 null이어도 실제 STT와 앱 호출이 성공했으므로 음성 입력을 차단하는 조건으로 쓰지 않는다. non-null도 listening의 증거는 아니다. Gleo 호출 반응·듣기 준비를 확인한 뒤 명령을 보내고, 실제 앱 수신·callback으로 판정한다. vendor private DB나 인증 정보를 고치지 않는다.

오디오 전송은 설치 SDK proto의 `MODE_UNSPECIFIED` 흐름 제어를 따른다. 이 모드는 에뮬레이터가 오디오 프레임을 요구할 때까지 서버 큐가 차단하므로 클라이언트에서 20ms씩 추가로 쉬지 않는다. 재생·렌더링 부하가 있는 상태에서도 음성 조각이 연속 전달되도록 한다. `MODE_REAL_TIME`은 별도 실험 모드이며 이 도구에서 사용하지 않는다.

공식 근거(확인 2026-11-15): 설치 SDK의 `emulator/lib/emulator_controller.proto`와 [AOSP inject_audio.py 예제](https://android.googlesource.com/platform/external/qemu/+/08aefd1f57c653d17e9ef972df9d82c978f7695c/android/android-grpc/python/samples/audio/inject_audio.py). 상세 결과·TC 범위는 [TTS 검증 기록](../../docs/testing/gleo-tour/synthesized-speech.md)을 참고한다.

### 실제 추천 배너에서 저장 재현(T-041)

Debug APK에서 위치 동의·관심 조건·관심 알림을 켜고, 유효한 테스트 위치를 공급한 뒤 개발용 ‘주행 시작’을 누른다. 시세 조회가 끝나면 다음 입력으로 실제 추천 상위 3개 중 미저장 후보의 검증된 위치에 접근한다. `arriveNow`를 생략하면 기존과 같이 60초간 이동하고, 도착 후에는 90초 동안 테스트 위치를 유지한다. 현재 조건·피드백을 사용하므로 후보가 없거나 모두 저장돼 있으면 알림이 발생하지 않는다. 동일 단지 24시간 억제 정책은 변경하지 않는다.

```sh
adb -s emulator-5554 shell am broadcast --user 10 \
  -a com.potatoes.book5.realestate.DEBUG_LOCATION \
  -n com.potatoes.book5.realestate/.vehicle.location.DebugLocationReceiver \
  --ez approachRecommendation true --ez arriveNow true
```

`RecommendationTts: event=requested kind=RECOMMENDATION`과 실제 추천 배너를 확인한 뒤에만 Gleo를 호출해 스크랩 명령을 발화한다. 대장 알림(`kind=LEADER`)은 TTS만 확인한다. 앱의 `success persisted=true`와 같은 배너의 ‘스크랩했어요/스크랩함’을 모두 확인해야 저장 성공으로 처리한다. `asking persisted=false`는 저장 성공이 아니다. 가상 마이크 합성 음성과 사람의 실제 마이크 발화 결과는 구분한다.
