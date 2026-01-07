package de.rapha149.signgui.util.scheduler;

import de.rapha149.signgui.util.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Folia-compatible scheduler implementation using region-based scheduling.
 * This implementation uses reflection to access Folia's API without requiring it at compile time.
 */
public class FoliaSchedulerAdapter implements SchedulerAdapter {
    
    private static final Class<?> REGION_SCHEDULER_CLASS;
    private static final Class<?> GLOBAL_REGION_SCHEDULER_CLASS;
    private static final Class<?> ENTITY_SCHEDULER_CLASS;
    private static final Class<?> ASYNC_SCHEDULER_CLASS;
    
    private static final Method GET_REGION_SCHEDULER_METHOD;
    private static final Method GET_GLOBAL_REGION_SCHEDULER_METHOD;
    private static final Method GET_ASYNC_SCHEDULER_METHOD;
    private static final Method REGION_EXECUTE_METHOD;
    private static final Method REGION_RUN_DELAYED_METHOD;
    private static final Method GLOBAL_RUN_METHOD;
    private static final Method ASYNC_RUN_NOW_METHOD;
    private static final Method ENTITY_GET_SCHEDULER_METHOD;
    private static final Method ENTITY_EXECUTE_METHOD;
    private static final Method ENTITY_RUN_DELAYED_METHOD;
    private static final Method IS_OWNED_BY_CURRENT_REGION_METHOD;
    
    private static final boolean FOLIA_AVAILABLE;
    
    static {
        boolean available = false;
        Class<?> regionSchedulerClass = null;
        Class<?> globalRegionSchedulerClass = null;
        Class<?> entitySchedulerClass = null;
        Class<?> asyncSchedulerClass = null;
        Method getRegionScheduler = null;
        Method getGlobalRegionScheduler = null;
        Method getAsyncScheduler = null;
        Method regionExecute = null;
        Method regionRunDelayed = null;
        Method globalRun = null;
        Method asyncRunNow = null;
        Method entityGetScheduler = null;
        Method entityExecute = null;
        Method entityRunDelayed = null;
        Method isOwnedByCurrentRegion = null;
        
        try {
            regionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            globalRegionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            entitySchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            asyncSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            
            Class<?> serverClass = Bukkit.getServer().getClass();
            getRegionScheduler = serverClass.getMethod("getRegionScheduler");
            getGlobalRegionScheduler = serverClass.getMethod("getGlobalRegionScheduler");
            getAsyncScheduler = serverClass.getMethod("getAsyncScheduler");
            regionExecute = regionSchedulerClass.getMethod("execute", org.bukkit.plugin.Plugin.class, Location.class, Runnable.class);
            regionRunDelayed = regionSchedulerClass.getMethod("runDelayed", org.bukkit.plugin.Plugin.class, Location.class, 
                    java.util.function.Consumer.class, long.class);
            
            globalRun = globalRegionSchedulerClass.getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class);
            
            asyncRunNow = asyncSchedulerClass.getMethod("runNow", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class);
            
            entityGetScheduler = Entity.class.getMethod("getScheduler");
            entityExecute = entitySchedulerClass.getMethod("execute", org.bukkit.plugin.Plugin.class, Runnable.class, 
                    Runnable.class, long.class);
            entityRunDelayed = entitySchedulerClass.getMethod("runDelayed", org.bukkit.plugin.Plugin.class, 
                    java.util.function.Consumer.class, Runnable.class, long.class);
            
            Class<?> regionizedServerClass = Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isOwnedByCurrentRegion = regionizedServerClass.getMethod("isOwnedByCurrentRegion", Location.class);
            
            available = true;
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: Required Folia/Paper scheduler class not found while initializing FoliaSchedulerAdapter: "
                    + e.getMessage());
            System.err.println("Falling back to Bukkit scheduler. This usually indicates a Folia/Paper version incompatibility or that Folia is not present.");
            e.printStackTrace(System.err);
        } catch (NoSuchMethodException e) {
            System.err.println("Warning: Required Folia/Paper scheduler method not found while initializing FoliaSchedulerAdapter: "
                    + e.getMessage());
            System.err.println("Falling back to Bukkit scheduler. This usually indicates a Folia/Paper or Bukkit API version incompatibility.");
            e.printStackTrace(System.err);
        } catch (Exception e) {
            System.err.println("Warning: Unexpected error while initializing FoliaSchedulerAdapter. Falling back to Bukkit scheduler.");
            e.printStackTrace(System.err);
        }
        
        FOLIA_AVAILABLE = available;
        REGION_SCHEDULER_CLASS = regionSchedulerClass;
        GLOBAL_REGION_SCHEDULER_CLASS = globalRegionSchedulerClass;
        ENTITY_SCHEDULER_CLASS = entitySchedulerClass;
        ASYNC_SCHEDULER_CLASS = asyncSchedulerClass;
        GET_REGION_SCHEDULER_METHOD = getRegionScheduler;
        GET_GLOBAL_REGION_SCHEDULER_METHOD = getGlobalRegionScheduler;
        GET_ASYNC_SCHEDULER_METHOD = getAsyncScheduler;
        REGION_EXECUTE_METHOD = regionExecute;
        REGION_RUN_DELAYED_METHOD = regionRunDelayed;
        GLOBAL_RUN_METHOD = globalRun;
        ASYNC_RUN_NOW_METHOD = asyncRunNow;
        ENTITY_GET_SCHEDULER_METHOD = entityGetScheduler;
        ENTITY_EXECUTE_METHOD = entityExecute;
        ENTITY_RUN_DELAYED_METHOD = entityRunDelayed;
        IS_OWNED_BY_CURRENT_REGION_METHOD = isOwnedByCurrentRegion;
    }
    
    /**
     * Checks if Folia is available.
     *
     * @return true if Folia is available, false otherwise
     */
    public static boolean isFoliaAvailable() {
        return FOLIA_AVAILABLE;
    }
    
    @Override
    public void runTask(JavaPlugin plugin, Runnable task) {
        if (!FOLIA_AVAILABLE) {
            fallbackScheduler().runTask(plugin, task);
            return;
        }
        
        try {
            Object globalScheduler = GET_GLOBAL_REGION_SCHEDULER_METHOD.invoke(Bukkit.getServer());
            GLOBAL_RUN_METHOD.invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> task.run());
        } catch (Exception e) {
            e.printStackTrace();
            fallbackScheduler().runTask(plugin, task);
        }
    }
    
    @Override
    public void runTask(JavaPlugin plugin, Entity entity, Runnable task) {
        if (!FOLIA_AVAILABLE) {
            fallbackScheduler().runTask(plugin, entity, task);
            return;
        }
        
        try {
            Object entityScheduler = ENTITY_GET_SCHEDULER_METHOD.invoke(entity);
            ENTITY_EXECUTE_METHOD.invoke(entityScheduler, plugin, task, null, 1L);
        } catch (Exception e) {
            e.printStackTrace();
            fallbackScheduler().runTask(plugin, entity, task);
        }
    }
    
    @Override
    public void runTask(JavaPlugin plugin, Location location, Runnable task) {
        if (!FOLIA_AVAILABLE) {
            fallbackScheduler().runTask(plugin, location, task);
            return;
        }
        
        try {
            boolean isOwned = (boolean) IS_OWNED_BY_CURRENT_REGION_METHOD.invoke(null, location);
            if (isOwned) {
                try {
                    task.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return;
            }
            
            Object regionScheduler = GET_REGION_SCHEDULER_METHOD.invoke(Bukkit.getServer());
            REGION_EXECUTE_METHOD.invoke(regionScheduler, plugin, location, task);
        } catch (Exception e) {
            e.printStackTrace();
            fallbackScheduler().runTask(plugin, location, task);
        }
    }
    
    @Override
    public void runTaskAsynchronously(JavaPlugin plugin, Runnable task) {
        if (!FOLIA_AVAILABLE) {
            fallbackScheduler().runTaskAsynchronously(plugin, task);
            return;
        }
        
        try {
            Object asyncScheduler = GET_ASYNC_SCHEDULER_METHOD.invoke(Bukkit.getServer());
            ASYNC_RUN_NOW_METHOD.invoke(asyncScheduler, plugin, (java.util.function.Consumer<Object>) scheduledTask -> task.run());
        } catch (Exception e) {
            e.printStackTrace();
            fallbackScheduler().runTaskAsynchronously(plugin, task);
        }
    }
    
    @Override
    public void runTaskLater(JavaPlugin plugin, Runnable task, long delayTicks) {
        if (!FOLIA_AVAILABLE) {
            fallbackScheduler().runTaskLater(plugin, task, delayTicks);
            return;
        }
        
        try {
            Object globalScheduler = GET_GLOBAL_REGION_SCHEDULER_METHOD.invoke(Bukkit.getServer());
            
            Method globalRunDelayed = globalScheduler.getClass().getMethod("runDelayed",
                    org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class);
            globalRunDelayed.invoke(globalScheduler, plugin,
                    (java.util.function.Consumer<Object>) scheduledTask -> task.run(),
                    delayTicks);
        } catch (Exception e) {
            e.printStackTrace();
            fallbackScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }
    
    @Override
    public void runTaskLater(JavaPlugin plugin, Entity entity, Runnable task, long delayTicks) {
        if (!FOLIA_AVAILABLE) {
            fallbackScheduler().runTaskLater(plugin, entity, task, delayTicks);
            return;
        }
        
        try {
            Object entityScheduler = ENTITY_GET_SCHEDULER_METHOD.invoke(entity);
            ENTITY_RUN_DELAYED_METHOD.invoke(entityScheduler, plugin, 
                    (java.util.function.Consumer<Object>) scheduledTask -> task.run(), null, delayTicks);
        } catch (Exception e) {
            e.printStackTrace();
            fallbackScheduler().runTaskLater(plugin, entity, task, delayTicks);
        }
    }
    
    @Override
    public void runTaskLater(JavaPlugin plugin, Location location, Runnable task, long delayTicks) {
        if (!FOLIA_AVAILABLE) {
            fallbackScheduler().runTaskLater(plugin, location, task, delayTicks);
            return;
        }
        
        try {
            Object regionScheduler = GET_REGION_SCHEDULER_METHOD.invoke(Bukkit.getServer());
            REGION_RUN_DELAYED_METHOD.invoke(regionScheduler, plugin, location, 
                    (java.util.function.Consumer<Object>) scheduledTask -> task.run(), delayTicks);
        } catch (Exception e) {
            e.printStackTrace();
            fallbackScheduler().runTaskLater(plugin, location, task, delayTicks);
        }
    }
    
    @Override
    public boolean isOnMainThread() {
        if (!FOLIA_AVAILABLE) {
            return fallbackScheduler().isOnMainThread();
        }
        // On Folia, there is no single main thread; use Bukkit's primary thread check,
        // which returns true on region/global region threads and false on async threads.
        return Bukkit.isPrimaryThread();
    }
    
    /**
     * Gets the fallback scheduler adapter.
     *
     * @return The fallback scheduler adapter
     */
    private SchedulerAdapter fallbackScheduler() {
        return new BukkitSchedulerAdapter();
    }
}
