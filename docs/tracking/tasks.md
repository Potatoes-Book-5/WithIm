# 작업 목록

등록은 `harness/new-id.sh`로 합니다. 운영 규칙은 [README.md](README.md)를 봅니다.

| ID | 유형 | 제목 | 상태 | 담당 | 우선순위 | 산출물 | 의존 |
|---|---|---|---|---|---|---|---|
| T-001 | research | 주행 중 TTS·지도 화면·DO 선언·앱 카테고리 Pleos 문의 | todo | - | P0 | ADR-001 | - |
| T-002 | design | 공식 .fig에서 Color·Typography 토큰 추출 | todo | - | P1 | - | - |
| T-003 | docs | TTS 알림 기능·시나리오 작성 | review | - | P1 | F-006, S-003 | T-001 |
| T-004 | harness | 하네스·문서 뼈대 구축 | review | - | P0 | ADR-002 | - |
| T-005 | research | 시세 데이터 출처와 이용 조건 선정 | todo | - | P0 | - | - |
| T-006 | docs | 원칙·플랫폼·디자인 문서 원문 대조 및 Layout·Iconography 보강 | todo | - | P1 | - | - |
| T-007 | harness | CI에 verify, commit, PR 제목 검사 연동 | todo | - | P2 | - | - |
| T-008 | harness | 브랜치 보호 설정과 CODEOWNERS 담당자 지정 | todo | - | P1 | - | - |
| T-009 | docs | F-001~F-005, F-007 기능 명세 작성 | review | - | P1 | - | T-005 |
| T-010 | docs | S-001, S-002, S-004~S-007 시나리오 작성 | review | - | P1 | - | T-009 |
| T-011 | design | SCR 화면 명세 작성과 Figma 프레임 구성 | review | - | P1 | - | T-002, T-010 |
| T-012 | docs | Clean Architecture + MVI 아키텍처 문서 작성 | review | - | P1 | ADR-003 | - |
| T-013 | research | 지도 SDK와 지도 데이터 선정 | doing | - | P1 | ADR-004, SCR-009 | - |
| T-014 | feature | 온보딩 화면 구현(SCR-006 동의, SCR-003 관심 조건) | review | - | P0 | SCR-006, SCR-003 | - |
| T-015 | feature | 홈·주행 화면 구현(SCR-001, SCR-004, SCR-009, SCR-005) | review | - | P0 | SCR-001, SCR-004, SCR-009, SCR-005 | T-014 |
| T-016 | feature | 스크랩 기능 구현(SCR-007, SCR-002, Room 저장) | review | - | P0 | SCR-007, SCR-002, ADR-005 | T-015 |
| T-017 | feature | 마이 페이지 구현(SCR-010) | review | - | P1 | SCR-010 | T-016 |
| T-018 | feature | 주행 요약 구현(SCR-008) | review | - | P1 | SCR-008 | T-016 |
| T-019 | feature | 설정 수정 모드 구현(SCR-006) | review | - | P1 | SCR-006 | T-014 |
| T-020 | feature | 국토부 아파트 실거래가 Retrofit 조회 | todo | - | P1 | ADR-006 | - |
| T-021 | feature | 수원 영통구 실거래와 주소 좌표 연결 | review | - | P1 | ADR-007 | T-020 |
| T-022 | debt | 앱 코드를 도메인 데이터 영통구 기능 모듈로 분리 | review | - | P1 | ADR-008 | T-021 |
| T-023 | feature | 주행 중 영통구 실거래 랭킹 표시 | review | - | P1 | SCR-004, SCR-009 | T-021 |
| T-024 | debt | 주행 화면을 feature driving 모듈로 분리하고 디자인시스템·공용 UI 모듈 추가 | review | - | P1 | ADR-009 | T-013, T-022 |
| T-025 | feature | 주행 중 관심 매물 상단 배너 알림(SCR-005 A안) | review | - | P1 | SCR-005 | T-024 |
| T-026 | feature | 주차 후 관심 매물 모아보기 화면과 주행 요약 연결(E안) | review | - | P1 | SCR-011 | T-025 |
| T-027 | feature | 앱 전체 부동산 샘플을 실거래 데이터로 전환 | review | - | P1 | F-001, F-002, F-003, SCR-001, SCR-009 | T-023, T-024 |
| T-028 | harness | 공공 API 키를 local.properties에서 읽도록 빌드 설정 변경 | review | - | P2 | - | - |
| T-029 | feature | Pleos FusedLocation 콜백을 차량 위치 입력 포트에 연결하고 주행 지도에 반영 | review | - | P1 | F-001, SCR-009 | T-027 |
| T-030 | feature | 주행 화면 실거래 순위를 SCR-004·SCR-009 와이어프레임대로 복원 | review | - | P1 | SCR-004, SCR-009 | T-023 |
| T-031 | feature | 현재 구 3km 순위와 근거리 지도 및 지역 대장 기록 | review | - | P1 | F-002, F-003, F-009, SCR-009 | T-027, T-030 |
| T-032 | feature | PR 23 최신 버전 에뮬레이터 설치와 실행 확인 | done | - | P1 | - | - |
| T-033 | feature | 공동주택 API로 아파트 상세 세대수 연결 | review | - | P1 | SCR-002, F-008 | T-027 |
| T-034 | feature | 차량 기어 상태로 주행 중과 주행 완료(P) 판정 | review | - | P1 | F-009, S-005, S-009, SCR-008 | - |
| T-035 | feature | 주행 화면 DO 선언과 CarUxRestrictions 적용 | review | - | P1 | F-007, S-005, SCR-004, SCR-009 | T-034 |
| T-036 | fix | 이동 중 조회 취소 감소와 실거래 메모리 캐시 재사용 개선 | review | - | P1 | F-002, F-003, SCR-009 | T-031 |
| T-037 | feature | 주행 관심 매물 실시간 추천 | doing | - | P1 | F-010, S-011, SCR-008 | T-001, T-005, T-027, T-034, T-036 |
| T-038 | feature | 지도 탭에 실거래 상위 5개 단지 표시 | review | - | P1 | F-003, SCR-009 | T-031 |
| T-039 | feature | 시세 순위 조회 기간을 최근 3개월로 변경 | review | - | P1 | F-002, F-003, SCR-009 | T-038 |
| T-040 | feature | Gleo adapter 모듈 통합 및 debug 음성 E2E | review | - | P1 | - | - |
| T-041 | feature | Nudge와 앱 상태 기반 음성 실행 context | review | - | P1 | - | - |
| T-042 | feature | 음성용 메모리 read model과 스크랩 빠른 조회 | review | - | P1 | - | - |
| T-043 | feature | TTS 음성 입력 기반 Pleos Gleo 자동 검증 | review | - | P1 | F-011, S-012 | T-041, T-042 |
| T-044 | feature | Gleo 가격 API에 실제 준비된 실거래 연결 | review | - | P0 | F-011, F-002 | T-042, T-043 |
| T-045 | feature | 명세 발화별 실제 App API와 단위 테스트 완성 | review | geonu | P0 | F-011, S-012, ADR-012 | T-044 |
| T-046 | feature | 주행 지도 표시 반경을 차량 주변 1km로 확대 | review | - | P1 | F-003, SCR-009 | T-031 |
