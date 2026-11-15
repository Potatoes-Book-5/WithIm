---
task: T-043
---
# T-043 TTS 기반 음성 검증

## 목표
실 마이크 없이 생성된 한국어 TTS WAV를 Pleos Connect의 가상 마이크에 주입하고, 음성 수락·Gleo 해석·앱 action·callback/저장 결과를 단계별로 기록한다. 기존 TC의 자동 검증 가능 범위와 외부 Gate를 분리한다.

## 범위 밖
일반 Phone 실행, 실제 사용자 DB/계정/키 조회·변경, Gleo private DB 수정·권한 우회, 가짜 STT 결과의 음성 E2E 통과 처리.

## 작업 체크리스트
- [x] SDK 공식 injectAudio와 offline TTS 준비
- [x] Pleos-only 사전 확인, 실 마이크 off, bounded injection·진단 로그
- [x] 정상/모호/만료 하위 계층 테스트와 앱 경계 계측 실행·결과 분류
- [x] 문서·코드 검증·독립 리뷰
- [x] 코드·검증 문서 분리 커밋

## 검증 방법
호스트 단위 테스트와 docs 검사, Pleos 계측, RPC 주입 결과·실제 action 로그를 구분한다. 실제 STT/함수 선택을 관찰하지 못하면 PASS를 부여하지 않는다. API 등록·인증 등 외부 조건 미충족은 BLOCKED/INCONCLUSIVE로 기록한다.

## 결정 로그
Emulator SDK에 배포된 emulator_controller.proto의 injectAudio를 사용한다. TTS 파일은 say/afconvert로 offline 생성하며 호스트 스피커로 재생하지 않는다. gRPC는 테스트 인스턴스의 loopback에만 허용하고, 기존 인증 파일/키를 읽지 않는다. 앱·SDK 버전은 변경하지 않는다.

## 진행 기록
2026-11-15: 공식 RPC와 설치된 Yuna 한국어 voice 확인. 현재 온라인 기기 없음.

2026-11-15: read-only Pleos Connect 테스트 인스턴스로 세 합성 음원 주입 RPC OK. Gleo 연결이 `mBound=true mService=null`이며 앱 이벤트가 없어 최초 3건 INCONCLUSIVE. 이후 보호 절차에서는 알려진 null 연결을 BLOCKED로 판정하고 주입을 생략한다. 실제 STT/최종 발화는 검증하지 않았다.

2026-11-15: Android/JVM 155개, Python 도구 17개, Pleos Gleo 경계 10개 및 전체 Android 계측 17개 통과. 수집 작업 대기 중 준비 데이터 조회 1,000회 10ms(에뮬레이터) 확인. 빌드·lint 통과. 독립 리뷰 2건 수정 후 재리뷰 차단 없음. 앱/제품 TC/실제 음성 범위를 분리한 [검증 기록](../../testing/gleo-tour/synthesized-speech.md)과 합성 JSON 증거를 추가했다.

## 편차와 남은 Gate

- 정상·모호·만료의 앱 처리/도메인 테스트는 실행했으나 실제 음성으로 전부 수행한 것은 아니다. Gleo 연결 선행 조건 때문에 발화 성공을 만들어 내지 않았다.
- 기존 기기 설치가 지연되어 해당 테스트 인스턴스를 정리하고, 관리 도구에서 read-only 신규 인스턴스와 bounded `--no-streaming` 설치로 재실행했다.
- T-041에서 미확인이었던 준비 데이터의 Android 비차단 조회 계측은 이번 실제 Pleos 실행으로 해소했다. 전체 음성 latency·최종 TTS, 실제 등록 스키마 인식은 남아 있다.
