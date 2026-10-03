package es.bytescolab.msauth.seeder;

import es.bytescolab.msauth.entity.User;
import es.bytescolab.msauth.enums.UserRole;
import es.bytescolab.msauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@RequiredArgsConstructor
@Slf4j
public class AdminUserSeeder {

    private static final String ADMIN_EMAIL = "admin@fleetcontrol.com";
    private static final String ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${demo.admin-password:}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (userRepository.count() > 0) {
            log.info("Seeder omite la creación del usuario admin: la tabla users no está vacía");
            return;
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("demo.admin-password no está definida: no se crea el usuario admin");
            return;
        }

        User admin = User.builder()
                .username(ADMIN_USERNAME)
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(adminPassword))
                .role(UserRole.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Usuario admin creado: {}", ADMIN_EMAIL);
    }
}
