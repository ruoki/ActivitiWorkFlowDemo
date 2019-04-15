package com.zr.workflow.activiti.service;

import com.zr.workflow.activiti.entity.BaseVO;
import com.zr.workflow.activiti.entity.CusUserTask;

/**
 * 自定义流程的接口
 * 设置流程用户节点执行人时，可根据项目所需实现该接口的setUserTaskAssgine方法动态设置节点执行人
 * @author zhourq
 *
 */
public interface CusProcess {
	public void setUserTaskAssgine(BaseVO baseVO, CusUserTask cusUserTask,String assigneeExpression) throws Exception;
}
