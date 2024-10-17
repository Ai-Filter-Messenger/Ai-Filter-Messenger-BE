package sisyphus_core.sisyphus_core.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import sisyphus_core.sisyphus_core.chat.model.Message;
import sisyphus_core.sisyphus_core.chat.service.MessageService;

@Controller
@MessageMapping("/chat")
@Slf4j
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @MessageMapping("/send")
    public void send(Message message) {
        log.info("MessageMapping /chat/send");
        messageService.sendMessage(message);
    }
}
