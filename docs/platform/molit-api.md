# 국토부 아파트 매매 실거래가 조회

T-020, [ADR-006](../decisions/ADR-006-molit-retrofit.md)의 구현 사용법이다. 국토교통부의 [아파트 매매 실거래가 상세 자료](https://www.data.go.kr/data/15126468/openapi.do)를 `Retrofit`으로 조회한다.

## 설정

발급받은 서비스 키를 빌드 프로세스의 `MOLIT_API_KEY` 환경 변수 또는 사용자 Gradle 속성으로 전달한다. 코드, 문서, 저장소 설정 파일에 키를 쓰지 않는다. 키가 없으면 조회 결과는 `DomainError.MissingApiKey`다. 앱의 `BuildConfig`에 들어간 키는 APK에서 추출될 수 있으므로 배포용 키 관리 방식은 별도로 결정해야 한다.

## 호출

`RealEstateApplication.container.getApartmentTrades`가 `GetApartmentTradesUseCase`다. 예를 들어 2026년 9월의 강남구 거래 첫 페이지는 `getApartmentTrades("11680", YearMonth.of(2026, 9), pageNumber = 1, pageSize = 100)`으로 요청한다. 결과는 `Outcome<TradePage>`이며 다음 페이지 존재 여부는 `TradePage.nextPage`로 확인한다. 지역 코드 5자리와 계약 연월 6자리 기준의 조회이므로 도로명 주소를 직접 검색하는 API는 아니다.

가격 단위는 **만원**, 면적 단위는 **제곱미터**다. 공식 응답에 없는 선택 정보는 `null`로 둔다. 취소 관련 필드의 제공값은 원문으로 보존하며, 의미가 확인되지 않은 상태값을 임의로 해석하지 않는다. HTTP 상태, 제공기관 오류, 응답 형식 오류, 네트워크 단절과 시간 초과는 `DomainError`로 구분한다.

## 검증과 현재 범위

기존 개발 브랜치에서 에뮬레이터 앱을 실행해 국토부 API를 직접 호출했고, 2026년 9월 영통구의 실제 거래 응답 3건이 앱에 표시되는 것을 확인했다. 이 PR에는 조회 계층과 앱 컨테이너 연결만 포함한다. 빌드 및 문서 검사를 통과했으며 별도의 테스트 코드는 추가하지 않았다.

현재 조회 UseCase는 앱 컨테이너에 연결됐지만 화면의 순위와 단지 상세는 샘플 데이터를 사용한다. 단지 좌표, 반경 필터, 대표 가격, 세대수 등 화면에 필요한 별도 데이터와 정책은 아직 이 API만으로 채울 수 없다. 주소→좌표 API 연결과 화면 데이터 교체는 후속 작업이다.
