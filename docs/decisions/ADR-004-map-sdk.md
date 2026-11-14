---
id: ADR-004
task: T-013
---

# ADR-004 지도 SDK로 네이버 지도 채택

## 배경

주행 중 지도 모드(SCR-009)는 차량 위치를 기준으로 순위 단지와 관심 매물을 실제 지도 위에 보여 준다. 지금까지는 SDK가 정해지지 않아 간이 지도(`SchematicMap`)를 그렸다.

Pleos Connect가 제공하는 SDK는 Vehicle, NaviHelper, TextToSpeech, SpeechToText, FusedLocation이다. NaviHelper(v3.0.5 문서 기준)는 내장 내비 앱의 목적지·경유지 설정과 화면 전환(`requestNavigationScreen`)만 제공하고, 앱 안에 지도를 그리거나 마커를 추가하는 API는 없다. 따라서 외부 지도 SDK가 필요하다.

## 선택지

1. 네이버 지도 Android SDK
2. 카카오맵 Android SDK v2
3. Google Maps SDK for Android
4. 간이 지도 유지(레이더 대체안, SCR-009 시안 C)

## 결정

2026-11-14 사용자 결정으로 네이버 지도 Android SDK를 채택한다.

- 버전: `com.naver.maps:map-sdk` 3.24.1. 저장소는 `https://repository.map.naver.com/archive/maven`
- 인증: 네이버 클라우드 플랫폼 Maps Application의 Client ID(Dynamic Map 선택). `local.properties`의 `naver.map.clientId` → `BuildConfig.NAVER_MAP_CLIENT_ID`로 전달하고 `Application`에서 설정한다. Client Secret은 지도 표시에 쓰지 않으므로 앱에 넣지 않는다.
- 위치: `:feature:driving` 모듈의 `ui/driving/NaverDrivingMap.kt`(ADR-009). 지도 SDK 타입은 UI 계층 밖으로 나가지 않고, domain에는 `GeoPoint`, `VehiclePose`만 둔다.
- 위치 소스: SDK의 `FusedLocationSource`(Google Play 서비스 필요)를 쓰지 않는다. 차량 위치는 도메인 데이터로 받아 직접 표시한다. 실제 위치는 Pleos FusedLocation SDK로 연결한다(F-001).
- Compose 래퍼 라이브러리는 쓰지 않고 `AndroidView`로 `MapView`를 감싼다.
- 클라이언트 ID가 없거나 차량 위치가 없으면 간이 지도로 대체한다.

## 결과와 영향

- 국내 지도 데이터 위에 실제 길과 단지 위치가 맞게 표시된다.
- 네이버 클라우드 플랫폼 사용량과 과금 조건을 따른다. 키는 팀원마다 `local.properties`에 넣는다.
- AAOS(Pleos Connect)에서의 지원 여부는 SDK 문서에 없다. 에뮬레이터 확인 결과를 T-013 계획에 기록하고, 실차에서 다시 확인한다.
- 주행 중 지도 표시 허용 여부는 T-001에서 확정한다. 허용되지 않으면 간이 지도(레이더) 대체안을 쓴다.
