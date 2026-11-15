package Sikgu.com.example.Web.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import Sikgu.com.example.Web.domain.block.entity.Block;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.post.enums.PostSort;
import Sikgu.com.example.Web.domain.post.enums.PostStatus;
import Sikgu.com.example.Web.domain.post.repository.PostSearchRepository.PostSearchRow;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.support.Fixtures;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 거리 계산 네이티브 쿼리를 H2(메모리 DB)로 실제 실행해서 확인한다.
 * 기준 위치: 역삼동(37.5006, 127.0364)
 */
@DataJpaTest
class PostSearchRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);
    private static final double LAT = 37.5006;
    private static final double LNG = 127.0364;

    @Autowired
    PostRepository postRepository;
    @Autowired
    EntityManager em;

    User viewer;
    User host;

    @BeforeEach
    void setUp() {
        viewer = persist(Fixtures.newUser("viewer"));
        host = persist(Fixtures.newUser("host"));
    }

    @Test
    void 반경_안의_글만_가깝게_계산한_거리와_함께_돌려준다() {
        Post near = save("가까운 생수", 37.5012, 127.0396, 5000, NOW.plusDays(1));   // 약 0.3km
        Post mid = save("2km 생수", 37.5180, 127.0364, 5000, NOW.plusDays(1));      // 약 1.9km
        save("먼 생수", 37.5850, 127.0364, 5000, NOW.plusDays(1));                  // 약 9.4km

        Page<PostSearchRow> within1 = search(null, null, 1, true, PostSort.LATEST);
        Page<PostSearchRow> within3 = search(null, null, 3, true, PostSort.LATEST);

        assertThat(within1.getContent()).extracting(PostSearchRow::postId).containsExactly(near.getId());
        assertThat(within1.getContent().get(0).distanceKm()).isBetween(0.2, 0.4);
        assertThat(within3.getContent()).extracting(PostSearchRow::postId)
                .containsExactlyInAnyOrder(near.getId(), mid.getId());
        assertThat(within3.getTotalElements()).isEqualTo(2);
    }

    @Test
    void 제목이나_상품명에_검색어가_있는_글만_대소문자_무시하고_찾는다() {
        Post water = save("WATER 공구", 37.5012, 127.0396, 5000, NOW.plusDays(1));
        save("휴지 공구", 37.5012, 127.0396, 5000, NOW.plusDays(1));

        assertThat(search("water", null, 3, true, PostSort.LATEST).getContent())
                .extracting(PostSearchRow::postId).containsExactly(water.getId());
        // 상품명(커클랜드 생수)으로도 찾는다
        assertThat(search("커클랜드", null, 3, true, PostSort.LATEST).getTotalElements()).isEqualTo(2);
        // % 는 글자 그대로 찾는다
        assertThat(search("%", null, 3, true, PostSort.LATEST).getTotalElements()).isZero();
    }

    @Test
    void 모집중만_보기와_전체_보기() {
        save("모집중", 37.5012, 127.0396, 5000, NOW.plusDays(1));
        setStatus(save("주문완료", 37.5012, 127.0396, 5000, NOW.plusDays(1)), PostStatus.ORDERED);
        setStatus(save("취소", 37.5012, 127.0396, 5000, NOW.plusDays(1)), PostStatus.CANCELLED);

        assertThat(search(null, null, 3, true, PostSort.LATEST).getTotalElements()).isEqualTo(1);
        // 전체 보기는 취소만 뺀다
        assertThat(search(null, null, 3, false, PostSort.LATEST).getTotalElements()).isEqualTo(2);
    }

    @Test
    void 카테고리로_거른다() {
        save("식품", 37.5012, 127.0396, 5000, NOW.plusDays(1));

        assertThat(search(null, Category.FOOD, 3, true, PostSort.LATEST).getTotalElements()).isEqualTo(1);
        assertThat(search(null, Category.LIVING, 3, true, PostSort.LATEST).getTotalElements()).isZero();
    }

    @Test
    void 정렬_마감임박순은_마감이_지난_글을_빼고_가격순은_싼_순서() {
        Post soon = save("곧 마감", 37.5012, 127.0396, 6000, NOW.plusHours(1));
        Post later = save("나중 마감", 37.5012, 127.0396, 3000, NOW.plusDays(3));
        save("마감 지남", 37.5012, 127.0396, 1200, NOW.minusHours(1));

        assertThat(search(null, null, 3, true, PostSort.DEADLINE).getContent())
                .extracting(PostSearchRow::postId).containsExactly(soon.getId(), later.getId());
        assertThat(search(null, null, 3, true, PostSort.PRICE).getContent().get(0).postId())
                .isNotEqualTo(soon.getId());
    }

    @Test
    void 인기순은_조회수가_많은_순서() {
        Post popular = save("인기", 37.5012, 127.0396, 5000, NOW.plusDays(1));
        save("보통", 37.5012, 127.0396, 5000, NOW.plusDays(1));
        ReflectionTestUtils.setField(popular, "viewCount", 10);
        em.flush();

        assertThat(search(null, null, 3, true, PostSort.POPULAR).getContent().get(0).postId())
                .isEqualTo(popular.getId());
    }

    @Test
    void 차단은_양방향으로_글을_숨긴다() {
        User blockedHost = persist(Fixtures.newUser("blocked"));
        User blockerHost = persist(Fixtures.newUser("blocker"));
        Post visible = save("보임", 37.5012, 127.0396, 5000, NOW.plusDays(1));
        savePostOf(blockedHost);
        savePostOf(blockerHost);
        em.persist(Block.of(viewer, blockedHost)); // 내가 차단
        em.persist(Block.of(blockerHost, viewer)); // 나를 차단
        em.flush();

        assertThat(search(null, null, 3, true, PostSort.LATEST).getContent())
                .extracting(PostSearchRow::postId).containsExactly(visible.getId());
    }

    @Test
    void 페이지를_나눈다() {
        for (int i = 0; i < 5; i++) {
            save("글" + i, 37.5012, 127.0396, 5000, NOW.plusDays(1));
        }

        Page<PostSearchRow> second = postRepository.search(new PostSearchCondition(viewer.getId(), LAT, LNG,
                null, null, 3, true, PostSort.LATEST, NOW, 1, 2));

        assertThat(second.getContent()).hasSize(2);
        assertThat(second.getTotalElements()).isEqualTo(5);
        assertThat(second.hasNext()).isTrue();
    }

    private Page<PostSearchRow> search(String keyword, Category category, int radius, boolean recruitingOnly,
                                       PostSort sort) {
        return postRepository.search(new PostSearchCondition(viewer.getId(), LAT, LNG, keyword, category, radius,
                recruitingOnly, sort, NOW, 0, 20));
    }

    private Post save(String title, double lat, double lng, int unitPriceTimesSix, LocalDateTime deadlineAt) {
        Post post = Post.create(host, title, null, "커클랜드 생수", null, Category.FOOD, "단위", 6,
                unitPriceTimesSix * 6, deadlineAt, "정문", "역삼동", lat, lng);
        postRepository.save(post);
        em.flush();
        return post;
    }

    private void savePostOf(User owner) {
        postRepository.save(Post.create(owner, "숨김", null, "상품", null, Category.FOOD, "단위", 6, 6000,
                NOW.plusDays(1), "정문", "역삼동", 37.5012, 127.0396));
    }

    private void setStatus(Post post, PostStatus status) {
        ReflectionTestUtils.setField(post, "status", status);
        em.flush();
    }

    private User persist(User user) {
        em.persist(user);
        return user;
    }
}
