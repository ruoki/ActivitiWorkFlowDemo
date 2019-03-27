package com.zr.activiti.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zr.activiti.entity.BaseVO;



public interface BaseVODao {
	void save(BaseVO apply);
	List<BaseVO> getByBusinessKey(String businessKey);
	List<BaseVO> getByUserId(String userId);
	void update(BaseVO app);

	void deleteByUserId(@Param("createId")String createId);
	void deleteByProcessInstanceId(@Param("processInstanceId")String processInstanceId);
	void deleteByBusinessKey(@Param("businessKey")String businessKey);

	Integer deleteAll();
}
