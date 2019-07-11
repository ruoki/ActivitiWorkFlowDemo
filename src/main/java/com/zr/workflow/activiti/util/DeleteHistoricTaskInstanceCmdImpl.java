package com.zr.workflow.activiti.util;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.impl.cmd.DeleteHistoricTaskInstanceCmd;
import org.activiti.engine.impl.interceptor.CommandContext;

public class DeleteHistoricTaskInstanceCmdImpl extends DeleteHistoricTaskInstanceCmd {


    public DeleteHistoricTaskInstanceCmdImpl(String taskId) {
        super(taskId);
    }

    @Override
    public Object execute(CommandContext commandContext) {

        if (taskId == null) {
            throw new ActivitiIllegalArgumentException("taskId is null");
        }
        deleteHistoricTaskInstanceById(taskId,commandContext);
        return null;
    }

    private void deleteHistoricTaskInstanceById(String taskId,CommandContext commandContext){
        CustomHistoricTaskInstanceEntityManager historicTaskInstanceEntityManager = (CustomHistoricTaskInstanceEntityManager) commandContext.getHistoricTaskInstanceEntityManager();
        historicTaskInstanceEntityManager.deleteHistoricTaskInstanceById(taskId,false);
    }
}
