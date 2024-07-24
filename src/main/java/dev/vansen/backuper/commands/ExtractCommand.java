package dev.vansen.backuper.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;
import org.xerial.snappy.SnappyInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ExtractCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("This command can only be run from the console.");
            return false;
        }

        if (args.length != 1) {
            sender.sendMessage("Usage: /extract <backup_num>");
            return false;
        }

        String backupNum = args[0];
        File backupDir = new File("backups");
        File backupFile = new File(backupDir, "backup-" + backupNum + ".snappy");

        if (!backupFile.exists()) {
            sender.sendMessage("Backup file not found.");
            return false;
        }

        File extractDir = new File("restored_backups", "backup-" + backupNum);
        if (!extractDir.exists()) {
            if (!extractDir.mkdirs()) {
                sender.sendMessage("Failed to create directory for extraction.");
                return false;
            }
        }

        sender.sendMessage("Restoring backup... This may take some time");
        long startTime = System.currentTimeMillis();
        try (FileInputStream fis = new FileInputStream(backupFile);
             SnappyInputStream sis = new SnappyInputStream(fis);
             ZipInputStream zis = new ZipInputStream(sis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(extractDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!outFile.mkdirs()) {
                        sender.sendMessage("Failed to create directory: " + outFile.getAbsolutePath());
                    }
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            long durationInSeconds = duration / 1000;

            sender.sendMessage("Backup extracted successfully.");
            sender.sendMessage("Took " + durationInSeconds + " seconds (" + duration + " milliseconds)");
        } catch (final IOException e) {
            sender.sendMessage("Error during extraction: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}