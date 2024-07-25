package dev.vansen.backuper.utils;

import dev.vansen.backuper.Backuper;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.xerial.snappy.SnappyOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupCreator {

    public static Runnable createBackup() {
        return () -> {
            File backupDir = new File("backups");
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                Backuper.getInstance().getLogger().severe("Failed to create backups directory.");
                return;
            }

            File dataFolder = Backuper.getInstance().getDataFolder();
            File countingFile = new File(dataFolder, "counting.yml");
            FileConfiguration countingConfig = YamlConfiguration.loadConfiguration(countingFile);

            int currentCount = countingConfig.getInt("current_count", 1);
            File backupFile = new File(backupDir, "backup-" + currentCount + ".snappy");

            Backuper.getInstance().getLogger().info("Creating backup... This may take a while");

            long startTime = System.currentTimeMillis();
            try (FileOutputStream fos = new FileOutputStream(backupFile);
                 SnappyOutputStream sos = new SnappyOutputStream(fos);
                 ZipOutputStream zos = new ZipOutputStream(sos)) {

                Predicate<Path> isExcluded = path -> Backuper.getInstance().getConfig().getStringList("blacklisted_files")
                        .stream()
                        .noneMatch(path.toString()::contains);

                Files.walk(Paths.get("."))
                        .filter(Files::isRegularFile)
                        .filter(isExcluded)
                        .forEach(path -> {
                            try (FileInputStream fis = new FileInputStream(path.toFile())) {
                                ZipEntry zipEntry = new ZipEntry(path.toString());
                                zos.putNextEntry(zipEntry);

                                byte[] buffer = new byte[8192]; // since I/O can be very large, so 8192 seems reasonable
                                int length;
                                while ((length = fis.read(buffer)) > 0) {
                                    zos.write(buffer, 0, length);
                                }
                                zos.closeEntry();
                            } catch (final IOException | IllegalArgumentException ignored) {

                            }
                        });

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                long durationInSeconds = duration / 1000;

                Backuper.getInstance().getLogger().info("Backup created successfully: " + backupFile.getAbsolutePath());
                Backuper.getInstance().getLogger().info("Took " + durationInSeconds + " seconds (" + duration + " milliseconds)");

                countingConfig.set("current_count", currentCount + 1);
                countingConfig.save(countingFile);

                File backupTimestampsFile = new File(dataFolder, "backup_timestamps.yml");
                FileConfiguration backupTimestampsConfig = YamlConfiguration.loadConfiguration(backupTimestampsFile);
                backupTimestampsConfig.set("backup-" + currentCount, System.currentTimeMillis());
                backupTimestampsConfig.save(backupTimestampsFile);

            } catch (final IOException e) {
                Backuper.getInstance().getLogger().severe("Error creating backup file: " + backupFile.getAbsolutePath());
                e.printStackTrace();
            }
        };
    }

    public static void deleteOldBackups() {
        File dataFolder = Backuper.getInstance().getDataFolder();
        File backupDir = new File("backups");
        File backupTimestampsFile = new File(dataFolder, "backup_timestamps.yml");
        FileConfiguration backupTimestampsConfig = YamlConfiguration.loadConfiguration(backupTimestampsFile);

        long currentTime = System.currentTimeMillis();
        long expirationTime = 3 * 60 * 60 * 1000; // 3 hours in milliseconds

        for (String key : backupTimestampsConfig.getKeys(false)) {
            long timestamp = backupTimestampsConfig.getLong(key);
            if (currentTime - timestamp > expirationTime) {
                File backupFile = new File(backupDir, key + ".snappy");
                if (backupFile.exists() && backupFile.delete()) {
                    backupTimestampsConfig.set(key, null);
                    Backuper.getInstance().getLogger().info("Deleted old backup: " + backupFile.getAbsolutePath());
                }
            }
        }

        try {
            backupTimestampsConfig.save(backupTimestampsFile);
        } catch (final IOException e) {
            Backuper.getInstance().getLogger().severe("Error saving backup timestamps file.");
            e.printStackTrace();
        }
    }
}