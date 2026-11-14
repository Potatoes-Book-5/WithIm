---
task: T-039
---

# gleo-intent-adapter 외부 호출 계약 초안

내부 호출은 [App Action API](app-action-api.md), 코드 배치는 [ADR-010](../decisions/ADR-010-gleo-intent-adapter.md), 작업 순서는 [실행 계획](../plans/active/T-039-gleo-integration.md)을 따른다. T-039에서 debug dummy 경로를 구현했다. 실제 Gleo 호환성/등록은 미검증이다.

## 1. 컴포넌트 경계

논리 이름은 `gleo-intent-adapter`, 패키지는 `vehicle.gleo`다. 어댑터는 다음을 수행한다.

1. Android Intent의 action/extras를 전송 DTO로 변환한다.
2. wire 형식과 값 타입을 검사한다.
3. 명시적 target을 Android와 무관한 API 입력으로 변환한다.
4. action별 domain UseCase를 호출한다.
5. typed 결과를 Gleo Callback으로 변환·송신한다.

가격 계산, 지역 저장, 추천, 프로필 식별은 어댑터 책임이 아니다. Android Intent와 MVI Intent를 혼용하지 않으며 ui/ViewModel/data 구현체를 참조하지 않는다.

## 2. 스키마 vNext

[schema.example.json](../platform/examples/gleo/schema.example.json)은 아래 target 계약으로 갱신했다. debug assets/schema.json에 동일한 합성 계약을 배포한다. release에는 배포하지 않는다.

- GET_AREA_PRICE_SUMMARY와 SAVE_INTEREST_AREA에 `target: string enum`을 추가한다.
- enum: CURRENT_LOCATION / LAST_REFERENCED_AREA / NAMED_AREA.
- `target`을 required로 지정한다. 누락 가능성은 앱이 다시 검사한다.
- `areaQuery: string`은 NAMED_AREA에만 필요하다. 조건부 필수는 앱이 검사하며 JSON Schema의 oneOf/if 같은 미확인 기능에 의존하지 않는다.
- GET_REAL_ESTATE_BRIEFING은 무인자를 유지한다. 음성 함수는 총 3개다.
- raw utterance, profileId, 좌표, requestId, 권한 우회 플래그, 임의의 Callback 주소를 LLM 입력으로 받지 않는다.

target 누락/잘못된 enum/이름 누락은 해당 요청을 실행하지 않는다. 사용자에게 필요한 정보를 물을 수 있는 known action의 경우 asking으로 변환한다. 잘못된 wire 타입은 fail로 처리한다.

## 3. Intent wire profile

공식 예제에는 ACTION_PARAM JSON과 actionParameters 등 상이한 표현이 있어 실제 설치 버전 검증이 필요하다. 자동 테스트는 ACTION_PARAM 객체와 개별 extra 두 profile을 검증한다. 설치 버전에서 음성 호출 확인은 별도다.

현재 decoder는 ACTION_PARAM 객체 JSON 또는 개별 target/areaQuery extras를 지원한다. 복수 키가 충돌하면 거부하고, 임의 fallback/이중 JSON 문자열 자동 복구/여러 형식 추측은 하지 않는다. 다른 profile은 설치 버전 검증 후 추가한다. 일반화한 profile 선택 계층은 아직 없다.

- action은 등록한 3개만 allowlist 처리한다.
- null/알 수 없는 action은 API와 Callback을 호출하지 않는다.
- known action의 malformed JSON/타입 오류는 알려진 functionName의 fail로 반환할 수 있다.
- Gleo wire DTO와 JSONObject는 vehicle.gleo 안에만 둔다.
- Intent의 packageName·문자열 sender 필드를 호출자 인증으로 신뢰하지 않는다.

## 4. 수신 생명주기·기한·동시성

- `GleoActionReceiver`는 루트 Android 진입점 패키지의 debug 소스에 둔다. Application/AppContainer 연결은 이 bootstrap만 담당하며, vehicle.gleo에서 di나 Application을 역참조하지 않는다. debug manifest에서만 노출한다. debug에는 schema asset을 넣고 release에는 exported receiver·schema asset을 넣지 않는다.
- `onReceive`는 즉시 `goAsync()`로 수신 생명주기를 확보하고 짧은 비동기 작업으로 넘긴다. Activity/FGS/상주 서비스는 시작하지 않는다.
- DI가 제공한 scope/dispatcher로 실행하고 `PendingResult.finish()`는 Job completion callback과 원자적 1회 gate로 모든 종료 경로에서 한 번 호출한다. 코루틴 본문 진입 전 취소에도 정리되도록 finally 대신 completion callback을 사용한다.
- 수신 시점부터 전체 내부 상한은 기존 제안 5초다. API budget은 4초, 전체는 5초로 생성자 주입하며 송신/정리 시간을 확보한다. 수신 시각은 루트 Receiver에서 단조 시계로 기록한다. 이는 플랫폼 보장값이 아니다.
- 수락한 요청은 Callback을 최대 한 번만 송신한다. 송신 시점이 기한을 지났으면 뒤늦은 응답을 보내지 않는다. deadline과 완료의 경합을 한 종료 상태로 처리한다.
- 실제 Gleo correlation 정보 확인 전에는 앱 내 single-flight로 수락 요청을 1개로 제한한다. 대기열은 만들지 않는다.
- 중복/겹친 요청에 요청 식별자가 없다면 무조건 BUSY Callback을 보내지 않는다. 같은 functionName의 이전 응답으로 오인될 수 있으므로 추가 요청을 실행·응답하지 않고 종료한다. 수락한 첫 요청만 정상 완료한다. 이는 임시 제한이며 실제 다중 요청 호환성 검증 전 운영 사용하지 않는다.
- 내부 requestId는 로컬 추적 전용이다. 이를 Gleo에 보내면 상관관계가 해결된다고 가정하지 않는다.

Android 동작 근거(확인일 2026-11-14): [BroadcastReceiver.goAsync](https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync()), [Broadcasts overview](https://developer.android.com/develop/background-work/background-tasks/broadcasts). goAsync는 무제한 실행을 보장하지 않으며 완료 시 finish가 필요하다.

## 5. Callback 변환과 dummy 의미

Gleo envelope의 functionName/status/message/mode/data는 [공식 Callback 규격](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback)을 따른다(확인일 2026-11-14). data는 객체 배열을 직렬화한 문자열이다. Gleo enum/status/영문 문구를 domain으로 들여보내지 않는다.

| 내부 결과 | Callback | 의미 |
|---|---|---|
| 가격/브리핑 Completed, DUMMY | success, mode=silent | 테스트 처리가 끝남. data와 영문 message 모두 합성 결과임을 명시 |
| 저장 Completed, DUMMY/NOT_APPLIED | fail, mode=silent | 테스트 응답이며 실제 저장 없음. DUMMY_NOT_PERSISTED 코드 포함 |
| NeedsInput | asking, mode=silent | 필요한 필드·명시된 합성 후보 전달. API 부작용 없음 |
| DomainError.Invalid | fail, mode=silent | 입력 오류, 실제 사용자 데이터 변경 없음 |
| DomainError.Unsupported | fail, mode=silent | 실제 지역 해석 등 이번 dummy가 제공하지 않는 기능 |
| 수락한 요청의 Timeout/내부 실패 | fail, mode=silent, 송신 기한 내에만 | 원인을 구분하고 중복/늦은 응답 금지 |

dummy 저장에는 success를 쓰지 않으므로 F-011의 영속 저장 후 성공 규칙과 충돌하지 않는다. 모든 초기 응답을 silent로 하여 합성 가격이 실제 가격으로 음성 안내되지 않게 한다. 실제 음성 호환성은 별도 후속 검증이다.

## 6. Callback 송신 경로와 보안

- 송신 action/extra는 실제 Gleo 문서/설치 버전과 맞춰 검증한다. 공식 예제의 APP_ACTION_RESPONSE와 packageName/response 필드를 근거로 하되, packageName extra를 ‘목적지 패키지’로 오인하지 않는다.
- debug 앱은 DI가 고정한 공식 APP_ACTION_RESPONSE broadcast로 보내고, 입력 extras가 임의의 수신 앱/주소를 정하지 못하게 한다.
- 실제 Gleo 수신 패키지·권한·호출자 인증은 미결이다. 임의의 vendor permission이나 패키지명을 만들어 쓰지 않는다.
- debug 공개 receiver에는 개인 정보/위치/실제 DB 접근을 연결하지 않는다. release 외부 진입을 열기 전에 인증·최소 권한·데이터 처리 정책을 별도 검토한다.

## 7. 후속 대화와 지원 경계

초기 구현은 명시한 target의 한 Action 처리와 주입한 합성 문맥만 검증한다. 별도의 target 없이 말한 ‘이 동네’에 대한 운영 fallback은 F-010 R-CONTEXT와 조정 전 지원 완료로 표시하지 않는다. ‘첫 번째 후보’처럼 원천 세션·후속 호출 연결이 필요한 대화를 지원 완료로 표시하지 않는다. 합성 LAST_REFERENCED_AREA는 앱이 소유한 문맥이며 Gleo 대화 세션의 복원이 아니다.

가격·브리핑의 실제 데이터 조회, 지역 영속 저장, 현재 GPS, 주행 중 실행, 자동 발화, UI 이동, 카메라, 내비는 이 어댑터 dummy 구현의 완료 조건에 포함하지 않는다.

## 구현 파일과 개발 검증

구현은 vehicle.gleo/GleoAndroidHandler(Decoder 포함), GleoIntentAdapter, GleoRequestValidator, GleoResponseMapper, GleoRequestCoordinator, GleoContract, BroadcastGleoCallbackSender(Encoder 포함)로 구성한다. 순수 부분과 Android codec을 분리했다. wire JSON은 4,096자, 지역명은 256자로 제한하며 이는 개발용 입력 제한값이다. 실행 방법과 검증 결과는 [dummy 검증 가이드](../testing/gleo-tour/dummy-adapter.md)를 따른다.

## T-039 통합 갱신

위 초기 테스트 전용 설명은 다음 배포 배치로 확장한다. core/domain에 AppAction 모델·포트·UseCase, core/data/src/debug에 dummy 구현, app/vehicle/gleo에 adapter를 둔다. debug GleoBindings는 공식 APP_ACTION_RESPONSE sender를 주입하고 debug assets/schema.json을 배포한다. release 바인딩은 null이며 receiver/schema는 없다. decoder는 ACTION_PARAM 객체 또는 개별 target/areaQuery extra를 받으며 혼합 입력은 거부한다. 이중 인코딩 JSON은 지원하지 않는다. 실제 Gleo schema 승인과 음성 호환성은 기기에서 확인해야 한다. [E2E 절차](../testing/gleo-tour/e2e.md).

현재 dummy 평균 fixture는 최신 main 순위 정책과 독립적이다. 실데이터 API를 연결할 때는 Top 3/5 평균을 사용하지 않고 원본 거래를 기준으로 별도 집계한다. 위치 없는 지도 참고 좌표를 CURRENT_LOCATION으로 사용하지 않는다.

## 요청형 지역 확인의 스키마 확장 제안

[F-010 R-AREA-CHECK](../product/features/F-010-gleo-app-control.md)는 가격 조회와 다른 의미의 지역 확인 요청이다. 기존 가격 함수를 GET_AREA_OVERVIEW로 교체/확장하고 purpose=PRICE_SUMMARY 또는 LOCATION_CHECK로 구분해 총 3개 함수를 유지하는 안을 제안한다. LOCATION_CHECK에서 areaQuery로 지정한 지역은 비교 대상이고 실제 위치는 앱이 신뢰 가능한 입력에서 확보한다. “저장한 지역에 들어왔나?”는 활성 프로필의 관심 영역을 조회하며 후보가 모호하면 asking이다.

함수명·필수 파라미터·기존 가격 호출 호환성은 미결이며 실행 schema/decoder/action allowlist를 이번 문서 변경으로 갱신하지 않는다. 현재 가격 함수에 지역 확인을 억지로 매핑하거나 네 번째 함수를 등록하지 않는다. 별도 schema 변경 시 내부 API·callback·자연어 E2E를 함께 변경해야 한다. 앱 context를 Gleo에 해석 전에 제공하는 기능도 구현된 것으로 가정하지 않는다.

앱은 지역 확인의 typed 결과(아래 내부 API 확장안)를 callback으로 매핑한다. asking은 대상 명확화, fail은 유효 위치/필수 실행 조건 불충족, success는 실제 판정 완료(OUTSIDE도 정상 결과)에 사용한다. UNCERTAIN은 판정 불확실성을 명시하며 INSIDE로 바꾸지 않는다. 추천만 실패하면 지역 확인 결과와 추천 실패 상태를 함께 전달한다. 완료 여부·추천의 데이터 시각과 합성 여부도 유지한다. UI 전달은 domain 결과를 기존 MVI 경로로 반영하며 adapter→ViewModel 직접 호출을 추가하지 않는다.

## T-040 현재 등록 함수와 실제 저장

현재 assets schema는 GET_AREA_PRICE_SUMMARY / SCRAP_APARTMENT / GET_REAL_ESTATE_BRIEFING이다. SAVE_INTEREST_AREA는 이전 debug 호출 호환을 위해 receiver/decoder에만 남는다. SCRAP_APARTMENT는 무인자이며 외부 단지 ID/프로필/좌표 주입을 거부한다. 앱 수신 시 확보한 context로 대상을 확정한다. 실제 로컬 저장 성공은 persisted=true, executionMode=LOCAL로 반환하고 dummy로 표시하지 않는다. timeout/예외도 LOCAL로 표시하며 저장 결과를 확인할 수 없을 때 ‘데이터 변경 없음’으로 단정하지 않는다. 모든 callback은 현재 silent다. [context](voice-context.md).
