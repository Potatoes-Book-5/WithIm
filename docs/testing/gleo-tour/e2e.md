---
task: T-039
---
# Gleo E2E

## 자동 검증

JDK 17과 Android SDK, 온라인 Pleos 기기가 필요하다. 저장소 루트에서 실행한다.

```bash
./harness/verify.sh
./harness/gleo-e2e.sh
```

macOS에서 기본 Java가 17이 아니면 먼저 `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`을 실행한다. 기기가 여러 개면 `export ANDROID_SERIAL=emulator-5556`처럼 adb devices의 대상 serial을 지정한다.

두 번째 명령은 앱 설치 후 GleoWireTest를 실행한다. 명시적 manifest receiver → Application → variant DI → adapter → domain UseCase → debug dummy repository → 공식 APP_ACTION_RESPONSE broadcast 전체를 확인한다. 가격 12억원·합성 표시, 저장 fail/persisted=false, 브리핑 3개, 잘못된 입력, flat extras와 혼합 거부를 검사한다. 이는 Gleo LLM을 거치지 않는 앱 경계 E2E다. 결과는 app/build/reports/androidTests/connected/debug/에 생성된다.

## 실제 음성 E2E

1. debug 앱 설치 후 schema가 들어 있는지 확인한다. release에는 schema/receiver가 없다.
2. Gleo가 실행되는 Pleos 환경에서 설치/부팅 시 schema 재스캔과 승인 상태를 확인한다. 스키마 승인·Gleo 계정/서비스 상태는 앱 코드만으로 보장하지 않는다.
3. “부동산 앱에서 이 동네 집값 알려줘”, “이 동네 관심 지역으로 저장해줘”, “부동산 브리핑 알려줘”를 순서대로 말한다.
4. 각 요청이 GET_AREA_PRICE_SUMMARY / SAVE_INTEREST_AREA / GET_REAL_ESTATE_BRIEFING으로 전달되는지 확인한다. target 누락은 asking, 실제 지역명을 지정한 요청은 dummy가 실거래를 가장하지 않고 unsupported를 반환해야 한다.
5. callback은 공식 ai.pleos.playground.intent.action.APP_ACTION_RESPONSE, packageName과 response extra를 사용한다. mode=silent이므로 소리가 안 나는 것만으로 실패라고 판단하지 않는다. 합성 결과는 음성으로 재생하지 않는다.
6. 하나의 요청이 진행 중일 때 추가 요청을 보내 중복 응답이 없는지 확인한다. D/P를 전환해 기존 주행 화면이 유지되는지도 확인한다.

음성 인식·Gleo action 변환·schema 승인까지 확인해야 실제 음성 E2E 통과다. 앱 wire 계측 통과만으로 이를 통과 처리하지 않는다. 설치된 Gleo 버전의 wire 형식이 JSON 객체 ACTION_PARAM 또는 개별 target/areaQuery extra인지 확인한다. 문서 예제의 이중 인코딩 문자열은 자동 추측하지 않는다.

## 수동 앱 진입 smoke

```bash
adb shell am broadcast -n com.potatoes.book5.realestate/.GleoActionReceiver -a GET_AREA_PRICE_SUMMARY --es target CURRENT_LOCATION
```

Broadcast completed는 수신 완료일 뿐 비동기 API 성공 증거가 아니다. callback의 status/data는 위 계측 테스트로 검증한다. force-stop 상태면 앱을 먼저 실행하거나 계측 명령으로 설치·기동한다.

## 출처

2026-11-15 확인: [schema 전달](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema), [호출·callback 예제](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples), [callback 규격](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback).

## 현재 환경 확인

2026-11-15 연결된 emulator-5554는 온라인이다. pm list packages의 gleo/voice/assistant 검색에서 Gleo로 식별 가능한 패키지가 없고, 공식 APP_ACTION_RESPONSE에 대한 정적 receiver 조회도 결과가 없었다. 동적 receiver는 이 명령에 나타나지 않을 수 있으므로 부재를 단정하지 않는다. 현재 기기에서 실제 Gleo 음성 E2E가 가능한지는 확인되지 않았으며, 앱 경계 계측 E2E와 구분한다.

## 기기 검증 중 확인한 사항

공식 response는 외부 앱이 받는 implicit broadcast이므로 계측의 임시 callback 수신기도 RECEIVER_EXPORTED로 등록해야 한다. 앱 내부 전용 NOT_EXPORTED 수신기를 쓰면 Android 15에서 공식 경로의 응답을 받지 못했다. 임시 테스트 수신기는 finally에서 해제하고 실제 데이터·변경 권한을 제공하지 않는다. 앱의 release receiver 공개 여부와는 별개다.

2026-11-15 emulator-5556(Pleos API 35)에서 GleoWireTest 8개 통과. debug schema/receiver 존재, release APK schema·manifest receiver 부재 및 release 단위 테스트 컴파일 통과를 확인했다. 최초 emulator-5554의 package 서비스 준비 실패·종료는 별도 기기 문제로 기록한다. 실제 Gleo 음성 검증은 아직 수행하지 않았다.
