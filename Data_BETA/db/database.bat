@echo off
title Database: Installer

REM #--------------------------------------------
REM #                Settings
REM #--------------------------------------------
set mysqlBinPath=C:\Program Files\MySQL\MySQL Server 5.1\bin
set DateT=%date%
REM #--------------------------------------------
set user=root
set pass=
set db=l2dream
set host=localhost
REM #--------------------------------------------
set mysqldumpPath="%mysqlBinPath%\mysqldump"
set mysqlPath="%mysqlBinPath%\mysql"
REM #--------------------------------------------

:Begin
@cls
echo.#--------------------------------------------
echo.#        Databse Installer Dream Files
echo.#--------------------------------------------
echo.#   1 - Install database.
echo.#   2 - Install custom tables.
echo.#   3 - Exit.
echo.#--------------------------------------------
echo.

set Beginprompt=x
set /p Beginprompt=Choice: 
if /i %Beginprompt%==1 goto install
if /i %Beginprompt%==2 goto custom
if /i %Beginprompt%==3 goto exit
goto Begin


:install
@cls
echo.#--------------------------------------------
echo.#             Delete old Databse
echo.#--------------------------------------------
echo.
echo Old tables on %db% deleting!
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/installer/clean.sql
echo.
echo ..Done!
echo.
pause
goto installpart2


:installpart2
@cls
echo.#--------------------------------------------
echo.#            Install new Databse
echo.#--------------------------------------------
echo.
echo ~*Characters*~
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/char_creation_items.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_friends.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_hennas.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_macroses.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_offline_trade.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_quest_global_data.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_quests.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_raid_points.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_recipebook.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_recommends.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_shortcuts.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_skills.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_skills_save.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_subclasses.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/characters.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/characters_custom_data.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/characters/character_variables.sql
echo.
echo ..Done!

echo.
echo ~*Events*~
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/events/z_bw_teams.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/events/z_tvt_teams.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/events/z_ctf_teams.sql
echo.
echo ..Done!

echo.
echo ~*Features*~
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/accounts.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/armor.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/auction.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/auction_bid.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/auction_watch.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/augmentations.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/auto_announcements.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/castle.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/castle_door.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/castle_doorupgrade.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/castle_manor_procure.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/castle_manor_production.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/castle_siege_guards.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/clan_data.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/clan_notices.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/clan_privs.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/clan_skills.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/clan_subpledges.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/clan_wars.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/clanhall.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/clanhall_functions.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/clanhall_siege.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/class_list.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/cursed_weapons.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/droplist.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/etcitem.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/forums.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/four_sepulchers_spawnlist.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/games.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/gameservers.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/global_tasks.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/grandboss_data.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/grandboss_list.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/henna_trees.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/heroes.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/items.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/itemsonground.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/merchant_buylists.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/merchant_lease.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/merchant_shopids.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/mods_wedding.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/noble_teleport.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/npc.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/npcskills.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/olympiad_nobles.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/pets.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/pkkills.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/posts.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/quest_global_data.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/raidboss_spawnlist.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/random_spawn.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/random_spawn_loc.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/seven_signs.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/seven_signs_festival.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/seven_signs_status.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/siege_clans.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/siege_guards_respawn_time.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/spawnlist.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/topic.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/vanhalter_spawnlist.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/weapon.sql
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/features/zone_vertices.sql
echo.
echo ..Done!

echo.
echo Database installed successfully!
echo.
pause
goto Begin


:custom
@cls
echo Instaling custom tables...
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/custom/custom_armor.sql
echo Armor custom table installed successfully.
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/custom/custom_armorsets.sql
echo Armorsets custom table installed successfully.
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/custom/custom_droplist.sql
echo Droplist custom table installed successfully.
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/custom/custom_etcitem.sql
echo Etcitem custom table installed successfully.
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/custom/custom_merchant_buylists.sql
echo Merchant buy lists custom table installed successfully.
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/custom/custom_merchant_shopids.sql
echo Merchant shop ids custom table installed successfully.
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/custom/custom_notspawned.sql
echo Notspawned custom table installed successfully.
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/custom/custom_npc.sql
echo Npc custom table installed successfully.
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/custom/custom_spawnlist.sql
echo Spawnlist custom table installed successfully.
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/custom/custom_teleport.sql
echo Teleport custom table installed successfully.
%mysqlPath% -h %host% -u %user% --password=%pass% -D %db% < sql/custom/custom_weapon.sql
echo Weapon Custom table installed successfully.

echo Custom tables installed successfully.
pause
goto Begin


:exit