---
task: T-014
---

# T-014 온보딩 화면 구현

## 목표

첫 실행 흐름인 동의 화면(SCR-006)과 관심 조건 설정 화면(SCR-003)을 Clean Architecture + MVI로 구현한다. 와이어프레임 `SCR-006_landscape_parked.svg`, `SCR-003_landscape_parked.svg`를 기준으로 한다.

## 범위 밖

- 실제 위치 권한 요청과 FusedLocation 연동(F-001)
- 음성 검색(SpeechToText SDK). 버튼만 두고 준비 중 안내를 표시한다.
- 주행 규제 연동(CarUxRestrictions). 주행 중 진입 차단은 이후 작업에서 한다.
- 홈 화면(SCR-001). 온보딩 이후에는 임시 홈 화면을 보여 준다.
- 공식 색상·글꼴 토큰(T-002). 와이어프레임의 임시 값을 테마에 모아 둔다.

## 작업 체크리스트

- [x] lifecycle-viewmodel-compose, lifecycle-runtime-compose, coroutines 의존성 추가
- [x] 테마 색상·글꼴 크기와 공통 컴포넌트(칩, 버튼, 토글)
- [x] domain: 동의 설정, 관심 조건 모델, 포트, UseCase
- [x] data: SharedPreferences 저장소, 동네 추천 임시 데이터
- [x] AppContainer와 앱 시작 화면 결정(AppViewModel)
- [x] SCR-006 Contract, Reducer, ViewModel, Route, Screen
- [x] SCR-003 Contract, Reducer, ViewModel, Route, Screen
- [x] Reducer, UseCase, ViewModel 단위 테스트
- [x] `./harness/verify.sh` 통과
- [x] 에뮬레이터에서 화면 확인

## 검증 방법

- L1: `./harness/verify.sh`
- L2·L3: Pleos 에뮬레이터에 설치해 온보딩 흐름을 직접 확인

## 결정 로그

- 2026-11-14: AVD 설정은 160dpi지만 앱 창은 229dpi로 실행된다. 와이어프레임 px를 `wpx`·`wsp`(÷1.43)로 환산한다.
- 2026-11-14: lifecycle-runtime-compose 2.9.0 추가로 Compose UI가 1.6.6에서 1.7.8로 올라갔다. 빌드와 테스트로 확인했다.
- 2026-11-14: 내비게이션·DI·DataStore 라이브러리는 ADR이 필요하므로 쓰지 않는다. 화면 전환은 AppViewModel 상태로, 저장은 SharedPreferences로 한다.
- 2026-11-14: 가격대·평수 구간과 동네 추천 목록은 확정 전 임시 값이다(F-004, T-005).

## 진행 기록

- 2026-11-14: 계획 작성
- 2026-11-14: 구현 완료. 단위 테스트 23개 통과
- 2026-11-14: 에뮬레이터에서 동의 → 관심 조건 → 저장 → 재실행 시 홈 진입까지 확인. 저장값(알림 설정, 조건) 확인
- 2026-11-14: 에뮬레이터에는 Driving View가 없고 앱이 가로 전체(2560×1268)를 쓴다. Portrait 창 배치는 Preview로만 확인했다
