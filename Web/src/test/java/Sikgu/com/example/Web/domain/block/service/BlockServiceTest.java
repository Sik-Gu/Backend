package Sikgu.com.example.Web.domain.block.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import Sikgu.com.example.Web.domain.block.entity.Block;
import Sikgu.com.example.Web.domain.block.repository.BlockRepository;
import Sikgu.com.example.Web.domain.user.repository.UserRepository;
import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import Sikgu.com.example.Web.support.Fixtures;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock
    BlockRepository blockRepository;
    @Mock
    UserRepository userRepository;

    @InjectMocks
    BlockService blockService;

    @Test
    void 사용자를_차단한다() {
        given(userRepository.findById(2L)).willReturn(Optional.of(Fixtures.user(2L)));
        given(userRepository.getReferenceById(1L)).willReturn(Fixtures.user(1L));

        assertThat(blockService.block(1L, 2L).blockedUserId()).isEqualTo(2L);
        verify(blockRepository).saveAndFlush(any(Block.class));
    }

    @Test
    void 자기_자신은_차단할_수_없다() {
        assertThatThrownBy(() -> blockService.block(1L, 1L))
                .isInstanceOf(GeneralException.class)
                .extracting("errorStatus").isEqualTo(ErrorStatus.CANNOT_BLOCK_SELF);
    }

    @Test
    void 없는_사용자면_COMMON404() {
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> blockService.block(1L, 2L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.USER_NOT_FOUND);
    }

    @Test
    void 이미_차단했으면_BLOCK4091() {
        given(userRepository.findById(2L)).willReturn(Optional.of(Fixtures.user(2L)));
        given(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> blockService.block(1L, 2L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.ALREADY_BLOCKED);
        verify(blockRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동시에_두번_눌러_UNIQUE에_걸려도_BLOCK4091() {
        given(userRepository.findById(2L)).willReturn(Optional.of(Fixtures.user(2L)));
        given(userRepository.getReferenceById(1L)).willReturn(Fixtures.user(1L));
        given(blockRepository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> blockService.block(1L, 2L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.ALREADY_BLOCKED);
    }

    @Test
    void 차단한_적이_없으면_해제할_때_COMMON404() {
        given(blockRepository.findByBlockerIdAndBlockedId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> blockService.unblock(1L, 2L))
                .extracting("errorStatus").isEqualTo(ErrorStatus.BLOCK_NOT_FOUND);
    }
}
