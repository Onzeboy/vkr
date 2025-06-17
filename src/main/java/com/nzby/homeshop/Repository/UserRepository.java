package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.Enum.Role;
import com.nzby.homeshop.POJO.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    default boolean existsByEmailIgnoreCase(String email) {
        return findByEmail(email.toLowerCase()).isPresent();
    }


    @Query("SELECT u FROM User u WHERE (:email IS NULL OR LOWER(u.email) LIKE '%' || LOWER(CAST(:email AS string)) || '%') AND (:role IS NULL OR u.role = :role)")
    Page<User> findUsers(@Param("email") String email, @Param("role") Role role, Pageable pageable);
}