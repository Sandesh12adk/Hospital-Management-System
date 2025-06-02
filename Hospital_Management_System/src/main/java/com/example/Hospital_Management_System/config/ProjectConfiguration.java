package com.example.Hospital_Management_System.config;

import com.example.Hospital_Management_System.constant.USER_ROLE;
import com.example.Hospital_Management_System.model.User;
import com.example.Hospital_Management_System.repo.UserRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.List;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
@Configuration
public class ProjectConfiguration {

    @Bean
    public CommandLineRunner createFirstAdmin(UserRepo userRepo) {
        return args -> {
            List<User> adminUsers = userRepo.findAll().stream()
                    .filter(user -> user.getRole() == USER_ROLE.ADMIN)
                    .toList();

            // No admin present — create default admin
            if (adminUsers.isEmpty()) {
                User admin = new User();
                admin.setName("admin");
                admin.setEmail("admin@example.com");
                admin.setPassword(new BCryptPasswordEncoder(5).encode("admin123"));
                admin.setRole(USER_ROLE.ADMIN);
                userRepo.save(admin);
                System.out.println("✔ Default admin created.");
            }
            // More than one admin exists — delete default admin if present
            else if (adminUsers.size() > 1) {
                adminUsers.stream()
                        .filter(user -> "admin@example.com".equalsIgnoreCase(user.getEmail()))
                        .findFirst()
                        .ifPresent(user -> {
                            userRepo.delete(user);
                            System.out.println("⚠ Default admin removed (other admins present).");
                        });
            }
        };
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow all origins (for testing). For production, specify exact origins like "https://yourdomain.com"
        configuration.setAllowedOrigins(List.of("*"));

        // Allowed HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allowed headers (add more if you use custom headers)
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));

        // Allow credentials (cookies, authorization headers, etc.)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this config for all endpoints
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
