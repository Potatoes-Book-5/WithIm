---
task: T-041
---
# T-041 음성용 조회 스냅샷

## 목표
주행 수집과 음성 읽기를 분리한다. 네트워크·수집 Mutex 없이 준비된 단지 저장 정보와 지역 가격 집계를 읽고, 음성/배너 스크랩에서 12개월 상세·세대수 fetch를 제거한다.

## 범위 밖
가격 음성 함수의 실제 시세 전환, 관심 지역 영속 저장, 원본 거래/좌표의 Room 저장, SDK·DB 버전 변경. 기존 가격·브리핑 dummy 계약은 유지한다.

## 작업 체크리스트
- [x] immutable bounded read model·readiness·TTL
- [x] 완료된 주행 월별 거래와 상세 결과로 백그라운드 갱신
- [x] 스크랩 cache-only 읽기·cold/stale 즉시 실패
- [x] JVM 단위·경합 검증, Android 계측 APK 빌드, lint, release 및 독립 리뷰
- [ ] Pleos Connect에서 Room·Intent·수집 경합 기기 E2E 실행 완료(환경 불안정으로 미완료)

## 검증 방법
네트워크 중단/잠금 대기 중에도 스냅샷 읽기 완료, 완료 전 미게시, stale·cold·future clock 거부, 원본 평균/해제·기간·면적 필터, bounded eviction, 준비된 단지의 네트워크 없는 실제 Room 저장. 1초 목표는 측정 범위를 명시하고 플랫폼 보장으로 쓰지 않는다.

## 결정 로그
기존 geocoder 좌표 정책을 유지하고 메모리 파생 모델만 추가한다. current main fetch 후 기존 geonu 브랜치에서 T-041/ADR-011을 등록한다. 사용자 지시에 따라 선등록 문서와 구현을 단계별 커밋하며 main 직접 push는 하지 않는다.

## 진행 기록
2026-11-15: 현재 detail()의 12개월 fetch·세대수 조회·수집 Mutex 경합을 확인했다.

2026-11-15 검증: JVM 155개(core/domain 21, core/data 31, app 95, feature/driving 8) 통과. 실제 LiveMarketRepository 수집을 지오코딩에서 정지시킨 JVM 경합 테스트에서 메모리 조회 1,000회 3ms. 이 값은 Mac JVM의 조회 부분이며 전체 음성 응답/실기기 성능을 보장하지 않는다. verify 로그 harness/runs/20261115-030631-fast.log. assembleRelease, app/core:data release 테스트 컴파일, assembleDebugAndroidTest 통과. 독립 리뷰 지적(배치 교체·freshness·식별자 누락 집계·시계 역행·부분 게시 세대·후보 검증) 반영 후 추가 차단 문제 없음.

Pleos 계측은 package 서비스 부재/시간 초과와 계측 시작 정지로 완료하지 못했다. 새 Room cold/ready/eviction 케이스와 수집 경합 Android 테스트는 빌드됐으나 이번 환경에서 통과로 기록하지 않는다. 일반 Phone AVD를 대체 실행하려던 시도는 사용자의 ‘Pleos Connect만 유효’ 지시에 따라 중단하고 결과에서 제외했다. 개발 규칙과 하네스에 환경 사전 검사를 추가했다. 이후 Android 실행·계측은 Pleos Connect만 사용한다.
