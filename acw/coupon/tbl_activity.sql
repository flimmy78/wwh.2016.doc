/*
Navicat MySQL Data Transfer

Source Server         : eichong
Source Server Version : 50624
Source Host           : 10.9.2.107:3306
Source Database       : eichong

Target Server Type    : MYSQL
Target Server Version : 50624
File Encoding         : 65001

Date: 2016-08-10 17:36:35
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for tbl_activity
-- ----------------------------
DROP TABLE IF EXISTS `tbl_activity`;
CREATE TABLE `tbl_activity` (
  `pk_Activity` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `act_ActivityName` varchar(50) NOT NULL DEFAULT '' COMMENT '活动名称',
  `act_Type` int(11) NOT NULL DEFAULT '0' COMMENT '活动类型：1-用户活动，2-大客户活动',
  `act_ChannelType` int(11) NOT NULL DEFAULT '0' COMMENT '渠道类型（tbl_company）',
  `act_Status` smallint(4) NOT NULL DEFAULT '0' COMMENT '状态（保留）',
  `act_ActivityRule` int(11) NOT NULL DEFAULT '0' COMMENT '活动规则（1-注册送现金券活动，2-首次体验享现金券，3-邀请注册返现金券活动，4-邀请首次消费返现金券活动）',
  `act_CreateUserId` varchar(20) NOT NULL DEFAULT '' COMMENT '创建人',
  `act_UpdateUserId` varchar(20) NOT NULL DEFAULT '' COMMENT '操作人',
  `act_Remark` varchar(200) NOT NULL DEFAULT '' COMMENT '备注（奖品发放规则）',
  `act_BiginDate` date NOT NULL DEFAULT '1900-01-01' COMMENT '活动开始时间',
  `act_EndDate` date NOT NULL DEFAULT '1900-01-01' COMMENT '活动结束时间',
  `act_Createdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `act_Updatedate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`pk_Activity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='活动表';
