#!/bin/bash

err=1
until [ $err == 0 ];
do
        #mysqlcheck -h $DBHOST -u $USER --password=$PASS -s -r $DBNAME>>"log/`date +%Y-%m-%d_%H:%M:%S`-sql_check.log"
        #mysqldump -h $DBHOST -u $USER --password=$PASS $DBNAME|zip "backup/`date +%Y-%m-%d_%H:%M:%S`-l2jdb_gameserver.zip" -
        [ -f log/java0.log.0 ] && mv log/java0.log.0 "log/`date +%Y-%m-%d_%H-%M-%S`_java.log"
        [ -f log/stdout.log ] &&  mv log/stdout.log "log/`date +%Y-%m-%d_%H-%M-%S`_stdout.log"
        [ -f log/chat.log ] && mv log/chat.log "log/`date +%Y-%m-%d_%H:%M:%S`-chat.log"
        #java -server -Dfile.encoding=UTF- -Xms2g -Xmx3g -Xmn512m -Xss128k -XX:PermSize=128m -XX:MaxPermSize=768m -XX:ParallelGCThreads=2 -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycleMin=0 -XX:CMSIncrementalDutyCycle=10 -XX:+CMSParallelRemarkEnabled -XX:SurvivorRatio=8 -XX:TargetSurvivorRatio=90 -XX:MaxTenuringThreshold=31 -XX:+AggressiveOpts -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:gc.txt -cp :lib/*:L2Dream.jar com.src.gameserver.GameServer > log/stdout.log 2>&1
		java -Xms1024m -Xmx1024m -cp lib/*:L2Dream.jar com.src.gameserver.GameServer > log/stdout.log 2>&1
        err=$?
        sleep 10
done