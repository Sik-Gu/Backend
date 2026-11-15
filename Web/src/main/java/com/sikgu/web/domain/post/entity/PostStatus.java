package com.sikgu.web.domain.post.entity;

public enum PostStatus {
    RECRUITING,   // 모집중
    RECRUITED,    // 모집완료
    ORDERED,      // 주문완료
    SHIPPING,     // 배송중
    DISTRIBUTED,  // 나눔완료
    RECEIVED,     // 수령확인
    SETTLED,      // 정산완료
    CANCELED,     // 취소 (호스트)
    EXPIRED       // 마감 (인원 미달)
}