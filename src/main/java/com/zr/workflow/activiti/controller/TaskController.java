package com.zr.workflow.activiti.controller;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.ActivitiTaskAlreadyClaimedException;
import org.activiti.engine.task.Task;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zr.workflow.activiti.entity.BaseVO;
import com.zr.workflow.activiti.entity.CommentVO;
import com.zr.workflow.activiti.entity.CusUserTask;
import com.zr.workflow.activiti.entity.Page;
import com.zr.workflow.activiti.service.CusTaskService;
import com.zr.workflow.activiti.service.CusUserTaskService;
import com.zr.workflow.activiti.service.ProcessService;
import com.zr.workflow.activiti.util.GFJsonUtil;
import com.zr.workflow.activiti.util.ProcessDefinitionCache;
import com.zr.workflow.activiti.util.StringUtil;

/**
 * 任务节点相关web类
 * @author zhourq
 *
 */
@RestController
@RequestMapping("/task")
public class TaskController {
	@Resource
	CusTaskService cusTaskService;
	@Resource
	ProcessService processService;
	@Resource
	private CusUserTaskService cusUserTaskService;
	@Resource
	ProcessController processControllder;

	/**
	 * 我的请求
	 * 
	 * @param page
	 * @param rows
	 * @param userId
	 * @param userName
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/myAllProcess")
	public String myAllProcess(@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "rows", required = false) Integer rows, HttpServletRequest request,
			@RequestParam("userId") String userId,@RequestParam("userName") String userName) {

		Map<String, Object> resultMap = new HashMap<>();
		try {
			Page<BaseVO> p = initPage(page, rows);
			List<BaseVO> processList = this.processService.findMyProcessInstances(p, userId);

			for (BaseVO base : processList) {
				generateBaseVO(base, userId,userName);
			}
			System.out.println("我的请求：" + processList);
			resultMap.put("type", "success");
			resultMap.put("data", processList);
		} catch (Exception e) {
			resultMap.put("type", "error");
			resultMap.put("msg", e.getMessage());
			e.printStackTrace();

		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 查询待办任务 <br/>
	 * 
	 * select g.* from ACT_ID_GROUP g, ACT_ID_MEMBERSHIP membership where g.ID_ =
	 * membership.GROUP_ID_ and membership.USER_ID_ = ? <br/>
	 * 
	 * select distinct RES.* from ACT_RU_TASK RES left join ACT_RU_IDENTITYLINK I on
	 * I.TASK_ID_ = RES.ID_ WHERE (RES.ASSIGNEE_ = ? or (RES.ASSIGNEE_ is null and
	 * I.TYPE_ = 'candidate' and (I.USER_ID_ = ? ))) order by RES.ID_ asc LIMIT ?
	 * OFFSET ? <br/>
	 * 
	 * select distinct RES.* , P.KEY_ as ProcessDefinitionKey, P.ID_ as
	 * ProcessDefinitionId, P.NAME_ as ProcessDefinitionName, P.VERSION_ as
	 * ProcessDefinitionVersion, P.DEPLOYMENT_ID_ as DeploymentId from
	 * ACT_RU_EXECUTION RES inner join ACT_RE_PROCDEF P on RES.PROC_DEF_ID_ = P.ID_
	 * WHERE RES.PARENT_ID_ is null and RES.ID_ = ? and RES.PROC_INST_ID_ = ? and
	 * (RES.SUSPENSION_STATE_ = 1) order by RES.ID_ asc LIMIT ? OFFSET ? <br/>
	 * 
	 * select * from ACT_RU_EXECUTION where ID_ = ? <br/>
	 * select * from ACT_RU_VARIABLE where EXECUTION_ID_ = ? and NAME_= ? and
	 * TASK_ID_ is null <br/>
	 * select * from ACT_GE_BYTEARRAY where ID_ = ? <br/>
	 * select distinct RES.* from ACT_RE_PROCDEF RES WHERE RES.ID_ = ? order by
	 * RES.ID_ asc LIMIT ? OFFSET ?
	 * 
	 * @param page
	 * @param rows
	 * @param userId
	 * @param userName
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/todoTask")
	public String toDoTask(@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "rows", required = false) Integer rows, HttpServletRequest request,
			@RequestParam("userId") String userId,@RequestParam("userName") String userName) {

		Map<String, Object> resultMap = new HashMap<>();
		try {
			Page<BaseVO> p = initPage(page, rows);
			// 查询我的任务列表
			List<BaseVO> taskList = this.cusTaskService.findTodoTask(userId, p);

			for (BaseVO base : taskList) {
				generateBaseVO(base, userId,userName);
			}
			System.out.println("待办任务：" + taskList);
			resultMap.put("type", "success");
			resultMap.put("data", taskList);
		} catch (Exception e) {
			resultMap.put("type", "error");
			resultMap.put("msg", e.getMessage());
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 查看已办任务列表<br/>
	 * select distinct RES.* from ACT_HI_TASKINST RES WHERE RES.ASSIGNEE_ = ? and
	 * RES.END_TIME_ is not null order by RES.ID_ asc LIMIT ? OFFSET ? <br/>
	 * select RES.* from ACT_HI_VARINST RES WHERE RES.PROC_INST_ID_ = ? order by
	 * RES.ID_ asc LIMIT ? OFFSET ? <br/>
	 * select * from ACT_GE_BYTEARRAY where ID_ = ? <br/>
	 * select distinct RES.* from ACT_RU_TASK RES WHERE RES.PROC_INST_ID_ = ? order
	 * by RES.ID_ asc LIMIT ? OFFSET ?
	 * 
	 * @param page
	 * @param rows
	 * @param userId
	 * @param userName
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/doneTask")
	public String findDoneTask(@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "rows", required = false) Integer rows, HttpServletRequest request,
			@RequestParam("userId") String userId,@RequestParam("userName") String userName) {

		Map<String, Object> resultMap = new HashMap<>();
		try {
			Page<BaseVO> p = initPage(page, rows);
			List<BaseVO> taskList = this.cusTaskService.findDoneTask(userId, p);
			for (BaseVO base : taskList) {
				generateBaseVO(base, userId,userName);
			}
			System.out.println("已办任务 ：" + taskList);
			resultMap.put("type", "success");
			resultMap.put("data", taskList);
		} catch (Exception e) {
			resultMap.put("type", "error");
			resultMap.put("msg", e.getMessage());
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 初始化分页
	 * @param page
	 * @param rows
	 * @return
	 */
	private Page<BaseVO> initPage(Integer page, Integer rows) {
		Page<BaseVO> p = (null == page || null == rows) ? null : new Page<BaseVO>(page, rows);
		return p;
	}

	/**
	 * 查看办结任务列表
	 * 
	 * @param page
	 * @param rows
	 * @param userId
	 * @param userName
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/finishedTask")
	public String findFinishedTask(@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "rows", required = false) Integer rows, HttpServletRequest request,
			@RequestParam("userId") String userId,@RequestParam("userName") String userName) {

		Map<String, Object> resultMap = new HashMap<>();
		try {
			Page<BaseVO> p = initPage(page, rows);
			List<BaseVO> taskList = this.processService.findFinishedProcessInstances(p, userId, true);
			for (BaseVO base : taskList) {
				generateBaseVO(base, userId,userName);
			}

			System.out.println("办结任务：" + taskList);
			resultMap.put("type", "success");
			resultMap.put("data", taskList);
		} catch (Exception e) {
			resultMap.put("type", "error");
			resultMap.put("msg", e.getMessage());
			e.printStackTrace();

		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 补充BaseVO数据，评论列表中执行人名称为空，需要自行实现
	 * 
	 * @param base
	 * @param userId
	 * @param userName
	 * @throws Exception 
	 */
	private void generateBaseVO(BaseVO base, String userId,String userName) throws Exception {
		String assignedId = base.getAssignedId();
		String nextAssign = base.getAssign();
		String assignedName = base.getAssignedName();
		String nextAssignName = base.getAssignName();

		if (StringUtils.isEmpty(assignedName) && !StringUtils.isEmpty(assignedId)) {
			//			assignedName = getUserName(assignedId);
			base.setAssignedName(assignedName);
		}

		if (!StringUtils.isEmpty(nextAssign)) {
			//			nextAssignName = getUserName(nextAssign);
			if(StringUtil.isEmpty(nextAssignName)) {
				base.setAssignName(base.getCandidate_names());
			}else {
				base.setAssignName(nextAssignName);
			}
		}

		String description = base.getDescription();
		System.out.println("generateBaseVO  currentUserName:" + userName + ";description:" + description);
		if (description.contains(userName)) {
			description = description.replaceAll(userName, "您");
		}

		final String contentInfo = base.getContentInfo().toString();
		base.setContentInfo(GFJsonUtil.get().parseJson(contentInfo,JSONObject.class));
		base.setDescription(description);
		List<CommentVO> commentList = getCommentList(base);
		base.setComments(commentList);

	}

	private List<CommentVO> getCommentList(BaseVO base) {
		List<CommentVO> commentList = new ArrayList<>();
		try {
			commentList = this.cusTaskService.getComments(base.getProcessInstanceId());

			List<CommentVO> historyCommentList = base.getComments();
			for (int i = 0;i<commentList.size();i++) {
				CommentVO commentVO = commentList.get(i);
				//				commentVO.setUserName(getUserName(commentVO.getUserId()));

				if(i == 0) {
					if(base.isEnd()) {
						commentVO.setNextAssignName("流程结束");
					}else {
						commentVO.setNextAssign(base.getAssign());
						commentVO.setNextAssignName(base.getAssignName());
					}
				}else {
					for (CommentVO historyComment : historyCommentList) {
						if(historyComment.getTaskId().equals(commentVO.getTaskId())) {
							commentVO.setNextAssign(historyComment.getNextAssign());
							commentVO.setNextAssignName(historyComment.getNextAssignName());
							break;
						}
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		System.out.println("评论列表："+commentList);
		return commentList;
	}


	//	/**
	//	 * 根据userId查找userName,存在多个用逗号分隔
	//	 * 
	//	 * @param userIds
	//	 * @return
	//	 * @throws Exception 
	//	 */
	//	
	//	public String getUserName(String userIds){
	//		String userName = "";
	//		if (StringUtil.isEmpty(userIds))
	//			return userName;
	//		List<User> users = this.userService.findUsersByIds(userIds);
	//		for (User user : users) {
	//			userName += user.getName() + ",";
	//		}
	//		if (userName.contains(",")) {
	//			userName = userName.substring(0, userName.length() - 1);
	//		}
	//		return userName;
	//	}


	/**
	 * 审批流程详情
	 * 
	 * @param taskId 任务id<br/>
	 * @return
	 * @throws NumberFormatException
	 * @throws Exception
	 */
	@RequestMapping("/flowDetail")
	public String toApproval(@RequestParam("taskId") String taskId) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			if(StringUtil.isEmpty(taskId)) {
				resultMap.put("type", "error");
				resultMap.put("msg", "taskId must not be empty");
			}else {
				BaseVO baseVO = cusTaskService.getBaseVOByTaskIdOrProcessInstanceId(taskId);
				List<CommentVO> commentList = this.cusTaskService.getComments(baseVO.getProcessInstanceId());
				System.out.println("任务详情：" + baseVO);
				System.out.println("评论列表：" + commentList);
				resultMap.put("type", "success");
				resultMap.put("data", baseVO);
				resultMap.put("comments", commentList);
			}
		} catch (Exception e) {
			resultMap.put("type", "error");
			resultMap.put("msg", e.getMessage());
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 处理任务
	 * 
	 * @param params:{
	 *            userId:'',//流程执行者id<br/>
	 *            userName:'',//流程执行者名称<br/>
	 *            taskId:'',//任务id，不能为空<br/>
	 *            isPass:'',//是否同意，注意：传了该参数就不要传reapply或end<br/>
	 *            reapply:'',//是否重新申请，注意：传了该参数就不要传isPass或end<br/>
	 *            end:'',//是否归档，注意：传了该参数就不要传isPass或reapply<br/>
	 *            content:'',//评论内容，可为空<br/>
	 *            candidate_ids:'',//指定下一节点执行人id（多个人用逗号分隔），不需要动态指定时不传<br/>
	 *            candidate_names:'',//指定下一节点执行人name，不需要动态指定时不传<br/>
	 *            isChangeData:'',//true or false,下一节点执行人是否有改变,为true时不能传candidate_ids或candidate_names，否则以candidate_ids为准
	 *            					//场景：下一节点执行人动态与业务挂钩时需要动态指定下一节点执行人；
	 *            					//示例请看周报（项目外包/行内人员）中的重新申请节点，新增或减少项目，都会影响下一节点的执行人，此时，只用传isChangeData:'true',
	 *            					//后台会获取各项目的项目经理为下一节点的执行人
	 *            taskDefinitionKey:'',//当前节点key，指定下一节点执行人时必传，不需要动态指定时不传<br/>
	 *            contentInfo:'',//与流程无关的业务信息<br/>
	 *             }
	 */
	@RequestMapping("/handle")
	public String handleTask(@RequestBody String json, HttpServletRequest request) {
		System.out.println("handleTask===================params==" + json);

		Map<String, Object> resultMap = new HashMap<>();
		if (json.isEmpty()) {
			resultMap.put("msg", "任务办理失败，必输参数为空！");
			resultMap.put("type", "empty");
		}else {

			String content = GFJsonUtil.get().getProperty(json, "content");
			final String isChangeDataStr = GFJsonUtil.get().getProperty(json,"isChangeData");
			Map<String, Object> variables = new HashMap<String, Object>();
			StringBuilder handleFlag = new StringBuilder();
			String condition = "";
			String taskId = GFJsonUtil.get().getProperty(json, "taskId");
			String isPassStr = GFJsonUtil.get().getProperty(json, "isPass");
			String reapplyStr = GFJsonUtil.get().getProperty(json, "reapply");
			String isEnd = GFJsonUtil.get().getProperty(json, "end");
			BaseVO baseVO = getBaseVO(json);

			try {
				String userId = GFJsonUtil.get().getProperty(json, "userId");
				String userName = GFJsonUtil.get().getProperty(json, "userName");
				condition = checkIsPassTask(isPassStr,taskId,userName, baseVO, variables, handleFlag);
				if (StringUtil.isEmpty(condition)) {
					condition = checkIsReapplyTask(reapplyStr,baseVO, variables, handleFlag);
				}
				if (StringUtil.isEmpty(condition)) {
					checkIsEndTask(isEnd,baseVO, handleFlag, variables);
				}
				final String businessKey = checkUpdateNextNodeAssignees(request, baseVO, variables, condition,isChangeDataStr);
				List<String> nextAssignes = this.cusTaskService.handleTask(taskId,userId,userName,handleFlag, content, baseVO, variables);
				deleteExtraContentInfo(baseVO.getContentInfo().toString(), handleFlag, businessKey,request);
				//多实例节点未全部通过时
				boolean notSetPreNodeInfo = (handleFlag != null && BaseVO.APPROVAL_SUCCESS.equals(handleFlag.toString()))
						&&(!baseVO.getDescription().contains("已同意 ") && !baseVO.getDescription().contains(BaseVO.SUB_DESCRIPTION_PASS));
				if(notSetPreNodeInfo) {
					resultMap.put("msg", "已审核！");
				}else {
					resultMap.put("msg", "当前任务执行完毕");
				}
				resultMap.put("type", "success");
				resultMap.put("nextAssignes", nextAssignes);
			} catch (ActivitiObjectNotFoundException e) {
				deleteComment(baseVO);
				resultMap.put("msg", "此任务不存在，请联系管理员！");
				resultMap.put("type", "notFound");
			} catch (Exception e) {
				e.printStackTrace();
				deleteComment(baseVO);
				resultMap.put("msg", e.getMessage());
				resultMap.put("type", "error");
			}
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 删除该条评论
	 * @param baseVO
	 */
	private void deleteComment(BaseVO baseVO) {
		try {
			List<CommentVO> commentList = this.cusTaskService.getComments(baseVO.getProcessInstanceId());
			if(null != commentList && commentList.size()>0) {
				this.cusTaskService.deleteComment(commentList.get(0).getId());
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * 从请求参数中获取最新的实体对象
	 * @param paramsJson
	 * @return
	 */
	private BaseVO getBaseVO(String params) {
		String taskId = GFJsonUtil.get().getProperty(params, "taskId");
		String candidate_ids = GFJsonUtil.get().getProperty(params, "candidate_ids");
		String candidate_names = GFJsonUtil.get().getProperty(params, "candidate_names");
		String currentTaskActivitiId = GFJsonUtil.get().getProperty(params, "taskDefinitionKey");
		final JSONObject contentInfo = GFJsonUtil.get().getJSONObject(params,"contentInfo");
		final JSONArray comments = GFJsonUtil.get().getJSONArray(params,"comments");

		BaseVO baseVO = this.cusTaskService.getBaseVOByTaskIdOrProcessInstanceId(taskId);
		baseVO.setTaskDefinitionKey(currentTaskActivitiId);// 当前节点key
		baseVO.setCandidate_ids(candidate_ids);// 下一节点执行人id
		baseVO.setCandidate_names(candidate_names);// 下一节点执行人name
		baseVO.setContentInfo(contentInfo);
		if(comments != null) {
			Gson gson = new Gson();
			Type type = new TypeToken<List<CommentVO>>(){}.getType();
			List<CommentVO> commentsRequest = gson.fromJson(comments.toString(), type);
			baseVO.setComments(commentsRequest);
		}
		return baseVO;
	}

	/**
	 * 流程取消后删除除流程实例外的信息，这样可以重新启动该流程
	 * @param contentInfo 业务内容
	 * @param handleFlag 流程处理类型
	 * @param businessKey 业务key
	 * @throws Exception
	 */
	private void deleteExtraContentInfo(final String contentInfo, StringBuilder handleFlag,
			final String businessKey,HttpServletRequest request) throws Exception {
		if (BaseVO.CANCEL.equals(handleFlag.toString())) {
			final String contentInfoId = GFJsonUtil.get().getProperty(contentInfo, "contentInfoId");
			processControllder.deleteProcessInstance(null, businessKey, contentInfoId, "",true,request);
		}
	}

	/**
	 * 是否审核通过
	 * 
	 * @param isPassStr
	 * @param taskId
	 * @param userName
	 * @param baseVO
	 * @param variables
	 * @param handleFlag
	 * @throws Exception 
	 */
	private String checkIsPassTask(String isPassStr,String taskId,String userName, BaseVO baseVO,
			Map<String, Object> variables, StringBuilder handleFlag) throws Exception {
		String pass = "";

		if (StringUtil.isNotEmpty(isPassStr)) {
			pass = isPassStr;
			boolean isPass = "true".equals(isPassStr) ? true : false;
			final String variableKey = "isPass" + "_" + baseVO.getTaskDefinitionKey();

			variables.put(variableKey, isPassStr);

			final String workFloWTitle = StringUtil.isEmpty(baseVO.getTitle()) ? "请求" : baseVO.getTitle();
			if (!isPass) {
				setBackMembers(variables, baseVO);
				handleFlag.append(BaseVO.APPROVAL_FAILED);
				baseVO.setProcessStatus(BaseVO.APPROVAL_FAILED);
				baseVO.setDescription(
						baseVO.getCreateName() + " 的" + workFloWTitle + "被 " + userName + "驳回,需修改后重新提交！");
			} else {
				setAgreeMembers(variables, baseVO.getBusinessKey(),baseVO.getProcessInstanceId(),baseVO.getTaskDefinitionKey());
				handleFlag.append(BaseVO.APPROVAL_SUCCESS);
				baseVO.setProcessStatus(BaseVO.APPROVAL_SUCCESS);
				Task currentTask = this.cusTaskService.getTaskByTaskId(taskId);
				int totalMembers = getMember("nrOfInstances", currentTask.getExecutionId());
				int agreeMembers = getAgreeMember(baseVO);
				if ((totalMembers == 0) || ((totalMembers - agreeMembers) == 1)) {
					String nextActivitiId = ProcessDefinitionCache.get().getNextActivitiId(baseVO,"true");
					if(ProcessDefinitionCache.ARCHIVE.equals(nextActivitiId)) {//到归档前一个节点才算真正通过
						baseVO.setDescription(baseVO.getCreateName() + " 的" + workFloWTitle + BaseVO.SUB_DESCRIPTION_PASS);
					} else {
						baseVO.setDescription(
								userName + " 已同意 " + baseVO.getCreateName() + "的" + workFloWTitle);
					}
				} else {
					baseVO.setDescription(
							userName + " 已审核 " + baseVO.getCreateName() + "的" + workFloWTitle);
				}
			}
		}
		return pass;
	}

	/**
	 * 获取同意人数
	 * @param baseVO
	 * @return
	 */
	private int getAgreeMember(BaseVO baseVO) {
		int agreeMembers = 0;
		final String procDefKey = baseVO.getBusinessKey().contains(":") ? baseVO.getBusinessKey().split(":")[0] : "";
		boolean isWeeklyReport = ProcessController.MONTHLYREPORT_PERSON_PROCESS.equals(procDefKey) || ProcessController.MONTHLYREPORT_PROJECT_PROCESS.equals(procDefKey);
		if(isWeeklyReport) {
			agreeMembers = getMember("agreeMembers", baseVO.getProcessInstanceId());
		}else {
			agreeMembers = getMember("agreeMembers"+"_"+baseVO.getTaskDefinitionKey(), baseVO.getProcessInstanceId());
		}
		return agreeMembers;
	}

	/**
	 * 设置驳回人数
	 * 
	 * @param variables
	 * @param baseVO
	 * @throws Exception 
	 */
	private void setBackMembers(Map<String, Object> variables, BaseVO baseVO) throws Exception {

		final String processInstanceId = baseVO.getProcessInstanceId();
		final String taskDefinitionKey = baseVO.getTaskDefinitionKey();
		final String businessKey = baseVO.getBusinessKey();
		final String procDefKey = businessKey.contains(":") ? businessKey.split(":")[0] : "";
		boolean isWeeklyReport = ProcessController.MONTHLYREPORT_PERSON_PROCESS.equals(procDefKey) || ProcessController.MONTHLYREPORT_PROJECT_PROCESS.equals(procDefKey);
		CusUserTask userTask = cusUserTaskService.findByProcAndTask(businessKey, taskDefinitionKey);
		if(CusUserTask.TYPE_MULTI.equals(userTask.getActivityType())) {
			if(isWeeklyReport) {
				setMemberToVariable("backMembers", variables, processInstanceId);
			}else {
				setMemberToVariable("backMembers"+"_"+taskDefinitionKey, variables, processInstanceId);
			}
		}
	}

	/**
	 * 设置同意人数
	 * 
	 * @param variables
	 * @param processInstanceId
	 * @param taskDefinitionKey 
	 * @throws Exception 
	 */
	private void setAgreeMembers(Map<String, Object> variables,String businessKey, String processInstanceId, String taskDefinitionKey) throws Exception {

		final String procDefKey = businessKey.contains(":") ? businessKey.split(":")[0] : "";
		boolean isWeeklyReport = ProcessController.MONTHLYREPORT_PERSON_PROCESS.equals(procDefKey) || ProcessController.MONTHLYREPORT_PROJECT_PROCESS.equals(procDefKey);
		CusUserTask userTask = cusUserTaskService.findByProcAndTask(businessKey, taskDefinitionKey);
		if(CusUserTask.TYPE_MULTI.equals(userTask.getActivityType())) {
			if(isWeeklyReport) {
				setMemberToVariable("agreeMembers", variables, processInstanceId);
			}else {
				setMemberToVariable("agreeMembers"+"_"+taskDefinitionKey, variables, processInstanceId);
			}
		}
	}

	private void setMemberToVariable(String variableKey, Map<String, Object> variables, String processInstanceId) {
		int agreeMembers = getMember(variableKey, processInstanceId);
		variables.put(variableKey, agreeMembers + 1);
	}

	private int getMember(String variableKey, String processInstanceId) {
		Object obj = processService.getRunVariable(variableKey, processInstanceId);
		int members = obj == null ? 0 : (int) obj;
		return members;
	}

	/**
	 * 是否重新申请
	 * 
	 * @param reapplyStr
	 * @param baseVO
	 * @param variables
	 * @param handleFlag
	 */
	private String checkIsReapplyTask(String reapplyStr, BaseVO baseVO, Map<String, Object> variables,
			StringBuilder handleFlag) {
		String isReapply = "";
		if (StringUtil.isNotEmpty(reapplyStr)) {
			isReapply = reapplyStr;
			boolean reapply = "true".equals(reapplyStr) ? true : false;
			final String variableKey = "reapply" + "_" + baseVO.getTaskDefinitionKey();

			variables.put(variableKey, reapplyStr);

			final String workFloWTitle = StringUtil.isEmpty(baseVO.getTitle()) ? "请求" : baseVO.getTitle();
			if (!reapply) {
				handleFlag.append(BaseVO.CANCEL);
				baseVO.setProcessStatus(BaseVO.CANCEL);
				baseVO.setDescription(baseVO.getCreateName() + " 的" + workFloWTitle + "已取消");
			} else {
				handleFlag.append(BaseVO.WAITING_FOR_APPROVAL);
				baseVO.setProcessStatus(BaseVO.WAITING_FOR_APPROVAL);
				baseVO.setDescription(baseVO.getCreateName() + " 的" + workFloWTitle + "已重新调整");
			}
		}
		return isReapply;
	}

	/**
	 * 是否结束任务
	 * 
	 * @param isEnd
	 * @param baseVO
	 * @param handleFlag
	 * @param variables
	 */
	private void checkIsEndTask(String isEnd, BaseVO baseVO, StringBuilder handleFlag,
			Map<String, Object> variables) {

		if (StringUtil.isNotEmpty(isEnd) && "true".equals(isEnd)) {
			//			resetAllMembers(variables,baseVO);
			handleFlag.append(BaseVO.FILED);
			baseVO.setProcessStatus(BaseVO.FILED);
			// final String workFloWTitle =
			// StringUtil.isEmpty(baseVO.getTitle())?"请求":baseVO.getTitle();
			// baseVO.setDescription(baseVO.getCreateName() + " 的"+workFloWTitle+"已归档");
		}
	}

	/**
	 * 判断下一节点是否更新执行人和是否需要自动执行
	 * @param request
	 * @param baseVO
	 * @param variables 流程变量
	 * @param condition 网关的判断条件值，true或false
	 * @param isChangeDataStr 下一节点执行人是否有改变
	 * @return
	 * @throws Exception
	 */
	private String checkUpdateNextNodeAssignees(HttpServletRequest request, BaseVO baseVO, Map<String, Object> variables,
			String condition,String isChangeDataStr) throws Exception {

		final String businessKey = baseVO.getBusinessKey();
		final String procDefKey = businessKey.contains(":") ? businessKey.split(":")[0] : "";
		String nextActivitiId = ProcessDefinitionCache.get().getNextActivitiId(baseVO,condition);

		final String description = baseVO.getDescription();
		boolean isAutoCompleteNextActiviti = false;
		if(ProcessDefinitionCache.ARCHIVE.equals(nextActivitiId)&& description.contains(BaseVO.SUB_DESCRIPTION_PASS)) {
			isAutoCompleteNextActiviti = true;
			variables.put("autoComplete", true);
		}else {
			isAutoCompleteNextActiviti = false;
		}

		updateContentInfo(request, baseVO, procDefKey,isAutoCompleteNextActiviti);//先更新业务信息，再变更各节点的执行人

		boolean isChangeData = "true".equals(isChangeDataStr) ? true : false;//数据改变了更新下一节点执行人
		updateNextCusUserTaskAssigness(baseVO, condition, isChangeData);


		CusUserTask userTask = cusUserTaskService.findByProcAndTask(businessKey, nextActivitiId);
		if(CusUserTask.TYPE_MULTI.equals(userTask.getActivityType())) {
			String candidate_ids = userTask.getCandidate_ids();
			String[] candidate_users = candidate_ids.split(",");
			List<String> assigneeList = new ArrayList<>();
			if (null != candidate_users) {
				System.out.println("ProcessController genareteVariable candidateUserIds:" + candidate_users);
				assigneeList = Arrays.asList(candidate_users);
			}
			if (assigneeList.size() < 1) {//节点的执行人为空时自动跳过该节点
				cusUserTaskService.autoPass(variables,baseVO.getProcessInstanceId(), nextActivitiId);
			}

			variables.put("assigneeList" + "_" + nextActivitiId, assigneeList);
			resetMembers(variables, businessKey,nextActivitiId);
		}

		return businessKey;
	}

	/**
	 * 更新下一节点执行人
	 * @param baseVO
	 * @param condition
	 * @param isChangeData
	 * @throws Exception
	 */
	private void updateNextCusUserTaskAssigness(BaseVO baseVO, String condition, boolean isChangeData) throws Exception {
		final String processKey = baseVO.getBusinessKey().split("\\:")[0];
		switch (processKey) {
		//		case ProcessController.REQUIREMENTCONFIRMATION_PROCESS:
		//		case ProcessController.REQUIREMENTSPLIT_PROCESS:
		//			this.cusUserTaskService.updateNextCusUserTaskInfo(baseVO, condition,isChangeData,requirementService);
		//			break;
		default://默认从前端页面获取candidate_ids和candidate_names进行设置节点执行人
			this.cusUserTaskService.updateNextCusUserTaskInfo(baseVO, condition,isChangeData,null);
			break;
		}
	}


	/**
	 * 重置多实例任务节点同意人数或驳回人数流程变量
	 * @param variables
	 * @param businessKey
	 * @param nextActivitiId
	 * @throws Exception
	 */
	private void resetMembers(Map<String, Object> variables, String businessKey, String taskDefinitionKey) throws Exception {
		final String procDefKey = businessKey.contains(":") ? businessKey.split(":")[0] : "";
		boolean isWeeklyReport = ProcessController.MONTHLYREPORT_PERSON_PROCESS.equals(procDefKey) || ProcessController.MONTHLYREPORT_PROJECT_PROCESS.equals(procDefKey);
		if(isWeeklyReport) {
			variables.put("backMembers", 0);
			variables.put("agreeMembers", 0);
		}else {
			variables.put("backMembers"+"_"+taskDefinitionKey, 0);
			variables.put("agreeMembers"+"_"+taskDefinitionKey, 0);
		}
	}

	/**
	 * 更新各流程的业务信息
	 * @param request
	 * @param baseVO
	 * @param processKey
	 * @param isAutoCompleteNextActiviti 
	 */
	private void updateContentInfo(HttpServletRequest request, BaseVO baseVO, String processKey, boolean isAutoCompleteNextActiviti) {
		JSONObject jsonObject = baseVO.getContentInfo();
		if (jsonObject == null)
			return;
		switch (processKey) {
		case ProcessController.MONTHLYREPORT_PERSON_PROCESS:
		case ProcessController.MONTHLYREPORT_PROJECT_PROCESS:
			if(isAutoCompleteNextActiviti) {//归档保存周报
				jsonObject.put("reportStatus", "0");
			}
			//			reportControllder.updateReport(request, jsonObject.toString());
			break;
			//		 case ProcessController.REQUIREMENTCONFIRMATION_PROCESS:
			//		 case ProcessController.REQUIREMENTSPLIT_PROCESS:
			//		 break;

		}
	}

	/**
	 * userId认领任务 <br/>
	 * task.setAssignee(userId)
	 * 
	 * @return
	 */
	@RequestMapping("/claim")
	public String claim(@RequestParam("taskId") String taskId, @RequestParam("userId") String userId) {

		Map<String, Object> resultMap = new HashMap<>();
		try {
			this.cusTaskService.claim(taskId, userId);
			resultMap.put("type", "success");
			resultMap.put("msg", "任务认领成功！");
		} catch (ActivitiObjectNotFoundException e) {

			resultMap.put("type", "notFound");
			resultMap.put("msg", "此任务不存在！任务认领失败！");
		} catch (ActivitiTaskAlreadyClaimedException e) {
			resultMap.put("type", "claimed");
			resultMap.put("msg", "此任务已被其他组成员认领！请刷新页面重新查看！");
		} catch (Exception e) {
			resultMap.put("type", "error");
			resultMap.put("msg", "任务认领失败！请联系管理员！");
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 委托任务给userId，<br/>
	 * task.setOwner(task.getAssignee());
	 * task.setAssignee(userId)
	 * 
	 * @return
	 */
	@RequestMapping("/delegate")
	public String delegateTask(@RequestParam("taskId") String taskId, @RequestParam("userId") String userId) {

		System.out.println("delegateTask=======");
		Map<String, Object> resultMap = new HashMap<>();
		try {
			this.cusTaskService.delegateTask(taskId, userId);
			resultMap.put("type", "success");
			resultMap.put("msg", "任务委托成功！");
		} catch (ActivitiObjectNotFoundException e) {
			resultMap.put("type", "error");
			resultMap.put("msg", "此任务不存在！任务委托失败！");
		} catch (Exception e) {
			resultMap.put("type", "error");
			resultMap.put("msg", "任务委托失败！请联系管理员！");
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}


	/**
	 * 撤销任务<br/>
	 * 	  userId:撤回的执行人id
          taskId:撤回的目标taskId,<br/>
          processInstanceId:撤回所在的流程实例,<br/>
          historyTaskDefKey:撤回的目标节点id,<br/>
          nextTaskDefKey:被撤回的节点id
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/revoke")
	public String revoke(@RequestParam("userId") String userId,
			@RequestParam("taskId") String taskId,
			@RequestParam("processInstanceId") String processInstanceId,
			@RequestParam("historyTaskDefKey") String historyTaskDefKey,
			@RequestParam("nextTaskDefKey") String nextTaskDefKey) {
		Map<String, Object> resultMap = new HashMap<>();

		try {
			Integer revokeFlag = this.cusTaskService.revoke(userId,taskId, processInstanceId,historyTaskDefKey,nextTaskDefKey);
			if (revokeFlag == 0) {
				resultMap.put("type", "success");
				resultMap.put("msg", "撤销任务成功！");
			} else if (revokeFlag == 1) {
				resultMap.put("type", "error");
				resultMap.put("msg", "撤销任务失败 - [ 此审批流程已结束! ]");
			} else if (revokeFlag == 2) {
				resultMap.put("type", "error");
				resultMap.put("msg", "撤销任务失败 - [ 下一结点已经通过,不能撤销! ]");
			} else if (revokeFlag == 3) {
				resultMap.put("type", "error");
				resultMap.put("msg", "撤销任务失败 - [ 该任务非当前用户提交，无法撤回! ]");
			} else if (revokeFlag == -1) {
				resultMap.put("type", "error");
				resultMap.put("msg", "撤销任务失败 - [ 未找到目标任务，无法撤回! ]");
			}else {
				resultMap.put("type", "error");
				resultMap.put("msg", "撤销任务失败 - [ 内部错误！ ]");
			}
		} catch (Exception e) {
			resultMap.put("type", "error");
			resultMap.put("msg", "撤销任务失败 - [ 内部错误！ ]");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

}