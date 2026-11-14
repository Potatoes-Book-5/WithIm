# 디자인·UI

Figma 디자인과 화면 가이드의 입력 문서입니다. [Pleos Connect UI Design Guide](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro)를 기준으로 합니다.

## 작업 순서

1. 공식 Figma 리소스를 받아 팀 Figma에 연결합니다(T-002).
2. Color와 Typography 토큰을 `tokens/`에 추출합니다.
3. `flows/navigation-map.md`로 화면 흐름을 확정합니다.
4. `screens/SCR-NNN-*.md`를 작성합니다(T-011).
5. [figma-handoff.md](figma-handoff.md)의 명명 규칙대로 프레임을 만듭니다.
6. 프레임 링크를 SCR 문서에 기록합니다.

## 공식 리소스

- Pleos Connect display guide (.fig)
- Pleos Connect design assets (.fig)

다운로드 위치는 UI Design Guide의 Design Resources Download 항목입니다.

## Figma 파일

| 파일 | 링크 | 담당 |
|---|---|---|
| 팀 디자인 파일 | 미정 | - |
| 와이어프레임 화면 가이드(캔버스, 비공개) | https://claude.ai/artifact/2KA9gccXWjpw8CVjeGRBBZ | - |
| 와이어프레임 원본 SVG | [wireframes/](wireframes/README.md) | - |

## R-UI — Gleo 임장 공통 UI 제안

light 기본 테마, 플랫폼 창 크기 변화 대응, 정적 왕관, 정적 뉴스 카드, 명확한 단위/기준일을 사용한다. 시스템 Driving view를 덮는 오버레이를 만들지 않는다. 모든 인과성은 색상만으로 전달하지 않는다. 로딩·빈 데이터·부분 실패·권한 없음·미지원·오프라인 상태를 구분한다.

세부 화면은 SCR-002, SCR-004, SCR-005, [SCR-011](screens/SCR-011-interest-area-list.md), [SCR-012](screens/SCR-012-daily-briefing.md)이다. 이 추가 명세는 기존 와이어프레임/Figma 갱신 완료를 뜻하지 않는다. 검증은 [S-011 테스트](../testing/gleo-tour/README.md)를 따른다.
