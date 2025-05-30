#!/bin/sh
java -Djava.util.logging.config.file=config/other/console.cfg -cp ./lib/*:L2JDream.jar:mysql-connector-java-5.1.18-bin.jar com.src.accmanager.SQLAccountManager
