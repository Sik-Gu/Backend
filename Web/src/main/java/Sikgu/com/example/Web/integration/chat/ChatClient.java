package Sikgu.com.example.Web.integration.chat;

import java.util.Optional;

/**
 * 공구 기능이 채팅 기능을 호출하는 창구.
 *
 * <p>채팅 담당 팀원이 이 인터페이스를 구현한다. 구현이 들어오기 전까지는 {@link FakeChatClient}가 대신 동작한다.
 * 모든 메서드는 호출한 쪽의 트랜잭션 안에서 실행된다 (전파 REQUIRED).
 */
public interface ChatClient {

    /** 공구 등록 시 채팅방을 만들고 호스트를 입장시킨다. 만든 채팅방 ID를 돌려준다. */
    Long createRoom(Long postId, Long hostId, String name);

    /** 공구 참여 시 채팅방 입장 (+ "○○님이 참여했어요" SYSTEM 메시지) */
    void join(Long postId, Long userId);

    /** 참여 취소 시 채팅방 퇴장 (+ SYSTEM 메시지) */
    void leave(Long postId, Long userId);

    /** 정산완료·취소 시 채팅방 종료(CLOSED) */
    void close(Long postId);

    /** 상태 변경·차액 청구 등 안내용 SYSTEM 메시지 */
    void sendSystemMessage(Long postId, String content);

    /** 공구의 채팅방 ID (공구 상세의 chatRoomId) */
    Optional<Long> findRoomId(Long postId);
}
