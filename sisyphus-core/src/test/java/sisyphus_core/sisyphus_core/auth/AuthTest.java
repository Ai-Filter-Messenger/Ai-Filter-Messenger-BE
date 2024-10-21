package sisyphus_core.sisyphus_core.auth;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserLoginIdException;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserNicknameException;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRequest;
import sisyphus_core.sisyphus_core.auth.model.dto.UserResponse;
import sisyphus_core.sisyphus_core.auth.service.UserService;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomRequest;
import sisyphus_core.sisyphus_core.chat.service.ChatRoomService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Slf4j
public class AuthTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ChatRoomService chatRoomService;

    @BeforeEach
    void before(){
        UserRequest.register register1= UserRequest.register.builder()
                .loginId("테스트1")
                .password("1234")
                .nickname("test1")
                .email("test1@test.com")
                .name("test1")
                .build();

        UserRequest.register register2= UserRequest.register.builder()
                .loginId("테스트2")
                .password("1234")
                .nickname("test2")
                .email("test2@test.com")
                .name("test2")
                .build();

        userService.register(register1);
        userService.register(register2);
    }

    @AfterEach
    void after(){
        userService.deleteAll();
    }

    @Test
    @DisplayName("유저 회원가입")
    void userRegister(){
        UserRequest.register register2= UserRequest.register.builder()
                .loginId("테스트3")
                .password("1234")
                .nickname("test3")
                .email("test3@test.com")
                .name("test3")
                .build();

        userService.register(register2);

        User test3 = userService.findByNickname("test3");
        List<UserResponse> allUser = userService.findAllUser();
        assertThat(test3.getLoginId()).isEqualTo("테스트3");
        assertThat(allUser.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("중복 로그인 아이디 체크")
    void userRegisterDuplicateLoginId(){
        assertThatThrownBy(() -> userService.checkDuplicateLoginId("테스트1"))
                .isInstanceOf(DuplicateUserLoginIdException.class)
                .hasMessage("이미 존재하는 아이디입니다.");
    }

    @Test
    @DisplayName("중복 닉네임 체크")
    void userRegisterDuplicateNickname(){
        assertThatThrownBy(() -> userService.checkDuplicateNickname("test2"))
                .isInstanceOf(DuplicateUserNicknameException.class)
                .hasMessage("이미 존재하는 닉네임입니다.");
    }

    @Test
    @DisplayName("회원 탈퇴")
    void userWithdrawal(){
        userService.withdrawal("테스트1");

        List<UserResponse> allUser = userService.findAllUser();
        assertThat(allUser.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("회원 탈퇴 채팅방 정보 번경")
    void updateChatRoomByUserWithdrawal(){
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .loginId("테스트1")
                .nicknames(nicknames)
                .type("open")
                .build();

        chatRoomService.createRoom(chatRegister);

        userService.withdrawal("테스트1");
        ChatRoom room = chatRoomService.findByRoomName("test2");
        assertThat(room.getUserCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("회원 아이디 찾기")
    void findUserLoginId(){
        UserResponse.find find = userService.findLoginId("test1@test.com");

        assertThat(find.getLoginId()).isEqualTo("테스트1");
    }

    @Test
    @DisplayName("회원 비밀번호 찾기")
    void findUserPassword(){
        UserResponse.find find = userService.findPassword("테스트2");
        assertThat(find.getPassword()).isEqualTo("1234");
    }
}
