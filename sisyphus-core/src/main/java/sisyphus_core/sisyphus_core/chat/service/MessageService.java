package sisyphus_core.sisyphus_core.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import sisyphus_core.sisyphus_core.chat.model.Message;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final SimpMessagingTemplate template;
    private final KafkaProducerService kafkaProducerService;

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
}
