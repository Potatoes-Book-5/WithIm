---
id: ADR-005
task: T-016
---

# ADR-005 로컬 DB로 Room 채택

## 배경

스크랩(F-008)은 단지별 기준 시세, 마지막 확인 시세, 변동 내역을 기기에 저장하고 목록을 실시간으로 갱신해야 한다. 지금까지 쓴 SharedPreferences는 단순 설정값에는 충분하지만, 여러 행을 정렬·조회하고 변경을 관찰하기에는 맞지 않는다. 이후 주행 요약(F-009) 기록도 같은 저장소를 쓸 가능성이 크다.

## 선택지

1. SharedPreferences에 JSON으로 저장
2. Room(SQLite)
3. DataStore(Proto)

## 결정

2026-11-14 팀 요청으로 Room을 채택한다.

- 버전: Room 2.6.1, 코드 생성은 KSP 2.0.0-1.0.24(Kotlin 2.0.0과 호환)
- 위치: `data/local/db/`. Room 타입은 `data` 밖으로 나가지 않고, domain에는 `ScrapRepository` 포트만 노출한다.
- 스키마 파일은 `app/schemas/`에 내보내 버전 관리한다. 스키마가 바뀌면 마이그레이션을 함께 작성한다.
- 동의·관심 조건 같은 단순 설정은 기존 SharedPreferences를 유지한다.

## 결과와 영향

- 스크랩 목록을 Flow로 관찰해 홈, 목록, 상세가 같은 데이터로 바로 갱신된다.
- KSP 플러그인이 빌드에 추가된다. Kotlin을 올릴 때 KSP 버전도 함께 맞춰야 한다.
- DAO 동작은 JVM 단위 테스트가 아니라 기기 테스트(L2, `connectedDebugAndroidTest`)로 확인한다.
- 개인정보 원칙(SP-004)에 따라 스크랩에는 단지 정보만 저장하고 위치 이력은 저장하지 않는다.
