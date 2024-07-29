package dev.vansen.backuper.utils;

import dev.vansen.backuper.MaticalBackups;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupCreator {

    public static Runnable createBackup() {
        return () -> {
            File backupDir = new File("backups");
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                MaticalBackups.getInstance().getLogger().severe("Failed to create backups directory.");
                return;
            }

            File dataFolder = MaticalBackups.getInstance().getDataFolder();
            File countingFile = new File(dataFolder, "counting.yml");
            FileConfiguration countingConfig = YamlConfiguration.loadConfiguration(countingFile);

            int currentCount = countingConfig.getInt("current_count", 1);
            File backupFile = new File(backupDir, "backup-" + currentCount + ".snappy");

            MaticalBackups.getInstance().getLogger().info("Creating backup... This may take a while");

            long startTime = System.currentTimeMillis();
            try (FileOutputStream fos = new FileOutputStream(backupFile);
                 SnappyOutputStream sos = new SnappyOutputStream(fos);
                 ZipOutputStream zos = new ZipOutputStream(sos)) {

                Predicate<Path> isExcluded = path -> MaticalBackups.getInstance().getConfig().getStringList("blacklisted_files")
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

                MaticalBackups.getInstance().getLogger().info("----------------------------------------------------------------------");
                MaticalBackups.getInstance().getLogger().info("Backup created successfully: " + backupFile.getAbsolutePath());
                MaticalBackups.getInstance().getLogger().info("Took " + durationInSeconds + " seconds (" + duration + " milliseconds)");
                MaticalBackups.getInstance().getLogger().info("----------------------------------------------------------------------");

                countingConfig.set("current_count", currentCount + 1);
                countingConfig.save(countingFile);

                File backupTimestampsFile = new File(dataFolder, "backup_timestamps.yml");
                FileConfiguration backupTimestampsConfig = YamlConfiguration.loadConfiguration(backupTimestampsFile);
                backupTimestampsConfig.set("backup-" + currentCount, new SimpleDateFormat("yy-MM-dd HH:mm:ss").format(new Date()));
                backupTimestampsConfig.save(backupTimestampsFile);

            } catch (final IOException e) {
                MaticalBackups.getInstance().getLogger().severe("Error creating backup file: " + backupFile.getAbsolutePath());
                e.printStackTrace();
            }
        };
    }

    public static void deleteOldBackups() {
        File dataFolder = MaticalBackups.getInstance().getDataFolder();
        File backupDir = new File("backups");
        File backupTimestampsFile = new File(dataFolder, "backup_timestamps.yml");
        FileConfiguration backupTimestampsConfig = YamlConfiguration.loadConfiguration(backupTimestampsFile);

        long currentTime = System.currentTimeMillis();
        long expirationTime = 3 * 60 * 60 * 1000; // 3 hours in milliseconds

        for (String key : backupTimestampsConfig.getKeys(false)) {
            try {
                Date timestamp = new SimpleDateFormat("yy-MM-dd HH:mm:ss").parse(backupTimestampsConfig.getString(key));
                long timestampMillis = timestamp.getTime();
                if (currentTime - timestampMillis > expirationTime) {
                    File backupFile = new File(backupDir, key + ".snappy");
                    if (backupFile.exists() && backupFile.delete()) {
                        backupTimestampsConfig.set(key, null);
                        MaticalBackups.getInstance().getLogger().info("Deleted old backup: " + backupFile.getAbsolutePath());
                    }
                }
            } catch (final Exception e) {
                MaticalBackups.getInstance().getLogger().severe("Error parsing timestamp for backup: " + key);
                e.printStackTrace();
            }
        }

        try {
            backupTimestampsConfig.save(backupTimestampsFile);
        } catch (final IOException e) {
            MaticalBackups.getInstance().getLogger().severe("Error saving backup timestamps file.");
            e.printStackTrace();
        }
    }
}