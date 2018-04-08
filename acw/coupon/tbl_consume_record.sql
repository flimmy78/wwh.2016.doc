-- ----------------------------
-- Table structure for tbl_consume_record
-- ----------------------------
DROP TABLE IF EXISTS `tbl_consume_record`;
CREATE TABLE `tbl_consume_record` (
  `pk_consume_record` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ep_code` varchar(40) NOT NULL COMMENT '桩体编号',
  `ep_gun_no` smallint(4) NOT NULL DEFAULT '0' COMMENT '枪口编号(1,2,3……)',
  `transaction_number` varchar(40) NOT  NULL COMMENT '交易流水号',
  `account_type` smallint(4) NOT NULL DEFAULT '0' COMMENT '账号类型',
  `user_origin` smallint(4) NOT NULL DEFAULT '0' COMMENT '用户来源',
  `user_account` varchar(50)  NULL COMMENT '用户编号',
  `trans_type` smallint(4) NOT NULL COMMENT '离线交易类型',
  `begin_charge_time` int(11) NOT NULL DEFAULT '0' COMMENT '充电开始时间',
  `end_charge_time` int(11) NOT NULL DEFAULT '0' COMMENT '充电结束时间',
  `tip_power` int(11) NOT NULL DEFAULT '0' COMMENT '尖时段用电度数',
  `tip_money` int(11) NOT NULL DEFAULT '0' COMMENT '尖时段金额',
  `peak_power` int(11) NOT NULL DEFAULT '0' COMMENT '峰时段用电度数',
  `peak_money` int(11) NOT NULL DEFAULT '0' COMMENT '峰时段金额',
  `usual_power` int(11) NOT NULL DEFAULT '0' COMMENT '平时段用电度数',
  `usual_money` int(11) NOT NULL DEFAULT '0' COMMENT '平时段金额',
  `valley_power` int(11) NOT NULL DEFAULT '0' COMMENT '谷时段用电度数',
  `valley_money` int(11) NOT NULL DEFAULT '0' COMMENT '谷时段金额',
  `total_power` int(11) NOT NULL DEFAULT '0' COMMENT '总电量',
  `charge_money` int(11) NOT NULL DEFAULT '0' COMMENT '总充电金额',
  `service_money` int(11) NOT NULL DEFAULT '0' COMMENT '充电服务费',
  `start_meter_num` int(11) NOT NULL DEFAULT '0' COMMENT '开始充电总电量',
  `end_meter_num` int(11) NOT NULL DEFAULT '0' COMMENT '结束充电总电量',
  `stop_cause` smallint(4) NOT NULL DEFAULT '0' COMMENT '停止充电原因',
  `car_vin_code` varchar(40)  NULL COMMENT 'VIN码',
  `start_soc` smallint(4) NULL COMMENT '开始SOC(电池容量)',
  `end_soc` smallint(4) NULL COMMENT '结束SOC(电池容量)',
  `calc_bit_type` smallint(1) unsigned NOT NULL DEFAULT '0' COMMENT '默认两位 0 四位 1 ',
  `opt_flag` smallint(4) NOT NULL DEFAULT '0' COMMENT '操作标志',
  `create_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`pk_consume_record`),
  KEY `index_consume_record_code` (`ep_code`),
  KEY `index_chor_transaction_number` (`transaction_number`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='充电消费订单原始数据';
