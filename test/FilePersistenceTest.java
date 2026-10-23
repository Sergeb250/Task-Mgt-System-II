import models.Project;
import models.SoftwareProject;
import models.TaskStatus;
import org.junit.jupiter.api.Test;
import services.ProjectService;
import services.TaskService;
import utils.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilePersistenceTest {

    @Test
    void saveAndLoadKeepsProjectAndTask() throws Exception {
        Path path = Files.createTempDirectory("task-manager").resolve("projects_data.json");
        ProjectService saved = new ProjectService();
        TaskService taskService = new TaskService(saved);
        SoftwareProject project = new SoftwareProject("Alpha", "Demo", 3, 10, 1000, "Tuyizere", "Java");
        saved.addProject(project);
        taskService.addTask(project, "Design", "Tuyizere", TaskStatus.COMPLETED, 2);

        FileUtils.saveProjects(path, saved);

        ProjectService loaded = new ProjectService();
        assertTrue(FileUtils.loadProjects(path, loaded));
        Project again = loaded.findById(project.getId());
        assertEquals("Alpha", again.getName());
        assertEquals("Software", again.getType());
        assertEquals("Java", ((SoftwareProject) again).getTechnology());
        assertEquals(1, again.getTaskCount());
        assertEquals("Design", again.getTasks().get(0).getName());
        assertEquals(TaskStatus.COMPLETED, again.getTasks().get(0).getStatus());
    }

    @Test
    void missingFileReturnsFalse() {
        ProjectService projects = new ProjectService();
        boolean loaded = FileUtils.loadProjects(Path.of("data", "does-not-exist.json"), projects);
        assertFalse(loaded);
        assertEquals(0, projects.getCount());
    }

    @Test
    void malformedFileReturnsFalse() throws Exception {
        Path path = Files.createTempDirectory("task-manager").resolve("bad.json");
        Files.writeString(path, "this is not json");
        ProjectService projects = new ProjectService();
        assertFalse(FileUtils.loadProjects(path, projects));
        assertEquals(0, projects.getCount());
    }

}
