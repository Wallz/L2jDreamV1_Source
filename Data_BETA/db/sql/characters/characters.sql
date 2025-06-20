CREATE TABLE `characters` (
  `account_name` varchar(45) DEFAULT NULL,
  `obj_Id` decimal(11,0) NOT NULL DEFAULT '0',
  `char_name` varchar(35) NOT NULL,
  `level` decimal(11,0) DEFAULT NULL,
  `maxHp` decimal(11,0) DEFAULT '1',
  `curHp` decimal(18,0) DEFAULT '1',
  `maxCp` decimal(11,0) DEFAULT '1',
  `curCp` decimal(18,0) DEFAULT '1',
  `maxMp` decimal(11,0) DEFAULT '1',
  `curMp` decimal(18,0) DEFAULT '1',
  `acc` decimal(11,0) DEFAULT NULL,
  `crit` decimal(10,0) DEFAULT NULL,
  `evasion` decimal(11,0) DEFAULT NULL,
  `mAtk` decimal(11,0) DEFAULT NULL,
  `mDef` decimal(11,0) DEFAULT NULL,
  `mSpd` decimal(11,0) DEFAULT NULL,
  `pAtk` decimal(11,0) DEFAULT NULL,
  `pDef` decimal(11,0) DEFAULT NULL,
  `pSpd` decimal(11,0) DEFAULT NULL,
  `runSpd` decimal(11,0) DEFAULT NULL,
  `walkSpd` decimal(11,0) DEFAULT NULL,
  `str` decimal(11,0) DEFAULT NULL,
  `con` decimal(11,0) DEFAULT NULL,
  `dex` decimal(11,0) DEFAULT NULL,
  `_int` decimal(11,0) DEFAULT NULL,
  `men` decimal(11,0) DEFAULT NULL,
  `wit` decimal(11,0) DEFAULT NULL,
  `face` decimal(11,0) DEFAULT NULL,
  `hairStyle` decimal(11,0) DEFAULT NULL,
  `hairColor` decimal(11,0) DEFAULT NULL,
  `sex` decimal(11,0) DEFAULT NULL,
  `heading` decimal(11,0) DEFAULT NULL,
  `x` decimal(11,0) DEFAULT NULL,
  `y` decimal(11,0) DEFAULT NULL,
  `z` decimal(11,0) DEFAULT NULL,
  `movement_multiplier` decimal(9,8) DEFAULT NULL,
  `attack_speed_multiplier` decimal(10,9) DEFAULT NULL,
  `colRad` decimal(10,3) DEFAULT NULL,
  `colHeight` decimal(10,3) DEFAULT NULL,
  `exp` decimal(20,0) DEFAULT NULL,
  `expBeforeDeath` decimal(20,0) DEFAULT '0',
  `sp` decimal(11,0) DEFAULT NULL,
  `karma` decimal(11,0) DEFAULT NULL,
  `pvpkills` decimal(11,0) DEFAULT NULL,
  `pkkills` decimal(11,0) DEFAULT NULL,
  `clanid` decimal(11,0) DEFAULT NULL,
  `maxload` decimal(11,0) DEFAULT NULL,
  `race` decimal(11,0) DEFAULT NULL,
  `classid` decimal(11,0) DEFAULT NULL,
  `base_class` int(2) NOT NULL DEFAULT '0',
  `deletetime` decimal(20,0) DEFAULT NULL,
  `cancraft` decimal(11,0) DEFAULT NULL,
  `title` varchar(16) DEFAULT NULL,
  `rec_have` int(3) NOT NULL DEFAULT '0',
  `rec_left` int(3) NOT NULL DEFAULT '0',
  `accesslevel` decimal(4,0) DEFAULT NULL,
  `online` decimal(1,0) DEFAULT NULL,
  `onlinetime` decimal(20,0) DEFAULT NULL,
  `char_slot` decimal(1,0) DEFAULT NULL,
  `newbie` decimal(1,0) DEFAULT '1',
  `lastAccess` decimal(20,0) DEFAULT NULL,
  `clan_privs` int(11) DEFAULT '0',
  `wantspeace` decimal(1,0) DEFAULT '0',
  `isin7sdungeon` decimal(1,0) NOT NULL DEFAULT '0',
  `punish_level` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `punish_timer` INT UNSIGNED NOT NULL DEFAULT 0,
  `power_grade` decimal(11,0) DEFAULT NULL,
  `nobless` decimal(1,0) NOT NULL DEFAULT '0',
  `hero` decimal(1,0) NOT NULL DEFAULT '0',
  `subpledge` int(1) NOT NULL DEFAULT '0',
  `last_recom_date` decimal(20,0) NOT NULL DEFAULT '0',
  `lvl_joined_academy` int(1) NOT NULL DEFAULT '0',
  `apprentice` int(1) NOT NULL DEFAULT '0',
  `sponsor` int(1) NOT NULL DEFAULT '0',
  `varka_ketra_ally` int(1) NOT NULL DEFAULT '0',
  `clan_join_expiry_time` decimal(20,0) NOT NULL DEFAULT '0',
  `clan_create_expiry_time` decimal(20,0) NOT NULL DEFAULT '0',
  `death_penalty_level` int(2) NOT NULL DEFAULT '0',
  `name_color` varchar(8) NOT NULL DEFAULT '\0\0\0',
  `title_color` varchar(8) NOT NULL DEFAULT '\0\0\0',
  `first_log` int(11) DEFAULT '1',
  `haswhacc` tinyint(3) unsigned NOT NULL DEFAULT '0',
  `whaccid` varchar(45) DEFAULT NULL,
  `whaccpwd` varchar(45) DEFAULT NULL,
  `aio` decimal(1,0) NOT NULL DEFAULT 0,
  `aio_end` decimal(20,0) NOT NULL DEFAULT 0,
  `vip` decimal(1,0) NOT NULL DEFAULT 0,
  `vip_end` decimal(20,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`obj_Id`),
  KEY `clanid` (`clanid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;