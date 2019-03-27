package com.zr.activiti.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.zr.activiti.entity.BaseVO;
import com.zr.activiti.entity.CommentVO;
import com.zr.activiti.entity.CusUserTask;
import com.zr.activiti.entity.User;
import com.zr.activiti.service.BaseVOService;
import com.zr.activiti.service.CusTaskService;
import com.zr.activiti.service.CusUserTaskService;
import com.zr.activiti.service.ProcessService;
import com.zr.activiti.service.impl.RequirementService;
import com.zr.activiti.utils.DateFormatUtil;
import com.zr.activiti.utils.FileUtil;
import com.zr.activiti.utils.JsonUtil;
import com.zr.activiti.utils.LocalSessions;
import com.zr.activiti.utils.StringUtil;
import com.zr.activiti.utils.UUIDUtil;


/**
 * 流程相关web类
 * 
 *
 */
@RestController
@RequestMapping("/process")
public class ProcessController {
	public static final String REQUIRECONFIRMATION_PROCESS = "requireConfirmationProcess";// 需求确认流程
	public static final String REQUIRECONFIRMATION_MULTI_PROCESS = "requireConfirmationProcessMultiInstance";// 需求确认流程
	public static final String MONTHLYREPORT_PERSON_PROCESS = "monthlyreportForPersonProcess";// 周报（人力外包）
	public static final String MONTHLYREPORT_PROJECT_PROCESS = "monthlyreportForProjectProcess";// 周报（项目外包/行内人员）
	public static final String REQUIREMENTCONFIRMATION_PROCESS = "requirementConfirmProcess";// 需求确认流程
	public static final String REQUIREMENTSPLIT_PROCESS = "requirementSplitProcess";// 需求拆分流程
	@Resource
	ProcessService processService;
	@Resource
	CusTaskService cusTaskService;
	@Resource
	BaseVOService baseVOService;
	@Resource
	protected CusUserTaskService userTaskService;

	@Resource
	private RequirementService requirementService;
//	@Resource
//	private MonthlyReportService monthlyReportService;

	/**
	 * 查询已部署的流程定义列表
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping("/findDeployedProcessList")
	public String findDeployedProcessList(HttpServletRequest request) {
		List<ProcessDefinition> list = processService.findDeployedProcessList();
		// 定义有序map，相同的key,添加map值后，后面的会覆盖前面的值
		Map<String, ProcessDefinition> map = new LinkedHashMap<String, ProcessDefinition>();
		// 遍历相同的key，替换最新的值
		for (ProcessDefinition pd : list) {
			map.put(pd.getKey(), pd);
		}

		List<ProcessDefinition> linkedList = new LinkedList<ProcessDefinition>(map.values());
		List<Map<String, Object>> processList = new ArrayList<>();
		for (ProcessDefinition pd : linkedList) {
			Map<String, Object> map2 = new HashMap<>();
			map2.put("id", pd.getId());
			map2.put("category", pd.getCategory());
			map2.put("name", pd.getName());
			map2.put("key", pd.getKey());
			map2.put("version", pd.getVersion());
			map2.put("description", pd.getDescription());
			map2.put("resourceName", pd.getResourceName());
			map2.put("deploymentId", pd.getDeploymentId());
			map2.put("suspended", pd.isSuspended());
			map2.put("diagramResourceName", pd.getDiagramResourceName());
			processList.add(map2);
		}
		
		Map<String,Object> resultMap = new HashMap<>();
		resultMap.put("data", processList);
		resultMap.put("projectPath", request.getContextPath());
		
		String json = JsonUtil.get().toJson(resultMap);
		System.out.println("ProcessController json:"+json);

		JsonObject jsonObj = new JsonObject();
		JsonUtil.get().addProperty(jsonObj,"projectPath", request.getContextPath());
		JsonUtil.get().addProperty(jsonObj,"data", processList);
		System.out.println("ProcessController json1:"+jsonObj.toString());
		return json;
	}


	/**
	 * 删除已部署的流程
	 * 
	 * @param deploymentId
	 * @return
	 */
	@RequestMapping("/deleteDeployment")
	public String deleteDeployment(@RequestParam("deploymentId") String deploymentId,HttpServletRequest request) {
		Map<String,Object> result = new HashMap<>();
		try {
			deleteDeployedProcess(deploymentId,request);
			result.put("msg", "流程删除成功");
			result.put("type", "success");
		} catch (Exception e) {
			result.put("msg", "流程删除失败");
			result.put("type", "fail");
			e.printStackTrace();
		}
		return result.toString();
	}

	/**
	 * 一键删除已部署的流程
	 * 
	 * @param json
	 * @return
	 */
	@RequestMapping("/oneButtonToDeleteDeployment")
	public String oneButtonToDeleteDeployment(@RequestBody String json,HttpServletRequest request) {
		Map<String,Object> result = new HashMap<>();
		try {
			JSONArray processList = JsonUtil.get().parseArray(json);
			for (int i = 0; i < processList.size(); i++) {
				JSONObject process = (JSONObject) processList.get(i);
				String deploymentId = JsonUtil.get().getProperty(process, "deploymentId");
				deleteDeployedProcess(deploymentId,request);
			}

			result.put("msg", "流程删除成功");
			result.put("type", "success");
		} catch (Exception e) {
			result.put("msg", "流程删除失败");
			result.put("type", "error");
			e.printStackTrace();
		}
		return result.toString();
	}

	public void deleteDeployedProcess(String deploymentId,HttpServletRequest request) throws Exception {
		List<HistoricProcessInstance> pIList = this.processService.getPIsByDeploymentId(deploymentId);
		for (HistoricProcessInstance historicProcessInstance : pIList) {
			String processInstanceId = historicProcessInstance.getId();
			String businessKey = historicProcessInstance.getBusinessKey();
			BaseVO baseVO = cusTaskService.getBaseVOByTaskIdOrProcessInstanceId(processInstanceId);

			JSONObject jsonObject = baseVO.getContentInfo();
			String contentInfoId = "";
			if (jsonObject != null) {
				contentInfoId = JsonUtil.get().getProperty(jsonObject, "contentInfoId");
			}
			deleteProcessInstance(processInstanceId, businessKey, contentInfoId, "",true,request);
		}
		this.processService.deleteDeployedProcess(deploymentId);
	}

	/**
	 * 启动流程并执行第一个任务
	 * 
	 * @param params:{
	 *            workFlowTitle:'',//流程名称<br/>
	 *            businesskey:'',//业务key，用来绑定流程，命名规则：[流程名称:业务id（唯一）]<br/>
	 *            					比如人力外包周报为：monthlyreportForPersonProcess:113(userId):20180120(startTime):20180127(endTime)<br/>
	 *            reason:'',//申请理由<br/>
	 *            candidate_ids:'',//指定下一节点执行人id<br/>
	 *            candidate_names:'',//指定下一节点执行人name<br/>
	 *            contentInfo:'',//与流程无关的业务信息<br/>
	 *             }
	 */
	@SuppressWarnings("unused")
	@RequestMapping(value = "/start", method = RequestMethod.POST)
	public String startProcess(@RequestParam("params") String params, HttpServletRequest request) {
		System.out.println("ProcessController  startProcess==========paramJson==" + params);

		Map<String,Object> result = new HashMap<>();

		BaseVO baseVO = makeBaseVO(request, params);
		if (baseVO == null) {
			result.put("msg", "流程启动失败,必输参数当前用户user为空");
			result.put("type", "empty");
			return result.toString();
		}

		final String businesskey = baseVO.getBusinessKey();// 设置业务key
		final String procDefKey = businesskey.contains(":") ? businesskey.split(":")[0] : "";
		List<BaseVO> baseVOs = baseVOService.getByBusinessKey(businesskey);
		ProcessInstance instance = null;
		if (baseVOs.size() > 0) {
			result.put("msg", "同一个流程只能申请一次");
			result.put("type", "onetime");
		} else {
			try {

				handleController(request, baseVO, procDefKey);
				initUserTaskAssignees(baseVO);
				Map<String, Object> variables = genareteVariable(baseVO);

				instance = processService.startWorkFlow(baseVO, variables);

				baseVO.setProcessInstanceId(instance.getId());
				baseVOService.save(baseVO);

				List<User> nextAssignes = cusTaskService.excuteFirstTask(instance.getId(),params, baseVO, variables);

				System.out.println("流程启动成功");
				result.put("msg", "流程启动成功");
				result.put("type", "success");
				result.put("nextAssignes", nextAssignes);

			} catch (Exception e) {
				result.put("msg", e);
				result.put("type", "delete");
				deleteProcessInFo(result, baseVO, businesskey, instance,request);
				e.printStackTrace();
			}
		}
		return result.toString();
	}
	private void initUserTaskAssignees(BaseVO baseVO) throws Exception {
		final String processKey = baseVO.getBusinessKey().split("\\:")[0];
		switch (processKey) {
//		case ProcessController.MONTHLYREPORT_PERSON_PROCESS:
//			this.userTaskService.initProcessUserTaskInfo(baseVO,monthlyReportService);
//			break;
//		case ProcessController.MONTHLYREPORT_PROJECT_PROCESS:
//			this.userTaskService.initProcessUserTaskInfo(baseVO,monthlyReportService);
//			break;
		case ProcessController.REQUIREMENTCONFIRMATION_PROCESS:
		case ProcessController.REQUIREMENTSPLIT_PROCESS:
			this.userTaskService.initProcessUserTaskInfo(baseVO,requirementService);
			break;
		}
	}
	private void deleteProcessInFo(Map<String, Object> result, BaseVO baseVO, final String businesskey,ProcessInstance instance,HttpServletRequest request) {
		if (instance != null) {
			try {
				JSONObject jsonObject = baseVO.getContentInfo();
				String contentInfoId = "";
				if (jsonObject != null) {
					contentInfoId = JsonUtil.get().getProperty(jsonObject, "contentInfoId");
				}
			String processInstanceId = instance == null ? "" : instance.getId();
			deleteProcessInstance(processInstanceId,businesskey, contentInfoId, "",true,request);

			} catch (Exception e1) {
				result.put("msg", e1);
				result.put("type", "deleteError");
				e1.printStackTrace();
			}
		}
	}

	private Map<String, Object> genareteVariable(BaseVO baseVO) throws Exception {
		Map<String, Object> variables = new HashMap<>();
		variables.put("applyuserid", baseVO.getCreateId());// 用来设置第一个任务的申请人和重新申请的处理人
//		variables.put("entity", baseVO);
		final String businessKey = baseVO.getBusinessKey();
		List<CusUserTask> userTaskList = this.userTaskService.findByProcAndActivityType(businessKey,CusUserTask.TYPE_MULTI);
		if(null != userTaskList && userTaskList.size()>0) {
			final String procDefKey = businessKey.contains(":") ? businessKey.split(":")[0] : "";
			boolean isWeeklyReport = ProcessController.MONTHLYREPORT_PERSON_PROCESS.equals(procDefKey) || ProcessController.MONTHLYREPORT_PROJECT_PROCESS.equals(procDefKey);
			for (CusUserTask userTask : userTaskList) {
				String taskDefKey = userTask.getTaskDefKey();
				if(isWeeklyReport) {
					variables.put("agreeMembers", 0);// 通过人数
					variables.put("backMembers", 0);// 驳回人数
				}else {
					variables.put("agreeMembers"+"_"+taskDefKey, 0);
					variables.put("backMembers"+"_"+taskDefKey, 0);
				}
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
	 *            workFlowTitle:'',//流程名称<br/>
	 *            businesskey:'',//业务key，用来绑定流程，命名规则：[流程名称:业务id（唯一）]<br/>
	 *            					比如人力外包周报为：monthlyreportForPersonProcess:113(userId):20180120(startTime):20180127(endTime)<br/>
	 *            reason:'',//申请理由<br/>
	 *            candidate_ids:'',//指定下一节点执行人id<br/>
	 *            candidate_names:'',//指定下一节点执行人name<br/>
	 *            contentInfo:'',//与流程无关的业务信息<br/>
	 *             }
	 * @return
	 */
	private BaseVO makeBaseVO(HttpServletRequest request, String params) {

		User currentUser = LocalSessions.getCurrentUser(request);
		if (currentUser == null)
			return null;

		BaseVO baseVO = new BaseVO();
		final String workFlowTitle = JsonUtil.get().getProperty(params, "workFlowTitle");
		final String businesskey = JsonUtil.get().getProperty(params, "businesskey");// 设置业务key
		final String reason = JsonUtil.get().getProperty(params, "reason");
		final JSONObject contentInfo = JsonUtil.get().getJSONObject(params,"contentInfo");
		String contentInfoId = JsonUtil.get().getProperty(params, "contentInfoId");
		String candidate_ids = JsonUtil.get().getProperty(params, "candidate_ids");
		String candidate_names = JsonUtil.get().getProperty(params, "candidate_names");

		baseVO.setTitle(workFlowTitle);
		baseVO.setBusinessKey(businesskey);
		baseVO.setReason(reason);
		baseVO.setCandidate_ids(candidate_ids);// 下一节点执行人id
		baseVO.setCandidate_names(candidate_names);// 下一节点执行人name

		final String procDefKey = businesskey.contains(":") ? businesskey.split(":")[0] : "";
		if (ProcessController.MONTHLYREPORT_PERSON_PROCESS.equals(procDefKey)
				|| ProcessController.MONTHLYREPORT_PROJECT_PROCESS.equals(procDefKey)) {
			contentInfoId = UUIDUtil.uuid();
			contentInfo.put("contentInfoId", contentInfoId);
			contentInfo.put("reportId", contentInfoId);
		}else {
			contentInfo.put("contentInfoId", contentInfoId);
		}
		baseVO.setContentInfo(contentInfo);

		baseVO.setCreateId(currentUser.getUserId());
		// baseVO.setCreateId(currentUser.getCode());
		baseVO.setCreateName(currentUser.getUserName());
		baseVO.setCreateTime(DateFormatUtil.format(new Date()));
		baseVO.setProcessStatus(BaseVO.WAITING_FOR_APPROVAL);// 待审批
		baseVO.setDescription(currentUser.getUserName() + "提出" + workFlowTitle);

		return baseVO;
	}

	private void handleController(HttpServletRequest request, BaseVO baseVO, String processKey) {
		System.out.println("ProcessController handleController:processKey:" + processKey);
		JSONObject jsonObject = baseVO.getContentInfo();
		if (jsonObject == null)
			return;
		switch (processKey) {
		case ProcessController.MONTHLYREPORT_PERSON_PROCESS:
		case ProcessController.MONTHLYREPORT_PROJECT_PROCESS:
			break;
		// case ProcessController.REQUIRECONFIRMATION_PROCESS:
		// case ProcessController.REQUIRECONFIRMATION_MULTI_PROCESS:
		// break;

		}
	}

	/**
	 * 删除流程实例
	 * @param params:{
	 * 			  processInstanceId:'',//流程实例id,不能为空
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
	public String deleteStartedProcess(@RequestParam("params") String params,HttpServletRequest request) {
		System.out.println("ProcessController  deleteStartedProcess==========params==" + params);

		final String processInstanceId = JsonUtil.get().getProperty(params, "processInstanceId");
		final String taskId = JsonUtil.get().getProperty(params, "taskId");
		final String businessKey = JsonUtil.get().getProperty(params, "businessKey");
		final String contentInfoId = JsonUtil.get().getProperty(params, "contentInfoId");
		final String reason = JsonUtil.get().getProperty(params, "reason");
		final String isDeleteHistory = JsonUtil.get().getProperty(params, "isDeleteHistory");
		boolean cascade = StringUtil.isNotEmpty(isDeleteHistory)&& "false".equals(isDeleteHistory)?false:true;

		User currentUser = LocalSessions.getCurrentUser(request);
		Map<String,Object> result = new HashMap<>();
		try {
			if(StringUtil.isNotEmpty(taskId)) {
				addComment(params, processInstanceId, taskId, reason, currentUser);
			}
			deleteProcessInstance(processInstanceId, businessKey, contentInfoId, reason,cascade,request);
			result.put("msg", "流程删除成功");
			result.put("type", "success");
		} catch (Exception e) {
			result.put("msg", "流程删除失败");
			result.put("type", "fail");
			e.printStackTrace();
		}
		return result.toString();
	}

	private void addComment(String paramsJson, final String processInstanceId, final String taskId,
			final String reason, User currentUser) {
		final JSONArray comments = JsonUtil.get().getJSONArray(paramsJson,"comments");
		BaseVO baseVO = (BaseVO) this.cusTaskService.getBaseVOByTaskIdOrProcessInstanceId(taskId);
		

		final JSONObject contentInfo = JsonUtil.get().getJSONObject(paramsJson,"contentInfo");
		if(null != contentInfo) {
			baseVO.setContentInfo(contentInfo);
		}
		if(null != comments) {
			Gson gson = new Gson();
			Type type = new TypeToken<List<CommentVO>>(){}.getType();
			List<CommentVO> commentsRequest = gson.fromJson(comments.toString(), type);
			baseVO.setComments(commentsRequest);
		}
		System.out.println("addComment baseVO:"+baseVO);
		if(null != contentInfo || null != comments) {
			Map<String, Object> variables = new HashMap<>();
			variables.put("entity", baseVO);
			this.cusTaskService.setLocalVariable(taskId, variables);
		}
		Authentication.setAuthenticatedUserId(currentUser.getUserId());
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
		Map<String,Object> result = new HashMap<>();
		try {
			JSONArray processList = JsonUtil.get().parseArray(json);
			for (int i = 0; i < processList.size(); i++) {
				JSONObject process = (JSONObject) processList.get(i);
				final String processInstanceId = JsonUtil.get().getProperty(process, "processInstanceId");
				final String businessKey = JsonUtil.get().getProperty(process, "businessKey");
				final String contentInfoId = JsonUtil.get().getProperty(process, "contentInfoId");
				final String reason = JsonUtil.get().getProperty(process, "reason");
				final String isDeleteHistory = JsonUtil.get().getProperty(process, "isDeleteHistory");
				boolean cascade = StringUtil.isNotEmpty(isDeleteHistory)&& "false".equals(isDeleteHistory)?false:true;
				deleteProcessInstance(processInstanceId, businessKey, contentInfoId, reason,cascade,request);
			}

			result.put("msg", "流程删除成功");
			result.put("type", "success");
		} catch (Exception e) {
			result.put("msg", "流程删除失败");
			result.put("type", "error");
			e.printStackTrace();
		}
		return result.toString();
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
		System.out.println("ProcessController deleteProcessInstance businessKey:" + businessKey + ";contentInfoId:"
				+ contentInfoId + ";processInstanceId:" + processInstanceId);
		if (StringUtil.isNotEmpty(processInstanceId)) {
			this.processService.deleteProcessInstance(processInstanceId, reason, cascade);
		}

		if(cascade) {//级联删除才删除业务信息，否则视为结束流程
			this.userTaskService.deleteByProcDefKey(businessKey);
			this.baseVOService.deleteByBusinessKey(businessKey);
			deleteContentInfo(contentInfoId, businessKey);
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

	private void deleteContentInfo(String contentInfoId, String businessKey) throws Exception {
		System.out.println(
				"ProcessController deleteContentInfo businessKey:" + businessKey + ";contentInfoId:" + contentInfoId);
		if (StringUtil.isEmpty(contentInfoId))
			return;

		final String procDefKey = businessKey.contains(":") ? businessKey.split(":")[0] : "";
		switch (procDefKey) {
		case ProcessController.MONTHLYREPORT_PERSON_PROCESS:
		case ProcessController.MONTHLYREPORT_PROJECT_PROCESS:
//			reportControllder.delete(contentInfoId);
			break;
		// case ProcessController.REQUIRECONFIRMATION_PROCESS:
		// case ProcessController.REQUIRECONFIRMATION_MULTI_PROCESS:
		// break;

		}
	}

	/**
	 * 通过部署id显示图片，不带流程跟踪(没有乱码问题)
	 * 
	 * @param processDefinitionId
	 * @param resourceType
	 *            资源类型(xml|image)
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping(value = "/showProcessImg")
	public String loadByDeployment(@RequestParam("deploymentId") String deploymentId,
			@RequestParam("resourceType") String resourceType,HttpServletRequest request) throws Exception {
		try {
			// 设置页面不缓存
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
	 * 获取流程图像，已执行节点和流程线高亮显示
	 * 
	 * @param processInstanceId
	 * @param response
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
		System.out.println("saveToFile processImagesRoot:"+processImagesRoot+"/"+fileName);
		File filePath = new File(processImagesRoot);
		boolean exists = filePath.exists();
		System.out.printf("saveToFile %s %s \n", filePath,exists?"already exists":"does not exist");
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
		System.out.println("ProcessController  activateProcessInstance==========processInstanceId==" + processInstanceId);
		JSONObject result = new JSONObject();
		try {
			if (StringUtil.isEmpty(processInstanceId)) {
				result.put("msg", "param processInstanceId can not be empty");
				result.put("type", "fail");
				return result.toString();
			}
			this.processService.activateProcessInstance(processInstanceId);
			result.put("msg", "流程激活成功");
			result.put("type", "success");
		} catch (Exception e) {
			result.put("msg", e.getMessage());
			result.put("type", "fail");
			e.printStackTrace();
		}
		return result.toString();
	}
	


	/**
	 * 激活流程定义
	 * 
	 * @param processInstanceId
	 * @return
	 */
	@RequestMapping("/activateProcessDefination")
	public String activateProcessDefination(String processDefinitionKey) {
		System.out.println("ProcessController  activateProcessDefination==========processDefinitionKey==" + processDefinitionKey);
		JSONObject result = new JSONObject();
		try {

			if (StringUtil.isEmpty(processDefinitionKey)) {
				result.put("msg", "param processDefinitionKey can not be empty");
				result.put("type", "fail");
				return result.toString();
			}
			this.processService.activateProcessDefination(processDefinitionKey);
			result.put("msg", "流程激活成功");
			result.put("type", "success");
		} catch (Exception e) {
			result.put("msg", e.getMessage());
			result.put("type", "fail");
			e.printStackTrace();
		}
		return result.toString();
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
		System.out.println("ProcessController  suspengProcess==========processInstanceId==" + processInstanceId);

		JSONObject result = new JSONObject();
		try {

			if (StringUtil.isEmpty(processInstanceId)) {
				result.put("msg", "param processInstanceId can not be empty");
				result.put("type", "fail");
				return result.toString();
			}
			this.processService.suspendProcessInstance(processInstanceId);
			result.put("msg", "流程挂起成功");
			result.put("type", "success");
		} catch (Exception e) {
			result.put("msg", e.getMessage());
			result.put("type", "fail");
			e.printStackTrace();
		}
		return result.toString();
	}
	

	/**
	 * 挂起流程定义（包括未启动的和已启动且未归档的流程）<br/>
	 * 已挂起的流程定义无法启动
	 * 
	 * 
	 * @param processInstanceId
	 * @return
	 */
	@RequestMapping("/suspendProcessDefination")
	public String suspendProcessDefination(String processDefinitionKey) {
		System.out.println("ProcessController  suspendProcessDefination==========processDefinitionKey==" + processDefinitionKey);

		JSONObject result = new JSONObject();
		try {
			if (StringUtil.isEmpty(processDefinitionKey)) {
				result.put("msg", "param processDefinitionKey can not be empty");
				result.put("type", "fail");
				return result.toString();
			}
			this.processService.suspendProcessDefinition(processDefinitionKey);
			result.put("msg", "流程挂起成功");
			result.put("type", "success");
		} catch (Exception e) {
			result.put("msg", e.getMessage());
			result.put("type", "fail");
			e.printStackTrace();
		}
		return result.toString();
	}
	

	/**
	 * 通过部署id显示图片，不带流程跟踪<br/>
	 * 直接返回图片数据<br/>
	 * vue中可直接用a标签显示：<br/>
	 * return h('a',{
                  attrs: {
                    target:'_blank',
                    title:'点击查看流程图',
                    href: this.projectPath+'/process/showProcessImg.do?deploymentId='+params.row.deploymentId+'&resourceType=image',
                  },
                },params.row.name)
	 * 
	 * @param processDefinitionId
	 * @param resourceType
	 *            资源类型(xml|image)
	 * @param response
	 * @throws Exception
	 */
//	@RequestMapping(value = "/showProcessImg")
//	public void loadByDeployment(@RequestParam("processDefinitionId") String processDefinitionId,
//			@RequestParam("resourceType") String resourceType, HttpServletResponse response) throws Exception {
//		try {
//			// 设置页面不缓存
//			response.setHeader("Pragma", "No-cache");
//			response.setHeader("Cache-Control", "no-cache");
//			response.setDateHeader("Expires", 0);
//			if (resourceType.equals("png") || resourceType.equals("image")) {
//				response.setContentType("image/png");
//			}
//			InputStream resourceAsStream = this.processService.getDiagramByProDefinitionId_noTrace(resourceType,
//					processDefinitionId);
//
//			writeToPrinter(response, resourceAsStream);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}


	/**
	 * 获取流程图像，已执行节点和流程线高亮显示<br/>
	 * 返回流程图数据<br/>
	 * vue中可使用方法<br/>
	 * window.open(this.projectPath+'/process/trace.do?pid='+processInstanceId)<br/>
	 * 或者跟显示流程图一样用a标签
	 * @param processInstanceId
	 * @param response
	 * @throws Exception
	 */
//	@RequestMapping(value = "/trace")
//	public void traceProcess(@RequestParam("pid") String processInstanceId, HttpServletResponse response)
//			throws Exception {
//		try {
//			// 设置页面不缓存
//			response.setHeader("Pragma", "No-cache");
//			response.setHeader("Cache-Control", "no-cache");
//			response.setDateHeader("Expires", 0);
//			response.setContentType("image/png");
//
//			InputStream resourceAsStream = this.processService.getActivitiProccessImage(processInstanceId);
//
//			writeToPrinter(response, resourceAsStream);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}


//	private void writeToPrinter(HttpServletResponse response, InputStream resourceAsStream) throws IOException {
//		OutputStream os = response.getOutputStream();
//		int bytesRead = 0;
//		byte[] buffer = new byte[8192];
//		while ((bytesRead = resourceAsStream.read(buffer, 0, 8192)) != -1) {
//			os.write(buffer, 0, bytesRead);
//		}
//		os.close();
//		resourceAsStream.close();
//	}

}
