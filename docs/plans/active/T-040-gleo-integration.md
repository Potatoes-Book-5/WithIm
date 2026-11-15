---
task: T-040
---
# T-040 Gleo integration
## 목표
최신 main의 모듈 구조에 Gleo adapter, debug schema, 공식 callback 경로와 dummy API를 통합한다.
## 범위 밖
실거래 평균, 지역 영속 저장, 뉴스 provider, 운영 release 공개 receiver. dummy 합성 결과만 제공한다.
## 작업 체크리스트
- [x] 최신 main에서 시작, ID 충돌 재등록
- [x] domain/data/vehicle 및 variant DI 경계 유지
- [x] debug assets schema 및 공식 response action 연결
- [x] 개별 extra / ACTION_PARAM 명시적 처리와 혼합 거부
- [x] 문서·빌드·JVM·기기·release 확인
- [x] 독립 리뷰
## 검증 방법
harness verify, core domain JVM test, debug 계측 wire E2E, release manifest/schema 부재.
## 결정 로그
사용자가 전체 통합 구현을 요청했다. 기존 branch ID와 main ID 충돌을 신규 ID로 이관하고 같은 변경에서 제공한다. 등록 선병합 대기 없이 작업하며 main 직접 push는 하지 않는다.
## 진행 기록
2026-11-15: main 88583a0 기반 구현 시작. 실 Gleo 음성 E2E는 기기 서비스·schema 승인 여부에 의존한다.

2026-11-15 완료: verify --device 전체 통과(로그 harness/runs/20261115-004408-device.log). JVM 133개, 앱 계측 15개 중 Gleo 8개 통과. assembleRelease 및 app/core:data release unit test compile 통과. APK schema 및 manifest receiver debug/release 분리 확인. 독립 리뷰 5항목 수정 후 차단 문제 없음. 기존 LiveMarketRepository 동일 LogTagMismatch 오탐에 함수 한정 억제 추가. 공식 implicit callback의 테스트 수신기는 EXPORTED로 등록. 기존 emulator 종료 후 read-only emulator-5556에서 재검증했다. 실제 Gleo 음성·schema 승인 검증은 미수행.

2026-11-15 문서 보강: 사용자 요청으로 STEP 3 요청형 지역 확인을 F-011 R-AREA-CHECK에 추가. S-012, F-012, 상태 매트릭스, SCR-009, 외부/내부 API 확장안 및 TC-083~TC-090을 연결했다. GET_AREA_OVERVIEW는 제안이며 실행 schema/API 변경은 이번 범위 밖이다. 다른 음성 context 작업 트리 변경은 보존한다. 제품 테스트는 총 90개이며 추가 8개는 미실행.

문서 보강 검증: verify --docs 및 diff --check 통과, CSV 90행·ID 유일성·열 정합성·미실행 상태 확인. 로그 harness/runs/20261115-010111-docs.log. 코드/기기 테스트는 문서 변경 범위에서 재실행하지 않았다.
