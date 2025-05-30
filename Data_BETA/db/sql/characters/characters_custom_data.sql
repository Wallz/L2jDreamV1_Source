DROP TABLE IF EXISTS `characters_custom_data`;
CREATE TABLE `characters_custom_data` (
  `obj_Id` decimal(11,0) NOT NULL default '0',
  `char_name` varchar(35) NOT NULL default '',
  `hero` decimal(1,0) NOT NULL default '0',
  `noble` decimal(1,0) NOT NULL default '0',
  `donator` decimal(1,0) NOT NULL default '0',
  `hero_end_date` BIGINT NOT NULL default '0',
  PRIMARY KEY  (`obj_Id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;