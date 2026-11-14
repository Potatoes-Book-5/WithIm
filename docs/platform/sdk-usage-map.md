# 기능별 SDK 사용 맵

의존성 추가 전에 이 표를 먼저 갱신합니다. `미확정` 항목은 레퍼런스 확인 후 채웁니다.

| 기능 | 사용 API | 지원 확인 | 미지원 시 동작 | 관련 규칙 | 상태 |
|---|---|---|---|---|---|
| F-001 차량 위치 | FusedLocation SDK 1.1.3(`ai.pleos.playground:FusedLocation`) | `isSupported` | 위치 입력 UNSUPPORTED, 안내 화면 S-006은 미구현 | CP-004 | 사용 중(T-029) |
| F-006 TTS 알림 | TextToSpeech SDK | 전체 호환 | - | CP-003, AF-001 | 미확정 |
| 주행 중·주행 완료(F-009, S-005, S-009) | AAOS `CarPropertyManager` `GEAR_SELECTION`(`useLibrary("android.car")`) | `getCarPropertyConfig`와 지원 기어에 P 포함 | 기어를 못 읽으면 주차 유지, 개발 빌드는 수동 전환 | API-002, CP-005, CP-006 | 사용 중(T-034) |
| 주행 규제 상태(DO 선언 앱의 화면 제한) | `CarUxRestrictionsManager`(`useLibrary("android.car")`) | `FEATURE_AUTOMOTIVE`, 차량 서비스 연결 | 기어 기준 주행 상태만 사용 | DI-001, DI-002, HW-003 | 사용 중(T-035) |
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

## 기어 상태(T-034)

> 확인일: 2026-11-14. 원문: Android 개발자 레퍼런스 `android.car.VehiclePropertyIds.GEAR_SELECTION`, `android.car.VehicleGear`, `CarPropertyManager`. Pleos 정책 API-002(속도, 기어 등 표준 데이터는 AAOS Car API)

처음에는 Pleos Vehicle SDK 2.3.2의 `Vehicle.getGear()`를 썼다. 이 SDK의 `Gear` 세 함수에는 모두 `@Deprecated("This API is deprecated, use the Google GEAR_SELECTION property.")`가 붙어 있어 AAOS 표준 속성으로 바꿨다. Pleos 원문(Vehicle SDK v2.3.1 문서)에는 `Gear` 도메인이 없다.

| 항목 | 원문 내용 | 앱 적용 |
|---|---|---|
| 속성 | `VehiclePropertyIds.GEAR_SELECTION`(289408000). 사용자가 고른 기어. 읽기 전용, 전역 영역, ON_CHANGE, Integer | `vehicle/gear/CarGearSource`에서만 읽는다 |
| 값 | `VehicleGear`: `GEAR_UNKNOWN`(0), `GEAR_NEUTRAL`(1), `GEAR_REVERSE`(2), `GEAR_PARK`(4), `GEAR_DRIVE`(8), `GEAR_FIRST`~`GEAR_NINTH`(16~4096) | P → 주차, R·N·D·1~9단 → 주행 중, 그 밖의 값 → 상태 유지 |
| 권한 | normal 권한 `Car.PERMISSION_POWERTRAIN`(`android.car.permission.CAR_POWERTRAIN`) | 매니페스트 선언, 런타임 요청 없음 |
| 지원 확인 | `configArray`가 차량이 지원하는 기어 목록이다. 수동 변속기 예시에는 P가 없다 | `getCarPropertyConfig(GEAR_SELECTION)`가 없거나 목록에 P가 없으면 `Unavailable`(CP-005) |
| 연결 | `Car.createCar(context, handler, waitTimeout, listener)`, `getCarManager(Car.PROPERTY_SERVICE)` | 준비될 때마다 콜백을 다시 등록. 차량 환경(`FEATURE_AUTOMOTIVE`)이 아니면 `Unavailable`(HW-003) |
| 구독 | `registerCallback(callback, propertyId, SENSOR_RATE_ONCHANGE)`, `onChangeEvent(CarPropertyValue)`, `onErrorEvent` | 상태가 `STATUS_AVAILABLE`이 아니면 UNKNOWN, 오류 이벤트는 `Unavailable` |

### 원문에 없는 항목(미결)

- 수동 변속기 차량의 주행 완료 기준. P가 없어 지금은 기어로 판정하지 않고 대체 흐름(개발용 수동 전환)을 따른다
- 차량 서비스 재연결 때 이벤트 순서
- 주행 중(D)에는 플랫폼이 DO 미선언 앱을 `주행 중에는 사용할 수 없습니다` 화면으로 가린다. 앱은 DO를 선언했다(T-035)

## Gleo debug 통합

T-039: SDK 추가 없이 Android broadcast를 app/vehicle/gleo에 격리한다. debug assets/schema.json·GleoActionReceiver로 가격·브리핑 합성 action과 실제 단지 스크랩 action을 받고 APP_ACTION_RESPONSE로 반환한다. T-040에서 Nudge와 앱 상태의 실행 context를 연결했다. release에는 외부 진입점이 없다. 설치 버전의 schema 승인·음성 동작은 [E2E](../testing/gleo-tour/e2e.md)에서 확인한다.
