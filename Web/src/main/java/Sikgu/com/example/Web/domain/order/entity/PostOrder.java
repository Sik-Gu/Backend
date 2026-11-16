package Sikgu.com.example.Web.domain.order.entity;

import Sikgu.com.example.Web.domain.post.entity.Post;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 정보 (docs/DB.md 4.5 orders). 공구 하나에 하나. 클래스 이름은 SQL 예약어 ORDER를 피해 PostOrder.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    /** 택배사. 오프라인 매장에서 직접 산 경우 없음 */
    @Column(length = 30)
    private String courier;

    @Column(length = 30)
    private String trackingNumber;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    /** 나눔 예정 일시 (표시용. 지나도 자동 수령 처리는 없음, D17) */
    private LocalDateTime distributeAt;

    /** 주문 전에 참여자 전원이 합의한 나눔 장소 (게시글에서 복사) */
    @Column(length = 100)
    private String distributePlace;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ReceiptImage> receiptImages = new ArrayList<>();

    /** 영수증 사진 등록. 금액은 받지 않는다 (차액 정산 없음, 게시글 가격 기준) */
    public static PostOrder register(Post post, List<String> receiptImageUrls, LocalDateTime now) {
        PostOrder order = new PostOrder();
        order.post = post;
        order.registeredAt = now;
        receiptImageUrls.forEach(url -> order.receiptImages.add(ReceiptImage.of(order, url)));
        return order;
    }

    public List<String> receiptImageUrls() {
        return receiptImages.stream().map(ReceiptImage::getImageUrl).toList();
    }

    public void ship(String courier, String trackingNumber) {
        this.courier = courier;
        this.trackingNumber = trackingNumber;
    }

    public void scheduleDistribution(LocalDateTime distributeAt, String distributePlace) {
        this.distributeAt = distributeAt;
        this.distributePlace = distributePlace;
    }
}
