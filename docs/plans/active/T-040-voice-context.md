---
task: T-040
---
# T-040 Nudge and app-state voice context

## 목표
SCR-005의 ‘스크랩해 줘’ Nudge와 현재 상세 화면을 신뢰 가능한 실행 context로 연결한다. 수신 시 context를 동결하고 단지 조회→멱등 스크랩→배너 반영→callback까지 구현한다.

## 범위 밖
Gleo에 runtime context를 전송하는 미확인 API, 자체 STT/LLM, 운영 release 공개 receiver, 실거래 평균·지역 저장·뉴스 provider 교체. 기존 세 API의 dummy 동작은 유지한다.

## 작업 체크리스트
- [x] Nudge·화면·foreground·동의·세션 snapshot과 대상 확정 규칙
- [x] 스크랩용 별도 action, 조회 및 멱등 저장, callback
- [x] UI Nudge 발행/만료·저장 관찰·앱 화면 연결
- [x] 단위·기기 E2E·release 검증과 독립 리뷰

## 검증 방법
harness verify --device, context 정책 JVM 테스트, 실제 Room을 통한 음성 스크랩 계측, release 빌드·컴파일.

## 결정 로그
- SCR-005에 음성 스크랩 Nudge가 명시됨을 재확인했다. 지역 저장과 단지 스크랩을 혼용하지 않는다.
- schema 최대 3개를 유지: 가격·단지 스크랩·브리핑. SAVE_INTEREST_AREA는 debug 호환 진입만 유지하며 schema에서 제외한다.
- 배너가 떠 있는 동안만 Nudge 유효. 명시된 상세 대상이 있으면 상세 대상, 그렇지 않으면 보이는 Nudge를 사용한다. 주행 화면 뒤에 가려진 상세는 후보가 아니다. 불명확/만료/백그라운드는 asking/fail이며 마지막 대장을 추측하지 않는다.
- 앱 메모리의 context는 Gleo 대화 메모리가 아니다. 앱은 이미 해석된 action을 받으며 Nudge가 생겼다고 Gleo에 발화/청취를 강제하지 않는다.
- 등록 선병합을 기다리지 않고 사용자가 승인한 구현 범위에서 같은 브랜치에 등록·구현 커밋을 제공한다. main 직접 push는 하지 않는다.

## 진행 기록
2026-11-15: 기존 검증된 통합을 1103e87로 커밋. T-040 등록 후 구현 시작.

2026-11-15 검증 완료: JVM 141개(core/domain 21, core/data 18, app 94, feature/driving 8), Android 계측 16개(기존 Gleo 8 + 신규 Intent/Room 스크랩 1 포함) 통과. 최종 전체 기기 로그 harness/runs/20261115-010657-device.log. assembleRelease와 app/core:data release 단위 테스트 컴파일 통과. debug 전용 소스 폴더에서 누락됐던 adapter 테스트 3개를 공통 JVM source set으로 이동하고 fake 포트로 교체한 뒤 app:testDebugUnitTest 및 release 테스트 컴파일 재통과. 독립 리뷰에서 hidden Nudge·탭 경합·초기 저장 상태·응답 메타데이터·asking 문구 지적을 반영했다. GUI emulator의 QEMU hang 이후 headless emulator-5556에서 전체 검증했다. 실제 Gleo 음성·스키마 승인·주행 심사는 별도 외부 검증으로 남는다.
