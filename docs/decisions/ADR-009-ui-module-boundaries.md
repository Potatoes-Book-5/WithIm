---
id: ADR-009
task: T-024
---

# ADR-009 UI 모듈 경계(designsystem, ui, feature)

## 배경

[ADR-008](ADR-008-gradle-module-boundaries.md)은 Compose UI와 MVI를 모두 `:app`에 둔다. 화면이 늘면서 화면별 의존성(예: 주행 지도 모드의 네이버 지도 SDK, ADR-004)이 앱 전체에 퍼지고, 한 화면의 변경이 앱 모듈 전체를 다시 빌드한다. 팀은 Now in Android의 모듈 구조를 따르기로 했다.

## 선택지

1. ADR-008 유지(UI는 `:app`)
2. 화면마다 `:feature:<name>` 모듈을 두고, 공용 UI를 `:core:designsystem`, `:core:ui`로 분리
3. 2와 같지만 Hilt 등 DI 프레임워크로 ViewModel 주입

## 결정

2026-11-14 사용자 요청으로 2를 채택한다. ADR-008의 `:app` 책임 중 화면 UI 부분을 이 결정으로 대체한다.

| 모듈 | 책임 | 직접 의존 |
|---|---|---|
| `:core:designsystem` | 테마(색, 타이포, `wpx`), 도메인을 모르는 공용 컴포넌트와 아이콘 | 없음 |
| `:core:ui` | 여러 화면이 쓰는 표시 모델, 포맷, 도메인 값 라벨, 공용 화면 조각 | `:core:domain`, `:core:designsystem` |
| `:feature:<name>` | 한 화면(또는 화면 묶음)의 Contract, Reducer, ViewModel, Route, Screen, 화면 전용 문자열과 SDK | `:core:domain`, `:core:ui`, `:core:designsystem` |
| `:app` | 화면 전환, DI(`AppContainer`, ViewModel 생성), 매니페스트와 키 주입, 아직 분리하지 않은 화면 | 위 모듈 |

- feature 모듈끼리는 서로 의존하지 않는다. 화면 전환은 `:app`이 맡는다.
- feature의 Route는 ViewModel을 인자로 받는다. ViewModel 생성은 `:app`의 수동 DI(`AppViewModels`)가 맡는다. DI 프레임워크는 도입하지 않는다(ARCHITECTURE의 ADR 필요 항목).
- 키와 `BuildConfig`는 `:app`에 둔다. feature는 필요한 값을 Route 인자로 받는다.
- 패키지명은 옮기기 전과 같게 둔다(`...ui.theme`, `...ui.common`, `...ui.driving`). 모듈만 바뀌고 import는 바뀌지 않는다.
- 첫 대상은 주행 화면(`:feature:driving`, SCR-004·SCR-005·SCR-009)이다. 나머지 화면은 이후 작업에서 같은 방식으로 옮긴다.
- 기존 `:feature:yeongtong`은 화면이 없는 UseCase 모듈이라 이 표의 feature와 성격이 다르다. 화면이 생기면 이 규칙을 따른다.

## 결과와 영향

- 네이버 지도 SDK 의존성은 `:feature:driving`에만 남는다.
- 화면 전용 문자열은 feature 모듈의 리소스로 옮긴다. `nonTransitiveRClass`라 각 모듈은 자기 `R`을 쓴다.
- 화면 테스트는 해당 feature 모듈에 둔다. 공용 테스트 대역 모듈(`:core:testing`)은 필요해질 때 만든다.
