package service;

import entity.Project;
import entity.User;
import repository.ProjectRepository;
import repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Project createProject(String name, String description, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Only admins can create projects");
        }
        
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setCreatedBy(admin);
        
        return projectRepository.save(project);
    }
    
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }
    
    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }
    
    public void addMemberToProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<User> members = project.getMembers();
        if (members != null && !members.contains(user)) {
            members.add(user);
            project.setMembers(members);
            projectRepository.save(project);
        } else if (members == null) {
            project.setMembers(List.of(user));
            projectRepository.save(project);
        }
    }
}