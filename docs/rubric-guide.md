# Rubric guide (beginner)

This page points at the code for each grading row and explains that code in plain language.

Words used below:

- A **list** (`ArrayList`) grows when you add an item. The old code used a fixed array of 20 slots.
- A **map** (`HashMap`) stores a value under a key. Here the key is the project id, such as `PRJ001`.
- A **stream** walks a list without a `for` loop you write yourself. `filter` keeps some items. `map` turns each item into something else. `reduce` combines the items into one number.
- A **lambda** is a small function written where you need it: `task -> task.getStatus() == status`.
- A **method reference** is a lambda that only calls one method: `Task::isCompleted` means "call `isCompleted` on each task".

## 1. Collections and functional code (18 points)

**Where**

| Idea | File |
| --- | --- |
| Tasks in a list | `src/models/Project.java` |
| Projects in a map | `src/services/ProjectService.java` |
| Users in a list | `src/services/UserService.java` |
| Functional interface | `src/interfaces/TaskFilter.java` |
| Lambda used on that interface | `src/services/TaskService.java` (`countByStatus`) |
| filter, map, reduce | `src/services/StreamService.java` |
| Completion with a stream | `src/models/Project.java` (`getCompletionRate`, `isCompleted`) |

**What the code does**

`Project` keeps tasks in `List<Task> tasks = new ArrayList<>()`. Adding a task is `tasks.add(task)`. There is no `MAX_TASKS` cap.

`ProjectService` keeps projects in `Map<String, Project>`. `addProject` does `projects.put(project.getId(), project)`. Finding a project is a stream:

```java
projects.values().stream()
    .filter(project -> project.getId().equalsIgnoreCase(projectId))
    .findFirst()
    .orElse(null);
```

That means: look at every project, keep the one whose id matches, take the first match, or null if none matched.

`getCompletionRate` counts finished tasks with a stream. One completed task out of two is `1 / 2 * 100 = 50`.

`TaskFilter` is the functional interface. It has one method, `test(Task)`. This lambda is a `TaskFilter`:

```java
TaskFilter filter = task -> task.getStatus() == status;
project.getTasks().stream().filter(filter::test).count();
```

`StreamService` is the three stream shapes the lab asks for:

- `filter` keeps completed tasks (`Task::isCompleted`).
- `map` turns tasks into names (`Task::getName`).
- `reduce` adds the completion percents, starting from `0.0`.

The catalog screen **Completed Projects (over 70%)** uses the same filter idea in `ProjectService.printAboveCompletion`.

## 2. File persistence (18 points)

**Where**

| Idea | File |
| --- | --- |
| Write and read the file | `src/utils/FileUtils.java` |
| Call load on startup and save on exit | `src/Main.java` |
| Saved sample data | `data/projects_data.json` |
| Tests that do not touch the real file | `test/FilePersistenceTest.java` |

**What the code does**

`Main` prints `Loading projects from file...` and calls `FileUtils.loadProjects`. If the file is missing, load prints `File not found` and returns false. `Main` then prints `Starting with empty catalog.` If the file is broken, load prints `malformed data` and does not keep a half-loaded list.

On the way out, `finally` always calls `saveProjects`. `finally` runs after a normal exit and also when the input closes early.

Save sorts projects by id, then writes text that looks like JSON. A software project's technology and a hardware project's equipment both go in the field `"extra"`. The write is:

```java
try (BufferedWriter writer = Files.newBufferedWriter(path)) {
    writer.write(text.toString());
}
```

`try (` ... `)` is try-with-resources. Java closes the writer when the block ends.

Load reads with a stream of lines:

```java
try (Stream<String> lines = Files.lines(path)) {
    List<String> all = lines.collect(Collectors.toList());
}
```

`Files.lines` and `Files.newBufferedWriter` are `java.nio.file` (NIO). The reader rebuilds a `SoftwareProject` or a `HardwareProject` from the type line, then adds the tasks with their old `TSK` ids.

## 3. Regex validation (12 points)

**Where**

| Idea | File |
| --- | --- |
| The patterns | `src/utils/RegexValidator.java` |
| Using the patterns on keyboard input | `src/utils/ValidationUtils.java` (`normalizeId`, `readEmail`, `readPrefixedId`) |
| The error the user sees | `ValidationUtils.readPrefixedId` |
| Tests | `test/StreamOperationsTest.java` (`regexAcceptsProjectAndTaskIds`, `badTaskIdThrows`) |

**What the code does**

A pattern is a rule for the shape of text.

- `PRJ\\d{3}` means the letters `PRJ` and then exactly three digits. `PRJ001` matches. `TX01` does not.
- `TSK\\d{3}` is the same idea for tasks.
- The email pattern requires text, then `@`, then text, then a dot, then text.

`Pattern.compile` builds the rule once. `matcher(text).matches()` checks one string against that rule.

When the user types an id, `normalizeId` also allows `prj001` (any case) or a short number such as `1`, which becomes `PRJ001`. After that, `RegexValidator` must agree. If it does not, `readPrefixedId` throws `InvalidInputException` with:

`Invalid ID format. Use pattern PRJ### (e.g., PRJ001).`

`MenuScreen` catches that and asks again. The menu does not close.

## 4. Concurrency (17 points)

**Where**

| Idea | File |
| --- | --- |
| Three threads and a parallel stream | `src/services/ConcurrencyService.java` |
| The lock on writes | `src/services/TaskService.java` (`synchronized` on `updateStatus`, `addTask`, `removeTaskById`) |
| Menu entry | Main menu option 5, `src/utils/ConsoleMenu.java` and `TaskMenu.simulateUpdates` |
| Test | `test/ConcurrencyTest.java` |

**What the code does**

A thread is a second path of work that can run while another path is also running. `simulate` starts three of them:

- Thread-1 sets the first task to In Progress.
- Thread-2 sets the second task to Completed.
- Thread-3 sets the third task to Completed.

Each thread calls `taskService.updateStatus`. That method is `synchronized`, so only one thread at a time can be inside it. That is what stops two threads from fighting over the same status change.

`join()` waits until that thread has finished. After all three joins, the updates are done.

Then a **parallel stream** prints the task names. `parallelStream()` may use more than one thread for the print. It does not change the tasks. The test checks the three statuses after `simulate` returns, so the result is In Progress, Completed, Completed.

Projects are sorted by id before the threads start, so "first task" is stable.

## 5. Testing and Git (13 points)

**Where**

| Tests | File | What they prove |
| --- | --- | --- |
| Project, task, 50 percent, missing task, admin vs regular | `test/ProjectTests.java` | Core behavior. The missing-task test uses a Mockito fake catalog with `getCount() == 0`. |
| filter, map, reduce, regex | `test/StreamOperationsTest.java` | Streams and id patterns |
| save, load, missing file, bad file, hardware round trip | `test/FilePersistenceTest.java` | File I/O on a temp file |
| three threads | `test/ConcurrencyTest.java` | Statuses after the threads finish |

**Git**

The Week 3 history is three branches merged into `main`:

| Branch | Commit |
| --- | --- |
| `feature/collections-fp` | Lists, map, streams, regex, split menus |
| `feature/file-io` | Save and load `projects_data.json` |
| `feature/concurrency` | Threads and `synchronized` updates |

Remote: https://github.com/Sergeb250/Task-Mgt-System-II

How to run the tests is in `docs/testing.md`.

## 6. Code quality and docs (10 points)

**Where**

| Idea | File |
| --- | --- |
| One job per menu | `ConsoleMenu` only runs the main menu. `ProjectMenu`, `TaskMenu`, `UserMenu`, and `ReportMenu` each own one area. Shared prompts are in `MenuScreen`. |
| What each class does and why it calls other classes | `docs/class-logic.md` |
| This rubric map | `docs/rubric-guide.md` |
| How to run tests | `docs/testing.md` |
| Week 3 behavior and branch names | `README.md` |

Empty `finally` blocks were removed from the menus. Nothing in those `try` blocks needed closing. The `finally` in `Main` stays, because that one saves the file.

## 7. Data structures and algorithms (10 points)

**Where:** the same collection code as row 1, plus the sorts and the completion math.

**What is efficient here, in beginner terms**

- A `HashMap` finds a project by id without scanning a fixed array yourself. `findById` still streams the values so a lower-case id such as `prj001` matches `PRJ001`.
- An `ArrayList` adds and removes a task without shifting a handmade array. `remove` on the list does that shift inside Java.
- Sorting for the printed catalog is `sorted` by name, ignoring case. The file is sorted by id before it is written, so the saved order does not jump around.
- Completion is one pass over the task list: count the completed ones, divide by the size. That is `filter` + `count` in `getCompletionRate`.
- The report average is `mapToDouble(...).average()`, which is a pass over the projects.
- `StreamService.totalCompletion` is the same numbers added with `reduce`.

The old bubble-sort loops on arrays are gone. The streams do the filter and the sort instead.
