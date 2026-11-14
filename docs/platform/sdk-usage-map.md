# 기능별 SDK 사용 맵

의존성 추가 전에 이 표를 먼저 갱신합니다. `미확정` 항목은 레퍼런스 확인 후 채웁니다.

| 기능 | 사용 API | 지원 확인 | 미지원 시 동작 | 관련 규칙 | 상태 |
|---|---|---|---|---|---|
| F-001 차량 위치 | FusedLocation SDK 1.1.3(`ai.pleos.playground:FusedLocation`) | `isSupported` | 위치 입력 UNSUPPORTED, 안내 화면 S-006은 미구현 | CP-004 | 사용 중(T-029) |
| F-006 TTS 알림 | TextToSpeech SDK | 전체 호환 | - | CP-003, AF-001 | 미확정 |
| 주행 중·주행 완료(F-009, S-005, S-009) | Vehicle SDK 2.3.2 `Gear`(`ai.pleos.playground:Vehicle`) | `checkXXXCapabilities` 없음. 호출 실패로 판단 | 기어를 못 읽으면 주차 유지, 개발 빌드는 수동 전환 | CP-001, CP-006, API-002 | 사용 중(T-034) |
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

## 기어 상태(T-034)

> 확인일: 2026-11-14. 원문: Pleos Connect Developer Guide > SDK 사용 가이드 > Vehicle SDK(Introduction, Callback, Permission, Error Code, API Reference `Vehicle`). 원문 기준 버전은 Vehicle SDK v2.3.1(SDK 릴리스 노트)이다.

원문의 `Vehicle` 함수 목록과 권한 표에는 기어가 없다. 아래 API 항목은 저장소(`nexus-playground.pleos.ai`)의 Vehicle SDK 2.3.2 AAR 공개 시그니처와 공식 샘플 앱(`Pleos-Playground/pleos-sample-vehicle-app`) 매니페스트에서 확인했다. 저장소에는 2.0.3, 2.3.2만 있다.

| 항목 | 출처 | 내용 | 앱 적용 |
|---|---|---|---|
| 생명주기 | 원문 | `initialize()`로 연결하고 끝나면 `release()`. 비동기 콜백이 끝나기 전에 `release()`하지 않는다 | 수집하는 동안 새 `Vehicle` 하나를 쓰고, 닫을 때 해제 |
| 도메인 객체 | AAR | `Vehicle.getGear(): Gear` | `vehicle/gear/PleosGearSource`에서만 호출 |
| 현재 값 | AAR | `getCurrentGearSelectionState(onComplete: (VehicleGear?) -> Unit, onFailed: (Exception) -> Unit)` | 기어를 받기 전까지 2초 간격 3회 |
| 구독 | 원문(패턴), AAR | `registerGearSelection(GearSelectionListener)`, `unregisterGearSelection(...)`. 리스너 `onSelected(VehicleGear?)`, `onFailed(Exception)` | 등록 후 변경마다 도메인 `GearReading`으로 전달 |
| 값 | AAR | `GEAR_UNKNOWN`, `GEAR_NEUTRAL`, `GEAR_REVERSE`, `GEAR_PARK`, `GEAR_DRIVE`, `GEAR_1`~`GEAR_9` | P → 주차, R·N·D·1~9 → 주행 중, UNKNOWN·null → 상태 유지 |
| 권한 | AAR, 샘플 앱 | `@RequiresPermission("pleos.car.permission.CAR_DRIVE")`. 샘플 앱은 Vehicle Service normal 보호 수준 읽기 권한으로 선언 | 매니페스트 선언, 런타임 요청 없음 |
| 지원 확인 | 원문, AAR | 원문은 `checkXXXCapabilities` 사용(CP-001). `Gear`에는 없다 | 호출 실패를 `Unavailable`로 본다 |
| 오류 | 원문 | `onFailed`의 예외는 `VehicleException`(`ERROR_NOT_SUPPORTED_PROPERTY`, `ERROR_SECURITY_PERMISSION`, `ERROR_SERVICE_CONNECTION_FAILURE` 등)일 수도 아닐 수도 있다 | 2.3.2에서 예외 클래스가 공개 이름이 아니어서 종류를 나누지 않고 로그만 남긴다 |

### 원문에 없는 항목(미결)

- `Gear` 도메인과 `VehicleGear` 값, 기어와 `CAR_DRIVE`의 관계(원문 권한 표의 `CAR_DRIVE`는 DrivingMode, Brake, Odometer만 적는다)
- 기어 지원 여부 확인 방법과 미지원 차량의 오류 코드
- `registerGearSelection` 직후 현재 값 전달 여부, 콜백 스레드, 서비스 재연결
- API-002는 기어 같은 표준 데이터에 AAOS Car API를 권한다. 사용자 결정으로 Pleos API를 쓴다([T-034 계획](../plans/active/T-034-gear-driving-state.md))
- Vehicle 서비스(`ai.pleos.playground.vehicle.VEHICLE_START`)가 있는 에뮬레이터 이미지. 2026-11-14 로컬 AVD `Pleos_Connect_v2.1.0`에는 없었다
