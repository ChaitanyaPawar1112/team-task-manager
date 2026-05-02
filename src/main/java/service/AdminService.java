package service;

import entity.User;
import repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class AdminService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // Auto-create admin account if not exists
    @PostConstruct
    public void createDefaultAdmin() {
        if (!userRepository.existsByEmail("admin@example.com")) {
            User admin = new User();
            admin.setName("Admin User");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("✅ Default admin created: admin@example.com / admin123");
        }
    }
    
    public User promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole("ADMIN");
        return userRepository.save(user);
    }
    
    public User demoteToMember(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole("MEMBER");
        return userRepository.save(user);
    }
    
    public boolean isAdmin(String email) {
        return userRepository.findByEmail(email)
            .map(user -> "ADMIN".equals(user.getRole()))
            .orElse(false);
    }
}