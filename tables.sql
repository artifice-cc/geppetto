-- MySQL dump 10.13  Distrib 5.5.31, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: retrospect
-- ------------------------------------------------------
-- Server version	5.5.31-1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `parameters`
--

DROP TABLE IF EXISTS `parameters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `parameters` (
  `paramid` int(11) NOT NULL AUTO_INCREMENT,
  `problem` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `rev` int(11) NOT NULL DEFAULT '1',
  `comparison` text,
  `control` text,
  `description` text,
  PRIMARY KEY (`paramid`),
  KEY `PROBLEM` (`problem`),
  KEY `NAME` (`name`),
  KEY `REVISION` (`rev`,`paramid`)
) ENGINE=InnoDB AUTO_INCREMENT=691 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `runs`
--

DROP TABLE IF EXISTS `runs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `runs` (
  `runid` int(11) NOT NULL AUTO_INCREMENT,
  `paramid` int(11) NOT NULL,
  `branch` varchar(255) NOT NULL,
  `commit` varchar(255) NOT NULL,
  `commitmsg` text NOT NULL,
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
  `simcount` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`runid`),
  KEY `PARAMS` (`paramid`)
) ENGINE=InnoDB AUTO_INCREMENT=580 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-06-20 22:31:34
