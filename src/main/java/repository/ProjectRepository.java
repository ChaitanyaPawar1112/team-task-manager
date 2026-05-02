package repository;

import org.springframework.data.jpa.repository.JpaRepository;
import entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}