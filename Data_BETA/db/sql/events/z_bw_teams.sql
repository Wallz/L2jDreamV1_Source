DROP TABLE IF EXISTS `z_bw_teams`;
CREATE TABLE `z_bw_teams` (
  `teamId` int(4) NOT NULL DEFAULT '0',
  `teamName` varchar(255) NOT NULL DEFAULT '',
  `teamX` int(11) NOT NULL DEFAULT '0',
  `teamY` int(11) NOT NULL DEFAULT '0',
  `teamZ` int(11) NOT NULL DEFAULT '0',
  `baseX` int(11) NOT NULL DEFAULT '0',
  `baseY` int(11) NOT NULL DEFAULT '0',
  `baseZ` int(11) NOT NULL DEFAULT '0',
  `teamColor` varchar(6) NOT NULL DEFAULT '0',
  PRIMARY KEY (`teamId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `z_bw_teams` VALUES ('0', 'Blue', '150324', '46730', '-3413', '150629', '46704', '-3411', '0000FF'), ('1', 'Red', '148667', '46726', '-3413', '148369', '46725', '-3413', 'FF0000');
