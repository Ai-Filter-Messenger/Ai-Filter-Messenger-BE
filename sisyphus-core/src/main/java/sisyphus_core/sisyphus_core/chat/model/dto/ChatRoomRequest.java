package sisyphus_core.sisyphus_core.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatRoomRequest {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class register{
        private String roomName;
        private String[] nicknames;
        private String loginId;
        private String type;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class invite{
        private Long chatRoomId;
        private String loginId;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class join{
        private Long chatRoomId;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class leave{
        private String loginId;
        private Long chatRoomId;
    }
}
