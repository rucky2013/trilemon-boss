# ************************************************************
# Sequel Pro SQL dump
# Version 4096
#
# http://www.sequelpro.com/
# http://code.google.com/p/sequel-pro/
#
# Host: 127.0.0.1 (MySQL 5.5.27)
# Database: trilemon-boss-infra-sync-2
# Generation Time: 2013-12-10 12:42:06 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table calc_seller_day_rate_2
# ------------------------------------------------------------

DROP TABLE IF EXISTS `calc_seller_day_rate_2`;

CREATE TABLE `calc_seller_day_rate_2` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(11) unsigned NOT NULL DEFAULT '0',
  `good_rate_num` int(11) unsigned NOT NULL DEFAULT '0',
  `neutral_rate_num` int(11) unsigned NOT NULL DEFAULT '0',
  `bad_rate_num` int(11) unsigned NOT NULL DEFAULT '0',
  `rate_time` date NOT NULL DEFAULT '0000-00-00',
  `add_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `upd_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table sync_rate_3
# ------------------------------------------------------------

DROP TABLE IF EXISTS `sync_rate_3`;

CREATE TABLE `sync_rate_3` (
  `user_id` bigint(11) unsigned NOT NULL,
  `num_iid` bigint(11) unsigned NOT NULL DEFAULT '0',
  `valid_score` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `tid` bigint(11) unsigned NOT NULL DEFAULT '0',
  `oid` bigint(11) unsigned NOT NULL DEFAULT '0',
  `role` varchar(8) NOT NULL DEFAULT '',
  `nick` varchar(32) NOT NULL DEFAULT '',
  `result` varchar(8) NOT NULL DEFAULT '',
  `created` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `rated_nick` varchar(32) NOT NULL DEFAULT '',
  `item_title` varchar(64) NOT NULL DEFAULT '',
  `item_price` decimal(11,2) unsigned NOT NULL DEFAULT '0.00',
  `content` varchar(500) NOT NULL DEFAULT '',
  `reply` varchar(500) NOT NULL DEFAULT '',
  `add_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `upd_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table sync_rate_4
# ------------------------------------------------------------

DROP TABLE IF EXISTS `sync_rate_4`;

CREATE TABLE `sync_rate_4` (
  `user_id` bigint(11) unsigned NOT NULL,
  `num_iid` bigint(11) unsigned NOT NULL DEFAULT '0',
  `valid_score` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `tid` bigint(11) unsigned NOT NULL DEFAULT '0',
  `oid` bigint(11) unsigned NOT NULL DEFAULT '0',
  `role` varchar(8) NOT NULL DEFAULT '',
  `nick` varchar(32) NOT NULL DEFAULT '',
  `result` varchar(8) NOT NULL DEFAULT '',
  `created` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `rated_nick` varchar(32) NOT NULL DEFAULT '',
  `item_title` varchar(64) NOT NULL DEFAULT '',
  `item_price` decimal(11,2) unsigned NOT NULL DEFAULT '0.00',
  `content` varchar(500) NOT NULL DEFAULT '',
  `reply` varchar(500) NOT NULL DEFAULT '',
  `add_time` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `upd_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`oid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
