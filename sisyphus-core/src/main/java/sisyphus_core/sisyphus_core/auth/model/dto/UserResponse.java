package sisyphus_core.sisyphus_core.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String email;
    private String nickname;
    private String name;
    private String profileImageUrl;
    private UserState state;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class find{
        private String loginId;
        private String password;
    }
}
