package com.social_navigator_ar.demo.repository;

import com.social_navigator_ar.demo.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByUserId(String userId);
    List<Message> findByParentMessageId(Long parentMessageId);
}