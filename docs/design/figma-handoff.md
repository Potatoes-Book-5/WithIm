# Figma 핸드오프 규칙

## 프레임 이름

`SCR-NNN/<방향>/<주행 상태>/<테마>`

| 항목 | 값 |
|---|---|
| 방향 | `portrait`, `landscape` |
| 주행 상태 | `parked`, `driving`, `offline`, `empty`, `error`, `loading` |
| 테마 | `light`, `dark` |

예: `SCR-001/portrait/parked/dark`

## 페이지 구성

| 페이지 | 내용 |
|---|---|
| Foundations | 공식 토큰 연결 |
| Components | [components/inventory.md](components/inventory.md) 기준 |
| Flows | 화면 흐름 |
| SCR-NNN | 화면별 프레임 |

## 규칙

- 색상, 글꼴, 그림자는 토큰으로만 사용합니다.
- 공식 컴포넌트를 우선 사용하고, 추가 컴포넌트는 inventory에 등록합니다.
- SCR 문서의 상태 변형과 프레임 목록을 일치시킵니다.
- 프레임을 수정하면 SCR 문서도 같은 PR에서 갱신합니다.
