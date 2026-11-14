# Gleo dummy 어댑터 개발 검증

T-021은 최신 main의 화면·Room·설정 위에 외부 호출 경로를 추가한다. 실제 Gleo 자연어/자동 발화/차량 위치/가격 원천 연동과 별개다.

## 구현 범위

- 루트 debug GleoActionReceiver → vehicle.gleo → 3개 domain UseCase → data.sample.gleo.
- ACTION_PARAM에 JSON 객체 문자열을 담는 개발 profile 하나만 지원한다.
- 조회/브리핑은 합성 결과, 저장은 NOT_APPLIED/persisted=false다.
- 모든 응답은 silent이며 앱 내부 고정 action `com.potatoes.book5.realestate.debug.GLEO_DUMMY_RESPONSE`로만 전달한다. 운영 APP_ACTION_RESPONSE 경로는 아직 연결하지 않았다.
- release에는 Receiver와 schema 등록 asset이 없다. 앱 UI/기존 저장 데이터는 이 경로에서 변경하지 않는다.

## 개발용 Intent 예

아래는 앱의 debug 테스트 진입점 호출이다. Gleo의 자연어 해석 검증이 아니며 실제 지역 가격/저장을 제공하지 않는다.

```bash
adb shell am broadcast \
  -n com.potatoes.book5.realestate/.GleoActionReceiver \
  -a GET_AREA_PRICE_SUMMARY \
  --es ACTION_PARAM '{"target":"CURRENT_LOCATION"}'
```

SAVE_INTEREST_AREA도 같은 target을 사용한다. GET_REAL_ESTATE_BRIEFING의 ACTION_PARAM은 `{}`다. 실제 지역명 대신 `{"target":"NAMED_AREA","areaQuery":"가상마을 A"}` 합성 대상만 사용할 수 있다. 실제 지역명은 Unsupported다.

am broadcast의 콘솔 메시지는 전달 요청 결과이며 앱 Callback JSON이 아니다. 응답 JSON은 아래 instrumentation 테스트가 앱 내부 수신점에서 확인한다.

## 검증 명령과 대상

- `./harness/verify.sh`: Docs, assembleDebug, 전체 JVM 테스트, lintDebug.
- `./harness/verify.sh --device`: 위 항목과 installDebug/전체 Android instrumentation.
- release merged manifest에서 GleoActionReceiver가 없고, debug manifest에서만 존재하는지 검사한다.

| 새 테스트 | 범위 |
|---|---|
| AppActionUseCasesTest | 합성 금액/문맥/입력 검증, 실제 지역 거부, 저장 비영속/결정성 |
| GleoRequestValidatorTest | target 누락/타입/enum, 입력 충돌, 프로필·목적지 주입 거부 |
| GleoRequestCoordinatorTest | timeout/지연·중복·취소·sender 기한/오류·gate 해제 |
| GleoIntentAdapterTest | 3개 dispatch, asking/fail/success 매핑, 합성 저장의 실패 표시 |
| GleoWireTest | Android JSON/Intent/Callback envelope와 실제 debug Receiver round trip |

기존 test-cases.csv의 제품/Gleo E2E 케이스를 dummy 테스트 통과로 바꾸지 않는다. 실제 실행 결과와 환경은 [구현 계획](../../plans/active/T-021-gleo-intent-adapter-dummy.md)의 진행 기록에 남긴다.

## 2026-11-14 실행 결과

전체 JVM 99개(새 Gleo 28개), Pleos API 35 Android instrumentation 13개(새 Gleo 6개), assembleDebug/lintDebug, release manifest 분리 검사를 통과했다. 이 결과는 앱 내부 dummy 테스트 수신점 기준이며 실제 Gleo 음성/등록 검증이 아니다.
