---
task: T-044
---
# T-044 실제 실거래 가격 Action

## 목표
GET_AREA_PRICE_SUMMARY의 runtime dummy 연결을 제거하고 기존 MOLIT 완료 배치의 구 전체 집계를 반환한다. Intent 수신 시 위치/앱 문맥을 고정하고 음성 요청에서는 네트워크·수집 잠금에 접근하지 않는다. 실제 데이터가 없는 기능은 합성 성공 대신 미지원/미준비를 반환한다.

## 범위 밖
SDK/DB 버전 변경, 키/로컬 설정 읽기, Phone 실행, release 공개, 청약/뉴스 공급자 신설, 관심 지역 영속 저장, 지역 진입 판정·Gleo private API. 기존 브리핑과 지역 저장의 허위 dummy 응답은 제거하되 별도 기능을 스크랩 브리핑으로 가장하지 않는다.

## 작업 체크리스트
- [x] 구 집계의 최고 단지 이름·빈/부족 표본·범위 메타데이터 완성
- [x] 실제 위치 판별 결과의 수신 snapshot, 명시 지역/직전 조회 지역, TTL/동의 검증
- [x] prepared read 전용 실제 repository와 debug DI·callback 연결
- [x] JVM/Intent/수집 경합·빈 결과/부족 표본/만료/위치 없는 조건 테스트
- [x] Pleos Connect 전체 계측·빌드·lint·독립 리뷰
- [x] API/spec/재실행 방법·실행 결과 갱신, 코드와 문서 분리 커밋

## 검증 방법
합성 원천 fixture를 실제 수집/집계/실제 가격 repository/adapter에 통과시켜 데이터 변동이 callback에 반영되는지 확인한다. 실제 공급자·차량 위치 검증과 구분한다. 가격 처리 경로에 fetch/districtAt/geocode를 연결하지 않는다. 고정 12억 응답을 제거했는지, 원→만원 변환/기간/구 전체 범위/출처가 정확한지 검사한다. TTS의 Gleo 연결이 여전히 불가하면 별도 BLOCKED로 기록한다.

## 결정 로그
- origin 최신화 확인: 현재 branch에 origin/main 미포함 커밋 없음(16 ahead, 0 behind). 기존 geonu/gleo-integration에서 진행한다.
- 현재 구 전체·달력 3개월·전용 [80,90)㎡ 집계를 사용하며 반경 3km 순위/법정동 평균으로 바꾸어 부르지 않는다.
- 현재 위치는 SDK 실제 입력과 공식 POINT 판별이 있는 최근 snapshot만 사용한다. 참고 좌표 사용 금지. 정확도 미제공/불명확은 areaQuery를 요청하고, 준비된 명시 시군구 조회는 제공한다. 단위 미확인 confidence를 m로 변환하지 않는다.
- 위치 경계 다각형이 없으므로 법정동/경계 비중첩을 보장하지 않는다. 응답은 확정된 시군구 이름을 표시하며 관심 영역 진입 판정으로 사용하지 않는다.
- 명시 지역은 준비된 지역의 정확한 전체 이름 또는 단일한 시군구 이름만 일치시킨다. 모호/미준비는 확인 요청이며 음성 중 새 지역 검색을 기다리지 않는다.
- 직전 성공 조회는 앱 세션·프로필·동의 세대별 60초이며 재참조로 TTL을 연장하지 않는다. 현재 위치를 명시하면 직전 지역으로 대체하지 않는다.

## 진행 기록
2026-11-15: 실제 수집·prepared 집계는 있으나 debug GleoBindings가 dummy를 사용함을 확인. 가격 모델의 금액/최고 단지 필드가 non-null이라 빈 표본을 표현하지 못하고, 최고 단지 이름도 집계에 없음을 확인했다.


2026-11-15 구현/검증: JVM 173개·호스트 17개·Pleos 계측 18개 통과, debug/release 빌드·lint 통과. 무주입 production DI의 실제 공급자 probe에서 336개 표본의 LIVE/synthetic=false callback을 확인했다. 후속 콜드 실행은 MARKET_NOT_READY/PROVIDER_REJECTED로 BLOCKED였으며 공급자 가용성 정상화로 기록하지 않는다. Gleo 음성은 기존 연결 null로 3건 BLOCKED다. [결과](../../testing/gleo-tour/live-price.md), [JSON](../../testing/gleo-tour/runs/2026-11-15-live-price.json).

독립 리뷰 지적: 공개 debug receiver에서 위치/조회 문맥을 외부 응답으로 연결하지 않도록 GetAreaPriceSummaryUseCase policy와 runtime DI에서 CURRENT_LOCATION/LAST_REFERENCED_AREA를 사전 차단했다. 외부 schema는 NAMED_AREA/areaQuery 필수이며 0-read 단위 테스트와 실제 wire 단언을 추가했다. 내부 현재 위치/직전 지역 지원과 외부 활성화는 구분한다. 재검토 차단 지적 없음.

범위 편차: 가격 제공 범위를 명시한 준비된 시군구로 제한했다. 정확도 단위/공식 호출자 인증을 추정하지 않았으며 네트워크 검색 fallback·새 데이터 공급자·미지원 청약/뉴스의 대체 합성 응답을 추가하지 않았다. 테스트 runner의 opt-in 실제 공급자 점검을 추가했고 기존 일반 JUnit 실행을 보존했다.
