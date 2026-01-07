package de.rapha149.signgui.util.scheduler;

import de.rapha149.signgui.util.PlatformDetector;
import de.rapha149.signgui.util.SchedulerAdapter;

/**
 * Factory class for creating appropriate scheduler adapters based on the server platform.
 */
public class SchedulerFactory {
    
    private static SchedulerAdapter cachedAdapter;
    
    /**
     * Gets the appropriate scheduler adapter for the current platform.
     *
     * @return The scheduler adapter
     */
    public static SchedulerAdapter getScheduler() {
        if (cachedAdapter != null) {
            return cachedAdapter;
        }
        
        if (PlatformDetector.isFolia()) {
            cachedAdapter = new FoliaSchedulerAdapter();
        } else {
            cachedAdapter = new BukkitSchedulerAdapter();
        }
        
        return cachedAdapter;
    }
    
    /**
     * Resets the cached scheduler adapter. Useful for testing or reloading.
     */
    public static void reset() {
        cachedAdapter = null;
    }
}
