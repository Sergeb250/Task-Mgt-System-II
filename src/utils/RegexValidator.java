package utils;

import java.util.regex.Pattern;

// Checks IDs and emails with Pattern and Matcher.
public class RegexValidator {

    // Week 2 IDs look like PRJ001 and TSK001.
    private static final Pattern PROJECT_ID = Pattern.compile("PRJ\\d{3}");
    private static final Pattern TASK_ID = Pattern.compile("TSK\\d{3}");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private RegexValidator() {
    }

    public static boolean isProjectId(String text) {

        return text != null && PROJECT_ID.matcher(text).matches();
    }

    public static boolean isTaskId(String text) {

        return text != null && TASK_ID.matcher(text).matches();
    }

    public static boolean isEmail(String text) {

        return text != null && EMAIL.matcher(text).matches();
    }
}
