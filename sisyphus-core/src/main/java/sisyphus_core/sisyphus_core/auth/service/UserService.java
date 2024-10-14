package sisyphus_core.sisyphus_core.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserLoginIdException;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserNicknameException;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRequest;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRole;
import sisyphus_core.sisyphus_core.auth.model.dto.UserState;
import sisyphus_core.sisyphus_core.auth.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Value("${default.profile.image.url}")
    private String defProfileImage;

    @Transactional
    public void register(UserRequest.register register){
        User user = User.builder()
                .loginId(register.getLoginId())
                .password(register.getPassword())
                .nickname(register.getNickname())
                .name(register.getName())
                .describe(" ")
                .profileImageUrl(defProfileImage)
                .state(UserState.ACTIVE)
                .userRole(UserRole.GENERAL)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public void checkDuplicateLoginId(String loginId){
        if(userRepository.findByLoginId(loginId).isPresent()){
            new DuplicateUserLoginIdException("이미 존재하는 아이디입니다.");
        }
    }

    @Transactional
    public void checkDuplicateNickname(String nickname){
        if(userRepository.findByNickname(nickname).isPresent()){
            new DuplicateUserNicknameException("이미 존재하는 닉네임입니다.");
        }
    }
}
