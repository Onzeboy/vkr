package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.SupportTicket;
import com.nzby.homeshop.POJO.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUser(User user);

    Arrays findByStatus(SupportTicket.TicketStatus ticketStatus);
}