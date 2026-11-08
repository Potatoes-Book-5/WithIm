# 기여 가이드

## 처음 시작

1. [docs/SETUP.md](docs/SETUP.md)대로 개발 환경을 설치합니다.
2. git 훅을 설정합니다.

   ```bash
   ./harness/setup.sh
   ```

3. 다음 문서를 읽습니다.
   - [그라운드 룰](docs/team/ground-rules.md)
   - [Git 워크플로](docs/team/git-workflow.md)
   - [작업·ID 관리](docs/tracking/README.md)
   - [AI 협업 규칙](docs/team/ai-collaboration.md)

## 작업 흐름

1. `./harness/new-id.sh`로 T-ID를 등록하고 등록 PR을 먼저 머지합니다.
2. `<type>/T-NNN-<slug>` 브랜치를 만듭니다.
3. 구현하고 `./harness/verify.sh`를 통과시킵니다.
4. PR을 올리고 리뷰를 받습니다.
5. Squash merge합니다.
