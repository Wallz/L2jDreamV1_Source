@echo off
color 0C
title Dream account manager console
@java -Djava.util.logging.config.file=config/other/console.cfg -cp ./lib/*;L2Dream.jar com.src.accmanager.SQLAccountManager
@pause