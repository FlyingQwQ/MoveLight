package top.kuoer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;

public class MoveLight extends JavaPlugin implements Listener {

    private TaskTimer taskTimer;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();

        this.taskTimer = new TaskTimer(this);
        this.taskTimer.runTaskTimer(this, 0, getConfig().getInt("refresh"));
        Bukkit.getConsoleSender().sendMessage("§8[§aMoveLight§8] §a移动光源加载成功");
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("§8[§aMoveLight§8] §6正在卸载移动光源");

        this.taskTimer.cancel();
        this.taskTimer.removeAllPlayerLight();

        Bukkit.getConsoleSender().sendMessage("§8[§aMoveLight§8] §a移动光源卸载完成");
    }




    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        this.taskTimer.removePlayerLight(e.getPlayer());
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length < 1) {
            this.showHelp(sender);
        } else {
            if(args[0].equals("help")) {
                this.showHelp(sender);
            } else if(args[0].equals("reload")) {
                this.reload(sender);
            } else if(args[0].equals("toggle")) {
                this.toggle(sender);
            }
        }

        return true;
    }

    public void showHelp(CommandSender sender) {
        if(!sender.hasPermission("movelight.help")) {
            sender.sendMessage("§8[§aMoveLight§8] §c你没有权限使用该命令");
            return;
        }

        sender.sendMessage("");
        sender.sendMessage(" §2§lMoveLight 移动光源");
        sender.sendMessage("");
        sender.sendMessage(" §7§l· §a/movel reload §6§l- §7重载插件");
        sender.sendMessage(" §7§l· §a/movel toggle §6§l- §7开关移动光源");
        sender.sendMessage("");
    }

    public void reload(CommandSender sender) {
        if(!sender.hasPermission("movelight.reload")) {
            sender.sendMessage("§8[§aMoveLight§8] §c你没有权限使用该命令");
            return;
        }

        saveDefaultConfig();
        reloadConfig();
        this.taskTimer.cancel();
        this.taskTimer.removeAllPlayerLight();
        this.taskTimer = new TaskTimer(this);
        this.taskTimer.runTaskTimer(this, 0, getConfig().getInt("refresh"));
        sender.sendMessage("§8[§aMoveLight§8] §a重载完成");
    }

    public void toggle(CommandSender sender) {
        if(!sender.hasPermission("movelight.toggle")) {
            sender.sendMessage("§8[§aMoveLight§8] §c你没有权限使用该命令");
            return;
        }

        if(getConfig().getBoolean("enable")) {
            getConfig().set("enable", false);
            sender.sendMessage("§8[§aMoveLight§8] §6已经关闭移动光源");
        } else {
            getConfig().set("enable", true);
            sender.sendMessage("§8[§aMoveLight§8] §6已经开启移动光源");
        }
        saveConfig();
        this.taskTimer.cancel();
        this.taskTimer.removeAllPlayerLight();
        this.taskTimer = new TaskTimer(this);
        this.taskTimer.runTaskTimer(this, 0, getConfig().getInt("refresh"));
    }

}
