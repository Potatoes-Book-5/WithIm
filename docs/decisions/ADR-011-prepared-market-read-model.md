---
id: ADR-011
task: T-041
---
# 주행 수집과 음성 읽기 분리

## 결정
core/domain에 즉시 읽는 PreparedScrapReader 포트와 저장에 필요한 최소 단지 모델을 둔다. core/data의 bounded immutable 메모리 read model이 이를 구현한다. 수집은 기존 LiveMarketRepository와 네트워크 Mutex 안에서 수행하지만, 읽기는 AtomicReference snapshot 하나를 사용하며 네트워크·Mutex·DB를 호출하지 않는다.

최대 256개 단지와 8개 구 집계를 유지한다. 새 배치를 게시할 때 sourceAsOf를 보존하고 외부에서 map을 수정할 수 없게 복사한다. TTL은 5분이며 미래 시각도 사용할 수 없다. sourceAsOf는 실제 월별 응답 완료 시각을 사용하고 UI 재계산으로 freshness를 늘리지 않는다. 새 지역을 current region으로 추정하지 않고 집계를 명시적 구 코드로 조회한다.

스크랩 baseline은 단지별 최고 거래가가 아니라 준비된 원본의 최신 유효 거래다. 누락된 세대수는 null로 저장하고 상세 화면/후속 갱신에서 보강한다. 이미 저장됐으면 read model이 없어도 멱등 성공한다. cold/stale는 DATA_NOT_READY/STALE_DATA로 즉시 실패하고 서버로 fallback하지 않는다.

구 가격 집계는 완료된 월별 원본의 기간 내 미해제 거래를 사용한다. 80㎡ 이상 90㎡ 미만, 양수이며 원 단위로 안전하게 표현되는 금액만 포함한다. 평균·최고·표본수·면적·달력 기간·원천 시각·DISTRICT 범위를 보존한다. 이는 현재 구 3km 순위와 다른 집계이므로 해당 음성 API에 묵시적으로 연결하지 않는다. 거래 0건은 완료된 빈 집계이며 미준비와 구별한다.

## 결과
네트워크 준비 비용과 음성 응답 비용이 분리된다. cold start와 eviction 뒤에는 빠른 실패가 가능하다. 앱 프로세스 종료 시 메모리 데이터는 소멸한다. 영속 실거래 DB를 구축한 것으로 표현하지 않는다. 수집 캐시도 월별 원본의 최대 항목 수를 제한한다.
