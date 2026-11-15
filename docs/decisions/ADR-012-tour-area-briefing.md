---
id: ADR-012
task: T-044
---
# 관심 지역·브리핑 저장과 공급자 경계

## 결정
기존 Room을 v5로 확장해 프로필/지역 유일키의 관심 지역, 내용 버전별 읽음 상태, 실제 진입 기록을 저장한다. 삭제 시 연관 항목을 함께 제거한다. 관심 지역 저장은 단지 스크랩과 분리하며 멱등이다. 저장한 영역 정의만 보존하고 연속 차량 위치나 원본 실거래 좌표는 저장하지 않는다.

브리핑은 저장 지역의 실제 MOLIT 집계 내용이 변한 경우 수집 시각을 밝혀 '집계 갱신'으로 제공한다. 이것을 개별 신규 계약/청약 공고/뉴스로 바꾸어 말하지 않는다. 청약/뉴스 공급자 미연결은 source status로 구분한다. callback/fetch만으로 읽음 처리하지 않고 실제 확인 API가 일치 버전에만 읽음을 기록한다.

장소 검색은 기존 Vworld 키/HTTP 환경의 공식 검색 API로 준비하며 좌표는 PLACE point임을 표시한다. 이를 차량 출입구 좌표로 사용하지 않는다. 장소 검색이 모호/실패하면 영역을 만들어내지 않는다. 원형 역세권 반경은 명시적으로 500m이며 법정동/시군구 경계로 표현하지 않는다.

NaviHelper SDK는 공식 가이드의 3.0.5를 별도 라이브러리로 도입할 수 있다. 기존 compile/min/target SDK, Kotlin, AGP, JDK, FusedLocation 버전은 올리지 않는다. 호환 불가 시 그 환경을 변경하지 않고 capability 미지원으로 반환한다. SDK 타입은 vehicle에 제한한다. 카메라 스트리밍 SDK를 추정하거나 추가하지 않는다.

## 제한
공개 Gleo 호출 인증이 미확인인 상태에서 개인 지역/위치/브리핑을 노출하지 않는다. 신규 개인정보 공유·클라우드 동기화·백그라운드 FGS·자동 발화를 활성화하지 않는다. 새 저장 기능은 기존 동의와 명시 저장 요청, 활성 로컬 프로필 범위에 한정하고 기존 개인 데이터 삭제에 포함한다.

## 근거
2026-11-15 확인: [NaviHelper 도입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk), [권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions), [목적지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations). 기존 ADR-005/ADR-011의 저장/읽기 분리를 확장한다.
