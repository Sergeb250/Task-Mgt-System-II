# Task Management System

## Project Overview

**Lab 3 – Java Advanced**

**Technology Stack:** Java 21 (LTS), IntelliJ IDEA Community Edition, Maven, JUnit 5, Mockito

This lab keeps the Project/Task Management System from Lab 1 and Lab 2 and adds collections, functional code, file persistence, regular expressions, and concurrency.

Lab 2 still stands: custom exceptions, one job per class, and JUnit tests. Lab 3 replaces the fixed arrays with collections and makes the catalog survive a restart.

What this version does:

- Stores projects in a `HashMap` and each project's tasks in an `ArrayList`
- Filters, maps, and reduces with streams, lambdas, and method references
- Uses `TaskFilter`, a functional interface, to count tasks by status
- Checks `PRJ###`, `TSK###`, and email text with `Pattern` and `Matcher`
- Loads `data/projects_data.json` on startup and saves it on exit with NIO and try-with-resources
- Runs three threads from main menu option 5; `TaskService.updateStatus` is `synchronized`
- Covers those paths with 14 JUnit tests

## Project Objectives

By completing this lab, you will be able to:

- Replace fixed arrays with `ArrayList` and `HashMap`.
- Use streams (`filter`, `map`, `reduce`), lambdas, and method references.
- Pass behavior with a functional interface (`TaskFilter`).
- Validate ids and emails with regular expressions.
- Save and load the catalog with `java.nio.file` and try-with-resources.
- Update different tasks from three threads and protect the write with `synchronized`.
- Prove each of those behaviors with JUnit 5, including a missing file, a broken file, and a run with fewer than three tasks.
- Keep the history on three feature branches merged into `main`.

## System Features Overview

### Feature 1: Collections and functional code

- `ProjectService` stores projects in `Map<String, Project>`. Lookup by id does not use a fixed slot count.
- `Project` stores tasks in `List<Task>`. Adding a task is `tasks.add(task)`. There is no 20-task cap.
- `StreamService` filters completed tasks, maps tasks to names, and reduces completion percentages.
- `TaskService.countByStatus` builds a `TaskFilter` lambda and passes `filter::test` to a stream.
- `Project.getCompletionRate()` counts completed tasks with a stream. One completed task out of two is 50.

### Feature 2: File persistence

- `Main` calls `FileUtils.loadProjects` before the menu and `FileUtils.saveProjects` in a `finally` block on the way out.
- Save sorts projects by id and writes JSON-like text. Software technology and hardware equipment both go in `"extra"`.
- The writer and the line stream are opened with try-with-resources, so the file is closed when the block ends.
- A missing file prints `File not found` and the app starts with an empty catalog. A broken file prints `malformed data` and does not keep a half-loaded list.

### Feature 3: Regex validation

- `RegexValidator` compiles `PRJ\\d{3}`, `TSK\\d{3}`, and an email pattern once.
- `PRJ001` and `TSK004` match. `TX01` and `T001` do not.
- `ValidationUtils` also accepts a short number such as `1` and turns it into `PRJ001`. After that, the pattern must still match, or the menu asks again.

### Feature 4: Concurrency

- Main menu option **5. Simulate Concurrent Updates** calls `ConcurrencyService.simulate`.
- Projects are sorted by id, then their tasks are placed in one list.
- Thread-1 sets task 0 to In Progress. Thread-2 sets task 1 to Completed. Thread-3 sets task 2 to Completed.
- `join()` waits until all three threads finish. If a thread's index is past the end of the list, it prints that it has no task and returns.
- `updateStatus`, `addTask`, and `removeTaskById` are `synchronized`, so only one thread at a time is inside those writes.

### Feature 5: Tests and Git

- `test/ProjectTests.java` — create, add task, 50 percent, missing task, admin and regular user
- `test/StreamOperationsTest.java` — filter, map, reduce, and id patterns
- `test/FilePersistenceTest.java` — save and load, a missing file, and a malformed file
- `test/ConcurrencyTest.java` — three tasks updated together, and one task when the other threads have nothing to update
- Lab 3 history is `feature/collections-fp`, `feature/file-io`, and `feature/concurrency`, each merged into `main`

## Console UI Examples

Below are sample enhanced console outputs showing error handling and testing feedback.

### 1. Main Menu

```
╔════════════════════════════════════════════╗
║   JAVA PROJECT MANAGEMENT SYSTEM v3.0      ║
╚════════════════════════════════════════════╝

Current User:  Alice Johnson (Admin)

Main Menu:
-----------
1. Manage Projects
2. Manage Tasks
3. View Status Reports
4. Switch User
5. Simulate Concurrent Updates
6. Exit

Enter your choice: _
```

### 2. Invalid Input Handled via Exception

```
Enter project ID: PRJ999
❌ Error: ProjectNotFoundException – Project ID 'PRJ999' does not exist.
Please try again.

Enter project ID: PRJ001
✓ Project 'Alpha Tracker' loaded successfully.
```

### 3. Add Task – Validation

```
╔════════════════════════════════════════════╗
║               ADD NEW TASK                 ║
╚════════════════════════════════════════════╝

Enter task name: Implement API
Enter status (Pending/In Progress/Completed): In Progress

✓ Task 'Implement API' successfully added to Project PRJ001.
```

### 4. Exception During Task Update

```
Enter task ID: TSK999
❌ Error: TaskNotFoundException – Task 'TSK999' was not found in the project.

Operation aborted. Returning to task menu...
```

### 5. Status Report Display

```
╔════════════════════════════════════════════╗
║            PROJECT STATUS REPORT           ║
╚════════════════════════════════════════════╝

PROJECT ID | PROJECT NAME      | TASKS | COMPLETED | PROGRESS (%)
-----------------------------------------------------------------
PRJ001     | Alpha Tracker     |   4   |    3      |   75%
PRJ002     | IoT Sensor Kit    |   2   |    1      |   50%
-----------------------------------------------------------------
AVERAGE COMPLETION: 62.5%

✓ Report generated successfully.
```

### 6. Git Commit Workflow (Example Output)

```bash
$ git checkout -b feature/collections-fp
$ git commit -m "Replace fixed arrays with lists and a map"
$ git checkout main
$ git merge feature/collections-fp
$ git checkout -b feature/file-io
$ git checkout -b feature/concurrency
```

### 7. JUnit Test Example

```java
@Test
void threeThreadsUpdateThreeTasks() {
    taskService.addTask(project, "Design", "", TaskStatus.PENDING, 1);
    taskService.addTask(project, "Build", "", TaskStatus.PENDING, 1);
    taskService.addTask(project, "Test", "", TaskStatus.PENDING, 1);

    new ConcurrencyService(taskService).simulate(projectService);

    assertEquals(TaskStatus.IN_PROGRESS, project.getTasks().get(0).getStatus());
    assertEquals(TaskStatus.COMPLETED, project.getTasks().get(1).getStatus());
    assertEquals(TaskStatus.COMPLETED, project.getTasks().get(2).getStatus());
}
```

## Expected User Workflows

### Workflow 1: Grow the catalog with collections

User adds projects and tasks past the old limit of 20. The `HashMap` and each `ArrayList` grow. Browse and status reports still list every project.

### Workflow 2: Restart and keep the data

User exits the app. `Main` writes `data/projects_data.json`. The next run prints how many projects were loaded. If that file is missing or broken, the menu opens on an empty catalog.

### Workflow 3: Reject a bad id

User types `TX01` at a project or task id prompt. `RegexValidator` rejects it, the menu prints the `PRJ###` or `TSK###` pattern, and the prompt repeats.

### Workflow 4: Update tasks on three threads

User chooses **5. Simulate Concurrent Updates**. Three threads change the first three tasks, then the menu prints that the updates finished. With only one task, the first becomes In Progress and the other two threads report that they have no task.

## User Stories

### Epic 1: Collections and streams

**US-1.1:** Store a growing catalog — **As a user** I want to add projects and tasks without hitting a fixed array size.

**Acceptance Criteria:**

- Projects are stored in a `HashMap` keyed by id.
- Tasks are stored in an `ArrayList` on the project.
- Completion and catalog filters use streams.

**Technical Requirements:**

- `ProjectService`, `Project`, `StreamService`, and `TaskFilter`.
- `filter`, `map`, and `reduce` in `StreamService`.
- A lambda assigned to `TaskFilter` in `countByStatus`.

### Epic 2: File persistence

**US-2.1:** Keep the catalog after exit — **As a user** I want my projects to still be there the next time I start the app.

**Acceptance Criteria:**

- Startup loads `data/projects_data.json`.
- Exit saves that file, including after an early close.
- A missing or broken file does not crash the menu.

**Technical Requirements:**

- `FileUtils.saveProjects` and `FileUtils.loadProjects`.
- `Files.newBufferedWriter` and `Files.lines` inside try-with-resources.
- `Main` saves inside `finally`.

### Epic 3: Regex validation

**US-3.1:** Accept only well-formed ids — **As a system** I want project ids, task ids, and emails to match a pattern.

**Acceptance Criteria:**

- `PRJ001` and `TSK004` are accepted.
- `TX01` and `T001` are rejected with `InvalidInputException`.
- The menu stays open and asks again.

**Technical Requirements:**

- `Pattern.compile` in `RegexValidator`.
- `ValidationUtils.readPrefixedId` and `readEmail`.

### Epic 4: Concurrency

**US-4.1:** Update tasks at the same time — **As a user** I want a demo that changes three tasks from three threads without losing a status write.

**Acceptance Criteria:**

- Three threads start and `join()` waits for all of them.
- Statuses end as In Progress, Completed, Completed when three tasks exist.
- A thread with no task at its index does not add a task.

**Technical Requirements:**

- `new Thread(() -> updateOne(...), "Thread-1")` and the same for threads 2 and 3.
- `synchronized` on `TaskService.updateStatus`.

### Epic 5: Tests and version control

**US-5.1:** Prove Lab 3 with tests and branches — **As a developer** I want each new behavior covered by a test and a merged branch.

**Acceptance Criteria:**

- `mvn test` runs 14 tests with 0 failures.
- `feature/collections-fp`, `feature/file-io`, and `feature/concurrency` are merged into `main`.

**Technical Requirements:**

- JUnit 5 under `test/`.
- Mockito only where a test fakes `ProjectService` (`missingTaskThrows`).

## Class diagram

The diagram shows inheritance and the links between classes.

![Class diagram](docs/class-diagram.png)

How to read it:

- A white triangle means **extends**. `SoftwareProject` and `HardwareProject` extend `Project`. `AdminUser` and `RegularUser` extend `User`.
- A dashed line means **implements**. `Project` and `Task` both implement `Completable`.
- A filled diamond means **has**. One project has many tasks in an `ArrayList`. Each task has one status. One status report summarizes one project.

A longer note is in [docs/designDecisions.md](docs/designDecisions.md).

## Project Structure

```
project-management-system/
│
├── src/
│   ├── Main.java
│   ├── models/
│   │   ├── Project.java
│   │   ├── SoftwareProject.java
│   │   ├── HardwareProject.java
│   │   ├── Task.java
│   │   ├── TaskStatus.java
│   │   ├── User.java
│   │   ├── RegularUser.java
│   │   ├── AdminUser.java
│   │   └── StatusReport.java
│   │
│   ├── interfaces/
│   │   ├── Completable.java
│   │   └── TaskFilter.java
│   │
│   ├── services/
│   │   ├── ProjectService.java
│   │   ├── TaskService.java
│   │   ├── ReportService.java
│   │   ├── UserService.java
│   │   ├── StreamService.java
│   │   └── ConcurrencyService.java
│   │
│   ├── utils/
│   │   ├── ConsoleMenu.java
│   │   ├── ProjectMenu.java
│   │   ├── TaskMenu.java
│   │   ├── UserMenu.java
│   │   ├── ReportMenu.java
│   │   ├── MenuScreen.java
│   │   ├── ValidationUtils.java
│   │   ├── RegexValidator.java
│   │   ├── FileUtils.java
│   │   ├── SampleDataLoader.java
│   │   └── exceptions/
│   │       ├── InvalidInputException.java
│   │       ├── ProjectNotFoundException.java
│   │       ├── TaskNotFoundException.java
│   │       └── EmptyProjectException.java
│
├── test/
│   ├── ProjectTests.java
│   ├── StreamOperationsTest.java
│   ├── FilePersistenceTest.java
│   └── ConcurrencyTest.java
│
├── docs/
│   ├── class-diagram.png
│   ├── designDecisions.md
│   ├── class-logic.md
│   ├── rubric-guide.md
│   └── testing.md
│
├── pom.xml
└── README.md
```

## Implementation Phases

### Phase 1: Collections and functional code

**Branch:** `feature/collections-fp`

**Tasks:**

- Move projects to a `HashMap` and tasks to an `ArrayList`.
- Add `StreamService`, `TaskFilter`, and regex checks.
- Split the menu so each screen owns one area.

**Deliverables:** The catalog grows without a fixed size. Streams filter and sort it.

### Phase 2: File persistence

**Branch:** `feature/file-io`

**Tasks:**

- Save and load `data/projects_data.json` with NIO.
- Close the writer and the line stream with try-with-resources.
- Load on startup and save in `Main`'s `finally` block.

**Deliverables:** Projects and tasks are still there after a restart. A missing or broken file starts an empty catalog.

### Phase 3: Concurrency

**Branch:** `feature/concurrency`

**Tasks:**

- Start three threads that update three different tasks.
- Wait with `join()`.
- Mark the status write `synchronized`.

**Deliverables:** Menu option 5 finishes with In Progress, Completed, Completed when three tasks exist.

### Phase 4: Tests

**Tasks:**

- Add `StreamOperationsTest`, `FilePersistenceTest`, and `ConcurrencyTest`.
- Keep the Lab 2 tests in `ProjectTests`.

**Deliverables:** `mvn test` reports 14 tests, 0 failures.

## Minimum Requirements Checklist

- JDK 21, Maven, and IntelliJ IDEA
- Projects in a `HashMap`, tasks in an `ArrayList`
- `filter`, `map`, and `reduce` in `StreamService`
- `TaskFilter` used by a lambda in `countByStatus`
- `RegexValidator` for `PRJ###`, `TSK###`, and email
- `data/projects_data.json` loaded on startup and saved on exit
- Try-with-resources around the writer and the line stream
- Three threads, `join()`, and `synchronized` status updates
- 14 JUnit tests passing, including missing file, malformed file, and one-task concurrency
- `feature/collections-fp`, `feature/file-io`, and `feature/concurrency` merged into `main`
- Lab 1 and Lab 2 behavior still works (menu, exceptions, completion)
- README describes the Lab 3 behavior

## Grading Rubric

| **Criteria** | **Points** | **Where it lives in this project** |
| --- | --- | --- |
| Collections and functional code | 18 | `Project`, `ProjectService`, `StreamService`, `TaskFilter`, `getCompletionRate` |
| File persistence | 18 | `FileUtils`, `Main` load/save, `test/FilePersistenceTest.java` |
| Regex validation | 12 | `RegexValidator`, `ValidationUtils.readPrefixedId` |
| Concurrency | 17 | `ConcurrencyService`, `synchronized` writes, menu option 5, `test/ConcurrencyTest.java` |
| Testing and Git | 13 | Four test classes, 14 tests, three Lab 3 branches merged to `main` |
| Code quality and docs | 10 | One menu class per area, `docs/class-logic.md`, this README |
| Data structures and algorithms | 10 | `HashMap` lookup, `ArrayList` add/remove, stream sort, one-pass completion |
| **Total** | **98** | Point split from `docs/rubric-guide.md` |

## Submission Requirements

### Required Deliverables:

- Public GitHub Repository containing:
  - All source code (/src, /test)
  - README.md with setup, run, and Lab 3 feature notes
  - `docs/class-logic.md`, `docs/rubric-guide.md`, and `docs/testing.md`
  - JUnit test results screenshot (optional)
- **Documentation** must include:
  - Setup and run instructions
  - Feature summary mapped to user stories
  - Class diagram and refactoring explanation
  - Commit log snapshot showing version control usage

### Submission Link:

Please fill out this form (**insert actual form link here**)

## Testing the Application

Documented results below match this Lab 3 build: `HashMap` plus `ArrayList`, ids like `PRJ001` / `TSK001`, NIO save and load, three update threads, and JUnit 5 + Mockito.

### Test Scenario 1: Refactored Project Creation

**Steps**

1. Run `Main` (IntelliJ or `mvn -q compile` then `java -cp target/classes Main`).
2. Main menu → **1. Manage Projects** → **1. Add Project**.
3. Choose Software, then enter name, description, duration, budget, team size, lead, technology.

**Result — Done**

- `SoftwareProject` / `HardwareProject` constructors set private fields; callers use getters only.
- `Project` assigns the next ID with `String.format("PRJ%03d", nextNumber)` (sample data already loads `PRJ001`–`PRJ005`, so a new project is `PRJ006`).
- Banner and success lines print through `ConsoleMenu`; `Main` only wires services.

**Example**

```text
╔════════════════════════════════════════════╗
║              MANAGE PROJECTS               ║
╚════════════════════════════════════════════╝

1. Add Project
2. Browse Project Catalog
3. <<Back

Enter your choice: 1
... (Software + details) ...
✓ Project 'My App' created with ID PRJ006.
```

### Test Scenario 2: Add Task with Exception Handling

**Steps**

1. Main menu → **2. Manage Tasks** → **1. Add Task** (or add from a project details screen).
2. Enter a missing project id such as `PRJ999`.
3. Enter a valid id (for example `PRJ001`) and task details.

**Result — Done**

- `promptForProject` throws `ProjectNotFoundException` when the id is missing; `printError` shows it and asks again — the menu does not exit.
- A valid project creates a task via `TaskService.addTask(...)` with a generated `TSK` id.
- Updating/removing a missing task throws `TaskNotFoundException`.

**Example**

```text
Enter project ID (or 0 to go back): PRJ999
 Error: ProjectNotFoundException – Project ID 'PRJ999' does not exist.
Please try again.

Enter project ID (or 0 to go back): PRJ001
✓ Task 'Design' successfully added to Project PRJ001.
```

```text
Enter task ID: TSK999
 Error: TaskNotFoundException – Task 'TSK999' was not found in the project.
```

### Test Scenario 3: View Projects (After Refactoring)

**Steps**

1. Main menu → **1. Manage Projects** → **2. Browse Project Catalog**.
2. Choose **1. View All Projects**, or Software / Hardware filters.
3. Open **View Project Details** for one id.

**Result — Done**

- Listing goes through `ProjectService` (`printAll`, `printByType`, `printByBudget`).
- Details use getters / polymorphism (`getType()`, `getProjectDetails()`), not raw fields.
- Each project keeps its own task list; opening a project shows only that project’s tasks (no duplicate rows from another project).

**Example**

```text
PROJECT CATALOG → View All Projects

PRJ001 | Alpha Tracker   | Software | ...
PRJ002 | IoT Sensor Kit  | Hardware | ...
PRJ003 | Mobile Checkout | Software | ...
...

Enter project ID: PRJ001
 → PROJECT DETAILS: PRJ001 (tasks and completion for Alpha Tracker only)
```

### Test Scenario 4: Calculate Completion Percentage (Refactored Logic)

**Steps**

1. Main menu → **3. View Status Reports** (all projects + average).
2. Or open a project’s details to see that project’s progress.
3. For a project with zero tasks, open details / completion path and confirm the empty-project path.

**Result — Done**

- Per-project percent: `TaskService.calculateCompletionPercentage(project)` → completed / total × 100.
- Catalog average: `ReportService` builds `StatusReport` rows and prints `AVERAGE COMPLETION`.
- Zero tasks: `EmptyProjectException` (“Project 'PRJ005' has no tasks.” for Payroll Portal when completion is requested).

**Example**

```text
PROJECT ID | PROJECT NAME      | TASKS | COMPLETED | PROGRESS (%)
-----------------------------------------------------------------
PRJ001     | Alpha Tracker     |   3   |    1      |   33.33%
PRJ002     | IoT Sensor Kit    |   2   |    1      |   50.00%
...
AVERAGE COMPLETION: xx.xx%
```

JUnit check (same formula):

```java
// createProject / addTask / calculateCompletion in ProjectTests
taskService.addTask(project, "Design", "", TaskStatus.COMPLETED, 2);
taskService.addTask(project, "Build", "", TaskStatus.PENDING, 3);
assertEquals(50.0, taskService.calculateCompletionPercentage(project));
```

### Test Scenario 5: Exception Handling for Invalid Inputs

**Steps**

1. At any numeric prompt, type a letter (for example `abc`).
2. Enter budget `0` or a negative team size / duration / hours.
3. Confirm the menu stays open and prompts again.

**Result — Done**

- `ValidationUtils` throws `InvalidInputException` for bad numbers, empty text, bad email, budget ≤ 0, etc.
- `ConsoleMenu` catches, prints `Error: InvalidInputException – ...`, then `Please try again.`
- Missing project/task ids use `ProjectNotFoundException` / `TaskNotFoundException`; empty completion uses `EmptyProjectException`.

**Example**

```text
Enter your choice: abc
Please enter a number.

Enter minimum budget: 0
 Error: InvalidInputException – Budget must be greater than 0.
Please try again.
```

### Test Scenario 6: Run JUnit Tests

**Steps**

1. Open `test/ProjectTests.java` or run `mvn test`.
2. Run the class (JUnit 5 + Mockito).

**Result — Done**

`mvn test` runs 14 tests across four classes.

| Test | What it proves |
| --- | --- |
| `createProject` | Real `SoftwareProject` stored in real `ProjectService`; name/type/count asserted |
| `addTask` | Task added onto the project; `getTaskCount() == 1` |
| `calculateCompletion` | One of two completed → `50.0` |
| `missingTaskThrows` | Mocked `ProjectService.getCount() == 0` → `TaskNotFoundException` |
| `adminCanDeleteProject` | `AdminUser.canUpdateAndDelete()` is `true` |
| `regularUserCannotDeleteProject` | `RegularUser.canUpdateAndDelete()` is `false` |
| `testFilterCompletedTasksUsingStreams` | Filter keeps the completed task, map keeps both names, reduce totals `50.0` |
| `regexAcceptsProjectAndTaskIds` | `PRJ001` and `TSK004` match; `TX01` and `T001` do not |
| `badTaskIdThrows` | A bad task id throws `InvalidInputException` |
| `saveAndLoadKeepsProjectAndTask` | A software project and its task survive save and load |
| `missingFileReturnsFalse` | A missing file returns false and adds no projects |
| `malformedFileReturnsFalse` | Broken file text returns false and adds no projects |
| `threeThreadsUpdateThreeTasks` | Three tasks end as In Progress, Completed, Completed |
| `fewerThanThreeTasksUpdatesOnlyExistingOnes` | One task becomes In Progress and the list stays size 1 |

**Example**

```text
$ mvn test
...
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test Scenario 7: Git Commit and Branch Workflow

**Steps**

1. Work was versioned on GitHub (repo already existed; no second `git init`).
2. Feature branches for Lab 2, then the three Lab 3 branches, each merged into `main`.

**Result — Done**

| Branch | Purpose |
| --- | --- |
| `feature/refactor` | Phase 1 — services / single responsibility |
| `feature/exceptions` | Phase 2 — custom exceptions + menu try/catch |
| `feature/testing` | Phase 3 — JUnit tests |
| `feature/taskvalidation` | Follow-up — Maven `src/test`, Mockito, README |
| `feature/collections-fp` | Lab 3 — lists, map, streams, regex, and smaller menus |
| `feature/file-io` | Lab 3 — save and load `projects_data.json` |
| `feature/concurrency` | Lab 3 — three threads update tasks safely |

Merged into `main`: `feature/refactor`, `feature/exceptions`, `feature/testing`, `feature/taskvalidation`, `feature/collections-fp`, `feature/file-io`, `feature/concurrency`.

**Example commit history**

```text
Move tests under Maven src/test and document JUnit/Mockito coverage.
Add Maven so the tests run with JUnit and Mockito.
Merge feature/testing into main.
Merge feature/exceptions into main.
Merge feature/refactor into main.
Add JUnit tests for completion, task status, and budget.
exception handling
Add checked exceptions so bad input stays in the menu.
Refactor each class to one job so the menu only reads input.
```

### Test Scenario 8: Git Push and Remote Verification

**Steps**

1. Push all workflow branches to GitHub.
2. Confirm README, `.gitignore`, `src/`, and tests are on the remote.

**Result — Done**

- Remote: [https://github.com/Sergeb250/Task-Mgt-System-II.git](https://github.com/Sergeb250/Task-Mgt-System-II.git)
- Branches on remote: `main`, `feature/refactor`, `feature/exceptions`, `feature/testing`, `feature/taskvalidation`, `feature/collections-fp`, `feature/file-io`, `feature/concurrency`
- Clone path for a teammate:

```bash
git clone https://github.com/Sergeb250/Task-Mgt-System-II.git
cd Task-Mgt-System-II
mvn test
mvn -q compile
java -cp target/classes Main
```

### Test Scenario 9: Code Cleanliness and Comments Check

**Steps**

1. Review `Project.java`, `Task.java`, `Main.java`, services, and `ConsoleMenu`.
2. Check naming, indentation, and dead code.

**Result — Done**

- `Main` loads `data/projects_data.json`, starts the menu, and saves the file on exit.
- Models keep fields private. Projects sit in a `HashMap`. Tasks sit in an `ArrayList`.
- Short class/method comments describe intent (for example on `Project`, `TaskService`, exception types).
- Test sources live under Maven `test/`.

## Lab 3 at a glance

Projects are stored in a `HashMap`. Each project keeps its tasks in an `ArrayList`. Filtering and completion use streams, lambdas, and method references (`StreamService`, `TaskFilter`, `Project.getCompletionRate()`).

IDs are checked with `Pattern` and `Matcher` in `RegexValidator` (`PRJ###`, `TSK###`).

On startup the app reads `data/projects_data.json`. On exit it writes that file again with try-with-resources. A missing or broken file prints an error and starts with an empty catalog.

Main menu option **5** runs three threads that update tasks. `TaskService.updateStatus` is `synchronized`. `join()` waits until those threads finish.

Run the app from the project folder so `data/projects_data.json` is found.

```bash
mvn test
mvn -q compile
java -cp target/classes Main
```

`mvn test` runs 14 tests:

| Class | What it covers |
| --- | --- |
| `test/ProjectTests.java` | Create, add task, 50 percent completion, missing task, admin and regular user |
| `test/StreamOperationsTest.java` | Filter, map, reduce, and id patterns |
| `test/FilePersistenceTest.java` | Save and load, a missing file, and a malformed file |
| `test/ConcurrencyTest.java` | Three tasks updated together, and one task when the other threads have nothing to update |
