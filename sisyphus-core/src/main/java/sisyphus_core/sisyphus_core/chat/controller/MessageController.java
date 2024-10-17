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

    @MessageMapping("/join")
    public void join(Message message) {
      log.info("MessageMapping /chat/join");
      message.setMessage(message.getSenderName() + "님이 입장하셨습니다.");
      messageService.join(message);
    }

    @MessageMapping("/leave")
    public void leave(Message message) {
        log.info("MessageMapping /chat/leave");
        message.setMessage(message.getSenderName() + "님이 퇴장하셨습니다.");
        messageService.leave(message);
    }

    @MessageMapping("/send")
    public void send(Message message) {
        log.info("MessageMapping /chat/send");
        messageService.sendMessage(message);
    }
}
