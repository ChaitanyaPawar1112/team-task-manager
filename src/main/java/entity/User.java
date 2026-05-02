package entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    @JsonIgnore
    private String password;
    
    @Column(nullable = false)
    private String role;
    
    @OneToMany(mappedBy = "createdBy")
    @JsonIgnore
    private List<Project> createdProjects;
    
    @OneToMany(mappedBy = "assignedTo")
    @JsonIgnore
    private List<Task> assignedTasks;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public List<Project> getCreatedProjects() {
        return createdProjects;
    }
    
    public void setCreatedProjects(List<Project> createdProjects) {
        this.createdProjects = createdProjects;
    }
    
    public List<Task> getAssignedTasks() {
        return assignedTasks;
    }
    
    public void setAssignedTasks(List<Task> assignedTasks) {
        this.assignedTasks = assignedTasks;
    }
}