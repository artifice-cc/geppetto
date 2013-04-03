CREATE DOMAIN IF NOT EXISTS resultstype AS VARCHAR DEFAULT 'control'
       CHECK VALUE IN('control', 'comparison', 'comparative');

CREATE DOMAIN IF NOT EXISTS valtype AS VARCHAR DEFAULT 'strval'
       CHECK VALUE IN('strval', 'floatval', 'intval');

DROP TABLE IF EXISTS `results_fields`;
CREATE TABLE `results_fields` (
  `rfid` int(11) NOT NULL,
  `simid` int(11) NOT NULL,
  `resultstype` resultstype NOT NULL,
  `field` varchar(255) NOT NULL,
  `valtype` valtype NOT NULL,
  `strval` varchar(255) DEFAULT NULL,
  `floatval` float DEFAULT NULL,
  `intval` int(11) DEFAULT NULL,
  PRIMARY KEY (`rfid`),
);

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

DROP TABLE IF EXISTS `simulations`;
CREATE TABLE `simulations` (
  `simid` int(11) AUTO_INCREMENT,
  `runid` int(11) NOT NULL,
  `controlparams` varchar(2000) NOT NULL,
  `comparisonparams` varchar(2000),
  PRIMARY KEY (`simid`)
);

