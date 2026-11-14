# Gleo 임장 테스트 — S-011

작성일: 2026-11-14 · 실행 상태: **82개 모두 미실행**. 이 문서는 테스트 설계이며 기능 구현/지원/승인 완료를 의미하지 않는다.

## 원본과 변경 규칙

- 기능: [S-011 시나리오와 기능 지도](../../product/scenarios/S-011-conversational-property-tour.md)
- 상세 케이스 원본: [test-cases.csv](test-cases.csv). ID·선행 조건·절차·기대 결과·우선순위·환경·Gate·실제 결과·증거·명세 경로를 관리한다.
- 합성 입력/기대값: [fixtures.json](fixtures.json). 실제 지역 가격/개인 위치 데이터가 아니다.
- 제품 숫자 원본: [gleo-tour-defaults](../../product/gleo-tour-defaults.md). 변경 시 관련 케이스와 fixture를 함께 검토한다.
- 동일한 테스트 상세를 별도 Markdown 표로 복사하지 않는다. 이 문서는 실행 방법과 추적 색인만 유지한다.
- TC-*와 R-*는 이 묶음 내 로컬 표기다. 저장소 전역 ID는 F/S/SCR 및 T-014로 연결한다. 상태/품질 공통 케이스의 feature_ids는 대표 통합 기능 F-010에 연결한다.
- 음성 동의어 테스트는 현재 함수 계약의 지역 질의만 다룬다. 면적/기간을 발화로 바꾸는 기능은 F-010의 미결 사항이며 현재 스키마로 검증했다고 기록하지 않는다.

## 실행 방법과 출시 Gate

- 도메인/통합: 시계·가격·위치·저장 실패·응답 순서를 주입한다. 경계는 제어 가능한 시계로 재현한다.
- Intent: 앱 처리/Callback 계약을 검증한다. Gleo 자연어 해석 E2E를 대체하지 않는다.
- Gleo E2E: 실제 등록 환경에서 발화→함수 선택→파라미터→처리→최종 음성의 사실성을 검사한다.
- Emulator/차량: UX 제한·창 크기·오디오·SDK 지원·생명주기를 검사한다.
- Gate는 실제 API/권한/지원 증거를 확보하기 전 BLOCKED로 기록하고 해당 기능을 비활성 유지한다. 기본 케이스에서 미지원 기능 비활성도 검사한다.
- CSV의 status는 미실행으로 시작한다. 실제 수행 후 PASS/FAIL/BLOCKED와 actual_result/evidence를 기록한다. 명세 검사는 앱 케이스 PASS가 아니다.
- P0 65개, P1 17개. 이 중 확장 활성화 Gate 5개. 활성화 범위의 P0 실패는 출시 차단이다.

## 영역별 색인

| 영역 | 케이스 | 개수 |
|---|---|---:|
| 공통 상태 | TC-001~TC-008 | 8 |
| 가격 질의 | TC-009~TC-020 | 12 |
| 지역 저장 | TC-021~TC-028 | 8 |
| 지역 진입 | TC-029~TC-036 | 8 |
| 가격·추천 순위 | TC-037~TC-042 | 6 |
| 심층 분석 | TC-043~TC-048 | 6 |
| 목적지 | TC-049~TC-053 | 5 |
| 카메라 | TC-054~TC-057 | 4 |
| 데일리 브리핑 | TC-058~TC-067 | 10 |
| Gleo·오디오 | TC-068~TC-074 | 7 |
| 개인정보·생명주기 | TC-075~TC-080 | 6 |
| 성능·전체 여정 | TC-081~TC-082 | 2 |

## 요구사항 소유 문서

| 로컬 요구사항 | 단일 기준 문서 | 케이스 |
|---|---|---|
| R-01 | [F-010-gleo-app-control](../../product/features/F-010-gleo-app-control.md), [F-002-nearby-price-lookup](../../product/features/F-002-nearby-price-lookup.md) | TC-009, TC-010, TC-011, TC-013, TC-014, TC-015, TC-016, TC-017, TC-018, TC-020, TC-071, TC-082 |
| R-02 | [F-011-interest-area-booking](../../product/features/F-011-interest-area-booking.md) | TC-021, TC-022, TC-023, TC-024, TC-025, TC-026, TC-027, TC-028, TC-058, TC-075, TC-082 |
| R-03 | [F-011-interest-area-booking](../../product/features/F-011-interest-area-booking.md), [F-003-apartment-ranking](../../product/features/F-003-apartment-ranking.md) | TC-029, TC-030, TC-031, TC-032, TC-033, TC-034, TC-035, TC-036, TC-037, TC-040, TC-042, TC-075, TC-082 |
| R-04 | [SCR-002-apartment-detail](../../design/screens/SCR-002-apartment-detail.md), [F-002-nearby-price-lookup](../../product/features/F-002-nearby-price-lookup.md) | TC-001, TC-002, TC-003, TC-005, TC-043, TC-044, TC-045, TC-046, TC-047, TC-048, TC-082 |
| R-05 | [F-012-apartment-navigation](../../product/features/F-012-apartment-navigation.md), [F-014-camera-site-scan](../../product/features/F-014-camera-site-scan.md) | TC-049, TC-050, TC-051, TC-052, TC-053, TC-054, TC-055, TC-056, TC-057, TC-082 |
| R-06 | [F-013-area-daily-briefing](../../product/features/F-013-area-daily-briefing.md) | TC-019, TC-058, TC-059, TC-060, TC-061, TC-062, TC-063, TC-064, TC-065, TC-066, TC-067, TC-082 |
| R-MODE | [feature-state-matrix](../../product/feature-state-matrix.md) | TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-007, TC-008, TC-028, TC-047, TC-054, TC-056 |
| R-PRICE | [F-002-nearby-price-lookup](../../product/features/F-002-nearby-price-lookup.md), [F-003-apartment-ranking](../../product/features/F-003-apartment-ranking.md), [F-004-interest-conditions](../../product/features/F-004-interest-conditions.md) | TC-009, TC-011, TC-012, TC-013, TC-017, TC-019, TC-020, TC-037, TC-038, TC-039, TC-040, TC-041, TC-044, TC-060, TC-074 |
| R-CONTEXT | [F-010-gleo-app-control](../../product/features/F-010-gleo-app-control.md) | TC-010, TC-014, TC-015, TC-018, TC-021, TC-022, TC-023, TC-069, TC-078 |
| R-GLEO | [F-010-gleo-app-control](../../product/features/F-010-gleo-app-control.md), [gleo-integration](../../platform/gleo-integration.md) | TC-026, TC-035, TC-065, TC-068, TC-069, TC-070, TC-071, TC-072, TC-074 |
| R-AUDIO | [F-006-tts-alert](../../product/features/F-006-tts-alert.md) | TC-034, TC-072, TC-073 |
| R-PRIVACY | [F-007-consent-settings](../../product/features/F-007-consent-settings.md) | TC-016, TC-027, TC-033, TC-057, TC-064, TC-075, TC-076, TC-077, TC-080 |
| R-LIFECYCLE | [gleo-integration](../../platform/gleo-integration.md) | TC-032, TC-036, TC-066, TC-078, TC-079, TC-080 |
| R-UI | [README](../../design/README.md) | TC-007, TC-041, TC-042, TC-043, TC-046, TC-048, TC-067, TC-081 |
| R-PERF | [QUALITY](../../QUALITY.md) | TC-004, TC-081 |

## 합성 fixture와 별도 변형

- BASE 5건 평균은 12억 원, 최고 거래/왕관은 C 15.5억이다. 추천은 예산 필터 후 B/A다. 숫자/산식 원본은 fixture와 기능 문서에 있다.
- EXCLUDED는 면적/취소/거래종류/기간/지역 필터 검증용이다.
- 경계·과거 고가 거래·공백·중복/정정·지연·오류 케이스는 각 CSV 절차대로 BASE의 독립 변형을 만든다. 모든 변형을 BASE에 누적하지 않는다.
- Gleo·카메라·시동·백그라운드의 실제 지원 여부는 fixture로 증명할 수 없다.

## 검증 기록

문서 검사와 CSV/JSON/합성 산술 정합성만 수행한다. 실행 명령·결과·리뷰는 [T-014 계획](../../plans/active/T-014-gleo-spec-integration.md)에 기록한다. 앱 코드가 없으므로 L1/L2/L3 또는 실제 Gleo/차량 E2E를 실행했다고 기록하지 않는다.
