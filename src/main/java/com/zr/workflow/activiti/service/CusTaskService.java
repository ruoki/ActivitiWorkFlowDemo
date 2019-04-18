package com.zr.workflow.activiti.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;

import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.DelegationState;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.stereotype.Service;

import com.zr.workflow.activiti.entity.BaseVO;
import com.zr.workflow.activiti.entity.CommentVO;
import com.zr.workflow.activiti.entity.Page;
import com.zr.workflow.activiti.util.ProcessDefinitionCache;
import com.zr.workflow.activiti.util.RevokeTaskCmd;
import com.zr.workflow.activiti.util.StringUtil;

/**
 * 任务节点处理service
 * @author zhourq
 *
 */
@Service
public class CusTaskService{
	@Resource
	RepositoryService repositoryService;
	@Resource
	RuntimeService runtimeService;
	@Resource
	HistoryService historyService;
	@Resource
	ManagementService managementService;
	@Resource
	TaskService taskService;
	@Resource
	CusUserTaskService userTaskService;

	@Resource
	ProcessService processService;

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
			System.out.printf("TaskServiceImpl findTodoTask processInstance:%s isSuspend:%s \n",processInstanceId,isSuspended);
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
		}


		variables.put("entity", baseVO);

		// 设置流程的start_userId和评论人的id
		Authentication.setAuthenticatedUserId(userId);
		if(handleFlag != null) {
			addComment(task, handleFlag, content);
		}

		setLocalVariable(taskId, variables);
		completeTask(task,  taskId, variables);
		checkAutoCompleteTask(taskId, task, variables,baseVO.getDescription());
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
		System.out.println("TaskService getNextNodeAssigneInfos userList:"+userList);
		return userList;
	}

	/**
	 * 执行任务
	 * 
	 * @param task
	 * @param taskId
	 *            任务id
	 * @param variables
	 *            流程遍量
	 */
	private void completeTask(Task task,  String taskId, Map<String, Object> variables) {
		// 完成委派任务
		if (DelegationState.PENDING == task.getDelegationState()) {
			resolveTask(taskId, variables);
			return;
		}

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
	 * 添加评论
	 * 
	 * @param task
	 * @param handleFlag
	 * @param content
	 */
	private void addComment(Task task, StringBuilder handleFlag, String content) {
		System.out.println("CusTaskService addComment handleFlag:"+handleFlag+";content:"+content);
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
		System.out.println("getComment  handleFlag:" + handleFlag);
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
		/** 查找指定人的历史任务列表 */
		List<HistoricTaskInstance> hTaskAssigneeList = getHistoryTaskList(userId,"assignee",page);
		/** 查找参与者的历史任务列表 */
		List<HistoricTaskInstance> hTaskCandidateList = getHistoryTaskList(userId,"candidate",page);
		hTaskAssigneeList.addAll(hTaskCandidateList);

		List<BaseVO> doneTaskList = new ArrayList<>();
		for (HistoricTaskInstance historicTaskInstance : hTaskAssigneeList) {
			String processInstanceId = historicTaskInstance.getProcessInstanceId();
			HistoricProcessInstance historicProcessInstance = processService.getHisProcessInstanceByInstanceId(processInstanceId);

			BaseVO base = null;
			if (null != historicProcessInstance.getEndTime()) {
				base = processService.getBaseVOFromHistoryVariable(null, historicProcessInstance, processInstanceId);
				base.setEnd(true);
				base.setDeleteReason(historicProcessInstance.getDeleteReason());
				//				base = processService.getBaseVOFromHistoryVariable(historicTaskInstance, null, processInstanceId);
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
			if(null == taskStartTime)taskStartTime = task.getCreateTime();
			base.setTask(task);
			base.setTaskDefinitionKey(task.getTaskDefinitionKey());
			base.setToHandleTaskName(task.getName());
			base.setToHandleTaskId(task.getId());
			base.setSuspended(task.isSuspended());
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

	/**
	 * 查询历史任务列表
	 * @param userId 用户id
	 * @param queryType 查询类型 "assignee"：根据任务执行人查询:查找指定人的历史任务列表；<br/>
	 * 						  "candidate":根据任务参与者查询:查找参与者的历史任务列表；
	 * @param page 
	 * @return
	 */
	private List<HistoricTaskInstance> getHistoryTaskList(final String userId,String queryType, Page<BaseVO> page) {
		HistoricTaskInstanceQuery historyTaskQuery;
		if("assignee".equals(queryType)) {
			historyTaskQuery = historyService.createHistoricTaskInstanceQuery().taskAssignee(userId)
					.finished().orderByHistoricTaskInstanceEndTime().desc();
		}else {
			historyTaskQuery = historyService.createHistoricTaskInstanceQuery()
					.taskCandidateUser(userId).finished().orderByHistoricTaskInstanceEndTime().desc();
		}

		List<HistoricTaskInstance> hTaskAssigneeList = new ArrayList<>();
		hTaskAssigneeList = historyTaskQuery.list();
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
	 * 获取任务task的候选人
	 */
	public String getCandidateIdsOfTask(List<Task> taskList) {
		String candidateUserIds = "";
		for (Task task : taskList) {
			System.out.println("getCandidateIdsOfTask assignee:"+task.getAssignee());
			if(task.getAssignee() != null) {
				candidateUserIds = candidateUserIds.concat(task.getAssignee()+",");//只能获取多实例的执行人，无法获取候选人
			}else {
				List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
				for (IdentityLink identityLink : identityLinks) {
					System.out.println("getAssignee userId:" + identityLink.getUserId());
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
	 * @param processInstanceId
	 * @param model
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
	 * @param model
	 * @return
	 * @throws Exception
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
		System.out.println("getBaseVOByTaskId  queryId:" + queryId);
		String processInstanceId = "";
		BaseVO base = null;
		Task task = getTaskByTaskId(queryId);
		if (task == null) {
			HistoricTaskInstance historicTaskInstance = null;
			historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(queryId).singleResult();
			if (historicTaskInstance != null) {
				processInstanceId = historicTaskInstance.getProcessInstanceId();

				base = processService.getBaseVOFromHistoryVariable(historicTaskInstance, null, processInstanceId);
				if(base != null)
					setBaseVO(base, null,historicTaskInstance,null, null);
			} else {
				HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
						.processInstanceId(queryId).singleResult();
				base = processService.getBaseVOFromHistoryVariable(null, historicProcessInstance, queryId);
				if(base != null) {
					setBaseVO(base,null,null,null, historicProcessInstance);
				}
			}
		} else {
			base = (BaseVO) processService.getBaseVOFromRu_Variable(task.getProcessInstanceId());
			if (null != base) 
				setBaseVO(base,task,null,null, null);
		}
		return base;
	}

	/**
	 * userId认领任务 <br/>
	 * task.setAssignee(userId)
	 * 可用于候选组与候选用户，不能用于多实例节点
	 * 认领以后，这个用户就会成为任务的执行人 ， 任务会从组的其他成员的任务列表中消失
	 * 
	 * @param taskId
	 * @param userId
	 */

	public void claim(String taskId, String userId) {
		Authentication.setAuthenticatedUserId(userId);
		this.taskService.claim(taskId, userId);
	}


	/**
	 * 委托任务给userId，<br/>
	 * 是将任务节点分给其他人处理，等其他人处理好之后，委派任务会自动回到委派人的任务中 <br/>
	 * setDelegationState(DelegationState.PENDING);
	 * task.setOwner(task.getAssignee());
	 * task.setAssignee(userId)
	 * 
	 * @param taskId
	 * @param userId
	 */

	public void delegateTask(String taskId, String userId) {
		this.taskService.delegateTask(taskId, userId);
	}

	/**
	 * 被委派人办理任务后
	 * 正在运行的任务表中被委派人办理任务后hr的任务会回到委派人xxhr ，历史任务表中也一样<br/>
	 * 
	 * setDelegationState(DelegationState.RESOLVED);
	 * setAssignee(task.getOwner());
	 * @param taskId
	 * @param variables
	 */
	private void resolveTask(String taskId, Map<String, Object> variables) {
		this.taskService.resolveTask(taskId, variables);
	}





	/**
	 * 撤回任务，一次只能撤回一个节点
	 */
	public Integer revoke(String userId,String historyTaskId, String processInstanceId,String taskDefinitionKey,String nextTaskDefKey) throws Exception {
		Command<Integer> cmd = new RevokeTaskCmd(userId,historyTaskId, processInstanceId,taskDefinitionKey,nextTaskDefKey);
		Integer revokeFlag = managementService.executeCommand(cmd);
		return revokeFlag;
	}
}
