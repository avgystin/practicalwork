package BellSpring.repository;

import BellSpring.model.MessageEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends ReactiveCrudRepository<MessageEntity, Long> {
}