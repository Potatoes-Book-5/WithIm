---
task: T-039
---
# Gleo E2E

최신 실제 가격 계약/검증은 [T-043 실제 가격](live-price.md), 이전 합성 음성/기기 기록은 [T-042](synthesized-speech.md)을 따른다. T-042 당시 생성 음성 주입 3개와 Pleos 계측 17개를 확인했다. T-043 현재 전체 계측은 18개이며, 실제 Gleo 음성 실행은 assistant 연결 부재로 BLOCKED다. 아래 이전 실행 기록과 구분한다.

## 자동 검증

JDK 17과 Android SDK, 온라인 Pleos 기기가 필요하다. 저장소 루트에서 실행한다.

```bash
./harness/verify.sh
./harness/gleo-e2e.sh
```

macOS에서 기본 Java가 17이 아니면 먼저 `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`을 실행한다. 기기가 여러 개면 `export ANDROID_SERIAL=emulator-5556`처럼 adb devices의 대상 serial을 지정한다.

두 번째 명령은 앱 설치 후 GleoWireTest를 실행한다. 명시적 manifest receiver → Application → variant DI → adapter → domain UseCase → runtime repository → 공식 APP_ACTION_RESPONSE broadcast 전체를 확인한다. 실제 가격 또는 미준비 응답, 지역 저장/브리핑 미지원, 잘못된 입력, flat extras와 혼합 거부를 검사한다. 이는 Gleo LLM을 거치지 않는 앱 경계 E2E다. 결과는 app/build/reports/androidTests/connected/debug/에 생성된다.

## 실제 음성 E2E

1. debug 앱 설치 후 schema가 들어 있는지 확인한다. release에는 schema/receiver가 없다.
2. Gleo가 실행되는 Pleos 환경에서 설치/부팅 시 schema 재스캔과 승인 상태를 확인한다. 스키마 승인·Gleo 계정/서비스 상태는 앱 코드만으로 보장하지 않는다.
3. “부동산 앱에서 이 동네 집값 알려줘”, “이 단지 스크랩해 줘”, “부동산 브리핑 알려줘”를 순서대로 말한다.
4. 각 요청이 GET_AREA_PRICE_SUMMARY / SCRAP_APARTMENT / GET_REAL_ESTATE_BRIEFING으로 전달되는지 확인한다. target 누락은 asking, 명시한 준비된 시군구는 실제 집계를 반환하며, 공개 debug의 현재 위치/직전 지역 요청은 unsupported다.
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

## T-040 Nudge → 스크랩 E2E

`./harness/gleo-e2e.sh`는 기존 wire 검증과 VoiceScrapE2ETest를 함께 실행한다. 후자는 synthetic 단지 fixture와 별도 in-memory Room을 사용하여 Nudge snapshot → SCRAP_APARTMENT Android Intent decode → 상세 fetch → 실제 Room 저장 → 공식 callback → 재요청의 alreadySaved → 배너 만료 뒤 asking을 검증한다. 실제 사용자 DB/동의 설정을 변경하지 않는다. manifest receiver/DI 경로 자체는 GleoWireTest가 별도로 검사한다. 이 둘을 실제 Gleo LLM 음성 E2E와 혼동하지 않는다.

실제 음성 확인은 관심 매물 배너가 표시 중인 상태에서 ‘스크랩해 줘’를 말해 SCRAP_APARTMENT 선택, persisted=true callback, 배너 ‘스크랩함’, 주차 후 목록을 확인한다. P 전환·앱 background·배너 만료 뒤에는 숨겨진 Nudge를 저장하지 않아야 한다. 배너 탭과 음성을 연속 실행해도 해제되지 않아야 한다. 현재 schema의 세 함수 중 지역 저장은 단지 스크랩으로 교체됐다.

T-040 최종 기록: headless emulator-5556에서 verify --device 통과(Android 16개). 별도 Gleo E2E는 9개 통과. JVM 141개 통과, release 빌드·테스트 컴파일 통과. 실제 Gleo 서비스의 자연어 해석/음성 출력·승인은 미검증이다.

## 실제 기기 사전 확인 갱신

[2026-11-15 실제 Gleo 확인 기록](voice-e2e-20261115.md). 현재 Gleo는 ai.umos.ambientai로 확인됐다. 이전 단순 패키지명 검색 결과를 Gleo 부재로 해석하지 않는다. 실제 발화·스키마 승인·Nudge 스크랩은 여전히 미검증이다.

## T-041 수집과 음성 읽기 분리 검증

`./harness/gleo-e2e.sh`에 PreparedMarketE2ETest를 추가했다. fake 거래 공급자의 완료를 지연한 동안 새 후보가 미준비인지 확인하고, 완료 뒤 지오코딩이 대기하며 수집 Mutex가 점유된 상태에서 새 후보 1,000회 읽기가 1초 이내인지 검사한다. 이는 메모리 조회 부분의 계측이며 Gleo 전체 발화·LLM·DB 저장까지 1초를 보장하는 결과가 아니다. 로그 태그 PreparedMarketPerf에 실제 시간을 남긴다.

VoiceScrapE2ETest는 cold read의 DATA_NOT_READY와 저장 없음, 준비된 데이터에서 실제 in-memory Room 저장, 캐시 eviction 뒤 이미 저장된 요청의 멱등 성공을 확인한다. 실제 network/keys/사용자 DB에 의존하지 않는다.

Android 실행·계측 대상은 Pleos Connect로 제한한다. 하네스는 automotive 기능과 Pleos 핵심 차량 서비스 패키지를 사전 확인하며, 일반 Phone AVD·미확인 기기에서는 설치 전에 종료한다. ANDROID_SERIAL 지정 시 해당 기기만, 미지정 시 연결된 모든 온라인 기기를 검사한다. 이는 개발 환경 검사이며 앱의 차량 모델별 분기가 아니다.

T-041 검증 범위: JVM 155개, debug/release 빌드·lint·release 테스트 컴파일·Android 계측 APK 빌드 통과. JVM 경합 테스트의 1,000회 읽기는 3ms였다. Pleos 기기에서 새 계측은 package/계측 서비스 불안정으로 미완료다. 이전 T-040 기기 통과 기록을 T-041의 기기 통과로 재사용하지 않는다. 일반 Phone AVD 시도는 유효 검증에서 제외한다. 안정적인 Pleos 환경에서 `./harness/gleo-e2e.sh`로 재검증한다.

T-042 후속 실행: read-only Pleos Connect에서 Gleo 경계 10개와 전체 Android 계측 17개가 통과했다. PreparedMarketPerf의 1,000회 조회는 10ms였다. [기계 판독 증거](runs/2026-11-15-tts.json)에 계측 이벤트와 음성 경로의 별도 BLOCKED 상태를 남긴다.
