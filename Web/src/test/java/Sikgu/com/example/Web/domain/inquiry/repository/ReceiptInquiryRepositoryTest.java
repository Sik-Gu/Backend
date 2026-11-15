package Sikgu.com.example.Web.domain.inquiry.repository;

import static org.assertj.core.api.Assertions.assertThat;

import Sikgu.com.example.Web.domain.inquiry.entity.ReceiptInquiry;
import Sikgu.com.example.Web.domain.inquiry.enums.InquiryEnums.InquiryType;
import Sikgu.com.example.Web.domain.participation.entity.Participation;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.support.Fixtures;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ReceiptInquiryRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 15, 12, 0);

    @Autowired
    ReceiptInquiryRepository repository;
    @Autowired
    EntityManager em;

    @Test
    void 공구의_문의를_사진과_함께_조회하고_내_문의만_따로_조회한다() {
        User host = persist(Fixtures.newUser("host"));
        User a = persist(Fixtures.newUser("aaaa"));
        User b = persist(Fixtures.newUser("bbbb"));
        Post post = Fixtures.post(host, NOW.plusDays(1));
        em.persist(post);
        Participation aPa = Participation.participant(post, a, 1, NOW);
        Participation bPa = Participation.participant(post, b, 1, NOW);
        em.persist(aPa);
        em.persist(bPa);
        repository.save(ReceiptInquiry.open(aPa, InquiryType.WRONG_OR_DAMAGED, null, null, "파손",
                List.of("/i1.jpg", "/i2.jpg")));
        repository.save(ReceiptInquiry.open(bPa, InquiryType.NOT_RECEIVED, null, null, "못 받음", List.of()));
        em.flush();
        em.clear();

        List<ReceiptInquiry> all = repository.findByPostId(post.getId());
        List<ReceiptInquiry> mine = repository.findByPostIdAndUserId(post.getId(), a.getId());

        assertThat(all).hasSize(2);
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).getImageUrls()).containsExactly("/i1.jpg", "/i2.jpg");
        assertThat(mine.get(0).getParticipation().getUser().getLoginId()).isEqualTo("aaaa");
    }

    private User persist(User user) {
        em.persist(user);
        return user;
    }
}
