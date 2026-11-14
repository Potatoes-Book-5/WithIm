# King of Real Estate

현대자동차 **Pleos Connect** 차량 인포테인먼트 앱.
차량 위치 주변 아파트 시세를 조회해 순위를 매기고, 사용자의 관심 조건에 맞는 매물이 주변에 있으면 음성(TTS)으로 알려 주는 것을 목표로 합니다.

> 현재 단계: **기초 개발 환경** — Pleos Connect Emulator에서 "Hello World"를 띄우는 샘플.
> 기능 요구사항과 사용할 Pleos SDK는 확정 후 추가합니다.

## 기술 스택

| 항목 | 버전 |
|---|---|
| Kotlin / Jetpack Compose (Material 3) | 2.0.0 / BOM 2024.04.01 |
| Android Gradle Plugin / Gradle | 8.8.2 / 8.10.2 |
| compileSdk · targetSdk / minSdk | 35 / 28 |
| JDK | 17 |
| 실행 환경 | Pleos Connect Emulator v2.1.0 (Android 14, API 34) |

버전은 Pleos 공식 [Vehicle SDK Sample App](https://github.com/Pleos-Playground/pleos-sample-vehicle-app) 기준에 맞췄습니다.

## 빠른 시작

```bash
git clone https://github.com/Potatoes-Book-5/King-of-real-estate.git
cd King-of-real-estate
./harness/setup.sh                              # 커밋 규칙 훅 활성화 (클론 후 1회, Windows는 Git Bash)
cp local.properties.example local.properties   # Pleos 키 입력 (git 제외 파일)
./gradlew installDebug                          # Pleos AVD 실행 중이어야 함
```

에뮬레이터·AVD 설치부터의 전체 절차(macOS / Windows)는 **[docs/SETUP.md](docs/SETUP.md)** 를 보세요.

## 프로젝트 구조

구조와 의존 방향은 **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** 를 보세요.

## 협업

팀 참여 방법은 **[CONTRIBUTING.md](CONTRIBUTING.md)**, 작업과 ID 관리는 **[docs/tracking](docs/tracking/README.md)** 을 보세요.

## Pleos 키 관리

`local.properties`(git 제외)에 넣으면 빌드 시 `BuildConfig` 필드로 들어갑니다.

| local.properties | BuildConfig | 용도 |
|---|---|---|
| `pleos.projectId` | `PLEOS_PROJECT_ID` | 앱 심사 요청 시 App Market Console 입력 |
| `pleos.clientId` | `PLEOS_CLIENT_ID` | TextToSpeech / SpeechToText SDK 앱 등록 |
| `pleos.clientSecret` | `PLEOS_CLIENT_SECRET` | 〃 |
| `pleos.testCrn` | — | 데브박스 테스트용 (에뮬레이터 2.1.0+에서는 불필요) |

**키를 코드나 커밋에 직접 넣지 마세요.**

## 앱 정보

- Package: `com.potatoes.book5.realestate`

## 대화형 임장 명세

기능 구현 전 검토할 문서의 진입점은 [S-011 신혼부부 6단계 임장](docs/product/scenarios/S-011-conversational-property-tour.md)입니다. 기능별 명세·플랫폼 지원 미결·화면·[82개 테스트 설계](docs/testing/gleo-tour/README.md)를 연결합니다. 문서의 제안값/확장 기능이 구현·승인되었다는 뜻은 아닙니다.
