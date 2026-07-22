# Register And Login Plugin
轻量级离线模式服务器登录注册插件，有效防止离线服务器账号被盗。

## ✨功能
1. **注册系统**
指令：`/register <密码> <确认密码>`
未注册玩家无法移动、聊天，仅能执行注册指令。两次输入密码必须保持一致，注册成功自动登录。

2. **登录系统**
指令：`/login <密码>`
已注册玩家进入服务器处于锁定状态，无法移动、聊天，输入正确密码登录后解除全部限制。

3. **数据持久化**
玩家账号密码保存在 `plugins/RegisterAndLoginPlugin/accounts/玩家UUID.txt`
使用玩家UUID作为文件名存储账号信息，不怕玩家改名导致账号丢失，服务器重启数据不会丢失。

## ⚙️支持服务端
✅ Paper 1.21 ~ 1.21.11
✅ Purpur 1.21.x
❌ 不支持 Spigot、Fabric、26.1 系列新版本

## ⚠️注意
当前版本密码为明文存储，适合小型私人服务器使用，公网服务器请谨慎部署。
后续版本会增加密码哈希加密、修改密码、超时踢出等功能，同时推出适配26.1的独立版本。

## 📂开源信息
本项目采用 MIT License 开源
源码仓库：https://github.com/chfengciyueaiwan/RegisterAndLoginPlugin
欢迎提交反馈与PR，共同完善插件。

---

# Register And Login Plugin
Lightweight offline-mode server registration & login plugin, effectively preventing account theft on offline servers.

## ✨ Features
1. **Register System**
Command: `/register <Password> <ConfirmPassword>`
Unregistered players will be restricted from moving and chatting. Only the register command can be executed. The two entered passwords must match, and you will log in automatically after successful registration.

2. **Login System**
Command: `/login <Password>`
Registered players enter the server in locked status. You cannot move or chat until you enter the correct password and lift all restrictions.

3. **Data Persistence**
Storage path: `plugins/RegisterAndLoginPlugin/accounts/PlayerUUID.txt`
Accounts are saved by player UUID. Your account will not be lost even if you change your in-game name, and data will survive server restarts.

## ⚙️ Supported Server Software
✅ Paper 1.21 ~ 1.21.11
✅ Purpur 1.21.x
❌ Not compatible with Spigot, Fabric, 26.1 new versions

## ⚠️ Notice
Passwords are stored in plain text in the current version. Suitable for small private servers. Use cautiously on public networks.
Future updates: password hash encryption, password change command, offline timeout kick, and independent version for 26.1.

## 📄 Open Source
This project is open source under MIT License
Source Code: https://github.com/chfengciyueaiwan/RegisterAndLoginPlugin
Issues and Pull Requests are welcome.
