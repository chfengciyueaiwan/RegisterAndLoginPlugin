package chfeng.ciyueaiwan.registerAndLoginPlugin121;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
import java.util.concurrent.ConcurrentHashMap;

public class RegisterAndLoginPlugin121 extends JavaPlugin implements Listener, CommandExecutor {
    public static final String VERSION = "1.0.4";
    private final Map<UUID, String> accountPassword = new HashMap<>();
    private final Map<UUID, Boolean> isLoggedIn = new HashMap<>();
    private final Map<UUID, BukkitRunnable> loginTimerTasks = new ConcurrentHashMap<>();
    private File accountFolder;

    // ====================== 【新增】登录失败记录和IP封禁 ======================
    private final Map<String, Integer> loginFailCount = new ConcurrentHashMap<>(); // IP -> 失败次数
    private final Map<String, Long> ipBanUntil = new ConcurrentHashMap<>(); // IP -> 解封时间戳

    private static final int LOGIN_TIMEOUT_SECONDS = 60;
    private static final int MAX_LOGIN_FAILS = 3; // 最多失败3次
    private static final int IP_BAN_SECONDS = 30; // 封禁30秒

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
                URI uri = URI.create("https://ipapi.co/" + ipAddr + "/json/");
                URL url = uri.toURL();
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String flag = "\"country_code\":\"";
                int start = json.indexOf(flag) + flag.length();
                int end = json.indexOf("\"", start);
                return json.substring(start, end);
            } catch (Exception e) {
                return "未知地区";
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    // ====================== 【新增】检查IP是否被封禁 ======================
    private boolean isIpBanned(String ip) {
        Long banEndTime = ipBanUntil.get(ip);
        if (banEndTime == null) return false;
        if (System.currentTimeMillis() < banEndTime) {
            return true; // 还在封禁时间内
        } else {
            // 封禁已过期，清理记录
            ipBanUntil.remove(ip);
            loginFailCount.remove(ip);
            return false;
        }
    }

    // ====================== 【新增】封禁IP ======================
    private void banIp(String ip, int seconds) {
        long banEndTime = System.currentTimeMillis() + (seconds * 1000L);
        ipBanUntil.put(ip, banEndTime);
        loginFailCount.put(ip, 0); // 重置失败次数
        getLogger().warning("IP " + ip + " 已被封禁 " + seconds + " 秒");

        // 踢出该IP的所有在线玩家
        for (Player player : getServer().getOnlinePlayers()) {
            String playerIp = player.getAddress().getAddress().getHostAddress();
            if (playerIp.equals(ip)) {
                player.kickPlayer("§c登录失败次数过多！\n§e你的IP已被封禁 " + seconds + " 秒\n§6请稍后再试！");
            }
        }
    }

    // ====================== 【新增】记录登录失败 ======================
    private void recordLoginFail(String ip) {
        int count = loginFailCount.getOrDefault(ip, 0) + 1;
        loginFailCount.put(ip, count);
        getLogger().warning("IP " + ip + " 登录失败 " + count + " 次");

        if (count >= MAX_LOGIN_FAILS) {
            banIp(ip, IP_BAN_SECONDS);
        }
    }

    @Override
    public void onEnable() {
        getLogger().info("§aRegister and Login Plugin v" + VERSION + " 已加载 [1.21系列]");
        getLogger().info("§e登录超时时间: " + LOGIN_TIMEOUT_SECONDS + " 秒");
        getLogger().info("§e最大登录失败次数: " + MAX_LOGIN_FAILS + " 次，封禁 " + IP_BAN_SECONDS + " 秒");
        accountFolder = new File(getDataFolder(), "accounts");
        if (!accountFolder.exists()) accountFolder.mkdirs();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("register").setExecutor(this);
        getCommand("login").setExecutor(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("§cRegister and Login Plugin 已卸载");
        for (BukkitRunnable task : loginTimerTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        loginTimerTasks.clear();
        accountPassword.clear();
        isLoggedIn.clear();
        loginFailCount.clear();
        ipBanUntil.clear();
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
            String saveText = passHash + "|" + ipInfo;
            Files.writeString(getPlayerFile(uuid), saveText);
        } catch (IOException e) {
            getLogger().severe("保存账号文件异常:" + uuid);
        }
    }

    // 启动登录倒计时
    private void startLoginTimer(Player player) {
        UUID uuid = player.getUniqueId();
        cancelLoginTimer(uuid);

        BukkitRunnable task = new BukkitRunnable() {
            int timeLeft = LOGIN_TIMEOUT_SECONDS;

            @Override
            public void run() {
                if (isLoggedIn.getOrDefault(uuid, false)) {
                    cancelLoginTimer(uuid);
                    return;
                }
                if (!player.isOnline()) {
                    cancelLoginTimer(uuid);
                    return;
                }

                if (timeLeft == 60) {
                    player.sendMessage("§c你还有 §e60秒 §c时间登录，否则将被踢出！");
                } else if (timeLeft == 30) {
                    player.sendMessage("§c你还有 §e30秒 §c时间登录，否则将被踢出！");
                } else if (timeLeft == 15) {
                    player.sendMessage("§c你还有 §e15秒 §c时间登录，否则将被踢出！");
                } else if (timeLeft == 10) {
                    player.sendMessage("§c你还有 §e10秒 §c时间登录，否则将被踢出！");
                } else if (timeLeft <= 5 && timeLeft > 0) {
                    player.sendMessage("§c你还有 §e" + timeLeft + "秒 §c时间登录！");
                }

                if (timeLeft <= 0) {
                    player.kickPlayer("§c登录超时！\n§e请在 " + LOGIN_TIMEOUT_SECONDS + " 秒内登录！\n§6使用 /login <密码> 登录");
                    cancelLoginTimer(uuid);
                    getLogger().info("玩家 " + player.getName() + " 因登录超时被踢出");
                    return;
                }
                timeLeft--;
            }
        };

        task.runTaskTimer(this, 0L, 20L);
        loginTimerTasks.put(uuid, task);
    }

    // 取消登录计时器
    private void cancelLoginTimer(UUID uuid) {
        BukkitRunnable task = loginTimerTasks.remove(uuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c仅游戏内玩家可用该指令！");
            return true;
        }

        UUID uuid = player.getUniqueId();
        String playerIp = player.getAddress().getAddress().getHostAddress();

        // ====================== 【新增】检查IP是否被封禁 ======================
        if (isIpBanned(playerIp)) {
            long remaining = (ipBanUntil.get(playerIp) - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c你的IP已被封禁！剩余 §e" + remaining + " §c秒");
            return true;
        }

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
            String p1 = args[0];
            String p2 = args[1];
            if (!p1.equals(p2)) {
                player.sendMessage("§c两次输入的密码不一致！");
                return true;
            }
            String hash = sha256Encrypt(p1);
            getIpCountry(playerIp).thenAccept(country -> {
                String fullIpData = playerIp + "-" + country;
                savePassword(uuid, hash, fullIpData);
            });
            accountPassword.put(uuid, hash);
            isLoggedIn.put(uuid, true);
            cancelLoginTimer(uuid);
            // ====================== 【新增】登录成功清理IP失败记录 ======================
            loginFailCount.remove(playerIp);
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

            String inputHash = sha256Encrypt(args[0]);
            if (savedPassHash.equals(inputHash)) {
                isLoggedIn.put(uuid, true);
                cancelLoginTimer(uuid);
                // ====================== 【新增】登录成功清理IP失败记录 ======================
                loginFailCount.remove(playerIp);
                player.sendMessage("§a登录成功！");
                getLogger().info("玩家 " + player.getName() + " 登录成功");
            } else {
                // ====================== 【新增】记录登录失败 ======================
                player.sendMessage("§c密码错误！");
                recordLoginFail(playerIp);

                // 显示剩余尝试次数
                int remainingAttempts = MAX_LOGIN_FAILS - loginFailCount.getOrDefault(playerIp, 0);
                if (remainingAttempts > 0) {
                    player.sendMessage("§c你还剩 §e" + remainingAttempts + " §c次尝试机会！");
                } else {
                    // 如果已经达到最大次数，IP已经被封禁
                    if (isIpBanned(playerIp)) {
                        long remaining = (ipBanUntil.get(playerIp) - System.currentTimeMillis()) / 1000;
                        player.sendMessage("§c你的IP已被封禁！剩余 §e" + remaining + " §c秒");
                    }
                }
                getLogger().warning("玩家 " + player.getName() + " 登录失败 - 密码错误");
            }
            return true;
        }
        return false;
    }

    // 进服重置登录状态并启动计时器
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerIp = player.getAddress().getAddress().getHostAddress();

        // ====================== 【新增】检查IP是否被封禁 ======================
        if (isIpBanned(playerIp)) {
            long remaining = (ipBanUntil.get(playerIp) - System.currentTimeMillis()) / 1000;
            player.kickPlayer("§c你的IP已被封禁！\n§e剩余 " + remaining + " 秒\n§6请稍后再试！");
            return;
        }

        isLoggedIn.put(uuid, false);
        startLoginTimer(player);

        getIpCountry(playerIp).thenAccept(area -> {
            getLogger().info("玩家【" + player.getName() + "】上线 | IP：" + playerIp + " | 归属：" + area);
        });

        String pass = loadPassword(uuid);
        if (pass == null) {
            player.sendMessage("§6欢迎来到服务器！请先注册：/register 密码 确认密码");
            player.sendMessage("§c你有 §e" + LOGIN_TIMEOUT_SECONDS + "秒 §c时间注册或登录！");
        } else {
            player.sendMessage("§6请登录：/login 密码");
            player.sendMessage("§c你有 §e" + LOGIN_TIMEOUT_SECONDS + "秒 §c时间登录，否则将被踢出！");
        }
    }

    // 退出时清理计时器
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        isLoggedIn.put(uuid, false);
        cancelLoginTimer(uuid);
    }

    // 未登录禁止移动
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (!isLoggedIn.getOrDefault(uuid, false)) {
            e.setCancelled(true);
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

    // 未登录禁止破坏方块
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
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

    // 未登录禁止放置方块
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
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

    // 未登录禁止拾取掉落物
    @EventHandler
    public void onPickupItem(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        UUID uuid = p.getUniqueId();
        if (!isLoggedIn.getOrDefault(uuid, false)) {
            e.setCancelled(true);
        }
    }
}