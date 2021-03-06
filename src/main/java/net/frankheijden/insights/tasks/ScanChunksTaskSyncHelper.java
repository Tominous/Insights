package net.frankheijden.insights.tasks;

import net.frankheijden.insights.Insights;
import net.frankheijden.insights.api.entities.ScanOptions;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScanChunksTaskSyncHelper implements Runnable {
    private Insights plugin;
    private ScanOptions scanOptions;
    private ScanChunksTask scanChunksTask;
    private final Queue<Chunk> chunks;

    private int taskID;
    private int counter;

    public ScanChunksTaskSyncHelper(Insights plugin, ScanOptions scanOptions, ScanChunksTask scanChunksTask) {
        this.plugin = plugin;
        this.scanOptions = scanOptions;
        this.scanChunksTask = scanChunksTask;
        this.chunks = new ConcurrentLinkedQueue<>();
    }

    public void start() {
        this.taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, 1);
    }

    public void stop() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> Bukkit.getScheduler().cancelTask(taskID), 20);
    }

    public void addChunk(Chunk chunk) {
        this.chunks.add(chunk);
    }

    @Override
    public void run() {
        while (!chunks.isEmpty()) {
            scanChunksTask.addBlockStates(chunks.poll().getTileEntities());
            counter++;
        }

        if (counter == scanOptions.getChunkCount()) {
            stop();
        }
    }
}
