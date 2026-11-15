# 문서 색인

경로는 `docs/` 기준입니다. 등록은 `harness/new-id.sh`로 합니다.

## 규칙 접두어

| 접두어 | 문서 |
|---|---|
| DD | `principles/driver-distraction.md` |
| DR | `principles/driving-restriction.md` |
| CAT | `principles/app-category.md` |
| SM | `principles/app-structure-screen-model.md` |
| PF | `principles/performance-resource.md` |
| SP | `principles/security-privacy.md` |
| HW | `platform/hardware-access.md` |
| API | `platform/sdk-api-policy.md` |
| CP | `platform/api-compatibility.md` |
| DI | `platform/driving-restriction-impl.md` |
| AF | `platform/audio-focus-impl.md` |
| DG | `design/foundations/design-principles.md` |
| CLR | `design/foundations/colors.md` |
| TYP | `design/foundations/typography.md` |
| ANM | `design/foundations/animation.md` |
| SHD | `design/foundations/shadow.md` |

## 문서

| ID | 종류 | 제목 | 상태 | 경로 | 생성 작업 | 관련 ID |
|---|---|---|---|---|---|---|
| F-001 | feature | 차량 위치 획득 | draft | `product/features/F-001-vehicle-location.md` | T-009 | S-001, S-006, SCR-001, SCR-004 |
| F-002 | feature | 현재 구 3km 시세 조회 | draft | `product/features/F-002-nearby-price-lookup.md` | T-009 | S-001, S-006, SCR-001, SCR-004 |
| F-003 | feature | 아파트 가격 순위 경쟁 | draft | `product/features/F-003-apartment-ranking.md` | T-009 | S-001, S-005, S-010, S-009, SCR-001, SCR-004, SCR-005, SCR-008 |
| F-004 | feature | 관심 조건 설정 | draft | `product/features/F-004-interest-conditions.md` | T-009 | S-002, SCR-003, SCR-006 |
| F-005 | feature | 관심 매물 근접 판정 | draft | `product/features/F-005-proximity-matching.md` | T-009 | F-010, S-003, SCR-005 |
| F-006 | feature | 알림과 TTS 안내 | draft | `product/features/F-006-tts-alert.md` | T-003, T-041 | F-010, S-003, S-004, S-010, SCR-004, SCR-005 |
| F-007 | feature | 동의와 설정 | draft | `product/features/F-007-consent-settings.md` | T-009 | S-002, S-007, SCR-006 |
| S-001 | scenario | 주차 중 주변 순위 확인 | draft | `product/scenarios/S-001-parked-browse-ranking.md` | T-010 | F-002, F-003, F-008, SCR-001, SCR-002 |
| S-002 | scenario | 첫 실행과 관심 조건 설정 | draft | `product/scenarios/S-002-parked-register-interest.md` | T-010 | F-004, F-007, SCR-006, SCR-003, SCR-001 |
| S-003 | scenario | 주행 중 관심 매물 알림 | draft | `product/scenarios/S-003-driving-match-alert.md` | T-003 | F-005, F-006, F-008, SCR-004, SCR-005 |
| S-004 | scenario | 내비·미디어 재생 중 알림 | draft | `product/scenarios/S-004-alert-during-nav-media.md` | T-010 | F-006, SCR-005 |
| S-005 | scenario | 주행 시작 전환 | draft | `product/scenarios/S-005-parked-to-driving.md` | T-010 | F-003, F-009, SCR-001, SCR-004 |
| S-006 | scenario | 오프라인·위치 미지원 | draft | `product/scenarios/S-006-offline-location-unsupported.md` | T-010 | F-001, F-002, SCR-001 |
| S-007 | scenario | 운전자 변경과 데이터 삭제 | draft | `product/scenarios/S-007-driver-change.md` | T-010 | F-007, SCR-006 |
| SCR-001 | screen | 홈 | draft | `design/screens/SCR-001-ranking-home.md` | T-011 | F-002, F-003, F-008, S-001, S-005, S-006, S-008 |
| SCR-002 | screen | 아파트 상세 | draft | `design/screens/SCR-002-apartment-detail.md` | T-011 | F-002, F-008, S-001, S-008 |
| SCR-003 | screen | 관심 조건 설정 | draft | `design/screens/SCR-003-interest-conditions.md` | T-011 | F-004, S-002 |
| SCR-004 | screen | 주행 중 순위 경쟁 | draft | `design/screens/SCR-004-driving-simple-view.md` | T-011 | F-003, F-006, S-005, S-010, S-003 |
| SCR-005 | screen | 알림 표시 | draft | `design/screens/SCR-005-alert-overlay.md` | T-011 | F-005, F-006, F-008, F-010, S-003, S-004, S-010 |
| SCR-006 | screen | 동의와 설정 | draft | `design/screens/SCR-006-consent-settings.md` | T-011 | F-007, F-004, S-002, S-007 |
| ADR-001 | decision | 주행 중 알림 범위와 앱 카테고리 | reserved | `decisions/ADR-001-driving-tts-category.md` | T-001 | F-006, S-003 |
| ADR-002 | decision | Pleos SDK 경계 격리 | draft | `decisions/ADR-002-pleos-sdk-boundary.md` | T-004 | - |
| ADR-003 | decision | Clean Architecture + MVI 채택 | approved | `decisions/ADR-003-clean-architecture-mvi.md` | T-012 | - |
| F-008 | feature | 스크랩과 변동 알림 | draft | `product/features/F-008-scrap-change-alert.md` | T-009 | S-008, S-009, SCR-001, SCR-002, SCR-007, SCR-008 |
| F-009 | feature | 주행 요약 | draft | `product/features/F-009-drive-summary.md` | T-009 | S-009, SCR-008, F-010 |
| S-008 | scenario | 앱 실행 시 스크랩 변동 확인 | draft | `product/scenarios/S-008-launch-scrap-change.md` | T-010 | F-008, SCR-001, SCR-007, SCR-002 |
| S-009 | scenario | 주행 종료 요약과 스크랩 | draft | `product/scenarios/S-009-drive-end-summary.md` | T-010 | F-009, F-008, F-003, F-010, SCR-008, SCR-007 |
| S-010 | scenario | 주행 중 대장 아파트 교체 알림 | draft | `product/scenarios/S-010-leader-change-alert.md` | T-010 | F-003, F-006, F-009, SCR-004, SCR-005 |
| SCR-007 | screen | 스크랩 목록 | draft | `design/screens/SCR-007-scrap-list.md` | T-011 | F-008, S-008, S-009 |
| SCR-008 | screen | 주행 요약 | draft | `design/screens/SCR-008-drive-summary.md` | T-011 | F-009, F-003, F-008, F-010, S-009, S-011 |
| SCR-009 | screen | 주행 중 지도 모드 | draft | `design/screens/SCR-009-driving-map-mode.md` | T-011 | F-003, F-005, S-005, S-010 |
| ADR-004 | decision | 지도 SDK 선정 | approved | `decisions/ADR-004-map-sdk.md` | T-013 | - |
| SCR-010 | screen | 마이 페이지 | draft | `design/screens/SCR-010-my-page.md` | T-011 | F-004, F-008, S-002, S-008 |
| ADR-005 | decision | 로컬 DB로 Room 채택 | approved | `decisions/ADR-005-room-local-db.md` | T-016 | - |
| ADR-006 | decision | 국토부 실거래가 HTTP 연동 | draft | `decisions/ADR-006-molit-retrofit.md` | T-020 | - |
| ADR-007 | decision | 주소 좌표 변환과 임시 기준 위치 | draft | `decisions/ADR-007-geocoding-reference.md` | T-021 | - |
| ADR-008 | decision | Gradle 모듈 경계와 의존 방향 | draft | `decisions/ADR-008-gradle-module-boundaries.md` | T-022 | - |
| ADR-009 | decision | UI 모듈 경계(designsystem, ui, feature) | approved | `decisions/ADR-009-ui-module-boundaries.md` | T-024 | - |
| SCR-011 | screen | 주차 후 관심 매물 모아보기 | draft | `design/screens/SCR-011-interest-catch-up.md` | T-026 | F-009, F-008, F-005, S-009 |
| F-010 | feature | 주행 관심 매물 추천 | draft | `product/features/F-010-drive-interest-recommendation.md` | T-037 | F-002, F-004, F-005, F-006, F-009, S-011, SCR-008 |
| S-011 | scenario | 주행 추천 수집과 종료 요약 | draft | `product/scenarios/S-011-drive-recommendations-summary.md` | T-037 | F-010, S-003, S-009, SCR-008 |
| F-011 | feature | Gleo 음성 앱 제어 | draft | `product/features/F-011-gleo-app-control.md` | T-040 | - |
| F-012 | feature | 관심 지역 저장 | draft | `product/features/F-012-interest-area-booking.md` | T-040 | - |
| F-013 | feature | 아파트 내비게이션 | draft | `product/features/F-013-apartment-navigation.md` | T-040 | - |
| F-014 | feature | 지역 데일리 브리핑 | draft | `product/features/F-014-area-daily-briefing.md` | T-040 | - |
| F-015 | feature | 카메라 임장 스캔 | draft | `product/features/F-015-camera-site-scan.md` | T-040 | - |
| S-012 | scenario | 대화형 부동산 임장 | draft | `product/scenarios/S-012-conversational-property-tour.md` | T-040 | - |
| ADR-010 | decision | Gleo Intent adapter | draft | `decisions/ADR-010-gleo-intent-adapter.md` | T-040 | - |
| SCR-012 | screen | 관심 지역 목록 | draft | `design/screens/SCR-012-interest-area-list.md` | T-040 | - |
| SCR-013 | screen | 데일리 브리핑 | draft | `design/screens/SCR-013-daily-briefing.md` | T-040 | - |
| ADR-011 | decision | 주행 데이터 read model과 음성 조회 분리 | reserved | `decisions/ADR-011-prepared-market-read-model.md` | T-042 | - |
| ADR-012 | decision | 관심 지역과 브리핑 로컬 저장 및 공급자 경계 | draft | `decisions/ADR-012-tour-area-briefing.md` | T-045 | - |
