---
task: T-045
---
# 명세 발화의 하위 계층 검증

2026-11-15 기준 [내부 API](../../api/tour-intent-api.md)를 검증한다. 테스트 이름의 자연어는 추적용 라벨이다. `TourIntent`를 직접 공급하므로 STT/Gleo 자연어 이해·함수 선택의 통과 증거가 아니다. 원본 90개 제품 TC의 상태는 변경하지 않는다.

## 검증 대상

`TourSpecActionsTest`는 아래 15개를 실제 adapter → domain UseCase → production repository/준비 데이터 모델에 전달한다. 위치·시계·공급자 응답·저장/SDK 포트는 통제한다. JVM에서 실 HTTP를 호출하는 테스트가 아니며, 실제 SQL은 Pleos Connect의 별도 계측으로 검증한다.

| 발화 라벨 | 정규화 Intent | 검증 결과/범위 |
|---|---|---|
| 이쪽 동네 집값이 얼마 정도야? | Price(CURRENT_LOCATION) | 실제 집계 읽기 구현, 제어한 유효 위치 |
| 영통구 집값 알려줘 | Price(NAMED_AREA) | 실제 구 집계 구현, 합성 표시 없는 결과 |
| 이 동네 관심 지역으로 저장해 줘 | SaveArea() | 문맥 기반 지역 해석·저장 호출 |
| 지금 현재 위치를 관심 지역으로 저장해 줘 | SaveArea(preferCurrent=true) | 현재 위치 우선 |
| 광교중앙역 인근 관심 지역으로 저장해 줘 | SaveArea(query) | 장소 준비→500m 영역 저장 |
| 여기가 광교중앙역 인근인 건가? | CheckArea(query) | 위치 판정/추천/대장 분리 |
| 지금 내가 저장해 둔 관심 지역에 들어온 거야? | CheckArea() | 저장 영역 조회, 판정 |
| 여기서 우리 조건에 맞는 아파트는 어디야? | Recommend() | 기존 선호 조건·실제 준비 후보 필터 |
| 이 단지 스크랩해 줘 | Scrap | 선택/Nudge와 거래 baseline |
| 이 단지 상세 분석 보여줘 | Analytics() | 준비된 실제 상세 읽기 |
| 선택한 단지 출입구로 안내해 줘 | Navigate | 검증한 출입구 fixture, SDK 포트 접수까지 |
| 주변 카메라로 보여줘 | CameraScan | Unsupported Gate 통과, 촬영 성공 아님 |
| 관심 지역 새 소식 알려줘 | Briefing | 실제 수집 집계 갱신, 읽음 자동 변경 없음 |
| 브리핑 확인했어 | ConfirmBriefing | 일치 내용 버전 확인 |
| 다음 시동 때 자동 브리핑 | AutomaticBriefing | Unsupported Gate 통과, 자동 실행 아님 |

`TourSafetyTest`는 주행/신호 대기·백그라운드·동의·공개 호출 차단, 프로필 격리/멱등, 잘못된 출입구/기존 경로 확인, 가격 상한/메타데이터 누락, 조회 중 지역 삭제, 오래된 읽음 receipt, 내비 연결 중 취소를 검사한다. `NavigationCompletionTest`는 3초 후 정상 callback, 10초 timeout 원인 보존, 늦은/취소 callback 억제와 요청 식별자를 검사한다.

`TourEntryTest`는 최초 내부 관측, 500/600m 이력, 경계 불확실성, 간격/정확도, 세션/동의 세대, 삭제·재저장, 10분 간격을 검사한다. `TourNoticeSourcesTest`는 실제 RSS/청약 파서·날짜 정밀도·페이징/중복·잘못된 문서 처리를, `TourNoticeCollectorTest`는 실제 파서→수집→브리핑·원천 캐시·동의 철회·재실행 후 지역 연결을 검사한다.

`RoomTourStoreTest`는 실제 SQLite 파일 재열기, 내용 버전·읽음 멱등, 정정/중복/프로필 격리, 삭제 cascade, 트랜잭션 무효화 rollback, 과거 저장 세대의 진입 거부를 검사한다. `AppDatabaseMigrationTest`는 기존 스크랩을 보존하는 v4→5를 검사한다.

## 실행 결과

[JSON 기록](runs/2026-11-15-spec-actions.json)을 따른다.

| 계층 | 결과 | 범위 |
|---|---|---|
| JVM | 217개 PASS, 실패/오류/skip 0 | 정규화 Intent 15개, 안전/경계/내비 완료/진입/공지 파서·수집 포함 |
| 호스트 도구 | 17개 PASS | 음원/소유 세션/마이크/중복·과거 로그·실패 정리·완료 개수(23개) |
| Pleos 전체 계측 | 23개 PASS, 9.167초 | Room v5 재열기·cascade·rollback, v4→5 마이그레이션, Gleo wire/실제 가격/스크랩 회귀 |
| 실제 음성 | BLOCKED | Gleo assistant 연결 null. STT/함수 선택/최종 음성은 통과 아님 |
| debug/release 빌드·lint | PASS | SDK 환경·공개 범위 유지 |

## 재현

JDK 17 환경에서:

```sh
./harness/verify.sh
python3 -m unittest discover -s harness/voice -p 'test_*.py'
python3 harness/voice/run-pleos.py --out /tmp/gleo-tour-check --instrumentation all
```

마지막 명령은 전용 Pleos Connect 인스턴스만 띄우며 종료 시 정리한다. Android 계측과 이후 생성 TTS 검증은 별도 판정이다. Gleo 연결이 없으면 TTS는 BLOCKED일 수 있으며 이 상태를 계측 실패/성공과 섞지 않는다. [자동화 안내](../../../harness/voice/README.md).

## 남은 실제 환경 조건

- 공개 Gleo 함수는 여전히 세 개이며 인증 미확인 개인 요청은 차단한다. 새 내부 API를 외부 함수로 노출하려면 인증과 schema 호환/기기 검증이 필요하다.
- SDK 위치 정확도 미제공은 UNCERTAIN/명확화다. fixture의 정확도로 실기기 정밀도를 증명하지 않는다.
- 실제 청약 키/이용 승인 미구성은 UNCONFIGURED다. RSS 클라이언트 구현·파서 검증과 실서버 접속 성공을 구분한다.
- 내비는 실제 SDK를 사용하지만 실제 출입구/Navi POI 식별자 공급이 필요하며, 단위 테스트는 SDK 안내 시작을 증명하지 않는다.
- 카메라·자동 진입 발화·시동 자동형·새 결과의 대화면 UX는 이 내부 API 검증으로 완료 처리하지 않는다.
