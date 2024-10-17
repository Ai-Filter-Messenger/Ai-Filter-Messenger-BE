package sisyphus_core.sisyphus_core.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.repository.UserRepository;
import sisyphus_core.sisyphus_core.chat.exception.ChatRoomNotFoundException;
import sisyphus_core.sisyphus_core.chat.exception.DuplicateChatRoomNameException;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomRequest;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomResponse;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomType;
import sisyphus_core.sisyphus_core.chat.repository.ChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.repository.UserChatRoomRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserChatRoomRepository userChatRoomRepository;
    private final UserRepository userRepository;

    //채팅 방 생성
    @Transactional
    public ChatRoom createRoom(ChatRoomRequest.register register){
        String loginId = register.getLoginId();
        String roomName = register.getRoomName();
        String[] nicknames = register.getNicknames();
        String type = register.getType();
        boolean customRoomName = true;

        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        if(roomName == null) {
            customRoomName = false;
            if (nicknames.length == 1) {
                roomName = user.getNickname() + ", " + nicknames[0];
            } else {
                roomName = user.getNickname() + ", " + String.join(", ", nicknames);
            }
        }

        Optional<ChatRoom> byRoomName = chatRoomRepository.findByRoomName(roomName);
        if(byRoomName.isPresent()){
            if(byRoomName.get().getType() == ChatRoomType.OPEN){
                throw new DuplicateChatRoomNameException("이미 존재하는 오픈채팅방입니다.");
            }else{
                return byRoomName.get();
            }
        }

        ChatRoomType chatRoomType = type.equals("open") ? ChatRoomType.OPEN : ChatRoomType.GENERAL;
        ChatRoom chatRoom = ChatRoom.builder()
                .roomName(roomName)
                .userCount(nicknames.length + 1)
                .type(chatRoomType)
                .customRoomName(customRoomName)
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

        return chatRoom;
    }

    //유저 채팅방 조회
    @Transactional
    public List<ChatRoomResponse> userChatRoomList(String loginId){
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        List<UserChatRoom> userChatRoomsByUser = userChatRoomRepository.findUserChatRoomsByUser(user);

        return toResponseChatRoom(userChatRoomsByUser);
    }

    //채팅방 입장 (오픈채팅)
    @Transactional
    public void joinChatRoom(String loginId, Long chatRoomId){
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));

        chatRoom.joinUser();

        UserChatRoom userChatRoom = UserChatRoom.builder()
                .chatRoom(chatRoom)
                .user(user)
                .build();

        userChatRoomRepository.save(userChatRoom);
    }

    //채팅방 초대 (일반채팅)
    @Transactional
    public void inviteChatRoom(ChatRoomRequest.invite invite){
        User user = userRepository.findByLoginId(invite.getLoginId()).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        ChatRoom chatRoom = chatRoomRepository.findById(invite.getChatRoomId()).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));

        chatRoom.joinUser();
        if(!chatRoom.isCustomRoomName()) chatRoom.setRoomName(chatRoom.getRoomName() + ", " + user.getNickname());

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
        if(!chatRoom.isCustomRoomName()) {
            String renewalChatRoomName = chatRoom.getRoomName().replace(user.getNickname(), "").trim();

            renewalChatRoomName = renewalChatRoomName.replaceAll(",\\s*,", ", ")
                    .replaceAll(",\\s*$", "")
                    .replaceAll("^\\s*,", "")
                    .trim();

            chatRoom.setRoomName(renewalChatRoomName);
        }

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
                    .userCount(chatRoom.getUserCount())
                    .build();

            roomResponses.add(chatRoomResponse);
        }

        return roomResponses;
    }

    //테스트 코드용 데이터 전체 삭제
    @Transactional
    public void deleteAll(){
        userChatRoomRepository.deleteAll();
        chatRoomRepository.deleteAll();
    }

    @Transactional
    public ChatRoom findByRoomName(String roomName){
        return chatRoomRepository.findByRoomName(roomName).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
    }
}
