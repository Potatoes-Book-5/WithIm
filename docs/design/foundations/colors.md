# Colors

> 확인일: 2026-11-08 · 상태: 초안. 페이지 요약을 바탕으로 작성했으며 T-006에서 원문과 대조합니다.
> 공식 문서를 복제하지 않고 요약합니다. 판단이 필요하면 원문을 우선합니다.

## 원문

- [Colors](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/colors)

## 요약

색상은 Basic Color와 용도별 System Color로 구성됩니다.

## 규칙

강도(MUST/SHOULD)는 원문 표현을 바탕으로 팀이 해석한 값입니다.

| ID | 강도 | 규칙 |
|---|---|---|
| CLR-001 | MUST | Static Color는 테마와 무관하게 raw 값을 유지하며 주로 Overlay 위 요소에 사용한다. |
| CLR-002 | SHOULD | 특별한 시맨틱이 없는 경우 Scale Token을 사용한다. |
| CLR-003 | SHOULD | 색상 대비로 정보 위계를 표현한다. |

## 토큰 구조

- **Basic Color:** Grayscale, Alpha Color(White Alpha, Gray Alpha), Static Color(Light, Dark)
- **System Color:** Background, Button, Controller, Climate, Dropdown, Dim, Driving, Fields, Gleo AI, Icon, Informative, Keyboard(Normal, Static), Media, Overlay, Phone, Regulation, Surface, Switch, Text

토큰 이름과 값은 페이지에 이미지로만 제공됩니다. 공식 `.fig`에서 추출해 `tokens/`에 기록합니다(T-002).

## 이 앱에 적용

- Figma와 Compose 모두 토큰만 사용하고 HEX 값을 직접 쓰지 않습니다.
- 순위 상승·하락 표현은 색상만으로 구분하지 않습니다(팀 제안값).

## 원문에 없는 항목

- 토큰 이름과 값(이미지로만 제공)
- 라이트·다크 테마 규칙
- 금지사항
