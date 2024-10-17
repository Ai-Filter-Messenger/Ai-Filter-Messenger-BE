package sisyphus_core.sisyphus_core.chat.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.Message;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomRequest;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomResponse;
import sisyphus_core.sisyphus_core.chat.service.ChatRoomService;
import sisyphus_core.sisyphus_core.chat.service.MessageService;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "채팅 API", description = "컨트롤러에 대한 설명입니다.")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final MessageService messageService;

    @PostMapping("/create")
    public ResponseEntity<ChatRoom> createChatRoom(@RequestBody @Valid ChatRoomRequest.register register){
        ChatRoom room = chatRoomService.createRoom(register);
        return ResponseEntity.ok().body(room);
    }

    @PostMapping("/invite")
    public ResponseEntity<String> inviteChatRoom(@RequestBody @Valid ChatRoomRequest.invite invite){
        chatRoomService.inviteChatRoom(invite);
        return ResponseEntity.ok("방에 초대하였습니다.");
    }

    @PostMapping("/join")
    public ResponseEntity<String> joinChatRoom(Authentication auth, @RequestBody @Valid ChatRoomRequest.join join){
        chatRoomService.joinChatRoom(auth.getName(), join.getChatRoomId());
        return ResponseEntity.ok("방에 입장하였습니다.");
    }

    @PostMapping("/leave")
    public ResponseEntity<String> leaveChatRoom(@RequestBody @Valid ChatRoomRequest.leave leave){
        chatRoomService.leaveChatRoom(leave);
        return ResponseEntity.ok("방에서 퇴장하셨습니다.");
    }

    @GetMapping("/find/list")
    public ResponseEntity<List<ChatRoomResponse>> getUserChatRoomList(@RequestParam String loginId){
        List<ChatRoomResponse> roomResponses = chatRoomService.userChatRoomList(loginId);
        return ResponseEntity.ok().body(roomResponses);
    }

    @GetMapping("/find/message")
    public ResponseEntity<List<Message>> getChatRoomMessages(@RequestParam Long chatRoomId){
        return ResponseEntity.ok().body(messageService.chatRoomMessages(chatRoomId));
    }
}