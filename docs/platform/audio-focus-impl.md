# 오디오 포커스

> 확인일: 2026-11-08 · 상태: 초안. 페이지 요약을 바탕으로 작성했으며 T-006에서 원문과 대조합니다.
> 공식 문서를 복제하지 않고 요약합니다. 판단이 필요하면 원문을 우선합니다.

## 원문

- [오디오 포커스 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development)

## 요약

Pleos Connect에서는 오디오 포커스 변경 이벤트 처리를 구현해야 합니다. 세부 정책은 Interaction Matrix v1.0(2026-01-28)을 따릅니다.

## 규칙

강도(MUST/SHOULD)는 원문 표현을 바탕으로 팀이 해석한 값입니다.

| ID | 강도 | 규칙 |
|---|---|---|
| AF-001 | MUST | 포커스 획득·상실 이벤트를 처리한다. |
| AF-002 | SHOULD | `GAIN`은 재생, `LOSS`는 즉시 중단, `LOSS_TRANSIENT`는 일시정지, `LOSS_TRANSIENT_CAN_DUCK`은 볼륨 감쇄로 매핑한다. |
| AF-003 | MUST | Gleo 호출로 `LOSS_TRANSIENT`를 받으면 재생을 중단하고 필요 시 포커스를 반납한다. |
| AF-004 | SHOULD | Duck 적용 중 일시정지하면 동시 재생 조건이 깨질 수 있으므로 주의한다. |
| AF-005 | SHOULD | 포커스 정책 설정값을 하드코딩하지 않는다. |

## 이 앱에 적용

- 매물 알림은 내비게이션 안내보다 낮은 우선순위로 설계합니다(S-004).
- 포커스를 잃으면 알림을 다시 재생할지 폐기할지 F-006에서 정합니다.

## 원문에 없는 항목

- TTS 전용 포커스 유형과 AudioAttributes usage
- TTS 선행 묵음 적용 기준(별도 가이드 확인 필요)
