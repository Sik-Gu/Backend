# 식구(Sik-Gu) API 명세서

> 출처: 팀 Google Doc "식구(Sik-Gu) API 명세서" 탭
> 원본 명세를 기준으로 정리하고, **DB 설계([DB.md](DB.md))에서 정한 결정과 충돌하는 부분은 DB 쪽에 맞춰 수정**했습니다.
>
> 표기
> - `(보완)`: 원본에 없던 요청·응답 형식이나 에러를 새로 채운 것
> - `(가정)`: 값이 정해지지 않아 기본값을 제안한 것 (팀 확정 필요)
> - `[D번호]`: DB.md 결정 기록에 맞춰 원본에서 수정한 것 (예: `[D9]`)
>
> 원본에서 무엇을 어떻게 바꿨는지는 [11. DB 결정에 맞춰 수정한 내용](#11-db-결정에-맞춰-수정한-내용)에 모아뒀습니다.

## 목차

- [0. 공통 규칙](#0-공통-규칙)
- [1. 에러 코드](#1-에러-코드)
- [2. ENUM](#2-enum)
- [3. 엔드포인트 요약](#3-엔드포인트-요약)
- [4. 인증·회원](#4-인증회원)
- [5. 이미지](#5-이미지)
- [6. 공구 게시글](#6-공구-게시글)
- [7. 공구 진행](#7-공구-진행)
- [8. 포인트](#8-포인트)
- [9. 채팅](#9-채팅)
- [10. 후기·신고·차단](#10-후기신고차단)
- [11. DB 결정에 맞춰 수정한 내용](#11-db-결정에-맞춰-수정한-내용)
- [12. 남은 미정 사항](#12-남은-미정-사항)

---

## 0. 공통 규칙

### 0.1 기본

| 항목 | 규칙 |
|---|---|
| Base URL | `/api` (채팅 WebSocket만 `/ws-stomp`) |
| 데이터 형식 | `application/json; charset=UTF-8`. 이미지 업로드만 `multipart/form-data` |
| 인증 | 로그인 후 받은 Access Token을 `Authorization: Bearer {token}` 헤더로 보냄. 권한이 **공개**인 API만 토큰 없이 호출 가능 |
| 날짜 | ISO 8601 (`2026-10-27T14:00:00`), 날짜만 쓸 때 `2026-10-27` |
| 금액·포인트 | 정수. 1원 = 1P |
| 이미지 | `POST /api/images`로 먼저 올리고 받은 URL을 각 API 요청에 넣음 |
| 게시글 상태 | 프론트가 상태값을 직접 보내는 API는 없음. 상태는 참여·주문 등록 같은 행위 API나 스케줄러 결과로만 바뀜 |
| 위치 노출 | 다른 사용자에게는 동 이름과 거리만 보여주고 정확한 좌표는 보내지 않음 [D14] |

### 0.2 권한

| 권한 | 의미 | 위반 시 |
|---|---|---|
| 공개 | 토큰 없이 호출 가능 | - |
| 로그인 | 유효한 Access Token 필요 | `AUTH401` |
| 호스트 | 해당 게시글을 연 사람 (`role = HOST`) | `AUTH403` |
| 참여자 | 해당 게시글에 `PARTICIPANT`로 참여한 사람 (호스트 제외) | `AUTH403` |
| 호스트·참여자 | 해당 게시글의 HOST 또는 PARTICIPANT | `AUTH403` |
| 방 참여자 | 해당 채팅방에 입장한 사람 | `AUTH403` |

### 0.3 응답 포맷

**성공**
```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공",
  "result": { }
}
```

**실패** — `result`는 `null`. 요청 값 검증 실패(`COMMON400`)일 때만 필드별 오류를 담음 (보완)
```json
{
  "isSuccess": false,
  "code": "COMMON400",
  "message": "요청 값이 올바르지 않습니다.",
  "result": {
    "errors": [
      { "field": "units", "reason": "1 이상이어야 합니다." }
    ]
  }
}
```

### 0.4 페이지네이션

목록은 `page`(0부터), `size`(기본 20). 채팅 메시지만 커서 방식(`cursor`, `size`).

```json
"result": {
  "content": [ ],
  "page": 0,
  "size": 20,
  "totalElements": 24,
  "hasNext": true
}
```

---

## 1. 에러 코드

원본에서 정의한 7개는 의미를 그대로 유지하고, 나머지는 (보완)입니다.
형식: `도메인 + HTTP 상태 3자리 + 일련번호 1자리`. 세 자리로 끝나는 코드(`POST409` 등)는 그 계열의 대표 코드입니다.

### 1.1 공통·인증

| HTTP | code | 상황 | 출처 |
|---|---|---|---|
| 200 | `COMMON200` | 성공 | 원본 |
| 400 | `COMMON400` | 요청 값 검증 실패 (필수 누락, 형식·범위 오류, 없는 ENUM 값) | 원본 |
| 404 | `COMMON404` | 대상 없음 (게시글·사용자·채팅방·주문 등) | 원본 |
| 500 | `COMMON500` | 서버 내부 오류 | 보완 |
| 401 | `AUTH401` | 토큰 없음·만료·위조 | 원본 |
| 403 | `AUTH403` | 권한 없음 (호스트 전용 API를 참여자가 호출 등) | 원본 |
| 400 | `AUTH4001` | 학교 이메일 형식이 아님 | 보완 |
| 400 | `AUTH4002` | 인증코드 불일치 | 보완 |
| 400 | `AUTH4003` | 인증코드 만료 또는 발송 이력 없음 | 보완 |
| 400 | `AUTH4004` | `verificationToken`이 유효하지 않거나 만료 | 보완 |
| 401 | `AUTH4011` | 로그인 실패 (아이디 또는 비밀번호 불일치) | 보완 |
| 401 | `AUTH4012` | Refresh Token 무효·만료 | 보완 |
| 403 | `AUTH4031` | **정지된 계정** (미수금 기한 초과 누적 10회) | 보완 [D9] |

### 1.2 회원·위치

| HTTP | code | 상황 | 출처 |
|---|---|---|---|
| 409 | `USER409` | 아이디·이메일 중복 | 원본 |
| 404 | `LOC4041` | 검색어·좌표에 맞는 동이 없음 | 보완 |

### 1.3 이미지

| HTTP | code | 상황 | 출처 |
|---|---|---|---|
| 400 | `IMAGE4001` | 허용하지 않는 파일 형식 | 보완 |
| 400 | `IMAGE4002` | 파일이 없거나 개수 초과 | 보완 |
| 413 | `IMAGE4131` | 파일 용량 초과 | 보완 |

### 1.4 게시글·참여·주문·문의

| HTTP | code | 상황 | 출처 |
|---|---|---|---|
| 409 | `POST409` | 현재 공구 상태에서 할 수 없는 요청 | 원본 |
| 400 | `POST4001` | `hostUnits`가 범위를 벗어남 (1 이상 `totalUnits - 1` 이하) | 보완 |
| 400 | `POST4002` | `deadlineAt`이 과거 | 보완 |
| 409 | `POST4091` | 수정 불가 (모집중이 아니거나 호스트 외 참여자가 있음) | 보완 |
| 409 | `POST4092` | 취소 불가 (주문완료 이후) | 보완 |
| 400 | `PART4001` | 신청 단위가 남은 단위를 초과 | 원본(400) |
| 409 | `PART4091` | 이미 참여한 공구 | 원본(409) |
| 409 | `PART4092` | 호스트는 참여 취소 불가 (공구 취소 API 사용) | 보완 |
| 409 | `PART4093` | 이미 '받았어요'를 누름 | 보완 |
| 403 | `PART4031` | 호스트와 차단 관계라 참여 불가 | 보완 |
| 409 | `ORDER4091` | 이미 주문 정보가 등록됨 | 보완 |
| 400 | `INQ4001` | `SHORT_QUANTITY`인데 `receivedUnits`가 없거나 범위 오류 | 보완 |
| 400 | `INQ4002` | `WRONG_OR_DAMAGED`인데 이미지가 없음 | 보완 |
| 409 | `INQ4091` | 이미 '받았어요'를 눌러 문의할 수 없음 | 보완 |

### 1.5 포인트

| HTTP | code | 상황 | 출처 |
|---|---|---|---|
| 409 | `POINT409` | 포인트 잔액 부족 | 원본 |
| 403 | `POINT4031` | **미납 미수금이 있어 공구 참여·등록 불가** | 보완 [D9] |
| 400 | `POINT4001` | 충전 승인 금액이 준비 단계 금액과 다름 | 원본(400) |
| 409 | `POINT4091` | 동시 변경 충돌 (재시도 후에도 실패) | 보완 [D4] |
| 502 | `POINT5021` | PG 승인 실패 | 보완 |

### 1.6 채팅

| HTTP | code | 상황 | 출처 |
|---|---|---|---|
| 400 | `CHAT4001` | 메시지 내용이 비었거나 너무 김, 또는 `IMAGE`인데 `imageUrl` 없음 | 보완 [D13] |
| 409 | `CHAT4091` | 종료된 채팅방에 메시지 전송 | 보완 |

### 1.7 후기·신고·차단

| HTTP | code | 상황 | 출처 |
|---|---|---|---|
| 400 | `REVIEW4001` | 평가할 수 없는 대상 (본인 등) | 보완 |
| 409 | `REVIEW4091` | 같은 대상에게 이미 후기를 씀 | 보완 |
| 409 | `REVIEW4092` | **작성 기간이 아님** (정산완료 전이거나 정산 후 3일 지남) | 보완 [D12] |
| 400 | `REPORT4001` | 본인을 신고함 | 보완 |
| 400 | `REPORT4002` | 사유별 필수 값 누락 | 보완 |
| 400 | `BLOCK4001` | 본인을 차단함 | 보완 |
| 409 | `BLOCK4091` | 이미 차단한 사용자 | 보완 |

---

## 2. ENUM

| ENUM | 값 | 비고 |
|---|---|---|
| `PostStatus` | `RECRUITING` 모집중 → `RECRUITED` 모집완료 → `ORDERED` 주문완료 → `SHIPPING` 배송중 → `DISTRIBUTED` **나눔 진행·수령확인 대기** → `SETTLED` 정산완료, 예외 `CANCELLED` | `DISTRIBUTED` 의미 수정 [D6] |
| `Category` | `FOOD` 식품, `LIVING` 생활용품, `DAILY` 생필품, `ETC` 기타 | |
| `ParticipationRole` | `HOST`, `PARTICIPANT` | |
| `PaymentStatus` | `PAID`, `REFUNDED` | |
| `ReceiptStatus` | `WAITING` 확인 대기, `RECEIVED` 받았어요, `INQUIRING` 문의중 | |
| `HoldStatus` | `HELD` 보관중, `CAPTURED` 정산됨, `RELEASED` 환급됨 | 응답에 직접 노출 안 함 |
| `PointTxType` | `CHARGE` 충전, `PAYMENT` 결제, `REFUND` 환급, `SETTLEMENT` 정산, `ADJUSTMENT_REFUND` 차액 환급, `ADJUSTMENT_CHARGE` 차액 추가 청구, `DEBT_REPAYMENT` 미수금 납부 | `INCENTIVE` 삭제 [D11], 차액·미수금 추가 [D9] |
| `ChargeMethod` | `KAKAOPAY`, `TOSSPAY`, `CARD` | |
| `DebtStatus` | `UNPAID` 미납, `PAID` 납부 완료, `WRITTEN_OFF` 종결 | 신규 [D9] |
| `PostSort` | `LATEST` 최신, `DEADLINE` 마감임박, `POPULAR` 인기(조회수), `PRICE` 가격(단위당 가격 낮은 순) | |
| `MyPostTab` | `ONGOING` 진행중, `DONE` 완료 | |
| `MyPostFilter` | `ALL`, `HOST` 내가 연 공구, `JOINED` 참여한 공구, `ACTION_REQUIRED` 액션 필요 | |
| `ActionRequired` | `ORDER`, `SHIP`, `DISTRIBUTE`, `CONFIRM_RECEIPT`, `WRITE_REVIEW`, `PAY_DEBT` | `PAY_DEBT` 추가 [D9] |
| `InquiryType` | `NOT_RECEIVED` 못 받음, `SHORT_QUANTITY` 수량 부족, `WRONG_OR_DAMAGED` 다른 상품·파손, `ETC` 기타 | |
| `DesiredResolution` | `REDELIVER` 다시 받기, `PARTIAL_REFUND` 부분 환불, `FULL_REFUND` 전액 환불 | 참여자의 희망 사항 |
| `InquiryStatus` | `OPEN` 접수, `IN_PROGRESS` 운영팀 처리중, `RESOLVED` 처리 완료 | 신규 [D10] |
| `InquiryResolution` | `REDELIVER`, `PARTIAL_REFUND`, `FULL_REFUND`, `REJECTED` 문제 없음 | 운영팀 최종 결정 [D10] |
| `ChatRoomStatus` | `ONGOING`, `CLOSED` | |
| `MessageType` | `TEXT`, `IMAGE`, `SYSTEM` | `IMAGE` 추가 [D13] |
| `ImageType` | `POST`, `RECEIPT`, `INQUIRY`, `REPORT`, `CHAT` | 업로드 용도 (보완), `CHAT` [D13] |
| `ReviewTag` | `PUNCTUAL` 시간 약속, `CLEAN_SPLIT` 소분 깔끔, `FAST_REPLY` 응답 빠름, `FAST_PAYMENT` 결제 빠름 | |
| `ReportReason` | `POST_HARMFUL`, `CHAT_ABUSE`, `TRADE_VIOLATION`, `ETC` | 신고 대상은 항상 사용자 |
| `ReportStatus` | `PENDING` 처리 전, `DONE` 처리 완료 | |

---

## 3. 엔드포인트 요약

| Method | URL | 설명 | 권한 | 화면 |
|---|---|---|---|---|
| POST | `/api/auth/email/send` | 학교 이메일 인증코드 발송 | 공개 | 01 |
| POST | `/api/auth/email/verify` | 인증코드 확인 → `verificationToken` | 공개 | 01 |
| GET | `/api/auth/check-id` | 아이디 중복확인 | 공개 | 01 |
| GET | `/api/locations/search` | 동 이름 검색 | 공개 | 01 |
| GET | `/api/locations/reverse` | 현재 위치 → 동 이름 | 공개 | 01 |
| POST | `/api/auth/signup` | 회원가입 (닉네임 랜덤) | 공개 | 01 |
| POST | `/api/auth/login` | 로그인 → 토큰 발급 | 공개 | 01 |
| POST | `/api/auth/reissue` | 토큰 재발급 | 공개 | 공통 |
| POST | `/api/auth/logout` | 로그아웃 | 로그인 | 07 |
| GET | `/api/users/me` | 내 프로필 + 공구 수 + 포인트 | 로그인 | 07 |
| PATCH | `/api/users/me/location` | 동네 변경 | 로그인 | 07, 02 |
| GET | `/api/users/me/posts` | 내 공구함 | 로그인 | 07 |
| GET | `/api/users/{userId}` | 유저 프로필 모달 | 로그인 | 08 |
| POST | `/api/images` | 이미지 업로드 → URL | 로그인 | 04, 03, 05, 08 |
| GET | `/api/posts` | 공구 목록·검색·필터 | 로그인 | 02 |
| GET | `/api/posts/{postId}` | 공구 상세 (조회수 +1) | 로그인 | 03 |
| POST | `/api/posts` | 공구 등록 (채팅방 자동 개설, **호스트 몫 결제**) | 로그인 | 04 |
| PATCH | `/api/posts/{postId}` | 공구 수정 | 호스트 | 04 |
| DELETE | `/api/posts/{postId}` | 공구 취소 (전원 환급) | 호스트 | 03 |
| POST | `/api/posts/{postId}/participations` | 참여 + 즉시 결제(보류) | 로그인 | 03 |
| DELETE | `/api/posts/{postId}/participations/me` | 참여 취소 + 환급 | 참여자 | 03 |
| GET | `/api/posts/{postId}/participations` | 결제·수령 현황 | 호스트·참여자 | 03-E |
| POST | `/api/posts/{postId}/order` | 영수증 등록 + 주문 완료 + **차액 정산** | 호스트 | 03-C |
| GET | `/api/posts/{postId}/order` | 주문 정보·영수증·차액 | 호스트·참여자 | 03-C, D, E |
| PATCH | `/api/posts/{postId}/order/shipping` | 택배사·송장 + 배송 시작 | 호스트 | 03-C |
| PATCH | `/api/posts/{postId}/order/distribution` | **나눔 일정·장소 입력** | 호스트 | 03-C |
| POST | `/api/posts/{postId}/receipt` | 받았어요 | 참여자 | 03-E |
| POST | `/api/posts/{postId}/inquiries` | 수령 문제 문의 | 참여자 | 03-F |
| GET | `/api/posts/{postId}/inquiries` | 문의 목록 | 호스트·참여자 | 03-C, 03-F |
| GET | `/api/points/me` | 잔액·보관중 금액·**미수금** | 로그인 | 06 |
| GET | `/api/points/debts` | **미수금 목록** (보완) [D9] | 로그인 | 06 |
| POST | `/api/points/charges` | 충전 준비 (주문번호 발급) | 로그인 | 06 |
| POST | `/api/points/charges/confirm` | PG 승인 → 포인트 적립 (**미수금 자동 차감**) | 로그인 | 06 |
| GET | `/api/points/transactions` | 거래 내역 | 로그인 | 06, 07 |
| GET | `/api/chats` | 채팅방 목록 | 로그인 | 05 |
| GET | `/api/chats/{chatRoomId}` | 방 헤더 + 공구 요약 | 방 참여자 | 05 |
| GET | `/api/chats/{chatRoomId}/messages` | 이전 메시지 (커서) | 방 참여자 | 05 |
| PATCH | `/api/chats/{chatRoomId}/read` | 읽은 시각 갱신 | 방 참여자 | 05 |
| GET | `/api/posts/{postId}/reviews/targets` | 평가 대상·작성 여부 | 호스트·참여자 | 09 |
| POST | `/api/posts/{postId}/reviews` | 후기 작성 (배열) | 호스트·참여자 | 09 |
| GET | `/api/users/{userId}/reviews` | 받은 후기 목록 | 로그인 | 08 |
| POST | `/api/reports` | 사용자 신고 | 로그인 | 08-B, 03-D |
| POST | `/api/users/{userId}/block` | 차단 | 로그인 | 08 |
| DELETE | `/api/users/{userId}/block` | 차단 해제 | 로그인 | 08 |
| GET | `/api/users/me/blocks` | 차단 목록 | 로그인 | 07 |

**WebSocket (STOMP)**

| 구분 | 경로 | 내용 |
|---|---|---|
| 연결 | `/ws-stomp` | CONNECT 헤더에 `Authorization: Bearer {token}` |
| 구독 | `/sub/chats/{chatRoomId}` | 새 메시지 수신 |
| 발행 | `/pub/chats/{chatRoomId}/messages` | 메시지 전송 (텍스트·이미지) [D13] |

---

## 4. 인증·회원

가입은 3단계(이메일 인증 → 아이디·비밀번호 → 동네 설정)지만 서버에 저장하는 건 마지막 `signup` 한 번입니다. 1단계에서 받은 `verificationToken`을 프론트가 들고 다니므로 중간에 이탈해도 빈 계정이 생기지 않습니다.

### POST /api/auth/email/send — 인증코드 발송

- 권한: 공개 · 화면: 01 가입 1단계

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `email` | string | Y | 학교 이메일 (`.ac.kr`) |

```json
{ "email": "haeseung@korea.ac.kr" }
```

**Response** (보완)
```json
{ "email": "haeseung@korea.ac.kr", "expiresInSec": 300 }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | 이메일 형식 오류 |
| 400 | `AUTH4001` | 학교 이메일이 아님 |
| 409 | `USER409` | 이미 가입된 이메일 |

### POST /api/auth/email/verify — 인증코드 확인

- 권한: 공개 · 화면: 01 가입 1단계

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `email` | string | Y | 인증코드를 받은 이메일 |
| `code` | string | Y | 숫자 6자리 |

```json
{ "email": "haeseung@korea.ac.kr", "code": "482913" }
```

**Response**
```json
{ "verificationToken": "eyJ...", "expiresInSec": 1800 }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `AUTH4002` | 인증코드 불일치 |
| 400 | `AUTH4003` | 인증코드 만료 또는 발송 이력 없음 |

### GET /api/auth/check-id — 아이디 중복확인

- 권한: 공개 · 화면: 01 가입 2단계

**Query**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `loginId` | string | Y | 확인할 아이디 |

**Response** (보완) — 중복이어도 HTTP 200에 `available: false`
```json
{ "loginId": "haeseung01", "available": true }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | `loginId` 누락·형식 오류 |

### GET /api/locations/search — 동 이름 검색

- 권한: 공개 · 화면: 01 가입 3단계, 07 동네 설정
- 동 이름 검색과 현재 위치(GPS) **둘 다 지원**합니다 [D14]

**Query**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `query` | string | Y | 동 이름 검색어 |

**Response** (보완) — 결과가 없으면 빈 배열
```json
[
  { "dongName": "역삼동", "fullAddress": "서울특별시 강남구 역삼동", "latitude": 37.5006, "longitude": 127.0364 }
]
```

`latitude`·`longitude`는 그 동의 중심 좌표입니다 [D14].

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | `query` 누락 |

### GET /api/locations/reverse — 현재 위치 → 동 이름

- 권한: 공개 · 화면: 01 가입 3단계, 07 동네 설정

**Query**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `lat` | double | Y | 위도 |
| `lng` | double | Y | 경도 |

**Response** (보완)
```json
{ "dongName": "역삼동", "fullAddress": "서울특별시 강남구 역삼동", "latitude": 37.5006, "longitude": 127.0364 }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | 좌표 누락·범위 오류 |
| 404 | `LOC4041` | 해당 좌표에 맞는 동이 없음 |

### POST /api/auth/signup — 회원가입

- 권한: 공개 · 화면: 01
- 닉네임은 서버가 랜덤으로 줍니다. 비밀번호는 BCrypt로 저장합니다 [D7]. 가입 시 포인트 지갑(잔액 0)을 함께 만듭니다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `verificationToken` | string | Y | 이메일 인증 후 받은 토큰 |
| `loginId` | string | Y | 아이디 |
| `password` | string | Y | 비밀번호 |
| `location.dongName` | string | Y | 동 이름 |
| `location.latitude` | double | Y | 위도 |
| `location.longitude` | double | Y | 경도 |

```json
{
  "verificationToken": "eyJ...",
  "loginId": "haeseung01",
  "password": "********",
  "location": { "dongName": "역삼동", "latitude": 37.5006, "longitude": 127.0364 }
}
```

**Response**
```json
{ "userId": 12, "nickname": "배고픈 고구마" }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | 필수 값 누락·형식 오류 |
| 400 | `AUTH4004` | `verificationToken` 무효·만료 |
| 409 | `USER409` | 아이디 또는 이메일 중복 |

### POST /api/auth/login — 로그인

- 권한: 공개 · 화면: 01
- `BCryptPasswordEncoder.matches()`로 비밀번호를 비교하고, 일치하면 토큰을 발급합니다 [D7]

**Request Body**

| 필드 | 타입 | 필수 |
|---|---|---|
| `loginId` | string | Y |
| `password` | string | Y |

```json
{ "loginId": "haeseung01", "password": "********" }
```

**Response**
```json
{ "accessToken": "eyJ...", "refreshToken": "eyJ...", "userId": 12, "nickname": "배고픈 고구마" }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 401 | `AUTH4011` | 아이디 또는 비밀번호 불일치 (둘을 구분하지 않음) |
| 403 | `AUTH4031` | 정지된 계정 [D9] |

### POST /api/auth/reissue — 토큰 재발급

- 권한: 공개 · Access Token이 만료돼 `AUTH401`을 받으면 호출

**Request Body** (보완)
```json
{ "refreshToken": "eyJ..." }
```

**Response** (보완)
```json
{ "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 401 | `AUTH4012` | Refresh Token 무효·만료 |
| 403 | `AUTH4031` | 정지된 계정 [D9] |

### POST /api/auth/logout — 로그아웃

- 권한: 로그인 · 화면: 07
- 바디 없음. 응답 `result`는 `null`

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 401 | `AUTH401` | 토큰 없음·만료 |

### GET /api/users/me — 내 정보

- 권한: 로그인 · 화면: 07, 02
- 원본은 "내 프로필 + 공구 수 + 포인트"까지만 정함. 응답 형식은 (보완)

**Response**
```json
{
  "userId": 12,
  "loginId": "haeseung01",
  "nickname": "배고픈 고구마",
  "email": "haeseung@korea.ac.kr",
  "universityName": "○○대학교",
  "location": { "dongName": "역삼동", "latitude": 37.5006, "longitude": 127.0364 },
  "joinedAt": "2026-03-02",
  "rating": 4.9,
  "reviewCount": 18,
  "tradeCount": 23,
  "postCounts": { "ongoing": 2, "done": 21, "actionRequired": 1 },
  "point": { "availableBalance": 12000, "heldAmount": 4840, "unpaidDebt": 0 },
  "restricted": false
}
```

| 필드 | 설명 |
|---|---|
| `point.unpaidDebt` | 미납 미수금 합계 [D9] |
| `restricted` | 미납 미수금이 있어 공구 참여·등록이 막혔는지 [D9] |

**Errors**: `AUTH401`

### PATCH /api/users/me/location — 동네 변경

- 권한: 로그인 · 화면: 07, 02
- 이후 공구 목록의 거리 계산 기준이 바뀝니다. 이미 등록한 게시글의 거래 장소는 바뀌지 않습니다.

**Request Body**

| 필드 | 타입 | 필수 |
|---|---|---|
| `dongName` | string | Y |
| `latitude` | double | Y |
| `longitude` | double | Y |

```json
{ "dongName": "역삼동", "latitude": 37.5006, "longitude": 127.0364 }
```

**Response** (보완)
```json
{ "dongName": "역삼동", "latitude": 37.5006, "longitude": 127.0364 }
```

**Errors**: `COMMON400`, `AUTH401`

### GET /api/users/me/posts — 내 공구함

- 권한: 로그인 · 화면: 07

**Query**

| 파라미터 | 타입 | 필수 | 기본값 |
|---|---|---|---|
| `tab` | MyPostTab | N | `ONGOING` |
| `filter` | MyPostFilter | N | `ALL` |
| `page`, `size` | int | N | `0`, `20` |

**Response** — `content[]` 한 개는 PostCard([GET /api/posts](#get-apiposts--공구-목록검색필터))에 아래 필드를 더한 것
```json
{
  "postId": 3,
  "title": "코스트코 생수 2L x 24 같이 사요",
  "status": "DISTRIBUTED",
  "myRole": "PARTICIPANT",
  "actionRequired": "CONFIRM_RECEIPT",
  "reviewProgress": { "written": 1, "total": 4 }
}
```

| 필드 | 설명 |
|---|---|
| `actionRequired` | 없으면 `null`. 아래 표 참고 |
| `reviewProgress` | 완료 탭에서만 값이 있음 |

| `actionRequired` | 대상 | 조건 |
|---|---|---|
| `ORDER` | 호스트 | 게시글 `RECRUITED` |
| `SHIP` | 호스트 | 게시글 `ORDERED` |
| `DISTRIBUTE` | 호스트 | 게시글 `SHIPPING` |
| `CONFIRM_RECEIPT` | 참여자 | 게시글 `DISTRIBUTED` + 내 수령 상태 `WAITING` |
| `WRITE_REVIEW` | 호스트·참여자 | 게시글 `SETTLED` + **정산 후 3일 이내** + 안 쓴 대상이 있음 [D12] |
| `PAY_DEBT` | 호스트·참여자 | 이 공구에서 생긴 미납 미수금이 있음 [D9] |

**Errors**: `COMMON400`, `AUTH401`

### GET /api/users/{userId} — 유저 프로필 모달

- 권한: 로그인 · 화면: 08
- 좌표는 보내지 않고 동 이름만 보냅니다 [D14]

**Response**
```json
{
  "userId": 12,
  "nickname": "배고픈 고구마",
  "universityName": "○○대학교",
  "dongName": "역삼동",
  "joinedAt": "2026-03-02",
  "rating": 4.9,
  "reviewCount": 18,
  "tradeCount": 23,
  "mainCategories": ["FOOD", "DAILY"],
  "tagCounts": [{ "tag": "PUNCTUAL", "count": 12 }],
  "recentReviews": [
    { "reviewerNickname": "졸린 감자", "postTitle": "생수 공구", "rating": 5, "content": "...", "createdAt": "2026-10-30" }
  ],
  "recentTrades": [
    { "postId": 3, "role": "HOST", "title": "...", "status": "RECRUITING", "settledAt": null }
  ],
  "isBlocked": false
}
```

**Errors**: `AUTH401`, `COMMON404`

---

## 5. 이미지

### POST /api/images — 이미지 업로드

- 권한: 로그인 · 화면: 04 공구 등록, 03-C 영수증, 03-F 수령 문의, 05 채팅, 08-B 신고
- Content-Type: `multipart/form-data`
- 원본은 "업로드 → URL"만 정함. 형식은 (보완)

**Request (form-data)**

| Part | 타입 | 필수 | 설명 |
|---|---|---|---|
| `files` | file[] | Y | 1개 이상 |
| `type` | ImageType | Y | `POST`, `RECEIPT`, `INQUIRY`, `REPORT`, `CHAT` [D13] |

**Response**
```json
{
  "images": [
    { "url": "https://.../post/2026/10/27/uuid-1.jpg", "originalName": "water.jpg", "size": 482113 }
  ]
}
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `IMAGE4001` | 허용하지 않는 파일 형식 |
| 400 | `IMAGE4002` | 파일이 없거나 개수 초과 |
| 413 | `IMAGE4131` | 파일 용량 초과 |

> 허용 형식·용량·개수 제한은 미정입니다 (D13 남은 미정).

---

## 6. 공구 게시글

목록·검색·카테고리·거리·정렬을 `GET /api/posts` 하나로 처리합니다. 상품 정보는 게시글에 포함됩니다 [D2].

### GET /api/posts — 공구 목록·검색·필터

- 권한: 로그인 · 화면: 02
- 거리는 내가 저장한 동네 좌표 기준입니다. 차단 관계인 사용자의 글은 빠집니다.

**Query**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `keyword` | string | N | 없음 | 제목·상품명 검색어 |
| `category` | Category | N | 전체 | |
| `radius` | int | N | `3` | `1`, `3`, `5` (km) |
| `recruitingOnly` | boolean | N | `true` | 모집중만 보기 |
| `sort` | PostSort | N | `LATEST` | |
| `page`, `size` | int | N | `0`, `9` | |

**Response** — `content[]` 한 개 (PostCard)
```json
{
  "postId": 3,
  "title": "코스트코 생수 2L x 24 같이 사요",
  "thumbnailUrl": "https://.../1.jpg",
  "category": "FOOD",
  "unitPrice": 4840,
  "baseUnit": "2L 생수 4병",
  "totalUnits": 6,
  "filledUnits": 4,
  "participantCount": 3,
  "dongName": "역삼동",
  "distanceKm": 0.8,
  "status": "RECRUITING",
  "viewCount": 52,
  "deadlineAt": "2026-10-29T23:59:00",
  "createdAt": "2026-10-27T14:00:00"
}
```

| 필드 | 설명 |
|---|---|
| `filledUnits` | 지금까지 찬 단위 수. **호스트 본인 단위 포함** [D3, D8] |
| `participantCount` | 참여 인원. 호스트 포함 [D3] |

**Errors**: `COMMON400`, `AUTH401`

### GET /api/posts/{postId} — 공구 상세

- 권한: 로그인 · 화면: 03 (03-A~F 공통)
- 호출할 때마다 조회수가 1 오릅니다

**Response**
```json
{
  "postId": 3,
  "title": "코스트코 생수 2L x 24 같이 사요",
  "productName": "커클랜드 생수 2L x 24",
  "productLink": "https://www.costco.co.kr/p/...",
  "category": "FOOD",
  "content": "차로 가져와서 정문 앞에서 나눠드릴게요!",
  "imageUrls": ["https://.../1.jpg"],
  "baseUnit": "2L 생수 4병",
  "totalUnits": 6,
  "filledUnits": 4,
  "participantCount": 3,
  "unitPrice": 4840,
  "totalPrice": 29040,
  "status": "RECRUITING",
  "statusTimeline": {
    "recruitedAt": null,
    "orderedAt": null,
    "shippedAt": null,
    "distributedAt": null,
    "settledAt": null,
    "cancelledAt": null
  },
  "dongName": "역삼동",
  "distanceKm": 0.8,
  "tradePlaceName": "○○대학교 정문 앞",
  "deadlineAt": "2026-10-29T23:59:00",
  "viewCount": 52,
  "createdAt": "2026-10-27T14:00:00",
  "host": { "userId": 12, "nickname": "배고픈 고구마", "rating": 4.9, "tradeCount": 23, "mainCategories": ["FOOD"] },
  "participants": [
    { "userId": 12, "nickname": "배고픈 고구마", "role": "HOST", "units": 2, "rating": 4.9, "tradeCount": 23, "mainCategories": ["FOOD", "DAILY"] }
  ],
  "me": { "role": "PARTICIPANT", "units": 1, "paymentStatus": "PAID", "receiptStatus": "WAITING" },
  "maxSelectableUnits": 2,
  "chatRoomId": 7
}
```

| 필드 | 설명 |
|---|---|
| `statusTimeline` | 단계별로 넘어간 시각 [D5]. `cancelledAt` 추가 |
| `statusTimeline.distributedAt` | 호스트가 나눔 일정·장소를 입력한 시각 [D6] |
| `me` | 참여하지 않았으면 `null` |
| `maxSelectableUnits` | `totalUnits - filledUnits`. 모집중이 아니면 0 |
| `chatRoomId` | 호스트·참여자에게만 값이 있고, 그 밖에는 `null` |

**Errors**: `AUTH401`, `COMMON404`

### POST /api/posts — 공구 등록

- 권한: 로그인 · 화면: 04
- 처리 (한 트랜잭션)
  1. 미납 미수금이 있으면 거부 [D9]
  2. 게시글을 `RECRUITING`으로 생성
  3. 호스트 참여(`HOST`, `hostUnits`) 생성
  4. **호스트 몫 `hostUnits × unitPrice`를 포인트로 결제·보류** [D8]
  5. `filledUnits = hostUnits`, `participantCount = 1` [D3]
  6. 채팅방을 만들고 호스트 입장

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `title` | string | Y | 제목 |
| `productName` | string | Y | 상품명 |
| `productLink` | string | N | 상품 링크 |
| `category` | Category | Y | |
| `content` | string | N | 내용 |
| `imageUrls` | string[] | N | 비우면 기본 이미지 |
| `baseUnit` | string | Y | 기본 단위 (예: "2L 생수 4병") |
| `totalUnits` | int | Y | 총 단위 수 |
| `totalPrice` | int | Y | 총 가격 (호스트가 예상한 가격) |
| `hostUnits` | int | Y | 호스트 본인 구매 단위 (1 이상 `totalUnits - 1` 이하) |
| `deadlineAt` | datetime | Y | 모집 마감 일시 |
| `tradePlace.name` | string | Y | 거래 장소 이름 |
| `tradePlace.latitude` | double | Y | |
| `tradePlace.longitude` | double | Y | |

```json
{
  "title": "코스트코 생수 2L x 24 같이 사요",
  "productName": "커클랜드 생수 2L x 24",
  "productLink": "https://www.costco.co.kr/p/...",
  "category": "FOOD",
  "content": "차로 가져와서 정문 앞에서 나눠드릴게요!",
  "imageUrls": ["https://.../1.jpg"],
  "baseUnit": "2L 생수 4병",
  "totalUnits": 6,
  "totalPrice": 29040,
  "hostUnits": 2,
  "deadlineAt": "2026-10-29T23:59:00",
  "tradePlace": { "name": "○○대학교 정문 앞", "latitude": 37.5012, "longitude": 127.0396 }
}
```

단위당 가격(`unitPrice = totalPrice / totalUnits`)은 서버가 계산합니다. `totalPrice`는 예상 가격이고, 실제 가격과 다르면 영수증 등록 때 차액을 정산합니다 [D9].

**Response**
```json
{ "postId": 3, "chatRoomId": 7, "unitPrice": 4840, "paidPoint": 9680, "balanceAfter": 2320 }
```

| 필드 | 설명 |
|---|---|
| `paidPoint` | 호스트 몫 결제(보류) 금액 [D8] |
| `balanceAfter` | 결제 후 사용 가능 잔액 [D8] |

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | 필수 값 누락·형식 오류 |
| 400 | `POST4001` | `hostUnits` 범위 오류 |
| 400 | `POST4002` | `deadlineAt`이 과거 |
| 403 | `POINT4031` | 미납 미수금이 있어 등록 불가 [D9] |
| 409 | `POINT409` | 호스트 몫을 결제할 잔액 부족 → 충전 후 등록 안내 [D8] |
| 409 | `POINT4091` | 지갑 동시 변경 충돌 |

### PATCH /api/posts/{postId} — 공구 수정

- 권한: 호스트 · 화면: 04
- 선행조건: `RECRUITING`이고 호스트 외 참여자가 없음
- 보낸 필드만 바뀝니다. 수정 가능: `title`, `productName`, `productLink`, `category`, `content`, `imageUrls`, `deadlineAt`, `tradePlace`
- `totalUnits`, `totalPrice`, `hostUnits`, `baseUnit`은 수정할 수 없습니다. 호스트가 이미 보류한 금액이 달라지기 때문입니다 [D8]. 바꾸려면 취소 후 다시 등록합니다.

**Request Body** — 등록과 같은 필드 중 수정 가능한 것만
```json
{ "title": "코스트코 생수 2L x 24 같이 사요 (정문 나눔)", "deadlineAt": "2026-10-30T23:59:00" }
```

**Response** (보완)
```json
{ "postId": 3 }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | 수정 불가 필드 포함, 형식 오류 |
| 403 | `AUTH403` | 호스트가 아님 |
| 404 | `COMMON404` | 게시글 없음 |
| 409 | `POST4091` | 모집중이 아니거나 참여자가 있음 |

### DELETE /api/posts/{postId} — 공구 취소

- 권한: 호스트 · 화면: 03
- 선행조건: `RECRUITING` 또는 `RECRUITED` (주문완료 전)
- 처리: 게시글 행 락 → 지갑 순서로 처리 [D4]. `CANCELLED`로 바꾸고 `cancelledAt` 기록 [D5], **호스트 포함** 전원의 보류 포인트 환급(`REFUND`), 채팅방 `CLOSED`. 게시글은 지우지 않습니다.

**Response** (보완)
```json
{ "postId": 3, "postStatus": "CANCELLED", "refundedCount": 4, "refundedPoint": 29040 }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 403 | `AUTH403` | 호스트가 아님 |
| 404 | `COMMON404` | 게시글 없음 |
| 409 | `POST4092` | 주문완료 이후라 취소 불가 |

---

## 7. 공구 진행

참여하는 순간 포인트를 보류(HELD)하고, 전원이 받으면 호스트에게 정산(CAPTURED)합니다. 프론트가 상태값을 직접 보내는 API는 없습니다.

### 7.1 상태 전이

| 단계 | 상태 | 넘어가는 조건 | 포인트 처리 | API |
|---|---|---|---|---|
| 1 | `RECRUITING` | 공구 등록 | **호스트 몫 보류** [D8] | `POST /api/posts` |
| 2 | `RECRUITED` | 총 단위가 다 참 (자동) | 참여 시 보류 | `POST .../participations` |
| 3 | `ORDERED` | 영수증 등록 | **차액 환급 또는 추가 청구** [D9] | `POST .../order` |
| 4 | `SHIPPING` | 배송 시작 | - | `PATCH .../order/shipping` |
| 5 | `DISTRIBUTED` | **호스트가 나눔 일정·장소 입력** [D6] | - | `PATCH .../order/distribution` |
| 6 | `SETTLED` | 전원 `RECEIVED` (직접 또는 **나눔 예정 시각 + 3일 자동** [D10]) | CAPTURED → 호스트 정산 (**인센티브 없음** [D11]) | `POST .../receipt`, 스케줄러 |
| 예외 | `CANCELLED` | 호스트 취소 | RELEASED → 호스트 포함 전원 환급 | `DELETE /api/posts/{postId}` |

- `RECRUITED`에서 참여 취소가 일어나면 `RECRUITING`으로 돌아갑니다.
- 상태가 바뀔 때마다 채팅방에 SYSTEM 메시지를 보냅니다 (보완).
- 모집 마감 시 인원 미달 자동 취소는 **보류** 상태입니다 (DB.md 8.1).

### POST /api/posts/{postId}/participations — 참여 + 즉시 결제

- 권한: 로그인 (호스트 본인 제외) · 화면: 03, 03-B
- 선행조건: `RECRUITING`, 마감 전, 미참여, 호스트와 차단 관계 아님, **미납 미수금 없음** [D9]
- 처리 (한 트랜잭션) [D3, D4]
  1. 미납 미수금 확인
  2. 게시글 행 **비관적 락**으로 조회
  3. 모집중인지, `filledUnits + units <= totalUnits`인지 확인
  4. `filledUnits += units`, `participantCount += 1`
  5. 지갑에서 `units × unitPrice` 보류 (지갑은 낙관적 락)
  6. 참여·보류·거래 내역 저장, 채팅방 입장
  7. 단위가 다 차면 `RECRUITED`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `units` | int | Y | 참여 단위 (1 이상, 남은 단위 이하) |

```json
{ "units": 1 }
```

**Response**
```json
{ "participationId": 21, "paidPoint": 4840, "balanceAfter": 7160, "postStatus": "RECRUITING", "chatRoomId": 7 }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | `units`가 1 미만 |
| 400 | `PART4001` | 남은 단위 초과 |
| 403 | `PART4031` | 호스트와 차단 관계 |
| 403 | `POINT4031` | 미납 미수금이 있어 참여 불가 [D9] |
| 409 | `POST409` | 모집중이 아니거나 마감 지남 |
| 409 | `PART4091` | 이미 참여함 (`UNIQUE(post_id, user_id)` 위반 포함) [D4] |
| 409 | `POINT409` | 잔액 부족 |
| 409 | `POINT4091` | 지갑 동시 변경 충돌 [D4] |

### DELETE /api/posts/{postId}/participations/me — 참여 취소

- 권한: 참여자 · 화면: 03
- 선행조건: `RECRUITING` 또는 `RECRUITED`. 호스트는 호출 불가 (공구 취소 API 사용)
- 처리: 게시글 락 → `filledUnits -= units`, `participantCount -= 1` → 지갑 환급(`REFUND`) [D3, D4]. 채팅방 퇴장. `RECRUITED`였으면 `RECRUITING`으로 돌아갑니다.

**Response** (보완)
```json
{ "refundedPoint": 4840, "balanceAfter": 12000, "postStatus": "RECRUITING" }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 403 | `AUTH403` | 참여자가 아님 |
| 409 | `PART4092` | 호스트는 참여 취소 불가 |
| 409 | `POST409` | 주문완료 이후라 취소 불가 |

### GET /api/posts/{postId}/participations — 결제·수령 현황

- 권한: 호스트·참여자 · 화면: 03-E

**Response**
```json
{
  "paidCount": 5,
  "receivedCount": 4,
  "totalCount": 5,
  "participants": [
    {
      "userId": 12,
      "nickname": "배고픈 고구마",
      "role": "HOST",
      "units": 2,
      "paidPoint": 9680,
      "paymentStatus": "PAID",
      "receiptStatus": "RECEIVED",
      "autoConfirmed": false,
      "isMe": false
    }
  ]
}
```

| 필드 | 설명 |
|---|---|
| `participants[]` | **호스트 포함** [D8] |
| `paidPoint` | 처음 결제한 금액 (차액 조정 전) |
| `receiptStatus` | 화면의 '수령확인' 단계는 `receivedCount / totalCount`로 표시 [D6] |
| `autoConfirmed` | 3일이 지나 자동으로 수령 처리됐는지 [D10] |

**Errors**: `AUTH403`

### POST /api/posts/{postId}/order — 영수증 등록 + 주문 완료

- 권한: 호스트 · 화면: 03-C
- 선행조건: `RECRUITED`, 주문 미등록
- 처리
  1. 주문 정보·영수증 이미지 저장, `ORDERED` (`orderedAt`)
  2. `difference = receiptAmount - totalPrice` 계산 후 **차액 정산** [D9]

**차액 정산 [D9]**

| 경우 | 처리 |
|---|---|
| 같음 (`difference = 0`) | 없음 |
| 영수증이 더 쌈 (`difference < 0`) | 차액을 단위 비율대로 나눠 **호스트 포함** 각자에게 환급 (`ADJUSTMENT_REFUND`) |
| 영수증이 더 비쌈 (`difference > 0`) | 차액을 단위 비율대로 **추가 청구** (`ADJUSTMENT_CHARGE`) + 알림. 상한 없음. 잔액이 부족하면 부족분은 **미수금**(납부 기한 7일) |

- 1인당 금액 = `|difference| × 내 단위 / totalUnits` (원 단위 내림). 나눗셈 나머지는 호스트에게 몰아서 처리 (DB.md D9 제안)
- 알림은 채팅방 SYSTEM 메시지로 보냅니다 (보완). 별도 알림 기능은 미정입니다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `receiptAmount` | int | Y | 영수증 금액 (1 이상) |
| `receiptImageUrls` | string[] | Y | 영수증 사진 1장 이상 |

```json
{ "receiptAmount": 31000, "receiptImageUrls": ["https://.../receipt1.jpg"] }
```

**Response**
```json
{
  "orderId": 5,
  "postStatus": "ORDERED",
  "receiptAmount": 31000,
  "recruitedAmount": 29040,
  "amountMatched": false,
  "difference": 1960,
  "adjustmentType": "CHARGE",
  "adjustments": [
    { "userId": 12, "role": "HOST", "units": 2, "amount": 656, "chargedAmount": 656, "debtAmount": 0 },
    { "userId": 15, "role": "PARTICIPANT", "units": 1, "amount": 326, "chargedAmount": 0, "debtAmount": 326 }
  ]
}
```

| 필드 | 설명 |
|---|---|
| `recruitedAmount` | 모집 금액 (`unitPrice × totalUnits`) |
| `difference` | 영수증 금액 − 모집 금액 [D9] |
| `adjustmentType` | `NONE`, `REFUND`(더 쌈), `CHARGE`(더 비쌈) [D9] |
| `adjustments[].amount` | 이 사람에게 환급하거나 청구할 금액 |
| `adjustments[].chargedAmount` | 실제로 지갑에서 빠진 금액 (`CHARGE`일 때) |
| `adjustments[].debtAmount` | 잔액 부족으로 미수금이 된 금액 (`CHARGE`일 때) [D9] |

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | 금액 오류, 영수증 사진 없음 |
| 403 | `AUTH403` | 호스트가 아님 |
| 409 | `POST409` | `RECRUITED`가 아님 |
| 409 | `ORDER4091` | 이미 주문 정보가 등록됨 |

### GET /api/posts/{postId}/order — 주문 정보 조회

- 권한: 호스트·참여자 · 화면: 03-C, 03-D, 03-E
- 선행조건: 주문 등록 후. 그 전에는 `COMMON404`
- 참여자는 영수증 사진과 금액을 보고 이상하면 문의·신고합니다. 금액 확인은 MVP에선 호스트 입력 + 사진 + 참여자 확인입니다 [D9]

**Response**
```json
{
  "orderId": 5,
  "courier": "CJ대한통운",
  "trackingNumber": "612345678901",
  "receiptAmount": 31000,
  "recruitedAmount": 29040,
  "amountMatched": false,
  "difference": 1960,
  "adjustmentType": "CHARGE",
  "myAdjustment": { "amount": 326, "chargedAmount": 0, "debtAmount": 326 },
  "receiptImageUrls": ["https://.../receipt1.jpg"],
  "registeredAt": "2026-10-29T10:20:00",
  "distributeAt": "2026-10-30T19:00:00",
  "distributePlace": "○○대학교 정문 앞",
  "autoConfirmAt": "2026-11-02T19:00:00",
  "heldPoint": 30674
}
```

| 필드 | 설명 |
|---|---|
| `courier`, `trackingNumber` | 배송 시작 전, 또는 오프라인 구매면 `null` |
| `distributeAt`, `distributePlace` | 나눔 일정 입력 전에는 `null` |
| `myAdjustment` | 내 차액 정산 내역 [D9] |
| `autoConfirmAt` | `distributeAt + 3일`. 이때까지 '받았어요'를 안 누르면 자동 수령 처리 [D10] |
| `heldPoint` | 이 공구에서 아직 보류 중인 포인트 합계 |

**Errors**: `AUTH403`, `COMMON404`

### PATCH /api/posts/{postId}/order/shipping — 배송 시작

- 권한: 호스트 · 화면: 03-C
- 선행조건: `ORDERED`. 처리 후 `SHIPPING` (`shippedAt`)
- 오프라인 매장에서 직접 산 경우 택배사·송장 없이 보낼 수 있습니다 (택배사·송장은 NULL 허용)

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `courier` | string | N | 택배사 |
| `trackingNumber` | string | N | 송장번호 |

```json
{ "courier": "CJ대한통운", "trackingNumber": "612345678901" }
```

**Response** (보완)
```json
{ "postStatus": "SHIPPING", "shippedAt": "2026-10-29T15:00:00" }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 403 | `AUTH403` | 호스트가 아님 |
| 409 | `POST409` | `ORDERED`가 아님 |

### PATCH /api/posts/{postId}/order/distribution — 나눔 일정·장소 입력 [D6]

- 권한: 호스트 · 화면: 03-C
- 선행조건: `SHIPPING`
- 처리 [D6]
  1. 나눔 일정·장소 저장
  2. 게시글 `DISTRIBUTED` (`distributedAt` = 입력 시각)
  3. 호스트 본인 참여는 `RECEIVED`로 처리, 참여자는 `WAITING`
- **호스트의 별도 '나눔완료' 버튼은 없습니다.** 직접 만나서 나누면 호스트의 나눔완료와 참여자의 '받았어요'가 사실상 동시에 일어나기 때문입니다. 참여자는 받으면 각자 '받았어요'를 누릅니다.
- `distributeAt + 3일`이 지나면 '받았어요'를 안 누른 `WAITING` 참여자를 스케줄러가 자동으로 `RECEIVED` 처리합니다. 문의중(`INQUIRING`)은 제외 [D10]

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `distributeAt` | datetime | Y | 나눔 예정 일시. 자동 수령 처리 기준 [D10] |
| `distributePlace` | string | Y | 나눔 장소 |

```json
{ "distributeAt": "2026-10-30T19:00:00", "distributePlace": "○○대학교 정문 앞" }
```

**Response** (보완)
```json
{ "postStatus": "DISTRIBUTED", "distributedAt": "2026-10-29T18:00:00", "autoConfirmAt": "2026-11-02T19:00:00" }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | 필수 값 누락 |
| 403 | `AUTH403` | 호스트가 아님 |
| 409 | `POST409` | `SHIPPING`이 아님 |

### POST /api/posts/{postId}/receipt — 받았어요

- 권한: 참여자 · 화면: 03-E
- 선행조건: `DISTRIBUTED`, 내 수령 상태가 `WAITING`
- 바디 없음
- 처리: 내 참여를 `RECEIVED` (`receivedAt` 기록). 전원이 `RECEIVED`가 되면
  1. 게시글 `SETTLED` (`settledAt`)
  2. 호스트 포함 전체 보류 `CAPTURED` → 호스트에게 정산 (`SETTLEMENT`). **인센티브 없음** [D11]
  3. 미수금이 있는 참여자 몫은 빼고 받은 만큼 먼저 정산, 미수금이 납부되면 그때 호스트에게 추가 지급 [D9]
  4. 채팅방 `CLOSED`

**Response**
```json
{ "receiptStatus": "RECEIVED", "postStatus": "DISTRIBUTED" }
```

마지막 확인이었으면 `postStatus`가 `SETTLED`입니다.

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 403 | `AUTH403` | 참여자가 아님 |
| 409 | `POST409` | `DISTRIBUTED`가 아님 |
| 409 | `PART4093` | 이미 받았어요를 누름 |

### POST /api/posts/{postId}/inquiries — 수령 문제 문의

- 권한: 참여자 · 화면: 03-F
- 선행조건: `DISTRIBUTED`, 내 수령 상태가 `WAITING`
- 처리: 문의를 `OPEN`으로 저장하고 내 수령 상태를 `INQUIRING`으로 바꿉니다. **운영팀이 확인하고 처리합니다** [D10]. 문의중이면 자동 수령 처리 대상에서 빠집니다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `type` | InquiryType | Y | 문의 유형 |
| `receivedUnits` | int | 조건 | `SHORT_QUANTITY`면 필수. 실제 받은 단위 |
| `desiredResolution` | DesiredResolution | N | 원하는 처리 (희망 사항) |
| `content` | string | Y | 내용 |
| `imageUrls` | string[] | 조건 | `WRONG_OR_DAMAGED`면 1장 이상 |

```json
{
  "type": "NOT_RECEIVED",
  "receivedUnits": null,
  "desiredResolution": "REDELIVER",
  "content": "약속 장소에 호스트가 나오지 않았어요",
  "imageUrls": []
}
```

**Response**
```json
{ "inquiryId": 2, "receiptStatus": "INQUIRING", "inquiryStatus": "OPEN" }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `INQ4001` | `SHORT_QUANTITY`인데 `receivedUnits` 없음·범위 오류 |
| 400 | `INQ4002` | `WRONG_OR_DAMAGED`인데 사진 없음 |
| 403 | `AUTH403` | 참여자가 아님 |
| 409 | `POST409` | `DISTRIBUTED`가 아님 |
| 409 | `INQ4091` | 이미 받았어요를 누름 |

### GET /api/posts/{postId}/inquiries — 문의 목록

- 권한: 호스트·참여자 · 화면: 03-C, 03-F
- 호스트는 전체 문의, 참여자는 자기 문의만 봅니다 (보완)

**Response** — 최신순 배열
```json
[
  {
    "inquiryId": 2,
    "userId": 15,
    "nickname": "졸린 감자",
    "type": "SHORT_QUANTITY",
    "receivedUnits": 0,
    "desiredResolution": "PARTIAL_REFUND",
    "content": "1단위 중 받은 게 없어요",
    "imageUrls": [],
    "status": "RESOLVED",
    "resolution": "PARTIAL_REFUND",
    "refundAmount": 4840,
    "createdAt": "2026-10-30T20:10:00",
    "resolvedAt": "2026-10-31T11:00:00"
  }
]
```

| 필드 | 설명 |
|---|---|
| `status` | `OPEN` → `IN_PROGRESS` → `RESOLVED` [D10] |
| `resolution` | 운영팀 최종 결정. 처리 전에는 `null` [D10] |
| `refundAmount` | 부분·전액 환불일 때 금액 [D10] |

운영팀의 처리 API·화면은 나중에 추가합니다. 그 전까지는 운영팀이 직접 처리합니다 [D10].

**Errors**: `AUTH403`

---

## 8. 포인트

충전은 '준비 → PG 결제창 → 승인' 두 번 호출합니다. 결제·환급·정산·차액 조정은 7장의 진행 API 안에서 포인트 서비스가 처리합니다.

### GET /api/points/me — 잔액 조회

- 권한: 로그인 · 화면: 06, 03-B, 07

**Response**
```json
{ "availableBalance": 12000, "heldAmount": 4840, "unpaidDebt": 326, "updatedAt": "2026-10-27T14:02:00" }
```

| 필드 | 설명 |
|---|---|
| `availableBalance` | 사용 가능 잔액 |
| `heldAmount` | 진행 중인 공구에 보류된 금액 |
| `unpaidDebt` | 미납 미수금 합계. 0보다 크면 공구 참여·등록 불가 [D9] |

**Errors**: `AUTH401`

### GET /api/points/debts — 미수금 목록 (보완) [D9]

- 권한: 로그인 · 화면: 06
- 영수증 금액이 더 비싸서 추가 청구됐는데 잔액이 부족했던 건을 보여줍니다

**Query**

| 파라미터 | 타입 | 필수 | 기본값 |
|---|---|---|---|
| `status` | DebtStatus | N | `UNPAID` |

**Response** — 배열
```json
[
  {
    "debtId": 1,
    "postId": 3,
    "postTitle": "코스트코 생수 2L x 24 같이 사요",
    "amount": 326,
    "status": "UNPAID",
    "dueAt": "2026-11-05T10:20:00",
    "overdue": false,
    "createdAt": "2026-10-29T10:20:00",
    "paidAt": null
  }
]
```

| 필드 | 설명 |
|---|---|
| `dueAt` | 납부 기한 = 생성 + **7일** [D9] |
| `overdue` | 기한이 지났는지. 기한 초과가 누적 10회가 되면 계정 정지 [D9] |

**Errors**: `AUTH401`

### POST /api/points/charges — 충전 준비

- 권한: 로그인 · 화면: 06
- 프론트는 응답값으로 PG 결제창을 엽니다

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `amount` | int | Y | 충전 금액 |
| `method` | ChargeMethod | Y | `KAKAOPAY`, `TOSSPAY`, `CARD` |

```json
{ "amount": 10000, "method": "KAKAOPAY" }
```

**Response**
```json
{ "orderId": "CHG-20261027-0001", "amount": 10000, "orderName": "식구 포인트 10,000P" }
```

`orderId`는 DB의 `card_payments.pg_order_id`에 저장됩니다 [D1].

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | 금액·결제수단 오류 |

> 충전 최소·최대 금액은 미정입니다.

### POST /api/points/charges/confirm — 충전 승인

- 권한: 로그인 · 화면: 06 (PG 성공 리다이렉트 후)
- 처리
  1. 준비 단계 금액과 비교 후 PG 승인 요청
  2. 포인트 적립 (`CHARGE`)
  3. **미납 미수금이 있으면 충전액에서 자동으로 먼저 차감** (`DEBT_REPAYMENT`) → 해당 공구 호스트에게 추가 지급 (`SETTLEMENT`) [D9]
- 멱등: 같은 `orderId`로 다시 요청하면 기존 결과를 그대로 돌려줍니다

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `orderId` | string | Y | 충전 준비에서 받은 주문번호 |
| `paymentKey` | string | Y | PG가 준 결제 키 |
| `amount` | int | Y | 준비 단계 금액과 같아야 함 |

```json
{ "orderId": "CHG-20261027-0001", "paymentKey": "tgen_...", "amount": 10000 }
```

**Response**
```json
{ "chargedPoint": 10000, "debtRepaid": 326, "balanceAfter": 21674 }
```

| 필드 | 설명 |
|---|---|
| `debtRepaid` | 이번 충전으로 자동 납부된 미수금 [D9] |
| `balanceAfter` | 충전·미수금 차감 후 사용 가능 잔액 |

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `POINT4001` | 금액이 준비 단계와 다름 |
| 404 | `COMMON404` | 주문번호 없음 또는 다른 사람의 주문 |
| 502 | `POINT5021` | PG 승인 실패 |

### GET /api/points/transactions — 거래 내역

- 권한: 로그인 · 화면: 06, 07

**Query**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `type` | string | N | `ALL` | `ALL` 또는 PointTxType 값 |
| `page`, `size` | int | N | `0`, `20` | |

**Response** — `content[]` 한 개
```json
{
  "transactionId": 101,
  "type": "PAYMENT",
  "description": "코스트코 생수 2L x 24 같이 사요 · 1단위",
  "amount": -4840,
  "balanceAfter": 7160,
  "postId": 3,
  "createdAt": "2026-10-28T09:12:00"
}
```

| `type` | `amount` 부호 | 설명 |
|---|---|---|
| `CHARGE` | + | 충전 |
| `PAYMENT` | − | 공구 참여·등록 결제 (보류) |
| `REFUND` | + | 취소 환급, 수령 문의 환불 |
| `SETTLEMENT` | + | 호스트 정산, 미수금 납부분 추가 지급 |
| `ADJUSTMENT_REFUND` | + | 영수증이 더 쌀 때 차액 환급 [D9] |
| `ADJUSTMENT_CHARGE` | − | 영수증이 더 비쌀 때 차액 추가 청구 [D9] |
| `DEBT_REPAYMENT` | − | 충전 시 미수금 자동 납부 [D9] |

`postId`는 `CHARGE`일 때 `null`입니다.

**Errors**: `COMMON400`, `AUTH401`

---

## 9. 채팅

채팅방은 게시글을 만들 때 자동으로 생기고 호스트가 입장합니다. 참여하면 입장, 참여 취소하면 퇴장합니다. `SETTLED`·`CANCELLED`가 되면 `CLOSED`로 바뀌고 읽기만 할 수 있습니다. 메시지는 **텍스트·이미지·시스템** 세 종류입니다 [D13].

### GET /api/chats — 채팅방 목록

- 권한: 로그인 · 화면: 05

**Query**

| 파라미터 | 타입 | 필수 | 기본값 |
|---|---|---|---|
| `status` | ChatRoomStatus | N | `ONGOING` |

**Response** — 배열, `lastMessageAt` 최신순
```json
[
  {
    "chatRoomId": 7,
    "postId": 3,
    "name": "생수 공구방",
    "thumbnailUrl": "https://.../1.jpg",
    "lastMessage": "사진",
    "lastMessageType": "IMAGE",
    "lastMessageAt": "2026-10-27T14:10:00",
    "unreadCount": 2,
    "participantCount": 3
  }
]
```

| 필드 | 설명 |
|---|---|
| `lastMessage` | 마지막 메시지가 이미지면 `"사진"` [D13] |
| `lastMessageType` | (보완) [D13] |
| `unreadCount` | 마지막으로 읽은 시각 이후 메시지 수 |

**Errors**: `AUTH401`

### GET /api/chats/{chatRoomId} — 방 헤더 + 공구 요약

- 권한: 방 참여자 · 화면: 05
- 원본은 "방 헤더 + 공구 요약"까지만 정함. 응답 형식은 (보완)

**Response**
```json
{
  "chatRoomId": 7,
  "name": "생수 공구방",
  "status": "ONGOING",
  "participantCount": 3,
  "participants": [
    { "userId": 12, "nickname": "배고픈 고구마", "role": "HOST" },
    { "userId": 15, "nickname": "졸린 감자", "role": "PARTICIPANT" }
  ],
  "post": {
    "postId": 3,
    "title": "코스트코 생수 2L x 24 같이 사요",
    "thumbnailUrl": "https://.../1.jpg",
    "status": "RECRUITED",
    "unitPrice": 4840,
    "filledUnits": 6,
    "totalUnits": 6
  }
}
```

**Errors**: `AUTH403`, `COMMON404`

### GET /api/chats/{chatRoomId}/messages — 이전 메시지

- 권한: 방 참여자 · 화면: 05

**Query**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `cursor` | long | N | 없음 (최신부터) | 이 `messageId`보다 오래된 메시지 |
| `size` | int | N | `20` | |

**Response**
```json
{
  "messages": [
    { "messageId": 502, "type": "IMAGE", "senderId": 12, "senderNickname": "배고픈 고구마", "content": null, "imageUrl": "https://.../chat/uuid.jpg", "sentAt": "2026-10-27T13:05:00" },
    { "messageId": 501, "type": "TEXT", "senderId": 12, "senderNickname": "배고픈 고구마", "content": "안녕하세요!", "imageUrl": null, "sentAt": "2026-10-27T13:02:00" },
    { "messageId": 500, "type": "SYSTEM", "senderId": null, "senderNickname": null, "content": "졸린 감자님이 참여했어요", "imageUrl": null, "sentAt": "2026-10-27T13:00:00" }
  ],
  "nextCursor": 499,
  "hasNext": true
}
```

| 필드 | 설명 |
|---|---|
| `type` | `TEXT`, `IMAGE`, `SYSTEM` [D13] |
| `content` | `TEXT`·`SYSTEM`의 내용. `IMAGE`면 `null` [D13] |
| `imageUrl` | `IMAGE`의 이미지 URL. 그 밖에는 `null` [D13] |
| `senderId` | `SYSTEM`이면 `null` |

**Errors**: `AUTH403`

### PATCH /api/chats/{chatRoomId}/read — 읽은 시각 갱신

- 권한: 방 참여자 · 화면: 05 (방에 들어가거나 새 메시지를 받을 때)
- 바디 없음

**Response** (보완)
```json
{ "chatRoomId": 7, "lastReadAt": "2026-10-27T14:11:00" }
```

**Errors**: `AUTH403`

### 9.1 WebSocket (STOMP)

| 구분 | 경로 | 내용 |
|---|---|---|
| 연결 | `/ws-stomp` | CONNECT 헤더에 `Authorization: Bearer {token}` |
| 구독 | `/sub/chats/{chatRoomId}` | 새 메시지 수신. 방 참여자만 가능 |
| 발행 | `/pub/chats/{chatRoomId}/messages` | 메시지 전송 |

**발행 페이로드** — 원본은 `{ "content": "..." }`. 이미지 전송을 위해 `type`, `imageUrl` 추가 [D13]

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `type` | string | Y | `TEXT` 또는 `IMAGE` (`SYSTEM`은 서버만 만듦) |
| `content` | string | 조건 | `TEXT`면 필수 |
| `imageUrl` | string | 조건 | `IMAGE`면 필수. `POST /api/images`(`type=CHAT`)로 먼저 업로드한 URL |

```json
// 텍스트
{ "type": "TEXT", "content": "그럼 내일 7시에 정문 앞에서 봬요!" }
// 이미지 (메시지 1개 = 이미지 1장)
{ "type": "IMAGE", "imageUrl": "https://.../chat/uuid.jpg" }
```

**수신 페이로드** (보완) — 이전 메시지 조회의 항목과 같은 형식 + `chatRoomId`
```json
{
  "chatRoomId": 7,
  "messageId": 503,
  "type": "IMAGE",
  "senderId": 15,
  "senderNickname": "졸린 감자",
  "content": null,
  "imageUrl": "https://.../chat/uuid.jpg",
  "sentAt": "2026-10-27T14:10:00"
}
```

**발행 에러** (보완) — 개인 오류 큐 `/user/queue/errors`로 전달

| code | 상황 |
|---|---|
| `AUTH403` | 방 참여자가 아님 |
| `CHAT4001` | 내용이 비었거나 `IMAGE`인데 `imageUrl` 없음 [D13] |
| `CHAT4091` | 종료된 채팅방 |

---

## 10. 후기·신고·차단

### GET /api/posts/{postId}/reviews/targets — 평가 대상

- 권한: 호스트·참여자 · 화면: 09-A (참여자 → 호스트), 09-B (호스트 → 참여자)
- 원본은 "평가 대상·작성 여부"까지만 정함. 응답 형식은 (보완)
- 참여자의 대상은 호스트 1명, 호스트의 대상은 참여자 전원입니다 (양방향 평가)

**Response**
```json
{
  "postId": 3,
  "postTitle": "코스트코 생수 2L x 24 같이 사요",
  "postStatus": "SETTLED",
  "myRole": "HOST",
  "settledAt": "2026-10-31T10:00:00",
  "writableUntil": "2026-11-03T10:00:00",
  "canWrite": true,
  "targets": [
    { "userId": 15, "nickname": "졸린 감자", "role": "PARTICIPANT", "units": 1, "written": false }
  ]
}
```

| 필드 | 설명 |
|---|---|
| `writableUntil` | `settledAt + 3일` [D12] |
| `canWrite` | `SETTLED` + 작성 기간 안 + 안 쓴 대상이 있음 |

**Errors**: `AUTH403`

### POST /api/posts/{postId}/reviews — 후기 작성 (배열)

- 권한: 호스트·참여자 · 화면: 09-A, 09-B
- 선행조건: `SETTLED`, **정산 후 3일 이내** [D12]
- **작성한 후기는 수정할 수 없습니다** (수정 API 없음) [D12]

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `reviews` | array | Y | 1개 이상 |
| `reviews[].targetUserId` | long | Y | 평가 대상 |
| `reviews[].rating` | int | Y | 별점 1~5 |
| `reviews[].tags` | ReviewTag[] | N | 태그 |
| `reviews[].content` | string | N | 내용 |

```json
{
  "reviews": [
    { "targetUserId": 12, "rating": 4, "tags": ["PUNCTUAL", "CLEAN_SPLIT"], "content": "시간 약속 잘 지키셨어요" }
  ]
}
```

**Response**
```json
{ "createdReviewIds": [31] }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | 별점 범위 오류 등 |
| 400 | `REVIEW4001` | 평가할 수 없는 대상 |
| 403 | `AUTH403` | 이 공구의 호스트·참여자가 아님 |
| 409 | `REVIEW4091` | 같은 대상에게 이미 씀 |
| 409 | `REVIEW4092` | 작성 기간이 아님 (정산 전 또는 3일 지남) [D12] |

### GET /api/users/{userId}/reviews — 받은 후기 목록

- 권한: 로그인 · 화면: 08

**Query**: `page`, `size` (기본 `0`, `20`)

**Response** (보완) — `content[]` 한 개, 최신순
```json
{
  "reviewId": 31,
  "reviewerId": 15,
  "reviewerNickname": "졸린 감자",
  "reviewerRole": "PARTICIPANT",
  "postId": 3,
  "postTitle": "코스트코 생수 2L x 24 같이 사요",
  "rating": 4,
  "tags": ["PUNCTUAL", "CLEAN_SPLIT"],
  "content": "시간 약속 잘 지키셨어요",
  "createdAt": "2026-10-31"
}
```

**Errors**: `AUTH401`, `COMMON404`

### POST /api/reports — 사용자 신고

- 권한: 로그인 · 화면: 08-B, 03-D
- 신고 대상은 항상 사용자입니다. 신고는 `PENDING`으로 저장하고, 처리는 운영팀이 합니다 (관리자 화면은 나중에 추가) [D10]

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `targetUserId` | long | Y | 신고 대상 (본인 불가) |
| `postId` | long | N | 관련 게시글 |
| `reason` | ReportReason | Y | 신고 사유 |
| `detail.messageIds` | long[] | 조건 | `CHAT_ABUSE`면 1개 이상 |
| `detail.violationStage` | string | 조건 | `TRADE_VIOLATION`이면 위반 단계 |
| `content` | string | N | 내용 |
| `imageUrls` | string[] | N | 증빙 사진 |

```json
{
  "targetUserId": 12,
  "postId": 3,
  "reason": "TRADE_VIOLATION",
  "detail": { "messageIds": [], "violationStage": "RECEIPT" },
  "content": "영수증 금액과 실제 상품이 달라요",
  "imageUrls": []
}
```

**Response**
```json
{ "reportId": 4, "status": "PENDING" }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `COMMON400` | 필수 값 누락 |
| 400 | `REPORT4001` | 본인 신고 |
| 400 | `REPORT4002` | 사유별 필수 값 누락 |
| 404 | `COMMON404` | 대상 사용자 없음 |

### POST /api/users/{userId}/block — 차단

- 권한: 로그인 · 화면: 08
- 바디 없음. 차단하면 서로의 게시글이 목록에서 숨겨지고 서로의 공구에 참여할 수 없습니다

**Response** (보완)
```json
{ "blockedUserId": 12, "blockedAt": "2026-10-31T11:00:00" }
```

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 400 | `BLOCK4001` | 본인 차단 |
| 404 | `COMMON404` | 대상 사용자 없음 |
| 409 | `BLOCK4091` | 이미 차단함 |

### DELETE /api/users/{userId}/block — 차단 해제

- 권한: 로그인 · 화면: 08
- 바디 없음. 응답 `result`는 `null`

**Errors**

| HTTP | code | 상황 |
|---|---|---|
| 404 | `COMMON404` | 차단한 적 없음 |

### GET /api/users/me/blocks — 차단 목록

- 권한: 로그인 · 화면: 07

**Query**: `page`, `size` (기본 `0`, `20`)

**Response** (보완) — `content[]` 한 개, 최신순
```json
{ "userId": 12, "nickname": "배고픈 고구마", "dongName": "역삼동", "blockedAt": "2026-10-31T11:00:00" }
```

**Errors**: `AUTH401`

---

## 11. DB 결정에 맞춰 수정한 내용

원본 API 명세와 DB 설계 결정이 충돌하는 부분을 **DB 쪽에 맞춰** 고쳤습니다.

| # | 위치 | 원본 | 수정 | 근거 |
|---|---|---|---|---|
| 1 | `PostStatus` `DISTRIBUTED` | 나눔완료·수령확인 | 나눔 진행·수령확인 대기 | D6 |
| 2 | `PATCH .../order/distribution` | "나눔 일정·장소 + 나눔 완료" | **나눔 일정·장소 입력**. 호스트 나눔완료 버튼 없음, 입력하면 바로 `DISTRIBUTED` | D6 |
| 3 | 자동 수령 처리 | 기한 없음 | 나눔 예정 시각(`distributeAt`) + 3일, 문의중 제외. 응답에 `autoConfirmAt` 추가 | D10 |
| 4 | `PointTxType` | `INCENTIVE` 있음 | `INCENTIVE` 삭제 | D11 |
| 5 | 상태 전이표 7단계 | "CAPTURED → 호스트 정산 + 인센티브" | 인센티브 제거 | D11 |
| 6 | 포인트 서비스 시그니처 | `pay`, `refund`, `settle`, `giveIncentive` | `giveIncentive` 삭제, 차액 조정(`adjust`)·미수금 납부(`repayDebt`) 추가 | D9, D11 |
| 7 | `POST /api/posts` | 응답 `{postId, chatRoomId}` | 호스트 몫 결제 → `paidPoint`, `balanceAfter` 추가. 잔액 부족 에러 | D8 |
| 8 | `POST .../order` | `amountMatched`만 표시 | 차액 정산 (더 싸면 환급, 더 비싸면 추가 청구·미수금). 응답에 `difference`, `adjustments` 추가 | D9 |
| 9 | `PointTxType` | 5종 | `ADJUSTMENT_REFUND`, `ADJUSTMENT_CHARGE`, `DEBT_REPAYMENT` 추가 | D9 |
| 10 | 참여·등록 API | - | 미납 미수금이 있으면 `POINT4031`로 거부 | D9 |
| 11 | `GET /api/points/me` | 잔액·보관중 | `unpaidDebt` 추가 | D9 |
| 12 | `GET /api/points/debts` | 없음 | 미수금 목록 API 신규 | D9 |
| 13 | `POST .../charges/confirm` | 충전만 | 미수금 자동 차감, 응답에 `debtRepaid` 추가 | D9 |
| 14 | 로그인·재발급 | - | 정지 계정 `AUTH4031` | D9 |
| 15 | `ActionRequired` | 5종 | `PAY_DEBT` 추가, `WRITE_REVIEW`는 정산 후 3일 안에만 | D9, D12 |
| 16 | `MessageType` | `TEXT`, `SYSTEM` | `IMAGE` 추가 | D13 |
| 17 | STOMP 발행 | `{ "content": "..." }` | `{ "type", "content", "imageUrl" }` | D13 |
| 18 | 메시지 조회·수신 응답 | `content`만 | `imageUrl` 추가, 이미지면 `content = null`. 목록 `lastMessage`는 "사진" | D13 |
| 19 | 후기 작성 | 기한·수정 미정 | 정산 후 3일까지, 수정 불가. `writableUntil = settledAt + 3일` | D12 |
| 20 | 수령 문의 | 해결 주체 미정 | 운영팀이 처리. 응답에 `status`, `resolution`, `refundAmount`, `resolvedAt` 추가 | D10 |
| 21 | `statusTimeline` | 5개 시각 | `cancelledAt` 추가 | D5 |
| 22 | `PATCH /api/posts/{postId}` | 모집중 + 참여자 없을 때 수정 | 가격·단위 관련 필드는 수정 불가 (호스트 보류 금액이 바뀌므로) | D8 |
| 23 | 충전 `orderId` | - | 이름은 그대로, DB에서는 `pg_order_id`에 저장 | D1 |

---

## 12. 남은 미정 사항

API에 영향을 주는 것만 모았습니다. 전체 목록은 [DB.md 8.1](DB.md)을 참고하세요.

- [ ] 모집 마감 시 인원 미달 자동 취소 (보류) — 정해지면 스케줄러 동작과 `CANCELLED` 사유 추가
- [ ] 문의중 참여자가 있을 때 나머지 몫을 먼저 정산할지, 전체 정산을 미룰지 (현재 명세: 전원 `RECEIVED`여야 정산)
- [ ] 운영팀 수령 문의 처리 기한, 운영팀 처리 API (관리자 화면은 나중에 추가)
- [ ] 계정 정지 기간 (기간 vs 영구)
- [ ] 충전 최소·최대 금액, 남은 포인트 현금 환불
- [ ] 이미지 형식·용량·개수 제한, 저장소
- [ ] 후기 삭제 허용 여부
- [ ] 알림 방식 (현재는 차액 청구·미수금 알림을 채팅 SYSTEM 메시지로 대신함)
- [ ] 거리 검색 방식 (위경도 단순 계산 vs PostGIS)
