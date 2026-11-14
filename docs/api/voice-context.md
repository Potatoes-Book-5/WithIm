---
task: T-040
---
# Voice context 실행 계약

자연어 + Gleo 해석 문맥 → action → 앱 수신 시 context snapshot → 대상 확정 → 데이터 조회 → 명령 수행 → callback → Gleo 응답으로 나눈다. Gleo의 동적 해석 문맥 전달 API는 확인되지 않았으므로 앱 context가 Gleo에 자동 제공된다고 가정하지 않는다.

앱 context는 SCR-005의 보이는 Nudge(단지 ID·key·만료 시각), 현재 표시 화면과 선택 단지, foreground, 온보딩, 위치 동의, 주행·규제 상태, 앱 실행 세션을 기반으로 한다. 앱이 소유하며 Intent extras로 덮어쓸 수 없다. 원문·좌표·프로필을 schema 입력으로 받지 않는다.

SCRAP_APARTMENT는 무인자다. foreground와 온보딩·동의 조건을 확인하고, 현재 상세 단지 → 유효 Nudge 순으로 대상을 확정한다. 두 후보를 임의 순위 목록이나 지도 대장으로 보정하지 않는다. Nudge 종료/대체는 key로 제어한다. foreground·동의 상실은 실행 세대를 무효화한다. 이미 수락된 요청은 확정 단지를 유지하며 새 배너나 차량 이동으로 다른 단지를 저장하지 않는다.

확정 ID의 상세 데이터를 repository에서 조회한 후 ScrapRepository에 저장한다. 기존 스크랩이면 성공을 반환하고 삭제하지 않는다. 실행 context를 fetch 뒤에도 재검증한다. 저장 결과는 Room 관찰을 통해 같은 배너에 반영된다. 저장 도중 timeout/취소의 commit 결과를 임의로 실패 확정하지 않으며, 재요청은 멱등 조회로 복구한다.

가격·지역 저장·브리핑은 기존 dummy API를 유지한다. 단지 스크랩은 실제 로컬 저장이며 debug에서만 외부 호출을 제공한다. release 인증·공개 권한은 여전히 미결이다. 배너 8초와 현재 UX 규칙을 유지하며 자체 청취·자동 발화를 추가하지 않는다.
