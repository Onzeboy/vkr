package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    default boolean existsByEmailIgnoreCase(String email) {
        return findByEmail(email.toLowerCase()).isPresent();
    }
    // Можно добавить другие методы по необходимости, например:
    // List<User> findByRole(Role role);
    // List<User> findByCreatedAtAfter(LocalDateTime date);
}