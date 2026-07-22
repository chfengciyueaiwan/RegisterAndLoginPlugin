package chfeng.ciyueaiwan.registerAndLoginPlugin121;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegisterAndLoginPlugin121 extends JavaPlugin implements Listener, CommandExecutor {
    public static final String VERSION = "1.0.0";
    private final Map<UUID, String> accountPassword = new HashMap<>();
    private final Map<UUID, Boolean> isLoggedIn = new HashMap<>();
    private File accountFolder;

    @Override
    public void onEnable() {
        getLogger().info("§aRegister and Login Plugin v" + VERSION + " 已加载 [1.21系列]");
        accountFolder = new File(getDataFolder(), "accounts");
        if (!accountFolder.exists()) accountFolder.mkdirs();

        // 注册事件与指令
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("register").setExecutor(this);
        getCommand("login").setExecutor(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("§cRegister and Login Plugin 已卸载");
        accountPassword.clear();
        isLoggedIn.clear();
    }

    // 获取玩家账号文件
    private Path getPlayerFile(UUID uuid) {
        return new File(accountFolder, uuid + ".txt").toPath();
    }

    // 读取本地密码
    private String loadPassword(UUID uuid) {
        Path file = getPlayerFile(uuid);
        if (!Files.exists(file)) return null;
        try {
            return Files.readString(file).trim();
        } catch (IOException e) {
            getLogger().severe("读取账号文件异常:" + uuid);
            return null;
        }
    }

    // 保存密码到txt
    private void savePassword(UUID uuid, String pass) {
        try {
            Files.writeString(getPlayerFile(uuid), pass);
        } catch (IOException e) {
            getLogger().severe("保存账号文件异常:" + uuid);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c仅游戏内玩家可用该指令！");
            return true;
        }
        UUID uuid = player.getUniqueId();
        String savedPass = loadPassword(uuid);

        // /register <密码> <确认密码>
        if (cmd.getName().equalsIgnoreCase("register")) {
            if (savedPass != null) {
                player.sendMessage("§c你已经注册账号，请使用 /login <密码>");
                return true;
            }
            if (args.length != 2) {
                player.sendMessage("§e用法：/register 密码 确认密码");
                return true;
            }
            String p1 = args[0];
            String p2 = args[1];
            if (!p1.equals(p2)) {
                player.sendMessage("§c两次输入的密码不一致！");
                return true;
            }
            savePassword(uuid, p1);
            accountPassword.put(uuid, p1);
            isLoggedIn.put(uuid, true);
            player.sendMessage("§a注册成功！你已自动登录");
            return true;
        }

        // /login <密码>
        if (cmd.getName().equalsIgnoreCase("login")) {
            if (savedPass == null) {
                player.sendMessage("§c尚未注册！请输入 /register 密码 确认密码");
                return true;
            }
            if (isLoggedIn.getOrDefault(uuid, false)) {
                player.sendMessage("§a你已经处于登录状态！");
                return true;
            }
            if (args.length != 1) {
                player.sendMessage("§e用法：/login <密码>");
                return true;
            }
            if (savedPass.equals(args[0])) {
                isLoggedIn.put(uuid, true);
                player.sendMessage("§a登录成功！");
            } else {
                player.sendMessage("§c密码错误！");
            }
            return true;
        }
        return false;
    }

    // 进服重置登录状态
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        isLoggedIn.put(uuid, false);
    }

    // 退出置为未登录
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        isLoggedIn.put(uuid, false);
    }

    // 未登录禁止移动
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (!isLoggedIn.getOrDefault(uuid, false)) {
            e.setCancelled(true);
            String pass = loadPassword(uuid);
            if (pass == null) {
                p.sendMessage("§6请先注册！指令：/register 密码 确认密码");
            } else {
                p.sendMessage("§6请先登录！指令：/login 密码");
            }
        }
    }

    // 未登录禁止聊天
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (!isLoggedIn.getOrDefault(uuid, false)) {
            e.setCancelled(true);
            String pass = loadPassword(uuid);
            if (pass == null) {
                p.sendMessage("§6请先注册！指令：/register 密码 确认密码");
            } else {
                p.sendMessage("§6请先登录！指令：/login 密码");
            }
        }
    }
}