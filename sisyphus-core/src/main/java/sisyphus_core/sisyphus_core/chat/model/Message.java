package sisyphus_core.sisyphus_core.chat.model;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import sisyphus_core.sisyphus_core.chat.model.dto.MessageType;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "message" , timeToLive = 86400)
public class Message {

    private MessageType type;

    @Id
    private String id = UUID.randomUUID().toString();

    @Setter
    private String message;

    @Indexed
    private String senderName;

    @Indexed
    private String roomId;
    private ZonedDateTime createAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
}
