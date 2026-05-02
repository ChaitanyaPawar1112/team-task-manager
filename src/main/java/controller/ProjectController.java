package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import repository.ProjectRepository;
import repository.UserRepository;
import entity.Project;
import entity.User;
import security.JwtUtil;
import dto.ProjectDTO;
import dto.MemberDTO;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        try {
            List<Project> projects = projectRepository.findAll();
            System.out.println("Fetching projects, found: " + projects.size());
            
            List<ProjectDTO> projectDTOs = projects.stream()
                .map(project -> {
                    String createdByName = "Unknown";
                    if (project.getCreatedBy() != null) {
                        createdByName = project.getCreatedBy().getName();
                    }
                    
                    List<MemberDTO> memberDTOs = new ArrayList<>();
                    if (project.getMembers() != null) {
                        memberDTOs = project.getMembers().stream()
                            .map(member -> new MemberDTO(
                                member.getId(),
                                member.getName(),
                                member.getEmail(),
                                member.getRole()
                            ))
                            .collect(Collectors.toList());
                    }
                    
                    return new ProjectDTO(
                        project.getId(),
                        project.getName(),
                        project.getDescription(),
                        createdByName,
                        memberDTOs
                    );
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(projectDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getProjectById(@PathVariable Long id) {
        try {
            Optional<Project> projectOpt = projectRepository.findById(id);
            if (projectOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
            }
            
            Project project = projectOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", project.getId());
            response.put("name", project.getName());
            response.put("description", project.getDescription());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProject(@RequestBody Project project, 
                                           @RequestHeader("Authorization") String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Creating project: " + project.getName());
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("error", "Authorization header missing");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            
            Optional<User> adminOpt = userRepository.findByEmail(email);
            
            if (!adminOpt.isPresent()) {
                response.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            User admin = adminOpt.get();
            
            if (!"ADMIN".equals(admin.getRole())) {
                response.put("error", "Only admins can create projects");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            if (project.getName() == null || project.getName().trim().isEmpty()) {
                response.put("error", "Project name is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            project.setCreatedBy(admin);
            project.addMember(admin);
            Project savedProject = projectRepository.save(project);
            
            response.put("id", savedProject.getId());
            response.put("name", savedProject.getName());
            response.put("description", savedProject.getDescription());
            response.put("message", "Project created successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // UPDATE Project
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id,
                                          @RequestBody Project projectUpdate,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            // Verify admin access
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            Optional<User> adminOpt = userRepository.findByEmail(email);
            
            if (adminOpt.isEmpty() || !"ADMIN".equals(adminOpt.get().getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only admins can update projects"));
            }
            
            Optional<Project> projectOpt = projectRepository.findById(id);
            if (projectOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
            }
            
            Project project = projectOpt.get();
            
            if (projectUpdate.getName() != null && !projectUpdate.getName().trim().isEmpty()) {
                project.setName(projectUpdate.getName());
            }
            if (projectUpdate.getDescription() != null) {
                project.setDescription(projectUpdate.getDescription());
            }
            
            Project savedProject = projectRepository.save(project);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedProject.getId());
            response.put("name", savedProject.getName());
            response.put("description", savedProject.getDescription());
            response.put("message", "Project updated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    // DELETE Project
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            // Verify admin access
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            Optional<User> adminOpt = userRepository.findByEmail(email);
            
            if (adminOpt.isEmpty() || !"ADMIN".equals(adminOpt.get().getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only admins can delete projects"));
            }
            
            Optional<Project> projectOpt = projectRepository.findById(id);
            if (projectOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
            }
            
            projectRepository.deleteById(id);
            
            return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    // Add member to project
    @PostMapping("/{projectId}/members/{userId}")
    public ResponseEntity<?> addMemberToProject(@PathVariable Long projectId,
                                                @PathVariable Long userId,
                                                @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            Optional<User> adminOpt = userRepository.findByEmail(email);
            
            if (adminOpt.isEmpty() || !"ADMIN".equals(adminOpt.get().getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only admins can add members"));
            }
            
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            Optional<User> userOpt = userRepository.findById(userId);
            
            if (projectOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
            }
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }
            
            Project project = projectOpt.get();
            User user = userOpt.get();
            
            project.addMember(user);
            projectRepository.save(project);
            
            return ResponseEntity.ok(Map.of("message", "Member added successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    // Remove member from project
    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<?> removeMemberFromProject(@PathVariable Long projectId,
                                                     @PathVariable Long userId,
                                                     @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            Optional<User> adminOpt = userRepository.findByEmail(email);
            
            if (adminOpt.isEmpty() || !"ADMIN".equals(adminOpt.get().getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only admins can remove members"));
            }
            
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            Optional<User> userOpt = userRepository.findById(userId);
            
            if (projectOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
            }
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
            }
            
            Project project = projectOpt.get();
            User user = userOpt.get();
            
            project.removeMember(user);
            projectRepository.save(project);
            
            return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    // Get project members
    @GetMapping("/{projectId}/members")
    public ResponseEntity<?> getProjectMembers(@PathVariable Long projectId,
                                               @RequestHeader("Authorization") String authHeader) {
        try {
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            
            if (projectOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
            }
            
            Project project = projectOpt.get();
            List<MemberDTO> members = project.getMembers().stream()
                .map(member -> new MemberDTO(
                    member.getId(),
                    member.getName(),
                    member.getEmail(),
                    member.getRole()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(members);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    // Get all available users
    @GetMapping("/available-users")
    public ResponseEntity<?> getAvailableUsers(@RequestHeader("Authorization") String authHeader) {
        try {
            List<User> allUsers = userRepository.findAll();
            List<MemberDTO> availableUsers = allUsers.stream()
                .map(user -> new MemberDTO(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(availableUsers);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}