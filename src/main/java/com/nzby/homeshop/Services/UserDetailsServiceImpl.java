package com.nzby.homeshop.Services;

import com.nzby.homeshop.POJO.User;
import com.nzby.homeshop.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Ищем пользователя по email
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Ползователь не найден"));

        // Проверка, подтверждена ли почта
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Email не подтвержден.");
        }

        // Преобразуем роль пользователя в коллекцию GrantedAuthority
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()));

        // Возвращаем пользователя с его ролями и паролем
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}
