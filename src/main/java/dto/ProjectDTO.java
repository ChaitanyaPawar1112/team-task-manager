package dto;

import java.util.List;

public class ProjectDTO {
    private Long id;
    private String name;
    private String description;
    private String createdByName;
    private List<MemberDTO> members;
    
    public ProjectDTO() {}
    
    public ProjectDTO(Long id, String name, String description, String createdByName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdByName = createdByName;
    }
    
    public ProjectDTO(Long id, String name, String description, String createdByName, List<MemberDTO> members) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdByName = createdByName;
        this.members = members;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCreatedByName() {
        return createdByName;
    }
    
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
    
    public List<MemberDTO> getMembers() {
        return members;
    }
    
    public void setMembers(List<MemberDTO> members) {
        this.members = members;
    }
}