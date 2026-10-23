# Class logic

This note explains every Java class from the interfaces down to the tests. For each class it says what is inside, how that code works, and which other classes it needs and why.

The program starts in `Main`. `Main` builds the services, loads users, reads `data/projects_data.json`, then hands the services to `ConsoleMenu`. The menu classes only read the keyboard. The services change the data. The models hold the data.

## Interfaces

### `Completable`

**Inside:** one method, `isCompleted()`.

**How:** the interface does not decide what "completed" means. `Project` and `Task` each give their own answer.

**Depends on:** nothing in this project.

**Why:** both a task and a project can be finished, but the rule is different. A task is finished when its status is Completed. A project is finished when every task is finished. The shared method lets other code ask either one the same question.

### `TaskFilter`

**Inside:** one method, `test(Task task)`, marked `@FunctionalInterface`.

**How:** a lambda is the filter. It returns true when the task should be kept. `TaskService.countByStatus` writes that lambda as `task -> task.getStatus() == status`, then the stream calls `filter::test`.

**Depends on:** `Task`.

**Why:** the filter has to look at one task. It does not need a project or a service, because the status is already on the task.

## Models

### `TaskStatus`

**Inside:** three values, `PENDING`, `IN_PROGRESS`, and `COMPLETED`. Each one stores a display label such as `"In Progress"`. `fromText` turns user text into one of those values.

**How:** the text is trimmed and compared in lower case. `1` or `pending` becomes `PENDING`. `2`, `in progress`, `inprogress`, or `in-progress` becomes `IN_PROGRESS`. `3` or `completed` becomes `COMPLETED`. Anything else returns null, and the caller treats that as bad input.

**Depends on:** `java.util.Locale` only, so the lower-case check does not change with the computer's language.

**Why:** status is a fixed set of words. An enum stops a task from storing a random string. The menus, the file loader, and the tests all share this one conversion.

### `Task`

**Inside:** id (`TSK001`, `TSK002`, ...), name, assigned person, hours, and a `TaskStatus`. A static counter makes the next id. A second constructor keeps an id that was read from the file.

**How:** the normal constructor asks for the next id, then stores the fields. The file constructor stores the given id and moves the counter past that number so the next new task does not reuse it. `isCompleted()` is true only when the status is `COMPLETED`. `setStatus` is the one field that changes after creation.

**Depends on:** `TaskStatus`, and it implements `Completable`.

**Why:** the status enum is the only legal set of states, and `isCompleted()` has to read that state. The task does not know about projects. The project is the one that holds the list of tasks.

### `Project`

**Inside:** id (`PRJ001`, ...), name, description, team size, duration, budget, lead name, and an `ArrayList<Task>`. It is abstract. Subclasses must say the type and the extra detail line.

**How:** the normal constructor assigns the next `PRJ` id. The file constructor keeps a saved id and moves the counter forward, same idea as `Task`. `getTasks()` returns the real list, so a service can add or remove tasks. `isCompleted()` is false when the list is empty. Otherwise it is true only when every task's `isCompleted()` is true (`allMatch`). `getCompletionRate()` is completed tasks divided by all tasks, times 100. An empty project is 0 percent.

**Depends on:** `Task`, and it implements `Completable`.

**Why:** completion is calculated from the tasks stored on this project. The project does not need `ProjectService` or the menu. Those classes call into the project. They do not live inside it.

### `SoftwareProject`

**Inside:** one extra field, `technology`. Two constructors: one makes a new id, one is used when the file already has an id.

**How:** both constructors pass the shared fields to `Project`, then store the technology. `getType()` returns `"Software"`. `getProjectDetails()` returns `"Technology: "` plus that value.

**Depends on:** `Project`.

**Why:** a software project has the same id, tasks, and budget as any project, plus one field that only software needs. Extending `Project` keeps that shared data in one place. `FileUtils` checks `instanceof SoftwareProject` when it writes the technology into the file.

### `HardwareProject`

**Inside:** one extra field, `equipment`. Same two constructors as `SoftwareProject`.

**How:** `getType()` returns `"Hardware"`. `getProjectDetails()` returns `"Equipment: "` plus the equipment name.

**Depends on:** `Project`.

**Why:** same reason as `SoftwareProject`. The catalog can store both types in one `HashMap<String, Project>` and still print the right extra line.

### `User`

**Inside:** id (`USR001`, ...), name, email, and role. `canUpdateAndDelete()` is abstract.

**How:** the constructor assigns the next user id and stores the role string it was given. Subclasses choose the role word and the permission answer.

**Depends on:** nothing else in this project.

**Why:** admin and regular users share the id, name, and email. Only the permission differs, so that answer is left to the subclass.

### `AdminUser`

**Inside:** a constructor only.

**How:** it calls `User` with the role `"Admin"`. `canUpdateAndDelete()` returns true.

**Depends on:** `User`.

**Why:** the menus call `canUpdateAndDelete()` before an update, a delete, or creating a user. An admin must be a `User` so it can sit in the same user list as a regular user.

### `RegularUser`

**Inside:** a constructor only.

**How:** it calls `User` with the role `"Regular"`. `canUpdateAndDelete()` returns false.

**Depends on:** `User`.

**Why:** a regular user is still a `User`, so switching users does not care which subclass is logged in until a permission check runs.

### `StatusReport`

**Inside:** a snapshot of one project: id, name, counts for completed, pending, and in progress, total tasks, completion percent, and a complete flag. Every field is final.

**How:** `ReportService` fills it once. Later printing only reads the getters. The report does not go back to the project.

**Depends on:** nothing else in this project.

**Why:** the numbers are copied out so the printed row stays stable even if another part of the program changes a task while the table is being built. It does not need `Project` or `Task` because it only stores already calculated numbers.

## Exceptions

These four classes extend `Exception`, so they are checked. A method that throws one must say so, and the menu must catch it. That is why bad input returns to the prompt instead of crashing the program. None of them depend on models or services. They only carry a message.

### `InvalidInputException`

**How:** thrown when a number, a piece of text, an email, a budget, a status, or an id is not usable.

**Why it exists:** `ValidationUtils` throws it. `MenuScreen` catches it, prints the message, and asks again.

### `ProjectNotFoundException`

**How:** thrown when the typed project id is not in the catalog.

**Why it exists:** `ProjectMenu.askForProject` catches it and asks for the id again. The menu stays open.

### `TaskNotFoundException`

**How:** thrown when a task id is missing from one project or from the whole catalog.

**Why it exists:** `TaskService.findRequired` throws it. `TaskMenu` catches it and returns to the task menu.

### `EmptyProjectException`

**How:** thrown when a project details screen asks for a completion rate and the project has no tasks.

**Why it exists:** `ProjectMenu` still prints the project header, then catches this and shows the error. The empty project is not treated as 100 percent complete.

## Services

### `ProjectService`

**Inside:** a `HashMap<String, Project>` keyed by project id. Methods add a project, count them, find one by id, and print lists.

**How:** `addProject` puts the project in the map, so there is no fixed size of 20. `isFull()` is always false for that reason. `findById` streams the values and keeps the first id that matches, ignoring case. `printAll`, `printByType`, and `printByBudget` filter with a stream, sort by name, and print a table. `printAboveCompletion` keeps projects whose `getCompletionRate()` is above the given number. `getProjects()` returns a copy of the values so a caller can sort that copy without changing the map. `getAt` uses that copy by index. `find` across tasks still uses `getCount` and `getAt`.

**Depends on:** `Project`.

**Why:** this class is the catalog. It stores projects and prints them. It does not need `TaskService`, because each project already knows its own completion rate. It does not read the keyboard.

### `TaskService`

**Inside:** a reference to the `ProjectService`, plus the task operations: add, find, remove, count by status, completion percent, and status update.

**How:** tasks are stored on the project, in `project.getTasks()`, not in this service. `addTask` refuses a duplicate name, creates a `Task`, and appends it. `countByStatus` builds a `TaskFilter` lambda and counts matches with a stream. `calculateCompletionPercentage` returns `project.getCompletionRate()`. `findById` walks every project through `getCount` and `getAt`. `findRequired` calls that and throws `TaskNotFoundException` when the result is null. `addTask`, `removeTaskById`, and `updateStatus` are `synchronized` so two threads cannot change the list or a status at the same time.

**Depends on:** `ProjectService`, `Project`, `Task`, `TaskStatus`, `TaskFilter`, and `TaskNotFoundException`.

**Why each one:**

- `ProjectService` is needed when the task id might be in any project. The service does not keep its own copy of the projects.
- `Project` and `Task` are the data being changed.
- `TaskStatus` is the value stored on a task and the value passed to `updateStatus`.
- `TaskFilter` is the small rule object for "does this task have this status?" so the count can be a stream filter.
- `TaskNotFoundException` is how a missing id is reported to the menu without returning a null that the menu might forget to check.

### `ReportService`

**Inside:** a `TaskService`, and methods that print one project or the whole catalog.

**How:** `makeReport` asks the task service for the three status counts and the completion percent, asks the project if it is complete, and stores all of that in a new `StatusReport`. `printReport(ProjectService)` sorts projects by name, prints one row per report, then averages the percents with `mapToDouble(...).average()`. `printReport(Project)` prints the single report as labeled lines. An empty catalog prints `No projects found.`

**Depends on:** `TaskService`, `Project`, `ProjectService`, `StatusReport`, and `TaskStatus`.

**Why:** the report should not count tasks itself. `TaskService` already owns that rule. `StatusReport` holds one finished row so the print method is only formatting. `ProjectService` is the list of projects to include. `TaskStatus` is passed into `countByStatus` so the three counts stay the same three states as the rest of the app.

### `StreamService`

**Inside:** three static methods and no fields.

**How:** `completedTasks` filters with `Task::isCompleted` and collects a list. `taskNames` maps each task to `Task::getName`. `totalCompletion` maps each project to `getCompletionRate` and `reduce`s the numbers by adding them, starting at 0.

**Depends on:** `Task` and `Project`.

**Why:** these are the filter, map, and reduce examples in one place the tests can call. They do not need a service or the catalog, because the caller already has the list.

### `UserService`

**Inside:** an `ArrayList<User>` and the current user.

**How:** `addUser` appends. `getAt` reads by index for the switch-user menu. `emailExists` streams the list and compares email, ignoring case. `isFull()` is false because a list has no fixed cap. `setCurrentUser` is what login and "switch user" change.

**Depends on:** `User`.

**Why:** the menu must not keep the user array itself. One service owns the list and the logged-in user. It stores `User`, not `AdminUser`, so both roles fit in the same list.

### `ConcurrencyService`

**Inside:** a `TaskService`, and `simulate(ProjectService)`.

**How:** it collects every task, projects sorted by id so the order does not change between runs. Thread-1 sets task 0 to In Progress. Thread-2 sets task 1 to Completed. Thread-3 sets task 2 to Completed. Each thread calls `taskService.updateStatus`, which is synchronized. The method `join`s all three threads, then prints task names with a parallel stream. If a thread's index is past the end of the list, it prints that it has no task.

**Depends on:** `TaskService`, `ProjectService`, `Project`, `Task`, and `TaskStatus`.

**Why:** `ProjectService` is where the tasks are found. `TaskService.updateStatus` is the synchronized write, so the threads do not set a status by touching the task on their own. `TaskStatus` is the new value each thread applies. The parallel stream only prints names. It does not change data.

## Utils

### `RegexValidator`

**Inside:** three patterns. `PRJ` plus three digits, `TSK` plus three digits, and a simple email shape (`something@something.something`).

**How:** each method runs `matcher(text).matches()` on its pattern. Null returns false.

**Depends on:** `java.util.regex.Pattern` only.

**Why:** id and email rules should not be copied into the menu and the tests. `ValidationUtils` asks this class after it has normalized an id. The patterns match the ids this app already creates (`PRJ001`, `TSK001`), not a different prefix.

### `ValidationUtils`

**Inside:** static readers. Each one prints a prompt, reads one line, and either returns a value or throws `InvalidInputException`.

**How:** `readChoiceInRange` and `readPositiveInt` parse an int and check the range. `readBudget` parses a double and rejects 0 or less. `readStatus` uses `TaskStatus.fromText`. `readText` rejects a blank line. `readEmail` uses `readText`, then `RegexValidator.isEmail`. `readPrefixedId` returns null when the user types `0` and zero is allowed (that is the "go back" choice). Otherwise `normalizeId` accepts `PRJ001` / `TSK001` or a number from 1 to 999 and builds the prefixed id. The result must pass `RegexValidator` for that prefix.

**Depends on:** `TaskStatus`, `RegexValidator`, and `InvalidInputException`. The method also takes a `Scanner`.

**Why:** `TaskStatus` is the only legal status result. `RegexValidator` already owns the patterns, so this class does not compile them again. `InvalidInputException` is the checked signal that the menu retries. The class does not depend on a service because checking a line of text does not need the catalog.

### `FileUtils`

**Inside:** `DATA_FILE` (`data/projects_data.json`), `saveProjects`, and `loadProjects`.

**How:** save sorts projects by id and writes a json-like text file. Software projects store technology in `"extra"`. Hardware projects store equipment there. Each task is one line with id, name, assigned person, status label, and hours. The write uses `Files.newBufferedWriter` inside try-with-resources. Load returns false when the file is missing and prints that message. When the file exists, `Files.lines` reads it in a try-with-resources stream. A project starts at `{`, fields are read by name, a line that contains `"id"` becomes a task, and `}` builds either a `SoftwareProject` or a `HardwareProject` and adds the tasks. A file that is not a `[` list, or a project missing a name or type, is treated as malformed and load returns false without keeping a half-read catalog.

**Depends on:** `ProjectService`, `Project`, `SoftwareProject`, `HardwareProject`, `Task`, and `TaskStatus`.

**Why:** the service is the catalog being filled or read. The two subclasses are required because a loaded project must be the same type it was when it was saved. `Task`'s id constructor keeps `TSK` ids stable. `TaskStatus.fromText` turns the saved label back into the enum. `FileUtils` does not know about the menu. `Main` calls it at startup and again in `finally` on the way out.

### `SampleDataLoader`

**Inside:** `loadUsers`, `loadSampleProjects`, and `load`, which calls both.

**How:** `loadUsers` adds one admin (`eric mugisha`) and one regular user (`shima elyse`) and sets the admin as the current user. `loadSampleProjects` builds the five demo projects and their tasks. `Main` calls only `loadUsers`. The projects the running app shows come from the json file. `loadSampleProjects` is still here so the demo list can be rebuilt in code.

**Depends on:** `AdminUser`, `RegularUser`, `SoftwareProject`, `HardwareProject`, `TaskStatus`, `UserService`, `ProjectService`, and `TaskService`.

**Why:** users must exist before the menu prints "Current User". Projects and tasks are created through the services so they get real ids and sit in the same lists the menu uses later.

### `MenuScreen`

**Inside:** the shared `Scanner`, the `UserService`, the retry loops for every kind of input, `requireAdmin`, `printError`, and `printBanner`.

**How:** each reader calls `ValidationUtils` inside `while (true)`. On `InvalidInputException` it prints the error and asks again. There is no `finally` here, because nothing was opened that has to be closed. The scanner stays open for the whole program. `requireAdmin` reads the current user and prints a refusal when `canUpdateAndDelete()` is false.

**Depends on:** `UserService`, `ValidationUtils`, and `InvalidInputException`. It also uses `TaskStatus` as the return type of `readStatus`.

**Why:** every screen needs the same prompts. Putting them here means `ProjectMenu`, `TaskMenu`, `UserMenu`, and `ReportMenu` do not each copy the loops. `UserService` is required because permission depends on who is logged in, not on which screen is open. `ValidationUtils` does the actual check so this class only decides to retry.

### `ProjectMenu`

**Inside:** add project, the catalog filters, budget search, and the project details screen.

**How:** `open` loops until Back. Add project asks software or hardware, then name, description, duration, budget, and team size. The lead name is the current user's name. Browse can show all, software only, hardware only, a budget range, or projects over 70 percent complete. Details prints the project and its tasks. If there are no tasks it throws and catches `EmptyProjectException`. From details, choices 1 to 3 call `TaskMenu` to add, update, or remove a task. `askForProject` reads a `PRJ` id, looks it up, and repeats when it is missing. Typing `0` returns null, which the caller treats as Back.

**Depends on:** `MenuScreen`, `ProjectService`, `TaskService`, `TaskMenu`, `SoftwareProject`, `HardwareProject`, `Project`, `InvalidInputException`, `ProjectNotFoundException`, and `EmptyProjectException`.

**Why:** `ProjectService` is the catalog being changed and printed. `SoftwareProject` and `HardwareProject` are the objects created from the form. `TaskService` supplies the completion percent on the details screen. `TaskMenu` owns add, update, and remove, so this class does not also implement task editing. The exceptions are the errors this screen knows how to show and recover from.

### `TaskMenu`

**Inside:** the task menu, plus add, update, and remove used both from that menu and from project details. `simulateUpdates` starts the thread demo.

**How:** `open` loops until Back. Add task refuses to run when the catalog is empty, asks for a name, then asks `ProjectMenu` for the project. `saveTask` blocks a duplicate name, reads status and hours, and calls `taskService.addTask`. Update and remove call `requireAdmin` first. Remove catches `TaskNotFoundException` and returns to the task menu. `printTasks` prints the task table, or `No tasks found.` `simulateUpdates` prints the banner and calls `ConcurrencyService.simulate`.

**Depends on:** `MenuScreen`, `ProjectService`, `TaskService`, `ProjectMenu`, `ConcurrencyService`, `Project`, `Task`, `TaskStatus`, and `TaskNotFoundException`.

**Why:** `ProjectService` answers "are there any projects?" and is passed into the concurrency demo. `TaskService` performs the add, find, update, and remove. `ProjectMenu` is needed because choosing a project id is project-screen behavior, and the task screen should not duplicate that prompt. `TaskMenu` is constructed first, then `setProjectMenu` connects them, because each menu is created with the other one already in hand. `ConcurrencyService` is only the demo. The menu does not start threads itself.

### `UserMenu`

**Inside:** the switch-user list and `createUser`.

**How:** `open` numbers every user and shows whether they can update and delete. The last two choices are Create User and Back. Picking a user calls `setCurrentUser`. Create user checks `requireAdmin`, rejects a duplicate email, then builds an `AdminUser` or a `RegularUser`.

**Depends on:** `MenuScreen`, `UserService`, `User`, `AdminUser`, and `RegularUser`.

**Why:** `UserService` is the list and the logged-in user. The two subclasses are constructed here because this screen is where the role choice happens. Permission still goes through `User.canUpdateAndDelete()`, so the menu does not check the class with `instanceof`.

### `ReportMenu`

**Inside:** one screen, `open`.

**How:** it prints the banner, calls `reportService.printReport(projectService)`, then waits for Back.

**Depends on:** `MenuScreen`, `ReportService`, and `ProjectService`.

**Why:** `ReportService` knows how to build and print the table. `ProjectService` is the catalog the report reads. This class does not calculate a percent itself.

### `ConsoleMenu`

**Inside:** the four screen objects and `start()`.

**How:** the constructor makes one `Scanner` and passes it to every screen. It builds `TaskMenu` first, then `ProjectMenu` with that task menu, then calls `setProjectMenu`. `start` prints the main menu until choice 6. Choice 1 opens projects, 2 opens tasks, 3 opens reports, 4 opens users, 5 runs `taskMenu.simulateUpdates()`, and 6 prints `Goodbye.`

**Depends on:** `MenuScreen`, `ProjectMenu`, `TaskMenu`, `UserMenu`, `ReportMenu`, and the four services it receives so it can pass them on. It also reads the current `User` only to print the name at the top.

**Why:** `Main` should talk to one menu object. The other menu classes exist so project, task, user, and report screens are not methods in this file. `ConsoleMenu` does not store projects or tasks.

## `Main`

**Inside:** `main` only.

**How:** it creates `ProjectService`, `TaskService` (given that project service), `ReportService` (given the task service), and `UserService`. Then `SampleDataLoader.loadUsers` so a current user exists. It loads `FileUtils.DATA_FILE`. On success it prints how many projects were loaded. On failure it says the catalog starts empty. `ConsoleMenu.start()` runs until the user exits. If input ends early, `NoSuchElementException` prints `Goodbye.` The `finally` block always tries to save. That `finally` is real cleanup: the file is written on the way out, including when input closes suddenly.

**Depends on:** `ProjectService`, `TaskService`, `ReportService`, `UserService`, `SampleDataLoader`, `FileUtils`, and `ConsoleMenu`.

**Why:** these are the objects the rest of the program needs, and `Main` is the one place that wires them. `TaskService` needs the project catalog in its constructor because finding a task looks through projects. `ReportService` needs `TaskService` because the report asks it for counts. `FileUtils` is called here, not inside a menu, so both a normal exit and a closed input stream still save.

## Tests

The tests live in `test/`. They construct real objects unless a test says it uses a mock. They do not start `Main` or read `data/projects_data.json`, except the file tests, which use a temporary file.

### `ProjectTests`

**Inside:** six tests. The setup makes a mock `ProjectService`, a real `TaskService` around that mock, and a real `SoftwareProject` named Alpha.

**How:**

- `createProject` uses a real `ProjectService`, adds Alpha, and checks the name, the type `Software`, and a count of 1.
- `addTask` adds Design and checks the name and `getTaskCount() == 1`. The mock is not used, because `addTask` on a project does not ask the catalog.
- `calculateCompletion` adds one completed task and one pending task and expects 50.
- `missingTaskThrows` stubs `getCount()` to return 0, expects `TaskNotFoundException` from `findRequired("TSK999")`, and verifies `getCount()` was called.
- `adminCanDeleteProject` expects `canUpdateAndDelete()` true.
- `regularUserCannotDeleteProject` expects false.

**Depends on:** `SoftwareProject`, `Task`, `TaskStatus`, `ProjectService`, `TaskService`, `AdminUser`, `RegularUser`, `TaskNotFoundException`, and Mockito.

**Why:** `TaskService.findRequired` only searches projects by calling `ProjectService.getCount` and `getAt`. The mock makes that catalog look empty without building projects. The other tests use real objects because they check the project and the task list directly. `AdminUser` and `RegularUser` are the permission rule the menus rely on.

### `StreamOperationsTest`

**Inside:** the stream example, plus the regex checks.

**How:** `testFilterCompletedTasksUsingStreams` adds Design UI as completed and Write Tests as pending. `completedTasks` must have size 1. `taskNames` must be those two names in order. `totalCompletion` on that one project must be 50. `regexAcceptsProjectAndTaskIds` accepts `PRJ001`, `TSK004`, and a normal email, and rejects `TX01` and `T001`. `badTaskIdThrows` feeds `TX01` to `readPrefixedId` and expects `InvalidInputException`.

**Depends on:** `SoftwareProject`, `Task`, `TaskStatus`, `ProjectService`, `TaskService`, `StreamService`, `RegexValidator`, and `ValidationUtils`.

**Why:** `TaskService` is used only to create tasks the same way the app does. `StreamService` is the code under test for filter, map, and reduce. `RegexValidator` and `ValidationUtils` are the code under test for ids. A mock catalog is not needed, because none of these calls search other projects.

### `FilePersistenceTest`

**Inside:** four tests. Each write test uses a temp folder, not `data/projects_data.json`.

**How:** `saveAndLoadKeepsProjectAndTask` saves Alpha with one completed Design task, loads into a new `ProjectService`, and checks the name, type, technology, task name, and status. `missingFileReturnsFalse` points at a path that is not there and expects false and an empty catalog. `malformedFileReturnsFalse` writes `this is not json` and expects false and no projects added. `hardwareProjectRoundTrip` checks that equipment (`Motors`) survives save and load.

**Depends on:** `FileUtils`, `ProjectService`, `TaskService`, `SoftwareProject`, `HardwareProject`, `Project`, and `TaskStatus`.

**Why:** `FileUtils` is the class under test. A new `ProjectService` on load proves the objects were rebuilt from the file, not left in the original service. Temp files are required so a test cannot wipe the sample catalog. `SoftwareProject` and `HardwareProject` are both needed because the file stores a different extra field for each type.

### `ConcurrencyTest`

**Inside:** `threeThreadsUpdateThreeTasks`.

**How:** it builds one real project with three pending tasks, calls `ConcurrencyService.simulate`, then checks index 0 is In Progress and indexes 1 and 2 are Completed.

**Depends on:** `ConcurrencyService`, `TaskService`, `ProjectService`, `SoftwareProject`, and `TaskStatus`.

**Why:** `simulate` reads tasks from the real `ProjectService` and writes through the real synchronized `TaskService`. A mock would not run the threads against a real list. The expected statuses match the three threads: In Progress, Completed, Completed.
