# 식구 (Sik-Gu)

> 대학생 자취생을 위한 식재료·생필품 소분 공동구매 플랫폼

혼자 먹기엔 너무 많은 대용량 상품, 같은 동네 학생들과 단위별로 나눠 사요.
학교 이메일로 인증된 이웃끼리만 거래하고, 서비스 내 포인트로 결제와 정산을 처리해 돈 문제와 신뢰 문제를 줄입니다.

예) 코스트코 생수 2L × 24개(29,040원) → 4병씩 6단위로 나눠 **1단위 4,840원**

<br>

## 주요 기능

| 기능 | 설명 |
| --- | --- |
| 학교 이메일 인증 | 아이디 + 학교 이메일 인증으로 가입, 동네(○○동) 설정 |
| 내 동네 공구 찾기 | 거리(1·3·5km), 카테고리, 검색, 정렬(최신·마감임박·인기·가격) |
| 공구 열기 | 기본 단위·총 단위 수·총 가격을 입력하면 단위당 가격 자동 계산 |
| 포인트 결제 | 카카오페이·토스페이·카드로 충전, 공구 참여 시 즉시 결제 |
| 진행 상태 관리 | 모집 → 주문 → 배송 → 나눔 → 수령 확인 → 정산 단계별 추적 |
| 영수증 확인 | 호스트가 영수증·송장 등록, 참여자가 금액 일치 여부 확인 |
| 공구 채팅 | 공구 글마다 채팅방 자동 개설, 나눔 일정·장소 조율 |
| 후기 · 별점 | 호스트 ↔ 참여자 양방향 별점 + 태그, 프로필에 평균 별점 표시 |
| 신고 · 차단 | 사용자 신고(사유 선택 + 증빙), 차단 |

> 위치는 동 단위로만 노출되고, 닉네임은 랜덤으로 부여됩니다.

<br>

## 공구 진행 흐름

```
모집중 ──▶ 모집완료 ──▶ 주문완료 ──▶ 배송중
(참여=결제)              (영수증·송장)   (영수증 확인)
   │                                      │
   ▼                                      ▼
인원 미달                  정산 완료 ◀── 수령 확인 ◀── 나눔 완료
→ 취소·자동 환급         (호스트 정산    (전원 '받았어요')  (직접 전달)
                          + 인센티브)
```

- 참여자가 결제한 포인트는 바로 호스트에게 가지 않고 **보류(HOLD)** 상태로 묶여 있다가 모집이 완료되면 확정됩니다.
- 모집 기한 내 인원이 차지 않으면 공구가 취소되고 포인트는 자동 환급됩니다.
- 참여자 전원이 수령을 확인하면 호스트에게 정산되고 인센티브 포인트가 지급됩니다.

<br>

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Frontend | React |
| Backend | Spring Boot |
| Database | PostgreSQL, Redis |
| Architecture | Layered + MVC, Monolithic |
| Payment | PG 테스트 모드 연동 (포인트 충전) |
| Infra | Docker (검토 중) |

<br>

## 문서

- 와이어프레임: [Figma](https://www.figma.com/design/aknoKACSnNAOCu4k0qRqdP/Sik-Gu)
- ERD: [ERDCloud](https://www.erdcloud.com/p/35ycXvYR5RtYJnAwi)
- API 명세서 · 개발 일지: 팀 Google Docs

### API 개요

| 도메인 | Endpoint |
| --- | --- |
| 인증 | `POST /api/auth/signup` · `POST /api/auth/login` · `POST /api/auth/email/send` · `POST /api/auth/email/verify` |
| 공구 | `GET /api/posts` · `GET /api/posts/search` · `GET /api/posts/category` · `POST /api/posts` · `GET·POST·DELETE /api/posts/{postId}` |
| 공구 진행 | `PATCH /api/posts/{postId}/status` · `PATCH /api/posts/{postId}/status/me` · `POST /api/posts/{postId}/receipt` |
| 사용자 | `GET /api/users/me` · `GET /api/users/{userId}` |
| 포인트 | `POST /api/points/pay` |
| 채팅 | `POST /api/chats/list` · `POST /api/chats/{roomId}` |

<br>

## 시작하기

> TODO: 레포 구조와 환경 변수가 정해지면 채워주세요.

```bash
# Backend (이 레포)
./gradlew bootRun

# Frontend → https://github.com/Sik-Gu/Frontend
npm install
npm run dev
```
