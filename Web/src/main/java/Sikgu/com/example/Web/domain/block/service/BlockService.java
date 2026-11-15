package Sikgu.com.example.Web.domain.block.service;

import Sikgu.com.example.Web.domain.block.dto.response.BlockResponses.BlockResponse;
import Sikgu.com.example.Web.domain.block.dto.response.BlockResponses.BlockedUser;
import Sikgu.com.example.Web.domain.block.entity.Block;
import Sikgu.com.example.Web.domain.block.repository.BlockRepository;
import Sikgu.com.example.Web.domain.user.entity.User;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.PageResponse;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 차단·차단 해제·차단 목록 (docs/API.md 10장)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    @Transactional
    public BlockResponse block(Long blockerId, Long targetId) {
        if (blockerId.equals(targetId)) {
            throw new GeneralException(ErrorStatus.CANNOT_BLOCK_SELF);
        }
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId)) {
            throw new GeneralException(ErrorStatus.ALREADY_BLOCKED);
        }

        Block block = Block.of(userRepository.getReferenceById(blockerId), target);
        try {
            blockRepository.saveAndFlush(block); // 동시에 두 번 누른 경우 UNIQUE 제약으로 막는다
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(ErrorStatus.ALREADY_BLOCKED);
        }
        return new BlockResponse(targetId, block.getCreatedAt());
    }

    @Transactional
    public void unblock(Long blockerId, Long targetId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(blockerId, targetId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.BLOCK_NOT_FOUND));
        blockRepository.delete(block);
    }

    /** 내가 차단한 사용자만 보여준다. 나를 차단한 사용자는 보여주지 않는다. 최신순 */
    public PageResponse<BlockedUser> getMyBlocks(Long blockerId, int page, int size) {
        return PageResponse.of(
                blockRepository.findByBlockerIdOrderByCreatedAtDesc(blockerId, PageRequest.of(page, size)),
                block -> new BlockedUser(block.getBlocked().getId(), block.getBlocked().getNickname(),
                        block.getBlocked().getDongName(), block.getCreatedAt()));
    }

    /** 둘 중 한 명이라도 상대를 차단했는지 */
    public boolean isBlockedBetween(Long userA, Long userB) {
        return blockRepository.existsBetween(userA, userB);
    }
}
