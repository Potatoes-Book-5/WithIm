# 기능별 SDK 사용 맵

의존성 추가 전에 이 표를 먼저 갱신합니다. `미확정` 항목은 레퍼런스 확인 후 채웁니다.

| 기능 | 사용 API | 지원 확인 | 미지원 시 동작 | 관련 규칙 | 상태 |
|---|---|---|---|---|---|
| F-001 차량 위치 | FusedLocation SDK | `isSupported` | S-006 | CP-004 | 미확정 |
| F-006 TTS 알림 | TextToSpeech SDK | 전체 호환 | - | CP-003, AF-001 | 미확정 |
| 주행 규제 상태 | `CarUxRestrictionsManager` | - | 미확정 | DI-001 | 미확정 |
| SCR-009 주행 지도 | 네이버 지도 Android SDK(외부, [ADR-004](../decisions/ADR-004-map-sdk.md)) | 클라이언트 ID 유무 | 간이 지도 | API-001 | 사용 중 |
| 음성 입력 | SpeechToText SDK | 전체 호환 | 프리셋 입력 | CP-003, DD-012 | 미확정 |
