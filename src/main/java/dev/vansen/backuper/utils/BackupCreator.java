package dev.vansen.backuper.utils;

import dev.vansen.backuper.Backuper;
import org.bukkit.Bukkit;
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
            String backupName = "backup-" + currentCount + ".snappy";
            File backupFile = new File(backupDir, backupName);

            Backuper.getInstance().getLogger().info("Creating backup... This may take a while");
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

                                byte[] buffer = new byte[1024];
                                int length;
                                while ((length = fis.read(buffer)) > 0) {
                                    zos.write(buffer, 0, length);
                                }
                                zos.closeEntry();
                            } catch (final IOException | IllegalArgumentException ignored) {

                            }
                        });

                Backuper.getInstance().getLogger().info("Backup created successfully: " + backupFile.getAbsolutePath());

                countingConfig.set("current_count", currentCount + 1);
                countingConfig.save(countingFile);

                Bukkit.getScheduler().runTaskLater(Backuper.getInstance(), () -> {
                    if (backupFile.exists()) {
                        backupFile.delete();
                    }
                }, 20L * 60 * 60 * 3); // 3 hours

            } catch (final IOException e) {
                Backuper.getInstance().getLogger().severe("Error creating backup file: " + backupFile.getAbsolutePath());
                e.printStackTrace();
            }
        };
    }
}