package dev.vansen.backuper.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class LogsDeleter {

    public static void deleteLogsFolder() {
        File logsFolder = new File("logs");
        if (logsFolder.exists()) {
            try {
                Files.walk(logsFolder.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}