package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.ChatMessage;
import com.nzby.homeshop.POJO.SupportTicket;
import com.nzby.homeshop.POJO.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySender(User user);

    List<ChatMessage> findByTicket(SupportTicket ticket);
}
