package controller;

import dto.TaskDTO;
import entity.Project;
import entity.Task;
import entity.User;
import repository.ProjectRepository;
import repository.TaskRepository;
import repository.UserRepository;
import security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody TaskDTO taskDTO,
                                        @RequestHeader("Authorization") String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            
            Optional<User> adminOpt = userRepository.findByEmail(email);
            if (adminOpt.isEmpty() || !"ADMIN".equals(adminOpt.get().getRole())) {
                response.put("error", "Only admins can create tasks");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Optional<User> assignedToOpt = userRepository.findById(taskDTO.getAssignedToId());
            if (assignedToOpt.isEmpty()) {
                response.put("error", "Assigned user not found");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            Optional<Project> projectOpt = projectRepository.findById(taskDTO.getProjectId());
            if (projectOpt.isEmpty()) {
                response.put("error", "Project not found");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            Task task = new Task();
            task.setTitle(taskDTO.getTitle());
            task.setDescription(taskDTO.getDescription());
            task.setStatus(taskDTO.getStatus() != null ? taskDTO.getStatus() : "PENDING");
            task.setDeadline(taskDTO.getDeadline());
            task.setAssignedTo(assignedToOpt.get());
            task.setProject(projectOpt.get());
            
            Project project = projectOpt.get();
            project.addMember(assignedToOpt.get());
            projectRepository.save(project);
            
            Task savedTask = taskRepository.save(task);
            
            response.put("id", savedTask.getId());
            response.put("title", savedTask.getTitle());
            response.put("message", "Task created successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // GET single task by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable Long id,
                                        @RequestHeader("Authorization") String authHeader) {
        try {
            Optional<Task> taskOpt = taskRepository.findById(id);
            if (taskOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }
            
            Task task = taskOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", task.getId());
            response.put("title", task.getTitle());
            response.put("description", task.getDescription());
            response.put("status", task.getStatus());
            response.put("deadline", task.getDeadline());
            response.put("assignedToId", task.getAssignedTo() != null ? task.getAssignedTo().getId() : null);
            response.put("projectId", task.getProject() != null ? task.getProject().getId() : null);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    // UPDATE Task
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id,
                                       @RequestBody TaskDTO taskDTO,
                                       @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            Optional<User> adminOpt = userRepository.findByEmail(email);
            
            if (adminOpt.isEmpty() || !"ADMIN".equals(adminOpt.get().getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only admins can update tasks"));
            }
            
            Optional<Task> taskOpt = taskRepository.findById(id);
            if (taskOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }
            
            Task task = taskOpt.get();
            
            if (taskDTO.getTitle() != null && !taskDTO.getTitle().trim().isEmpty()) {
                task.setTitle(taskDTO.getTitle());
            }
            if (taskDTO.getDescription() != null) {
                task.setDescription(taskDTO.getDescription());
            }
            if (taskDTO.getStatus() != null) {
                task.setStatus(taskDTO.getStatus());
            }
            if (taskDTO.getDeadline() != null) {
                task.setDeadline(taskDTO.getDeadline());
            }
            if (taskDTO.getAssignedToId() != null) {
                Optional<User> assignedToOpt = userRepository.findById(taskDTO.getAssignedToId());
                if (assignedToOpt.isPresent()) {
                    task.setAssignedTo(assignedToOpt.get());
                }
            }
            if (taskDTO.getProjectId() != null) {
                Optional<Project> projectOpt = projectRepository.findById(taskDTO.getProjectId());
                if (projectOpt.isPresent()) {
                    task.setProject(projectOpt.get());
                }
            }
            
            Task updatedTask = taskRepository.save(task);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedTask.getId());
            response.put("title", updatedTask.getTitle());
            response.put("message", "Task updated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    // DELETE Task
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id,
                                       @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            Optional<User> adminOpt = userRepository.findByEmail(email);
            
            if (adminOpt.isEmpty() || !"ADMIN".equals(adminOpt.get().getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only admins can delete tasks"));
            }
            
            Optional<Task> taskOpt = taskRepository.findById(id);
            if (taskOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }
            
            taskRepository.deleteById(id);
            
            return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/all-tasks")
    public ResponseEntity<?> getAllTasks(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            User user = userOpt.get();
            List<Task> tasks;
            
            if ("ADMIN".equals(user.getRole())) {
                tasks = taskRepository.findAll();
            } else {
                tasks = taskRepository.findByAssignedToId(user.getId());
            }
            
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/my-tasks")
    public ResponseEntity<?> getMyTasks(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<Task> tasks = taskRepository.findByAssignedToId(userOpt.get().getId());
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getDashboardStats(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            User user = userOpt.get();
            List<Task> tasks;
            
            if ("ADMIN".equals(user.getRole())) {
                tasks = taskRepository.findAll();
            } else {
                tasks = taskRepository.findByAssignedToId(user.getId());
            }
            
            long total = tasks.size();
            long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
            long pending = tasks.stream().filter(t -> "PENDING".equals(t.getStatus())).count();
            long inProgress = tasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count();
            long overdue = tasks.stream()
                .filter(t -> !"COMPLETED".equals(t.getStatus()) && t.getDeadline().isBefore(LocalDate.now()))
                .count();
            
            Map<String, Long> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("completed", completed);
            stats.put("pending", pending);
            stats.put("inProgress", inProgress);
            stats.put("overdue", overdue);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(@RequestHeader("Authorization") String authHeader) {
        return getDashboardStats(authHeader);
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateTaskStatus(@PathVariable Long id, 
                                              @RequestBody Map<String, String> statusMap,
                                              @RequestHeader("Authorization") String authHeader) {
        try {
            Optional<Task> taskOpt = taskRepository.findById(id);
            if (taskOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }
            
            Task task = taskOpt.get();
            task.setStatus(statusMap.get("status"));
            Task updatedTask = taskRepository.save(task);
            
            return ResponseEntity.ok(updatedTask);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}