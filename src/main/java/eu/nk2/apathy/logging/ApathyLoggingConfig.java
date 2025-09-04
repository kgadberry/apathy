package eu.nk2.apathy.logging;

public class ApathyLoggingConfig {
    private static boolean debugEnabled = false;

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }
}
