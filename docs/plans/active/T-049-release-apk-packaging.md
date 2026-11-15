---
task: T-049
---

# T-049 Release APK packaging for submission

## 목표
현재 main 소스로 release APK를 빌드하고 `apk/PotatoesBook5_WithIm_release.apk`에 저장한다.

## 범위 밖
- 앱 기능 또는 프로젝트 SDK 설정 변경
- 서명 키, `local.properties`, API 키를 열거나 공유

## 작업 체크리스트
- [x] main 최신 상태와 작업 트리를 확인하고 T-ID 등록
- [x] Release APK 빌드
- [x] 지정된 `apk/` 경로에 복사
- [x] APK 패키지 정보와 파일 체크섬 확인
- [x] APK 서명 및 검증
- [ ] GitHub에 제출 가능한 방식으로 APK 게시

## 검증 방법
- `./gradlew :app:assembleRelease`
- `apkanalyzer` 또는 Android CLI로 application ID, version, APK 정보를 확인
- 생성된 APK의 SHA-256 기록

## 결정 로그
- 제출 요청 파일명은 그대로 사용한다: `PotatoesBook5_WithIm_release.apk`.
- 저장소 main에 직접 푸시하지 않는다.

## 진행 기록
- main이 `origin/main`과 일치하고 시작 시 작업 트리가 깨끗함을 확인.
- 프로젝트 release build type은 난독화를 끄며 별도 release signing config는 선언하지 않음.
- `AppRoot`는 `DevControls`를 `BuildConfig.DEBUG`에서만 표시하므로 release APK에는 개발용 주행 패널 UI가 노출되지 않는다. 소스 기능 수정은 하지 않음.
- Release 빌드 성공. 결과 APK는 지정 경로에 복사했고 application ID/version을 확인함.
- 기본 Gradle release 산출물은 unsigned였으나, 사용자가 지정 경로 APK에 v3 서명을 적용하고 기기 실행을 확인함. `apksigner verify`도 v3 서명을 검증함.
- APK 133 MB라 Git LFS를 설치하고 `apk/*.apk`를 추적하도록 설정함.
- APK와 Git LFS pointer를 원격 작업 브랜치로 푸시하는 중.
