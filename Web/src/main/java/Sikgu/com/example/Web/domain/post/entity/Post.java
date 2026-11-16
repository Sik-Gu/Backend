package Sikgu.com.example.Web.domain.post.entity;

import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.search.PostSearchKeyword;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 공구 게시글 (docs/DB.md 4.2 posts). 상품 정보도 게시글에 포함한다 (D2).
 *
 * <p>상태는 행위 메서드로만 바뀐다. 프론트가 상태값을 직접 보내지 않는다.
 * filledUnits·participantCount는 참여할 때 게시글 행에 비관적 락을 잡고 바꾼다 (D3).
 */
@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(length = 2000)
    private String content;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(length = 500)
    private String productLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    /** 기본 단위 (예: "2L 생수 4병") */
    @Column(nullable = false, length = 30)
    private String baseUnit;

    /** 총 단위 수. 이만큼 차면 모집완료 */
    @Column(nullable = false)
    private int totalUnits;

    /** 지금까지 찬 단위 수 (호스트 포함) */
    @Column(nullable = false)
    private int filledUnits;

    /** 참여 인원 (호스트 포함) */
    @Column(nullable = false)
    private int participantCount;

    /** 호스트가 예상한 총 가격. 실제 가격과 다르면 영수증 등록 때 차액 정산 (D9) */
    @Column(nullable = false)
    private int totalPrice;

    /** 단위당 가격 = totalPrice / totalUnits */
    @Column(nullable = false)
    private int unitPrice;

    @Column(nullable = false)
    private LocalDateTime deadlineAt;

    /**
     * 글을 올릴 때의 호스트 집 위치 (GPS). 목록의 500m·1km 거리 필터 기준이다.
     * 호스트가 나중에 집 위치를 바꿔도 이 값은 그대로다.
     * <b>다른 사용자에게는 좌표를 절대 내려주지 않는다.</b> 거리도 구간(500m 이내·1km 이내)으로만 알려준다.
     */
    @Column(nullable = false)
    private Double hostLatitude;

    @Column(nullable = false)
    private Double hostLongitude;

    /** 글을 올릴 때의 호스트 동 이름 (화면 표시용) */
    @Column(nullable = false, length = 30)
    private String dongName;

    // ---------- 나눔 장소 (주문 전에 참여자 전원 합의) ----------

    /** 호스트가 제안한 나눔 장소. 아직 제안 전이면 null */
    @Column(length = 100)
    private String distributionPlaceName;

    /** 나눔 장소는 공개 장소라 좌표를 그대로 저장한다 (선택) */
    private Double distributionPlaceLatitude;

    private Double distributionPlaceLongitude;

    private LocalDateTime distributionPlaceProposedAt;

    // ---------- 검색 색인 (PostSearchKeyword) ----------

    /** 제목 + 상품명 단어 색인. 예: " 생수 2l 공구 " (정확한 단어 검색용) */
    @Column(nullable = false, length = 400)
    private String searchWords;

    /** 제목 초성 (공백 제거). 예: "ㅅㅅㄱㄱ" (초성 검색용) */
    @Column(nullable = false, length = 100)
    private String titleChosung;

    @Column(nullable = false)
    private int viewCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    // 상태 변경 시각 (D5)
    private LocalDateTime recruitedAt;
    private LocalDateTime orderedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime distributedAt;
    private LocalDateTime settledAt;
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PostImage> images = new ArrayList<>();

    public static Post create(User host, String title, String content, String productName, String productLink,
                              Category category, String baseUnit, int totalUnits, int totalPrice,
                              LocalDateTime deadlineAt) {
        Post post = new Post();
        post.host = host;
        post.title = title;
        post.content = content;
        post.productName = productName;
        post.productLink = productLink;
        post.category = category;
        post.baseUnit = baseUnit;
        post.totalUnits = totalUnits;
        post.totalPrice = totalPrice;
        post.unitPrice = totalPrice / totalUnits;
        post.deadlineAt = deadlineAt;
        // 호스트 집 위치를 글에 저장 (호출 전에 host.hasLocation() 확인)
        post.hostLatitude = host.getLatitude();
        post.hostLongitude = host.getLongitude();
        post.dongName = host.getDongName();
        post.filledUnits = 0;
        post.participantCount = 0;
        post.viewCount = 0;
        post.status = PostStatus.RECRUITING;
        post.refreshSearchIndex();
        return post;
    }

    // ---------- 조회용 ----------

    public boolean isHost(Long userId) {
        return host.getId().equals(userId);
    }

    public int remainingUnits() {
        return totalUnits - filledUnits;
    }

    public boolean isDeadlinePassed(LocalDateTime now) {
        return !deadlineAt.isAfter(now);
    }

    public List<String> imageUrls() {
        return images.stream().map(PostImage::getImageUrl).toList();
    }

    public String thumbnailUrl() {
        return images.isEmpty() ? null : images.get(0).getImageUrl();
    }

    /** 이 상태가 아니면 POST409 */
    public void requireStatus(PostStatus expected) {
        if (status != expected) {
            throw new GeneralException(ErrorStatus.INVALID_POST_STATUS);
        }
    }

    // ---------- 수정 ----------

    /** 공구 수정. null인 값은 그대로 둔다. 검색 색인도 같이 갱신한다 */
    public void update(String title, String productName, String productLink, Category category, String content,
                       LocalDateTime deadlineAt) {
        if (title != null) {
            this.title = title;
        }
        if (productName != null) {
            this.productName = productName;
        }
        if (productLink != null) {
            this.productLink = productLink;
        }
        if (category != null) {
            this.category = category;
        }
        if (content != null) {
            this.content = content;
        }
        if (deadlineAt != null) {
            this.deadlineAt = deadlineAt;
        }
        refreshSearchIndex();
    }

    /** 제목·상품명이 바뀌면 검색 색인도 다시 만든다 */
    @PrePersist
    @PreUpdate
    void refreshSearchIndex() {
        this.searchWords = PostSearchKeyword.wordIndex(title, productName);
        this.titleChosung = PostSearchKeyword.chosung(title);
    }

    // ---------- 나눔 장소 ----------

    public boolean hasDistributionPlace() {
        return distributionPlaceName != null;
    }

    /**
     * 나눔 장소 제안·변경 (호스트). 주문 전(모집중·모집완료)에만 할 수 있다.
     * 장소가 바뀌면 참여자 동의는 서비스에서 모두 초기화한다.
     */
    public void proposeDistributionPlace(String name, Double latitude, Double longitude, LocalDateTime now) {
        requireBeforeOrder();
        this.distributionPlaceName = name;
        this.distributionPlaceLatitude = latitude;
        this.distributionPlaceLongitude = longitude;
        this.distributionPlaceProposedAt = now;
    }

    /** 주문 전(모집중·모집완료)이 아니면 POST409 */
    public void requireBeforeOrder() {
        if (status != PostStatus.RECRUITING && status != PostStatus.RECRUITED) {
            throw new GeneralException(ErrorStatus.INVALID_POST_STATUS);
        }
    }

    /** 이미지 목록 전체를 바꾼다 */
    public void replaceImages(List<String> imageUrls) {
        images.clear();
        if (imageUrls == null) {
            return;
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(PostImage.of(this, imageUrls.get(i), i));
        }
    }

    // ---------- 참여 (게시글 행 락 안에서 호출) ----------

    /**
     * 단위를 채운다. 다 차면 true.
     *
     * @throws GeneralException PART4001 남은 단위 초과
     */
    public boolean addParticipant(int units) {
        if (units > remainingUnits()) {
            throw new GeneralException(ErrorStatus.EXCEED_REMAINING_UNITS);
        }
        filledUnits += units;
        participantCount++;
        return filledUnits == totalUnits;
    }

    /** 참여 취소. 모집완료였으면 모집중으로 돌아간다. */
    public void removeParticipant(int units) {
        filledUnits -= units;
        participantCount--;
        if (status == PostStatus.RECRUITED) {
            status = PostStatus.RECRUITING;
            recruitedAt = null;
        }
    }

    // ---------- 상태 변경 ----------

    public void markRecruited(LocalDateTime now) {
        requireStatus(PostStatus.RECRUITING);
        status = PostStatus.RECRUITED;
        recruitedAt = now;
    }

    public void markOrdered(LocalDateTime now) {
        requireStatus(PostStatus.RECRUITED);
        status = PostStatus.ORDERED;
        orderedAt = now;
    }

    public void markShipping(LocalDateTime now) {
        requireStatus(PostStatus.ORDERED);
        status = PostStatus.SHIPPING;
        shippedAt = now;
    }

    /** 호스트가 나눔 일정·장소를 입력하면 바로 DISTRIBUTED (D6) */
    public void markDistributed(LocalDateTime now) {
        requireStatus(PostStatus.SHIPPING);
        status = PostStatus.DISTRIBUTED;
        distributedAt = now;
    }

    public void markSettled(LocalDateTime now) {
        requireStatus(PostStatus.DISTRIBUTED);
        status = PostStatus.SETTLED;
        settledAt = now;
    }

    /** 주문완료 전(모집중·모집완료)에만 취소할 수 있다 */
    public void cancel(LocalDateTime now) {
        if (status != PostStatus.RECRUITING && status != PostStatus.RECRUITED) {
            throw new GeneralException(ErrorStatus.POST_NOT_CANCELLABLE);
        }
        status = PostStatus.CANCELLED;
        cancelledAt = now;
    }
}
