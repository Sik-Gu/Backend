package Sikgu.com.example.Web.domain.review.entity;

import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.review.enums.ReviewTag;
import Sikgu.com.example.Web.domain.user.entity.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 후기 (docs/DB.md 4.8 reviews). 정산 후 3일 안에만 쓰고, 수정할 수 없다 (D12).
 */
@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"writer_id", "post_id", "target_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private User writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    /** 별점 1~5 */
    @Column(nullable = false)
    private int rating;

    @Column(length = 500)
    private String content;

    @ElementCollection
    @CollectionTable(name = "review_tags", joinColumns = @JoinColumn(name = "review_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 20)
    @BatchSize(size = 50)
    private Set<ReviewTag> tags = EnumSet.noneOf(ReviewTag.class);

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Review write(User writer, Post post, User target, int rating, Set<ReviewTag> tags, String content) {
        Review review = new Review();
        review.writer = writer;
        review.post = post;
        review.target = target;
        review.rating = rating;
        review.content = content;
        if (tags != null) {
            review.tags.addAll(tags);
        }
        return review;
    }
}
