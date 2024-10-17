package sisyphus_core.sisyphus_core.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sisyphus_core.sisyphus_core.chat.model.Message;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final SimpMessagingTemplate template;
    private final KafkaProducerService kafkaProducerService;
    private final RedisTemplate redisTemplate;

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

    public List<Message> chatRoomMessages(Long chatRoomId){
        String key = "room:" + chatRoomId;
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public Message recentMessage(Long chatRoomId){
        String key = "room:" + chatRoomId;
        return (Message) redisTemplate.opsForList().index(key, 0);
    }
}
