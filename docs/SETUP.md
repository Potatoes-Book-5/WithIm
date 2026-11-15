# 개발 환경 설정 가이드

Pleos Connect Emulator에서 이 프로젝트(Hello World)를 실행하기까지의 절차입니다.
기본은 **macOS (Apple Silicon)** 기준이며, Windows에서 다른 부분은 [Windows에서 설정하기](#windows에서-설정하기)에 정리했습니다.

> 기준 문서: [Pleos Connect Emulator 설치 및 실행](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator) ·
> [Emulator 설정](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator) ·
> [Virtual Console 셋업](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/use-vehicle-emulator)
> (2026-11 기준으로 정리. 버전이 바뀌면 원문을 우선합니다.)

---

## 0. 요구 사항

| 항목 | 기준 |
|---|---|
| Android Studio | 최신 버전 |
| JDK | 17 이상 (Android Studio 내장 JDK 사용 권장) |
| Gradle / AGP | Gradle 8.10.2 (wrapper 포함) / AGP 8.8.2 |
| Kotlin | 2.0.0 + Jetpack Compose |
| compileSdk / targetSdk | 35 |
| minSdk | **28 이상** (Pleos 필수 — 차량 보안용 사이닝 키 로테이션·리니지 지원) |
| Pleos Connect Emulator | v2.1.0 (Android 14, API 34 이미지) |
| GPU | 필수. 최소 RTX 3060 Laptop 6GB / RX 6600M 8GB급 (Apple Silicon은 내장 GPU로 동작) |

---

## 1. Android Studio 설치

1. [developer.android.com/studio](https://developer.android.com/studio)에서 macOS (Apple chip)용을 받아 설치합니다.
2. 첫 실행 마법사에서 Standard 설치를 진행하면 Android SDK가 `~/Library/Android/sdk`에 설치됩니다.

---

## 2. Pleos Connect Emulator 시스템 이미지 설치

> 이전 버전의 Pleos 이미지가 있다면 먼저 삭제하고 진행합니다.

1. Android Studio > **Tools > SDK Manager**
2. **SDK Update Sites** 탭 > 왼쪽 위 **+**
3. 다음과 같이 입력하고 **OK**
   - Name: `Pleos Connect System Image`
   - URL (macOS, Apple Silicon):
     ```
     https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml
     ```
4. **SDK Platforms** 탭 > 아래 **Show Package Details** 체크
5. **Android 14 (API 34)** 아래의 **Pleos Connect System Image** 체크 > **OK** → 설치
6. 설치가 끝나면 **Finish** 후 **Android Studio 재시작**

> Intel Mac이라면 아래 Windows/Linux용 URL(x86_64 이미지)을 사용합니다.

---

## 3. AVD(가상 차량) 만들기

1. **Tools > Device Manager** > **Create device** (구버전: Create Virtual Device)
2. 왼쪽 Form Factor에서 **Automotive** 선택
3. **Pleos Connect v2.1.0** 선택 > **Next**
4. **Device** 탭 > Select system image > API: **API 34** > **Pleos Connect v2.1.0 APIs** 선택
5. **Advanced Settings** 탭
   - Startup > Default boot: **Cold** ← 필수. Quick boot면 VHAL(차량 신호)을 쓸 수 없습니다.
   - Emulated Performance > RAM: **4 GB**
   - (구버전 Android Studio: Graphics `Hardware`, Boot option `Cold boot`)
6. **Finish**

AVD는 Device Manager의 ▶ 버튼으로 실행합니다. 에뮬레이터 UI는 기본 한국어이며, 영어가 필요하면 **차량 설정 > 일반 설정 > 언어/Language**에서 바꿉니다.

---

## 4. 프로젝트 열기

```bash
git clone https://github.com/Potatoes-Book-5/King-of-real-estate.git ~/AndroidStudioProjects/King-of-real-estate
```

1. Android Studio > **Open** > `King-of-real-estate` 폴더 선택
2. `local.properties` 만들기 (git에 올라가지 않는 파일)
   ```bash
   cd ~/AndroidStudioProjects/King-of-real-estate
   cp local.properties.example local.properties
   ```
   Pleos Playground > Workspace > **My Project** > 프로젝트 정보에서 값을 채웁니다.
   - `pleos.projectId` — 프로젝트 ID(Key)
   - `pleos.clientId`, `pleos.clientSecret` — SDK 클라이언트 ID / 시크릿 (TTS·STT SDK에서 사용)
   - 빌드 시 `BuildConfig.PLEOS_PROJECT_ID` / `PLEOS_CLIENT_ID` / `PLEOS_CLIENT_SECRET`으로 들어갑니다.
   - 실거래·좌표 조회 키 `MOLIT_API_KEY`, `VWORLD_API_KEY`도 여기에 넣습니다. 프로젝트 `gradle.properties`는 git에 올라가므로 넣지 않습니다.
   - 키를 바꾼 뒤에는 다시 빌드해야 `BuildConfig`에 반영됩니다.
3. git 훅을 활성화합니다. 클론 후 한 번만 실행하며, 빠뜨리면 [커밋 규칙](team/git-workflow.md)이 검사되지 않습니다.
   ```bash
   ./harness/setup.sh
   ```
4. Gradle Sync가 끝나기를 기다립니다. 처음에는 Gradle 8.10.2와 의존성을 내려받느라 몇 분 걸립니다.
   - Gradle JDK가 17 미만이면: **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK**를 내장 JDK(jbr)로 지정

---

## 5. 실행 — 화면에 "Hello World"

**Android Studio에서**: 상단 기기 목록에서 Pleos AVD를 고르고 **Run 'app'** (▶)

**터미널에서**:

```bash
./gradlew installDebug
adb shell am start -n com.potatoes.book5.realestate/.MainActivity
```

앱 런처에는 `app_name`인 **임과 함께**(집 모양 아이콘)로 표시됩니다. 영어 이름은 **WithIm**입니다.

> `adb`가 없다고 나오면 PATH에 추가합니다 (`~/.zshrc`):
> ```bash
> export ANDROID_HOME="$HOME/Library/Android/sdk"
> export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
> ```

---

## 6. Pleos Connect Virtual Console (차량 상태 시뮬레이터)

에뮬레이터 실행 중에 기어·도어·공조·배터리 등 차량 상태를 웹에서 조작하는 도구입니다.

```bash
adb forward tcp:8088 tcp:8088
```

브라우저에서 <http://127.0.0.1:8088/> 을 엽니다.

접속이 안 되면: 에뮬레이터 실행 여부 → `adb devices`로 연결 확인 → forward 재실행 → 8088 포트를 다른 프로세스가 쓰는지 확인.

---

## 7. 마이크 (STT 테스트 시)

에뮬레이터 패널 **⋯ (Extended Controls) > Microphone**에서 호스트 오디오 입력을 켭니다.

---

## 8. CRN 등록 — 에뮬레이터에서는 불필요

Pleos Connect Emulator **2.1.0부터는 CRN 입력이 필요 없습니다.**
CRN은 **데브박스(실차 개발 보드)** 에서 테스트할 때만 등록합니다. 값은 `local.properties`의 `pleos.testCrn` (또는 My Project > 테스트 정보 > CRN)에 있습니다.

```bash
adb root        # 'adb root on'이 아니라 'adb root'
adb shell su 0 "echo 'propId: 554696961 areaId: 0 values: <CRN>' > /data/vendor/vsomeip/vhal_fifo"
adb reboot
```

---

## Windows에서 설정하기

위 절차와 같고, 아래 항목만 다릅니다.

| 항목 | macOS | Windows |
|---|---|---|
| 시스템 이미지 URL (2단계) | `.../crp-arm64-sys-img.xml` | `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml` |
| Android SDK 기본 경로 | `~/Library/Android/sdk` | `%LOCALAPPDATA%\Android\Sdk` |
| `local.properties`의 sdk.dir | `/Users/<사용자>/Library/Android/sdk` | `C\:\\Users\\<사용자>\\AppData\\Local\\Android\\Sdk` (`:`와 `\` 이스케이프) |
| local.properties 복사 | `cp local.properties.example local.properties` | `copy local.properties.example local.properties` |
| git 훅 활성화 | 터미널에서 `./harness/setup.sh` | **Git Bash**에서 `./harness/setup.sh` |
| Gradle 실행 | `./gradlew installDebug` | `gradlew.bat installDebug` |
| adb 위치 | `$ANDROID_HOME/platform-tools` | `%LOCALAPPDATA%\Android\Sdk\platform-tools` |

**Windows 추가 확인 사항**

1. **가상화 가속**: BIOS/UEFI에서 가상화(Intel VT-x / AMD-V)를 켜고, Windows 기능에서 **Windows 하이퍼바이저 플랫폼(WHPX)** 을 활성화합니다. (Android Studio SDK Manager의 SDK Tools 탭에서 Android Emulator hypervisor driver를 설치하는 방법도 있습니다.)
2. **외장 GPU 필수**: 권장 RTX 4060/5060 Laptop 8GB 또는 RX 7700S/7600M XT급, 최소 RTX 3060 Laptop 6GB / RX 6600M·6700S 8GB급. 노트북은 Android Studio·에뮬레이터가 외장 GPU를 쓰도록 NVIDIA 제어판/Windows 그래픽 설정에서 지정합니다.
3. **PATH 설정** (PowerShell, 사용자 환경 변수):
   ```powershell
   [Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")
   [Environment]::SetEnvironmentVariable("Path", "$env:Path;$env:LOCALAPPDATA\Android\Sdk\platform-tools", "User")
   ```
   새 터미널을 열어야 반영됩니다.
4. **줄바꿈**: git이 `gradlew`의 줄바꿈을 바꾸지 않도록 `git config --global core.autocrlf input` 권장.

---

## 문제 해결

| 증상 | 확인할 것 |
|---|---|
| Device Manager에 Pleos Connect가 안 보임 | SDK Manager에서 이미지 설치 후 Android Studio를 재시작했는지 |
| 에뮬레이터가 검은 화면/느림 | Graphics Hardware, RAM 4GB, GPU 사양 |
| 차량 신호(VHAL)가 안 옴 | AVD 부팅 옵션이 **Cold**인지 (Quick boot면 VHAL 불가) |
| `SDK location not found` | `local.properties`의 `sdk.dir` 경로 |
| `Unsupported class file major version` / JDK 오류 | Gradle JDK를 17 이상(내장 jbr)으로 지정 |
| Pleos SDK 의존성 다운로드 실패 | `settings.gradle.kts`에 `https://nexus-playground.pleos.ai/repository/maven-releases/` 저장소가 있는지 |

그 밖에는 [Android Emulator 문제 해결](https://developer.android.com/studio/run/emulator-troubleshooting?hl=ko), [Android Studio 알려진 문제](https://developer.android.com/studio/known-issues?hl=ko)를 참고합니다.
