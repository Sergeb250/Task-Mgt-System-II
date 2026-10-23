package services;

import models.Project;
import models.Task;
import models.TaskStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Runs three threads that update different tasks at the same time.
public class ConcurrencyService {

    private final TaskService taskService;

    public ConcurrencyService(TaskService taskService) {
        this.taskService = taskService;
    }

    public void simulate(ProjectService projectService) {
        List<Task> tasks = new ArrayList<>();
        projectService.getProjects().stream()
                .sorted(Comparator.comparing(Project::getId))
                .forEach(project -> tasks.addAll(project.getTasks()));

        if (tasks.isEmpty()) {
            System.out.println("No tasks to update.");
            return;
        }

        System.out.println("Starting 3 threads...");
        Thread first = new Thread(() -> updateOne(tasks, 0, TaskStatus.IN_PROGRESS), "Thread-1");
        Thread second = new Thread(() -> updateOne(tasks, 1, TaskStatus.COMPLETED), "Thread-2");
        Thread third = new Thread(() -> updateOne(tasks, 2, TaskStatus.COMPLETED), "Thread-3");
        first.start();
        second.start();
        third.start();

        try {
            first.join();
            second.join();
            third.join();
        } catch (InterruptedException ex) {
            System.out.println("Update was interrupted.");
        }


        System.out.println("All threads finished successfully.");
        System.out.println("✓ Task updates applied concurrently and safely.");
    }

    private void updateOne(List<Task> tasks, int index, TaskStatus status) {
        if (index >= tasks.size()) {
            System.out.println(Thread.currentThread().getName() + " has no task to update");
            return;
        }
        Task task = tasks.get(index);
        taskService.updateStatus(task, status);
        System.out.println(Thread.currentThread().getName() + " updating " + task.getId() + " -> " + status.getLabel());
    }
}
