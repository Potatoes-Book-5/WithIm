---
name: commit
description: 커밋 메시지를 작성하거나 팀 규칙에 맞게 커밋할 때 사용한다.
---

1. `docs/team/git-workflow.md`를 읽습니다.
2. `git status`와 `git diff`로 변경을 확인합니다. 키, `local.properties`, 위치 데이터가 없는지 봅니다.
3. 한 목적의 변경만 스테이징합니다.
4. `<type>(<scope>): <summary in English>` 형식과 `Refs: T-NNN`으로 메시지를 작성합니다. 제목과 본문은 모두 영어로 쓰고, 본문은 `- `로 시작하는 항목을 한 줄에 하나씩 작성하며, `Co-Authored-By` 등 AI 표기는 넣지 않습니다.
5. 사용자가 요청했을 때만 커밋합니다.
