package sisyphus_core.sisyphus_core.chat;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRequest;
import sisyphus_core.sisyphus_core.auth.service.UserService;
import sisyphus_core.sisyphus_core.chat.exception.DuplicateChatRoomNameException;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.Message;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomRequest;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomResponse;
import sisyphus_core.sisyphus_core.chat.model.dto.MessageType;
import sisyphus_core.sisyphus_core.chat.repository.UserChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.service.ChatRoomService;
import sisyphus_core.sisyphus_core.chat.service.MessageService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Slf4j
public class ChatTest {

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserChatRoomRepository userChatRoomRepository;

    @BeforeEach
    void before(){
        UserRequest.register register1= UserRequest.register.builder()
                .loginId("test1")
                .password("1234")
                .nickname("test1")
                .email("test1@test.com")
                .name("test1")
                .build();

        UserRequest.register register2= UserRequest.register.builder()
                .loginId("test2")
                .password("1234")
                .nickname("test2")
                .email("test2@test.com")
                .name("test2")
                .build();

        UserRequest.register register3= UserRequest.register.builder()
                .loginId("test3")
                .password("1234")
                .nickname("test3")
                .email("test3@test.com")
                .name("test3")
                .build();

        UserRequest.register register4= UserRequest.register.builder()
                .loginId("test4")
                .password("1234")
                .nickname("test4")
                .email("test4@test.com")
                .name("test4")
                .build();

        userService.register(register1);
        userService.register(register2);
        userService.register(register3);
        userService.register(register4);
    }

    @AfterEach
    void after(){
        userService.deleteAll();
        chatRoomService.deleteAll();
    }

    @Test
    @DisplayName("개인 채팅방 생성")
    void createChatRoom1To1() throws InterruptedException {
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("채팅방1번")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createChatRoom(chatRegister);
        Thread.sleep(1000);

        ChatRoom room = chatRoomService.findByRoomName("채팅방1번");

        List<ChatRoomResponse> roomResponses = chatRoomService.userChatRoomList("test1");
        Message message = messageService.recentMessage(room.getChatRoomId());

        assertThat(message.getMessage()).isEqualTo("test1님이 test2님을 초대하였습니다.");
        assertThat(roomResponses.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("단체 채팅방 생성")
    void createChatRoom() throws InterruptedException {
        String[] nicknames = new String[]{"test2","test3","test4"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("채팅방1번")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createChatRoom(chatRegister);
        Thread.sleep(1000);

        ChatRoom room = chatRoomService.findByRoomName("채팅방1번");

        List<ChatRoomResponse> roomResponses = chatRoomService.userChatRoomList("test1");
        Message message = messageService.recentMessage(room.getChatRoomId());

        assertThat(message.getMessage()).isEqualTo("test1님이 test2님과 test3님과 test4님을 초대하였습니다.");
        assertThat(roomResponses.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("중복 오픈 채팅방 생성")
    void createDuplicateOpenChatRoom(){
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("채팅방1번")
                .nicknames(nicknames)
                .type("open")
                .build();

        chatRoomService.createChatRoom(chatRegister);
        assertThatThrownBy(() -> chatRoomService.createChatRoom(chatRegister))
                .isInstanceOf(DuplicateChatRoomNameException.class)
                .hasMessage("이미 존재하는 오픈채팅방입니다.");
    }

    @Test
    @DisplayName("중복 기본 채팅방 생성")
    void createDuplicateGeneralChatRoom(){
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("채팅방1번")
                .nicknames(nicknames)
                .type("general")
                .build();

        chatRoomService.createChatRoom(chatRegister);
        ChatRoom room = chatRoomService.createChatRoom(chatRegister);

        assertThat(room.getRoomName()).isEqualTo("채팅방1번");
    }

    @Test
    @DisplayName("채팅방 이름 변경")
    void modifyChatRoomName(){
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("채팅방1번")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createChatRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("채팅방1번");
        ChatRoomRequest.modify modify = ChatRoomRequest.modify.builder()
                        .chatRoomId(room.getChatRoomId())
                                .newRoomName("커스텀채팅방1번")
                                        .build();
        chatRoomService.modifyChatRoom(modify);
        ChatRoom room1 = chatRoomService.findByRoomName("커스텀채팅방1번");
        assertThat(room1).isNotNull();
        assertThat(room1.isCustomRoomName()).isTrue();

    }

    @Test
    @DisplayName("채팅방 이름 변경 시 존재하는 오픈 채팅방")
    void modifyDuplicateOpenChatRoomNameFail(){
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("채팅방1번")
                .nicknames(nicknames)
                .type("open")
                .build();
        ChatRoomRequest.register chatRegister2 = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("채팅방2번")
                .nicknames(nicknames)
                .type("open")
                .build();
        chatRoomService.createChatRoom(chatRegister);
        chatRoomService.createChatRoom(chatRegister2);

        ChatRoom room = chatRoomService.findByRoomName("채팅방1번");
        ChatRoomRequest.modify modify = ChatRoomRequest.modify.builder()
                .chatRoomId(room.getChatRoomId())
                .newRoomName("채팅방2번")
                .build();

        assertThatThrownBy(() -> chatRoomService.modifyChatRoom(modify))
                .isInstanceOf(DuplicateChatRoomNameException.class)
                .hasMessageContaining("이미 존재하는 오픈채팅방입니다.");
    }

    @Test
    @DisplayName("커스텀룸네임 채팅방 한명 초대")
    void inviteCustomChatRoom(){
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("채팅방1번")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createChatRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("채팅방1번");

        String[] nicknames2 = new String[]{"test3"};
        ChatRoomRequest.invite invite = ChatRoomRequest.invite.builder()
                .chatRoomId(room.getChatRoomId())
                .nicknames(nicknames2)
                .build();
        chatRoomService.inviteChatRoom(invite);

        List<ChatRoomResponse> roomResponses = chatRoomService.userChatRoomList("test3");

        assertThat(roomResponses.size()).isEqualTo(1);
        assertThat(roomResponses.get(0).getUserCount()).isEqualTo(3);
        assertThat(roomResponses.get(0).getRoomName()).isEqualTo("채팅방1번");
    }

    @Test
    @DisplayName("일반룸네임 채팅방 한명 초대")
    void inviteGeneralChatRoom() throws InterruptedException {
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createChatRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("test1, test2");

        String[] nicknames2 = new String[]{"test3"};
        ChatRoomRequest.invite invite = ChatRoomRequest.invite.builder()
                .invitorName("test1")
                .chatRoomId(room.getChatRoomId())
                .nicknames(nicknames2)
                .build();
        chatRoomService.inviteChatRoom(invite);
        Thread.sleep(1000);

        List<ChatRoomResponse> roomResponses = chatRoomService.userChatRoomList("test3");
        Message message = messageService.recentMessage(room.getChatRoomId());

        assertThat(message.getMessage()).isEqualTo("test1님이 test3님을 초대하였습니다.");
        assertThat(roomResponses.size()).isEqualTo(1);
        assertThat(roomResponses.get(0).getUserCount()).isEqualTo(3);
        assertThat(roomResponses.get(0).getRoomName()).isEqualTo("test1, test2, test3");
    }

    @Test
    @DisplayName("일반룸네임 채팅방 여러 명 초대")
    void inviteGeneralChatRoomMany() throws InterruptedException {
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createChatRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("test1, test2");

        String[] nicknames2 = new String[]{"test3", "test4"};
        ChatRoomRequest.invite invite = ChatRoomRequest.invite.builder()
                .invitorName("test1")
                .chatRoomId(room.getChatRoomId())
                .nicknames(nicknames2)
                .build();
        chatRoomService.inviteChatRoom(invite);
        Thread.sleep(1000);

        List<ChatRoomResponse> roomResponses = chatRoomService.userChatRoomList("test3");
        Message message = messageService.recentMessage(room.getChatRoomId());

        assertThat(message.getMessage()).isEqualTo("test1님이 test3님과 test4님을 초대하였습니다.");
        assertThat(roomResponses.size()).isEqualTo(1);
        assertThat(roomResponses.get(0).getUserCount()).isEqualTo(4);
        assertThat(roomResponses.get(0).getRoomName()).isEqualTo("test1, test2, test3, test4");
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
        chatRoomService.createChatRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("오픈채팅방");

        ChatRoomRequest.join join = ChatRoomRequest.join.builder()
                .chatRoomId(room.getChatRoomId())
                .build();

        chatRoomService.joinChatRoom("test3", join.getChatRoomId());

        List<ChatRoomResponse> roomResponses = chatRoomService.userChatRoomList("test3");

        assertThat(roomResponses.size()).isEqualTo(1);
        assertThat(roomResponses.get(0).getUserCount()).isEqualTo(3);
        assertThat(roomResponses.get(0).getRoomName()).isEqualTo("오픈채팅방");
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
        chatRoomService.createChatRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("test1, test2");

        ChatRoomRequest.leave leave = ChatRoomRequest.leave.builder()
                .chatRoomId(room.getChatRoomId())
                .loginId("test2")
                .build();

        chatRoomService.leaveChatRoom(leave);

        List<ChatRoomResponse> roomResponsesTest1 = chatRoomService.userChatRoomList("test1");
        List<ChatRoomResponse> roomResponsesTest2 = chatRoomService.userChatRoomList("test2");

        assertThat(roomResponsesTest1.get(0).getRoomName()).isEqualTo("test1");
        assertThat(roomResponsesTest1.get(0).getUserCount()).isEqualTo(1);
        assertThat(roomResponsesTest2.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("채팅방 메세지 조회")
    void selectChatRoomMessages() throws InterruptedException {
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createChatRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("test1, test2");

        for(int i=0; i<5; i++){
            Message message = Message.builder()
                    .type(MessageType.MESSAGE)
                    .roomId(room.getChatRoomId())
                    .message("안녕하세요" + i)
                    .senderName("test1")
                    .build();

            messageService.sendMessage(message);
            Thread.sleep(1000);
        }

        List<Message> messages = messageService.chatRoomMessages(room.getChatRoomId(), "test1");

        assertThat(messages.size()).isEqualTo(6);
    }

    @Test
    @DisplayName("유저 참여 후 채팅방 메세지 조회")
    void selectChatRoomAfterJoin() throws InterruptedException {
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createChatRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("test1, test2");

        for(int i=0; i<3; i++){
            Message message = Message.builder()
                    .type(MessageType.MESSAGE)
                    .roomId(room.getChatRoomId())
                    .message("안녕하세요" + i)
                    .senderName("test1")
                    .build();

            messageService.sendMessage(message);
            Thread.sleep(1000);
        }

        chatRoomService.joinChatRoom("test3", room.getChatRoomId());
        Thread.sleep(1000);

        for(int i=3; i<5; i++){
            Message message = Message.builder()
                    .type(MessageType.MESSAGE)
                    .roomId(room.getChatRoomId())
                    .message("안녕하세요" + i)
                    .senderName("test1")
                    .build();

            messageService.sendMessage(message);
            Thread.sleep(1000);
        }

        List<Message> messages = messageService.chatRoomMessages(room.getChatRoomId(), "test3");

        assertThat(messages.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("채팅방 최근 메세지 조회")
    void selectChatRoomRecentMessage() throws InterruptedException{
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("최근메세지조회 채팅방")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createChatRoom(chatRegister);

        ChatRoom room = chatRoomService.findByRoomName("최근메세지조회 채팅방");

        for(int i=0; i<5; i++){
            Message message = Message.builder()
                    .type(MessageType.MESSAGE)
                    .roomId(room.getChatRoomId())
                    .message("안녕하세요" + i)
                    .senderName("test1")
                    .build();

            messageService.sendMessage(message);
            Thread.sleep(1000);
        }

        Message message = messageService.recentMessage(room.getChatRoomId());
        assertThat(message.getMessage()).isEqualTo("안녕하세요4");
    }

    @Test
    @DisplayName("채팅방 알림 수 변경")
    void realTimeChangeNotificationCount() throws InterruptedException {
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("채팅방 알림 수 변경 채팅방")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createChatRoom(chatRegister);
        User test2 = userService.findByNickname("test2");

        ChatRoom room = chatRoomService.findByRoomName("채팅방 알림 수 변경 채팅방");
        for(int i=0; i<5; i++){
            Message message = Message.builder()
                    .type(MessageType.MESSAGE)
                    .roomId(room.getChatRoomId())
                    .message("안녕하세요" + i)
                    .senderName("test1")
                    .build();

            messageService.sendMessage(message);
            Thread.sleep(1000);
            UserChatRoom userChatRoomByChatRoomAndUser = userChatRoomRepository.findUserChatRoomByChatRoomAndUser(room, test2);
            assertThat(userChatRoomByChatRoomAndUser.getNotificationCount()).isEqualTo(i+1);
        }
    }

    @Test
    @DisplayName("채팅방 알림 수 리셋")
    void realTimeResetNotificationCount() throws InterruptedException {
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("test1")
                .roomName("채팅방 알림 수 리셋 채팅방")
                .nicknames(nicknames)
                .type("general")
                .build();
        chatRoomService.createChatRoom(chatRegister);
        User test2 = userService.findByNickname("test2");

        ChatRoom room = chatRoomService.findByRoomName("채팅방 알림 수 리셋 채팅방");
        for(int i=0; i<5; i++){
            Message message = Message.builder()
                    .type(MessageType.MESSAGE)
                    .roomId(room.getChatRoomId())
                    .message("안녕하세요" + i)
                    .senderName("test1")
                    .build();

            messageService.sendMessage(message);
            Thread.sleep(1000);
        }
        UserChatRoom userChatRoomByChatRoomAndUser = userChatRoomRepository.findUserChatRoomByChatRoomAndUser(room, test2);
        assertThat(userChatRoomByChatRoomAndUser.getNotificationCount()).isEqualTo(5);

        ChatRoomRequest.notification notification = ChatRoomRequest.notification.builder()
                .roomId(room.getChatRoomId())
                .nickname("test2")
                .build();
        messageService.resetNotification(notification);
        UserChatRoom userChatRoomByChatRoomAndUser2 = userChatRoomRepository.findUserChatRoomByChatRoomAndUser(room, test2);
        assertThat(userChatRoomByChatRoomAndUser2.getNotificationCount()).isEqualTo(0);
    }
}
