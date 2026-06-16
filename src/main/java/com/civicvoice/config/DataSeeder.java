package com.civicvoice.config;

import com.civicvoice.user.domain.Role;
import com.civicvoice.user.domain.User;
import com.civicvoice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser("priya@authority.gov", "Priya Authority", Role.AUTHORITY);
        seedUser("vikram@admin.gov", "Vikram Admin", Role.ADMIN);
        seedUser("citizen@example.com", "Demo Citizen", Role.CITIZEN);
        seedUser("arjun@example.com", "Arjun Citizen", Role.CITIZEN);
    }

    private void seedUser(String email, String name, Role role) {
        if (!userRepository.existsByEmail(email)) {
            User user = User.builder()
                    .email(email)
                    .fullName(name)
                    .passwordHash(passwordEncoder.encode("password"))
                    .role(role)
                    .isActive(true)
                    .isVerified(true)
                    .build();
            userRepository.save(user);
        }
    }
}
