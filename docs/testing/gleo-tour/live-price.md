# 실제 가격 연결 검증 — T-043

2026-11-15. **가격 runtime dummy를 제거하고 실제 MOLIT 거래 집계→가격 App API→callback을 연결했다.** 실제 공급자 probe에서는 fixture를 주입하지 않은 앱에서 **336건의 실제 집계가 LIVE/synthetic=false callback으로 반환**됐다. 실제 조회 결과의 지역명·가격·좌표·키 원문은 검증 로그에 저장하지 않았다. 고정 발화 fixture에는 공개 시군구 이름이 포함된다.

## 현재 제공 범위

- 공개 debug Gleo 함수: GET_AREA_PRICE_SUMMARY, target=NAMED_AREA, areaQuery 필수. 준비된 시군구의 전체 이름 또는 유일한 시군구 이름을 사용한다.
- 범위: 해당 시군구 전체, 달력 3개월, 전용 [80,90)㎡, 해제 거래 제외 원본 평균·최고 단일 거래. 현재 구 3km 순위나 법정동 평균이 아니다.
- 실제 원천: 기존 RemoteApartmentTradeRepository와 MOLIT API. 음성 요청은 완료된 immutable prepared 집계만 읽는다. network/geocode/districtAt/수집 잠금을 호출하지 않는다.
- 실제 성공: LIVE, synthetic=false, sourceId=molit-apartment-sale, 실제 수집 시각·기간·면적·표본수·범위·최고 단지 이름. 없는 단지 식별자는 null로 둔다.
- 표본 0: success/EMPTY, 금액 null. 1~2: INSUFFICIENT. 3 이상: AVAILABLE. 적은 표본을 동네의 일반 시세로 단정하지 않는다.
- cold/미준비/5분 만료: 확인 요청 또는 DATA_NOT_READY/STALE_DATA. 기존 값의 시각을 새로 찍거나 지연 fetch로 대체하지 않는다.

현재 위치/직전 지역은 내부 API에 구현되어 있지만 **공개 debug 경로에서는 저장소 읽기 전 거부**한다. 공식 호출자 인증·응답 대상 확인이 남아 있으며 공개 broadcast로 사적 위치/조회 이력을 노출하지 않는다. SDK 정확도는 단위 미제공이라 null이며 0m로 바꾸지 않는다. 내부 CURRENT_LOCATION도 그런 경우 areaQuery를 요청한다.

지역 저장·브리핑은 실제 영속 구현/공급자가 없어 UNSUPPORTED로 응답한다. 기존 가짜 청약·뉴스·가격 성공은 runtime에서 제거했다. 단지 스크랩은 기존 실제 Room 저장을 유지한다. 실제 Gleo 검증이 남아 callback은 silent이며 release 외부 진입점은 활성화하지 않았다.

## 재실행

기존 Java 17/Android SDK·키 설정을 그대로 사용한다. 키 파일을 열거나 출력할 필요가 없다. [음성 도구 준비](../../../harness/voice/README.md)를 따르고 저장소 루트에서 실행한다.

```sh
./harness/verify.sh
python3 -m unittest discover -s harness/voice -p 'test_*.py' -v
python3 harness/voice/run-pleos.py --out /tmp/gleo-live-price --instrumentation all --live-provider-probe
```

- --instrumentation all: 전체 Android 계측 18개. Gleo 경계만은 --instrumentation(11개).
- --live-provider-probe: 실제 앱 DI·실제 공급자 점검. 가격 fixture/가짜 위치/동의를 주입하지 않는다. 기존 foreground/온보딩/동의가 필요하다. 명시 지역은 앱이 실제 준비한 시군구에서 선택하며 현재 차량 위치라는 주장은 하지 않는다.
- 결과: app-boundary.json, live-provider.json, results.json, run.json. 실행마다 새 디렉터리이며 read-only Pleos 인스턴스를 종료한다.
- live-provider의 PASS와 음성 결과는 독립이다. Gleo 연결이 없으면 전체 종료 코드는 2여도 실제 공급자 PASS 기록은 남는다. 시장 데이터 미준비/초기 수집 실패도 BLOCKED로 구분한다.
- 음성 fixture는 명시한 강남구 가격/브리핑 미지원/대상 없는 스크랩이다. 준비된 지역이 다르거나 동의/화면 조건이 없으면 기대 상태와 다를 수 있으므로 결과를 임의 PASS로 바꾸지 않는다.

## 실행 증거의 구분

[JSON 기록](runs/2026-11-15-live-price.json)을 따른다.

| 계층 | 결과 | 범위 |
|---|---|---|
| JVM | 173개 PASS, 실패/오류/skip 0 | 실제 가격 repository, 교체·빈 표본·만료·동의·모호성·문맥, 공개 위치 차단 포함 |
| 호스트 도구 | 17개 PASS | 음원/소유 세션/마이크/중복·과거 로그·실패 정리·완료 개수 |
| Pleos 전체 계측 | 18개 PASS | 기존 DB/스크랩/wire와 실제 가격 수집 pipeline 회귀 |
| 실제 가격 pipeline 회귀 | PASS | provider fixture를 실제 LiveMarketRepository에 전달→prepared→실제 repository→Android decoder/callback. 실제 공급자 성공과 별도 |
| 실제 공급자 probe | PASS, 336건 | 무주입 production DI의 실제 원천·가격 callback. 후속 콜드 실행은 MARKET_NOT_READY/PROVIDER_REJECTED로 BLOCKED도 관찰 |
| 실제 음성 | BLOCKED | Gleo assistant 연결 null. STT/함수 선택/최종 음성은 통과 아님 |
| debug/release 빌드·lint | PASS | SDK/DB 버전과 release 공개 범위를 유지 |

후속 실제 공급자 점검은 원천 거부(PROVIDER_REJECTED)로 수집이 준비되지 않았다. 거부 원인은 키 내용을 읽거나 오류 원문을 노출하지 않아 특정하지 않았으며, 정상화 완료로 기록하지 않는다.

시장 수집이 완료되기 전에는 즉시 미준비를 응답한다. 단 한 번의 실제 원천 성공을 모든 지역·환경의 준비 시간이나 가용성 보장으로 해석하지 않는다. 별도 실공급자 probe 결과에는 공개된 가격 대신 성공 상태·표본수·합성 여부만 남긴다.

원본 제품 TC 90개는 이번 하위 계층 검증으로 일괄 PASS 처리하지 않는다. 가격 관련 TC의 기간/범위는 현재 실행 계약과 초기 S-011 제안의 차이를 함께 검토해야 한다. 관심 지역 저장/브리핑/진입·카메라·내비 전체 여정은 미구현/지원 Gate가 남아 있다.

## 리뷰

독립 리뷰의 공개 위치 경로 지적을 반영했다. 외부 schema를 NAMED_AREA로 제한하고 domain UseCase policy로 현재 위치/직전 지역을 저장소 읽기 전 차단했다. 실제 wire 테스트와 0-read 단위 테스트를 추가했다. 재리뷰에서 새 차단 지적은 없었다. 내부 API의 시군구 결과를 법정동 내부/관심 영역 진입 판정으로 사용하지 않는다.
