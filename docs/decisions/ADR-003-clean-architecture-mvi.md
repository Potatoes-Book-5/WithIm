---
id: ADR-003
task: T-012
---

# ADR-003 Clean Architecture + MVI 채택

## 배경

여러 명이 AI 에이전트와 함께 개발하므로 코드 위치와 상태 변경 방식을 예측할 수 있어야 합니다. 핵심 로직은 차량 환경 없이 검증해야 하고, 화면은 주행 상태, 오프라인, 로딩 등 여러 상태를 일관되게 표현해야 합니다.

## 선택지

1. MVVM과 계층 구분 없는 구조
2. Clean Architecture + MVVM
3. Clean Architecture + MVI

## 결정

3번을 채택합니다.

- 계층은 `ui`, `domain`, `data`, `vehicle`로 나누고 의존성은 `domain`을 향합니다.
- 화면 상태는 `Intent → ViewModel → UseCase → Result → Reducer → State`의 단방향으로 변경합니다.
- 단일 `app` 모듈에서 패키지로 계층을 구분합니다.
- 현재 프로젝트의 Android SDK와 빌드 환경을 유지합니다.

세부 규칙은 [ARCHITECTURE.md](../ARCHITECTURE.md)에 있습니다.

## 결과와 영향

- 순위, 조건 판정, 알림 정책, Reducer를 JVM 단위 테스트로 검증할 수 있습니다.
- 화면 상태가 한 객체에 모여 주행 전환과 프로세스 복원을 다루기 쉽습니다.
- 화면마다 Contract, Reducer, ViewModel, Route, Screen 파일이 필요해 코드량이 늘어납니다.
- 계층 규칙은 현재 코드 리뷰로 확인하며 자동 검사는 없습니다.
