package Sikgu.com.example.Web.domain.post.service;

import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCard;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.post.enums.PostSort;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.post.repository.PostSearchCondition;
import Sikgu.com.example.Web.domain.post.repository.PostSearchRepository.PostSearchRow;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.PageResponse;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.global.util.GeoUtils;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 공구 목록·검색 (docs/API.md 6장 GET /api/posts)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    /** 허용하는 반경 (km) */
    static final Set<Integer> ALLOWED_RADIUS = Set.of(1, 3, 5);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public PageResponse<PostCard> search(Long viewerId, String keyword, Category category, int radius,
                                         boolean recruitingOnly, PostSort sort, int page, int size) {
        if (!ALLOWED_RADIUS.contains(radius)) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        if (viewer.getLatitude() == null || viewer.getLongitude() == null) {
            throw new GeneralException(ErrorStatus.LOCATION_REQUIRED);
        }

        Page<PostSearchRow> rows = postRepository.search(new PostSearchCondition(
                viewerId, viewer.getLatitude(), viewer.getLongitude(),
                StringUtils.hasText(keyword) ? keyword.trim() : null, category, radius, recruitingOnly, sort,
                LocalDateTime.now(clock), page, size));

        // ID 순서(정렬 결과)를 지키면서 게시글을 한 번에 불러온다
        Map<Long, Post> posts = postRepository.findAllWithImagesByIdIn(
                        rows.getContent().stream().map(PostSearchRow::postId).toList())
                .stream().collect(Collectors.toMap(Post::getId, Function.identity()));
        List<PostCard> cards = rows.getContent().stream()
                .map(row -> toCard(posts.get(row.postId()), GeoUtils.roundOneDecimal(row.distanceKm())))
                .toList();
        return PageResponse.of(cards, page, size, rows.getTotalElements());
    }

    /** 목록 카드. 내 공구함에서도 같은 형식을 쓴다 */
    public static PostCard toCard(Post post, Double distanceKm) {
        return new PostCard(post.getId(), post.getTitle(), post.thumbnailUrl(), post.getCategory(),
                post.getUnitPrice(), post.getBaseUnit(), post.getTotalUnits(), post.getFilledUnits(),
                post.getParticipantCount(), post.getDongName(), distanceKm, post.getStatus(), post.getViewCount(),
                post.getDeadlineAt(), post.getCreatedAt());
    }
}
