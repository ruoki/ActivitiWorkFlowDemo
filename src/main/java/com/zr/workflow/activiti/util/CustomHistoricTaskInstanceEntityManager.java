package com.zr.workflow.activiti.util;

import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomHistoricTaskInstanceEntityManager extends HistoricTaskInstanceEntityManager {

    public void deleteHistoricTaskInstanceById(String taskId) {
          deleteHistoricTaskInstanceById(taskId,true);
    }

    public void deleteHistoricTaskInstanceById(String taskId,boolean deleteComment) {
        if(deleteComment){
            super.deleteHistoricTaskInstanceById(taskId);
        }else{
            deleteHistoricTaskInstanceByIdWithoutComment(taskId);
        }
    }

    /**
     * 删除历史任务表，不删除评论
     * @param taskId
     */
    private void deleteHistoricTaskInstanceByIdWithoutComment(String taskId) {
        if (getHistoryManager().isHistoryEnabled()) {
            HistoricTaskInstanceEntity historicTaskInstance = findHistoricTaskInstanceById(taskId);
            if (historicTaskInstance != null) {
                CommandContext commandContext = Context.getCommandContext();

                List<HistoricTaskInstance> subTasks = findHistoricTasksByParentTaskId(taskId);
                for (HistoricTaskInstance subTask: subTasks) {
                    deleteHistoricTaskInstanceById(subTask.getId());
                }

                commandContext
                        .getHistoricDetailEntityManager()
                        .deleteHistoricDetailsByTaskId(taskId);

                commandContext
                        .getHistoricVariableInstanceEntityManager()
                        .deleteHistoricVariableInstancesByTaskId(taskId);

                commandContext
                        .getAttachmentEntityManager()
                        .deleteAttachmentsByTaskId(taskId);

                commandContext.getHistoricIdentityLinkEntityManager()
                        .deleteHistoricIdentityLinksByTaskId(taskId);

                getDbSqlSession().delete(historicTaskInstance);
            }
        }
    }

}
