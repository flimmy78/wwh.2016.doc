@echo off
D:
if "%1"=="ep" cd D:\eichong\protocol\dEpPlatform\dEpGate
if "%1"=="dp" cd D:\eichong\protocol\dEpPlatform\dPhoneGate
call ant
rem pause