package service;

import entity.Task;
import entity.User;
import entity.Project;
import repository.TaskRepository;
import repository.UserRepository;
import repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    public Task createTask(String title, String description, String status, 
                          LocalDate deadline, Long assignedToId, Long projectId, String adminEmail) {
        
        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Only admins can create tasks");
        }
        
        User assignedTo = userRepository.findById(assignedToId)
            .orElseThrow(() -> new RuntimeException("Assigned user not found"));
        
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status != null ? status : "PENDING");
        task.setDeadline(deadline);
        task.setAssignedTo(assignedTo);
        task.setProject(project);
        
        return taskRepository.save(task);
    }
    
    public List<Task> getTasksByUser(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        return taskRepository.findByAssignedToId(user.getId());
    }
    
    public Task updateTaskStatus(Long taskId, String newStatus, String userEmail) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
        
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if task is assigned to this user OR user is admin
        if (!task.getAssignedTo().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("You can only update your own tasks");
        }
        
        task.setStatus(newStatus);
        return taskRepository.save(task);
    }
    
    public DashboardStats getDashboardStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Task> tasks = taskRepository.findByAssignedToId(user.getId());
        
        long total = tasks.size();
        long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long pending = tasks.stream().filter(t -> "PENDING".equals(t.getStatus())).count();
        long inProgress = tasks.stream().filter(t -> "IN_PROGRESS".equals(t.getStatus())).count();
        long overdue = tasks.stream()
            .filter(t -> !"COMPLETED".equals(t.getStatus()) && t.getDeadline().isBefore(LocalDate.now()))
            .count();
        
        return new DashboardStats(total, completed, pending, inProgress, overdue);
    }
    
    // Inner class for dashboard statistics
    public static class DashboardStats {
        private final long total;
        private final long completed;
        private final long pending;
        private final long inProgress;
        private final long overdue;
        
        public DashboardStats(long total, long completed, long pending, long inProgress, long overdue) {
            this.total = total;
            this.completed = completed;
            this.pending = pending;
            this.inProgress = inProgress;
            this.overdue = overdue;
        }
        
        // Getters
        public long getTotal() { return total; }
        public long getCompleted() { return completed; }
        public long getPending() { return pending; }
        public long getInProgress() { return inProgress; }
        public long getOverdue() { return overdue; }
    }
}