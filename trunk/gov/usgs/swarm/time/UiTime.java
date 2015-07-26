package gov.usgs.swarm.time;

/**
 * A class to track time of last user action. Used by helicorder in kiosk mode.
 * 
 * @author tparker
 *
 */
public final class UiTime {
    private static long lastUiTime = System.currentTimeMillis();
    
    private UiTime() {}
    
    public static void touchTime() {
        lastUiTime = System.currentTimeMillis();
    }

    public static long getTime() {
        return lastUiTime;
    }
}
