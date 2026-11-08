# 아키텍처

## 모듈

단일 `app` 모듈을 유지합니다. 빌드 시간이나 팀 경계 문제가 생기면 분리를 다시 검토합니다.

## 패키지와 의존 방향

```
ui ──────▶ domain ◀────── data
                  ◀────── vehicle
```

| 패키지 | 책임 | 규칙 |
|---|---|---|
| `ui` | Compose 화면, 상태 표시 | SDK를 직접 호출하지 않음 |
| `domain` | 순위 계산, 조건 판정, 알림 정책 | Android와 Pleos 의존 없음 |
| `data` | 시세 API, 캐시, 저장소 | `domain` 인터페이스 구현 |
| `vehicle` | Pleos SDK, AAOS API 호출 | SDK 의존은 이 패키지에만 존재 |

결정 배경은 [ADR-002](decisions/ADR-002-pleos-sdk-boundary.md)에 있습니다.

## 저장소 구조

```
King-of-real-estate/
├── AGENTS.md, CLAUDE.md, CONTRIBUTING.md
├── .claude/          # 공용 스킬, 에이전트, 권한
├── .github/          # PR·이슈 템플릿, CODEOWNERS
├── harness/          # 검증, ID 발급, git 훅
├── app/              # Android 앱
├── gradle/libs.versions.toml
└── docs/
    ├── tracking/     # 작업과 ID 색인
    ├── team/         # 협업 규칙
    ├── product/      # 기능, 시나리오
    ├── principles/   # Pleos 설계 원칙
    ├── platform/     # SDK·API 정책
    ├── design/       # Figma 입력 문서
    ├── decisions/    # ADR
    ├── plans/        # 실행 계획
    ├── references/   # 외부 자료
    └── learnings/    # 실패와 개선 기록
```

## 현재 상태

`MainActivity`의 Hello World와 테마만 있습니다. 패키지는 기능 구현 시 추가합니다.
