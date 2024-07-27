package dev.vansen.backuper.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class LogsDeleter {

    public static void deleteLogsFolder() {
        File logsFolder = new File("logs");
        if (logsFolder.exists()) {
            try {
                Files.walk(logsFolder.toPath())
                        .filter(path -> path.toFile().isFile() && path.toFile().getName().endsWith(".gz"))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (final IOException e) {
                                e.printStackTrace();
                            }
                        });
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }
}