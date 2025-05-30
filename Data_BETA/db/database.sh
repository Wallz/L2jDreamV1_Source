#!/bin/bash
############################################
## Writen by DrLecter                     ##
## License: GNU GPL                       ##
## Based on Tiago Tagliaferri's script    ##
## E-mail: tiago_tagliaferri@msn.com      ##
## From "L2J-DataPack"                    ##
############################################
trap finish 2

configure() {
echo "#############################################"
echo "# You entered script configuration area     #"
echo "# No change will be performed in your DB    #"
echo "# I will just ask you some questions about  #"
echo "# your hosts and DB.                        #"
echo "#############################################"
MYSQLDUMPPATH=`which mysqldump 2>/dev/null`
MYSQLPATH=`which mysql 2>/dev/null`
if [ $? -ne 0 ]; then
echo "We were unable to find MySQL binaries on your path"
while :
 do
  echo -ne "\nPlease enter MySQL binaries directory (no trailing slash): "
  read MYSQLBINPATH
    if [ -e "$MYSQLBINPATH" ] && [ -d "$MYSQLBINPATH" ] && [ -e "$MYSQLBINPATH/mysqldump" ] && [ -e "$MYSQLBINPATH/mysql" ]; then
       MYSQLDUMPPATH="$MYSQLBINPATH/mysqldump"
       MYSQLPATH="$MYSQLBINPATH/mysql"
       break
    else
       echo "The data you entered is invalid. Please verify and try again."
       exit 1
    fi
 done
fi
#LS
echo -ne "\nPlease enter MySQL Login Server hostname (default localhost): "
read LSDBHOST
if [ -z "$LSDBHOST" ]; then
  LSDBHOST="localhost"
fi
echo -ne "\nPlease enter MySQL Login Server database name (default l2jdb): "
read LSDB
if [ -z "$LSDB" ]; then
  LSDB="l2dream"
fi
echo -ne "\nPlease enter MySQL Login Server user (default root): "
read LSUSER
if [ -z "$LSUSER" ]; then
  LSUSER="root"
fi
echo -ne "\nPlease enter MySQL Login Server $LSUSER's password (won't be displayed) :"
stty -echo
read LSPASS
stty echo
echo ""
if [ -z "$LSPASS" ]; then
  echo "Hum.. i'll let it be but don't be stupid and avoid empty passwords"
elif [ "$LSUSER" == "$LSPASS" ]; then
  echo "You're not too brilliant choosing passwords huh?"
fi
#GS
echo -ne "\nPlease enter MySQL Game Server hostname (default $LSDBHOST): "
read GSDBHOST
if [ -z "$GSDBHOST" ]; then
  GSDBHOST="$LSDBHOST"
fi
echo -ne "\nPlease enter MySQL Game Server database name (default $LSDB): "
read GSDB
if [ -z "$GSDB" ]; then
  GSDB="$LSDB"
fi
echo -ne "\nPlease enter MySQL Game Server user (default $LSUSER): "
read GSUSER
if [ -z "$GSUSER" ]; then
  GSUSER="$LSUSER"
fi
echo -ne "\nPlease enter MySQL Game Server $GSUSER's password (won't be displayed): "
stty -echo
read GSPASS
stty echo
echo ""
if [ -z "$GSPASS" ]; then
  echo "Hum.. i'll let it be but don't be stupid and avoid empty passwords"
elif [ "$GSUSER" == "$GSPASS" ]; then
  echo "You're not too brilliant choosing passwords huh?"
fi
save_config $1
}

save_config() {
if [ -n "$1" ]; then
CONF="$1"
else 
CONF="database_installer.rc"
fi
echo ""
echo "With these data i can generate a configuration file which can be read"
echo "on future updates. WARNING: this file will contain clear text passwords!"
echo -ne "Shall i generate config file $CONF? (Y/n):"
read SAVE
if [ "$SAVE" == "y" -o "$SAVE" == "Y" -o "$SAVE" == "" ];then 
cat <<EOF>$CONF
#Configuration settings for L2J-Datapack database installer script
MYSQLDUMPPATH=$MYSQLDUMPPATH
MYSQLPATH=$MYSQLPATH
LSDBHOST=$LSDBHOST
LSDB=$LSDB
LSUSER=$LSUSER
LSPASS=$LSPASS
GSDBHOST=$GSDBHOST
GSDB=$GSDB
GSUSER=$GSUSER
GSPASS=$GSPASS
EOF
chmod 600 $CONF
echo "Configuration saved as $CONF"
echo "Permissions changed to 600 (rw- --- ---)"
elif [ "$SAVE" != "n" -a "$SAVE" != "N" ]; then
  save_config
fi
}

load_config() {
if [ -n "$1" ]; then
CONF="$1"
else 
CONF="database_installer.rc"
fi
if [ -e "$CONF" ] && [ -f "$CONF" ]; then
. $CONF
else
echo "Settings file not found: $CONF"
echo "You can specify an alternate settings filename:"
echo $0 config_filename
echo ""
echo "If file doesn't exist it can be created"
echo "If nothing is specified script will try to work with ./database_installer.rc"
echo ""
configure $CONF
fi
}

asklogin(){
echo "#############################################"
echo "# WARNING: This section of the script CAN   #"
echo "# destroy your characters and accounts      #"
echo "# information. Read questions carefully     #"
echo "# before you reply.                         #"
echo "#############################################"
echo ""
echo "Choose upgrade (u) if you already have an 'accounts' table but no"
echo "'gameserver' table (ie. your server is a pre LS/GS split version.)"
echo "Choose skip (s) to skip loginserver DB installation and go to"
echo "gameserver DB installation/upgrade."
echo -ne "LOGINSERVER DB install type: (f) full, (u) upgrade or (s) skip or (q) quit? "
read LOGINPROMPT
case "$LOGINPROMPT" in
	"f"|"F") logininstall; loginupgrade; gsbackup; asktype;;
	"u"|"U") loginupgrade; gsbackup; asktype;;
	"s"|"S") gsbackup; asktype;;
	"q"|"Q") finish;;
	*) asklogin;;
esac
}

logininstall(){
echo "Deleting loginserver tables for new content."
$MYL < login_install.sql &> /dev/null
}

loginupgrade(){
echo "Installling new loginserver content."
$MYL < ../sql/accounts.sql &> /dev/null
$MYL < ../sql/gameservers.sql &> /dev/null
}

gsbackup(){
while :
  do
   echo ""
   echo -ne "Do you want to make a backup copy of your GSDB? (y/n): "
   read LSB
   if [ "$LSB" == "Y" -o "$LSB" == "y" ]; then
     echo "Making a backup of the original gameserver database."
     $MYSQLDUMPPATH --add-drop-table -h $GSDBHOST -u $GSUSER --password=$GSPASS $GSDB > gameserver_backup.sql
     if [ $? -ne 0 ];then
     echo ""
     echo "There was a problem accesing your GS database, either it wasnt created or authentication data is incorrect."
     exit 1
     fi
     break
   elif [ "$LSB" == "n" -o "$LSB" == "N" ]; then 
     break
   fi
  done 
}

lsbackup(){
while :
  do
   echo ""
   echo -ne "Do you want to make a backup copy of your LSDB? (y/n): "
   read LSB
   if [ "$LSB" == "Y" -o "$LSB" == "y" ]; then
     echo "Making a backup of the original loginserver database."
     $MYSQLDUMPPATH --add-drop-table -h $LSDBHOST -u $LSUSER --password=$LSPASS $LSDB > loginserver_backup.sql
     if [ $? -ne 0 ];then
        echo ""
        echo "There was a problem accesing your LS database, either it wasnt created or authentication data is incorrect."
        exit 1
     fi
     break
   elif [ "$LSB" == "n" -o "$LSB" == "N" ]; then 
     break
   fi
  done 
}

asktype(){
echo ""
echo ""
echo "WARNING: A full install (f) will destroy all existing character data."
echo -ne "GAMESERVER DB install type: (f) full install or (u) upgrade or (s) skip or (q) quit?"
read INSTALLTYPE
case "$INSTALLTYPE" in
	"f"|"F") fullinstall; upgradeinstall I; experimental; expinstall;;
	"u"|"U") upgradeinstall U; experimental; expinstall;;
	"s"|"S") experimental; expinstall;;
	"q"|"Q") finish;;
	*) asktype;;
esac
}

fullinstall(){
echo "Deleting all gameserver tables for new content."
$MYG < full_install.sql &> /dev/null
}

upgradeinstall(){
if [ "$1" == "I" ]; then 
echo "Installling new gameserver content."
else
echo "Upgrading gameserver content"
fi
$MYG < ..sql/characters/characters/char_creation_items &> /dev/null
$MYG < ..sql/characters/character_friends &> /dev/null
$MYG < ..sql/characters/character_hennas &> /dev/null
$MYG < ..sql/characters/character_macroses &> /dev/null
$MYG < ..sql/characters/character_offline_trade &> /dev/null
$MYG < ..sql/characters/character_quest_global_data &> /dev/null
$MYG < ..sql/characters/character_quests &> /dev/null
$MYG < ..sql/characters/character_raid_points &> /dev/null
$MYG < ..sql/characters/character_recipebook &> /dev/null
$MYG < ..sql/characters/character_recommends &> /dev/null
$MYG < ..sql/characters/character_shortcuts &> /dev/null
$MYG < ..sql/characters/character_skills &> /dev/null
$MYG < ..sql/characters/character_skills_save &> /dev/null
$MYG < ..sql/characters/character_subclasses &> /dev/null
$MYG < ..sql/characters/characters &> /dev/null
$MYG < ..sql/characters/characters_custom_data &> /dev/null
$MYG < ..sql/characters/character_variables &> /dev/null
$MYG < ..sql/events/z_bw_teams &> /dev/null
$MYG < ..sql/events/z_tvt_teams &> /dev/null
$MYG < ..sql/events/z_ctf_teams &> /dev/null
$MYG < ..sql/features/accounts &> /dev/null
$MYG < ..sql/features/armor &> /dev/null
$MYG < ..sql/features/auction &> /dev/null
$MYG < ..sql/features/auction_bid &> /dev/null
$MYG < ..sql/features/auction_watch &> /dev/null
$MYG < ..sql/features/augmentations &> /dev/null
$MYG < ..sql/features/auto_announcements &> /dev/null
$MYG < ..sql/features/castle &> /dev/null
$MYG < ..sql/features/castle_door &> /dev/null
$MYG < ..sql/features/castle_doorupgrade &> /dev/null
$MYG < ..sql/features/castle_manor_procure &> /dev/null
$MYG < ..sql/features/castle_manor_production &> /dev/null
$MYG < ..sql/features/castle_siege_guards &> /dev/null
$MYG < ..sql/features/clan_data &> /dev/null
$MYG < ..sql/features/clan_notices &> /dev/null
$MYG < ..sql/features/clan_privs &> /dev/null
$MYG < ..sql/features/clan_skills &> /dev/null
$MYG < ..sql/features/clan_subpledges &> /dev/null
$MYG < ..sql/features/clan_wars &> /dev/null
$MYG < ..sql/features/clanhall &> /dev/null
$MYG < ..sql/features/clanhall_functions &> /dev/null
$MYG < ..sql/features/clanhall_siege &> /dev/null
$MYG < ..sql/features/class_list &> /dev/null
$MYG < ..sql/features/cursed_weapons &> /dev/null
$MYG < ..sql/features/droplist &> /dev/null
$MYG < ..sql/features/etcitem &> /dev/null
$MYG < ..sql/features/fort &> /dev/null
$MYG < ..sql/features/fort_door &> /dev/null
$MYG < ..sql/features/fort_doorupgrade &> /dev/null
$MYG < ..sql/features/fort_siege_guards &> /dev/null
$MYG < ..sql/features/fortsiege_clans &> /dev/null
$MYG < ..sql/features/forums &> /dev/null
$MYG < ..sql/features/four_sepulchers_spawnlist &> /dev/null
$MYG < ..sql/features/games &> /dev/null
$MYG < ..sql/features/gameservers &> /dev/null
$MYG < ..sql/features/global_tasks &> /dev/null
$MYG < ..sql/features/grandboss_data &> /dev/null
$MYG < ..sql/features/grandboss_list &> /dev/null
$MYG < ..sql/features/henna_trees &> /dev/null
$MYG < ..sql/features/heroes &> /dev/null
$MYG < ..sql/features/items &> /dev/null
$MYG < ..sql/features/itemsonground &> /dev/null
$MYG < ..sql/features/merchant_buylists &> /dev/null
$MYG < ..sql/features/merchant_lease &> /dev/null
$MYG < ..sql/features/merchant_shopids &> /dev/null
$MYG < ..sql/features/mods_wedding &> /dev/null
$MYG < ..sql/features/noble_teleport &> /dev/null
$MYG < ..sql/features/npc &> /dev/null
$MYG < ..sql/features/npcskills &> /dev/null
$MYG < ..sql/features/olympiad_nobles &> /dev/null
$MYG < ..sql/features/pets &> /dev/null
$MYG < ..sql/features/pkkills &> /dev/null
$MYG < ..sql/features/posts &> /dev/null
$MYG < ..sql/features/quest_global_data &> /dev/null
$MYG < ..sql/features/raidboss_spawnlist &> /dev/null
$MYG < ..sql/features/random_spawn &> /dev/null
$MYG < ..sql/features/random_spawn_loc &> /dev/null
$MYG < ..sql/features/seven_signs &> /dev/null
$MYG < ..sql/features/seven_signs_festival &> /dev/null
$MYG < ..sql/features/seven_signs_status &> /dev/null
$MYG < ..sql/features/siege_clans &> /dev/null
$MYG < ..sql/features/siege_guards_respawn_time &> /dev/null
$MYG < ..sql/features/spawnlist &> /dev/null
$MYG < ..sql/features/topic &> /dev/null
$MYG < ..sql/features/vanhalter_spawnlist &> /dev/null
$MYG < ..sql/features/weapon &> /dev/null
$MYG < ..sql/features/zone_vertices &> /dev/null
}

expinstall(){
while :
  do
   echo ""
   echo -ne "Do you want to make another backup of GSDB before applying experimental? (y/N): "
   read LSB
   if [ "$LSB" == "Y" -o "$LSB" == "y" ]; then
     echo "Making a backup of the default gameserver tables."
     $MYSQLDUMPPATH --add-drop-table -h $GSDBHOST -u $GSUSER --password=$GSPASS $LSDB > experimental_backup.sql &> /dev/null
     if [ $? -ne 0 ];then
     echo ""
     echo "There was a problem accesing your GS database, server down?."
     exit 1
     fi
     break
   elif [ "$LSB" == "n" -o "$LSB" == "N" -o "$LSB" == "" ]; then 
     break
   fi
  done 
echo "Installing experimental content."
#$MYG < ../sql/experimental/npc.sql &> /dev/null
#$MYG < ../sql/experimental/npcskills.sql &> /dev/null
$MYG < ../sql/experimental/spawnlist-experimental.sql &> /dev/null
finish
}

finish(){
echo ""
echo "Script execution finished."
exit 0
}

clear
load_config $1
MYL="$MYSQLPATH -h $LSDBHOST -u $LSUSER --password=$LSPASS -D $LSDB"
MYG="$MYSQLPATH -h $GSDBHOST -u $GSUSER --password=$GSPASS -D $GSDB"
lsbackup
asklogin