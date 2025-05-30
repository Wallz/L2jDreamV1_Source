DROP TABLE IF EXISTS `z_ctf_teams`;
CREATE TABLE `z_ctf_teams` (
  `teamId` int(4) NOT NULL DEFAULT '0',
  `teamName` varchar(255) NOT NULL DEFAULT '',
  `teamX` int(11) NOT NULL DEFAULT '0',
  `teamY` int(11) NOT NULL DEFAULT '0',
  `teamZ` int(11) NOT NULL DEFAULT '0',
  `flagX` int(11) NOT NULL DEFAULT '0',
  `flagY` int(11) NOT NULL DEFAULT '0',
  `flagZ` int(11) NOT NULL DEFAULT '0',
  `teamColor` varchar(6) NOT NULL DEFAULT '0',
  PRIMARY KEY (`teamId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `z_ctf_teams` VALUES ('0', 'Blue', '169088', '-207760', '-3459', '169463', '-207908', '-3451', '0000FF'), ('1', 'Red', '164656', '-206201', '-3614', '164106', '-206171', '-3529', 'FF0000');
