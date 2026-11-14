---
task: T-035
---

# T-035 주행 화면 DO 선언과 CarUxRestrictions 적용

## 목표

T-034 검증에서 기어 D일 때 Pleos 플랫폼이 DO 미선언 앱을 `주행 중에는 사용할 수 없습니다` 화면으로 가렸다(DR-001). 앱이 DO를 선언해 주행 중에도 순위·지도 주행 화면(SCR-004, SCR-009)을 보여 준다. DO 선언 앱은 규제 상태를 받아 기능별 제한을 직접 적용해야 하므로(DI-002) `CarUxRestrictionsManager`도 연결한다.

## 범위 밖

- 앱 카테고리 확정과 Pleos 심사 문의(T-001, ADR-001). 주행 중 지도 허용 여부는 심사 결과에 따라 바뀔 수 있다.
- 주행 화면 안의 세부 제한 플래그(`UX_RESTRICTIONS_LIMIT_STRING_LENGTH`, `LIMIT_CONTENT` 등) 적용. 주행 화면은 영상·키보드·설정 입력이 없고 Top 3만 보여 준다(미결에 기록).
- 주행 화면을 별도 액티비티로 나누는 구조 변경

## 조사 결과

- Pleos 주행 규제 개발 가이드: DO를 선언한 앱도 규제가 켜지면 `CarUxRestrictions`와 `ActiveRestrictions`로 기능별 UX 제한을 직접 적용한다. `CarUxRestrictionsManager.registerListener()` → `onUxRestrictionsChanged()`. DO 선언 방법은 Pleos 문서에 없고 AAOS 문서를 참조한다.
- AAOS Driver Distraction Guidelines(source.android.com, 2026-11-14 확인): 액티비티 단위로 `<meta-data android:name="distractionOptimized" android:value="true"/>`를 선언한다. 규제 중에는 DO 액티비티만 계속 실행되거나 새로 열린다. 플랫폼은 매니페스트만 확인하고 실제 준수 여부는 심사에서 본다.

## 설계

- 매니페스트: `MainActivity`에 `distractionOptimized=true`. 화면이 한 액티비티라 앱 전체가 DO가 되므로, 앱이 주행 중 표시 화면을 직접 고른다.
- 규제 상태: `vehicle/restriction/CarUxRestrictionSource`(`UxRestrictionSource` 포트) → `ObserveUxRestrictedUseCase` → `AppResult.UxRestrictionChanged` → `AppState.uxRestricted`
  - `Car.createCar(context, null, CAR_WAIT_TIMEOUT_DO_NOT_WAIT, listener)`로 연결하고 준비될 때마다 현재 값과 리스너를 등록한다. 차량 환경이 아니거나(HW-003) 연결하지 못하면 UNAVAILABLE이며, 그때는 기어 기준 주행 상태만 쓴다.
  - `useLibrary("android.car")`로 플랫폼 선택 라이브러리를 컴파일에만 쓰고, 매니페스트에 `uses-library android.car required=false`를 둔다. compileSdk, minSdk, targetSdk는 바꾸지 않는다.
- 화면 선택(`AppState`)
  - 주행 보기 = 기어 주행 중(T-034) 또는 플랫폼 규제 중
  - 온보딩 후 주행 보기면 주행 화면(SCR-004·SCR-009)만 보여 준다. 홈, 상세, 스크랩, 마이 페이지, 설정, 요약은 주행 화면 뒤로 숨고 뒤로 가기도 막는다.
  - 온보딩 전 주행 보기면 동의·조건 입력 대신 `주차한 뒤 시작할 수 있어요` 안내(`ParkFirstScreen`)만 보여 준다(F-007 주행 중 진입 차단). 로딩 중에는 그대로 둔다.
- 주행 세션 기록(F-009)은 기어만 따른다. 규제 상태는 화면 선택에만 쓴다.

## 작업 체크리스트

- [x] T-035 등록
- [x] 도메인 포트·UseCase와 테스트
- [x] `CarUxRestrictionSource`, AppContainer, `useLibrary`, 매니페스트 DO 선언
- [x] `AppState.uxRestricted`, `showParkFirst`, `ParkFirstScreen`과 테스트
- [x] feature-state-matrix, driving-restriction, driving-restriction-impl, sdk-usage-map, ARCHITECTURE, F-007, S-005 갱신
- [x] Pleos 이미지 에뮬레이터에서 기어 D일 때 주행 화면 표시 확인
- [ ] Pleos 심사에서 DO와 주행 중 지도 허용 확인(T-001)

## 검증 방법

- L1: `./harness/verify.sh`, `./gradlew :core:domain:test`, `./harness/verify.sh --docs`
- 에뮬레이터: Pleos 이미지 AVD에서 Virtual Console로 기어 D를 고르면 플랫폼 차단 화면 없이 주행 화면 순위·지도가 보이고, P로 돌아오면 홈으로 돌아오는지 본다. 온보딩 화면에서 D를 고르면 주차 안내가 보이는지 본다.

## 미결

- 주행 화면 문자열 길이·항목 수 제한(`getMaxRestrictedStringLength`, `getMaxCumulativeContentItems`)을 적용할지. 현재는 Top 3와 단지명만 보여 준다.
- Lifestyle 후보 카테고리에서 주행 중 지도 표시가 허용되는지(feature-state-matrix `지도 모드(미확정)`, T-001)
- 한국 지역 규제 플래그 구성(DR 원문에 없음)

## 결정 로그

- 2026-11-14: 사용자가 주행 화면 DO 선언을 결정했다(ai-collaboration.md의 사람 승인 항목). 주행 중 순위와 지도를 보여 준다.
- 2026-11-14: 화면을 액티비티로 나누지 않고 한 액티비티에 DO를 선언한 뒤, 앱 상태로 주행 중 표시 화면을 제한한다. 기존 화면 스택 구조(ARCHITECTURE)를 유지하기 위해서다.

## 진행 기록

- 2026-11-14: 조사, 설계, 구현, 단위 테스트
- 2026-11-14: `./harness/verify.sh`, `:core:domain:test`, `--docs` 통과
- 2026-11-14: Pleos 이미지 AVD(`Pleos_Connect_T032_test`, connect v2.1.0)에서 Virtual Console로 기어 D를 고르자 플랫폼 차단 화면 없이 주행 화면이 보였고 순위·지도 탭을 오갈 수 있었다([지도](../../screenshots/T-035-driving-map-in-d.png)). P로 돌아오면 홈으로 돌아왔다. 앱 데이터를 지운 뒤(사용자 10) 동의 화면에서 D를 고르면 `주차한 뒤 시작할 수 있어요`만 보였고([화면](../../screenshots/T-035-park-first-in-d.png)), P로 돌아오면 동의 화면으로 돌아왔다
- 2026-11-14: 에뮬레이터의 FusedLocation 위치가 미국 팰로앨토 부근이라 국내 실거래가 없어 순위가 비어 있었다. 이 때문에 T-034의 주차 후 요약도 확인하지 못했다. 규제 상태(`isRequiresDistractionOptimization`) 값 자체는 로그로 남기지 않아 기어 D와 따로 확인하지 못했다
- 2026-11-14: `adb emu geo fix 127.0547 37.2585`(삼성전자 수원 본사 부근)로 에뮬레이터 GPS를 옮기자 FusedLocation이 그 위치를 앱에 넘겼고, 홈 순위가 광교e편한세상(3.6km), 광교중흥에스클래스(2.8km), 자연앤자이2단지(3.1km)로 채워졌다. 기어 D에서 주행 화면 순위 Top 3([화면](../../screenshots/T-035-suwon-rank-in-d.png))와 지도의 차량 위치·거리 원·순위 핀([화면](../../screenshots/T-035-driving-map-in-d.png))이 플랫폼 차단 없이 보였다
