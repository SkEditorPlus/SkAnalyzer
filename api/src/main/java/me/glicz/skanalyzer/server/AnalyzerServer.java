package me.glicz.skanalyzer.server;

import lombok.Getter;
import me.glicz.skanalyzer.SkAnalyzer;
import me.glicz.skanalyzer.loader.AddonsLoader;
import me.glicz.skanalyzer.server.command.AnalyzerConsoleCommandSender;
import me.glicz.skanalyzer.server.potion.AnalyzerPotionBrewer;
import me.glicz.skanalyzer.server.scheduler.AnalyzerScheduler;
import me.glicz.skanalyzer.server.structure.AnalyzerStructureManager;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.BlockData;
import org.bukkit.loot.LootTable;
import org.jspecify.annotations.Nullable;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock;
import org.mockbukkit.mockbukkit.scheduler.paper.FoliaAsyncScheduler;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.Logger;

@Getter
public class AnalyzerServer extends ServerMock {
    private final AnalyzerPotionBrewer potionBrewer = new AnalyzerPotionBrewer();
    private final AnalyzerScheduler scheduler = new AnalyzerScheduler();
    private final FoliaAsyncScheduler foliaAsyncScheduler = new FoliaAsyncScheduler(scheduler);
    private final AnalyzerStructureManager structureManager = new AnalyzerStructureManager();
    private final AnalyzerUnsafeValues unsafe = new AnalyzerUnsafeValues();
    private final Logger logger = Logger.getLogger("Server");
    private final SkAnalyzer skAnalyzer;
    private final AddonsLoader addonsLoader;
    private @Nullable AnalyzerConsoleCommandSender consoleSender;

    public AnalyzerServer(SkAnalyzer skAnalyzer) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        this.skAnalyzer = skAnalyzer;
        this.addonsLoader = new AddonsLoader(skAnalyzer, this);
    }

    @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
    public void startTicking() {
        try {
            while (true) {
                long start = System.currentTimeMillis();

                ((BukkitSchedulerMock) Bukkit.getScheduler()).performOneTick();

                long end = System.currentTimeMillis();
                long duration = end - start;

                long sleepTime = Ticks.SINGLE_TICK_DURATION_MS - duration;
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ex) {
                        skAnalyzer.getLogger().atError()
                                .setCause(ex)
                                .log("Something went wrong while trying to wait until next tick");
                    }
                }
            }
        } catch (Exception ex) {
            skAnalyzer.getLogger().atError()
                    .setCause(ex)
                    .log("Something went wrong while trying to tick");
        }
    }

    @Override
    public String getName() {
        return "SkAnalyzer";
    }

    public AnalyzerConsoleCommandSender getConsoleSender() {
        return consoleSender != null ? consoleSender : (consoleSender = new AnalyzerConsoleCommandSender());
    }

    @Override
    public BlockData createBlockData(String data) {
        String rawMaterial = (data.indexOf('[') == -1)
                ? data
                : data.substring(0, data.indexOf('['));
        Material material = Material.matchMaterial(rawMaterial);
        if (material == null) {
            throw new IllegalArgumentException();
        }
        return createBlockData(material);
    }

    @Override
    public BlockData createBlockData(Material material, String data) {
        return createBlockData(material);
    }

    @Override
    public @Nullable LootTable getLootTable(NamespacedKey key) {
        return null;
    }

    @Override
    public boolean isStopping() {
        return false;
    }
}
