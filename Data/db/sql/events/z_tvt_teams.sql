DROP TABLE IF EXISTS `z_tvt_teams`;
CREATE TABLE `z_tvt_teams` (
  `teamId` int(4) NOT NULL DEFAULT '0',
  `teamName` varchar(255) NOT NULL DEFAULT '',
  `teamX` int(11) NOT NULL DEFAULT '0',
  `teamY` int(11) NOT NULL DEFAULT '0',
  `teamZ` int(11) NOT NULL DEFAULT '0',
  `teamColor` varchar(6) NOT NULL DEFAULT '0',
  PRIMARY KEY (`teamId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `z_tvt_teams` VALUES ('0', 'Blue', '148385', '46719', '-3413', '0000FF'), ('1', 'Red', '150572', '46738', '-3413', 'FF0000'), ('2', 'Green', '149409', '45605', '-3413', '00FF00');
