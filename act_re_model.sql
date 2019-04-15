/*
Navicat MySQL Data Transfer

Source Server         : zrq
Source Server Version : 50721
Source Host           : localhost:3306
Source Database       : vue_yilion

Target Server Type    : MYSQL
Target Server Version : 50721
File Encoding         : 65001

Date: 2019-04-10 14:28:56
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for act_re_model
-- ----------------------------
DROP TABLE IF EXISTS `act_re_model`;
CREATE TABLE `act_re_model` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LAST_UPDATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `VERSION_` int(11) DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EDITOR_SOURCE_VALUE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EDITOR_SOURCE_EXTRA_VALUE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_MODEL_SOURCE` (`EDITOR_SOURCE_VALUE_ID_`),
  KEY `ACT_FK_MODEL_SOURCE_EXTRA` (`EDITOR_SOURCE_EXTRA_VALUE_ID_`),
  KEY `ACT_FK_MODEL_DEPLOYMENT` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_FK_MODEL_DEPLOYMENT` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `act_re_deployment` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE` FOREIGN KEY (`EDITOR_SOURCE_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`),
  CONSTRAINT `ACT_FK_MODEL_SOURCE_EXTRA` FOREIGN KEY (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) REFERENCES `act_ge_bytearray` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
-- Records of act_re_model
-- ----------------------------
INSERT INTO `act_re_model` VALUES ('70001', '4', '工时填报（项目外包/行内人员）模型', '70001', null, '2019-04-09 14:20:34.965', '2019-04-09 14:35:49.957', '1', '{\"name\":\"工时填报（项目外包/行内人员）模型\",\"revision\":1,\"description\":\"工时填报（项目外包/行内人员）流程，由项目外包人员或行内人员发起申请，项目经理审批，需要所有的项目的经理通过后，才可通过；进入下一节点，归档于项目经理和项目外包人员或行内人员\"}', null, '70002', '70003', '');
INSERT INTO `act_re_model` VALUES ('77501', '5', '需求提交流程模型', '77501', null, '2019-04-09 17:30:01.152', '2019-04-09 18:03:00.319', '1', '{\"name\":\"需求提交流程模型\",\"revision\":1,\"description\":\"需求提交流程：由业务经理提出并上传需求说明书，并判断项目是否已立项，未立项则由架构师经理立项，pmo进行实际立项；立项后发给业务经理重新发起申请；由产品创新中心审批；产创审批人如果退回，则业务经理重新上传需求说明书；产创审批人如果确认则经产品经理审批；产品经理下载需求说明书进行拆分明细后重命名，然后上传，并指定各系统负责人，即业务经理确认；业务经理如果驳回，则退回至产品经理，业务经理如果确认通过，则流程归档至发起人。\"}', null, '77502', '77503', '');
