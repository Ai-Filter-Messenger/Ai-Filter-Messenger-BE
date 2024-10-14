package sisyphus_core.sisyphus_core.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRequest;
import sisyphus_core.sisyphus_core.auth.service.UserService;

@RestController
@RequiredArgsConstructor
@Tag(name = "유저 API", description = "컨트롤러에 대한 설명입니다.")
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid UserRequest.register register){
        userService.register(register);
        return ResponseEntity.ok("계정 생성에 성공하였습니다.");
    }

    @PostMapping("/check/loginId")
    public ResponseEntity<String> checkLoginId(@RequestBody @Valid String loginId){
        userService.checkDuplicateLoginId(loginId);
        return ResponseEntity.ok("사용 가능한 아이디입니다.");
    }

    @PostMapping("/check/nickname")
    public ResponseEntity<String> checkNickname(@RequestBody @Valid String nickname){
        userService.checkDuplicateNickname(nickname);
        return ResponseEntity.ok("사용 가능한 닉네임입니다.");
    }
}
