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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RegisterAndLoginPlugin121 extends JavaPlugin implements Listener, CommandExecutor {
    public static final String VERSION = "2.0.0";
    private final Map<UUID, String> accountPassword = new HashMap<>();
    private final Map<UUID, Boolean> isLoggedIn = new HashMap<>();
    private File accountFolder;

    // ====================== 【新增】SHA256哈希加密方法 ======================
    private String sha256Encrypt(String rawStr) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawStr.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexSb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexSb.append("0");
                hexSb.append(hex);
            }
            return hexSb.toString();
        } catch (NoSuchAlgorithmException e) {
            getLogger().severe("哈希加密失败");
            return null;
        }
    }

    // ====================== 【新增】IP查询获取地区 ======================
    private CompletableFuture<String> getIpCountry(String ipAddr) {
        return CompletableFuture.supplyAsync(() -> {
            if (ipAddr == null || ipAddr.isBlank() || ipAddr.equals("127.0.0.1")) return "本地内网";
            HttpURLConnection conn = null;
            try {
                // 使用更可靠的IP API（支持IPv6）
                URI uri = URI.create("https://ipapi.co/" + ipAddr + "/json/");
                URL url = uri.toURL();
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestProperty("User-Agent", "Minecraft/RegisterAndLogin");
                String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                // 解析country_code
                String flag = "\"country_code\":\"";
                int start = json.indexOf(flag);
                if (start == -1) {
                    // 尝试备用解析
                    flag = "\"country_name\":\"";
                    start = json.indexOf(flag);
                    if (start == -1) return "未知地区";
                }
                start += flag.length();
                int end = json.indexOf("\"", start);
                if (end == -1) return "未知地区";
                return json.substring(start, end);
            } catch (Exception e) {
                return "未知地区";
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    @Override
    public void onEnable() {
        getLogger().info("§aRegister and Login Plugin v" + VERSION);
        accountFolder = new File(getDataFolder(), "accounts");
        if (!accountFolder.exists()) {
            if (!accountFolder.mkdirs()) {
                getLogger().severe("无法创建账户文件夹！");
            }
        }

        // 注册事件与指令
        getServer().getPluginManager().registerEvents(this, this);

        // 确保指令注册（需要在plugin.yml中定义）
        if (getCommand("register") != null) {
            getCommand("register").setExecutor(this);
        } else {
            getLogger().warning("指令 /register 未在plugin.yml中注册");
        }

        if (getCommand("login") != null) {
            getCommand("login").setExecutor(this);
        } else {
            getLogger().warning("指令 /login 未在plugin.yml中注册");
        }
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

    // 读取本地【哈希密码】
    private String loadPassword(UUID uuid) {
        Path file = getPlayerFile(uuid);
        if (!Files.exists(file)) return null;
        try {
            String content = Files.readString(file).trim();
            // 分割格式：哈希值|玩家IP
            String[] splitData = content.split("\\|");
            return splitData[0];
        } catch (IOException e) {
            getLogger().severe("读取账号文件异常:" + uuid);
            return null;
        }
    }

    // 保存【哈希密码 + IP信息】写入文件
    private void savePassword(UUID uuid, String passHash, String ipInfo) {
        try {
            // 储存格式：密码哈希 | IP地址-归属地
            String saveText = passHash + "|" + ipInfo;
            Files.writeString(getPlayerFile(uuid), saveText);
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
        String savedPassHash = loadPassword(uuid);

        // /register <密码> <确认密码>
        if (cmd.getName().equalsIgnoreCase("register")) {
            if (savedPassHash != null) {
                player.sendMessage("§c你已经注册账号，请使用 /login <密码>");
                return true;
            }
            if (args.length != 2) {
                player.sendMessage("§e用法：/register 密码 确认密码");
                return true;
            }

            // 密码长度验证（增强安全性）
            if (args[0].length() < 4) {
                player.sendMessage("§c密码长度至少为4个字符！");
                return true;
            }

            String p1 = args[0];
            String p2 = args[1];
            if (!p1.equals(p2)) {
                player.sendMessage("§c两次输入的密码不一致！");
                return true;
            }

            // 加密明文密码
            String hash = sha256Encrypt(p1);
            if (hash == null) {
                player.sendMessage("§c密码加密失败，请稍后重试");
                return true;
            }

            String playerIp = player.getAddress().getAddress().getHostAddress();
            // 异步获取地区并保存数据
            getIpCountry(playerIp).thenAccept(country -> {
                String fullIpData = playerIp + "-" + country;
                savePassword(uuid, hash, fullIpData);
                getLogger().info("玩家 " + player.getName() + " 注册成功，IP: " + fullIpData);
            });

            accountPassword.put(uuid, hash);
            isLoggedIn.put(uuid, true);
            player.sendMessage("§a注册成功！密码已加密储存，你已自动登录");
            return true;
        }

        // /login <密码>
        if (cmd.getName().equalsIgnoreCase("login")) {
            if (savedPassHash == null) {
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

            // 把输入的明文加密比对哈希
            String inputHash = sha256Encrypt(args[0]);
            if (inputHash == null) {
                player.sendMessage("§c密码验证失败，请稍后重试");
                return true;
            }

            if (savedPassHash.equals(inputHash)) {
                isLoggedIn.put(uuid, true);
                player.sendMessage("§a登录成功！");
                getLogger().info("玩家 " + player.getName() + " 登录成功");
            } else {
                player.sendMessage("§c密码错误！");
                // 记录登录失败（防暴力破解）
                getLogger().warning("玩家 " + player.getName() + " 登录失败 - 密码错误");
            }
            return true;
        }
        return false;
    }

    // 进服重置登录状态
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        isLoggedIn.put(uuid, false);

        // 获取IP信息
        try {
            String ip = player.getAddress().getAddress().getHostAddress();
            getIpCountry(ip).thenAccept(area -> {
                getLogger().info("玩家【" + player.getName() + "】上线 | IP：" + ip + " | 归属：" + area);
            });
        } catch (Exception ex) {
            getLogger().warning("无法获取玩家 " + player.getName() + " 的IP信息");
        }

        // 检查是否已注册
        String pass = loadPassword(uuid);
        if (pass == null) {
            player.sendMessage("§6欢迎来到服务器！请先注册：/register 密码 确认密码");
        } else {
            player.sendMessage("§6请登录：/login 密码");
        }
    }

    // 退出置为未登录
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        isLoggedIn.put(uuid, false);
        accountPassword.remove(uuid); // 清理内存
    }

    // 未登录禁止移动
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (!isLoggedIn.getOrDefault(uuid, false)) {
            // 检查是否移动了位置（避免重复消息）
            if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
                    e.getFrom().getBlockY() == e.getTo().getBlockY() &&
                    e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
                return; // 没有实际移动，不取消
            }

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