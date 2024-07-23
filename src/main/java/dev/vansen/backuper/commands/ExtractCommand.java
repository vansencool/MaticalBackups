package dev.vansen.backuper.commands;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;
import org.xerial.snappy.SnappyInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        File backupFile = new File(backupDir, "backup-" + backupNum + ".tar.sz");

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
        try (FileInputStream fis = new FileInputStream(backupFile);
             SnappyInputStream snappyInputStream = new SnappyInputStream(fis);
             TarArchiveInputStream tarInputStream = new TarArchiveInputStream(snappyInputStream)) {

            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextTarEntry()) != null) {
                Path outputPath = Paths.get(extractDir.getAbsolutePath(), entry.getName());
                System.out.println("Processing: " + entry.getName()); // Add this line

                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(outputPath.getParent());
                    Files.copy(tarInputStream, outputPath);
                    System.out.println("Extracted: " + entry.getName()); // Add this line
                }
            }

            sender.sendMessage("Backup extracted successfully.");
        } catch (final IOException | IllegalArgumentException e) {
            sender.sendMessage("Error during extraction: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}