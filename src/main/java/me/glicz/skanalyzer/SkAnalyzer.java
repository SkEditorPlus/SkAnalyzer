package me.glicz.skanalyzer;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.google.common.base.Preconditions;
import me.glicz.skanalyzer.loader.AddonsLoader;
import me.glicz.skanalyzer.mockbukkit.AnalyzerServer;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

public class SkAnalyzer {
    private static SkAnalyzer instance;
    private final AnalyzerServer server;
    private final Logger logger;

    private SkAnalyzer(List<String> args) {
        Preconditions.checkArgument(instance == null, "SkAnalyzer instance is already set!");
        instance = this;

        logger = LogManager.getLogger(args.contains("--enablePlainLogger") ? "PlainLogger" : "SkAnalyzer");

        System.out.printf("SkAnalyzer v%s - simple Skript parser. Created by Glicz.%n", getClass().getPackage().getSpecificationVersion());
        extractEmbeddedAddons();
        logger.info("Enabling...");

        server = MockBukkit.mock(new AnalyzerServer());
        AddonsLoader.loadAddons();
        AddonsLoader.getMockSkriptBridge().parseArgs(args);

        server.startTicking();
        logger.info("Successfully enabled. Have fun!");

        startReadingInput();
    }

    public static void main(String[] args) {
        new SkAnalyzer(List.of(args));
    }

    public static SkAnalyzer get() {
        return instance;
    }

    public AnalyzerServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    private void startReadingInput() {
        Thread thread = new Thread() {
            private final Scanner scanner = new Scanner(System.in);

            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    if (scanner.hasNext()) {
                        String line = scanner.nextLine();
                        if (line != null)
                            AddonsLoader.getMockSkriptBridge().parseScript(line);
                    }
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    private void extractEmbeddedAddons() {
        logger.info("Extracting embedded addons...");

        try (InputStream embeddedJar = getClass().getClassLoader().getResourceAsStream("MockSkript.jar.embedded")) {
            Preconditions.checkArgument(embeddedJar != null, "Couldn't find embedded MockSkript.jar");
            FileUtils.copyInputStreamToFile(embeddedJar, new File(AddonsLoader.ADDONS, "MockSkript.jar"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (InputStream embeddedJar = getClass().getClassLoader().getResourceAsStream("MockSkriptBridge.jar.embedded")) {
            Preconditions.checkArgument(embeddedJar != null, "Couldn't find embedded MockSkriptBridge.jar");
            FileUtils.copyInputStreamToFile(embeddedJar, new File(AddonsLoader.ADDONS, "MockSkriptBridge.jar"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.info("Successfully extracted embedded addons!");
    }
}
