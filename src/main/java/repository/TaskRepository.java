package repository;

import org.springframework.data.jpa.repository.JpaRepository;
import entity.Task;
import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedToId(Long userId);
    List<Task> findByStatusAndDeadlineBefore(String status, LocalDate date);
}