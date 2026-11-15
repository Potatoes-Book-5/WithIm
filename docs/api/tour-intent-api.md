---
task: T-044
---
# 임장 내부 Intent API

`AppContainer.tourActions`의 `TourIntentAdapter.handle(intent, context)`는 자연어 해석이 끝난 `TourIntent`를 실제 domain UseCase로 전달한다. 결과는 `Outcome<ActionResult<TourReply>>`다. 외부 Gleo schema와 내부 구현을 분리한다. 이 API 추가로 공개 함수가 늘어나거나 자연어 인식이 구현된 것은 아니다.

## 처리 순서

자연어와 해석 문맥 → Gleo의 intent 선택(외부 검증 대상) → 앱이 동결한 `AppCallContext` → 문맥 유효성 검사 → 실제 prepared 데이터/Room 읽기 → 명령 실행 → 동일 문맥 재검증 → typed 결과 순서다. `VoiceContextSnapshot`은 선택 단지와 유효한 스크랩 Nudge, foreground/동의/화면 상태/세대를 담는다. 외부 요청이 이 값이나 trusted를 지정할 수 없다. [문맥 계약](voice-context.md)을 따른다.

가격·추천·분석·브리핑 응답은 요청 중 무거운 수집을 기다리지 않는다. 실제 MOLIT/Vworld 수집의 준비 모델과 Room을 읽는다. 미준비/만료는 실패 또는 명확화이며 합성 값으로 대체하지 않는다. 명명 장소의 첫 miss는 Vworld 준비를 예약하고 이후 요청에서 사용한다. 내비 초기 연결만 별도 상한 2초를 갖는다.

## 입력과 결과

| Intent | 구현과 결과 | 데이터/제약 |
|---|---|---|
| Price(target) | GetAreaPriceSummaryUseCase → PriceSummaryPayload | 실제 구 전체 MOLIT 집계. 현재 위치/직전 지역은 내부 신뢰 문맥만 |
| SaveArea(query, preferCurrent) | SaveTourAreaUseCase → AreaSaveResult | Room 멱등 저장, 프로필 격리. 현재 위치 명시가 직전 지역보다 우선 |
| Scrap | ScrapVoiceTargetUseCase → VoiceScrapResult | 선택 단지/활성 스크랩 Nudge + 준비된 실제 거래 baseline |
| CheckArea(query) | CheckTourAreaUseCase → AreaOverview | 저장 영역/실제 위치/최근 진입/맞춤 추천/가격 대장을 각각 반환 |
| Recommend(query) | 같은 조회 UseCase, 저장 대상 필수 조건 해제 | 실제 준비 후보에 기존 선호 조건 적용. 가격 순위와 별개 |
| Analytics(apartmentId) | GetTourAnalyticsUseCase → ApartmentDetail | 실제 detail 수집 결과 캐시, 주차 상세 허용 상태 |
| Navigate(entrance, replaceConfirmed) | NavigateTourApartmentUseCase → NavigationReceipt | 검증된 출입구와 실제 Navi POI ID/sub ID 필수. SDK 초기화 직후 실행 자격 재검증 |
| Briefing | LiveDailyBriefingRepository → DailyBriefingPayload | 실제 집계 변경/공식 RSS/구성된 청약 원천, 최대 3개. 읽음 변경 없음 |
| ConfirmBriefing(receipts) | ConfirmTourBriefingUseCase → 처리 건수 | itemId/version 일치에 한해 트랜잭션 내 자격 재검증 후 읽음 |
| CameraScan / AutomaticBriefing | Unsupported | 카메라·기록·시동 자동 발화 지원 Gate 유지 |

## 영역·진입과 선호도

공식 Vworld 장소의 500m 원과 시군구 식별자를 구분한다. 코드만 있는 시군구는 경계가 없으므로 UNCERTAIN이다. 실제 위치 10초 이내, 정확도 50m 이하만 확정 판정에 사용하며 반경과 정확도 겹침은 UNCERTAIN이다. 현 SDK가 정확도를 주지 않으면 추정하지 않는다.

진입 기록은 최초 내부 위치 관측을 진입으로 만들지 않는다. 600m 밖에서 준비 후 500m 안 진입, 연속 유효 관측, 10분 재알림 간격을 적용한다. 방문·세션·문맥 세대·지역 저장 세대를 검증한다. foreground 위치 관측은 기록만 하며 자동 발화를 시작하지 않는다. 요청형 주행 음성은 승인 전 차단한다. 연속 GPS 이력은 저장하지 않는다.

추천은 기존 조건 정책에 가격대의 배타적 상한을 적용한다. 선택 조건의 세대수/준공연도 누락은 추천으로 추정하지 않고 PARTIAL_METADATA로 표시한다. 지역 조회/브리핑 반환 전 저장 지역 세대를 다시 비교해 삭제/재저장 이전 결과를 폐기한다.

## 저장·원천·내비 수명

Room v5는 관심 지역(프로필당 최대 100개), 내용 버전별 읽음, 지역당 최근 진입, 지역당 공지 최대 20개, 원천 상태를 저장한다. 지역 삭제는 관련 기록을 cascade 삭제하며 전체 개인 데이터 삭제에도 포함한다. 지역의 공식 시군구 이름을 함께 저장해 가격 캐시 축출/재실행 후에도 공고를 연결한다. 식별 불가 시 PARTIAL_AREA_MATCH다.

집계 갱신은 신규 계약 체결로 표현하지 않는다. 게시일과 수집일을 구분하며 RSS 게시일 누락은 null이다. 공식 국토교통부 RSS 제목/링크만 수집하고 전국 보도자료임을 표시한다. 청약홈 클라이언트는 별도 키 미구성 시 UNCONFIGURED다. 기존 실거래 키를 임의로 전송하지 않는다. 원천 준비는 동의된 foreground/주차 상태에서 최대 15분마다 수행하며 발화 요청에서 HTTP를 호출하지 않는다.

내비 `SUBMITTED`는 접수이고 `GUIDING`은 SDK 시작 callback이다. `TourNavigator.events`는 requestId를 포함하고 과거 이벤트를 replay하지 않는다. pending 상태는 완료/오류/10초 timeout/취소 정리까지 유지하며 늦은 callback은 무시한다. 현재 경로를 확인할 수 없으므로 교체 확인을 요구한다. Vworld 장소 중심을 출입구/Navi POI ID로 바꾸지 않는다.

공개 debug receiver는 인증이 미확인이므로 named-area 가격만 허용하고 개인 저장·스크랩·브리핑을 거부한다. 내부 API가 구현된 상태와 실제 Gleo schema/기기 지원은 별도다. release receiver는 열지 않는다. [검증 범위](../testing/gleo-tour/spec-actions.md)를 참고한다.
