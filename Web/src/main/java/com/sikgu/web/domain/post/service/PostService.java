package com.sikgu.web.domain.post.service;

import com.sikgu.web.domain.post.dto.CreatePostRequest;
import com.sikgu.web.domain.post.dto.CreatePostResponse;
import com.sikgu.web.domain.post.dto.PostDetailResponse;
import com.sikgu.web.domain.post.dto.UpdatePostRequest;
import com.sikgu.web.domain.post.entity.Post;
import com.sikgu.web.domain.post.repository.PostRepository;
import com.sikgu.web.domain.user.entity.User;
import com.sikgu.web.domain.user.repository.UserRepository;
import com.sikgu.web.global.exception.ErrorCode;
import com.sikgu.web.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreatePostResponse createPost(Long userId, CreatePostRequest request) {
        // 1. 호스트 = 로그인한 사용자
        User host = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 2. 가격·수량 규칙 검사
        if (request.totalPrice() % request.totalUnits() != 0) {
            throw new GeneralException(ErrorCode.PRICE_NOT_DIVISIBLE);
        }
        if (request.hostUnits() >= request.totalUnits()) {
            throw new GeneralException(ErrorCode.INVALID_HOST_UNITS);
        }

        // 3. 저장
        CreatePostRequest.TradePlaceRequest tradePlace = request.tradePlace();
        Post post = Post.builder()
                .host(host)
                .title(request.title())
                .content(request.content())
                .productName(request.productName())
                .productLink(request.productLink())
                .category(request.category())
                .baseUnit(request.baseUnit())
                .totalUnits(request.totalUnits())
                .totalPrice(request.totalPrice())
                .hostUnits(request.hostUnits())
                .tradePlaceName(tradePlace.name())
                .latitude(tradePlace.latitude())
                .longitude(tradePlace.longitude())
                .deadlineAt(request.deadlineAt())
                .build();

        Post savedPost = postRepository.save(post);
        return new CreatePostResponse(savedPost.getId());
    }

    @Transactional
    public PostDetailResponse getPost(Long postId) {
        // 1. 조회수 +1 (0줄이면 게시글 없음)
        int updated = postRepository.increaseViewCount(postId);
        if (updated == 0) {
            throw new GeneralException(ErrorCode.POST_NOT_FOUND);
        }

        // 2. 게시글 + 호스트 조회
        Post post = postRepository.findWithHostById(postId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        return PostDetailResponse.from(post);
    }

    @Transactional
    public void updatePost(Long userId, Long postId, UpdatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        if (!post.isHost(userId)) {
            throw new GeneralException(ErrorCode.NOT_POST_HOST);
        }
        if (!post.isRecruiting()) {
            throw new GeneralException(ErrorCode.POST_NOT_RECRUITING);
        }
        // TODO(5번 참여): 호스트 말고 다른 참여자가 있으면 수정 불가

        UpdatePostRequest.TradePlaceRequest tradePlace = request.tradePlace();
        post.update(
                request.title(),
                request.productName(),
                request.productLink(),
                request.category(),
                request.content(),
                request.deadlineAt(),
                tradePlace != null ? tradePlace.name() : null,
                tradePlace != null ? tradePlace.latitude() : null,
                tradePlace != null ? tradePlace.longitude() : null
        );
    }
}