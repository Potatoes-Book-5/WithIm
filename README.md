# WithIm (임과 함께)

**임(장)과 함께; 내가 궁금했던 부동산 정보와 함께하는 모빌리티 경험**

**WithIm (임과 함께)** 은 현대자동차그룹 **Pleos Connect** 차량 인포테인먼트(IVI) 플랫폼을 위한 **실시간 지능형 모빌리티 임장 앱**입니다. 차량의 실시간 위치(FusedLocation), 기어/주행 규제 상태(AAOS `GEAR_SELECTION`·`CarUxRestrictions`), 온디바이스 음성 안내(TextToSpeech), 대화형 음성 에이전트(Gleo AI), 내비게이션(NaviHelper)을 결합하여, 일상적인 주행과 드라이브 동선을 자연스러운 부동산 탐색(임장) 경험으로 확장합니다.

---

## 1. 기획 배경 및 핵심 기능

차창 밖으로 스쳐 지나가는 동네와 아파트 단지의 시세·입지가 궁금할 때, 기존에는 동승자가 스마트폰 부동산 앱으로 직접 지역과 단지를 수동 검색해야 했습니다. **임과 함께**는 이동 공간 자체를 실시간 부동산 탐색 맥락으로 연결합니다.

- **맞춤형 입지 프로파일링 & 관심 조건 설정 (`SCR-006`, `SCR-003`)**
  - 최초 실행 시 위치·알림 동의와 관심 조건(선호 동네, 매매 예산 가격대, 평형대, 세대수 규모, 건축 연식)을 설정합니다.
  - 스크랩한 단지와 추천 카드 상세 열람 이력을 로컬 선호 프로필에 반영해 추천 정확도를 높입니다.
- **Pleos 위치 기반 실시간 시세 순위 & 대장 아파트 알림 (`SCR-004`, `SCR-009`, `SCR-005`)**
  - 국토교통부 아파트 매매 실거래가(최근 3개월)와 브이월드(Vworld) 지오코딩·행정구역 데이터를 연동하여 차량 주변 반경 3km 내 실거래 상위 단지를 실시간으로 집계합니다.
  - **순위 모드(Top 3)** 와 **2D 네이버 지도 모드(Top 5 + 왕관/별 마커)** 를 지원하며, 지역 내 최고가 1위('대장 아파트') 발견·교체 시 상단 배너와 온디바이스 한국어 TTS로 안내합니다.
- **주행 중 맞춤 추천 매물 알림 & Gleo AI 음성 스크랩 (`F-006`, `F-010`, `F-011`)**
  - 주행 반경 10km 내 후보 단지를 누적·평가하여 내 조건에 부합하는 추천 매물 발견 시 상단 배너와 TTS(`"추천 매물을 발견했어요. {단지명} 매물을 스크랩하시겠어요?"`)로 알립니다.
  - 운전자의 시선 분산이나 화면 터치 없이 **"글레오, 스크랩해 줘"** 음성 발화(또는 원터치)만으로 현재 알림 매물을 즉시 스크랩하고, 주변 시세 요약·관심 지역 저장·추천 조회·단지 출입구 내비게이션 안내(`NaviHelper`)까지 확장합니다.
- **기어(P/D) 연동 주행 요약 & 심층 데이터 탐색 (`SCR-008`, `SCR-011`, `SCR-002`, `SCR-007`, `SCR-010`)**
  - 주행 중(`D`/`R`/`N`)에는 운전자 집중을 방해하지 않는 간소화된 DO(Distraction Optimized) 주행 화면만 표시합니다.
  - 목적지 도착 후 **기어를 `P`(주차)로 전환하면 즉시 '오늘의 임장 요약(`SCR-008`)'** 화면이 열리며, 주행 중 발견한 지역별 최고가 순위, 지나친 추천 매물 모아보기(`SCR-011`), 단지별 12개월 실거래 추이·세대수 상세 분석(`SCR-002`), 스크랩 목록(`SCR-007`) 및 마이페이지(`SCR-010`)를 대화면에서 확인할 수 있습니다.

---

## 2. 개발에 사용한 Pleos Connect 에뮬레이터 버전과 화면 규격

### 에뮬레이터 및 플랫폼 사양

| 항목 | 규격 / 버전 |
|---|---|
| 에뮬레이터 버전 | **Pleos Connect Emulator v2.1.0** (`Pleos_Connect_v2.1.0`, System Image: Android 14 / API 34 `crp_avd_arm64`) |
| 부팅 및 하드웨어 설정 | **Cold Boot 필수** (VHAL 차량 신호 연동용), Graphics: Hardware, RAM: 4 GB |
| 컴파일 / 타깃 / 최소 SDK | `compileSdk = 35` / `targetSdk = 35` / `minSdk = 28` (Pleos 차량 보안 키 로테이션 요건 준수) |
| 언어 및 빌드 환경 | Kotlin `2.0.0`, Jetpack Compose (Material 3 BOM `2024.04.01`), AGP `8.8.2`, Gradle `8.10.2`, JDK `17` |
| 연동 Pleos / 차량 SDK | `ai.pleos.playground:FusedLocation:1.1.3`, `ai.pleos.playground:TextToSpeech:3.3.1`, `ai.pleos.playground:NaviHelper:3.0.5`, AAOS `android.car` (`CarPropertyManager` `GEAR_SELECTION`, `CarUxRestrictionsManager`) |
| 외부 지도 / 데이터 | 네이버 지도 Android SDK (`com.naver.maps:map-sdk:3.24.1`), Room (`2.6.1`), Retrofit (`2.11.0`) |

### 디스플레이 및 화면 규격

| 항목 | 규격 | 비고 |
|---|---|---|
| 기준 디스플레이 모델 | **Connect-L (12.9인치, 16:9 가로형)** | Pleos Connect 표준 대화면 IVI |
| 전체 화면 해상도 | **2560 × 1440 px** | `adb shell wm size` 기준 |
| 화면 밀도 (DPI) | AVD 기본 `160 dpi` / **앱 창 실측 `229 dpi`** (`1 dp ≈ 1.43 px`) | `ui/theme/Dimens.kt`에서 `229 dpi` 기준 `wpx`·`wsp` 환산 적용 |
| 에뮬레이터 앱 표시 영역 | **2560 × 1268 px** | 상단 상태바(`76 px`), 하단 독(`96 px`) 제외 영역 |
| 공식 레이아웃 분할 규격 | 상태바 `112 px` / 좌측 Driving View `837 × 1140 px` / 우측 앱 영역(Landscape) `1699 × 1140 px` / 하단 독 `188 px` | 상세 수치는 [docs/design/foundations/layout.md](docs/design/foundations/layout.md) 참고 |

---

## 3. 테스트 환경 및 필요한 설정

### 3.1 필수 요구 사항
- **OS**: macOS (Apple Silicon 권장) 또는 Windows 10/11 (WHPX 가상화 및 외장 GPU 필수)
- **IDE & JDK**: Android Studio 최신 버전, **JDK 17** (Android Studio 내장 JBR 권장)
- **Android SDK & AVD**: Android 14 (API 34) **Pleos Connect System Image** 설치 및 `Pleos Connect v2.1.0` AVD 생성 (**Startup: Cold boot** 설정 필수)
  - 상세 설치 가이드는 **[docs/SETUP.md](docs/SETUP.md)** 를 참고하세요.

### 3.2 API 키 및 `local.properties` 설정
프로젝트 루트에 `local.properties` 파일을 생성하고 아래 키를 설정합니다 (`local.properties`는 Git 추적에서 제외됩니다).

```bash
cp local.properties.example local.properties
```

| 키 이름 (`local.properties`) | `BuildConfig` 필드 | 필수 여부 및 용도 |
|---|---|---|
| `sdk.dir` | — | 로컬 Android SDK 경로 (Android Studio가 자동 생성) |
| `MOLIT_API_KEY` | `MOLIT_API_KEY` | **[필수]** 공공데이터포털 국토교통부 아파트 매매 실거래가 API 키 |
| `VWORLD_API_KEY` | `VWORLD_API_KEY` | **[필수]** 브이월드(Vworld) 오픈 API 인증키 (주소→좌표 변환 및 시군구/법정동 경계 조회) |
| `KAPT_API_KEY` | `KAPT_API_KEY` | **[권장]** 공공데이터포털 공동주택 단지 목록(V4)·기본정보(V5) API 키 (아파트 상세 세대수 조회용) |
| `naver.map.clientId` | `NAVER_MAP_CLIENT_ID` | **[권장]** 네이버 클라우드 플랫폼 Dynamic Map Client ID (`X-NCP-APIGW-API-KEY-ID`로도 설정 가능. 미설정 시 간이 레이더 지도로 대체) |
| `pleos.projectId` | `PLEOS_PROJECT_ID` | Pleos Playground 프로젝트 ID |
| `pleos.clientId` | `PLEOS_CLIENT_ID` | Pleos Playground SDK Client ID |
| `pleos.clientSecret` | `PLEOS_CLIENT_SECRET` | Pleos Playground SDK Client Secret |

> `MOLIT_API_KEY`, `KAPT_API_KEY`, `VWORLD_API_KEY`는 `local.properties`에 없을 경우 `~/.gradle/gradle.properties` 또는 환경 변수에서도 자동으로 읽습니다.

### 3.3 런타임 권한 허용
앱 최초 온보딩(`SCR-006`)에서 위치·알림 동의를 저장하면 `pleos.car.permission.FUSED_LOCATION` 권한을 요청합니다. 에뮬레이터에서 위치·음성 안내(TTS)·내비게이션 기능을 즉시 테스트하려면 아래 명령으로 권한을 사전 부여할 수 있습니다.

```bash
adb shell pm grant com.potatoes.book5.realestate pleos.car.permission.FUSED_LOCATION
adb shell pm grant com.potatoes.book5.realestate pleos.car.permission.TTS_SERVICE
```

---

## 4. 실행 방법

### 4.1 앱 빌드 및 에뮬레이터 실행

1. **저장소 클론 및 Git 훅 설정**
   ```bash
   git clone https://github.com/Potatoes-Book-5/King-of-real-estate.git
   cd King-of-real-estate
   ./harness/setup.sh
   cp local.properties.example local.properties   # API 키 입력
   ```

2. **Pleos Connect v2.1.0 에뮬레이터 실행 후 앱 설치·실행**
   - **Android Studio**: 상단 기기 목록에서 `Pleos Connect v2.1.0` 선택 후 **Run 'app' (▶)**
   - **터미널**:
     ```bash
     ./gradlew installDebug
     adb shell am start -n com.potatoes.book5.realestate/.MainActivity
     ```
   - 런처에서는 **임과 함께** (영문 환경: **WithIm**), 패키지명 `com.potatoes.book5.realestate`로 설치됩니다.

### 4.2 차량 제어 및 주행 시나리오 테스트 방법

Pleos Connect 에뮬레이터에서 실제 주행(기어 변속, 차량 위치 이동, 음성 스크랩)을 재현하는 방법입니다.

1. **차량 기어 제어 (`P` ↔ `D`) — Pleos Virtual Console**
   ```bash
   adb forward tcp:8088 tcp:8088
   ```
   - 브라우저에서 `http://127.0.0.1:8088/` 접속 후 **Gear**를 `D`로 변경하면 앱이 즉시 **주행 화면(`SCR-004` 순위 / `SCR-009` 지도)** 으로 전환됩니다.
   - 주행 후 기어를 다시 **`P`(주차)** 로 변경하면 즉시 **오늘의 임장 요약(`SCR-008`)** 화면이 표시됩니다.
   - *(대체 방법)* Debug 빌드에서는 앱 화면 우측 상단의 개발용 `주행 시작` / `주차` 버튼으로도 기어 상태를 전환할 수 있습니다.

2. **차량 위치 및 주행 경로 제어**
   - **에뮬레이터 GPS 고정 (`FusedLocation` 연동)**:
     ```bash
     # 경도(longitude) 위도(latitude) 순서 (예: 용인 죽전역 -> 강남 방향 또는 수원 영통구)
     adb emu geo fix 127.1076 37.3248
     ```
   - **Debug 이동 경로 브로드캐스트**:
     ```bash
     adb shell am broadcast -a com.potatoes.book5.realestate.DEBUG_LOCATION \
       -n com.potatoes.book5.realestate/.vehicle.location.DebugLocationReceiver \
       --es latitude '37.2585' --es longitude '127.0547' --es accuracy '10'
     ```

3. **Gleo AI 음성 제어 및 알림 매물 음성 스크랩 테스트**
   - 주행 중 추천 매물 배너(`SCR-005`)가 표시되고 TTS 안내가 나온 상태에서 **"글레오, 스크랩해 줘"** 라고 발화하면(에뮬레이터 Extended Controls > Microphone 활성화 필요) Gleo AI가 `SCRAP_APARTMENT` 액션을 호출해 해당 단지를 즉시 스크랩합니다.
   - 에뮬레이터 환경에서 Gleo 브로드캐스트를 직접 재현하려면 아래 명령을 사용할 수 있습니다:
     ```bash
     ./harness/gleo-e2e.sh
     ```

4. **전체 빌드·단위 테스트·계측 검증 실행**
   ```bash
   ./harness/verify.sh --docs     # 문서 링크 및 ID 정합성 검사
   ./harness/verify.sh            # 문서 검사 + Debug 빌드 + JVM 단위 테스트 + Lint
   ./harness/verify.sh --device   # Pleos Connect 에뮬레이터 설치 및 Android 계측(Instrumentation) 테스트
   ```

---

## 5. APK 파일 위치 및 GitHub Release 링크

- **로컬 빌드 APK 생성 명령 및 위치**:
  - Debug APK 빌드: `./gradlew assembleDebug`
    - 생성 경로: `app/build/outputs/apk/debug/app-debug.apk`
  - Release APK 빌드: `./gradlew assembleRelease`
    - 생성 경로: `app/build/outputs/apk/release/app-release-unsigned.apk`
- **GitHub 저장소 및 Release 링크**:
  - GitHub Repository: <https://github.com/Potatoes-Book-5/King-of-real-estate> (공모전 제출 미러: <https://github.com/Potatoes-Book-5/WithIm>)
  - GitHub Releases (APK 다운로드): <https://github.com/Potatoes-Book-5/King-of-real-estate/releases>

---

## 6. 시연 영상 및 주요 문서

- **주행 시연 영상**: [docs/demos/README.md](docs/demos/README.md) ([MP4 영상](docs/demos/T037-driving-demo.mp4))
- **아키텍처 (Clean Architecture + MVI, 멀티 모듈 구조)**: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- **제품 기능 및 시나리오 명세**: [docs/product/overview.md](docs/product/overview.md)
- **Pleos SDK 연동 맵**: [docs/platform/sdk-usage-map.md](docs/platform/sdk-usage-map.md)
- **화면 명세 및 와이어프레임**: [docs/design/README.md](docs/design/README.md)
