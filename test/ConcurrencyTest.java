import models.SoftwareProject;
import models.TaskStatus;
import org.junit.jupiter.api.Test;
import services.ConcurrencyService;
import services.ProjectService;
import services.TaskService;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConcurrencyTest {

    @Test
    void threeThreadsUpdateThreeTasks() {
        ProjectService projectService = new ProjectService();
        TaskService taskService = new TaskService(projectService);
        SoftwareProject project = new SoftwareProject("Alpha", "Demo", 3, 10, 1000, "Tuyizere", "Java");
        projectService.addProject(project);
        taskService.addTask(project, "Design", "", TaskStatus.PENDING, 1);
        taskService.addTask(project, "Build", "", TaskStatus.PENDING, 1);
        taskService.addTask(project, "Test", "", TaskStatus.PENDING, 1);

        new ConcurrencyService(taskService).simulate(projectService);

        assertEquals(TaskStatus.IN_PROGRESS, project.getTasks().get(0).getStatus());
        assertEquals(TaskStatus.COMPLETED, project.getTasks().get(1).getStatus());
        assertEquals(TaskStatus.COMPLETED, project.getTasks().get(2).getStatus());
    }

    @Test
    void fewerThanThreeTasksUpdatesOnlyExistingOnes() {
        ProjectService projectService = new ProjectService();
        TaskService taskService = new TaskService(projectService);
        SoftwareProject project = new SoftwareProject("Alpha", "Demo", 3, 10, 1000, "Tuyizere", "Java");
        projectService.addProject(project);
        taskService.addTask(project, "Design", "", TaskStatus.PENDING, 1);

        new ConcurrencyService(taskService).simulate(projectService);

        assertEquals(1, project.getTasks().size());
        assertEquals(TaskStatus.IN_PROGRESS, project.getTasks().get(0).getStatus());
    }
}
