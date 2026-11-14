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
| T-013 | research | 지도 SDK와 지도 데이터 선정 | todo | - | P1 | - | - |
| T-014 | feature | 온보딩 화면 구현(SCR-006 동의, SCR-003 관심 조건) | review | - | P0 | SCR-006, SCR-003 | - |
| T-015 | feature | 홈·주행 화면 구현(SCR-001, SCR-004, SCR-009, SCR-005) | review | - | P0 | SCR-001, SCR-004, SCR-009, SCR-005 | T-014 |
| T-016 | feature | 스크랩 기능 구현(SCR-007, SCR-002, Room 저장) | review | - | P0 | SCR-007, SCR-002, ADR-005 | T-015 |
| T-017 | feature | 마이 페이지 구현(SCR-010) | review | - | P1 | SCR-010 | T-016 |
| T-018 | feature | 주행 요약 구현(SCR-008) | review | - | P1 | SCR-008 | T-016 |
| T-019 | feature | 설정 수정 모드 구현(SCR-006) | review | - | P1 | SCR-006 | T-014 |
| T-020 | feature | 국토부 아파트 실거래가 Retrofit 조회 | todo | - | P1 | ADR-006 | - |
| T-021 | feature | 수원 영통구 실거래와 주소 좌표 연결 | review | - | P1 | ADR-007 | T-020 |
| T-022 | debt | 앱 코드를 도메인 데이터 영통구 기능 모듈로 분리 | review | - | P1 | ADR-008 | T-021 |
