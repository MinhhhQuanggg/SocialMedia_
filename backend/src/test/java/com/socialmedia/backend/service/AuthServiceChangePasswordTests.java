package com.socialmedia.backend.service;

import com.socialmedia.backend.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class AuthServiceChangePasswordTests {

    @Autowired
    private AuthService authService;

    @Test
    void changePasswordWorkflow() {
        // register via AuthService to ensure password encoding
        User reg = authService.register("pwdtest2","pwdtest2@example.com","initialPass");
        Integer id = reg.getUserId();

        // wrong current password
        Assertions.assertThrows(RuntimeException.class, () -> {
            authService.changePassword(id, "wrong", "newpass123");
        });

        // correct current password
        Assertions.assertDoesNotThrow(() -> {
            authService.changePassword(id, "initialPass", "newpass123");
        });

        // login with new password should succeed (returns token)
        String token = authService.login("pwdtest2", "newpass123");
        Assertions.assertNotNull(token);
    }
}
