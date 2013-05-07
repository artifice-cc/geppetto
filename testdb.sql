DROP TABLE IF EXISTS `runs`;
CREATE TABLE `runs` (
  `runid` int(11) AUTO_INCREMENT,
  `paramid` int(11) NOT NULL,
  `branch` varchar(255) NOT NULL,
  `commit` varchar(255) NOT NULL,
  `commitmsg` varchar(2000) NOT NULL,
  `commitdate` datetime NOT NULL,
  `datadir` varchar(1024) NOT NULL,
  `starttime` datetime NOT NULL,
  `endtime` datetime NOT NULL,
  `hostname` varchar(45) NOT NULL,
  `nthreads` int(11) NOT NULL,
  `pwd` varchar(1024) NOT NULL,
  `recorddir` varchar(1024) NOT NULL,
  `repetitions` int(11) NOT NULL,
  `seed` int(11) NOT NULL,
  `username` varchar(255) NOT NULL,
  `project` varchar(255) DEFAULT NULL,
  `simcount` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`runid`),
);

DROP TABLE IF EXISTS `parameters`;
CREATE TABLE `parameters` (
  `paramid` int(11) AUTO_INCREMENT,
  `problem` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `rev` int(11) NOT NULL DEFAULT '1',
  `comparison` varchar(2000),
  `control` varchar(2000),
  `description` varchar(2000),
  PRIMARY KEY (`paramid`),
);

