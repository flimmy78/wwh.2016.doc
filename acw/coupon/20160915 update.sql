ALTER TABLE tbl_chargingorder ADD COLUMN `chOr_CouponMoney` DECIMAL (8, 2) NOT NULL DEFAULT '0.00' COMMENT 'ÓÅ»ÝÈ¯µÖ¿Û½ð¶î';

ALTER TABLE tbl_chargingorder ADD COLUMN `pk_Coupon` INT (11) NOT NULL DEFAULT '0' COMMENT 'ÓÅ»ÝÂëÖ÷¼ü';

ALTER TABLE tbl_purchasehistory MODIFY COLUMN `puHi_Type` INT (11) NOT NULL COMMENT 'Ïû·ÑÀàÐÍ 1-³äµçÏû·Ñ 2-Ô¤Ô¼Ïû·Ñ 3-¹ºÎïÏû·Ñ 4-³äÖµ 5-·¢Æ±ÓÊ·Ñ 6-ÓÅ»ÝÈ¯µÖ¿Û';

DROP TABLE
IF EXISTS tbl_activity;

CREATE TABLE `tbl_activity` (
	`pk_Activity` INT (11) NOT NULL AUTO_INCREMENT COMMENT 'Ö÷¼ü',
	`act_ActivityName` VARCHAR (50) NOT NULL DEFAULT '' COMMENT '»î¶¯Ãû³Æ',
	`act_Type` INT (11) NOT NULL DEFAULT '0' COMMENT '»î¶¯ÀàÐÍ£º1-ÓÃ»§»î¶¯£¬2-´ó¿Í»§»î¶¯',
	`act_ChannelType` INT (11) NOT NULL DEFAULT '0' COMMENT 'ÇþµÀÀàÐÍ£¨tbl_company£©',
	`act_Status` SMALLINT (4) NOT NULL DEFAULT '0' COMMENT '×´Ì¬£¨±£Áô£©',
	`act_ActivityRule` INT (11) NOT NULL DEFAULT '0' COMMENT '»î¶¯¹æÔò£¨1-×¢²áËÍÏÖ½ðÈ¯»î¶¯£¬2-Ê×´ÎÌåÑéÏíÏÖ½ðÈ¯£¬3-ÑûÇë×¢²á·µÏÖ½ðÈ¯»î¶¯£¬4-ÑûÇëÊ×´ÎÏû·Ñ·µÏÖ½ðÈ¯»î¶¯£©',
	`act_CreateUserId` VARCHAR (20) NOT NULL DEFAULT '' COMMENT '´´½¨ÈË',
	`act_UpdateUserId` VARCHAR (20) NOT NULL DEFAULT '' COMMENT '²Ù×÷ÈË',
	`act_Remark` VARCHAR (200) NOT NULL DEFAULT '' COMMENT '±¸×¢£¨½±Æ··¢·Å¹æÔò£©',
	`act_BiginDate` date NOT NULL DEFAULT '1900-01-01' COMMENT '»î¶¯¿ªÊ¼Ê±¼ä',
	`act_EndDate` date NOT NULL DEFAULT '1900-01-01' COMMENT '»î¶¯½áÊøÊ±¼ä',
	`act_Createdate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '´´½¨Ê±¼ä',
	`act_Updatedate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'ÐÞ¸ÄÊ±¼ä',
	PRIMARY KEY (`pk_Activity`)
) ENGINE = INNODB DEFAULT CHARSET = utf8 COMMENT = '»î¶¯±í';

DROP TABLE
IF EXISTS tbl_activityschedule;

CREATE TABLE `tbl_activityschedule` (
	`pk_ActivitySchedule` INT (11) NOT NULL AUTO_INCREMENT COMMENT 'Ö÷¼ü',
	`pk_Activity` INT (11) NOT NULL DEFAULT '0' COMMENT '»î¶¯±íÖ÷¼ü',
	`pk_CouponVariety` INT (11) NOT NULL DEFAULT '0' COMMENT 'ÓÅ»ÝÈ¯Æ·ÖÖÖ÷¼ü',
	`acsc_num` INT (11) NOT NULL DEFAULT '0' COMMENT 'ÓÅ»ÝÈ¯ÊýÁ¿',
	`acsc_Createdate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '´´½¨Ê±¼ä',
	`acsc_Updatedate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'ÐÞ¸ÄÊ±¼ä',
	PRIMARY KEY (`pk_ActivitySchedule`)
) ENGINE = INNODB DEFAULT CHARSET = utf8 COMMENT = '»î¶¯¸½±í';

DROP TABLE
IF EXISTS tbl_couponvariety;

CREATE TABLE `tbl_couponvariety` (
	`pk_CouponVariety` INT (11) NOT NULL AUTO_INCREMENT COMMENT 'Ö÷¼ü',
	`cova_ActivityName` VARCHAR (50) NOT NULL DEFAULT '' COMMENT '»î¶¯Ãû³Æ',
	`cova_Limitation` SMALLINT (4) NOT NULL DEFAULT '0' COMMENT 'µç×®ÏÞÖÆ£¨1-½öÏÞ½»Á÷µç×®£¬2-½öÏÞÖ±Á÷µç×°£¬3-²»ÏÞ³äµç×®£©',
	`cova_Stutas` SMALLINT (4) NOT NULL DEFAULT '0' COMMENT '×´Ì¬£¨0-ÉÏ¼Ü£¬1-ÏÂ¼Ü£©',
	`cova_CouponValue` INT (4) NOT NULL DEFAULT '0' COMMENT 'ÓÅ»ÝÈ¯ÃæÖµ',
	`cova_CouponCondition` INT (4) NOT NULL DEFAULT '0' COMMENT 'ÓÅ»ÝÈ¯Ê¹ÓÃÌõ¼þ',
	`cova_CouponTerm` INT (4) NOT NULL DEFAULT '0' COMMENT 'ÓÐÐ§ÆÚ£¨Ìì£©',
	`cova_CreateUserId` VARCHAR (20) NOT NULL DEFAULT '' COMMENT '´´½¨ÈË',
	`cova_UpdateUserId` VARCHAR (20) NOT NULL DEFAULT '' COMMENT '²Ù×÷ÈË',
	`cova_Remark` VARCHAR (200) NOT NULL DEFAULT '' COMMENT '±¸×¢',
	`cova_Createdate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '´´½¨Ê±¼ä',
	`cova_Updatedate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'ÐÞ¸ÄÊ±¼ä',
	PRIMARY KEY (`pk_CouponVariety`)
) ENGINE = INNODB DEFAULT CHARSET = utf8 COMMENT = 'ÓÅ»ÝÈ¯Æ·ÖÖ±í';

DROP TABLE
IF EXISTS tbl_coupon;

CREATE TABLE `tbl_coupon` (
	`pk_Coupon` INT (11) NOT NULL AUTO_INCREMENT COMMENT 'Ö÷¼ü',
	`pk_Activity` INT (11) NOT NULL DEFAULT '0' COMMENT '»î¶¯±íÖ÷¼ü',
	`pk_CouponVariety` INT (11) NOT NULL DEFAULT '0' COMMENT 'ÓÅ»ÝÈ¯Æ·ÖÖÖ÷¼ü',
	`cp_Status` SMALLINT (4) NOT NULL DEFAULT '0' COMMENT 'ÓÅ»ÝÈ¯×´Ì¬£¨1-ÒÑÊ¹ÓÃ£©',
	`cp_Limitation` SMALLINT (4) NOT NULL DEFAULT '0' COMMENT 'µç×®ÏÞÖÆ£¨1-½öÏÞ½»Á÷µç×®£¬2-½öÏÞÖ±Á÷µç×°£¬3-²»ÏÞ³äµç×®£©',
	`cp_CouponValue` INT (4) NOT NULL DEFAULT '0' COMMENT 'ÓÅ»ÝÈ¯ÃæÖµ',
	`cp_CouponCondition` INT (4) NOT NULL DEFAULT '0' COMMENT 'ÓÅ»ÝÈ¯Ê¹ÓÃÌõ¼þ',
	`cp_CouponCode` VARCHAR (10) NOT NULL DEFAULT '' COMMENT 'ÓÅ»ÝÂë',
	`cp_UserId` INT (11) NOT NULL DEFAULT '0' COMMENT '³ÖÓÐÈË£¨ÓÃ»§ID£©',
	`cp_BeginDate` date NOT NULL DEFAULT '1900-01-01' COMMENT 'ÉúÐ§Ê±¼ä',
	`cp_EndDate` date NOT NULL DEFAULT '1900-01-01' COMMENT 'Ê§Ð§Ê±¼ä',
	`cp_Createdate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '´´½¨Ê±¼ä',
	`cp_Updatedate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'ÐÞ¸ÄÊ±¼ä',
	PRIMARY KEY (`pk_Coupon`),
	UNIQUE KEY `cp_CouponCode` (`cp_CouponCode`)
) ENGINE = INNODB DEFAULT CHARSET = utf8 COMMENT = 'ÓÅ»ÝÈ¯±í';

