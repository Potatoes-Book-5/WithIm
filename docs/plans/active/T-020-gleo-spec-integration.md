---
task: T-020
---

# T-020 최신 main 기반 Gleo 명세 이관

## 목표

기존 geonu/gleo-spec의 스펙과 로컬 API/아키텍처 계획을 최신 origin/main 위로 이관한다. 기존 화면·Room·설정 구현을 보존하고 작업 번호 충돌을 해소한다.

## 범위 밖

main 직접 변경·force push·기존 원격 브랜치 이력 재작성은 하지 않는다. dummy 구현은 T-021에서 추적한다.

## 작업 체크리스트

- [x] git fetch --prune origin 수행
- [x] git ls-remote의 main과 origin/main이 c116c7d40f4785744e98b061bf15bfaf5f7c9c66로 일치함 확인
- [x] 미커밋 계획을 backup/gleo-spec-plan-20261114의 48895f2로 보존
- [x] origin/main에서 geonu/gleo-intent-adapter 생성
- [x] 본문 스펙 5550ee7과 계획 48895f2를 cherry-pick하여 이관
- [x] 기존 main의 작업/Room ADR을 유지하고 스펙 T-020/구현 T-021로 재등록
- [x] 문서 검사와 기존 코드 보존 확인

## 검증 방법

`./harness/verify.sh --docs`, 이관 직후 origin/main 대비 app/Gradle 코드 차이 없음 확인. 구현 검증은 [T-021](T-021-gleo-intent-adapter-dummy.md)에서 별도 기록한다.

## 결정 로그

이전 등록/전달 기록 커밋은 새 원장과 충돌하므로 그대로 적용하지 않고 등록을 새로 했다. 원본은 geonu/gleo-spec 및 backup 브랜치에서 보존한다. 코드/기존 팀 결정을 스펙 초안으로 되돌리지 않는다.

## 진행 기록

2026-11-14: 원격 검증과 두 커밋의 내용 이관 완료. 충돌은 작업 원장/색인과 F-004 세대수·준공 조건에서 해결했으며 최신 main의 구현 내용을 우선 유지했다.

Docs 검사 통과. 이관 직후 origin/main 대비 app/gradle/settings/build 파일 차이 없음 확인.
