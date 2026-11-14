# 기능별 SDK 사용 맵

의존성 추가 전에 이 표를 먼저 갱신합니다. `미확정` 항목은 레퍼런스 확인 후 채웁니다.

| 기능 | 사용 API | 지원 확인 | 미지원 시 동작 | 관련 규칙 | 상태 |
|---|---|---|---|---|---|
| F-001 차량 위치 | FusedLocation SDK | `isSupported` | S-006 | CP-004 | 미확정 |
| F-006 TTS 알림 | TextToSpeech SDK | 전체 호환 | - | CP-003, AF-001 | 미확정 |
| 주행 규제 상태 | `CarUxRestrictionsManager` | - | 미확정 | DI-001 | 미확정 |
| 음성 입력 | SpeechToText SDK | 전체 호환 | 프리셋 입력 | CP-003, DD-012 | 미확정 |

## S-011의 문서 확인과 설치 검증

| 기능 | 공식 문서에서 확인한 경로 | 실제 환경 검증/대체 | 상태 |
|---|---|---|---|
| F-010 Gleo | App Schema, Intent, Callback | wire format/등록·승인·자연어 E2E 필요 | 문서 확인, 연동 미실행 |
| F-011 지역 진입 | FusedLocation addProximityAlert, isSupported | 원형/동 경계 차이, 종료 후 전달·재등록 확인 | 문서 확인, 설치 미검증 |
| F-012 내비 | NaviHelper 목적지 요청·상태 이벤트 | 출입구/권한/실제 안내 시작 확인. 실패 시 주소 제공 | 문서 확인, 설치 미검증 |
| F-013 자동 브리핑 | 시동/자동 발화 API는 확인하지 못함 | 요청형 앱/Gleo 경로 사용, 자동형 비활성 | 미확정 |
| F-014 카메라 | 외부 앱 표준 스트림/녹화 API 미확인 | 직접 접근 금지, MVP 비활성 | 미확정 |

출처·확인일·현재 Emulator와의 버전 차이는 [gleo-integration](gleo-integration.md)을 따른다. 이 표가 기존 SDK 환경 변경이나 권한 승인을 의미하지 않는다.
