---
id: ADR-007
task: T-021
---

# ADR-007 주소 좌표 변환과 임시 기준 위치

## 배경
국토부 실거래가 API는 거래 주소를 주지만 지도용 위도·경도는 주지 않는다. 차량 좌표 연동은 후속 작업이다. 사용자는 그동안 수원 삼성전자 위치를 조회 기준으로 사용하도록 요청했다.

## 선택지
주소를 매번 좌표로 변환하거나, 고정 좌표를 거래 데이터에 추정해 채운다.

## 결정
Vworld Geocoder API 2.0 `getcoord`로 거래 주소를 좌표로 변환한다. 도로명 주소가 있으면 도로명 유형을, 없고 지번을 구성할 수 있으면 지번 유형을 요청한다. 좌표계는 EPSG:4326이며 응답의 `x`는 경도, `y`는 위도다. 좌표는 영구 저장하지 않는다.

국토부 거래 조회 지역은 수원시 영통구 법정동 시군구 코드 `41117`로 고정한다. 임시 기준점은 [삼성전자 수원 본사 주소](https://www.samsung.com/sec/ir/reports-disclosures/public-disclosure/4005/)인 경기도 수원시 영통구 삼성로 129를 Vworld로 변환해 얻는다. 이 기준점은 실제 차량 위치로 표시하지 않는다.

## 결과와 영향
거래 원본과 좌표를 조합해 임시 기준점에서의 직선거리를 계산할 수 있다. 거래 건별 거리와 단지별 대표 시세·순위는 서로 다른 개념으로 유지한다. 차량 위치 API가 연결되면 임시 기준점을 교체한다. 키는 환경 변수나 사용자 Gradle 속성으로 주입하며 저장소에 남기지 않는다.

## 출처
2026-11-14 확인: [Vworld 공식 Geocoder 예제](https://github.com/V-world/V-world_API_sample/blob/master/%EB%B8%8C%EC%9D%B4%EC%9B%94%EB%93%9C_%EA%B5%90%EC%9C%A1/2026%EB%85%84_%EB%B8%8C%EC%9D%B4%EC%9B%94%EB%93%9C_%EA%B5%90%EC%9C%A1/Readme.md), [삼성전자 본사 주소](https://www.samsung.com/sec/ir/reports-disclosures/public-disclosure/4005/).
