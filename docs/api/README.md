# App API와 외부 호출 계약

이 폴더는 기능 요구사항과 실제 구현 사이의 입력·결과·오류·부작용 계약을 소유한다. 현재 문서는 T-039의 **debug dummy 구현 계약**이다. 실제 Gleo/차량/원천 데이터 지원과 분리한다.

| 문서 | 책임 |
|---|---|
| [app-action-api.md](app-action-api.md) | Android와 무관한 UseCase 기반 App API, 결과 타입, dummy 의미 |
| [gleo-intent-adapter.md](gleo-intent-adapter.md) | Gleo Android Intent→App API→Callback의 변환·수명·전송 계약 |

- 제품이 무엇을 해야 하는지는 [F-010](../product/features/F-010-gleo-app-control.md), [F-011](../product/features/F-011-interest-area-booking.md), [F-013](../product/features/F-013-area-daily-briefing.md)에 둔다.
- 공식 규격과 실제 지원 미결은 [플랫폼 문서](../platform/gleo-integration.md), 배치 근거는 [ADR-010](../decisions/ADR-010-gleo-intent-adapter.md)에 둔다.
- 구체 작업 순서는 [T-039 계획](../plans/active/T-039-gleo-integration.md)을 따른다.
- App API는 Kotlin 호출 계약이다. HTTP 서버나 OpenAPI 도입을 전제하지 않는다. Android Intent나 JSON DTO를 domain API에 노출하지 않는다.
