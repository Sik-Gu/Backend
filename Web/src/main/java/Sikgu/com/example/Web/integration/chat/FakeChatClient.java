package Sikgu.com.example.Web.integration.chat;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 채팅 기능이 구현되기 전까지 쓰는 가짜 구현. 아무것도 하지 않고, 채팅방 ID는 없음(null)으로 응답한다.
 *
 * <p>채팅 담당 팀원이 {@link ChatClient}를 구현하면 이 클래스를 지우면 된다.
 */
@Slf4j
@Component
public class FakeChatClient implements ChatClient {

    @Override
    public Long createRoom(Long postId, Long hostId, String name) {
        log.debug("[FakeChat] createRoom post={} host={}", postId, hostId);
        return null;
    }

    @Override
    public void join(Long postId, Long userId) {
        log.debug("[FakeChat] join post={} user={}", postId, userId);
    }

    @Override
    public void leave(Long postId, Long userId) {
        log.debug("[FakeChat] leave post={} user={}", postId, userId);
    }

    @Override
    public void close(Long postId) {
        log.debug("[FakeChat] close post={}", postId);
    }

    @Override
    public void sendSystemMessage(Long postId, String content) {
        log.debug("[FakeChat] system post={} : {}", postId, content);
    }

    @Override
    public Optional<Long> findRoomId(Long postId) {
        return Optional.empty();
    }
}
