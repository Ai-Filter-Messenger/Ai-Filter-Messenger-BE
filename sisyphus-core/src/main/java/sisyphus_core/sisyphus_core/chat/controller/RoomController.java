package sisyphus_core.sisyphus_core.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.repository.UserRepository;
import sisyphus_core.sisyphus_core.chat.exception.ChatRoomNotFoundException;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.repository.ChatRoomRepository;

@Controller
@RequiredArgsConstructor
public class RoomController {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @GetMapping("/chat/{chatRoomId}/{loginId}")
    public String chatP(@PathVariable("chatRoomId") Long chatRoomId,
                        @PathVariable("loginId") String loginId,
                        Model model){
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        model.addAttribute("room", chatRoom);
        model.addAttribute("user", user);

        return "chatRoom";
    }

    @GetMapping("/chat/list")
    public String chatListP(){
        return "chatRooms";
    }

}
