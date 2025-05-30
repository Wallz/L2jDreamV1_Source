@echo off
color 0C
title GameServer: Console
:start

REM -------------------------------------
REM Default parameters for a basic server.
REM -------------------------------------

java -Xmx1g -XX:+AggressiveOpts -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -XX:MaxPermSize=256m -XX:+HeapDumpOnOutOfMemoryError -cp ./lib/*;L2Dream.jar com.src.gameserver.GameServer


if ERRORLEVEL 7 goto telldown
if ERRORLEVEL 6 goto tellrestart
if ERRORLEVEL 5 goto taskrestart
if ERRORLEVEL 4 goto taskdown
if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end
:tellrestart
echo.
echo Telnet server Restart ...
echo.
goto start
:taskrestart
echo.
echo Auto Task Restart ...
echo.
goto start
:restart
echo.
echo Admin Restart ...
echo.
goto start
:taskdown
echo .
echo Server terminated (Auto task)
echo .
:telldown
echo .
echo Server terminated (Telnet)
echo .
:error
echo.
echo Server terminated abnormaly
echo.
:end
echo.
echo Server terminated
echo.
:question
set choix=q
set /p choix=Restart(r) or Quit(q)
if /i %choix%==r goto start
if /i %choix%==q goto exit
:exit
exit
pause