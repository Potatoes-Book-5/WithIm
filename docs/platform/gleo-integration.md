# Gleo·위치·내비 연동 확인 사항

기능 정의: [F-010](../product/features/F-010-gleo-app-control.md) · 여정: [S-011](../product/scenarios/S-011-conversational-property-tour.md)

공식 문서 확인일은 모두 2026-11-14이다. 아래는 문서에 있는 설명과 이 제품의 제안을 구분한 것이다.

| 항목 | 확인 내용 | 설계 영향 | 출처 |
|---|---|---|---|
| Gleo Interface | 등록 함수 호출과 앱 Callback 기반 응답 | 자연어 해석은 Gleo, 데이터 조회·저장·계산은 앱 책임 | [Gleo 개념](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/core-concepts) |
| App Schema | actionCalls 최대 3개, description은 영어, 필수 파라미터 누락 가능 | MVP 음성 함수를 3개로 구성하고 앱에서 재검증 | [App Schema](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability) |
| Callback | success/asking/fail, voice/beep/silent, 7초 Action timeout | 제품 내부 완료 기한은 5초로 제안 | [Callback](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback) |
| Intent 전송 | activity는 startActivity, service는 sendBroadcast | service를 일반적인 startService 호출로 해석하지 않음 | [코드 예제](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples) |
| 위치·진입 감지 | FusedLocation의 위치 조회/갱신, isSupported, addProximityAlert | 지원 확인 후 사용. 마지막 위치는 현재 위치와 다를 수 있음 | [FusedLocation](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro) |
| 내비게이션 | 목적지 설정·취소, 화면 요청, 상태 이벤트 제공 | 경로 요청 제출과 실제 안내 시작을 구분 | [NaviHelper](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro) |
| 주행 UX | CarUxRestrictions와 ActiveRestrictions를 반영해야 함 | 속도 0 또는 P단 하나만으로 상세 화면을 해제하지 않음 | [주행 규제](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide) |
| 하드웨어 | 센서 직접 접근 금지, 표준 플랫폼 API 경유 | 카메라 접근을 구현 가능으로 가정하지 않음 | [하드웨어 접근](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture) |
| 오디오 | 포커스 획득·상실 처리, Gleo 호출 시 앱 음성 중단 필요 | 음성 출력은 단일 주체가 담당 | [오디오 포커스](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development) |
| 앱 생명주기 | 미사용 상태의 불필요한 FGS 유지 금지 | 시동 후 상주 프로세스로 자동 실행한다고 가정하지 않음 | [BG→CCH](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/bg-to-cch-transition-guide) |

현재 공개 문서 확인만으로는 외부 텍스트를 Gleo로 보내는 API, 앱 이벤트로 Gleo를 깨워 자동 발화시키는 API, 차량 전방/서라운드 카메라의 서드파티 스트림/녹화 API를 확인하지 못했다. 미확인은 불가능 판정이 아니다. 관련 기능은 실제 API·권한·심사 조건을 확인한 후 활성화한다.

기존 프로젝트는 Emulator v2.1.0을 사용한다. 현재 공식 위치/내비 문서의 SDK 버전과 실제 설치 환경의 호환성은 별도 확인한다. 이 명세 때문에 SDK 환경을 변경하지 않는다.

## Gleo 전송·응답 계약

- App Schema 예시는 [schema.example.json](examples/gleo/schema.example.json)에 둔다. 앱의 assets가 아니며 실제 등록/승인을 의미하지 않는다.
- Callback 예시는 [callback-price.example.json](examples/gleo/callback-price.example.json)에 둔다. data는 JSON 배열을 직렬화한 문자열이다.
- description과 Callback message는 영어로 작성한다. 사용자 언어의 고유명사 등은 담을 수 있다.
- component=service는 문서상 sendBroadcast 전달을 뜻한다. startService나 상주 Foreground Service 사용을 자동으로 허용하지 않는다.
- 일반 가이드 예제는 ACTION_PARAM을 사용하지만 [라디오 예제](https://document.pleos.ai/docs/pleos-only/app-integration-guide/use-cases/radio-app)는 actionParameters를 사용하는 등 문서의 파라미터 전달 설명이 일치하지 않는다. 실제 설치된 Gleo 버전의 wire format과 수신/응답 상관관계를 검증해야 한다. 한 가지 키가 모든 환경에서 보장된다고 쓰지 않는다.
- 앱 내부 요청 ID와 Gleo가 제공하는 correlation 정보는 별개다. 후자는 확인되지 않았다.
- 등록 방법과 승인: [Delivering the Schema](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema), 확인일 2026-11-14.

## R-LIFECYCLE — 제품 생명주기 요구사항

화면이 보이지 않을 때 화면 갱신을 중단한다. 진입 알림을 위해 유지할 수 있는 백그라운드 기능은 플랫폼 허용을 별도 검증한다. 앱 강제 종료·재시작·suspend/resume에서 관심 지역은 보존하되 만료 문맥은 복구하지 않는다. 위치 리스너/오디오/SDK 연결은 사용 종료 시 해제한다. 상주를 위해 불필요한 FGS를 유지하지 않는다.

현재 설치 SDK의 지원을 확인하기 전에는 백그라운드 geofence·시동 자동형·카메라 기능을 활성화하지 않는다. 검증용 Gate는 [테스트 가이드](../testing/gleo-tour/README.md)에 있다.

## 원문에 없는 항목

제품 timeout·위치 freshness·집계 기간·반경·프로필 정책은 [제품 제안 기본값](../product/gleo-tour-defaults.md) 및 각 기능의 초안이다. Gleo 호출의 액션 수 제한·7초 timeout과 혼동하지 않는다. 카메라 표준 API, 외부 텍스트 입력 API, 이벤트 기반 Gleo 자동 발화 API는 제공 방법을 확인하지 못했다.
