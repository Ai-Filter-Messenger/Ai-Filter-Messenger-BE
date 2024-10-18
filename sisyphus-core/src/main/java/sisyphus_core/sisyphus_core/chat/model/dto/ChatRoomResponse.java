package sisyphus_core.sisyphus_core.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {

    private Long chatRoomId;
    private ChatRoomType type;
    private String roomName;
    private List<String> profileImages;
    private int userCount;
    private String recentMessage;
    private boolean isCheck;
    private int NotificationCount;
}
