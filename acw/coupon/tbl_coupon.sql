/*
Navicat MySQL Data Transfer

Source Server         : eichong
Source Server Version : 50624
Source Host           : 10.9.2.107:3306
Source Database       : eichong

Target Server Type    : MYSQL
Target Server Version : 50624
File Encoding         : 65001

Date: 2016-08-10 17:36:49
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for tbl_coupon
-- ----------------------------
DROP TABLE IF EXISTS `tbl_coupon`;
CREATE TABLE `tbl_coupon` (
  `pk_Coupon` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pk_Activity` int(11) NOT NULL DEFAULT '0' COMMENT '活动表主键',
  `pk_CouponVariety` int(11) NOT NULL DEFAULT '0' COMMENT '优惠券品种主键',
  `cp_Status` smallint(4) NOT NULL DEFAULT '0' COMMENT '优惠券状态（1-已使用）',
  `cp_Limitation` smallint(4) NOT NULL DEFAULT '0' COMMENT '电桩限制（1-仅限交流电桩，2-仅限直流电装，3-不限充电桩）',
  `cp_CouponValue` int(4) NOT NULL DEFAULT '0' COMMENT '优惠券面值',
  `cp_CouponCondition` int(4) NOT NULL DEFAULT '0' COMMENT '优惠券使用条件',
  `cp_CouponCode` varchar(10) NOT NULL DEFAULT '' COMMENT '优惠码',
  `cp_UserId` int(11) NOT NULL DEFAULT '0' COMMENT '持有人（用户ID）',
  `cp_BeginDate` date NOT NULL DEFAULT '1900-01-01' COMMENT '生效时间',
  `cp_EndDate` date NOT NULL DEFAULT '1900-01-01' COMMENT '失效时间',
  `cp_Createdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `cp_Updatedate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`pk_Coupon`),
  UNIQUE KEY `cp_CouponCode` (`cp_CouponCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='优惠券表';

-- ----------------------------
-- Records of tbl_coupon
-- ----------------------------
