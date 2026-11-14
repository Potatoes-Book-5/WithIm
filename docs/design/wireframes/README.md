# 와이어프레임

화면 구조를 정하기 위한 초안(lo-fi)입니다. 색상과 글꼴은 공식 토큰이 확정되기 전의 임시 값입니다(T-002).

## 프레임 기준

- 크기: 2560×1440 px (Connect-L 12.9인치)
- 영역: [layout.md](../foundations/layout.md)
- 회색 영역(상태바, Driving View, 하단 독, 다른 앱 창)은 시스템 영역 참조용입니다. 디자인 대상은 흰색 앱 창입니다.

## 목록

| 파일 | Figma 프레임 이름 | 화면 문서 |
|---|---|---|
| [SCR-001_landscape_parked.svg](SCR-001_landscape_parked.svg) | `SCR-001/landscape/parked/light` | [SCR-001](../screens/SCR-001-ranking-home.md) |
| [SCR-002_landscape_parked.svg](SCR-002_landscape_parked.svg) | `SCR-002/landscape/parked/light` | [SCR-002](../screens/SCR-002-apartment-detail.md) |
| [SCR-003_landscape_parked.svg](SCR-003_landscape_parked.svg) | `SCR-003/landscape/parked/light` | [SCR-003](../screens/SCR-003-interest-conditions.md) |
| [SCR-004_portrait_driving.svg](SCR-004_portrait_driving.svg) | `SCR-004/portrait/driving/light` | [SCR-004](../screens/SCR-004-driving-simple-view.md) |
| [SCR-004_landscape_driving.svg](SCR-004_landscape_driving.svg) | `SCR-004/landscape/driving/light` | [SCR-004](../screens/SCR-004-driving-simple-view.md) |
| [SCR-005_portrait_driving.svg](SCR-005_portrait_driving.svg) | `SCR-005/portrait/driving/light` | [SCR-005](../screens/SCR-005-alert-overlay.md) |
| [SCR-005_landscape_driving.svg](SCR-005_landscape_driving.svg) | `SCR-005/landscape/driving/light` | [SCR-005](../screens/SCR-005-alert-overlay.md) |
| [SCR-006_landscape_parked.svg](SCR-006_landscape_parked.svg) | `SCR-006/landscape/parked/light` | [SCR-006](../screens/SCR-006-consent-settings.md) |
| [SCR-007_landscape_parked.svg](SCR-007_landscape_parked.svg) | `SCR-007/landscape/parked/light` | [SCR-007](../screens/SCR-007-scrap-list.md) |
| [SCR-008_landscape_parked.svg](SCR-008_landscape_parked.svg) | `SCR-008/landscape/parked/light` | [SCR-008](../screens/SCR-008-drive-summary.md) |
| [SCR-009_portrait_driving.svg](SCR-009_portrait_driving.svg) | `SCR-009/portrait/driving/light` | [SCR-009](../screens/SCR-009-driving-map-mode.md) |
| [SCR-009_landscape_driving.svg](SCR-009_landscape_driving.svg) | `SCR-009/landscape/driving/light` | [SCR-009](../screens/SCR-009-driving-map-mode.md) |
| [SCR-009_portrait_driving_fallback.svg](SCR-009_portrait_driving_fallback.svg) | 대체안(레이더), Figma 이관 보류 | [SCR-009](../screens/SCR-009-driving-map-mode.md) |
| [SCR-010_landscape_parked.svg](SCR-010_landscape_parked.svg) | `SCR-010/landscape/parked/light` | [SCR-010](../screens/SCR-010-my-page.md) |

전체 화면을 표지와 함께 한 파일로 모은 [wireframes.pdf](wireframes.pdf)도 있습니다. SVG를 고치면 PDF도 다시 만들어야 합니다.

## Figma로 가져오기

1. SVG 파일을 Figma 캔버스로 드래그합니다. 편집 가능한 벡터와 텍스트 레이어로 변환됩니다.
2. 가져온 프레임 이름을 위 표의 Figma 프레임 이름으로 바꿉니다.
3. `context (system, not app)` 그룹은 잠그거나 별도 참조 레이어로 둡니다.
4. 글꼴이 없다는 경고가 나오면 Asta Sans로 바꿉니다(TYP-001).
5. 임시 색상을 공식 토큰으로 교체합니다.

## 와이어프레임에서 정한 것

- 주행 화면은 내비게이션과 나란히 쓰는 Portrait를 기본으로 합니다.
- 주행 화면에는 `순위 | 지도` 전환 외의 버튼을 두지 않고 Top 3만 크게 표시합니다.
- 지도 모드는 Portrait 2D 지도, Landscape 2D 지도와 순위 목록으로 확정했습니다(SCR-009).
- 마이 페이지는 스크랩 단지를 내 조건과 오각형으로 비교하는 안으로 확정했습니다(SCR-010).
- 관심 조건은 한 화면에서 칩으로 선택하고, 동네는 음성 검색을 함께 제공합니다.
- 알림 카드의 버튼은 최대 1개(스크랩)이며, 주행 중 허용 여부는 T-001 답변 후 확정합니다.

## 다음 작업

- [ ] Portrait 주차 화면, 오프라인·빈 상태 프레임
- [ ] dark 테마
- [ ] 공식 컴포넌트와 토큰 적용
