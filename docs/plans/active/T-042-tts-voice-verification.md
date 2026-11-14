---
task: T-042
---
# T-042 TTS 기반 음성 검증

## 목표
실 마이크 없이 생성된 한국어 TTS WAV를 Pleos Connect의 가상 마이크에 주입하고, 음성 수락·Gleo 해석·앱 action·callback/저장 결과를 단계별로 기록한다. 기존 TC의 자동 검증 가능 범위와 외부 Gate를 분리한다.

## 범위 밖
일반 Phone 실행, 실제 사용자 DB/계정/키 조회·변경, Gleo private DB 수정·권한 우회, 가짜 STT 결과의 음성 E2E 통과 처리.

## 작업 체크리스트
- [ ] SDK 공식 injectAudio와 offline TTS 준비
- [ ] Pleos-only 사전 확인, 실 마이크 off, bounded injection·진단 로그
- [ ] 정상/모호/만료 TC와 앱 경계 계측 실행·결과 분류
- [ ] 문서·코드 검증·독립 리뷰·결과 커밋

## 검증 방법
호스트 단위 테스트와 docs 검사, Pleos 계측, RPC 주입 결과·실제 action 로그를 구분한다. 실제 STT/함수 선택을 관찰하지 못하면 PASS를 부여하지 않는다. API 등록·인증 등 외부 조건 미충족은 BLOCKED/INCONCLUSIVE로 기록한다.

## 결정 로그
Emulator SDK에 배포된 emulator_controller.proto의 injectAudio를 사용한다. TTS 파일은 say/afconvert로 offline 생성하며 호스트 스피커로 재생하지 않는다. gRPC는 테스트 인스턴스의 loopback에만 허용하고, 기존 인증 파일/키를 읽지 않는다. 앱·SDK 버전은 변경하지 않는다.

## 진행 기록
2026-11-15: 공식 RPC와 설치된 Yuna 한국어 voice 확인. 현재 온라인 기기 없음.
