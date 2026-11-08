# Git 워크플로

## 브랜치

- `main`은 보호하며 직접 푸시하지 않습니다.
- 형식: `<type>/T-NNN-<slug>` (예: `feat/T-012-tts-alert`)

## 커밋

```
<type>(<scope>): <summary in English>

- <change or reason, in English>
- <change or reason, in English>

Refs: T-NNN, F-NNN
```

| type | 용도 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 |
| `design` | 디자인 토큰, 화면 명세 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `test` | 테스트 |
| `chore` | 빌드, 설정, 프로젝트 초기 구성, ID 등록 |
| `harness` | 검증 스크립트, 스킬, 에이전트 설정 |

- scope는 기능 ID 또는 영역입니다. 예: `F-006`, `S-003`, `SCR-005`, `harness`, `tracking`
- **커밋 메시지의 제목과 본문은 영어로 작성합니다.** 사람과 에이전트 모두 적용하며, PR 제목도 같습니다.
- 요약은 명령형 현재 시제로 72자 이내로 쓰고 끝에 마침표를 찍지 않습니다. 예: `add`, `fix`, `update`
- 본문은 `- `로 시작하는 항목으로 쓰고, 항목 하나를 한 줄에 작성합니다. 줄을 바꿔 이어 쓰지 않습니다.
- `Refs:`에는 T-ID가 필수입니다. `chore(tracking):` 등록 커밋만 예외입니다.
- AI가 함께 작업했더라도 `Co-Authored-By`, `Generated with` 같은 AI 표기를 넣지 않습니다. Squash merge 메시지도 같습니다.
- 형식, 본문 항목, 한글 포함 여부, AI 표기는 `harness/hooks/commit-msg`가 검사합니다. 클론 후 `./harness/setup.sh`를 실행합니다.

예시:

```
feat(F-006): add TTS alert for matching properties

- Handle audio focus so the alert does not interrupt navigation guidance
- Skip the alert when the same property was announced recently

Refs: T-012, S-004
```

## PR

- 제목은 커밋 형식을 따릅니다.
- 리뷰어 1명 이상 승인과 `verify.sh` 통과가 필요합니다(제안값).
- 화면을 변경하면 스크린샷이나 Figma 프레임 링크를 첨부합니다.
- ID 등록 PR은 번호와 제목만 확인하고 빠르게 승인합니다.

## 머지

- Squash merge를 사용하고 머지 후 브랜치를 삭제합니다.
- 충돌은 작업자가 최신 `main`을 기준으로 해결합니다.
