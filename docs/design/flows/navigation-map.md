# 화면 흐름

```mermaid
flowchart LR
    Start([첫 실행]) --> Consent[SCR-006 동의]
    Consent --> Cond[SCR-003 관심 조건]
    Cond --> Home[SCR-001 홈]
    Home --> Detail[SCR-002 상세]
    Home --> Scrap[SCR-007 스크랩]
    Scrap --> Detail
    Home --> MyPage[SCR-010 마이 페이지]
    MyPage --> Scrap
    MyPage --> Cond
    Home --> Consent
    Home -. 주행 시작 .-> Drive[SCR-004 주행 중 순위]
    Drive <--> Map[SCR-009 지도 모드]
    Map --> Alert
    Drive --> Alert[SCR-005 알림]
    Alert --> Drive
    Drive -. 주행 종료 .-> Summary[SCR-008 주행 요약]
    Summary --> Scrap
    Summary --> Home
```

## 규칙

- 주행 중에는 SCR-004, SCR-009, SCR-005만 표시합니다. 다른 화면에 있었다면 상태를 저장하고 전환합니다(S-005).
- 다단계 이동은 홈에서 최대 2단계로 제한합니다(DD-009, 제안값).

## S-011 주차 탐색 확장

```mermaid
flowchart LR
    Home[SCR-001 홈] --> Areas[SCR-011 관심 지역]
    Home --> Brief[SCR-012 브리핑]
    Areas --> Detail[SCR-002 상세 분석]
    Areas --> Brief
    Brief --> Areas
    Detail --> Navi[시스템 내비 요청]
```

- 위 경로는 주차 준비 완료에서의 정보 구조 초안이다. 신규 와이어프레임/Figma는 아직 없다.
- 주행/신호 대기에서 상세·지역 목록·브리핑 상세 진입을 차단한다. 조건부 간소 카드만 기존 SCR-004/SCR-005에서 다룬다.
- 상세→내비 요청 결과와 시스템 Navigation 화면 표시는 F-012에 따른다. 일반 앱이 카메라 화면을 강제로 열지 않는다.
- 카테고리/DO/주행 허용은 [상태 매트릭스](../../product/feature-state-matrix.md)가 우선한다.
