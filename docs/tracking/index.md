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
| F-001 | feature | 차량 위치 획득 | draft | `product/features/F-001-vehicle-location.md` | T-009 | S-001, S-006, SCR-001 |
| F-002 | feature | 주변 아파트 시세 조회 | draft | `product/features/F-002-nearby-price-lookup.md` | T-009 | S-001, S-006, SCR-001, SCR-002 |
| F-003 | feature | 아파트 순위 산정 | draft | `product/features/F-003-apartment-ranking.md` | T-009 | S-001, SCR-001, SCR-002 |
| F-004 | feature | 관심 부동산·조건 관리 | draft | `product/features/F-004-interest-conditions.md` | T-009 | S-002, SCR-003 |
| F-005 | feature | 근접 매물 판정 | draft | `product/features/F-005-proximity-matching.md` | T-009 | S-003, SCR-005 |
| F-006 | feature | TTS 매물 알림 | draft | `product/features/F-006-tts-alert.md` | T-003 | S-003, S-004, SCR-004, SCR-005 |
| F-007 | feature | 동의와 설정 | draft | `product/features/F-007-consent-settings.md` | T-009 | S-007, SCR-006 |
| S-001 | scenario | 주차 중 주변 순위 조회 | draft | `product/scenarios/S-001-parked-browse-ranking.md` | T-010 | F-001, F-002, F-003, SCR-001, SCR-002 |
| S-002 | scenario | 주차 중 관심 조건 등록 | draft | `product/scenarios/S-002-parked-register-interest.md` | T-010 | F-004, SCR-003 |
| S-003 | scenario | 주행 중 관심 매물 알림 | draft | `product/scenarios/S-003-driving-match-alert.md` | T-003 | F-005, F-006, SCR-004, SCR-005 |
| S-004 | scenario | 내비·미디어 재생 중 알림 | draft | `product/scenarios/S-004-alert-during-nav-media.md` | T-010 | F-006, SCR-005 |
| S-005 | scenario | 사용 중 주행 전환 | draft | `product/scenarios/S-005-parked-to-driving.md` | T-010 | F-002, F-003, SCR-001, SCR-004 |
| S-006 | scenario | 오프라인·위치 미지원 | draft | `product/scenarios/S-006-offline-location-unsupported.md` | T-010 | F-001, F-002, SCR-001 |
| S-007 | scenario | 운전자 변경 | draft | `product/scenarios/S-007-driver-change.md` | T-010 | F-007, SCR-006 |
| SCR-001 | screen | 주변 순위 홈 | draft | `design/screens/SCR-001-ranking-home.md` | T-011 | F-001, F-002, F-003, S-001, S-005, S-006 |
| SCR-002 | screen | 아파트 상세 | draft | `design/screens/SCR-002-apartment-detail.md` | T-011 | F-002, F-003, S-001 |
| SCR-003 | screen | 관심 조건 | draft | `design/screens/SCR-003-interest-conditions.md` | T-011 | F-004, S-002 |
| SCR-004 | screen | 주행 중 간소 화면 | draft | `design/screens/SCR-004-driving-simple-view.md` | T-011 | F-006, S-003, S-005 |
| SCR-005 | screen | 알림 표시 | draft | `design/screens/SCR-005-alert-overlay.md` | T-011 | F-005, F-006, S-003, S-004 |
| SCR-006 | screen | 동의와 설정 | draft | `design/screens/SCR-006-consent-settings.md` | T-011 | F-007, S-007 |
| ADR-001 | decision | 주행 중 알림 범위와 앱 카테고리 | reserved | `decisions/ADR-001-driving-tts-category.md` | T-001 | F-006, S-003 |
| ADR-002 | decision | Pleos SDK 경계 격리 | draft | `decisions/ADR-002-pleos-sdk-boundary.md` | T-004 | - |
