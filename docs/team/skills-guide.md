# 공용 스킬 가이드

공용 스킬은 `.claude/skills/<이름>/SKILL.md`에 있으며 git으로 공유합니다. 개인 실험용 스킬은 `~/.claude/skills/`에 둡니다.

## 목록

| 스킬 | 용도 | 루프 단계 |
|---|---|---|
| `register` | 작업 등록과 ID 예약 | 시작 |
| `spec` | 기능·시나리오 문서 작성 | 명세 |
| `screen-spec` | SCR 문서, Figma 프레임 목록 작성 | 명세 |
| `exec-plan` | 실행 계획 작성 | 계획 |
| `implement` | 계획 기반 구현 | 구현 |
| `verify` | 검증 실행과 결과 해석 | 검증 |
| `pleos-check` | 공식 규칙 준수 점검 | 검증 |
| `commit` | 커밋 규칙에 맞춰 커밋 | 전달 |
| `pr` | PR 작성 | 전달 |
| `retro` | 실패를 learnings로 기록 | 학습 |

`plan` 대신 `exec-plan`을 쓰는 이유는 Claude Code의 내장 `/plan`과 겹치지 않게 하기 위해서입니다.

## 작성·변경 규칙

- 스킬 변경은 PR로만 합니다.
- 규칙을 스킬에 복사하지 말고 `docs/` 문서를 참조합니다.
- `description`에 언제 사용하는 스킬인지 구체적으로 적습니다.
- 사람의 승인이 필요한 단계는 스킬에 명시합니다.
- 한 스킬은 한 가지 일만 합니다.
