package sisyphus_core.sisyphus_core.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.repository.UserRepository;
import sisyphus_core.sisyphus_core.chat.exception.ChatRoomNotFoundException;
import sisyphus_core.sisyphus_core.chat.exception.DuplicateChatRoomNameException;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.Message;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomRequest;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomResponse;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomType;
import sisyphus_core.sisyphus_core.chat.model.dto.MessageType;
import sisyphus_core.sisyphus_core.chat.repository.ChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.repository.UserChatRoomRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final MessageService messageService;
    private final RedisTemplate redisTemplate;

    //채팅 방 생성
    @Transactional
    public ChatRoom createRoom(ChatRoomRequest.register register){
        String loginId = register.getLoginId();
        String roomName = register.getRoomName();
        String[] nicknames = register.getNicknames();
        String type = register.getType();
        boolean customRoomName = true;

        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        if(roomName == null || roomName.equals("")) {
            customRoomName = false;
            if (nicknames.length == 1) {
                roomName = user.getNickname() + ", " + nicknames[0];
            } else {
                roomName = user.getNickname() + ", " + String.join(", ", nicknames);
            }
        }

        Optional<ChatRoom> byRoomName = chatRoomRepository.findByRoomName(roomName);
        List<UserChatRoom> chatRoomList = userChatRoomRepository.findUserChatRoomsByUser(user);
        if(byRoomName.isPresent()){
            ChatRoom chatRoom = byRoomName.get();
            if(chatRoom.getType() == ChatRoomType.OPEN){
                throw new DuplicateChatRoomNameException("이미 존재하는 오픈채팅방입니다.");
            }else{
                for (UserChatRoom userChatRoom : chatRoomList) {
                    if(userChatRoom.getChatRoom().equals(chatRoom)) return chatRoom;
                }
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
        saveUserJoinTime(chatRoom.getChatRoomId(), user.getNickname());
        for (String nickname : nicknames) {
            User inviteUser = userRepository.findByNickname(nickname).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
            UserChatRoom userChatRoom = UserChatRoom.builder()
                    .chatRoom(chatRoom)
                    .user(inviteUser)
                    .build();
            userChatRoomRepository.save(userChatRoom);
            saveUserJoinTime(chatRoom.getChatRoomId(), nickname);
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
        saveUserJoinTime(chatRoom.getChatRoomId(), user.getNickname());
        chatRoom.joinUser();

        UserChatRoom userChatRoom = UserChatRoom.builder()
                .chatRoom(chatRoom)
                .user(user)
                .build();

        userChatRoomRepository.save(userChatRoom);
        Message message = Message.builder().roomId(chatRoomId).senderName(user.getNickname()).message(user.getNickname() + "님이 입장하셨습니다.").
                type(MessageType.JOIN).build();
        messageService.join(message);
    }

    //채팅방 초대 (일반채팅)
    @Transactional
    public void inviteChatRoom(ChatRoomRequest.invite invite){
        ChatRoom chatRoom = chatRoomRepository.findById(invite.getChatRoomId()).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
        for (String nickname : invite.getNicknames()) {
            User user = userRepository.findByNickname(nickname).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
            saveUserJoinTime(chatRoom.getChatRoomId(), nickname);
            chatRoom.joinUser();
            if(!chatRoom.isCustomRoomName()) chatRoom.setRoomName(chatRoom.getRoomName() + ", " + user.getNickname());

            UserChatRoom userChatRoom = UserChatRoom.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .build();

            Message message = Message.builder().roomId(chatRoom.getChatRoomId()).senderName(user.getNickname()).message(user.getNickname() + "님이 초대되었습니다.").
                    type(MessageType.INVITE).build();
            messageService.join(message);
            userChatRoomRepository.save(userChatRoom);
        }
    }

    //채팅방 초대, 참여 시 유저 참여 시간 저장
    public void saveUserJoinTime(Long chatRoomId, String nickname) {
        String joinKey = "userJoin:" + chatRoomId + ":" + nickname;
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        redisTemplate.opsForValue().set(joinKey, now.toEpochSecond());
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

        Message message = Message.builder().roomId(chatRoom.getChatRoomId()).senderName(user.getNickname()).message(user.getNickname() + "님이 퇴장하셨습니다.").
                type(MessageType.LEAVE).build();
        messageService.leave(message);
        deleteUserJoinTime(chatRoom.getChatRoomId(), user.getNickname());

        if(chatRoom.getUserCount() == 0) chatRoomRepository.deleteById(leave.getChatRoomId());
        else chatRoomRepository.save(chatRoom);
    }

    //채팅방을 나갈때 유저 참여 시간 제거
    public void deleteUserJoinTime(Long chatRoomId, String nickname) {
        String joinKey = "userJoin:" + chatRoomId + ":" + nickname;
        redisTemplate.delete(joinKey);
    }

    //ResponseChatRoom으로 변환
    @Transactional
    public List<ChatRoomResponse> toResponseChatRoom(List<UserChatRoom> userChatRoomsByUser) {
        List<ChatRoomResponse> roomResponses = new ArrayList<>();
        for (UserChatRoom userChatRoom : userChatRoomsByUser) {
            List<String> profileImageUrls = new ArrayList<>();
            ChatRoom chatRoom = userChatRoom.getChatRoom();
            List<UserChatRoom> userChatRoomsByChatRoom = userChatRoomRepository.findUserChatRoomsByChatRoom(chatRoom);

            Message message = messageService.recentMessage(chatRoom.getChatRoomId());
            String recentMessage = "";
            if(message != null){
                recentMessage = message.getMessage();
            }

            for (UserChatRoom room : userChatRoomsByChatRoom) {
                profileImageUrls.add(room.getUser().getProfileImageUrl());
            }

            ChatRoomResponse chatRoomResponse = ChatRoomResponse.builder()
                    .chatRoomId(chatRoom.getChatRoomId())
                    .type(chatRoom.getType())
                    .roomName(chatRoom.getRoomName())
                    .profileImages(profileImageUrls)
                    .userCount(chatRoom.getUserCount())
                    .recentMessage(recentMessage)
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
