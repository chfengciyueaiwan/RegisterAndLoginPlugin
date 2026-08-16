@echo off
chcp 65001 >nul
title Git 快速同步

echo 正在同步代码...

cd /d "%~dp0"

REM 拉取最新代码
git pull origin main --no-edit

REM 添加所有更改
git add .

REM 如果有更改则提交
git diff --cached --quiet
if errorlevel 1 (
    git commit -m "自动同步更新 %date% %time%"
)

REM 推送到远程
git push origin main

echo.
echo 同步完成！
pause