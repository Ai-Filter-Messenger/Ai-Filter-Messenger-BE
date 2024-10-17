package sisyphus_core.sisyphus_core.chat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRequest;
import sisyphus_core.sisyphus_core.auth.service.UserService;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomRequest;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomResponse;
import sisyphus_core.sisyphus_core.chat.service.ChatRoomService;

import java.util.List;

@SpringBootTest
public class ChatTest {

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private UserService userService;

    @BeforeEach
    void before(){
        UserRequest.register register1= UserRequest.register.builder()
                .loginId("test1")
                .password("1234")
                .nickname("test1")
                .name("test1")
                .build();

        UserRequest.register register2= UserRequest.register.builder()
                .loginId("test2")
                .password("1234")
                .nickname("test2")
                .name("test2")
                .build();

        UserRequest.register register3= UserRequest.register.builder()
                .loginId("test3")
                .password("1234")
                .nickname("test3")
                .name("test3")
                .build();

        userService.register(register1);
        userService.register(register2);
        userService.register(register3);
    }

    @AfterEach
    void after(){
        userService.deleteAll();
        chatRoomService.deleteAll();
    }

    @Test
    @DisplayName("채팅방 생성")
    void createChatRoom(){
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("채팅방1번")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createRoom(chatRegister);
        List<ChatRoomResponse> roomResponses = chatRoomService.userChatRoomList("test1");

        Assertions.assertThat(roomResponses.size()).isEqualTo(1);

    }

    @Test
    @DisplayName("커스텀룸네임 채팅방 초대")
    void inviteCustomChatRoom(){
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("채팅방1번")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("채팅방1번");

        ChatRoomRequest.invite invite = ChatRoomRequest.invite.builder()
                .chatRoomId(room.getChatRoomId())
                .loginId("test3")
                .build();
        chatRoomService.inviteChatRoom(invite);

        List<ChatRoomResponse> roomResponses = chatRoomService.userChatRoomList("test3");

        Assertions.assertThat(roomResponses.size()).isEqualTo(1);
        Assertions.assertThat(roomResponses.get(0).getUserCount()).isEqualTo(3);
        Assertions.assertThat(roomResponses.get(0).getRoomName()).isEqualTo("채팅방1번");
    }

    @Test
    @DisplayName("일반룸네임 채팅방 초대")
    void inviteGeneralChatRoom(){
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("test1, test2");

        ChatRoomRequest.invite invite = ChatRoomRequest.invite.builder()
                .chatRoomId(room.getChatRoomId())
                .loginId("test3")
                .build();
        chatRoomService.inviteChatRoom(invite);

        List<ChatRoomResponse> roomResponses = chatRoomService.userChatRoomList("test3");

        Assertions.assertThat(roomResponses.size()).isEqualTo(1);
        Assertions.assertThat(roomResponses.get(0).getUserCount()).isEqualTo(3);
        Assertions.assertThat(roomResponses.get(0).getRoomName()).isEqualTo("test1, test2, test3");
    }

    //user 로직 추가하면 테스트 추가해야함
    @Test
    @DisplayName("오픈채팅방 입장")
    void joinCustomChatRoom(){
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("오픈채팅방")
                .nicknames(nicknames)
                .type("open")
                .build();
        chatRoomService.createRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("오픈채팅방");

        ChatRoomRequest.join join = ChatRoomRequest.join.builder()
                .chatRoomId(room.getChatRoomId())
                .build();

        chatRoomService.joinChatRoom("test3", join.getChatRoomId());

        List<ChatRoomResponse> roomResponses = chatRoomService.userChatRoomList("test3");

        Assertions.assertThat(roomResponses.size()).isEqualTo(1);
        Assertions.assertThat(roomResponses.get(0).getUserCount()).isEqualTo(3);
        Assertions.assertThat(roomResponses.get(0).getRoomName()).isEqualTo("오픈채팅방");
    }

    @Test
    @DisplayName("채팅방 나가기")
    void leaveChatRoom(){
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("test1, test2");

        ChatRoomRequest.leave leave = ChatRoomRequest.leave.builder()
                .chatRoomId(room.getChatRoomId())
                .loginId("test2")
                .build();

        chatRoomService.leaveChatRoom(leave);

        List<ChatRoomResponse> roomResponsesTest1 = chatRoomService.userChatRoomList("test1");
        List<ChatRoomResponse> roomResponsesTest2 = chatRoomService.userChatRoomList("test2");

        Assertions.assertThat(roomResponsesTest1.get(0).getRoomName()).isEqualTo("test1");
        Assertions.assertThat(roomResponsesTest1.get(0).getUserCount()).isEqualTo(1);
        Assertions.assertThat(roomResponsesTest2.size()).isEqualTo(0);
    }
}
