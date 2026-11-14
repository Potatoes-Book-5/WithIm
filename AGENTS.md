# King of Real Estate

Pleos Connect 차량 앱입니다. 차량 위치 주변 아파트 시세를 조회해 순위를 매기고, 관심 조건에 맞는 부동산이 주변에 있으면 TTS로 알립니다.

이 파일은 목차입니다. 자세한 내용은 링크된 문서를 읽습니다.

## 시작 전 필수

1. [docs/tracking/tasks.md](docs/tracking/tasks.md)에서 작업의 T-ID를 확인합니다. 없으면 `register` 스킬로 먼저 등록합니다.
2. 관련 기능, 시나리오, 화면 문서를 [docs/tracking/index.md](docs/tracking/index.md)에서 찾아 읽습니다.
3. 여러 단계 작업이면 `exec-plan` 스킬로 `docs/plans/active/`에 계획을 작성합니다.

## 명령

| 목적 | 명령 |
|---|---|
| 문서·ID 검사 | `./harness/verify.sh --docs` |
| 빌드, 단위 테스트, lint | `./harness/verify.sh` |
| 에뮬레이터 테스트 | `./harness/verify.sh --device` |
| ID 발급 | `./harness/new-id.sh` |
| 초기 설정 | `./harness/setup.sh` |

## 절대 규칙

- `local.properties`와 키를 읽거나, 출력하거나, 커밋하지 않습니다.
- 앱 코드는 Clean Architecture + MVI 규칙을 따르고, 현재 프로젝트의 SDK 환경을 변경하지 않습니다.
- SDK 호출은 `vehicle/` 패키지에만 둡니다.
- 차량 모델명으로 분기하지 않습니다. 지원 여부 API로 확인합니다.
- 주행 규제 상태에서 영상을 재생하지 않고, Driving view를 가리지 않습니다.
- 공식 문서에 없는 값을 추정해 쓰지 않고 미결로 기록합니다.
- 등록되지 않은 ID를 사용하지 않습니다.
- `main`에 직접 푸시하지 않습니다.
- 커밋 메시지는 영어로 쓰고, `Co-Authored-By` 등 AI 표기를 넣지 않습니다.

## 문서 지도

| 알고 싶은 것 | 위치 |
|---|---|
| 작업과 ID | [docs/tracking/](docs/tracking/README.md) |
| 협업, 커밋, PR 규칙 | [docs/team/](docs/team/ground-rules.md) |
| 기능과 시나리오 | [docs/product/](docs/product/overview.md) |
| 주행 중 기능 허용 범위 | [feature-state-matrix.md](docs/product/feature-state-matrix.md) |
| Pleos 설계 원칙 | [docs/principles/](docs/principles/README.md) |
| SDK·API 정책 | [docs/platform/](docs/platform/README.md) |
| 디자인, 화면 명세 | [docs/design/](docs/design/README.md) |
| 앱 아키텍처(Clean + MVI), SDK 환경 | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| 완료 기준과 검증 | [docs/QUALITY.md](docs/QUALITY.md) |
| 시나리오별 테스트 설계 | [docs/testing/](docs/testing/README.md) |
| 개발 환경 | [docs/SETUP.md](docs/SETUP.md) |
| 결정 기록 | [docs/decisions/](docs/decisions/README.md) |
| AI 협업 규칙 | [docs/team/ai-collaboration.md](docs/team/ai-collaboration.md) |

## 작업 루프

`등록 → 명세 → 계획 → 구현 → 검증 → 리뷰 → 학습`

스킬 목록은 [docs/team/skills-guide.md](docs/team/skills-guide.md)에 있습니다.
