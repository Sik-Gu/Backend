package com.sikgu.web.domain.post.entity;

import com.sikgu.web.domain.user.entity.User;
import com.sikgu.web.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    // 호스트 (글 쓴 사람)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(length = 500)
    private String productLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    // 단위·가격
    @Column(nullable = false, length = 50)
    private String baseUnit;

    @Column(nullable = false)
    private Integer totalUnits;

    @Column(nullable = false)
    private Integer totalPrice;

    @Column(nullable = false)
    private Integer unitPrice;      // 총 가격 ÷ 총 단위 수

    @Column(nullable = false)
    private Integer filledUnits;    // 모인 단위 수 (호스트 구매분 포함)

    // 거래 장소
    @Column(nullable = false, length = 100)
    private String tradePlaceName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Column(nullable = false)
    private LocalDateTime deadlineAt;

    @Column(nullable = false)
    private Integer viewCount;

    // 동시성 제어 (마지막 단위에 동시에 참여할 때)
    @Version
    private Long version;

    @Builder
    private Post(User host, String title, String content, String productName, String productLink,
                 Category category, String baseUnit, Integer totalUnits, Integer totalPrice,
                 Integer hostUnits, String tradePlaceName, Double latitude, Double longitude,
                 LocalDateTime deadlineAt) {
        this.host = host;
        this.title = title;
        this.content = content;
        this.productName = productName;
        this.productLink = productLink;
        this.category = category;
        this.baseUnit = baseUnit;
        this.totalUnits = totalUnits;
        this.totalPrice = totalPrice;
        this.unitPrice = totalPrice / totalUnits;
        this.filledUnits = hostUnits;
        this.tradePlaceName = tradePlaceName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.deadlineAt = deadlineAt;
        this.status = PostStatus.RECRUITING;
        this.viewCount = 0;
    }

    // 호스트인지 확인
    public boolean isHost(Long userId) {
        return host.getId().equals(userId);
    }

    // 모집중인지 확인
    public boolean isRecruiting() {
        return status == PostStatus.RECRUITING;
    }

    // 수정 (null이 아닌 값만 바꿈)
    public void update(String title, String productName, String productLink, Category category,
                       String content, LocalDateTime deadlineAt,
                       String tradePlaceName, Double latitude, Double longitude) {
        if (title != null) this.title = title;
        if (productName != null) this.productName = productName;
        if (productLink != null) this.productLink = productLink;
        if (category != null) this.category = category;
        if (content != null) this.content = content;
        if (deadlineAt != null) this.deadlineAt = deadlineAt;
        if (tradePlaceName != null) {
            this.tradePlaceName = tradePlaceName;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    // 취소 (호스트가 모집 중에 취소)
    public void cancel() {
        this.status = PostStatus.CANCELED;
    }
}