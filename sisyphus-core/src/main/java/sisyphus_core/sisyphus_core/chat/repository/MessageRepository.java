package sisyphus_core.sisyphus_core.chat.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import sisyphus_core.sisyphus_core.chat.model.Message;

@Repository
public interface MessageRepository extends CrudRepository<Message, String> {
}
