package com.zr.workflow.activiti.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.io.FileUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zr.workflow.activiti.entity.BaseVO;
import com.zr.workflow.activiti.entity.CommentVO;
import com.zr.workflow.activiti.entity.CusUserTask;
import com.zr.workflow.activiti.entity.Page;
import com.zr.workflow.activiti.service.CusProcess;
import com.zr.workflow.activiti.service.CusTaskService;
import com.zr.workflow.activiti.service.CusUserTaskService;
import com.zr.workflow.activiti.service.ProcessService;
import com.zr.workflow.activiti.util.DateFormatUtil;
import com.zr.workflow.activiti.util.FileUtil;
import com.zr.workflow.activiti.util.GFJsonUtil;
import com.zr.workflow.activiti.util.StringUtil;


/**
 * 流程相关web类
 * @author zhourq
 * 
 *
 */
public abstract class ProcessController {
	@Resource
	private ProcessService processService;
	@Resource
	private CusTaskService cusTaskService;
	@Resource
	private CusUserTaskService userTaskService;

	/**
	 * 查询已部署的流程列表
	 * 非最新版本的"deprecated"为true
	 * 
	 * @return
	 */
	@RequestMapping("/findDeployedProcessList")
	public String findDeployedProcessList(HttpServletRequest request) {
		List<ProcessDefinition> list = processService.findDeployedProcessList();
		//		// 定义有序map，相同的key,添加map值后，后面的会覆盖前面的值
		//		Map<String, ProcessDefinition> map = new LinkedHashMap<String, ProcessDefinition>();
		//		// 遍历相同的key，替换最新的值
		//		for (ProcessDefinition pd : list) {
		//			map.put(pd.getKey(), pd);
		//		}
		//
		//		List<ProcessDefinition> linkedList = new LinkedList<ProcessDefinition>(map.values());
		Map<String, Integer> map = new HashMap<String, Integer>();
		List<Map<String, Object>> processList = new ArrayList<>();
		int i = 0;
		for (ProcessDefinition pd : list) {
			Map<String, Object> map2 = new HashMap<>();

			final String processKey = pd.getKey();
			if(map.containsKey(processKey)) {
				int index = map.get(processKey);
				int versionOld = (int) processList.get(index).get("version");
				int versionNew = pd.getVersion();
				if(versionNew > versionOld) {
					processList.get(index).put("deprecated", true);
				}else {
					map2.put("deprecated", true);
				}
			}else {
				map.put(processKey, i);
			}

			map2.put("id", pd.getId());
			map2.put("category", pd.getCategory());
			map2.put("name", pd.getName());
			map2.put("key", processKey);
			map2.put("version", pd.getVersion());
			map2.put("description", pd.getDescription());
			map2.put("resourceName", pd.getResourceName());
			map2.put("deploymentId", pd.getDeploymentId());
			map2.put("suspended", pd.isSuspended());
			map2.put("diagramResourceName", pd.getDiagramResourceName());
			processList.add(map2);
			i++;
		}

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("data", processList);
		resultMap.put("projectPath", request.getContextPath());
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 删除已部署的流程
	 * 
	 * @param deploymentId 流程部署id
	 * @return
	 */
	@RequestMapping("/deleteDeployment")
	public String deleteDeployment(@RequestParam("deploymentId") String deploymentId,HttpServletRequest request) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			deleteDeployedProcess(deploymentId,request);
			resultMap.put("msg", "流程删除成功");
			resultMap.put("type", "success");
		} catch (Exception e) {
			resultMap.put("msg", "流程删除失败");
			resultMap.put("type", "fail");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 一键删除已部署的流程
	 * 
	 * @param json [], //包含已部署的流程信息，其中deploymentId不可少
	 * @return
	 */
	@RequestMapping("/oneButtonToDeleteDeployment")
	public String oneButtonToDeleteDeployment(@RequestBody String json,HttpServletRequest request) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			JSONArray processList = GFJsonUtil.get().parseArray(json);
			for (int i = 0; i < processList.size(); i++) {
				JSONObject process = (JSONObject) processList.get(i);
				String deploymentId = GFJsonUtil.get().getProperty(process, "deploymentId");
				deleteDeployedProcess(deploymentId,request);
			}

			resultMap.put("msg", "流程删除成功");
			resultMap.put("type", "success");
		} catch (Exception e) {
			resultMap.put("msg", "流程删除失败");
			resultMap.put("type", "error");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	public void deleteDeployedProcess(String deploymentId,HttpServletRequest request) throws Exception {
		List<HistoricProcessInstance> pIList = this.processService.getPIsByDeploymentId(deploymentId);
		for (HistoricProcessInstance historicProcessInstance : pIList) {
			String processInstanceId = historicProcessInstance.getId();
			String businessKey = historicProcessInstance.getBusinessKey();
			BaseVO baseVO = cusTaskService.getBaseVOByTaskIdOrProcessInstanceId(processInstanceId);
			if(null != baseVO) {
				JSONObject jsonObject = baseVO.getContentInfo();
				String contentInfoId = "";
				if (jsonObject != null) {
					contentInfoId = GFJsonUtil.get().getProperty(jsonObject, "contentInfoId");
				}
				deleteProcessInstance(processInstanceId, businessKey, contentInfoId, "",true,request);
			}
		}
		this.processService.deleteDeployedProcess(deploymentId);
	}


	/**
	 * 所有流程实例
	 * 
	 * @param page
	 * @param rows
	 * @param userId
	 * @param userName
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/findAllProcess")
	public String findAllProcess(@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "rows", required = false) Integer rows, HttpServletRequest request,
			@RequestParam("userId") String userId,@RequestParam("userName") String userName) {

		Map<String, Object> resultMap = new HashMap<>();
		try {
			Page<BaseVO> p = initPage(page, rows);
			List<BaseVO> processList = this.processService.findAllProcessInstances(p);

			for (BaseVO base : processList) {
				generateBaseVO(base, userId,userName);
			}
			System.out.println("所有流程：" + processList);
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
	 * 初始化分页
	 * @param page
	 * @param rows
	 * @return
	 */
	public Page<BaseVO> initPage(Integer page, Integer rows) {
		Page<BaseVO> p = (null == page || null == rows) ? null : new Page<BaseVO>(page, rows);
		return p;
	}

	/**
	 * 补充BaseVO数据，评论列表中执行人名称为空，需要自行实现
	 * 
	 * @param base
	 * @param userId
	 * @param userName
	 * @throws Exception 
	 */
	public void generateBaseVO(BaseVO base, String userId,String userName) throws Exception {
		String assignedId = base.getAssignedId();
		String nextAssign = base.getAssign();
		String assignedName = base.getAssignedName();
		String nextAssignName = base.getAssignName();

		if (StringUtil.isEmpty(assignedName) && !StringUtil.isEmpty(assignedId)) {
			assignedName = getUserNamesFromLocal(assignedId);
			base.setAssignedName(assignedName);
		}

		if (!StringUtil.isEmpty(nextAssign)) {
			nextAssignName = getUserNamesFromLocal(nextAssign);
			if(StringUtil.isEmpty(nextAssignName)) {
				base.setAssignName(base.getCandidate_names());
			}else {
				base.setAssignName(nextAssignName);
			}
		}

		String description = base.getDescription();
		if (description.contains(userName)) {
			description = description.replaceAll(userName, "您");
		}
		final String contentInfo = null == base.getContentInfo()?"":base.getContentInfo().toString();
		if(StringUtil.isNotEmpty(contentInfo)) {
			base.setContentInfo(GFJsonUtil.get().parseJson(contentInfo,JSONObject.class));
		}
		base.setDescription(description);
		List<CommentVO> commentList = getCommentList(base);
		base.setComments(commentList);

	}

	public List<CommentVO> getCommentList(BaseVO base) {
		List<CommentVO> commentList = new ArrayList<>();
		try {
			commentList = this.cusTaskService.getComments(base.getProcessInstanceId());
			List<CommentVO> historyCommentList = base.getComments();

			int curCommentLength = commentList.size();
			int hisCommentLength = historyCommentList.size();

			if((curCommentLength == hisCommentLength)) {//有撤回操作
				int i = 0;
				for (i = 0; i<commentList.size(); i++) {
					CommentVO commentVO = commentList.get(i);
					commentVO.setUserName(getUserNamesFromLocal(commentVO.getUserId()));
					if(i != 0) {
						setOtherCommentNextAssign(historyCommentList, commentVO, i);
					}else {
						setFirstCommentNextAssign(base, commentVO);
					}
				}
			}else {
				int i = 0;
				for (i = 0; i<commentList.size(); i++) {
					CommentVO commentVO = commentList.get(i);
					commentVO.setUserName(getUserNamesFromLocal(commentVO.getUserId()));
					if(i != 0) {
						int j = i-1;
						setOtherCommentNextAssign(historyCommentList, commentVO, j);
					}else {
						setFirstCommentNextAssign(base, commentVO);
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		System.out.println("评论列表："+commentList);
		return commentList;
	}

	/**
	 * 设置最新一条评论的下一接收人
	 * @param base
	 * @param commentVO
	 */
	private void setFirstCommentNextAssign(BaseVO base, CommentVO commentVO) {
		if(base.isEnd()) {
			commentVO.setNextAssignName("流程结束");
		}else {
			commentVO.setNextAssign(base.getAssign());
			commentVO.setNextAssignName(base.getAssignName());
		}
	}


	/**
	 * 设置除第一个之外的评论的下一接收人
	 * @param historyCommentList
	 * @param commentVO
	 * @param j
	 */
	private void setOtherCommentNextAssign(List<CommentVO> historyCommentList, CommentVO commentVO, int j) {
		CommentVO historyComment;
		if (j < historyCommentList.size()) {
			historyComment = historyCommentList.get(j);
			commentVO.setNextAssign(historyComment.getNextAssign());
			commentVO.setNextAssignName(historyComment.getNextAssignName());
		}
	}

	/**
	 * 根据userId查找userName,存在多个用逗号分隔
	 * 
	 * @param userIds
	 * @return
	 * @throws Exception 
	 */
	public String getUserNamesFromLocal(String userIds){
		String userName = "";
		if (StringUtil.isEmpty(userIds))
			return userName;
		userName = getUserNames(userIds);
		if (userName.contains(",")) {
			userName = userName.substring(0, userName.length() - 1);
		}
		return userName;
	}


	/**
	 * 启动流程并执行第一个任务
	 * 
	 * @param params:{
	 *            userId:'',//流程创建者id<br/>
	 *            userName:'',//流程创建者名称<br/>
	 *            workFlowTitle:'',//流程名称<br/>
	 *            businesskey:'',//业务key，用来绑定流程，命名规则：[流程名称:业务id（唯一）]<br/>
	 *            					比如人力外包周报为：monthlyreportForPersonProcess:113(userId):20180120(startTime):20180127(endTime)<br/>
	 *            reason:'',//申请理由<br/>
	 *            candidate_ids:'',//指定下一节点执行人id<br/>
	 *            candidate_names:'',//指定下一节点执行人name<br/>
	 *            contentInfoId:'',//业务id，用于删除流程时删除相关的业务信息<br/>
	 *            contentInfo:'',//与流程无关的业务信息<br/>
	 *             }
	 */
	@RequestMapping(value = "/start")
	public String startProcess(@RequestBody String params, HttpServletRequest request) {
		Map<String, Object> resultMap = new HashMap<>();

		BaseVO baseVO = makeBaseVO(request, params);
		if (baseVO == null) {
			resultMap.put("msg", "流程启动失败,必输参数当前用户user为空");
			resultMap.put("type", "empty");
		}else {
			final String businesskey = baseVO.getBusinessKey();// 设置业务key
			List<HistoricProcessInstance> processInstanceList = processService.getPIsByBusinessKey(businesskey);
			ProcessInstance instance = null;
			if (processInstanceList.size() > 0) {
				resultMap.put("msg", "同一个流程只能申请一次");
				resultMap.put("type", "onetime");
			} else {
				try {
					final String procDefKey = businesskey.contains(":") ? businesskey.split(":")[0] : "";
					JSONObject jsonObject = baseVO.getContentInfo();
					handleController(request, procDefKey,jsonObject);//业务处理
					initUserTaskAssignees(baseVO);//初始化流程已有的节点执行人信息

					Map<String, Object> variables = genareteVariable(baseVO);//初始化必要的流程变量

					instance = processService.startWorkFlow(baseVO, variables);//启动流程

					baseVO.setProcessInstanceId(instance.getId());

					//执行第一个任务
					String isPass = GFJsonUtil.get().getProperty(params, "isPass");
					List<String> nextAssignes = cusTaskService.excuteFirstTask(instance.getId(),isPass, baseVO, variables);

					resultMap.put("msg", "流程启动成功");
					resultMap.put("type", "success");
					resultMap.put("nextAssignes", nextAssignes);

				} catch (Exception e) {
					resultMap.put("msg", e);
					resultMap.put("type", "delete");
					deleteProcessInFo(resultMap, baseVO, businesskey, instance,request);
					e.printStackTrace();
				}
			}
		}	
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 初始化节点执行人保存到act_cus_user_task表<br/>
	 * 自定义流程时，可实现CusProcess接口的setUserTaskAssgine方法，根据需求设置节点执行人；
	 * 
	 * @param baseVO
	 * @throws Exception
	 */
	private void initUserTaskAssignees(BaseVO baseVO) throws Exception {
		final String processKey = baseVO.getBusinessKey().split("\\:")[0];
		CusProcess cusProcess = getProcess(processKey);
		if(null != cusProcess) {
			this.userTaskService.initProcessUserTaskInfo(baseVO,cusProcess);
		}else {
			this.userTaskService.initProcessUserTaskInfo(baseVO,null);
		}
	}


	/**
	 * 更新下一节点执行人
	 * 默认从前端页面获取candidate_ids和candidate_names进行设置节点执行人
	 * @param baseVO
	 * @param condition
	 * @param isChangeData
	 * @throws Exception
	 */
	public void updateNextCusUserTaskAssigness(BaseVO baseVO, String condition, boolean isChangeData) throws Exception {

		final String processKey = baseVO.getBusinessKey().split("\\:")[0];
		CusProcess cusProcess = getProcess(processKey);
		if(null != cusProcess) {
			this.userTaskService.updateNextCusUserTaskInfo(baseVO, condition,isChangeData,cusProcess);
		}else {
			this.userTaskService.updateNextCusUserTaskInfo(baseVO, condition,isChangeData,null);
		}
	}

	/**
	 * 启动失败则删除流程实例相关信息
	 * @param result
	 * @param baseVO
	 * @param businesskey
	 * @param instance
	 */
	private void deleteProcessInFo(Map<String, Object> resultMap, BaseVO baseVO, final String businesskey,
			ProcessInstance instance,HttpServletRequest request) {
		if (instance != null) {
			try {
				JSONObject jsonObject = baseVO.getContentInfo();

				String contentInfoId = GFJsonUtil.get().getProperty(jsonObject, "contentInfoId");
				String processInstanceId = instance == null ? "" : instance.getId();
				deleteProcessInstance(processInstanceId,businesskey, contentInfoId, "",true,request);

			} catch (Exception e1) {
				resultMap.put("msg", e1);
				resultMap.put("type", "deleteError");
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 初始化必要的流程变量
	 * @param baseVO
	 * @return
	 * @throws Exception
	 */
	private Map<String, Object> genareteVariable(BaseVO baseVO) throws Exception {
		Map<String, Object> variables = new HashMap<>();
		variables.put("applyuserid", baseVO.getCreateId());// 用来设置第一个任务的申请人和重新申请的处理人
		final String businessKey = baseVO.getBusinessKey();
		List<CusUserTask> userTaskList = this.userTaskService.findByProcAndActivityType(businessKey,CusUserTask.TYPE_MULTI);
		if(null != userTaskList && userTaskList.size()>0) {
			for (CusUserTask userTask : userTaskList) {
				String taskDefKey = userTask.getTaskDefKey();
				variables.put("agreeMembers"+"_"+taskDefKey, 0);
				variables.put("backMembers"+"_"+taskDefKey, 0);
				String candidate_ids = userTask.getCandidate_ids();
				String[] candidate_users = candidate_ids.split(",");
				List<String> assigneeList = new ArrayList<>();
				if (null != candidate_users) {
					assigneeList = Arrays.asList(candidate_users);
				}
				variables.put("assigneeList" + "_" + taskDefKey, assigneeList);
			}
		}
		return variables;
	}

	/**
	 * 请求参数生成BaseVO对象
	 * @param request
	 * 
	 * @param params:{
	 *            userId:'',//流程创建者id<br/>
	 *            userName:'',//流程创建者名称<br/>
	 *            workFlowTitle:'',//流程名称<br/>
	 *            businesskey:'',//业务key，用来绑定流程，命名规则：[流程名称:业务id（唯一）]<br/>
	 *            					比如人力外包周报为：monthlyreportForPersonProcess:113(userId):20180120(startTime):20180127(endTime)<br/>
	 *            reason:'',//申请理由<br/>
	 *            candidate_ids:'',//指定下一节点执行人id<br/>
	 *            candidate_names:'',//指定下一节点执行人name<br/>
	 *            contentInfoId:'',//业务id，注意，放在contentInfo中，用于删除流程时删除相关的业务信息<br/>
	 *            contentInfo:'',//与流程无关的业务信息<br/>
	 *             }
	 * @return
	 */
	private BaseVO makeBaseVO(HttpServletRequest request, String params) {

		final String userId = GFJsonUtil.get().getProperty(params, "userId");
		if(StringUtil.isEmpty(userId))return null;

		BaseVO baseVO = new BaseVO();
		final String userName = GFJsonUtil.get().getProperty(params, "userName");
		final String workFlowTitle = GFJsonUtil.get().getProperty(params, "workFlowTitle");
		final String businesskey = GFJsonUtil.get().getProperty(params, "businesskey");// 设置业务key
		final String reason = GFJsonUtil.get().getProperty(params, "reason");
		String contentInfoId = GFJsonUtil.get().getProperty(params, "contentInfoId");
		final JSONObject contentInfo = GFJsonUtil.get().getJSONObject(params,"contentInfo");
		String candidate_ids = GFJsonUtil.get().getProperty(params, "candidate_ids");
		String candidate_names = GFJsonUtil.get().getProperty(params, "candidate_names");

		baseVO.setTitle(workFlowTitle);
		baseVO.setReason(reason);
		baseVO.setCandidate_ids(candidate_ids);// 下一节点执行人id
		baseVO.setCandidate_names(candidate_names);// 下一节点执行人name

		if(null != contentInfo) {

			contentInfo.put("contentInfoId", contentInfoId);
		}
		baseVO.setBusinessKey(businesskey);
		baseVO.setContentInfo(contentInfo);

		baseVO.setCreateId(userId);
		baseVO.setCreateName(userName);
		baseVO.setCreateTime(DateFormatUtil.format(new Date()));
		baseVO.setProcessStatus(BaseVO.WAITING_FOR_APPROVAL);// 待审批
		baseVO.setDescription(userName + "提出" + workFlowTitle);
		return baseVO;
	}

	/**
	 * 删除流程实例
	 * @param paramsJson:{
	 * 			  processInstanceId:'',//流程实例id,不能为空
	 *            userId:'',//用户id，当只结束正在运行时的流程且希望在流程的评论列表中记录时不能为空；否则可不传<br/>
	 *            taskId:'',//任务id，当只结束正在运行时的流程且希望在流程的评论列表中记录时不能为空；否则可不传<br/>
	 *            businessKey:'',//业务key<br/>
	 *            contentInfoId:'',//流程的业务id，级联删除用<br/>
	 *            reason:'',//删除理由，可在评论列表中显示<br/>
	 *            isDeleteHistory:'',//是否级联删除，强制归档时为false，不传为true<br/>
	 *            comments:'',//评论列表，操作为强制归档时传输<br/>
	 *            contentInfo:'',//流程的业务信息,操作为强制归档时传输
	 *             }
	 */
	@RequestMapping("/deleteProcessInstance")
	public String deleteStartedProcess(@RequestBody String paramsJson,HttpServletRequest request) {

		final String processInstanceId = GFJsonUtil.get().getProperty(paramsJson, "processInstanceId");
		final String taskId = GFJsonUtil.get().getProperty(paramsJson, "taskId");
		final String businessKey = GFJsonUtil.get().getProperty(paramsJson, "businessKey");
		final String contentInfoId = GFJsonUtil.get().getProperty(paramsJson, "contentInfoId");
		final String reason = GFJsonUtil.get().getProperty(paramsJson, "reason");
		final String isDeleteHistory = GFJsonUtil.get().getProperty(paramsJson, "isDeleteHistory");
		boolean cascade = StringUtil.isNotEmpty(isDeleteHistory)&& "false".equals(isDeleteHistory)?false:true;

		Map<String, Object> resultMap = new HashMap<>();
		try {
			if(StringUtil.isNotEmpty(taskId)) {
				addComment(paramsJson, processInstanceId, taskId, reason);
			}
			deleteProcessInstance(processInstanceId, businessKey, contentInfoId, reason,cascade,request);
			resultMap.put("msg", "流程删除成功");
			resultMap.put("type", "success");
		} catch (Exception e) {
			resultMap.put("msg", "流程删除失败");
			resultMap.put("type", "fail");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}


	private void addComment(String paramsJson, final String processInstanceId, final String taskId,
			final String reason) {

		final String userId = GFJsonUtil.get().getProperty(paramsJson, "userId");
		final JSONObject contentInfo = GFJsonUtil.get().getJSONObject(paramsJson,"contentInfo");
		final JSONArray comments = GFJsonUtil.get().getJSONArray(paramsJson,"comments");

		BaseVO baseVO = this.cusTaskService.getBaseVOByTaskIdOrProcessInstanceId(taskId);

		if(null != contentInfo) {
			baseVO.setContentInfo(contentInfo);
		}
		if(null != comments) {
			Gson gson = new Gson();
			Type type = new TypeToken<List<CommentVO>>(){}.getType();
			List<CommentVO> commentsRequest = gson.fromJson(comments.toString(), type);
			baseVO.setComments(commentsRequest);
		}
		if(null != contentInfo || null != comments) {
			Map<String, Object> variables = new HashMap<>();
			variables.put("entity", baseVO);
			this.cusTaskService.setLocalVariable(taskId, variables);
		}
		// 设置流程的start_userId和评论人的id
		Authentication.setAuthenticatedUserId(userId);
		this.cusTaskService.addComment(taskId, processInstanceId, reason);
	}

	/**
	 * 一键删除流程实例
	 * 
	 * @param json
	 * @return
	 */
	@RequestMapping("/oneButtonToDeleteProcessInstance")
	public String oneButtonToDeleteStartedProcess(@RequestBody String json,HttpServletRequest request) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			JSONArray processList = GFJsonUtil.get().parseArray(json);
			for (int i = 0; i < processList.size(); i++) {
				JSONObject process = (JSONObject) processList.get(i);
				final String processInstanceId = GFJsonUtil.get().getProperty(process, "processInstanceId");
				final String businessKey = GFJsonUtil.get().getProperty(process, "businessKey");
				final String contentInfoId = GFJsonUtil.get().getProperty(process, "contentInfoId");
				final String reason = GFJsonUtil.get().getProperty(process, "reason");
				final String isDeleteHistory = GFJsonUtil.get().getProperty(process, "isDeleteHistory");
				boolean cascade = StringUtil.isNotEmpty(isDeleteHistory)&& "false".equals(isDeleteHistory)?false:true;
				deleteProcessInstance(processInstanceId, businessKey, contentInfoId, reason,cascade,request);
			}

			resultMap.put("msg", "流程删除成功");
			resultMap.put("type", "success");
		} catch (Exception e) {
			resultMap.put("msg", "流程删除失败");
			resultMap.put("type", "error");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 删除流程
	 * @param processInstanceId
	 * @param businessKey
	 * @param contentInfoId
	 * @param reason
	 * @param cascade
	 * @param request
	 * @throws Exception
	 */
	public void deleteProcessInstance(String processInstanceId, String businessKey, String contentInfoId, String reason,boolean cascade,HttpServletRequest request)
			throws Exception {
		if (StringUtil.isNotEmpty(processInstanceId)) {
			this.processService.deleteProcessInstance(processInstanceId, reason, cascade);
		}

		if(cascade) {//级联删除才删除业务信息，否则视为结束流程
			this.userTaskService.deleteByProcDefKey(businessKey);

			final String procDefKey = businessKey.contains(":") ? businessKey.split(":")[0] : "";
			deleteContentInfo(contentInfoId, procDefKey);
			if(StringUtil.isNotEmpty(processInstanceId)) {
				deleteProcessPicture(processInstanceId,request);
			}
		}
	}

	/**
	 * 删除流程图
	 * @param processInstanceId
	 * @param request
	 */
	private void deleteProcessPicture(String processInstanceId,HttpServletRequest request) {
		String processImagesRoot = getProcessImageRoot(processInstanceId, request);
		FileUtil.delete(processImagesRoot);
	}


	/**
	 * 显示流程图
	 * 通过部署id显示图片，不带流程跟踪<br/>
	 * 保存到文件，返回文件名<br/>
	 * 前端可直接用<img :src="processImageUrl">接收<br/>
	 * vue 脚本如下：<br/>
      showProcessImage(row){
        processApi.showProcessImg({
          params:{
            deploymentId: row.deploymentId,
            resourceType:'image'
          }
        }).then(res=>{
          this.processImageModal = true
          this.processName = row.name+" 流程图"
          this.processImageUrl = this.projectPath+"/processImages/"+res.data
        })
      },
	 * @param deploymentId
	 * @param resourceType
	 *            资源类型(xml|image)
	 * @throws Exception
	 */
	@RequestMapping(value = "/showProcessImg")
	public String loadByDeployment(@RequestParam("deploymentId") String deploymentId,
			@RequestParam("resourceType") String resourceType,HttpServletRequest request) throws Exception {
		try {

			String processImagesRoot = getProcessImageRoot(deploymentId, request);
			final String subFix = resourceType.equalsIgnoreCase("xml") ? ".xml":".png";
			String processImageName = deploymentId +"_"+ DateFormatUtil.getDoDay()+subFix;
			final String processImagePath = processImagesRoot+"/"+processImageName;
			File filePath = new File(processImagePath);
			boolean exists = filePath.exists();
			System.out.printf("loadByDeployment %s %s \n", processImageName,exists?"already exists":"does not exist");
			if(!exists) {
				InputStream resourceAsStream = this.processService.getDiagramByProDefinitionId_noTrace(resourceType,
						deploymentId);
				processImageName = saveToFile(processImagesRoot, processImageName, resourceAsStream);
			}
			return processImageName;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * 显示流程追踪图
	 * 获取流程图像，已执行节点和流程线高亮显示<br/>
	 * 返回保存后的图片文件路径，使用方法与显示流程图类似
	 * 
	 * @param processInstanceId
	 * @param request
	 * @throws Exception
	 */
	@RequestMapping(value = "/trace")
	public String traceProcess(@RequestParam("pid") String processInstanceId,HttpServletRequest request)
			throws Exception {
		try {
			// 设置页面不缓存
			String processImagesRoot = getProcessImageRoot(processInstanceId, request);
			String processImageName = System.currentTimeMillis()+".png";
			InputStream resourceAsStream = this.processService.getActivitiProccessImage(processInstanceId);
			processImageName = saveToFile(processImagesRoot, processImageName, resourceAsStream);
			return processImageName;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * 流程图路径
	 * @param processInstanceId
	 * @param request
	 * @return
	 */
	private String getProcessImageRoot(String processInstanceId, HttpServletRequest request) {
		String processImagesRoot = request.getServletContext().getRealPath("/processImages/"+processInstanceId+"/");
		return processImagesRoot;
	}

	/**
	 * 保存到文件，返回文件名
	 *
	 * @param processImagesRoot
	 * @param fileName
	 * @param resourceAsStream
	 * @return
	 * @throws IOException
	 */
	private String saveToFile(String processImagesRoot, String fileName, InputStream resourceAsStream)
			throws IOException {
		//设置文件的保存地址目录
		File filePath = new File(processImagesRoot);

		boolean exists = filePath.exists();
		System.out.printf("saveToFile %s %s \n", filePath,exists?"already exists":"does not exist");
		//如果保存文件的地址不存在，就先创建目录
		if(!exists) {
			filePath.mkdirs();
		}
		FileUtils.copyInputStreamToFile(resourceAsStream, FileUtils.getFile(processImagesRoot+"/"+fileName));
		return fileName;
	}


	/**
	 * 激活流程实例
	 * 
	 * @param processInstanceId
	 * @return
	 */
	@RequestMapping("/activateProcessInstance")
	public String activateProcessInstance(String processInstanceId) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			if (StringUtil.isEmpty(processInstanceId)) {
				resultMap.put("msg", "param processInstanceId can not be empty");
				resultMap.put("type", "fail");
			}else {
				this.processService.activateProcessInstance(processInstanceId);
				resultMap.put("msg", "流程激活成功");
				resultMap.put("type", "success");
			}
		} catch (Exception e) {
			resultMap.put("msg", e.getMessage());
			resultMap.put("type", "fail");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}



	/**
	 * 激活流程定义
	 * 
	 * @param processDefinitionKey
	 * @return
	 */
	@RequestMapping("/activateProcessDefination")
	public String activateProcessDefination(String processDefinitionKey) {
		Map<String, Object> resultMap = new HashMap<>();
		try {

			if (StringUtil.isEmpty(processDefinitionKey)) {
				resultMap.put("msg", "param processDefinitionKey can not be empty");
				resultMap.put("type", "fail");
			}else {
				this.processService.activateProcessDefination(processDefinitionKey);
				resultMap.put("msg", "流程激活成功");
				resultMap.put("type", "success");
			}
		} catch (Exception e) {
			resultMap.put("msg", e.getMessage());
			resultMap.put("type", "fail");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}



	/**
	 * 挂起流程实例,（指的是已启动且未归档的流程)<br/>
	 * 挂起后，获取不到待办事宜（目前获取的是激活状态下的待办）
	 * 
	 * @param processInstanceId
	 * @return
	 */
	@RequestMapping("/suspendProcessInstance")
	public String suspendProcessInstance(String processInstanceId) {

		Map<String, Object> resultMap = new HashMap<>();
		try {

			if (StringUtil.isEmpty(processInstanceId)) {
				resultMap.put("msg", "param processInstanceId can not be empty");
				resultMap.put("type", "fail");
			}else {
				this.processService.suspendProcessInstance(processInstanceId);
				resultMap.put("msg", "流程挂起成功");
				resultMap.put("type", "success");
			}
		} catch (Exception e) {
			resultMap.put("msg", e.getMessage());
			resultMap.put("type", "fail");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}


	/**
	 * 挂起流程定义（包括未启动的和已启动且未归档的流程）<br/>
	 * 已挂起的流程定义无法启动
	 * 
	 * 
	 * @param processDefinitionKey
	 * @return
	 */
	@RequestMapping("/suspendProcessDefination")
	public String suspendProcessDefination(String processDefinitionKey) {

		Map<String, Object> resultMap = new HashMap<>();
		try {
			if (StringUtil.isEmpty(processDefinitionKey)) {
				resultMap.put("msg", "param processDefinitionKey can not be empty");
				resultMap.put("type", "fail");
			}else {
				this.processService.suspendProcessDefinition(processDefinitionKey);
				resultMap.put("msg", "流程挂起成功");
				resultMap.put("type", "success");
			}
		} catch (Exception e) {
			resultMap.put("msg", e.getMessage());
			resultMap.put("type", "fail");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}


	/**
	 * 返回自定义流程的实例用来初始化流程各节点的执行人<br/>
	 * 自定义流程需实现CusProcess接口,可以为空实现
	 * @param processKey 流程定义key
	 * @return
	 */
	public abstract CusProcess getProcess(String processKey);

	/**
	 * 启动流程保存业务的相关信息，在集成时重写实现该方法,可以为空实现
	 * @param procDefKey 流程定义key
	 * @param contentInfo 业务信息
	 */
	public abstract void handleController(HttpServletRequest request,String procDefKey, JSONObject contentInfo) ;

	/**
	 * 处理任务时更新流程的业务信息，在集成时重写实现该方法,可以为空实现
	 * @param contentInfo 业务信息
	 * @param processKey 流程定义key
	 * @param isAutoCompleteNextActiviti 是否自动执行下一节点（归档节点自动执行，即下一节点是否为归档节点），
	 */
	public abstract void updateContentInfo(HttpServletRequest request, JSONObject contentInfo, String processKey, boolean isAutoCompleteNextActiviti);

	/**
	 * 删除流程或流程启动失败后需要删除的业务信息,可以为空实现
	 * @param contentInfoId 业务唯一标识key
	 * @param procDefKey 流程唯一标识key
	 * @throws Exception
	 */
	public abstract void deleteContentInfo(String contentInfoId, String procDefKey) throws Exception;


	/**
	 * 根据userId查找userName,存在多个用逗号分隔，用于显示下一节点接收人，必须实现
	 * 
	 * @param userIds
	 * @return
	 * @throws Exception 
	 */
	public abstract String getUserNames(String userIds);

}
