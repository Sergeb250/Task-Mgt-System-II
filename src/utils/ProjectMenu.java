package utils;

import models.HardwareProject;
import models.Project;
import models.SoftwareProject;
import services.ProjectService;
import services.TaskService;
import services.UserService;
import utils.exceptions.EmptyProjectException;
import utils.exceptions.InvalidInputException;
import utils.exceptions.ProjectNotFoundException;

import java.util.Locale;
import java.util.Scanner;

// Project screens: add a project, browse the catalog, and open one project.
class ProjectMenu extends MenuScreen {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final TaskMenu taskMenu;

    ProjectMenu(Scanner scanner, ProjectService projectService, TaskService taskService,
                UserService userService, TaskMenu taskMenu) {
        super(scanner, userService);
        this.projectService = projectService;
        this.taskService = taskService;
        this.taskMenu = taskMenu;
    }

    void open() {
        while (true) {
            printBanner("MANAGE PROJECTS");
            System.out.println();
            System.out.println("1. Add Project");
            System.out.println("2. Browse Project Catalog");
            System.out.println("3. <<Back");
            int choice = readChoice("Enter your choice: ", 1, 3);
            switch (choice) {
                case 1:
                    addProject();
                    break;
                case 2:
                    browseCatalog();
                    break;
                case 3:
                    return;
                default:
                    break;
            }
        }
    }

    Project askForProject(String prompt) {
        while (true) {
            try {
                String projectId = readId(prompt, "PRJ", true);
                if (projectId == null) {
                    return null;
                }
                Project project = projectService.findById(projectId);
                if (project == null) {
                    throw new ProjectNotFoundException("Project ID '" + projectId + "' does not exist.");
                }
                return project;
            } catch (ProjectNotFoundException ex) {
                printError(ex);
                System.out.println("Please try again.");
            }
        }
    }

    private void browseCatalog() {
        while (true) {
            printBanner("PROJECT CATALOG");
            System.out.println();
            System.out.println("Filter Options:");
            System.out.println("1. View All Projects (" + projectService.getCount() + ")");
            System.out.println("2. Software Projects Only");
            System.out.println("3. Hardware Projects Only");
            System.out.println("4. Search by Budget Range");
            System.out.println("5. Completed Projects (over 70%)");
            System.out.println("6. <<Back");
            int choice = readChoice("Enter filter choice: ", 1, 6);
            if (choice == 6) {
                return;
            }
            switch (choice) {
                case 1:
                    projectService.printAll();
                    break;
                case 2:
                    projectService.printByType("Software");
                    break;
                case 3:
                    projectService.printByType("Hardware");
                    break;
                case 4:
                    if (!searchByBudget()) {
                        continue;
                    }
                    break;
                case 5:
                    projectService.printAboveCompletion(70);
                    break;
                default:
                    break;
            }

            while (true) {
                System.out.println();
                System.out.println("1. View Project Details");
                System.out.println("2. <<Back");
                int next = readChoice("Enter your choice: ", 1, 2);
                if (next == 2) {
                    break;
                }
                Project project = askForProject("Enter project ID (or 0 to go back): ");
                if (project == null) {
                    continue;
                }
                showProjectDetails(project);
            }
        }
    }

    private boolean searchByBudget() {
        System.out.println();
        System.out.println("1. Enter Budget Range");
        System.out.println("2. <<Back");
        int choice = readChoice("Enter your choice: ", 1, 2);
        if (choice == 2) {
            return false;
        }
        while (true) {
            try {
                double min = readBudget("Enter minimum budget: ");
                double max = readBudget("Enter maximum budget: ");
                if (max < min) {
                    throw new InvalidInputException(
                            "Maximum budget must be greater than or equal to minimum budget.");
                }
                projectService.printByBudget(min, max);
                return true;
            } catch (InvalidInputException ex) {
                printError(ex);
                System.out.println("Please try again.");
            }
        }
    }

    private void showProjectDetails(Project project) {
        while (true) {
            printBanner("PROJECT DETAILS: " + project.getId());
            System.out.println();
            try {
                printProject(project);
            } catch (EmptyProjectException ex) {
                printError(ex);
            }
            System.out.println();
            System.out.println("Options:");
            System.out.println("1. Add New Task");
            System.out.println("2. Update Task Status");
            System.out.println("3. Remove Task");
            System.out.println("4. <<Back");
            int choice = readChoice("Enter your choice: ", 1, 4);
            switch (choice) {
                case 1:
                    taskMenu.addToProject(project);
                    break;
                case 2:
                    taskMenu.updateOnProject(project);
                    break;
                case 3:
                    taskMenu.removeOnProject(project);
                    break;
                case 4:
                    return;
                default:
                    break;
            }
        }
    }

    private void addProject() {


        System.out.println("1. Software Project");
        System.out.println("2. Hardware Project");
        System.out.println("3. <<Back");
        int typeChoice = readChoice("Enter project type: ", 1, 3);
        if (typeChoice == 3) {
            return;
        }

        String name = readText("Enter project name: ");
        String description = readText("Enter description: ");
        int duration = readPositiveInt("Enter duration (days): ", "Duration must be greater than 0.");
        double budget = readBudget("Enter budget: ");
        int teamSize = readPositiveInt("Enter team size: ", "Team size must be greater than 0.");
        String leadName = userService.getCurrentUser().getName();

        Project project;
        if (typeChoice == 1) {
            String technology = readText("Enter technology: ");
            project = new SoftwareProject(name, description, teamSize, duration, budget, leadName, technology);
        } else {
            String equipment = readText("Enter equipment: ");
            project = new HardwareProject(name, description, teamSize, duration, budget, leadName, equipment);
        }

        projectService.addProject(project);
        System.out.println("✓ Project successfully created.");
        System.out.println("ID: " + project.getId() + " | Type: " + project.getType());
    }

    private void printProject(Project project) throws EmptyProjectException {
        System.out.println("Project Name: " + project.getName());
        System.out.println("Type: " + project.getType());
        System.out.println("Team Size: " + project.getTeamSize());
        System.out.printf(Locale.US, "Budget: $%,.2f%n", project.getBudget());
        System.out.println("Description: " + project.getDescription());
        System.out.println(project.getProjectDetails());
        System.out.println();
        System.out.println("Associated Tasks:");
        taskMenu.printTasks(project);
        if (project.getTaskCount() == 0) {
            throw new EmptyProjectException("Project '" + project.getId() + "' has no tasks.");
        }
        System.out.printf(Locale.US, "Completion Rate: %.2f%%%n", taskService.calculateCompletionPercentage(project));
    }
}
