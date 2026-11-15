---
task: T-041
---

# 추천 매물 음성 스크랩과 추천·대장 알림 TTS

## 목표
사용자가 요청한 대로 추천 매물 알림이 보일 때 ‘스크랩해 줘’가 알림 버튼과 같은 멱등 저장을 실행하고, 추천 알림을 짧은 한국어 TTS로 안내한다.

## 범위 밖
자체 상시 음성 인식, Gleo 인증정보/내부 DB 수정, release 외부 진입점 공개, SDK 환경 버전 변경, 기존 개발 폴더의 미커밋 변경. 후속 사용자 요청으로 현재 변경의 커밋·브랜치 푸시·main 대상 PR 작성은 범위에 포함한다.

## 작업 체크리스트
- [x] 보이는 추천 알림만 실행하는 debug Gleo 명령을 버튼의 저장 명령에 연결
- [x] 알림 대상·만료·화면 숨김·중복 발화와 탭 경합 테스트 추가
- [x] foreground 추천 알림의 TTS 및 포커스·종료·설정 처리
- [x] 사용자 요청에 따라 빌드·설치 후 실제 Pleos 화면·TTS·명령·저장 확인
- [x] 합성 음성 입력 → 실제 Gleo 인식/함수 선택 → 앱 수신 → 저장·완료 UI 확인

## 검증 방법
`harness/verify.sh --device`, 도메인/상태 회귀 테스트, Intent→실제 in-memory Room 저장 계측. Gleo 실제 음성 수신과 TTS 재생은 계측 성공과 구분한다.

## 결정 로그
- 사용자 정정: 음성 입력으로 알림의 스크랩 버튼처럼 저장하는 것이 우선이며, 알림 내용 TTS는 추가 요청이다.
- 최신 main 9ee6f8f 기반 `fix/T-041-alert-voice`에서 기존 등록 T-041을 이어간다. 알림은 SCR-005, TTS는 F-006/S-003 계약을 따른다.
- 공개 debug 요청은 상세·저장 목록을 선택하지 못하고 현재 보이는 유효 알림만 실행한다. 외부 extras의 단지 ID를 받거나 전체 개인 데이터 경로를 trusted로 승격하지 않는다. 이 제한된 알림 버튼 명령은 사용자가 요청한 debug 동작이며 release 호출자 인증은 계속 미결이다.
- 실제 Gleo는 에뮬레이터에서 mBound=true mService=null로 확인됐다. 이 환경 문제를 앱 구현 성공으로 처리하지 않는다.

## 진행 기록
- PR #34 이후 에뮬레이터 재시험 성공: 최신 main 기반 APK에서 대장 TTS requested(kind=LEADER) → started → done(4.150초)을 확인했다. 이어 실제 추천 배너가 보일 때만 합성 호출어·명령을 공식 가상 마이크에 넣어 GleoE2E received SCRAP_APARTMENT → callback success/persisted=true를 확인했다(PID 16942, monotonic 3328.471/3328.504). 같은 추천 배너가 ‘스크랩했어요/스크랩함’으로 바뀌었고 주차 요약의 스크랩됨 표시도 확인했다. 추천 TTS 재생은 2.665초였다. 실제 PC 마이크는 계속 꺼져 있으며 사람의 발화 검증과 구분한다.
- 추천 배너가 없는 대장 알림에서 음성 저장 시험을 시작했던 첫 시도는 asking/persisted=false였다. 이후 진단에 고정 kind만 추가해 추천 requested에서만 호출하도록 바로잡았다. 성공 직후 후속 청취 시험은 준비 신호를 받지 못해 명령을 넣지 않았으며 반복 저장 성공으로 간주하지 않는다.
- 재현을 위해 기존 debug 위치 수신기에 approachRecommendation/arriveNow를 추가했다. 실제 저장된 관심·피드백과 같은 추천 정책으로 현재 상위 3개 중 미저장 후보의 검증된 위치로 이동하며 가격·알림·저장 결과는 합성하지 않는다. 기존 approachLeader도 arriveNow를 선택할 수 있다. 최종 installDebug 14초 성공. 증거는 ignored harness/runs/t041-arrive-result.json과 t041-arrive-banner.png, t041-ready-round-31.png에 남겼다. 원문 발화·좌표·개인 데이터는 커밋하지 않는다.
- 최종 후속: origin/main 99e8332를 pull해 기존 작업에 충돌 없이 반영했다. 대장 발견·교체 배너도 ‘새로운 대장 아파트를 발견했어요. {단지명}입니다.’로 안내한다. 도메인 포트와 실행기를 DrivingAlertSpeech/AnnounceDrivingAlertUseCase로 일반화하고, 두 종류의 설정을 독립적으로 검사한다. 해당 설정 해제·화면 숨김·포커스 상실·배너 만료에 재생을 종료한다.
- 최신 main과 대장 TTS를 포함한 installDebug 성공(34초), 종류별 설정 종료 보완 후 최종 installDebug 성공(14초), emulator-5554 설치 완료. 대장 이벤트의 실제 재생 청취는 아직 확인하지 못했다.
- 반복 호출 진단: 공식 injectAudio 기본 모드가 이미 큐를 기다리므로 클라이언트의 추가 20ms sleep을 제거했다. Gleo의 onRequestBargeIn/onReadyToSendBuffer 이후 명령을 넣자 독립적인 두 번의 호출 모두 앱 received와 asking/persisted=false가 돌아왔다. 이 두 케이스에는 유효 추천 배너가 없었으므로 저장 성공으로 처리하지 않는다. 최신 main 반영 후 최종 저장 재시험은 새 추천 알림이 발생하지 않아 미완료다.
- 사용자 요청으로 전체 verify·단위·계측·lint는 실행하지 않았다. 별도 읽기 전용 리뷰에서 차단 이슈는 없었으며 종류별 설정 해제 시 즉시 중단하도록 보완했다. 사용자 최종 요청에 따라 현재 브랜치를 커밋·푸시하고 main 대상 PR로 공유한다. 실제 사람 마이크 및 반복 저장 안정성은 미확인으로 남긴다.
- 2026-11-15: 요청 확인 후 구현 착수.
- 사용자 최종 문구: ‘추천 매물을 발견했어요. {단지명} 매물을 스크랩하시겠어요?’로 확정했다. 질문과 음성 명령 시간을 위해 추천 배너는 15초, TTS 초기화·재생 상한은 10초로 설정했다. 대장 알림은 기존 10초다.
- 사용자가 전체 자동 검증 대신 에뮬레이터 실동작을 요청했다. verify --device는 시작하지 않았으며 installDebug만 실행했다. 빌드·설치 성공(1분 46초). 새 단위·계측 테스트는 작성했지만 실행하지 않았고, 전체 lint·회귀·별도 리뷰도 생략했다.
- Pleos emulator-5554의 기존 사용자 데이터에서 실제 추천 알림 ‘영통아이파크캐슬1단지’를 표시했다. RecommendationTts의 requested/started/done을 확인했다. 알림 중 설치된 GleoActionReceiver에 SCRAP_APARTMENT를 직접 전달해 received와 success/persisted=true callback, 같은 알림의 ‘스크랩했어요/스크랩함’을 화면 캡처로 확인했다. 주차 후 주행 요약에도 해당 단지가 ‘스크랩됨’으로 표시됐다. 실제 자연어 인식 검증과는 구분한다.
- Gleo의 mBound=true mService=null 상태는 유지됐다. 실제 발화→Gleo 함수 선택은 미확인이다. 화면과 고정 진단 이벤트는 ignored harness/runs/t041-live-*에만 남겼으며 앱 코드에서 원문 발화·단지 정보·위치를 로그로 출력하지 않는다.
- 추가 요청: 긴 TTS와 발화 응답 중 다른 매물이 나오는 문제를 조사했다. 기존 질문은 SDK started→done 실측 8.205초였다. 사용자에게 선택지를 제시한 후 응답 없이 기본 제안으로 ‘{단지명}, 스크랩할까요?’로 단축해 설치했다(installDebug 20초). 추천 배너가 활성인 동안 새 추천·대장 알림이 덮어쓰지 않게 고정하고, 숨겨진 주행 ViewModel에서는 새 알림을 표시하지 않는다.
- 에뮬레이터 콜드 부팅 후 공식 오디오 RPC에 ‘글레오’와 ‘임과 함께에서 스크랩해 줘’ 음원을 실제로 주입했다. RPC 수락과 TTS 재생 완료는 확인했지만 앱 action은 관찰되지 않았다. Gleo 표준 voice interaction binder의 null만으로 모든 음성 기능이 불가능하다고 단정하지 않는다. 이후 일반 시간 질문과 호출어/명령 분리로 실제 수신 경로를 조사 중이다.
- 진단 정정: Gleo 자체의 STT final-result에 합성 발화 ‘글레오’와 ‘스크랩해 줘’가 인식됐고 서버 appAction은 packageName=com.potatoes.book5.realestate, action=SCRAP_APARTMENT, status=200이었다. 실제 Android 전달 단계에서 action이 com.potatoes.book5.realestate.SCRAP_APARTMENT로 바뀌어 앱의 짧은 action filter와 맞지 않았다. 이는 표준 voiceinteraction binder null과 별개의 실제 전송 형식 문제다. 관찰된 스크랩 별칭만 debug manifest와 decoder에 추가했다. installDebug 18초 성공 후 직접 action broadcast 없이 음성 재검증 중이다.
- 줄인 안내의 실측 재생 시간은 3.601초로 이전 8.205초보다 약 4.6초 짧아졌다.
- 실제 발화 경로의 확인 범위를 정정한다. 공식 가상 마이크에 넣은 합성 음성에서 Gleo STT final-result와 SCRAP_APARTMENT 함수 선택까지 확인됐다. 사람의 실제 PC 마이크 녹음은 수행하지 않았다. 별칭 수정 후 최종 저장 성공은 아직 확인되지 않았으며 이 상태를 통과로 처리하지 않는다.
- 후속 재시험에서 Android shell과 일반 창 종료 모두 응답이 멈췄다. 확인한 Pleos QEMU 프로세스만 종료하고 기존 AVD·사용자 데이터로 일반 콜드 부팅을 다시 실행했다. 테스트용 gRPC 오디오 allowlist는 일반 실행에서 제거했다. 새 키·Gleo private DB·SDK 환경은 변경하지 않았다.
- 5분 제한 후속 요청의 최종 결과: 정상 부팅한 Pleos에서 Gleo History 화면을 한 번 연 뒤 앱으로 돌아와 공식 가상 마이크로 ‘글레오’와 ‘임과 함께에서 스크랩해 줘’를 전달했다. 대상 알림이 없을 때는 앱 received와 asking/persisted=false를 확인했다. 이어 실제 ‘힐스테이트영통’ 추천 알림이 뜬 동안 같은 음성을 전달해 RecommendationTts focus_lost → GleoE2E received SCRAP_APARTMENT → callback success/persisted=true를 확인했다. 같은 알림이 ‘스크랩했어요/스크랩함’으로 바뀐 화면은 harness/runs/t041-five-min-final.png에 남겼다. 직접 버튼 탭이나 SCRAP_APARTMENT broadcast 없이 실제 Gleo를 거친 결과이며, 사람의 PC 마이크 발화 검증과는 구분한다. 음성 테스트 실행 상태를 유지했고 실제 PC 마이크 전달은 꺼져 있다.
- 2026-11-15 15:00 한 번 재시험 요청: 처음 위치에서 새 알림이 없어 테스트 위치를 약 1km 북쪽으로 옮겨 ‘광교호반베르디움’ 실제 추천 알림을 띄웠다. 같은 호출어와 앱 지정 스크랩 합성 음성을 한 번 주입했지만 새 GleoE2E 수신·callback이 없었고 알림도 ‘스크랩’ 상태로 남았다. 이전 PID 2767의 성공 로그를 이번 PID 27197의 성공으로 재사용하지 않았다. 한 번의 성공 경로는 확인됐으나 반복 호출의 안정성은 미확보다. 증거는 harness/runs/t041-retry-once-*에 있다.
