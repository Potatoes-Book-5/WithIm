# 차량 위치 입력 연결 (T-027)

위치 획득과 Pleos SDK 콜백은 차량 기능 담당자가 구현한다(T-029, `vehicle/location/PleosFusedLocationBridge`). 부동산 화면은 `VehicleLocationProvider` 포트에서 받은 위치만 처리한다. 앱은 Android GPS 센서를 직접 읽지 않는다. FusedLocation API에 필요한 `pleos.car.permission.FUSED_LOCATION`(dangerous)만 SCR-006에서 위치 사용 동의를 저장한 직후 요청한다.

## Pleos 콜백 연결

`RealEstateApplication.container.vehicleLocationProvider`는 `app/vehicle/location/VehicleLocationInput`이다. 차량 SDK 콜백에서 다음처럼 전달한다.

```kotlin
val input = (application as RealEstateApplication).container.vehicleLocationProvider
input.updateLocation(
    pose = VehiclePose(
        location = GeoPoint(latitude = sdkLatitude, longitude = sdkLongitude),
        headingDeg = sdkHeadingOrNull,
    ),
    accuracyMeters = sdkAccuracyMeters,
    observedAt = sdkObservedInstant,
    source = LocationSource.VEHICLE,
)
```

위도·경도는 WGS84 도(degree), 정확도는 미터, 관측 시각은 `Instant`로 전달한다. 진행 방향을 제공하지 못하면 `headingDeg = null`로 둔다. `0.0`을 미확인 방향의 대체값으로 쓰지 않는다. SDK 지원 여부와 권한·위치 서비스 상태는 담당자가 확인하고, 위치를 쓸 수 없으면 `input.unavailable(LocationStatus.PERMISSION_REQUIRED)`처럼 실제 원인에 맞는 상태를 전달한다. 차량 모델명으로 분기하지 않는다.

앱 입력 브리지는 위치 동의가 없으면 좌표를 화면·시세 계산에 전달하지 않는다. 정확도 100m 초과 또는 잘못된 좌표는 부정확 상태로, 관측 뒤 30초가 지난 좌표는 오래된 상태로 처리한다. 콜백은 위치가 바뀔 때 계속 전달해야 한다. GPS 원좌표와 Vworld 변환 좌표를 DB에 저장하지 않는다.

사용 가능한 차량 좌표가 없는 동안 실거래 저장소는 ADR-007의 삼성전자 수원 본사 좌표를 임시 조회·지도 중심으로 쓴다. 이는 차량 위치가 아니므로 차량 마커, 근접·대장 교체 알림, 주행 기록에는 사용하지 않는다. 유효한 콜백이 오면 실제 차량 좌표 기준으로 자동 전환한다.

## 개발 빌드의 이동 경로 입력

Debug APK에만 `com.potatoes.book5.realestate.DEBUG_LOCATION` 브로드캐스트 리시버가 있다. 위도·경도·정확도를 문자열 extra `latitude`, `longitude`, `accuracy`로 전달하고 진행 방향이 있으면 `heading`을 추가한다. 수신 위치의 출처는 `EMULATOR_ROUTE`다. 프로덕션 APK에는 이 리시버가 없다.

```shell
adb shell am broadcast -a com.potatoes.book5.realestate.DEBUG_LOCATION -n com.potatoes.book5.realestate/.vehicle.location.DebugLocationReceiver --es latitude '<위도>' --es longitude '<경도>' --es accuracy '10'
```

이 입력은 에뮬레이터 화면 검토용이다. 경로는 [동탄역(경기도 화성시 동탄역로 151)](https://www.srail.or.kr/cms/archive.do?pageId=KR0406020000)에서 [삼성전자 수원 본사](https://www.samsung.com/sec/ir/reports-disclosures/public-disclosure/4005/)로 진행한다. 주소를 좌표로 바꾼 결과는 검토 경로에만 일시적으로 사용하고 DB에 남기지 않는다.

## FusedLocation 연결 (T-029)

`PleosFusedLocationBridge`가 앱 프로세스 동안 위치 동의와 권한을 관찰하고, 둘 다 있을 때만 FusedLocation SDK를 열어 이 입력에 넘긴다. SDK 문서 요약은 [sdk-usage-map.md](sdk-usage-map.md#fusedlocation-sdkt-029)에 있다.

| SDK 상황 | 입력 호출 |
|---|---|
| 위치 동의 없음 | `unavailable(CONSENT_REQUIRED)`, SDK를 열지 않음 |
| `isSupported() == false` (권한보다 먼저 확인) | `unavailable(UNSUPPORTED)` |
| 권한 없음, `SecurityException` | `unavailable(PERMISSION_REQUIRED)` |
| 등록 직후, `onChanged(null)`, 그 밖의 `onError` | `unavailable(SEARCHING)` |
| `onChanged(LocationData)` | `updateLocation(pose = 위경도·headingDeg null, accuracyMeters = null, observedAt = timestampMillis, VEHICLE)` |

- SDK `heading`과 `confidence`는 단위·기준이 공식 문서에 없어 넘기지 않는다. 그래서 `accuracyMeters`는 미제공(null)을 받도록 바꿨고, null이면 100m 정확도 기준을 적용하지 않는다. `LiveMarketRepository`의 사용 가능 위치 판정도 같은 규칙으로 맞췄다. 단위가 확인되면 실제 값을 넘긴다.

### 에뮬레이터에서 FusedLocation 위치 바꾸기(T-035 확인)

Pleos 이미지 에뮬레이터(connect v2.1.0)의 FusedLocation 서비스는 에뮬레이터 GPS 위치를 전달한다. 기본 위치는 미국 팰로앨토 부근이라 국내 실거래가 없다. 아래처럼 바꾸면 개발용 브로드캐스트 없이 `VEHICLE` 출처로 들어온다. 경도, 위도 순서다. 좌표는 검토 경로의 삼성전자 수원 본사 부근이다.

```bash
adb emu geo fix 127.0547 37.2585
```

## 개발 빌드의 현재 단지 접근 재현 (T-031)

같은 DEBUG_LOCATION 액션에 approachLeader=true, apartmentName=현재 Top 3의 단지 이름을 전달하면 실제 조회 결과의 좌표로 60초 동안 직선 접근하고 90초 유지한다. 좌표를 추정하거나 파일·DB에 쓰지 않는다. 위치 동의의 기존 검증을 그대로 거치며 일반 좌표 입력을 보내면 접근 재현을 취소한다. 이름을 생략하면 현재 1위를 선택한다. 해당 단지가 현재 조회 결과에 없으면 이동하지 않는다. 실제 도로 경로와 Pleos GPS 연결을 검증하는 기능은 아니다.

실제 Pleos 위치 콜백이 활성화된 환경에서는 SDK와 DEBUG_LOCATION이 같은 입력 포트에 쓸 수 있다. 접근 시연은 실제 GPS 콜백이 비활성인 환경에서만 사용·검증했으며 두 입력의 동시 우선순위는 보장하지 않는다.
