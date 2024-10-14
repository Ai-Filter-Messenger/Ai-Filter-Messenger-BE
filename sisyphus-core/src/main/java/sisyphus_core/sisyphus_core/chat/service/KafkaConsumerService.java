package sisyphus_core.sisyphus_core.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import sisyphus_core.sisyphus_core.chat.model.Message;
import sisyphus_core.sisyphus_core.chat.repository.MessageRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final MessageRepository messageRepository;

    @KafkaListener(topics = "ai-chat", groupId = "myGroup")
    public void consume(Message message) throws Exception{
        log.info("kafka receive message : {}" , message);
        messageRepository.save(message);
    }
}
