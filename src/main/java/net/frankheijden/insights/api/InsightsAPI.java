package net.frankheijden.insights.api;

import net.frankheijden.insights.Insights;
import net.frankheijden.insights.api.builders.ScanTaskBuilder;
import net.frankheijden.insights.api.entities.ChunkLocation;
import net.frankheijden.insights.api.enums.ScanType;
import net.frankheijden.insights.api.events.ScanCompleteEvent;
import net.frankheijden.insights.hooks.HookManager;
import net.frankheijden.insights.tasks.LoadChunksTask;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class InsightsAPI {
    private Insights instance = null;

    /**
     * Initiates a new InsightsAPI instance.
     */
    public InsightsAPI() {}

    /**
     * Gets the instance of Insights.
     *
     * @return Insights Main class
     */
    public Insights getInstance() {
        if (instance == null) {
            instance = Insights.getInstance();
        }
        return instance;
    }

    /**
     * Scans chunks for all Materials and EntityTypes.
     *
     * @param world World in which we should scan
     * @param chunkLocations List of ChunkLocation to scan in
     * @return CompletableFuture which supplies the ScanCompleteEvent
     */
    public CompletableFuture<ScanCompleteEvent> scan(World world, List<ChunkLocation> chunkLocations) {
        return scan(world, new LinkedList<>(chunkLocations), null, null);
    }

    /**
     * Scans chunks for all Materials and EntityTypes.
     *
     * @param world World in which we should scan
     * @param chunkLocations Queue of ChunkLocation to scan in
     * @return CompletableFuture which supplies the ScanCompleteEvent
     */
    public CompletableFuture<ScanCompleteEvent> scan(World world, Queue<ChunkLocation> chunkLocations) {
        return scan(world, chunkLocations, null, null);
    }

    /**
     * Scans a single chunk for a single material
     *
     * @param chunk Chunk to scan
     * @param material Material to scan for
     * @return CompletableFuture which supplies the ScanCompleteEvent
     */
    public CompletableFuture<ScanCompleteEvent> scanSingleChunk(Chunk chunk, Material material) {
        return scan(chunk.getWorld(), new LinkedList<>(Collections.singletonList(new ChunkLocation(chunk))), Collections.singletonList(material), null);
    }

    /**
     * Scans a single chunk for a single material
     *
     * @param chunk Chunk to scan
     * @param material Material to scan for
     * @param debug Boolean if we should debug this scan
     * @return CompletableFuture which supplies the ScanCompleteEvent
     */
    public CompletableFuture<ScanCompleteEvent> scanSingleChunk(Chunk chunk, Material material, boolean debug) {
        return scan(chunk.getWorld(), new LinkedList<>(Collections.singletonList(new ChunkLocation(chunk))), Collections.singletonList(material), null, debug);
    }

    /**
     * Scans a single chunk for a single entity
     *
     * @param chunk Chunk to scan
     * @param entityType Entity to scan for
     * @return CompletableFuture which supplies the ScanCompleteEvent
     */
    public CompletableFuture<ScanCompleteEvent> scanSingleChunk(Chunk chunk, EntityType entityType) {
        return scan(chunk.getWorld(), new LinkedList<>(Collections.singletonList(new ChunkLocation(chunk))), null, Collections.singletonList(entityType));
    }

    /**
     * Scans a single chunk for a single entity
     *
     * @param chunk Chunk to scan
     * @param entityType Entity to scan for
     * @param debug Boolean if we should debug this scan
     * @return CompletableFuture which supplies the ScanCompleteEvent
     */
    public CompletableFuture<ScanCompleteEvent> scanSingleChunk(Chunk chunk, EntityType entityType, boolean debug) {
        return scan(chunk.getWorld(), new LinkedList<>(Collections.singletonList(new ChunkLocation(chunk))), null, Collections.singletonList(entityType), debug);
    }

    /**
     * Scans chunks for Materials and EntityTypes.
     *
     * @param world World in which we should scan
     * @param chunkLocations List of ChunkLocation to scan in
     * @param materials List of Material to scan for, null if none
     * @param entityTypes List of EntityType to scan for, null if none
     * @return CompletableFuture which supplies the ScanCompleteEvent
     */
    public CompletableFuture<ScanCompleteEvent> scan(World world, Queue<ChunkLocation> chunkLocations, List<Material> materials, List<EntityType> entityTypes) {
        return scan(world, chunkLocations, materials, entityTypes, false, true);
    }

    /**
     * Scans chunks for Materials and EntityTypes.
     *
     * @param world World in which we should scan
     * @param chunkLocations List of ChunkLocation to scan in
     * @param materials List of Material to scan for, null if none
     * @param entityTypes List of EntityType to scan for, null if none
     * @param debug Boolean if we should debug this scan
     * @return CompletableFuture which supplies the ScanCompleteEvent
     */
    public CompletableFuture<ScanCompleteEvent> scan(World world, Queue<ChunkLocation> chunkLocations, List<Material> materials, List<EntityType> entityTypes, boolean debug) {
        return scan(world, chunkLocations, materials, entityTypes, false, debug);
    }

    /**
     * Scans chunks for Materials and EntityTypes.
     *
     * @param world World in which we should scan
     * @param chunkLocations List of ChunkLocation to scan in
     * @param materials List of Material to scan for, null if none
     * @param entityTypes List of EntityType to scan for, null if none
     * @param saveWorld Boolean if we should save the world when the chunk generations have been completed (plugin induced saveworld)
     * @param debug Boolean if we should debug this scan
     * @return CompletableFuture which supplies the ScanCompleteEvent
     */
    public CompletableFuture<ScanCompleteEvent> scan(World world, Queue<ChunkLocation> chunkLocations, List<Material> materials, List<EntityType> entityTypes, boolean saveWorld, boolean debug) {
        return CompletableFuture.supplyAsync(() -> {
            final Object LOCK = new Object();

            String k = RandomStringUtils.randomAlphanumeric(16);
            while (getInstance().getCountsMap().containsKey(k)) {
                k = RandomStringUtils.randomAlphanumeric(16);
            }
            final String key = k;

            ScanTaskBuilder builder = new ScanTaskBuilder(getInstance(), ScanType.CUSTOM, world, chunkLocations)
                    .setMaterials(materials)
                    .setEntityTypes(entityTypes)
                    .setSaveWorld(saveWorld)
                    .setDebug(debug)
                    .setScanCompleteEventListener((event) -> {
                        getInstance().getCountsMap().put(key, event);

                        synchronized (LOCK) {
                            LOCK.notify();
                        }
                    });

            LoadChunksTask loadChunksTask = builder.build();
            loadChunksTask.start(System.currentTimeMillis());

            synchronized (LOCK) {
                try {
                    LOCK.wait(10000000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }

            return getInstance().getCountsMap().get(key);
        });
    }

    /**
     * Toggles realtime checking for the UUID specified.
     * NOTE: To use realtime checking, the user still needs the permission 'insights.check.realtime'.
     *
     * @param uuid UUID of player
     */
    public void toggleCheck(UUID uuid) {
        getInstance().getSqLite().toggleRealtimeCheck(uuid);
    }

    /**
     * Enables or disabled realtime checking for the UUID specified.
     * NOTE: To use realtime checking, the user still needs the permission 'insights.check.realtime'.
     *
     * @param uuid UUID of player
     * @param enabled boolean enabled
     */
    public void setToggleCheck(UUID uuid, boolean enabled) {
        getInstance().getSqLite().setRealtimeCheck(uuid, enabled);
    }

    /**
     * Checks if the player specified is scanning for chunks.
     *
     * @param uuid UUID of player
     * @return boolean scanning
     */
    public boolean isScanningChunks(UUID uuid) {
        return getInstance().getPlayerScanTasks().containsKey(uuid);
    }

    /**
     * Gets a percentage between 0 and 1 of the progress of scanning chunks,
     * returns null if the player is not scanning chunks.
     *
     * @param uuid UUID of player
     * @return double progress, or null if no ScanTask.
     */
    public Double getScanProgress(UUID uuid) {
        LoadChunksTask loadChunksTask = getInstance().getPlayerScanTasks().get(uuid);
        if (loadChunksTask != null) {
            double total = loadChunksTask.getTotalChunks();
            double done = loadChunksTask.getScanChunksTask().getChunksDone();
            double progress = done/total;
            if (progress < 0) {
                progress = 0;
            } else if (progress > 1) {
                progress = 1;
            }
            return progress;
        }
        return null;
    }

    /**
     * Gets the time elapsed for the current scan of a player
     *
     * @param uuid UUID of player
     * @return String time elapsed, or null if no ScanTask.
     */
    public String getTimeElapsedOfScan(UUID uuid) {
        LoadChunksTask loadChunksTask = getInstance().getPlayerScanTasks().get(uuid);
        if (loadChunksTask != null) {
            return getInstance().getUtils().getDHMS(loadChunksTask.getStartTime());
        }
        return null;
    }

    /**
     * Retrieves the HookManager instance
     *
     * @return HookManager instance
     */
    public HookManager getHookManager() {
        return getInstance().getHookManager();
    }
}
