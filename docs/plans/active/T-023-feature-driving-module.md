---
task: T-023
---

# T-023 주행 화면 feature 모듈 분리

## 목표

Now in Android 구조에 맞춰 공용 UI를 `:core:designsystem`, `:core:ui`로 나누고, 주행 화면(`ui/driving`)을 `:feature:driving`으로 옮긴다(ADR-009). 화면 동작은 바꾸지 않는다.

## 범위 밖

- 다른 화면(홈, 상세, 스크랩 등)의 feature 모듈 분리
- DI 프레임워크 도입, 내비게이션 라이브러리 도입
- SDK, AGP, Kotlin 버전 변경

## 작업 체크리스트

- [x] T-023, ADR-009 등록과 계획
- [x] `:core:designsystem`: `ui/theme`, 도메인을 모르는 공용 컴포넌트·아이콘
- [x] `:core:ui`: 순위 표시 모델, 포맷, 조건 라벨, 스크랩 공용 UI와 문자열
- [x] `:feature:driving`: 주행 화면, 지도, 문자열, 네이버 지도 의존성, 테스트
- [x] `:app`: Route에 ViewModel과 지도 사용 여부 전달
- [x] 빌드, 단위 테스트, 에뮬레이터 확인, ARCHITECTURE 갱신

## 검증 방법

- L1: `./harness/verify.sh`, `./gradlew :core:domain:test`
- 에뮬레이터: 홈 → 주행 시작 → 순위 보기 → 지도 보기(네이버 지도와 마커) → 주차 → 홈

## 결정 로그

- 2026-11-14: 주행 화면부터 feature 모듈로 옮긴다(사용자 요청). T-013 브랜치 위에서 작업한다.

## 진행 기록

- 2026-11-14: T-023, ADR-009 등록, 계획 작성
- 2026-11-14: `:core:designsystem`(theme, Components, Icons, LargeSnackbar, Pentagon), `:core:ui`(RankingUi, Format, ConditionLabels, ScrapUi와 문자열 29개), `:feature:driving`(주행 화면 7개 파일, 문자열 15개, 네이버 지도 의존성) 분리. 패키지명 유지
- 2026-11-14: `DrivingRoute`가 ViewModel과 지도 사용 여부를 인자로 받도록 바꿨다. 주행 테스트는 `:feature:driving`으로 옮겼다
- 2026-11-14: L1 통과. 단위 테스트 77건(app 62, feature:driving 5, core:domain 8, core:data 2). 에뮬레이터에서 홈 → 주행 시작 → 순위 → 지도(네이버 지도, 마커) → 주차 → 홈 확인
- `./harness/verify.sh`는 순수 Kotlin 모듈(`core:domain`, `feature:yeongtong`)의 `test`를 돌리지 않아 따로 실행했다
