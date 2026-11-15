# TTS 입력 기반 검증 — T-043

확인일: 2026-11-15. **Pleos Connect에서 앱 계측 17개는 통과했으며, 실제 Gleo 음성 E2E는 연결 선행 조건 때문에 BLOCKED다.** [기계 판독 결과](runs/2026-11-15-tts.json)는 합성 입력과 앱 소유 이벤트만 포함한다.

## 실행 결과

| 검증 계층 | 결과 | 증명하는 범위 |
|---|---|---|
| Android 빌드·lint·JVM | PASS, JVM 155개, 실패/오류/skip 0 | 현재 앱 로직과 빌드 |
| Python 검증 도구 | PASS, 17개 | PCM 형식·패킷 보존, 마이크 차단, 세션 소유, 중복/과거 로그 거부, 실패 시 정리 |
| Pleos Gleo 경계 계측 | PASS, 10개 | Android Intent/decoder/receiver/callback, Nudge→준비 데이터→in-memory Room 저장·재시도, 비차단 조회 |
| Pleos 전체 앱 계측 | PASS, 17개, 5.442초 | 위 10개와 Room 저장·조회·매핑/마이그레이션 회귀 |
| 생성 음원→가상 마이크 | RPC OK, 3개 | Yuna 한국어 PCM16/16kHz/mono 주입. 실제 STT 수신은 미확인 |
| 실제 음성→Gleo→앱 | 최초 3개 INCONCLUSIVE, 보호 절차 적용 후 3개 BLOCKED | 시스템 dump의 `mBound=true mService=null`, 앱 action/callback 없음 |
| 최종 Gleo 발화 | 미검증 | transcript·숫자 읽기·최종 음성·오디오 포커스는 아직 확인 불가 |

프로세스 재시작 후 저장 보존은 이번 계측으로 검증하지 않았다.

Pleos에서 데이터 수집/지오코딩 작업을 대기시킨 상태의 준비 데이터 조회 **1,000회 = 10ms**였다. 이는 메모리 읽기 경로의 회귀 증거이며, STT·LLM·네트워크·TTS가 포함된 음성 전체 응답 시간이나 차량 성능 수치가 아니다.

사용 환경은 `Pleos_Connect_v2.1.0`의 read-only 인스턴스뿐이다. 일반 Phone은 실행하지 않았다. 테스트 후 해당 인스턴스와 loopback 오디오 listener를 종료했고 호스트 마이크를 끈 상태로 정리했다. 실사용 계정/DB/키 또는 Gleo private DB를 변경하지 않았다.

## 다시 실행하기

저장소 루트에서, 기존 JDK/SDK와 Python 환경을 사용한다. 의존성 준비·옵션은 [도구 설명](../../../harness/voice/README.md)을 따른다.

```sh
python3 -m unittest discover -s harness/voice -p 'test_*.py' -v
./harness/verify.sh
python3 harness/voice/run-pleos.py --out /tmp/gleo-voice --instrumentation all
```

실행마다 고유 폴더를 만들며 `run.json`(전체), `results.json`(발화별), `app-boundary.json`/`instrumentation.txt`(별도 계측)을 남긴다. setup 실패에도 새 보고서를 남긴다. `BLOCKED`나 `INCONCLUSIVE`는 종료 코드 2이며 정상 음성 성공으로 계산하지 않는다. `ACTION_CALLBACK_MATCHED`여도 transcript/최종 발화 검증은 false다.

세 합성 발화는 [cases.json](../../../harness/voice/cases.json)에 있다. 현재 위치 집값·부동산 브리핑은 debug dummy 응답을 기대한다. “이 단지 스크랩해 줘”는 선택 상세와 보이는 Nudge가 없는 Home에서 asking을 기대한다. 실제 문맥이 생겨 성공 저장되면 이 음성 케이스에는 mismatch다. 성공 저장은 별도 in-memory Room 계측으로 재현하므로 운영 데이터를 만들 필요가 없다.

## 기존 90개 TC와의 관계

원본은 [test-cases.csv](test-cases.csv)다. 아래는 **하위 계층 증거의 연결**이며 제품 TC 실행 결과를 복제하지 않는다. 이번 작업에서 제품 TC에 새 PASS를 부여하지 않았다.

| 관련 제품 TC | 확보한 하위 증거 | 남은 제품/음성 조건 |
|---|---|---|
| TC-009~020 중 평균·필터·취소·정정·캐시 | [PreparedMarketTest](../../../core/data/src/test/java/com/potatoes/book5/realestate/data/repository/PreparedMarketTest.kt), 준비 데이터 계측 | 실제 가격 함수에 read model 연결, 법정동/위치/면적·기간/캐시 정책 전체 일치, 최종 답변 검증. 현재 음성 가격은 dummy |
| TC-023~026, TC-075/078의 유사 문맥·저장 조건 | [VoiceContextTest](../../../core/data/src/test/java/com/potatoes/book5/realestate/data/repository/VoiceContextTest.kt), [VoiceScrapE2ETest](../../../app/src/androidTest/java/com/potatoes/book5/realestate/vehicle/gleo/VoiceScrapE2ETest.kt) | 현재 검증 대상은 **단지 스크랩**. 원본의 **관심 지역 저장**은 별도 기능이며 이 결과로 대체 불가 |
| TC-058~067 | dummy 브리핑 Intent/callback의 3개 합성 항목 | 실제 소식 수집·정정·읽음 체크포인트·자동형 Gate·Gleo 발화 |
| TC-068/069/071 | [GleoWireTest](../../../app/src/androidTest/java/com/potatoes/book5/realestate/vehicle/gleo/GleoWireTest.kt), validator/mapper JVM | 등록 환경의 세 함수 인식·함수 선택·파라미터와 지역 확인 확장. 현재 세 함수는 가격/단지 스크랩/브리핑 |
| TC-070 | [GleoRequestCoordinatorTest](../../../app/src/test/java/com/potatoes/book5/realestate/vehicle/gleo/GleoRequestCoordinatorTest.kt)의 timeout/늦은 응답/단일 요청 | 실제 Gleo 기한·콜백 수락 관측 |
| TC-072/074 | callback `mode=silent` 인코딩 assertion | Gleo와 앱의 실제 소리 중복·숫자·단위·근거·최종 발화 |
| TC-077 | debug 이벤트에 action/status/persisted만 기록 | 전체 차량·공급자·제품 로그 개인정보 검증 |
| TC-081 | 준비 데이터 1,000회 조회 10ms | 실제 UX 피드백 250ms·전체 음성 기한·차량 CPU/RAM |
| 나머지 공통 상태·지도/분석·영역 진입·내비·카메라·자동 브리핑·전체 여정·지역 확인 | 이번 TTS 검증의 직접 인수 증거 없음 | 해당 TC의 선행 조건과 API/권한/지원 Gate를 따로 실행 |

기존 Android/JVM 테스트 중 주행·정책·UI reducer 등도 실행했지만, 제품 CSV의 전체 절차·차량 UI·발화 결과까지 수행한 것으로 보지 않는다. 카메라/자동형/지역 확인처럼 Gate가 있는 기능은 테스트 fixture만으로 활성화하지 않는다.

## 막힌 지점과 다음 검증

공개 시스템 진단에서 Gleo assistant component는 등록되어 있으나 서비스 연결은 null이며, 호출 후 Gleo 화면과 앱 action 이벤트가 없었다. 오디오 RPC 수락으로는 이 원인을 좁힐 수 없다. 로그인·네트워크·이미지 초기화·스키마 승인 중 어느 것이 원인인지는 아직 확인하지 않았다.

다음에는 Pleos의 정상 Gleo 화면/음성 세션과 앱의 실제 등록 스키마를 먼저 확인하고 같은 명령을 재실행한다. 그 뒤 안전한 합성 대상 Nudge를 제품 테스트 fixture로 제공해 **음성 스크랩 성공→DB/화면 갱신**까지 확장한다. 현재 도구는 binder 존재만으로 STT listening을 증명하지 않으므로 Gleo가 허용하는 transcript/최종 응답 관측 경로도 별도 확보해야 한다. vendor private DB나 인증 우회는 사용하지 않는다.

## 리뷰

독립 리뷰에서 이전 결과 재사용과 manifest 기록 실패 시 정리 누락 두 건을 수정했다. 고유 결과 디렉터리, fresh run 보고서, process 생성 직후 정리 보호, 10/17개 완료 수 검사, null 연결의 BLOCKED 처리를 재검토했다. 리뷰 재실행에서도 호스트 17개 통과, 추가 차단 지적 없음.
