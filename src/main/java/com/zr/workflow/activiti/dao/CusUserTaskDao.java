package com.zr.workflow.activiti.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zr.workflow.activiti.entity.CusUserTask;


public interface CusUserTaskDao {

	void add(CusUserTask userTask);

	void update(CusUserTask userTask);

	void deleteByProcDefKey(@Param("procDefKey")String procDefKey);

	Integer deleteAll();

	CusUserTask getBeanById(Integer id);

	List<CusUserTask> findByProcDefKey(String procDefKey);
	List<CusUserTask> findByProcAndActivityType(@Param("procDefKey")String procDefKey,@Param("activityType")String activityType);

	CusUserTask findByProcAndTask(@Param("procDefKey")String procDefKey,@Param("taskDefKey")String taskDefKey);

	List<CusUserTask> getAllList();

}
