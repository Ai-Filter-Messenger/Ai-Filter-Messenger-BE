package sisyphus_core.sisyphus_core.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sisyphus_core.sisyphus_core.chat.model.Message;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final SimpMessagingTemplate template;
    private final KafkaProducerService kafkaProducerService;
    private final RedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public void sendMessage(Message message){
        kafkaProducerService.sendMessage(message);
        template.convertAndSend("/topic/chatroom/" + message.getRoomId(), message);
    }

    public void leave(Message message) {
        kafkaProducerService.sendMessage(message);
        template.convertAndSend("/topic/chatroom/" + message.getRoomId(), message);
    }

    public void join(Message message){
        kafkaProducerService.sendMessage(message);
        template.convertAndSend("/topic/chatroom/" + message.getRoomId(), message);
    }

    public List<Message> chatRoomMessages(Long chatRoomId, String nickname){
        String key = "room:" + chatRoomId;
        List<Object> allMessagesRaw = redisTemplate.opsForList().range(key, 0, -1);
        List<Message> allMessages = new ArrayList<>();

        // LinkedHashMap을 Message로 변환
        for (Object rawMessage : allMessagesRaw) {
            // 강제로 Message 객체로 변환
            Message message = objectMapper.convertValue(rawMessage, Message.class);
            allMessages.add(message);
        }

        String joinKey = "userJoin:" + chatRoomId + ":" + nickname;
        Object userJoinEpochObj = redisTemplate.opsForValue().get(joinKey);

        Long userJoinEpoch = null;

        // Integer인지 Long인지 확인하여 변환
        if (userJoinEpochObj instanceof Integer) {
            userJoinEpoch = ((Integer) userJoinEpochObj).longValue();  // Integer 값을 Long으로 변환
        } else if (userJoinEpochObj instanceof Long) {
            userJoinEpoch = (Long) userJoinEpochObj;  // 이미 Long 타입일 경우
        }

        // 유저 참여 이후의 메시지만 필터링
        if (userJoinEpoch != null) {
            ZonedDateTime userJoinTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(userJoinEpoch), ZoneId.of("Asia/Seoul"));
            return allMessages.stream()
                    .filter(message -> message.getCreateAt().isAfter(userJoinTime))  // 참여 시간 이후의 메시지만 필터링
                    .collect(Collectors.toList());
        } else {
            // 참여 기록이 없으면 빈 리스트 반환
            return new ArrayList<>();
        }
    }

    public Message recentMessage(Long chatRoomId) {
        String key = "room:" + chatRoomId;
        Object result = redisTemplate.opsForList().index(key, 0);

        return objectMapper.convertValue(result, Message.class);
    }
}
