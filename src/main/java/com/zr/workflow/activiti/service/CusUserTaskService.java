package com.zr.workflow.activiti.service;

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

import com.zr.workflow.activiti.dao.CusUserTaskDao;
import com.zr.workflow.activiti.entity.BaseVO;
import com.zr.workflow.activiti.entity.CusUserTask;
import com.zr.workflow.activiti.util.ProcessDefinitionCache;
import com.zr.workflow.activiti.util.StringUtil;

/**
 * 操作act_cus_user_task表的service
 * @author zhourq
 *
 */
@Service
public class CusUserTaskService {

	@Resource
	private RepositoryService repositoryService;
	@Resource
	private RuntimeService runtimeService;
	@Resource
	private ProcessService processService;
	@Resource
	private CusUserTaskDao userTaskDao;
	private CusProcess cusProcess;

	/**
	 * 初始化流程各任务节点信息保存至数据库
	 * 根据项目更新任务节点表 设置各节点处理人
	 * 
	 * cusProcess为null时，按照流程统一设置节点执行人方法配置：
	 * 1、多实例节点可有多个执行人；否则为单个执行人；
	 * 2、如在流程xml中设置Assignee表达式且为变量为applyuserid，则当前节点的执行人为当前登录用户
	 * 否则使用页面传过来的candidate_ids，如果页面未传该参数，且未设置applyuserid，则根据当前节点key包含的角色到系统角色中查找
	 * @param baseVO
	 * @param cusProcess
	 * @throws Exception
	 */
	public void initProcessUserTaskInfo(BaseVO baseVO,CusProcess cusProcess) throws Exception {
		this.cusProcess = cusProcess;
		List<ProcessDefinition> processDefinitionList = processService.findLastetDeployedProcessList();
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
		if(null != cusProcess) {
			cusProcess.setUserTaskAssgine(baseVO, cusUserTask,assigneeExpression);
		}else {
			setUserTaskAssgine(baseVO, cusUserTask,assigneeExpression);
		}
	}


	public void setUserTaskAssgine(BaseVO baseVO, CusUserTask cusUserTask, String assigneeExpression) throws Exception {
		final String activitiType = cusUserTask.getActivityType();
		String user_id = baseVO.getCandidate_ids();
		if(StringUtil.isNotEmpty(assigneeExpression) && assigneeExpression.contains("applyuserid") || (CusUserTask.TYPE_NORMAL.equals(activitiType) && !user_id.contains(","))){
			setAssignee(cusUserTask,  baseVO,assigneeExpression);
		} else  {
			setCandidateUsers(baseVO, cusUserTask);
		}
	}


	/**
	 * 设置指定执行人
	 * @param cusUserTask 
	 * @param baseVO 流程相关实体对象
	 * @param assigneeExpression 流程中设置的指定人变量表达式
	 * @throws Exception
	 */
	private void setAssignee(CusUserTask cusUserTask,  final BaseVO baseVO, String assigneeExpression)
			throws Exception {
		Map<String, String> candidateUser = new HashMap<>();
		cusUserTask.setTaskType(CusUserTask.TYPE_ASSIGNEE);

		if(StringUtil.isNotEmpty(assigneeExpression) && assigneeExpression.contains("applyuserid")) {
			final String userId = baseVO.getCreateId();
			final String userName = baseVO.getCreateName();
			candidateUser.put("candidateIds", userId);
			candidateUser.put("candidateNames", userName);
		}else {
			String user_id = baseVO.getCandidate_ids();
			String userName = baseVO.getCandidate_names();
			if(StringUtil.isNotEmpty(user_id)) {
				candidateUser.put("candidateIds", user_id);
				candidateUser.put("candidateNames", userName);
				//baseVO中的candidate_ids设置过一个节点后不让再给第二个节点
				baseVO.setCandidate_ids("");
				baseVO.setCandidate_names("");
			}
		}

		if(null != candidateUser) {
			String candidateIds = candidateUser.get("candidateIds");
			String candidateNames = candidateUser.get("candidateNames");
			cusUserTask.setCandidate_ids(candidateIds);
			cusUserTask.setCandidate_name(candidateNames);
		}
	}


	/**
	 * 设置候选人(多个人)
	 * @param baseVO
	 * @param cusUserTask
	 */
	private void setCandidateUsers(BaseVO baseVO, CusUserTask cusUserTask) {

		cusUserTask.setTaskType(CusUserTask.TYPE_CANDIDATEUSER);

		String candidateIds = baseVO.getCandidate_ids();
		String candidateNames = baseVO.getCandidate_names();
		if(candidateIds.startsWith(",")) {
			candidateIds = candidateIds.substring(1);
		}
		if(candidateNames.startsWith(",")) {
			candidateNames = candidateNames.substring(1);
		}
		cusUserTask.setCandidate_ids(candidateIds);
		cusUserTask.setCandidate_name(candidateNames);
		//baseVO中的candidate_ids设置过一个节点后不让再给第二个节点
		baseVO.setCandidate_ids("");
		baseVO.setCandidate_names("");
	}


	/**
	 * 设定下一节点执行人
	 * 
	 * @param baseVO
	 * @param condition
	 *            排他网关条件
	 * @param nextTaskDefKey
	 *            下一节点key
	 * @param isChangeData
	 * 				下一节点执行人数据是否改变，下一节点执行人数据改变才更新Activiti中下一节点执行人
	 * @param cusProcess
	 *            当前处理流程节点执行人对象
	 */
	public void updateNextCusUserTaskInfo(BaseVO baseVO, String condition,String nextTaskDefKey,boolean isChangeData,CusProcess cusProcess) throws Exception {
		this.cusProcess = cusProcess;
//		String nextTaskDefKey = ProcessDefinitionCache.get().getNextActivitiId(baseVO,condition);
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

		if(StringUtil.isEmpty(candidate_ids) && isChangeData) {
			updateCusUserTaskAssgine(baseVO, cusUserTask,"");
			// 更新用户人物节点执行人数据表
			doUpdate(cusUserTask);
		}else {
			if(!StringUtil.isEmpty(candidate_ids)) {
				String taskType = cusUserTask.getTaskType();
				if (CusUserTask.TYPE_ASSIGNEE.equals(taskType) || CusUserTask.TYPE_CANDIDATEUSER.equals(taskType)) {
					if(candidate_ids.contains(",")) {
						cusUserTask.setTaskType(CusUserTask.TYPE_CANDIDATEUSER);
					}
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
		if(nextNodeInfo.getId().contains("reapply")) {
			variables.put(nextNodeInfo.getId(), "false");//reapply_projectManagerAudit等等
		}else {
			variables.put(nextNodeInfo.getId(), "true");//isPass_projectManagerAudit等等
		}
	}
}
