# Register And Login Plugin
轻量级离线模式服务器登录注册插件，有效防止离线服务器账号被盗。

## ✨功能
1. **注册系统**
指令：`/register <密码> <确认密码>`
未注册玩家无法移动、聊天、破坏方块、拾取掉落物等，仅能执行注册指令。两次输入密码必须保持一致，注册成功自动登录。

2. **登录系统**
指令：`/login <密码>`
已注册玩家进入服务器处于锁定状态，无法移动、聊天，输入正确密码登录后解除全部限制。

3. **数据持久化**
玩家账号密码保存在 `plugins/RegisterAndLoginPlugin/accounts/玩家UUID.txt`
使用玩家UUID作为文件名存储账号信息，不怕玩家改名导致账号丢失，服务器重启数据不会丢失。

4. **安全防护**
登录错误超过三次封禁IP30秒，防止暴力破解

## ⚙️支持服务端
<<<<<<< HEAD
✅ Paper 1.21 ~ 26.2
=======
✅ Paper 1.21 ~ 1.21.11
>>>>>>> 0c5fda95d35f1942bbb0e2f8c8608d2972b759ba
✅ Purpur 1.21.x
❌ 不支持 Spigot、Fabric、26.1 系列新版本

## 📂开源信息
本项目采用 MIT License 开源
源码仓库：https://github.com/chfengciyueaiwan/RegisterAndLoginPlugin
欢迎提交反馈与PR，共同完善插件。

---

# Register And Login Plugin
A lightweight offline-mode server registration and login plugin, effectively preventing account theft on offline servers.

## ✨Features
1. **Registration System**
Command: `/register <password> <confirm password>`
Unregistered players cannot move, chat, break blocks, pick up dropped items, etc., and can only execute the registration command. The two password entries must match. Successful registration automatically logs the player in.

2. **Login System**
Command: `/login <password>`
Registered players enter the server in a locked state, unable to move or chat. Entering the correct password logs them in and removes all restrictions.

3. **Data Persistence**
Player account passwords are saved in `plugins/RegisterAndLoginPlugin/accounts/playerUUID.txt`
The player's UUID is used as the filename to store account information, so name changes won't cause account loss. Server restarts will not result in data loss.

4. **Security Protection**
Exceeding three failed login attempts results in a 30-second IP ban to prevent brute-force attacks.

## ⚙️Supported Server Versions
<<<<<<< HEAD
✅ Paper 1.21 ~ 26.2
=======
✅ Paper 1.21 ~ 1.21.11
>>>>>>> 0c5fda95d35f1942bbb0e2f8c8608d2972b759ba
✅ Purpur 1.21.x
❌ Spigot, Fabric, and 26.1 series new versions are not supported.

## 📂Open Source Information
This project is open-sourced under the MIT License.
Source repository: https://github.com/chfengciyueaiwan/RegisterAndLoginPlugin
Feedback and pull requests are welcome to help improve the plugin.