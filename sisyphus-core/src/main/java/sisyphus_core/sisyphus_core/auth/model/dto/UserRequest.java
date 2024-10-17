package sisyphus_core.sisyphus_core.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserRequest {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class register{
        private String loginId;
        private String password;
        private String nickname;
        private String name;
        private String describe;
        private String profileImageUrl;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class check{
        private String loginId;
        private String nickname;
    }
}
