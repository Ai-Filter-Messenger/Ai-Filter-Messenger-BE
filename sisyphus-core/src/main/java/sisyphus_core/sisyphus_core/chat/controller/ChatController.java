package sisyphus_core.sisyphus_core.chat.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomRequest;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomResponse;
import sisyphus_core.sisyphus_core.chat.service.ChatRoomService;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "채팅 API", description = "컨트롤러에 대한 설명입니다.")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/create")
    public ResponseEntity<String> createChatRoom(@RequestBody @Valid ChatRoomRequest.register register){
        chatRoomService.createRoom(register);
        return ResponseEntity.ok("방 생성에 성공하였습니다.");
    }

    @PostMapping("/join")
    public ResponseEntity<String> joinChatRoom(@RequestBody @Valid ChatRoomRequest.join join){
        chatRoomService.joinChatRoom(join);
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
}