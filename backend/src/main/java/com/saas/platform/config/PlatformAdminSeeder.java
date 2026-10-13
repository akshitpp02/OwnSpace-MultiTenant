package com.saas.platform.config;

import com.saas.platform.platform.PlatformAdmin;
import com.saas.platform.platform.PlatformAdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PlatformAdminSeeder {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner seedPlatformAdmin(
        PlatformAdminRepository platformAdminRepository,
        PasswordEncoder passwordEncoder,
        @Value("${PLATFORM_ADMIN_EMAIL:admin@example.com}") String adminEmail,
        @Value("${PLATFORM_ADMIN_PASSWORD:ChangeMe123!}") String adminPassword) {
        return args -> {
            if (platformAdminRepository.findByEmail(adminEmail).isPresent()) {
                return;
            }

            PlatformAdmin admin = new PlatformAdmin();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            platformAdminRepository.save(admin);
        };
    }
}
