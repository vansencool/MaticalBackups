package dev.vansen.backuper;

import dev.vansen.backuper.commands.ExtractCommand;
import dev.vansen.backuper.utils.BackupCreator;
import dev.vansen.backuper.utils.LogsDeleter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Backuper extends JavaPlugin {
    private static Backuper instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        LogsDeleter.deleteLogsFolder();
        getCommand("extract").setExecutor(new ExtractCommand());

        BackupCreator.deleteOldBackups();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, BackupCreator.createBackup(), 0L, 20L * 60 * 60); // Every hour
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, BackupCreator::deleteOldBackups, 0L, 20L * 60 * 60); // Every hour
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
    }

    public static Backuper getInstance() {
        return instance;
    }
}