package Sikgu.com.example.Web.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 영수증 사진 (docs/DB.md 4.6 receipt_images)
 */
@Entity
@Table(name = "receipt_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReceiptImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PostOrder order;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    static ReceiptImage of(PostOrder order, String imageUrl) {
        ReceiptImage image = new ReceiptImage();
        image.order = order;
        image.imageUrl = imageUrl;
        return image;
    }
}
