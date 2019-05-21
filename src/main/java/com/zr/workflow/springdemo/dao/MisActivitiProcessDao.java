package com.zr.workflow.springdemo.dao;

import java.util.HashMap;

import org.apache.ibatis.annotations.Param;

public interface MisActivitiProcessDao {

	int saveCurrentActiviti(@Param("processInstanceId")String processInstanceId, @Param("json")String json);
	HashMap<String, String> getCurrentActiviti(String processInstanceId);
	void deleteActiviti(String processInstanceId);

}
