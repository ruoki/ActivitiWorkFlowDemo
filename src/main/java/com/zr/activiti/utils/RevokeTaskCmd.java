package com.zr.activiti.utils;

import java.util.Date;
import java.util.List;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.HistoricActivityInstanceQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.cmd.GetDeploymentProcessDefinitionCmd;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


/**
 * Activiti 命令拦截器 Command
 *
 */

@Component
public class RevokeTaskCmd implements Command<Integer> {

	private static final Logger logger = LoggerFactory.getLogger(RevokeTaskCmd.class);
	
    private RepositoryService repositoryService;
    private HistoryService historyService;
	
    private RuntimeService runtimeService;
    private TaskService taskService;

    /**
     * 撤回的执行人id
     */
	private String userId;
    /**
     * 撤回的目标taskId
     */
	private String historyTaskId;
	
	/**
	 * 撤回所在的流程实例
	 */
	private String processInstanceId;
	
	/**
	 * 撤回的目标节点
	 */
	private String historyTaskDefKey;
	
	/**
	 * 被撤回的节点id
	 */
	private String nextTaskDefKey;
	public RevokeTaskCmd(){
		
	}
	
	public RevokeTaskCmd(String userId,String historyTaskId, String processInstanceId, String taskDefinitionKey,String nextTaskDefKey){
		this.userId = userId;
		this.historyTaskId = historyTaskId;
		this.processInstanceId = processInstanceId;
		this.historyTaskDefKey = taskDefinitionKey;
		this.nextTaskDefKey = nextTaskDefKey;
		initServices();
	}


	private void initServices() {
		if (null == repositoryService) {
			repositoryService = ApplicationContextHandler.getBean(RepositoryService.class);
		}
		if (null == historyService) {
			historyService = ApplicationContextHandler.getBean(HistoryService.class);
		}

		if (null == runtimeService) {
			runtimeService = ApplicationContextHandler.getBean(RuntimeService.class);
		}
		if (null == taskService) {
			taskService = ApplicationContextHandler.getBean(TaskService.class);
		}

	}
	
	/**
	 * 撤回，一次只能撤回一个节点，以下几种情况不能撤回
	 * 0-撤销成功 1-流程结束 2-下一结点已经通过,不能撤销 3-非当前用户提交,不能撤销 
	 * @param historyTaskId
	 * @param processInstanceId
	 * @return
	 */
	@Override
	public Integer execute(CommandContext commandContext) {
		try {
	        ProcessInstance processInstance = this.runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
	        List<HistoricTaskInstance> htiList = null;
	        if(processInstance != null){
	        	HistoricTaskInstance task = this.historyService.createHistoricTaskInstanceQuery().taskId(historyTaskId).singleResult();
	    		String assignee = task.getAssignee();
	    		System.out.println("revokeTask assignee:"+assignee+";current userId:"+userId);
	    		if(!this.userId.equals(assignee)) {
	    			return 3;
	    		}
	        	htiList = this.historyService.createHistoricTaskInstanceQuery().taskDefinitionKey(nextTaskDefKey).list();
	        	int completeCount = 0;
	        	for (HistoricTaskInstance hti : htiList) {//多实例节点由多个任务，全部执行完成才认为已经通过
	        		if("completed".equals(hti.getDeleteReason())){
	        			completeCount += 1;
		        	}
				}
	        	if(completeCount > 0 && completeCount == htiList.size()) {
	        		logger.info("cannot revoke {}", historyTaskId);
	        		return 2;
	        	}
	        }else{
	        	return 1;
	        }
	        if(htiList == null)return -1;
	    	//删除历史评论
	        Context.getCommandContext().getCommentEntityManager().deleteCommentsByTaskId(historyTaskId);
	        //删除被撤回的节点、历史节点、历史任务、历史评论
	        this.deleteHistoryActivities(htiList,this.historyTaskId, this.processInstanceId,this.historyTaskDefKey);
	        // 恢复期望撤销的任务和历史
	        this.processHistoryTask(historyTaskId);
	        return 0;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

    /**
     * 删除被撤回的节点、历史节点、历史任务、历史评论
     * @param historyTaskDefKey
     */
    public void deleteHistoryActivities(List<HistoricTaskInstance> htiList,String historyTaskId, String processInstanceId, String historyTaskDefKey) {
    	JdbcTemplate jdbcTemplate = ApplicationContextHandler.getBean(JdbcTemplate.class);
    	//List<HistoricActivityInstance> list = this.historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();
//    	List<HistoricActivityInstance> list = this.historyService.createHistoricActivityInstanceQuery().unfinished().processInstanceId(processInstanceId).list();

//    	for(HistoricActivityInstance hai : list){
//    		String taskId = hai.getTaskId();
//    		if(taskId != null && !taskId.equals(historyTaskId)){
//    			//获取当前节点
//    			Task currentTask = this.taskService.createTaskQuery().taskId(taskId).singleResult();
//    	        // 删除所有正在活动中的task
//    	        Command<Void> cmd = new DeleteActiveTaskCmd((TaskEntity)currentTask, "revoke", true);
//    	        Context.getProcessEngineConfiguration().getManagementService().executeCommand(cmd);
//    			//删除历史任务
//    			Context.getCommandContext()
//                .getHistoricTaskInstanceEntityManager()
//                .deleteHistoricTaskInstanceById(taskId);
//    			
//    			//删除历史行为
//    			jdbcTemplate.update("delete from ACT_HI_ACTINST where task_id_=?", taskId);
//    		}
//    	}
    	
    	for (HistoricTaskInstance hti : htiList) {
    		String taskId = hti.getId();
    		if(taskId != null && !taskId.equals(historyTaskId)){

    			//删除历史评论
    	        Context.getCommandContext().getCommentEntityManager().deleteCommentsByTaskId(taskId);
    	        
    	        if(!"completed".equals(hti.getDeleteReason())){
        			//获取当前节点
        			Task currentTask = this.taskService.createTaskQuery().taskId(taskId).singleResult();
        			if(currentTask != null) {
        				// 删除所有正在活动中的task
        				Command<Void> cmd = new DeleteActiveTaskCmd((TaskEntity)currentTask, "revoke", true);
        				Context.getProcessEngineConfiguration().getManagementService().executeCommand(cmd);
        			}
	        	}
    	        //删除历史任务
    			Context.getCommandContext()
                .getHistoricTaskInstanceEntityManager()
                .deleteHistoricTaskInstanceById(taskId);
    			
    			//删除历史行为
    			jdbcTemplate.update("delete from ACT_HI_ACTINST where task_id_=?", taskId);
    			

    		}
		}

		ActivityImpl nextNodeInfo;
		try {
			nextNodeInfo = ProcessDefinitionCache.get().getNextNodeInfo(repositoryService,runtimeService,processInstanceId,historyTaskDefKey);
			System.out.println("CusTaskListener setAssigneeList nextNodeid:"+nextNodeInfo.getId());
			HistoricActivityInstance exclusiveGatewayActivity = this.historyService.createHistoricActivityInstanceQuery().activityId(nextNodeInfo.getId()).singleResult();
			if(null != exclusiveGatewayActivity) {
				//删除历史行为
				jdbcTemplate.update("delete from ACT_HI_ACTINST where act_id_=?", exclusiveGatewayActivity.getActivityId());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    /**
     * 恢复任务
     * @param historicTaskInstanceEntity
     * @param historicActivityInstanceEntity
     */
    public void processHistoryTask(String taskId) {
    	
        //获得目标节点的历史任务
    	HistoricTaskInstanceEntity historicTaskInstanceEntity = Context
                .getCommandContext()
                .getHistoricTaskInstanceEntityManager()
                .findHistoricTaskInstanceById(taskId);
    	
    	// 获得目标节点的历史节点
        HistoricActivityInstanceEntity historicActivityInstanceEntity = getHistoricActivityInstanceEntity(taskId);
    	
        historicTaskInstanceEntity.setEndTime(null);
        historicTaskInstanceEntity.setDurationInMillis(null);
        historicActivityInstanceEntity.setEndTime(null);
        historicActivityInstanceEntity.setDurationInMillis(null);
        historicActivityInstanceEntity.setDeleteReason(null);
        
        TaskEntity task = TaskEntity.create(new Date());
        task.setProcessDefinitionId(historicTaskInstanceEntity.getProcessDefinitionId());
        task.setId(historicTaskInstanceEntity.getId());
        task.setAssigneeWithoutCascade(historicTaskInstanceEntity.getAssignee());
        task.setParentTaskIdWithoutCascade(historicTaskInstanceEntity.getParentTaskId());
        task.setNameWithoutCascade(historicTaskInstanceEntity.getName());
        task.setTaskDefinitionKey(historicTaskInstanceEntity.getTaskDefinitionKey());
        task.setExecutionId(historicTaskInstanceEntity.getExecutionId());
        task.setPriority(historicTaskInstanceEntity.getPriority());
        task.setProcessInstanceId(historicTaskInstanceEntity.getProcessInstanceId());
        task.setDescriptionWithoutCascade(historicTaskInstanceEntity.getDescription());

        Context.getCommandContext().getTaskEntityManager().insert(task);

        // 把流程指向任务对应的节点
        ExecutionEntity executionEntity = Context.getCommandContext()
                .getExecutionEntityManager()
                .findExecutionById(historicTaskInstanceEntity.getExecutionId());
        executionEntity.setActivity(getActivity(historicActivityInstanceEntity));
        logger.info("activiti is revoke {}", historicTaskInstanceEntity.getName());
    }

	/**
	 * 获得目标节点的历史节点
	 * @param historyTaskId
	 * @return
	 */
	public HistoricActivityInstanceEntity getHistoricActivityInstanceEntity(
            String historyTaskId) {
    	JdbcTemplate jdbcTemplate = ApplicationContextHandler.getBean(JdbcTemplate.class);
        String historicActivityInstanceId = jdbcTemplate.queryForObject(
                "select id_ from ACT_HI_ACTINST where task_id_=?",
                String.class, historyTaskId);
        logger.info("historicActivityInstanceId : {}",historicActivityInstanceId);

        HistoricActivityInstanceQueryImpl historicActivityInstanceQueryImpl = new HistoricActivityInstanceQueryImpl();
        historicActivityInstanceQueryImpl.activityInstanceId(historicActivityInstanceId);

        HistoricActivityInstanceEntity historicActivityInstanceEntity = (HistoricActivityInstanceEntity) Context
                .getCommandContext()
                .getHistoricActivityInstanceEntityManager()
                .findHistoricActivityInstancesByQueryCriteria(
                        historicActivityInstanceQueryImpl, new Page(0, 1))
                .get(0);

        return historicActivityInstanceEntity;
    }
    
    public ActivityImpl getActivity(
            HistoricActivityInstance historicActivityInstanceEntity) {
        ProcessDefinitionEntity processDefinitionEntity = new GetDeploymentProcessDefinitionCmd(
                historicActivityInstanceEntity.getProcessDefinitionId())
                .execute(Context.getCommandContext());

        return processDefinitionEntity
                .findActivity(historicActivityInstanceEntity.getActivityId());
    }

}
