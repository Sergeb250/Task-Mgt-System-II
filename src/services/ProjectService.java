package services;

import models.Project;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Stores projects in a HashMap so lookup by ID does not need a fixed array size.
public class ProjectService {

    private final Map<String, Project> projects;

    public ProjectService() {
        projects = new HashMap<>();
    }



    public void addProject(Project project) {
        projects.put(project.getId(), project);
    }

    public int getCount() {
        return projects.size();
    }

    public List<Project> getProjects() {
        return new ArrayList<>(projects.values());
    }

    public Project getAt(int index) {
        return getProjects().get(index);
    }

    public Project findById(String projectId) {
        return projects.values().stream()
                .filter(project -> project.getId().equalsIgnoreCase(projectId))
                .findFirst()
                .orElse(null);
    }

    public void printAll() {
        List<Project> sorted = sortedByName(getProjects());
        printTable(sorted);
        System.out.println("Total projects: " + projects.size());
    }

    public void printByType(String type) {
        List<Project> result = projects.values().stream()
                .filter(project -> project.getType().equals(type))
                .sorted(byName())
                .collect(java.util.stream.Collectors.toList());
        if (result.isEmpty()) {
            System.out.println("None.");
            return;
        }
        printTable(result);
    }

    public void printByBudget(double min, double max) {
        List<Project> result = projects.values().stream()
                .filter(project -> project.getBudget() >= min && project.getBudget() <= max)
                .sorted(byName())
                .collect(java.util.stream.Collectors.toList());
        if (result.isEmpty()) {
            System.out.println("No projects found.");
            return;
        }
        System.out.printf(Locale.US, "Projects with budget from $%,.2f to $%,.2f:%n", min, max);
        printTable(result);
    }

    // Shows projects whose completion is above the given percent.
    public void printAboveCompletion(double minimum) {
        List<Project> result = projects.values().stream()
                .filter(project -> project.getCompletionRate() > minimum)
                .sorted(byName())
                .collect(java.util.stream.Collectors.toList());
        if (result.isEmpty()) {
            System.out.println("None.");
            return;
        }
        result.forEach(project -> System.out.printf(Locale.US, "%s | %s | %.0f%%%n",
                project.getId(), project.getName(), project.getCompletionRate()));
    }

    private List<Project> sortedByName(List<Project> list) {
        return list.stream().sorted(byName()).collect(java.util.stream.Collectors.toList());
    }

    private Comparator<Project> byName() {
        return (
                first,
                second) ->
                first.getName()
                        .compareToIgnoreCase(second.getName());
    }

    private void printTable(List<Project> matches) {
        System.out.println("──────────────────────────────────────────────────────────────────────────────────────────────");
        System.out.printf("%-8s | %-20s | %-10s | %-9s | %s%n",
                "ID", "PROJECT NAME", "TYPE", "TEAM SIZE", "BUDGET");
        System.out.println("──────────────────────────────────────────────────────────────────────────────────────────────");
        for (Project project : matches) {
            System.out.printf(Locale.US, "%-8s | %-20s | %-10s | %-9d | $%,.2f%n",
                    project.getId(), project.getName(), project.getType(), project.getTeamSize(), project.getBudget());
            System.out.println("         | Description: " + project.getDescription());
            System.out.println("──────────────────────────────────────────────────────────────────────────────────────────────");
        }
    }
}
