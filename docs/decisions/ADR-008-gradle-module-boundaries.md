---
id: ADR-008
task: T-022
---

# ADR-008 Gradle 모듈 경계와 의존 방향

## 배경
앱의 Compose UI, 도메인 규칙, Retrofit·Room 저장소, 영통구 조회 기능이 단일 `:app` 모듈에 있다. [ADR-003](ADR-003-clean-architecture-mvi.md)의 패키지 경계를 Gradle 의존성에도 적용한다.

## 선택지
기존 단일 모듈을 유지하거나 계층과 기능을 분리한다.

## 결정
`:app`, `:core:domain`, `:core:data`, `:feature:yeongtong` 네 모듈로 분리한다.

| 모듈 | 책임 | 직접 의존 |
|---|---|---|
| `:core:domain` | 모델, 포트, UseCase, 순수 정책 | 없음 |
| `:core:data` | Retrofit, Room, 저장소 구현과 매퍼 | `:core:domain` |
| `:feature:yeongtong` | 영통구 거래·주소 좌표·임시 기준점 기능 | `:core:domain` |
| `:app` | Compose UI, MVI, 차량 어댑터, 앱 컨테이너와 실행 | 위 세 모듈 |

모듈 간 참조는 위 방향만 허용한다. 기존 패키지명과 화면 흐름을 유지하고 SDK·AGP·Kotlin 버전을 바꾸지 않는다. 앱의 차량 SDK 호출은 `vehicle/` 패키지에 둔다.

## 결과와 영향
도메인 계층은 Android 구현에 의존하지 않고 데이터 구현은 UI를 참조하지 않는다. Room 스키마 출력 위치는 데이터 모듈을 따른다. 기능 추가 시 화면과 데이터 구현의 경계를 먼저 확인한다. 이번 변경은 모듈 경계 정리이며 실거래 연동 범위나 화면 동작을 확장하지 않는다.
