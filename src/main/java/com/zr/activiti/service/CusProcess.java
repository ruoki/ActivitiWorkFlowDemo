package com.zr.activiti.service;

import com.zr.activiti.entity.BaseVO;
import com.zr.activiti.entity.CusUserTask;

/**
 * 自定义流程的接口
 * @author Administrator
 *
 */
public interface CusProcess {
	public void setUserTaskAssgine(BaseVO baseVO, CusUserTask cusUserTask,String assigneeExpression) throws Exception;
}
