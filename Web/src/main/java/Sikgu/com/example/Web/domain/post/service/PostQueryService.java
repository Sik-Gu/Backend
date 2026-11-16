package Sikgu.com.example.Web.domain.post.service;

import Sikgu.com.example.Web.domain.post.dto.response.PostResponses.PostCard;
import Sikgu.com.example.Web.domain.post.entity.Post;
import Sikgu.com.example.Web.domain.post.enums.Category;
import Sikgu.com.example.Web.domain.post.enums.DistanceRange;
import Sikgu.com.example.Web.domain.post.enums.PostSort;
import Sikgu.com.example.Web.domain.post.repository.PostRepository;
import Sikgu.com.example.Web.domain.post.repository.PostSearchCondition;
import Sikgu.com.example.Web.domain.post.repository.PostSearchRepository.PostSearchRow;
import Sikgu.com.example.Web.domain.post.search.PostSearchKeyword;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.PageResponse;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
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

/**
 * 공구 목록·검색 (docs/API.md 6장 GET /api/posts)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    /** 허용하는 반경 (m): 500m, 1km */
    static final Set<Integer> ALLOWED_RADIUS = Set.of(DistanceRange.NEAR_METERS, DistanceRange.FAR_METERS);

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
        if (!viewer.hasLocation()) {
            throw new GeneralException(ErrorStatus.LOCATION_REQUIRED);
        }
        PostSearchKeyword.Parsed parsedKeyword = parseKeyword(keyword);

        Page<PostSearchRow> rows = postRepository.search(new PostSearchCondition(
                viewerId, viewer.getLatitude(), viewer.getLongitude(),
                parsedKeyword, category, radius, recruitingOnly, sort,
                LocalDateTime.now(clock), page, size));

        // ID 순서(정렬 결과)를 지키면서 게시글을 한 번에 불러온다
        Map<Long, Post> posts = postRepository.findAllWithImagesByIdIn(
                        rows.getContent().stream().map(PostSearchRow::postId).toList())
                .stream().collect(Collectors.toMap(Post::getId, Function.identity()));
        List<PostCard> cards = rows.getContent().stream()
                .map(row -> toCard(posts.get(row.postId()), DistanceRange.of(row.distanceMeters())))
                .toList();
        return PageResponse.of(cards, page, size, rows.getTotalElements());
    }

    /** 정확한 단어 또는 초성만 허용. 섞인 입력("생ㅅ") 등은 SEARCH4001 */
    private static PostSearchKeyword.Parsed parseKeyword(String keyword) {
        try {
            return PostSearchKeyword.parse(keyword);
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ErrorStatus.INVALID_SEARCH_KEYWORD);
        }
    }

    /** 목록 카드. 내 공구함에서도 같은 형식을 쓴다. 거리는 구간으로만 (좌표·정확한 거리 비노출) */
    public static PostCard toCard(Post post, DistanceRange distance) {
        return new PostCard(post.getId(), post.getTitle(), post.thumbnailUrl(), post.getCategory(),
                post.getUnitPrice(), post.getBaseUnit(), post.getTotalUnits(), post.getFilledUnits(),
                post.getParticipantCount(), post.getDongName(), distance, post.getStatus(), post.getViewCount(),
                post.getDeadlineAt(), post.getCreatedAt());
    }
}
