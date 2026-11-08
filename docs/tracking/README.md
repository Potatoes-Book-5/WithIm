# 작업·ID 관리

모든 작업과 문서 ID는 이 폴더에서 발급합니다. **`main`에 머지된 ID만 유효합니다.**

| 파일 | 역할 |
|---|---|
| [tasks.md](tasks.md) | 작업과 todo. T-ID 발급 |
| [index.md](index.md) | 기능, 시나리오, 화면, 결정 색인. 문서 ID 발급 |

## 등록 절차

1. 최신 `main`에서 시작합니다.
2. 스크립트로 ID를 발급합니다.

   ```bash
   ./harness/new-id.sh T docs "TTS 문구 정리" P1
   ./harness/new-id.sh F "음성 명령 조회" voice-query T-012
   ```

3. `chore(tracking): register T-012`로 작은 PR을 만들어 먼저 머지합니다.
4. 머지 후 `feat/T-012-<slug>` 브랜치에서 작업합니다.
5. 작업 PR에서 상태를 함께 갱신합니다.

같은 번호를 동시에 잡으면 파일 끝에서 git 충돌이 납니다. 늦게 머지하는 쪽이 최신 `main`을 받아 다음 번호로 다시 발급합니다.

## 상태값

- **작업:** `todo` `doing` `review` `done` `blocked` `dropped`
- **문서:** `reserved` `draft` `approved` `deprecated`

## 규칙

- 행을 삭제하지 않고, 폐기된 ID를 재사용하지 않습니다.
- 작업 담당자와 문서 상태는 이 폴더에만 기록합니다.
- 문서 frontmatter에는 `id`와 `task`만 둡니다.
- 작업 유형: `feature` `fix` `docs` `design` `research` `debt` `harness`
- 우선순위: `P0` `P1` `P2` `P3`
- 표 아래에 다른 내용을 추가하지 않습니다. 스크립트가 파일 끝에 행을 추가합니다.

## ID 형식

| 대상 | 형식 | 파일 위치 |
|---|---|---|
| 작업 | `T-NNN` | `tasks.md`, 복잡한 작업은 `plans/active/T-NNN-*.md` |
| 기능 | `F-NNN` | `product/features/` |
| 시나리오 | `S-NNN` | `product/scenarios/` |
| 화면 | `SCR-NNN` | `design/screens/` |
| 결정 | `ADR-NNN` | `decisions/` |
| 공식 규칙 | `<접두어>-NNN` | 접두어별 문서 |
