package com.zr.workflow.activiti.util;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ProcessDefinitionImpl;

public class JumpActivityCmd implements Command<Object> {

	private String acitivityId;
	private String processInstanceId;
	private String jumpOrigin;

	public JumpActivityCmd(String acitivityId, String processInstanceId) {
		this(acitivityId,processInstanceId,"jump");
	}

	public JumpActivityCmd(String acitivityId, String processInstanceId,String jumpOrigin) {
		this.acitivityId = acitivityId;
		this.processInstanceId = processInstanceId;
		this.jumpOrigin = jumpOrigin;
	}
	

	@Override
	public Object execute(CommandContext commandContext) {
		ExecutionEntity executionEntity = commandContext.getExecutionEntityManager().findExecutionById(processInstanceId);
		executionEntity.destroyScope(jumpOrigin);
		ProcessDefinitionImpl processDefinition = executionEntity.getProcessDefinition();
		ActivityImpl activity = processDefinition.findActivity(acitivityId);
		executionEntity.executeActivity(activity);
		return executionEntity;
	}

}
