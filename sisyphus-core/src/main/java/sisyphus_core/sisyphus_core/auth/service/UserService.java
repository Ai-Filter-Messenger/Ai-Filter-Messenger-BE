package sisyphus_core.sisyphus_core.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserException;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserLoginIdException;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserNicknameException;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRequest;
import sisyphus_core.sisyphus_core.auth.model.dto.UserResponse;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRole;
import sisyphus_core.sisyphus_core.auth.model.dto.UserState;
import sisyphus_core.sisyphus_core.auth.repository.UserRepository;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;
import sisyphus_core.sisyphus_core.chat.repository.UserChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.service.ChatRoomService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ChatRoomService chatRoomService;
    private final UserChatRoomRepository userChatRoomRepository;

    @Value("${default.profile.image.url}")
    private String defProfileImage;

    //유저 회원가입
    @Transactional
    public void register(UserRequest.register register){
        if(userRepository.findByNickname(register.getNickname()).isPresent()){
            throw new DuplicateUserException("이미 존재하는 아이디입니다.");
        }

        User user = User.builder()
                .loginId(register.getLoginId())
                .password(register.getPassword())
                .nickname(register.getNickname())
                .email(register.getEmail())
                .name(register.getName())
                .describe(" ")
                .profileImageUrl(defProfileImage)
                .state(UserState.ACTIVE)
                .userRole(UserRole.GENERAL)
                .build();

        userRepository.save(user);
    }

    //회원탈퇴
    @Transactional
    public void withdrawal(String loginId){
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 회원이 없습니다."));
        List<UserChatRoom> userChatRoomsByUser = userChatRoomRepository.findUserChatRoomsByUser(user);
        userChatRoomRepository.deleteAllByUser(user);
        chatRoomService.withdrawalUser(userChatRoomsByUser, user.getNickname());
        userRepository.deleteByLoginId(loginId);
    }

    //아이디찾기
    @Transactional
    public UserResponse.find findLoginId(String email){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        return new UserResponse.find(user.getLoginId(), "");
    }

    //유저 로그인 아이디 중복 검증
    @Transactional
    public void checkDuplicateLoginId(String loginId){
        if(userRepository.findByLoginId(loginId).isPresent()){
            throw new DuplicateUserLoginIdException("이미 존재하는 아이디입니다.");
        }
    }

    //유저 닉네임 중복 검증
    @Transactional
    public void checkDuplicateNickname(String nickname){
        if(userRepository.findByNickname(nickname).isPresent()){
            throw new DuplicateUserNicknameException("이미 존재하는 닉네임입니다.");
        }
    }

    @Transactional
    public List<UserResponse> findAllUser(){
        List<User> allUser = userRepository.findAll();
        return toUserResponse(allUser);
    }

    //테스트 코드용 데이터 전체 삭제
    @Transactional
    public void deleteAll(){
        userRepository.deleteAll();
    }

    @Transactional
    public User findByNickname(String nickname){
        return userRepository.findByNickname(nickname).get();
    }

    protected List<UserResponse> toUserResponse(List<User> users){
        List<UserResponse> userResponseList = new ArrayList<>();
        for (User user : users) {
            UserResponse userResponse = UserResponse.builder()
                    .loginId(user.getLoginId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .name(user.getName())
                    .profileImageUrl(user.getProfileImageUrl())
                    .state(user.getState())
                    .build();

            userResponseList.add(userResponse);
        }

        return userResponseList;
    }
}
