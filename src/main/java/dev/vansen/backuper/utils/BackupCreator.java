package dev.vansen.backuper.utils;

import dev.vansen.backuper.Backuper;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.bukkit.Bukkit;
import org.xerial.snappy.SnappyOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

public class BackupCreator {

    public static Runnable createBackup() {
        return () -> {
            File backupDir = new File("backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String backupName = "backup-" + System.currentTimeMillis() + ".tar.sz";
            File backupFile = new File(backupDir, backupName);

            try (OutputStream fileOutputStream = Files.newOutputStream(backupFile.toPath());
                 SnappyOutputStream snappyOutputStream = new SnappyOutputStream(fileOutputStream);
                 TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(snappyOutputStream)) {

                Predicate<Path> isExcluded = path -> Backuper.getInstance().getConfig().getStringList("blacklisted_files")
                        .stream()
                        .noneMatch(path.toString()::contains);

                Files.walk(Paths.get("."))
                        .filter(Files::isRegularFile)
                        .filter(isExcluded)
                        .forEach(path -> {
                            try {
                                String entryName = path.toString().substring(Paths.get(".").toString().length() + 1);
                                TarArchiveEntry tarEntry = new TarArchiveEntry(path.toFile(), entryName);
                                tarOutputStream.putArchiveEntry(tarEntry);
                                Files.copy(path, tarOutputStream);
                                tarOutputStream.closeArchiveEntry();
                                System.out.println("Successfully backed up: " + path);
                            } catch (final IOException | IllegalArgumentException ignored) {

                            }
                        });

                tarOutputStream.finish();
                System.out.println("Backup created successfully: " + backupFile.getAbsolutePath());

                Bukkit.getScheduler().runTaskLater(Backuper.getInstance(), () -> {
                    if (backupFile.exists()) {
                        backupFile.delete();
                    }
                }, 20L * 60 * 60 * 3); // 3 hours

            } catch (final IOException ignored) {

            }
        };
    }
}