---
task: T-039
---

# UseCase 기반 App Action API 초안

T-039의 dummy 구현 계약이다. 실제 Gleo/원천 데이터 연동은 미검증이다. [ARCHITECTURE](../ARCHITECTURE.md)의 domain/port/usecase 규칙과 [ADR-003](../decisions/ADR-003-clean-architecture-mvi.md)을 따른다. Android wire 규칙은 [어댑터 계약](gleo-intent-adapter.md)에서 별도로 정의한다.

## 1. API의 의미와 소유 계층

`Intent handling API`는 이 문서의 App API를 뜻한다. Android Intent 자체를 받는 함수는 vehicle.gleo에만 존재한다. domain은 원래 자연어 발화·BroadcastReceiver·Bundle·JSONObject·Gleo status를 알지 못한다.

App API는 다음 세 UseCase의 공개 계약이다. 별도 Service 계층이나 단순 전달용 facade를 추가하지 않는다. 각 UseCase는 한 동작을 담당하고 기존 `Outcome<T>`를 반환한다.

| 외부 액션 | 내부 API/UseCase | 입력 | 결과 |
|---|---|---|---|
| GET_AREA_PRICE_SUMMARY | GetAreaPriceSummaryUseCase | AreaTarget + AppCallContext | Outcome<ActionResult<PriceSummaryPayload>> |
| SAVE_INTEREST_AREA | SaveInterestAreaUseCase | AreaTarget + AppCallContext | Outcome<ActionResult<InterestAreaSavePayload>> |
| GET_REAL_ESTATE_BRIEFING | GetDailyBriefingUseCase | AppCallContext | Outcome<ActionResult<DailyBriefingPayload>> |

호출 형태는 `suspend operator fun invoke(input, context)`다. 기존 domain/error/Outcome.kt를 재사용하며 새 Outcome을 만들지 않았다. 최신 main에는 DomainError와 Outcome이 `domain/error/Outcome.kt`에 함께 있으므로 다시 정의하지 않는다.

## 2. 공통 입력

### AreaTarget

| 필드 | 타입 | 규칙 |
|---|---|---|
| kind | enum | CURRENT_LOCATION / LAST_REFERENCED_AREA / NAMED_AREA |
| query | String? | NAMED_AREA일 때 비어 있지 않아야 함. 다른 kind에서는 값이 있으면 입력 충돌 |

이는 제품 대상의 명시적 표현이며 GPS 좌표나 Gleo의 원문을 뜻하지 않는다. 정규화한 target을 Android와 무관한 타입으로 전달한다. 대상 입력의 누락을 ‘현재 위치’로 임의 보정하지 않는다.

### AppCallContext

| 필드 | 값의 출처 | 의미 |
|---|---|---|
| requestId | 어댑터가 생성 | 앱 내부 추적용. Gleo correlation ID라는 의미 없음 |
| appSessionId | DI가 제공하는 앱 실행 세션 | Gleo 대화 세션을 추정하지 않음 |
| profileScope | 신뢰 가능한 앱 상태/이번 단계의 합성 프로필 | Intent extras에서 profileId를 받지 않음 |
| 실행 모드 | DI가 선택한 Dummy Repository | context 입력 필드가 아니며 결과 metadata에 DUMMY로 표시 |

앱 세션/프로필 타입은 구현 시 기존 모델을 먼저 재사용한다. dummy 프로필은 실제 사용자 로그인·동의·부부 식별을 구현한 것이 아니다. 실행 기한과 취소의 소유자는 외부 호출 어댑터이며 domain에는 Android 시계 타입을 전달하지 않는다.

## 3. 공통 결과와 예상 실패

기존 `Outcome`을 유지하면서 `ActionResult<T>`를 다음 두 가지 의미로 제안한다.

- `Completed<T>`: API 평가가 끝났고 typed payload와 `ResultMetadata`를 반환함. 저장 성공은 payload의 mutation 상태로 별도 확인한다.
- `NeedsInput`: 필요한 필드 코드를 반환함. 선택 후보를 이용한 후속 대화는 미구현. 사용자에게 보여 줄 영문 문장·Gleo `asking` 문자열은 어댑터에서 생성한다.

`ResultMetadata`는 `executionMode`, `sourceId`, `sourceAsOf`, `synthetic`를 갖는다. dummy에서는 `executionMode=DUMMY`, `synthetic=true`, 원천 식별자는 합성임을 명시한다. UI/Callback 표시용 억 단위 문자열 대신 원 단위 정수 금액을 반환한다.

예상 실패는 `Outcome.Failure(DomainError)`로 표현한다. 최신 main의 Offline/Timeout/Unsupported/Invalid/Unknown을 우선 사용한다. 취소 예외를 Unknown으로 잡아 성공/실패 응답을 다시 생성하지 않는다. 외부 오류 코드와 domain 오류의 변환은 어댑터 계약에 둔다.

## 4. 가격 요약 계약

- 입력: AreaTarget. 면적/기간의 음성 변경은 이번 스키마 범위 밖이다.
- PriceSummaryPayload: 확정된 합성 지역, 집계 면적/기간, 표본수, 평균 원 단위 가격, 최고 거래 금액/단지, 원천 메타데이터.
- 수치/집계 의미는 F-002가 소유한다. dummy는 기존 합성 fixture의 값만 반환하며 실제 가격 공급자·현재 위치를 조회하지 않는다.
- CURRENT_LOCATION은 실제 좌표 대신 문서화한 합성 현재 지역을 사용한다. NAMED_AREA는 합성 fixture의 알려진 이름에만 응답하고, 실제 지역명은 `Unsupported`로 반환한다. 실제 지역명에 합성 가격을 붙여 답하지 않는다.
- LAST_REFERENCED_AREA는 명시적으로 주입한 합성 문맥이 있을 때만 사용한다. 문맥이 없으면 NeedsInput이며 마지막 지역을 만들어내지 않는다.
- 동일 fixture와 입력에는 동일 payload를 반환한다. 랜덤 가격·현재 시각 기반의 변동을 넣지 않는다.

## 5. 관심 지역 저장 계약

- 입력: AreaTarget.
- InterestAreaSavePayload: 합성 지역, `mutationState`, `persisted`, 메타데이터.
- 초기 dummy 결과는 `mutationState=NOT_APPLIED`, `persisted=false`다. Completed는 호출 평가 완료만 의미하고 실제 저장 완료를 뜻하지 않는다.
- SharedPreferences/Room/파일/네트워크/기존 단지 스크랩을 변경하지 않는다. 단지 스크랩 저장소를 지역 저장의 대용으로 호출하지 않는다.
- 전송 계층은 이 결과를 Gleo `success`로 바꾸지 않는다. 정확한 매핑은 어댑터 계약에 있다.
- 실제 저장 구현으로 교체할 때만 COMMITTED/ALREADY_EXISTS를 추가하고 영속 저장 확인 후 성공을 허용한다. timeout과 commit 경계, 재시도·결과 불명 상태는 그 구현의 별도 명세가 필요하다.

## 6. 데일리 브리핑 계약

- 입력: AppCallContext를 통한 기본 요약 요청. 원천 접근·프로필 선택·읽음 변경을 외부 입력에 포함하지 않는다.
- DailyBriefingPayload: 합성 항목 목록, 항목 유형/ID, 합성 게시/수집 시점, 원천별 조회 상태, 메타데이터.
- 초기 결과는 고정된 합성 데이터다. 실거래·청약·뉴스를 수집하거나 읽음 체크포인트를 변경하지 않는다.
- 조회 API가 읽음/자동 브리핑 완료 처리를 겸하지 않는다. 실제 열람 확인 API는 후속 범위다.

## 7. 포트와 dummy 구현의 배치

각 UseCase는 domain/port의 인터페이스를 통해 값을 얻는다. 예: AreaPriceSummaryRepository, InterestAreaRepository, DailyBriefingRepository. Dummy 구현은 `data/sample/gleo/`에 둔다. 기존 main의 sample 패턴을 따르고 파일의 JSON/DB 타입을 반환하지 않는다.

domain UseCase는 입력 검증·필요 정보 판단 등 비즈니스 규칙을 갖고, wire action에 따른 분기는 vehicle.gleo가 맡는다. Repository 교체는 AppContainer에서 한다. dummy 값을 Adapter 또는 ViewModel에 하드코딩하지 않는다.

## 8. 내부 API 인수 조건

- 정상 입력·잘못된 대상 조합·합성 문맥 없음·미지원 실제 지역명·예상 실패를 JVM 테스트로 검증한다.
- domain 파일에는 android.*, androidx.*, org.json, vehicle/data/ui import가 없다.
- 같은 입력/fixture의 결과가 결정적이며 모든 dummy 결과의 메타데이터가 합성임을 명시한다.
- 저장과 브리핑 호출이 기존 Room/설정/읽음 상태를 변경하지 않는다.
- API 테스트는 Android Intent 없이 수행할 수 있다. Android Intent 성공을 자연어 E2E 성공으로 기록하지 않는다.
