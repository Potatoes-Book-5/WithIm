---
task: T-029
---

# T-029 Pleos FusedLocation을 차량 위치 입력 포트에 연결

## 목표

Pleos FusedLocation SDK 콜백을 T-027의 차량 위치 입력 포트(`VehicleLocationInput`, [location-input.md](../../platform/location-input.md))에 연결한다. 주행 지도(SCR-009)의 차량 마커와 거리 원(2km, 5km)이 사용 가능한 실제 위치를 따라 움직이게 한다. SDK 호출은 `vehicle/` 패키지에만 두고, 지도는 도메인 UseCase를 거쳐 주행 화면 MVI(Contract, Reducer, ViewModel)로 받는다.

## 범위 밖

- 실제 위치 기준 시세 조회와 순위 재계산. T-027의 `LiveMarketRepository`가 같은 입력 포트를 읽어 처리한다(main `3acab77`에 머지됨).
- FusedLocation 미지원 안내 화면(S-006)과 위치 접근 차단 팝업(`showLocationAccessBlockedPopup`)
- 권한을 거부하거나 철회한 뒤 다시 요청하는 흐름, 백그라운드 위치 갱신
- 근접 알림(`addProximityAlert`), `getLastKnownLocation`
- SDK `heading`, `confidence` 사용. 단위가 공식 문서에 없다(미결).

## 설계

공식 문서: Pleos Connect Developer Guide > Using SDKs > FusedLocation SDK. 요약은 [sdk-usage-map.md](../../platform/sdk-usage-map.md)에 있다.

- 의존성: `ai.pleos.playground:FusedLocation:1.1.3`. 문서 예시 버전 1.1.2는 저장소에 없다(2026-11-14 확인, 저장소 버전 1.0.4, 1.1.3).
- 권한: `pleos.car.permission.FUSED_LOCATION`(dangerous)을 선언한다. SCR-006에서 위치 사용에 동의하고 저장한 직후(주차 중) `ConsentEffect.RequestLocationPermission`으로 요청한다. 주행 화면에서는 요청하지 않는다.
- `vehicle/location/PleosFusedLocationBridge`
  - 앱 프로세스 동안 `ConsentRepository.observe()`와 권한 상태를 관찰한다. 권한 상태는 `MainActivity.onResume`에서 다시 확인한다.
  - 동의와 권한이 모두 있을 때만 SDK를 연다. 새 인스턴스 → `isSupported()` → `initialize()` → `registerFusedLocationCallback`, 닫을 때 `unregisterFusedLocationCallback` → `release()`
  - 상태 매핑은 location-input.md의 표를 따른다. heading과 정확도는 null로 넘긴다.
- `VehicleLocationInput.updateLocation`과 `LiveMarketRepository`의 사용 가능 위치 판정이 `accuracyMeters` null(미제공)을 받게 한다. null이면 100m 기준을 적용하지 않는다.
- 지도·순위: T-027의 흐름을 그대로 쓴다. `VehicleLocationInput` → `LiveMarketRepository`(시군구 경계 → 국토부 실거래 → 주소 좌표) → `RankingSnapshot.vehicle`과 순위 → 주행 ViewModel → 지도. heading이 null이면 지도는 북쪽을 위로 둔다.

## 작업 체크리스트

- [x] sdk-usage-map, location-input, ARCHITECTURE, F-001, SCR-009 갱신
- [x] 의존성, 매니페스트 권한
- [x] `PleosFusedLocationBridge`, AppContainer·Application·MainActivity 연결
- [x] `VehicleLocationInput` 정확도 미제공 허용
- [x] SCR-006 저장 후 권한 요청 Effect와 테스트
- [x] `feat/T-027-market-data` 위로 rebase, 겹치는 주행 화면 변경 제거, `LiveMarketRepository` 정확도 미제공 허용
- [x] L1 검증, 에뮬레이터 미지원 경로 확인
- [ ] 위치 서비스가 있는 환경에서 실제 위치 갱신 확인

## 검증 방법

- L1: `./harness/verify.sh`, `./gradlew :core:domain:test`
- 에뮬레이터: Pleos 위치 서비스(`ai.pleos.playground.service.location.fused`)가 없으면 `isSupported()`가 false라 UNSUPPORTED가 되고, 지도는 순위 스냅샷의 차량 위치로 그려지는지 확인한다. 이동 표시는 개발 빌드의 `DEBUG_LOCATION` 브로드캐스트(location-input.md)로 같은 입력 포트를 거쳐 확인한다.

## 결정 로그

- 2026-11-14: 처음에는 T-027 번호로 자체 포트(`VehicleLocationProvider.observeLocations`)와 진행 방향 계산을 만들었다. 같은 시각 main에 같은 번호의 PR #14(위치 입력 포트 계약)가 머지돼, 그 포트에 맞추고 번호를 T-029로 바꿨다(사용자 결정).
- 2026-11-14: main 규칙에 따라 방향을 모르면 null로 둔다. 연속 위치로 방향을 계산하던 구현은 뺐다.
- 2026-11-14: 위치 → 실거래 연동이 T-027의 `feat/T-027-market-data`(main 미머지)에 이미 있어, 그 브랜치 위로 rebase하고 주행 화면에 따로 넣었던 실제 위치 우선 처리와 `ObserveVehicleLocationUseCase`를 뺐다(사용자 결정).
- 2026-11-14: SDK `confidence` 단위가 문서에 없어 정확도를 null로 넘기고, 입력 포트가 null을 받도록 바꿨다.
- 2026-11-14: 위치 서비스가 없는 에뮬레이터에는 `pleos.car.permission.FUSED_LOCATION` 정의도 없어 권한 확인이 먼저면 UNSUPPORTED를 알 수 없었다. `isSupported()`를 권한보다 먼저 확인한다.

## 진행 기록

- 2026-11-14: 계획 작성, 구현
- 2026-11-14: `./harness/verify.sh`, `:core:domain:test`, `--docs` 통과. 에뮬레이터(Pleos_Connect_v2.1.0)에서 `isSupported` 실패 로그와 UNSUPPORTED 처리를 확인했다. 개발용 `DEBUG_LOCATION` 브로드캐스트로 동탄 → 영통 위치를 넣자 주행 지도의 차량 마커와 2km·5km 원이 그 위치로 옮겨지고, 방향 미확인이라 북쪽 위·위치 마커로 표시됐다. 실제 FusedLocation 콜백 수신은 위치 서비스가 있는 환경에서 확인해야 한다
- 2026-11-14: `feat/T-027-market-data` 위로 rebase 후 `./harness/verify.sh`, `:core:domain:test` 통과. 에뮬레이터에서 `DEBUG_LOCATION`으로 동탄역 → 삼성전자 수원 본사 부근 위치를 넣자 홈 순위, 주행 지도 차량 마커·거리 원·순위 핀, 실거래 TOP 3가 해당 위치 주변 실거래로 바뀌었다(동탄역 시범한화 꿈에그린 프레스티지 → 광교e편한세상). FusedLocation 정확도 null 경로는 기기에서 확인하지 못했다
- 2026-11-14: T-027 실거래 연동이 main에 머지돼(`3acab77`) main 위로 rebase했다. 문서 충돌(SCR-009, sdk-usage-map, F-001)만 정리했고 `./harness/verify.sh`, `:core:domain:test` 통과
