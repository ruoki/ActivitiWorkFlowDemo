package com.zr.workflow.activiti.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.DelegationState;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.zr.workflow.activiti.entity.BaseVO;
import com.zr.workflow.activiti.entity.CommentVO;
import com.zr.workflow.activiti.entity.CusUserTask;
import com.zr.workflow.activiti.entity.Page;
import com.zr.workflow.activiti.util.ApplicationContextHandler;
import com.zr.workflow.activiti.util.ProcessDefinitionCache;
import com.zr.workflow.activiti.util.StringUtil;

/**
 * 任务节点处理service
 * @author zhourq
 *
 */
@Service
public class CusTaskService {
	@Resource
	private RepositoryService repositoryService;
	@Resource
	private RuntimeService runtimeService;
	@Resource
	private HistoryService historyService;
	@Resource
	private ManagementService managementService;
	@Resource
	private TaskService taskService;
	@Resource
	private CusUserTaskService userTaskService;

	@Resource
	private ProcessService processService;

	/**
	 * 根据流程实例查询待办任务,可能是多实例的会签点，可能有多个任务
	 */

	public List<Task> findRunTaskByProcInstanceId(String proInstanceId) {
		List<Task> taskList = this.taskService.createTaskQuery().processInstanceId(proInstanceId).list();
		return taskList;
	}

	/**
	 * 查询待办任务
	 * 
	 * @param userId
	 * @param page
	 * @return
	 */
	public List<BaseVO> findTodoTask(String userId, Page<BaseVO> page) {
		TaskQuery taskQuery = this.taskService.createTaskQuery().taskCandidateOrAssigned(userId).orderByTaskCreateTime().desc();
		List<Task> tasks = new ArrayList<>();
		tasks = taskQuery.list();
		if(null != page) {//分页
			int[] pageParams = page.getPageParams(tasks.size());
			tasks = tasks.subList(pageParams[0], pageParams[0]+pageParams[1]);
			//			tasks = taskQuery.listPage(pageParams[0], pageParams[1]);
		}
		List<BaseVO> taskList = new ArrayList<BaseVO>();
		for (Task task : tasks) {
			String processInstanceId = task.getProcessInstanceId();
			ProcessInstance processInstance = this.runtimeService.createProcessInstanceQuery()
					.processInstanceId(processInstanceId).singleResult();
			boolean isSuspended = processInstance.isSuspended();
			if(isSuspended)continue;//获取激活状态下的流程实例

			BaseVO base = (BaseVO) processService.getBaseVOFromRu_Variable(processInstanceId);
			if (null == base) continue;
			setBaseVO(base, task,null,processInstance, null);
			taskList.add(base);
		}
		return taskList;
	}

	/**
	 * 办理完第1个任务“提交申请”
	 * 
	 * @param instanceId
	 * @param isPass
	 * @param baseVO
	 * @param variables
	 * @return 
	 * @throws Exception
	 */
	public List<String> excuteFirstTask(String instanceId, String isPass,BaseVO baseVO, Map<String, Object> variables) throws Exception {
		Task task = taskService.createTaskQuery()// 查询出本流程实例中当前仅有的一个任务“提交申请”
				.processInstanceId(instanceId).singleResult();
		baseVO.setTaskDefinitionKey(task.getTaskDefinitionKey());

		StringBuilder handleFlag = new StringBuilder();
		checkIsPassFirstTask(isPass, baseVO, variables, handleFlag);

		List<CommentVO> commentList = getComments(baseVO.getProcessInstanceId());

		Map<String, String> users = userTaskService.getNextUserTaskCandidateUsers(baseVO, "");
		if(null != users) {
			for (CommentVO commentVO : commentList) {
				commentVO.setNextAssign(users.get("candidate_ids"));
				commentVO.setNextAssignName(users.get("candidate_name"));
			}
			//		}else {
			//			variables.put("autoComplete", true);//如果下一节点执行人为空，则直接通过
		}
		baseVO.setComments(commentList);
		List<String> userList = handleTask(task.getId(),baseVO.getCreateId(),baseVO.getCreateName(),handleFlag, "发起申请", baseVO, variables);
		return userList;
	}



	/**
	 * 第一个任务是否审核通过
	 * 
	 * @param isPassStr
	 * @param baseVO
	 * @param variables
	 * @param handleFlag
	 */
	private String checkIsPassFirstTask(String isPassStr, BaseVO baseVO,
			Map<String, Object> variables, StringBuilder handleFlag) {
		String pass = "";
		if (StringUtil.isNotEmpty(isPassStr)) {
			pass = isPassStr;
			boolean isPass = "true".equals(isPassStr) ? true : false;
			final String variableKey = "isPass" + "_" + baseVO.getTaskDefinitionKey();
			variables.put(variableKey, isPassStr);

			if (!isPass) {
				handleFlag.append(BaseVO.APPROVAL_FAILED);
				baseVO.setProcessStatus(BaseVO.APPROVAL_FAILED);
			} else {
				handleFlag.append(BaseVO.APPROVAL_SUCCESS);
				baseVO.setProcessStatus(BaseVO.APPROVAL_SUCCESS);
			}
		}
		return pass;
	}

	/**
	 * 处理任务
	 * @return 
	 */
	public List<String> handleTask(String taskId,String userId,String userName,StringBuilder handleFlag, String content, BaseVO baseVO,
			Map<String, Object> variables) {
		Task task = getTaskByTaskId(taskId);

		//多实例节点未全部通过时不保存上一个节点信息
		boolean notSetPreNodeInfo = (handleFlag != null && BaseVO.APPROVAL_SUCCESS.equals(handleFlag.toString()))
				&&(!baseVO.getDescription().contains("已同意 ") && !baseVO.getDescription().contains(BaseVO.SUB_DESCRIPTION_PASS));
		if(!notSetPreNodeInfo) {
			baseVO.setHandledTaskId(taskId);
			baseVO.setAssignedId(userId);
			baseVO.setAssignedName(userName);
			baseVO.setHandledTaskDefinitionKey(task.getTaskDefinitionKey());
			baseVO.setHandledTaskName(task.getName());
			final String handledActivitiType = getActivitiType(baseVO.getBusinessKey(), task.getTaskDefinitionKey());
			baseVO.setHandledActivitiType(handledActivitiType);
			if (DelegationState.PENDING == task.getDelegationState()) {
				baseVO.setDescription(userName+"已完成委托的任务");
			}
		}
		variables.put("entity", baseVO);

		if (DelegationState.PENDING == task.getDelegationState()) {
			resolveTask(taskId, variables,content);
		}else {
			// 设置流程的start_userId和评论人的id
			Authentication.setAuthenticatedUserId(userId);
			if(handleFlag != null) {
				addComment(task, handleFlag, content);
			}
			completeTask(task,  taskId, variables);
			checkAutoCompleteTask(taskId, task, variables,baseVO.getDescription());
		}
		List<String> userList = getNextNodeAssigneInfos(baseVO.getProcessInstanceId());
		return userList;
	}

	public Task getTaskByTaskId(String taskId) {
		Task task = this.taskService.createTaskQuery().taskId(taskId).singleResult();
		return task;
	}

	/**
	 * 获取下一节点的执行人
	 * @param processInstanceId
	 * @return
	 */
	private List<String> getNextNodeAssigneInfos(String processInstanceId) {
		List<String> userList = new ArrayList<>();
		List<Task> toDotaskList = findRunTaskByProcInstanceId(processInstanceId);// 获取该流程的待办任务,可能是多实例的会签点，可能有多个执行人多个任务
		if (null != toDotaskList && toDotaskList.size() > 0) {
			final String userIdsStr = getCandidateIdsOfTask(toDotaskList);
			String[] userIds = new String[1];
			if (userIdsStr.contains(",")) {
				userIds = userIdsStr.split(",");
			}else {
				userIds[0] = userIdsStr;
			}
			for (int i = 0; i < userIds.length; i++) {
				userList.add(userIds[i]);
			}
		}
		return userList;
	}

	/**
	 * 执行任务
	 * taskService.complete(taskId, variables)方法是将variables存到execution中
	 * @param task
	 * @param taskId
	 *            任务id
	 * @param variables
	 *            流程遍量
	 */
	private void completeTask(Task task,  String taskId, Map<String, Object> variables) {

		this.taskService.complete(taskId, variables);
	}

	/**
	 * 自动执行(通过)
	 * 
	 * @param taskId 任务id
	 * @param task 任务
	 * @param variables 流程变量
	 * @param description 任务描述
	 */
	private void checkAutoCompleteTask(String preTaskId, Task preTask, Map<String, Object> variables, String description) {
		try {
			final String processInstanceId = preTask.getProcessInstanceId();
			Object autoCompleteObj = processService.getRunVariable("autoComplete", processInstanceId);
			boolean autoComplete = autoCompleteObj == null ? false : (boolean) autoCompleteObj;

			List<Task> tasks = taskService.createTaskQuery()// 查询出本流程实例中当前仅有的一个任务
					.processInstanceId(processInstanceId).list();
			if(tasks.size() > 1 )autoComplete = false;
			Task task = tasks.get(0);
			if (autoComplete || (ProcessDefinitionCache.ARCHIVE.equals(task.getTaskDefinitionKey()) && description.contains(BaseVO.SUB_DESCRIPTION_PASS))) {
				String nextTaskId = task.getId();
				variables.put("autoComplete", false);
				if (!preTaskId.equals(nextTaskId)) {
					completeTask(task,  nextTaskId, variables);
				}
			}
		} catch (Exception e) {// 有可能是最后一个节点（取消申请），无法继续自动执行下一个节点
			e.printStackTrace();
		}
	}

	/**
	 * 设置本地变量：设置实体与任务绑定
	 * 
	 * @param taskId
	 * @param variables
	 */

	public void setLocalVariable(String taskId, Map<String, Object> variables) {
		if (variables == null) return;
		for (String variableName : variables.keySet()) {
			if ("entity".equals(variableName)) {
				this.taskService.setVariableLocal(taskId, variableName, variables.get(variableName));
			}
		}
	}

	/**
	 * 设置流程变量
	 * 
	 * @param taskId
	 * @param variables
	 */

	public void setVariable(String taskId, Map<String, Object> variables) {
		if (variables == null) return;
		for (String variableName : variables.keySet()) {
			if ("entity".equals(variableName)) {
				this.taskService.setVariable(taskId, variableName, variables.get(variableName));
			}
		}
	}

	/**
	 * 添加评论
	 * 
	 * @param task
	 * @param handleFlag
	 * @param content
	 */
	private void addComment(Task task, StringBuilder handleFlag, String content) {
		if (StringUtil.isEmpty(content)) {
			content = getComment(handleFlag);
		}
		String processInstanceId = task.getProcessInstanceId();
		addComment(task.getId(), processInstanceId, content);
	}

	public void addComment(String taskId, String processInstanceId, String message) {
		this.taskService.addComment(taskId, processInstanceId, message);
	}

	/**
	 * 删除评论
	 * @param taskId
	 * @param processInstanceId
	 */
	public void deleteComment(String commentId){
		this.taskService.deleteComment(commentId);
	}

	/**
	 * 删除该流程的所有评论
	 * @param taskId
	 * @param processInstanceId
	 */
	public void deleteComments(String taskId,String processInstanceId){
		this.taskService.deleteComments(taskId, processInstanceId);
	}

	/**
	 * 获取默认评论
	 * 
	 * @param handleFlag
	 *            处理类型
	 */
	private String getComment(StringBuilder handleFlag) {
		String content = "批准";
		if (handleFlag == null || handleFlag.length() < 1)
			return content;
		switch (handleFlag.toString()) {
		case BaseVO.APPROVAL_SUCCESS:
			content = "批准";
			break;
		case BaseVO.APPROVAL_FAILED:
			content = "驳回";
			break;
		case BaseVO.WAITING_FOR_APPROVAL:
			content = "已重新申请";
			break;
		case BaseVO.FILED:
			content = "已查看";
			break;
		case BaseVO.CANCEL:
			content = "已取消申请";
			break;
		case BaseVO.TASK_PENDING:
			content = "已委托";
			break;
		case BaseVO.TASK_RESOLVED:
			content = "委托人已完成任务";
			break;
		case BaseVO.TASK_CLAIMED:
			content = "已认领";
			break;
		}
		return content;
	}

	/**
	 * 已完成的任务
	 * 
	 * @param userId
	 * @param model
	 * @return
	 * @throws Exception
	 */
	public List<BaseVO> findDoneTask(String userId, Page<BaseVO> page) throws Exception {

		List<BaseVO> doneTaskList = new ArrayList<>();

		List<HistoricTaskInstance> hTaskAssigneeList = getHistoryTaskList(userId,page);
		for (HistoricTaskInstance historicTaskInstance : hTaskAssigneeList) {
			String processInstanceId = historicTaskInstance.getProcessInstanceId();
			HistoricProcessInstance historicProcessInstance = processService.getHisProcessInstanceByInstanceId(processInstanceId);

			BaseVO base = null;
			if (null != historicProcessInstance.getEndTime()) {
				base = processService.getBaseVOFromHistoryVariable(processInstanceId);
				base.setEnd(true);
				base.setDeleteReason(historicProcessInstance.getDeleteReason());
			}else {
				base = processService.getBaseVOFromRu_Variable(processInstanceId);
			}
			if (null == base) continue;
			setBaseVO(base,null,historicTaskInstance,null, historicProcessInstance);
			doneTaskList.add(base);
		}
		return doneTaskList;
	}

	/**
	 * 查询历史任务列表
	 * @param userId 用户id
	 * @param page 
	 * @return
	 */
	private List<HistoricTaskInstance> getHistoryTaskList(final String userId, Page<BaseVO> page) {
		List<HistoricTaskInstance> hTaskAssigneeList = new ArrayList<>();
		hTaskAssigneeList = historyService.createHistoricTaskInstanceQuery().taskInvolvedUser(userId)
				.finished().orderByHistoricTaskInstanceEndTime().desc().list();
		if(null != page) {//分页
			//			Integer totalSum = historyTaskQuery.list().size();
			//			int[] pageParams = page.getPageParams(totalSum);
			//			hTaskAssigneeList = historyTaskQuery.listPage(pageParams[0], pageParams[1]);
			int[] pageParams = page.getPageParams(hTaskAssigneeList.size());
			hTaskAssigneeList = hTaskAssigneeList.subList(pageParams[0], pageParams[0]+pageParams[1]);
		}
		return hTaskAssigneeList;
	}

	/**
	 * 填充实体类
	 * @param base
	 * @param task
	 * @param historicTaskInstance
	 * @param processInstance
	 * @param historicProcessInstance
	 */
	public void setBaseVO(BaseVO base,Task task, HistoricTaskInstance historicTaskInstance,
			ProcessInstance processInstance,HistoricProcessInstance historicProcessInstance) {

		String proDefId = "";
		String processInstanceId = "";
		Date taskStartTime = null;
		String owner = "";
		String handledTaskDefinitionKey = "";
		String handledTaskId = "";
		String handledTaskName = "";
		if(null != task) {
			proDefId = task.getProcessDefinitionId();
			processInstanceId = task.getProcessInstanceId();
			taskStartTime = task.getCreateTime();
			owner = task.getOwner();
		}else if(null != historicTaskInstance) {
			proDefId = historicTaskInstance.getProcessDefinitionId();
			processInstanceId = historicTaskInstance.getProcessInstanceId();
			taskStartTime = historicTaskInstance.getCreateTime();
			owner = historicTaskInstance.getOwner();
			handledTaskDefinitionKey = historicTaskInstance.getTaskDefinitionKey();
			handledTaskId = historicTaskInstance.getId();
			handledTaskName = historicTaskInstance.getName();

			final String handledActivitiType = getActivitiType(base.getBusinessKey(), handledTaskDefinitionKey);
			base.setHandledTaskDefinitionKey(handledTaskDefinitionKey);
			base.setHandledTaskId(handledTaskId);
			base.setHandledTaskName(handledTaskName);
			base.setHandledActivitiType(handledActivitiType);

		}else if(null != processInstance) {
			proDefId = processInstance.getProcessDefinitionId();
			processInstanceId = processInstance.getId();
		}else if(null != historicProcessInstance) {
			proDefId = historicProcessInstance.getProcessDefinitionId();
			processInstanceId = historicProcessInstance.getId();
		}
		String userIds = "";
		List<Task> toDotaskList = findRunTaskByProcInstanceId(processInstanceId);// 获取该流程的待办任务,可能是多实例的会签点，可能有多个执行人多个任务
		if (null != toDotaskList && toDotaskList.size() > 0) {// 指定为空的情况下，表明该节点为会签节点，直接显示其候选组名
			if(null == task) {
				task = toDotaskList.get(0);
			}
			userIds = getCandidateIdsOfTask(toDotaskList);
		}

		base.setAssign(userIds);//未操作者
		base.setProcessInstanceId(processInstanceId);
		base.setHistoricTaskInstance(historicTaskInstance);
		base.setHistoricProcessInstance(historicProcessInstance);
		base.setProcessInstance(processInstance);
		base.setOwner(owner);
		if(null != task) {//下一节点的相关信息
			final String toHandleActivitiType = getActivitiType(base.getBusinessKey(), task.getTaskDefinitionKey());
			base.setToHandleActivitiType(toHandleActivitiType);
			if(null == taskStartTime)taskStartTime = task.getCreateTime();
			base.setTask(task);
			base.setTaskDefinitionKey(task.getTaskDefinitionKey());
			base.setToHandleTaskName(task.getName());
			base.setToHandleTaskId(task.getId());
			base.setSuspended(task.isSuspended());
			final DelegationState processStatus = task.getDelegationState();
			if(null != processStatus) {
				base.setDelegationState(processStatus.toString());
			}
		}
		base.setTaskStartTime(taskStartTime);
		ProcessDefinition process = processService.findProcessDefinitionById(proDefId);
		if(null != process) {
			base.setProcessDefinitionKey(process.getKey());
			base.setProcessDefinitionId(process.getId());
			base.setProcessDefinitionName(process.getName());
			base.setVersion(process.getVersion());
			base.setDeploymentId(process.getDeploymentId());
		}
	}

	private String getActivitiType(String businessKey, String taskDefinitionKey) {
		CusUserTask userTask = null;
		try {
			userTask = userTaskService.findByProcAndTask(businessKey, taskDefinitionKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String activitiType = null == userTask ? "":userTask.getActivityType();
		activitiType = StringUtil.isEmpty(activitiType) ? "N" : activitiType;
		return activitiType;
	}

	/**
	 * 获取任务task的候选人
	 */
	public String getCandidateIdsOfTask(List<Task> taskList) {
		String candidateUserIds = "";
		for (Task task : taskList) {
			if(task.getAssignee() != null) {
				candidateUserIds = candidateUserIds.concat(task.getAssignee()+",");//只能获取多实例的执行人，无法获取候选人
			}else {
				List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
				for (IdentityLink identityLink : identityLinks) {
					candidateUserIds = candidateUserIds.concat(identityLink.getUserId()+",");
				}
			}
		}
		candidateUserIds = StringUtil.cropTail(candidateUserIds);
		return candidateUserIds;
	}

	/**
	 * 获取一个任务的评论
	 * 
	 * @param taskId
	 * @return
	 * @throws Exception
	 */
	public List<CommentVO> getCommentsByTaskId(String taskId) throws Exception {
		List<Comment> comments = this.taskService.getTaskComments(taskId);
		List<CommentVO> commnetList = sortAndFormatComments(comments);
		return commnetList;
	}

	/**
	 * 获取一个流程的全部评论
	 * 
	 * @param processInstanceId
	 * @return
	 */
	public List<CommentVO> getComments(String processInstanceId) {
		List<Comment> comments = this.taskService.getProcessInstanceComments(processInstanceId);
		List<CommentVO> commnetList = sortAndFormatComments(comments);
		return commnetList;
	}

	private List<CommentVO> sortAndFormatComments(List<Comment> comments) {
		comments.sort(new Comparator<Comment>() {

			public int compare(Comment o1, Comment o2) {
				try {
					if (o1 == null || o2 == null)
						return -1;
					Date dt1 = o1.getTime();
					Date dt2 = o2.getTime();
					if (dt1 == null || dt2 == null)
						return -1;
					if (dt1.getTime() >= dt2.getTime()) {
						return -1;
					} else {
						return 1;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return 0;
			}
		});
		List<CommentVO> commnetList = new ArrayList<CommentVO>();
		for (Comment comment : comments) {
			CommentVO vo = new CommentVO();
			vo.setId(comment.getId());
			vo.setUserId(comment.getUserId());
			vo.setContent(comment.getFullMessage());
			vo.setTime(comment.getTime());
			vo.setTaskId(comment.getTaskId());
			vo.setProcessInstanceId(comment.getProcessInstanceId());
			//			vo.setUserName(getUserName(comment.getUserId()));
			commnetList.add(vo);
		}
		return commnetList;
	}

	/**
	 * 根据任务id或流程实例id查询实体对象，如果是办结事宜，参数为流程实例id，否则为任务id
	 */
	public BaseVO getBaseVOByTaskIdOrProcessInstanceId(String queryId) {
		BaseVO base = null;
		Task task = getTaskByTaskId(queryId);
		if (task == null) {
			base = processService.getBaseVOFromHistoryVariable(queryId);
			if(base != null) {
				HistoricTaskInstance historicTaskInstance = null;
				historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(queryId).singleResult();
				if (historicTaskInstance != null) {
					setBaseVO(base, null,historicTaskInstance,null, null);
				} else {
					HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
							.processInstanceId(queryId).singleResult();
					setBaseVO(base,null,null,null, historicProcessInstance);
				}
			}
		} else {
			base = (BaseVO) processService.getBaseVOFromRu_Variable(task.getProcessInstanceId());
			if (null != base) {
				setBaseVO(base,task,null,null, null);
			}
		}
		return base;
	}

	/**
	 * userId认领任务 <br/>
	 * task.setAssignee(userId)
	 * 可用于候选组与候选用户，不能用于多实例节点
	 * 认领以后，这个用户就会成为任务的执行人 ， 任务会从组的其他成员的任务列表中消失
	 * 
	 * @param taskId 签收的taskid
	 * @param userId 签收人id
	 * @param userName 签收人名称
	 * @param msg 意见
	 * @param hasComments 是否加入评论列表
	 * @param baseVO 流程实体类
	 */

	public void claim(String taskId, String userId,String userName,String msg,boolean hasComments,BaseVO baseVO) {
		Authentication.setAuthenticatedUserId(userId);
		this.taskService.claim(taskId, userId);

		if(hasComments) {
			Map<String, Object> variables = new HashMap<>();
			baseVO.setProcessStatus(BaseVO.TASK_CLAIMED);
			baseVO.setDescription("任务已被"+userName+"认领");
			variables.put("entity", baseVO);

			StringBuilder handleFlag = new StringBuilder();
			handleFlag.append(BaseVO.TASK_CLAIMED);
			Task task = getTaskByTaskId(taskId);
			addComment(task,handleFlag,msg);
			this.processService.setVariables(task.getProcessInstanceId(),variables);
		}
	}


	/**
	 * 委托任务给userId，<br/>
	 * 是将任务节点分给其他人处理，等其他人处理好之后，委派任务会自动回到委派人的任务中 <br/>
	 * setDelegationState(DelegationState.PENDING);<br/>
	 * task.setOwner(task.getAssignee());<br/>
	 * task.setAssignee(userId)<br/>
	 * <br/>
	 * @param taskId 被委托的任务id
	 * @param fromUserId 委托人id
	 * @param toUserId 被委托人id
	 * @param msg 意见
	 * @param variables 流程变量
	 */
	public void delegateTask(String taskId, String fromUserId,String toUserId,String msg,Map<String, Object> variables) {
		this.taskService.delegateTask(taskId, toUserId);
		if(variables.size() > 0) {
			Authentication.setAuthenticatedUserId(fromUserId);
			StringBuilder handleFlag = new StringBuilder();
			handleFlag.append(BaseVO.TASK_PENDING);
			Task task = getTaskByTaskId(taskId);
			addComment(task,handleFlag,msg);
			this.processService.setVariables(task.getProcessInstanceId(),variables);
		}
	}

	/**
	 * 被委派人办理任务后
	 * 正在运行的任务表中被委派人办理任务后hr的任务会回到委派人xxhr ，历史任务表中也一样<br/>
	 * 
	 * setDelegationState(DelegationState.RESOLVED);<br/>
	 * setAssignee(task.getOwner());<br/>
	 * <br/>
	 * 注意:taskService.resolveTask(taskId, variables)方法是将variables存到Task域中，为了保证每次获取到的实体变量都是流程最新的，<br/>
	 * 我们需要调用processService.setVariables(task.getProcessInstanceId(),variables);将variables存到execution中
	 * @param taskId
	 * @param variables
	 */
	private void resolveTask(String taskId, Map<String, Object> variables,String content) {

		StringBuilder handleFlag = new StringBuilder();
		handleFlag.append(BaseVO.TASK_RESOLVED);
		Task task = getTaskByTaskId(taskId);
		Authentication.setAuthenticatedUserId(task.getAssignee());

		if (StringUtil.isEmpty(content)) {
			content = getComment(handleFlag);
		}
		String processInstanceId = task.getProcessInstanceId();
		addComment(taskId, processInstanceId, content);

		this.processService.setVariables(task.getProcessInstanceId(),variables);
		this.taskService.resolveTask(taskId, variables);
	}





	/**
	 * 撤回任务，一次只能撤回一个节点
	 */
	public Integer revoke(String userId,String backToTaskId,String backToActivitiType,String backFromTaskId,String backFromActivitiType) throws Exception {
		int revokeFlag = recall(userId,backToTaskId,backToActivitiType,backFromTaskId,backFromActivitiType);
		return revokeFlag;
	}

	/**
	 * 撤回流程
	 * @param userId
	 * @param backToTaskId
	 * @param backToActivitiType
	 * @param backFromTaskId
	 * @param backFromActivitiType
	 * @return
	 */
	private int recall(String userId,String backToTaskId,String backToActivitiType,String backFromTaskId,String backFromActivitiType) {

		HistoricTaskInstance hisTaskInstanceBackTo = this.historyService.createHistoricTaskInstanceQuery().taskId(backToTaskId).singleResult();
		HistoricTaskInstance hisTaskInstanceBackFrom = this.historyService.createHistoricTaskInstanceQuery().taskId(backFromTaskId).singleResult();
		if(null == hisTaskInstanceBackTo || null == hisTaskInstanceBackFrom) return -1;

		boolean result1 = isProcessInstanceEnd(hisTaskInstanceBackTo.getProcessInstanceId());
		if(result1) return 1; 
		boolean result2 = isCommitTaskBy(userId, backToTaskId, hisTaskInstanceBackTo.getAssignee());
		if(!result2) return 2;
		boolean result3 = isNextTaskComplete(backFromTaskId, backFromActivitiType, hisTaskInstanceBackFrom.getTaskDefinitionKey());
		if(result3) return 3;

		JdbcTemplate jdbcTemplate = ApplicationContextHandler.getBean(JdbcTemplate.class);



		List<HistoricTaskInstance> backToHTIList = null;
		if("M".equals(backToActivitiType)) {//多实例节点
			/*该节点有多少个实例删多少个*/
			Object obj = processService.getHistoryVariable("nrOfInstances",hisTaskInstanceBackTo.getProcessInstanceId());
			int members = obj == null ? 0 : (int) obj;

			backToHTIList = this.historyService.createHistoricTaskInstanceQuery().taskDefinitionKey(hisTaskInstanceBackTo.getTaskDefinitionKey()).orderByTaskCreateTime().desc().list();
			backToHTIList = backToHTIList.subList(0, members);
		}else {
			backToHTIList = this.historyService.createHistoricTaskInstanceQuery().taskId(backToTaskId).list();
		}

		for (HistoricTaskInstance historicTaskInstance : backToHTIList) {
			deleteBackToActivitiHis(historicTaskInstance,jdbcTemplate);
		}
		updateNextUserTaskActivitiTransition(hisTaskInstanceBackTo,backToActivitiType, jdbcTemplate);
		return 0;
	}

	/**
	 * 判断流程是否已结束
	 * @param processInstanceId
	 * @return
	 */
	private boolean isProcessInstanceEnd(String processInstanceId) {
		// 取得流程实例
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		if(null == processInstance) return true;
		return false;
	}

	/**
	 * 判断任务是否为当前用户提交
	 * @param userId
	 * @param backToTaskId
	 * @param backToTaskAssignee
	 * @return
	 */
	private boolean isCommitTaskBy(String userId, String backToTaskId, String backToTaskAssignee) {
		//检测任务是否为该用户提交
		if(StringUtil.isEmpty(backToTaskAssignee)) {//当前节点如果是会签节点，用户作为候选人参与时，assignee为null
			List<Comment> comments = this.taskService.getTaskComments(backToTaskId);
			System.out.println("revokeTask comments:"+comments);
			backToTaskAssignee = (null != comments && comments.size()>0) ? comments.get(0).getUserId() : null;
		}
		if(!userId.equals(backToTaskAssignee)) {
			return false;
		}
		return true;
	}

	/**
	 * 判断撤回的目标hisTaskInstanceBackFrom任务是否已执行
	 * @param backFromTaskId
	 * @param backFromActivitiType
	 * @param taskDefinitionKey
	 * @return
	 */
	private boolean isNextTaskComplete(String backFromTaskId, String backFromActivitiType,
			String taskDefinitionKey) {
		//获取下一节点是否已经执行
		List<HistoricTaskInstance> htiList = null;
		int completeCount = 0;
		if("M".equals(backFromActivitiType)) {//多实例节点
			htiList = this.historyService.createHistoricTaskInstanceQuery().taskDefinitionKey(taskDefinitionKey).list();
		}else {
			htiList = this.historyService.createHistoricTaskInstanceQuery().taskId(backFromTaskId).list();
		}
		if(null != htiList) {
			for (HistoricTaskInstance hti : htiList) {//多实例节点由多个任务，全部执行完成才认为已经通过
				if("completed".equals(hti.getDeleteReason())){
					completeCount += 1;
				}
			}
		}
		if(completeCount > 0 && completeCount == htiList.size()) {
			return true;
		}
		return false;
	}

	/**
	 * 将hisTaskInstanceBackFrom任务后面的方向清空，把hisTaskInstanceBackTo任务拼接到原来的判断网关，然后结束hisTaskInstanceBackFrom任务
	 * @param hisTaskInstanceBackTo
	 * @param jdbcTemplate
	 */
	private void updateNextUserTaskActivitiTransition(HistoricTaskInstance hisTaskInstanceBackTo,String backToActivitiType,
			JdbcTemplate jdbcTemplate) {
		Map<String, Object> variables = runtimeService.getVariables(hisTaskInstanceBackTo.getProcessInstanceId());
		if("M".equals(backToActivitiType)) {//多实例节点,重置节点通过的条件
			resetMembers(variables,hisTaskInstanceBackTo.getTaskDefinitionKey());
		}
		System.out.println(variables);

		// 取得流程定义
		ProcessDefinitionEntity definitionEntity = (ProcessDefinitionEntity) repositoryService
				.getProcessDefinition(hisTaskInstanceBackTo.getProcessDefinitionId());
		System.out.println(definitionEntity);
		// 取得上一步活动
		ActivityImpl hisActivity = definitionEntity.findActivity(hisTaskInstanceBackTo.getTaskDefinitionKey());

		System.out.println(hisActivity);


		List<Task> currTasks = taskService.createTaskQuery().processInstanceId(hisTaskInstanceBackTo.getProcessInstanceId()).list();
		for (Task currTask : currTasks) {
			ActivityImpl currActivity = definitionEntity.findActivity(currTask.getTaskDefinitionKey());

			//hisTaskInstanceBackFrom任务后面的方向清空
			ArrayList<PvmTransition> oriPvmTransitionList = new ArrayList<>();
			List<PvmTransition> pvmTransitionList = currActivity.getOutgoingTransitions();
			for (PvmTransition pvmTransition : pvmTransitionList) {
				oriPvmTransitionList.add(pvmTransition);
			}
			pvmTransitionList.clear();

			// 建立新方向，把hisTaskInstanceBackTo任务拼接到原来的判断网关
			TransitionImpl newTransition = currActivity.createOutgoingTransition();
			newTransition.setDestination(hisActivity);

			//结束hisTaskInstanceBackFrom任务
			taskService.claim(currTask.getId(), null);
			taskService.complete(currTask.getId(), variables);

			historyService.deleteHistoricTaskInstance(currTask.getId());
			//删除历史行为
			jdbcTemplate.update("delete from ACT_HI_ACTINST where task_id_=?", currTask.getId());

			// 恢复方向
			hisActivity.getIncomingTransitions().remove(newTransition);
			List<PvmTransition> pvmTList = currActivity.getOutgoingTransitions();
			pvmTList.clear();
			for (PvmTransition pvmTransition : oriPvmTransitionList) {
				pvmTransitionList.add(pvmTransition);
			}
		}
	}

	/**
	 * 重置多实例任务节点同意人数或驳回人数流程变量
	 * @param variables
	 * @param taskDefinitionKey
	 */
	private void resetMembers(Map<String, Object> variables, String taskDefinitionKey) {
		variables.put("backMembers"+"_"+taskDefinitionKey, 0);
		variables.put("agreeMembers"+"_"+taskDefinitionKey, 0);
	}

	/**
	 * 删除当前节点的历史信息
	 * @param hisTaskInstanceBackTo
	 * @param jdbcTemplate
	 */
	private void deleteBackToActivitiHis(HistoricTaskInstance hisTaskInstanceBackTo,
			JdbcTemplate jdbcTemplate) {
		deleteHisEGA(hisTaskInstanceBackTo, jdbcTemplate);
		//删除历史行为
		jdbcTemplate.update("delete from ACT_HI_ACTINST where task_id_=?", hisTaskInstanceBackTo.getId());
		historyService.deleteHistoricTaskInstance(hisTaskInstanceBackTo.getId());
	}

	/**
	 * 删除上一步活动的网关节点
	 * @param hisTaskInstanceBackTo
	 * @param jdbcTemplate
	 */
	private void deleteHisEGA(HistoricTaskInstance hisTaskInstanceBackTo, JdbcTemplate jdbcTemplate) {
		ActivityImpl nextNodeInfo;
		try {
			// 取得上一步活动的网关节点
			nextNodeInfo = ProcessDefinitionCache.get().getNextNodeInfo(repositoryService,runtimeService,hisTaskInstanceBackTo.getProcessInstanceId(),hisTaskInstanceBackTo.getTaskDefinitionKey());
			if(null != nextNodeInfo) {
				HistoricActivityInstance exclusiveGatewayActivity = this.historyService.createHistoricActivityInstanceQuery().activityId(nextNodeInfo.getId()).singleResult();
				if(null != exclusiveGatewayActivity) {
					//删除网关节点的历史行为
					jdbcTemplate.update("delete from ACT_HI_ACTINST where act_id_=?", exclusiveGatewayActivity.getActivityId());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
