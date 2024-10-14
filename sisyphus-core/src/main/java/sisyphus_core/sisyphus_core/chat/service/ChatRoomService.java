package sisyphus_core.sisyphus_core.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.repository.UserRepository;
import sisyphus_core.sisyphus_core.chat.exception.ChatRoomNotFoundException;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomRequest;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomResponse;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomType;
import sisyphus_core.sisyphus_core.chat.repository.ChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.repository.UserChatRoomRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserChatRoomRepository userChatRoomRepository;
    private final UserRepository userRepository;

    //채팅 방 생성
    @Transactional
    public void createRoom(ChatRoomRequest.register register){
        String loginId = register.getLoginId();
        String roomName = register.getRoomName();
        String[] nicknames = register.getNicknames();
        String type = register.getType();
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        if(roomName == null){
            if(nicknames.length == 1){
                roomName = nicknames[0];
            }else{
                roomName = String.join(", " , nicknames);
            }
        }

        ChatRoomType chatRoomType = type.equals("open") ? ChatRoomType.OPEN : ChatRoomType.GENERAL;
        ChatRoom chatRoom = ChatRoom.builder()
                .roomName(roomName)
                .userCount(nicknames.length + 1)
                .type(chatRoomType)
                .build();

        chatRoomRepository.save(chatRoom);
        for (String nickname : nicknames) {
            User inviteUser = userRepository.findByNickname(nickname).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
            UserChatRoom userChatRoom = UserChatRoom.builder()
                    .chatRoom(chatRoom)
                    .user(inviteUser)
                    .build();
            userChatRoomRepository.save(userChatRoom);
        }

        UserChatRoom userChatRoom = UserChatRoom.builder()
                .chatRoom(chatRoom)
                .user(user)
                .build();

        userChatRoomRepository.save(userChatRoom);
    }

    //유저 채팅방 조회
    @Transactional
    public List<ChatRoomResponse> userChatRoomList(String loginId){
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        List<UserChatRoom> userChatRoomsByUser = userChatRoomRepository.findUserChatRoomsByUser(user);

        return toResponseChatRoom(userChatRoomsByUser);
    }


    //채팅방 입장 혹은 초대 (입장은 오픈채팅, 초대는 일반채팅)
    @Transactional
    public void joinChatRoom(ChatRoomRequest.join join){
        User user = userRepository.findByLoginId(join.getLoginId()).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        ChatRoom chatRoom = chatRoomRepository.findById(join.getChatRoomId()).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));

        chatRoom.joinUser();
        chatRoom.setRoomName(chatRoom.getRoomName() + ", " + user.getNickname());

        UserChatRoom userChatRoom = UserChatRoom.builder()
                .chatRoom(chatRoom)
                .user(user)
                .build();

        userChatRoomRepository.save(userChatRoom);
    }

    //채팅방 나가기
    @Transactional
    public void leaveChatRoom(ChatRoomRequest.leave leave){
        User user = userRepository.findByLoginId(leave.getLoginId()).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        ChatRoom chatRoom = chatRoomRepository.findById(leave.getChatRoomId()).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));

        chatRoom.leaveUser();
        List<UserChatRoom> userChatRoomsByUser = userChatRoomRepository.findUserChatRoomsByUser(user);
        for (UserChatRoom userChatRoom : userChatRoomsByUser) {
            if (userChatRoom.getChatRoom().getChatRoomId().equals(leave.getChatRoomId())) {
                userChatRoomRepository.delete(userChatRoom);
                break;
            }
        }

        if(chatRoom.getUserCount() == 0) chatRoomRepository.deleteById(leave.getChatRoomId());
        else chatRoomRepository.save(chatRoom);
    }

    //ResponseChatRoom으로 변환
    @Transactional
    public List<ChatRoomResponse> toResponseChatRoom(List<UserChatRoom> userChatRoomsByUser) {
        List<ChatRoomResponse> roomResponses = new ArrayList<>();
        for (UserChatRoom userChatRoom : userChatRoomsByUser) {
            List<String> profileImageUrls = new ArrayList<>();
            ChatRoom chatRoom = userChatRoom.getChatRoom();
            List<UserChatRoom> userChatRoomsByChatRoom = userChatRoomRepository.findUserChatRoomsByChatRoom(chatRoom);

            for (UserChatRoom room : userChatRoomsByChatRoom) {
                profileImageUrls.add(room.getUser().getProfileImageUrl());
            }

            ChatRoomResponse chatRoomResponse = ChatRoomResponse.builder()
                    .chatRoomId(chatRoom.getChatRoomId())
                    .type(chatRoom.getType())
                    .roomName(chatRoom.getRoomName())
                    .profileImages(profileImageUrls)
                    .build();

            roomResponses.add(chatRoomResponse);
        }

        return roomResponses;
    }
}
