# 기능별 SDK 사용 맵

의존성 추가 전에 이 표를 먼저 갱신합니다. `미확정` 항목은 레퍼런스 확인 후 채웁니다.

| 기능 | 사용 API | 지원 확인 | 미지원 시 동작 | 관련 규칙 | 상태 |
|---|---|---|---|---|---|
| F-001 차량 위치 | FusedLocation SDK 1.1.3(`ai.pleos.playground:FusedLocation`) | `isSupported` | 위치 입력 UNSUPPORTED, 안내 화면 S-006은 미구현 | CP-004 | 사용 중(T-029) |
| F-006 TTS 알림 | TextToSpeech SDK | 전체 호환 | - | CP-003, AF-001 | 미확정 |
| 주행 규제 상태 | `CarUxRestrictionsManager` | - | 미확정 | DI-001 | 미확정 |
| SCR-009 주행 지도 | 네이버 지도 Android SDK(외부, [ADR-004](../decisions/ADR-004-map-sdk.md)) | 클라이언트 ID 유무 | 지도 사용 불가 상태 표시 | API-001 | 사용 중 |
| 음성 입력 | SpeechToText SDK | 전체 호환 | 프리셋 입력 | CP-003, DD-012 | 미확정 |

T-027은 SDK 위치 획득을 구현하지 않고 [차량 위치 입력](location-input.md)으로 받은 위치 상태를 처리한다. Pleos FusedLocation 콜백은 T-029가 그 입력에 연결한다. 개발 빌드는 개발용 경로 입력도 제공한다.

## FusedLocation SDK(T-029)

> 확인일: 2026-11-14. 원문: Pleos Connect Developer Guide > Using SDKs > FusedLocation SDK(Importing FusedLocation SDK, Declaring Permission, Initialization / Release, Location Data Access, Error Handling, API Reference)

| 항목 | 문서 내용 | 앱 적용 |
|---|---|---|
| 의존성 | `ai.pleos.playground:FusedLocation`, 예시 버전 1.1.2 | 1.1.2가 저장소에 없어 저장소 최신 1.1.3 사용(저장소 버전 1.0.4, 1.1.3) |
| 권한 | `pleos.car.permission.FUSED_LOCATION`(dangerous). `getLastKnownLocation`, `register/unregisterFusedLocationCallback`에 필요 | 매니페스트 선언, SCR-006에서 위치 동의 저장 후 런타임 요청. 권한이 없으면 SDK를 열지 않고 PERMISSION_REQUIRED |
| 생명주기 | `isSupported()` → `initialize()` → 위치 API → `unregisterFusedLocationCallback()` → `release()`. `release()` 뒤 같은 인스턴스 재사용 금지 | 동의·권한이 생길 때마다 새 인스턴스(`vehicle/location/PleosFusedLocationBridge`) |
| `isSupported()` | 위치 서비스 패키지가 있는지만 확인. 초기화·서비스 연결과 무관 | false면 초기화하지 않고 UNSUPPORTED |
| 위치 수신 | `FusedLocationListener.onChanged(LocationData?)`, `onError(Exception)`. null은 유효한 위치가 아님 | null은 SEARCHING, `SecurityException`은 PERMISSION_REQUIRED, 그 밖의 오류는 SEARCHING |
| `LocationData` | `latitude`, `longitude`, `altitude: Double`, `heading`, `confidence: Float`, `timestampMillis: Long` | 위경도와 시각만 `VehicleLocationInput.updateLocation`에 넘김. heading, 정확도는 null |
| 차단 상태 | `showLocationAccessBlockedPopup()`으로 안내. 접근 권한을 주지는 않음 | 미구현 |

### 원문에 없는 항목(미결)

- `heading`, `confidence`의 단위와 기준. 진행 방향은 미확인(null, 지도 북쪽 위), 정확도는 미제공(null)으로 넘겨 100m 기준을 적용하지 않는다.
- 갱신 주기·거리 설정 API와 콜백 스레드. `timestampMillis`의 기준 시계(입력 포트는 30초가 지난 위치를 STALE로 버린다)
- `onError`로 오는 예외 종류와 재시도 기준
- 위치 서비스(`ai.pleos.playground.service.location.fused`)가 있는 에뮬레이터 이미지
