package com.zr.activiti.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.activiti.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import com.zr.activiti.dao.CusUserTaskDao;
import com.zr.activiti.entity.BaseVO;
import com.zr.activiti.entity.CusUserTask;
import com.zr.activiti.utils.ProcessDefinitionCache;
import com.zr.activiti.utils.StringUtil;

@Service
public class CusUserTaskService {

	@Resource
	protected RepositoryService repositoryService;
	@Resource
	RuntimeService runtimeService;
	@Resource
	private ProcessService processService;
	@Resource
	private CusUserTaskDao userTaskDao;
	private CusProcess cusProcess;

	/**
	 * 初始化流程各任务节点信息保存至数据库
	 * 
	 * 根据项目更新任务节点表 设置各节点处理人
	 */
	
	public void initProcessUserTaskInfo(BaseVO baseVO,CusProcess cusProcess) throws Exception {
		this.cusProcess = cusProcess;
		List<ProcessDefinition> processDefinitionList = processService.findDeployedProcessList();
		for (ProcessDefinition processDefinition : processDefinitionList) {
			final String businessKey = baseVO.getBusinessKey();// 设置业务key
			final String procDefKey = businessKey.contains(":") ? businessKey.split(":")[0] : "";
			boolean canHandleActiviti = (null != baseVO && StringUtil.isNotEmpty(procDefKey))
					&& procDefKey.equals(processDefinition.getKey());
			if (canHandleActiviti) {
				initSingleActivitiInfo(processDefinition.getId(), baseVO);
				return;
			}
		}
	}

	/**
	 * 读取用户任务节点信息保存到usertask表
	 * 
	 * @param processDefinition
	 * @param baseVO
	 * @throws Exception
	 */
	private void initSingleActivitiInfo(String processDefinitionId, BaseVO baseVO) throws Exception {

		List<ActivityImpl> activitiList = ProcessDefinitionCache.get().getActivities(processDefinitionId);
		for (ActivityImpl activity : activitiList) {
			ActivityBehavior activityBehavior = activity.getActivityBehavior();
			if (activityBehavior instanceof UserTaskActivityBehavior) {

				initUserTaskActivity(baseVO, (UserTaskActivityBehavior) activityBehavior,CusUserTask.TYPE_NORMAL);

			} else if (activityBehavior instanceof ParallelMultiInstanceBehavior) {

				ParallelMultiInstanceBehavior multiInstanceBehavior = (ParallelMultiInstanceBehavior) activityBehavior;
				ActivityBehavior innerActivityBehavior = multiInstanceBehavior.getInnerActivityBehavior();
				if (innerActivityBehavior instanceof UserTaskActivityBehavior) {
					initUserTaskActivity(baseVO, (UserTaskActivityBehavior) innerActivityBehavior,CusUserTask.TYPE_MULTI);
				}
			}
		}
	}

	/**
	 * 初始化流程各节点的处理人和处理类型
	 * 
	 * @param baseVO
	 * @param userTaskActivityBehavior
	 * @param activityType 
	 * @throws Exception
	 */
	private void initUserTaskActivity(BaseVO baseVO, UserTaskActivityBehavior userTaskActivityBehavior, String activityType)
			throws Exception {
		boolean isFound = false;

		TaskDefinition taskDefinition = userTaskActivityBehavior.getTaskDefinition();
		String assigneeExpression = null == taskDefinition.getAssigneeExpression() ? "" : taskDefinition.getAssigneeExpression().getExpressionText();
		String taskDefKey = taskDefinition.getKey();
		Expression taskName = taskDefinition.getNameExpression();

		final String businessKey = baseVO.getBusinessKey();
		List<CusUserTask> list = findByProcDefKey(businessKey);
		for (CusUserTask cusUserTask : list) {
			if (taskDefKey.equals(cusUserTask.getTaskDefKey())) {
				updateCusUserTaskAssgine(baseVO, cusUserTask,assigneeExpression);
				doUpdate(cusUserTask);
				isFound = true;
				break;
			}
		}
		if (isFound)
			return;
		CusUserTask cusUserTask = initCusUserTask(baseVO, businessKey, taskDefKey, taskName,activityType,assigneeExpression);
		doAdd(cusUserTask);
	}

	/**
	 * 初始化单个用户任务节点：节点key+节点name+节点的执行人类型（assignee/candidateGroup）+节点的执行人
	 * @param baseVO
	 * @param businessKey
	 * @param taskDefKey
	 * @param taskName
	 * @param assigneeExpression
	 * @param activityType 
	 * @return
	 * @throws Exception
	 */
	private CusUserTask initCusUserTask(BaseVO baseVO, final String businessKey, String taskDefKey, Expression taskName, String activityType, String assigneeExpression)
			throws Exception {
		CusUserTask cusUserTask = new CusUserTask();
		cusUserTask.setProcDefKey(businessKey);
		cusUserTask.setProcDefName(baseVO.getTitle());
		cusUserTask.setTaskDefKey(taskDefKey);
		cusUserTask.setTaskName(taskName.toString());
		cusUserTask.setActivityType(activityType);
		
		updateCusUserTaskAssgine(baseVO, cusUserTask,assigneeExpression);
		return cusUserTask;
	}

	/**
	 * 设置单节点处理人
	 * 
	 * @param baseVO
	 * @param cusUserTask
	 * @param assigneeExpression 
	 */
	private void updateCusUserTaskAssgine(BaseVO baseVO, CusUserTask cusUserTask, String assigneeExpression) throws Exception {
//		final String processKey = baseVO.getBusinessKey().split("\\:")[0];
//		System.out.println("CusUserTaskService updateCusUserTaskAssgine: processKey:" + processKey+";assigneeExpression:"+assigneeExpression);
//		switch (processKey) {
//		case ProcessController.MONTHLYREPORT_PERSON_PROCESS:
//			monthlyReportService.setUserTaskAssgineForUser(baseVO, cusUserTask,assigneeExpression);
//			break;
//		case ProcessController.MONTHLYREPORT_PROJECT_PROCESS:
//			monthlyReportService.setUserTaskAssgineForProjcet(baseVO, cusUserTask,assigneeExpression);
//			break;
//		case ProcessController.REQUIREMENTCONFIRMATION_PROCESS:
//		case ProcessController.REQUIREMENTSPLIT_PROCESS:
//			requirementService.setUserTaskAssgine(baseVO, cusUserTask,assigneeExpression);
//			break;
		
//		case ProcessController.REQUIRECONFIRMATION_PROCESS:
//		case ProcessController.REQUIRECONFIRMATION_MULTI_PROCESS:
//			requirementConfirmService.setUserTaskAssgine(baseVO, cusUserTask);
//			break;

//		}
		cusProcess.setUserTaskAssgine(baseVO, cusUserTask,assigneeExpression);

	}

	/**
	 * 设定下一节点执行人
	 * 
	 * @param baseVO
	 * @param condition
	 *            排他网关条件
	 * @param isChangeData
	 * 				下一节点执行人数据是否改变，下一节点执行人数据改变才更新Activiti中下一节点执行人
	 */
	public void updateNextCusUserTaskInfo(BaseVO baseVO, String condition,boolean isChangeData,CusProcess cusProcess) throws Exception {
		this.cusProcess = cusProcess;
		System.out.println("CusUserTaskService updateNextCusUserTaskInfo condition:" + condition + ";baseVO:" + baseVO);
		String nextTaskDefKey = ProcessDefinitionCache.get().getNextActivitiId(baseVO,condition);

		updateUserTaskAssignee(baseVO, isChangeData, nextTaskDefKey);
	}

	/**
	 * 动态更新act_cus_user_task表的节点执行人
	 * @param baseVO
	 * @param isChangeData
	 * @param businessKey
	 * @param taskDefKey
	 * @throws Exception
	 */
	public void updateUserTaskAssignee(BaseVO baseVO, boolean isChangeData, String taskDefKey)
			throws Exception {
		String businessKey = baseVO.getBusinessKey();
		CusUserTask cusUserTask = findByProcAndTask(businessKey, taskDefKey);
		if (cusUserTask == null)
			return;

		String candidate_ids = baseVO.getCandidate_ids();
		String candidate_names = baseVO.getCandidate_names();
		System.out.println("CusUserTaskService updateUserTaskAssignee isChangeData:" + isChangeData+";candidate_ids:"+candidate_ids);
		
		if(StringUtil.isEmpty(candidate_ids) && isChangeData) {
			updateCusUserTaskAssgine(baseVO, cusUserTask,"");
			// 更新用户人物节点执行人数据表
			doUpdate(cusUserTask);
		}else {
			if(!StringUtil.isEmpty(candidate_ids)) {
				String taskType = cusUserTask.getTaskType();
				if (CusUserTask.TYPE_ASSIGNEE.equals(taskType)) {// 普通用户节点
					cusUserTask.setCandidate_ids(candidate_ids);
					cusUserTask.setCandidate_name(candidate_names);
				} else if (CusUserTask.TYPE_CANDIDATEUSER.equals(taskType)) {// 多实例用户节点
					cusUserTask.setCandidate_ids(candidate_ids);
					cusUserTask.setCandidate_name(candidate_names);
				}else {
					cusUserTask.setGroup_id(candidate_ids);
					cusUserTask.setGroup_name(candidate_names);
				}
				// 更新用户人物节点执行人数据表
				doUpdate(cusUserTask);
			}
		}
	}

	public Map<String, String> getNextUserTaskCandidateUsers(BaseVO baseVO,String condition) {
		Map<String, String> user = new HashMap<>();
		String businessKey = baseVO.getBusinessKey();
		try {
			String nextTaskDefKey = ProcessDefinitionCache.get().getNextActivitiId(baseVO,condition);
	
			CusUserTask cusUserTask = findByProcAndTask(businessKey, nextTaskDefKey);
			if (cusUserTask == null)
				return null;
	
			String taskType = cusUserTask.getTaskType();
			if (CusUserTask.TYPE_ASSIGNEE.equals(taskType)) {// 普通用户节点
				user.put("candidate_ids", cusUserTask.getCandidate_ids());
				user.put("candidate_name", cusUserTask.getCandidate_name());
			} else if (CusUserTask.TYPE_CANDIDATEUSER.equals(taskType)) {// 多实例用户节点
				user.put("candidate_ids", cusUserTask.getCandidate_ids());
				user.put("candidate_name", cusUserTask.getCandidate_name());
			} else {// 会签用户节点
				user.put("candidate_ids", cusUserTask.getGroup_id());
				user.put("candidate_name", cusUserTask.getGroup_name());
			}
			return user;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	public void doAdd(CusUserTask cusUserTask) throws Exception {
		userTaskDao.add(cusUserTask);
	}

	
	public void doUpdate(CusUserTask cusUserTask) throws Exception {
		this.userTaskDao.update(cusUserTask);
	}

	
	public void deleteByProcDefKey(String procDefKey) throws Exception {
		this.userTaskDao.deleteByProcDefKey(procDefKey);
	}

	public CusUserTask findById(Integer id) throws Exception {
		return this.userTaskDao.getBeanById(id);
	}

	public Integer deleteAll() throws Exception {
		return this.userTaskDao.deleteAll();
	}

	public List<CusUserTask> findByProcDefKey(String procDefKey) throws Exception {
		return this.userTaskDao.findByProcDefKey(procDefKey);
	}

	/**
	 * 根据流程businessKey和任务节点类型查询所有节点
	 */
	public List<CusUserTask> findByProcAndActivityType(String procDefKey, String activityType) throws Exception {
		return this.userTaskDao.findByProcAndActivityType(procDefKey,activityType);
	}

	/**
	 * 根据流程businessKey和任务节点key查询当前节点的处理人
	 */
	
	public CusUserTask findByProcAndTask(String procDefKey, String taskDefKey) throws Exception {
		return this.userTaskDao.findByProcAndTask(procDefKey, taskDefKey);
	}

	
	public List<CusUserTask> getAll() throws Exception {
		return this.userTaskDao.getAllList();
	}


	/**
	 * 自动跳过
	 * @param string
	 * @param taskDefKey
	 * @throws Exception
	 */
	public void autoPass(Map<String, Object> variables,String processInstanceId, String taskDefKey) throws Exception {
		ActivityImpl nextNodeInfo = ProcessDefinitionCache.get().getNextNodeInfo(repositoryService,runtimeService,processInstanceId,taskDefKey);
		System.out.println("autoPass nextNodeid:"+nextNodeInfo.getId());
		if(nextNodeInfo.getId().contains("reapply")) {
			variables.put(nextNodeInfo.getId(), "false");//reapply_projectManagerAudit等等
		}else {
			variables.put(nextNodeInfo.getId(), "true");//isPass_projectManagerAudit等等
		}
	}
}
