

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"controller", "config", "security", "service"})
@EntityScan(basePackages = {"entity"})
@EnableJpaRepositories(basePackages = {"repository"})
public class TeamTaskManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeamTaskManagerApplication.class, args);
        System.out.println("========================================");
        System.out.println("Team Task Manager Application Started!");
        System.out.println("Access at: http://localhost:8080");
        System.out.println("========================================");
    }
}