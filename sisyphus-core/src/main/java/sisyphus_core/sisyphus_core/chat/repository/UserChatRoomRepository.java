package sisyphus_core.sisyphus_core.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;

import java.util.List;

@Repository
public interface UserChatRoomRepository extends JpaRepository<UserChatRoom, Long> {

    List<UserChatRoom> findUserChatRoomsByUser(User user);

    List<UserChatRoom> findUserChatRoomsByChatRoom(ChatRoom chatRoom);

    void deleteByChatRoom(ChatRoom chatRoom);
}
