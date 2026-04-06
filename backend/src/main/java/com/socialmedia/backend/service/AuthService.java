package com.socialmedia.backend.service;

import com.socialmedia.backend.entity.Role;
import com.socialmedia.backend.entity.User;
import com.socialmedia.backend.repository.RoleRepository;
import com.socialmedia.backend.repository.UserRepository;
import com.socialmedia.backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public User register(String userName, String email, String rawPassword) {
        if (userName == null || userName.isBlank()) throw new RuntimeException("USER_NAME_REQUIRED");
        if (email == null || email.isBlank()) throw new RuntimeException("EMAIL_REQUIRED");
        if (rawPassword == null || rawPassword.isBlank()) throw new RuntimeException("PASSWORD_REQUIRED");

        if (userRepository.existsByUserName(userName)) throw new RuntimeException("USER_NAME_EXISTS");
        if (userRepository.existsByEmail(email)) throw new RuntimeException("EMAIL_EXISTS");

        Role userRole = roleRepository.findByRoleName("USER")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setRoleName("USER");
                    return roleRepository.save(r);
                });

        User u = new User();
        u.setUserName(userName);
        u.setEmail(email);
        u.setPassWord(passwordEncoder.encode(rawPassword));
        u.setStatus(true);
        u.setCreatedAt(LocalDateTime.now());
        u.setRole(userRole);

        return userRepository.save(u);
    }

    @Transactional(readOnly = true)
    public String login(String login, String rawPassword) {
        if (login == null || login.isBlank()) throw new RuntimeException("LOGIN_REQUIRED");
        if (rawPassword == null || rawPassword.isBlank()) throw new RuntimeException("PASSWORD_REQUIRED");

        User u = userRepository.findForLoginWithRole(login)
                .orElseThrow(() -> new RuntimeException("INVALID_CREDENTIALS"));

        if (Boolean.FALSE.equals(u.getStatus())) throw new RuntimeException("USER_DISABLED");
        if (!passwordEncoder.matches(rawPassword, u.getPassWord()))
            throw new RuntimeException("INVALID_CREDENTIALS");

        String roleName = (u.getRole() != null) ? u.getRole().getRoleName() : "USER";
        return jwtUtil.generateToken(u.getUserId(), u.getUserName(), roleName);
    }

    @Transactional
    public void changePassword(Integer userId, String currentRaw, String newRaw) {
        if (currentRaw == null || currentRaw.isBlank()) throw new RuntimeException("CURRENT_PASSWORD_REQUIRED");
        if (newRaw == null || newRaw.isBlank() || newRaw.length() < 6) throw new RuntimeException("NEW_PASSWORD_INVALID");

        User u = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        if (!passwordEncoder.matches(currentRaw, u.getPassWord())) {
            throw new RuntimeException("INVALID_CURRENT_PASSWORD");
        }

        u.setPassWord(passwordEncoder.encode(newRaw));
        userRepository.save(u);
    }

    @Transactional(readOnly = true)
    public User findByUsernameOrEmail(String login) {
        return userRepository.findForLoginWithRole(login).orElse(null);
    }

}
