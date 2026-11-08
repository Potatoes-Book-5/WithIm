# 품질 기준

## 검증 레벨

| 레벨 | 실행 | 내용 | 실행 주체 |
|---|---|---|---|
| Docs | `./harness/verify.sh --docs` | 문서 링크, ID 정합성 | 에이전트, CI |
| L1 | `./harness/verify.sh` | Docs + `assembleDebug`, `testDebugUnitTest`, `lintDebug` | 에이전트, CI |
| L2 | `./harness/verify.sh --device` | L1 + `installDebug`, `connectedDebugAndroidTest` | 에뮬레이터 실행 시 |
| L3 | 수동 | Pleos 에뮬레이터 TTS, 주행 전환, CPU·메모리 측정 | 사람 |

## 완료 기준

- [ ] 인수 조건을 모두 충족
- [ ] 해당 레벨의 검증 통과
- [ ] 새 도메인 로직에 단위 테스트 추가
- [ ] 관련 문서와 [준수 매트릭스](principles/compliance-matrix.md) 갱신
- [ ] [tasks.md](tracking/tasks.md) 상태 갱신

## L3 측정 항목

| 항목 | 기준 | 규칙 |
|---|---|---|
| 주행 중 CPU | 5% 미만 | PF-002 |
| 정차 중 CPU | 25% 이하 | PF-003 |
| RAM | 400 MB 이하 | PF-004 |
| 입력 응답 | 0.25초 이내 | DG-005 |
| 대비 | 4.5:1 이상 | DG-006 |
