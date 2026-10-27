package com.social_navigator_ar.demo.service;

import com.social_navigator_ar.demo.model.Login;
import com.social_navigator_ar.demo.repository.LoginRepository;
// import org.springframework.beans.factory.annotation.Autowired; // Действительно не нужна, если конструктор один
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User; // Этот импорт используется ниже
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final LoginRepository loginRepository;

    public UserDetailsServiceImpl(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Login userLogin = loginRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь с email '" + email + "' не найден"));

        Set<GrantedAuthority> authorities = new HashSet<>();
        if (userLogin.isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            if (userLogin.isSuperAdmin()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            }
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new User(
                userLogin.getEmail(),
                userLogin.getPassword(),
                authorities);
    }
}