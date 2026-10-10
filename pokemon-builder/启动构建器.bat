@echo off
REM Pokemon 构建器：可视化导出 PC / 安卓（需要 Windows PowerShell 5.1，系统自带）
chcp 65001 >nul
set "HERE=%~dp0"
start "" powershell.exe -NoProfile -ExecutionPolicy Bypass -STA -WindowStyle Hidden -File "%HERE%BuilderGUI.ps1" %*
