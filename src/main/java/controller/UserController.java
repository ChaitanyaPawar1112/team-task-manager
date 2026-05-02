package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.UserRepository;
import entity.User;
import security.JwtUtil;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        try {
            // Verify admin access
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            
            List<User> users = userRepository.findAll();
            // Remove passwords from response
            users.forEach(user -> user.setPassword(null));
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}