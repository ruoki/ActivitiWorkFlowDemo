package com.zr.workflow.activiti.controller;

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

import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.avalon.framework.parameters.ParameterException;
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
import com.zr.workflow.activiti.entity.ProcessStatus;
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
	public String findDeployedProcessList(@RequestParam(value = "page", required = false) Integer page,
										  @RequestParam(value = "rows", required = false) Integer rows,HttpServletRequest request) {
		List<Map<String, Object>> processList = new ArrayList<>();
		List<ProcessDefinition> linkedList = processService.findLastetDeployedProcessList();

		int listSize = linkedList.size();
		Page<ProcessDefinition> p = (null == page || null == rows) ? null : new Page<>(page, rows);
		if(null != p && listSize > 0) {//分页
			int[] indexs = this.processService.getIndex(p,listSize);
			linkedList = linkedList.subList(indexs[0], indexs[1]);
		}
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
		
		/*亦显示旧版本-start*/
//		Map<String, Integer> map = new HashMap<String, Integer>();
//		int i = 0;
//		for (ProcessDefinition pd : list) {
//			Map<String, Object> map2 = new HashMap<>();
//
//			final String processKey = pd.getKey();
//			if(map.containsKey(processKey)) {
//				int index = map.get(processKey);
//				int versionOld = (int) processList.get(index).get("version");
//				int versionNew = pd.getVersion();
//				if(versionNew > versionOld) {
//					processList.get(index).put("deprecated", true);
//				}else {
//					map2.put("deprecated", true);
//				}
//			}else {
//				map.put(processKey, i);
//			}
//
//			map2.put("id", pd.getId());
//			map2.put("category", pd.getCategory());
//			map2.put("name", pd.getName());
//			map2.put("key", processKey);
//			map2.put("version", pd.getVersion());
//			map2.put("description", pd.getDescription());
//			map2.put("resourceName", pd.getResourceName());
//			map2.put("deploymentId", pd.getDeploymentId());
//			map2.put("suspended", pd.isSuspended());
//			map2.put("diagramResourceName", pd.getDiagramResourceName());
//			processList.add(map2);
//			i++;
//		}
		/*亦显示旧版本-end*/
		

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("data", processList);
		resultMap.put("totalSize", listSize);
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
			if(StringUtil.isEmpty(deploymentId)){
				resultMap.put("type", "empty");
				resultMap.put("msg", "必输参数:流程部署deploymentId不能为空");
			}else {
				deleteDeployedProcess(deploymentId, request);
				resultMap.put("msg", "流程删除成功");
				resultMap.put("type", "success");
			}
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
	 * @param json:[]//选中的要删除的流程信息
	 * @return
	 */
	@RequestMapping("/oneButtonToDeleteDeployment")
	public String oneButtonToDeleteDeployment(@RequestBody String json,HttpServletRequest request) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			JSONArray processList = GFJsonUtil.get().parseArray(json);
			int failIndex = -1;
			for (int i = 0; i < processList.size(); i++) {
				JSONObject process = (JSONObject) processList.get(i);
				String deploymentId = GFJsonUtil.get().getProperty(process, "deploymentId");
				if(StringUtil.isEmpty(deploymentId)){
					failIndex = i;
					break;
				}
				deleteDeployedProcess(deploymentId,request);
			}

			if(failIndex != -1){
				resultMap.put("type", "empty");
				resultMap.put("msg", "必输参数:第"+failIndex+"个流程部署deploymentId为空");
			}else {
				resultMap.put("msg", "流程删除成功");
				resultMap.put("type", "success");
			}

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
			BaseVO baseVO = cusTaskService.getBaseVOByTaskIdOrProcessInstanceId("",processInstanceId);
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
	 * 启动流程并执行第一个任务
	 *
	 * @param params:{
	 *            userId:'',//流程创建者id<br/>
	 *            userName:'',//流程创建者名称<br/>
	 *            workFlowTitle:'',//流程名称<br/>
	 *            businesskey:'',//业务key，用来绑定流程，命名规则：[流程key:业务id（唯一）]<br/>
	 *            					比如人力外包周报为：monthlyreportForPersonProcess:113(userId):20180120(startTime):20180127(endTime)<br/>
	 *            reason:'',//申请理由<br/>
	 *            candidate_ids:'',//指定下一节点执行人id<br/>
	 *            candidate_names:'',//指定下一节点执行人name<br/>
	 *            contentInfoId:'',//业务id，用于删除流程时删除相关的业务信息<br/>
	 *            contentInfo:'',//与流程无关的业务信息<br/>
	 *            excuteFirstTask:'true',//是否自动执行第一个任务<br/>
	 *             }
	 */
	@RequestMapping(value = "/start")
	public String startProcess(@RequestBody String params, HttpServletRequest request) {
		Map<String, Object> resultMap = new HashMap<>();

		try {
			BaseVO baseVO = makeBaseVO(params);

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
					variables.put("entity", baseVO);
					instance = processService.startWorkFlowByKey(baseVO.getBusinessKey(),baseVO.getCreateId(), variables);//启动流程

					if(instance != null && !StringUtil.isEmpty(instance.getId())) {
						baseVO.setProcessInstanceId(instance.getId());
						checkExcuteFirstTask(params, resultMap, baseVO, variables);
						resultMap.put("msg", "流程启动成功");
						resultMap.put("type", "success");
					}else{
						resultMap.put("msg", "流程启动失败");
						resultMap.put("type", "delete");
						deleteProcessInFo(resultMap, baseVO, businesskey, instance,request);
					}
				} catch (Exception e) {
					resultMap.put("msg", e.getMessage());
					resultMap.put("type", "delete");
					deleteProcessInFo(resultMap, baseVO, businesskey, instance,request);
					e.printStackTrace();
				}
			}
		} catch (ParameterException e) {
			resultMap.put("type", "empty");
			resultMap.put("msg", e.getMessage());
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 执行第一个任务
	 * @param params
	 * @param resultMap
	 * @param baseVO
	 * @param variables
	 * @throws Exception
	 */
	private void checkExcuteFirstTask(@RequestBody String params, Map<String, Object> resultMap, BaseVO baseVO,  Map<String, Object> variables) throws Exception {

		String excuteFirstTaskStr = GFJsonUtil.get().getProperty(params, "excuteFirstTask");
		boolean excuteFirstTask = "false".equals(excuteFirstTaskStr) ? false : true;
		if(excuteFirstTask) {
			//执行第一个任务
			String isPass = GFJsonUtil.get().getProperty(params, "isPass");
			List<String> nextAssignes = cusTaskService.excuteFirstTask(baseVO.getProcessInstanceId(), isPass, baseVO, variables);
			resultMap.put("nextAssignes", nextAssignes);
		}
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
	 * @param nextActivitiId 下一节点key
	 * @param isChangeData
	 * @throws Exception
	 */
	public void updateNextCusUserTaskAssigness(BaseVO baseVO, String nextActivitiId, boolean isChangeData) throws Exception {
		final String processKey = baseVO.getBusinessKey().split("\\:")[0];
		CusProcess cusProcess = getProcess(processKey);
		if(null != cusProcess) {
			this.userTaskService.updateNextCusUserTaskInfo(baseVO, nextActivitiId,isChangeData,cusProcess);
		}else {
			this.userTaskService.updateNextCusUserTaskInfo(baseVO, nextActivitiId,isChangeData,null);
		}
	}

	/**
	 * 启动失败则删除流程实例相关信息
	 * @param resultMap
	 * @param baseVO
	 * @param businessKey
	 * @param instance
	 */
	private void deleteProcessInFo(Map<String, Object> resultMap, BaseVO baseVO, final String businessKey,
			ProcessInstance instance,HttpServletRequest request) {
		if (instance != null) {
			try {
				JSONObject jsonObject = baseVO.getContentInfo();

				String contentInfoId = GFJsonUtil.get().getProperty(jsonObject, "contentInfoId");
				String processInstanceId = instance == null ? "" : instance.getId();
				deleteProcessInstance(processInstanceId,businessKey, contentInfoId, "",true,request);

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
	 *
	 * @param params:{
	 *            userId:'',//流程创建者id<br/>
	 *            userName:'',//流程创建者名称<br/>
	 *            workFlowTitle:'',//流程名称<br/>
	 *            businesskey:'',//业务key，用来绑定流程，命名规则：[流程key:业务id（唯一）]<br/>
	 *            					比如人力外包周报为：monthlyreportForPersonProcess:113(userId):20180120(startTime):20180127(endTime)<br/>
	 *            reason:'',//申请理由<br/>
	 *            candidate_ids:'',//指定下一节点执行人id<br/>
	 *            candidate_names:'',//指定下一节点执行人name<br/>
	 *            contentInfoId:'',//业务id，注意，放在contentInfo中，用于删除流程时删除相关的业务信息<br/>
	 *            contentInfo:'',//与流程无关的业务信息<br/>
	 *             }
	 * @return
	 */
	private BaseVO makeBaseVO(String params) throws ParameterException {

		final String userId = GFJsonUtil.get().getProperty(params, "userId");
		final String userName = GFJsonUtil.get().getProperty(params, "userName");
		final String workFlowTitle = GFJsonUtil.get().getProperty(params, "workFlowTitle");
		final String businesskey = GFJsonUtil.get().getProperty(params, "businesskey");// 设置业务key

		String errorMsg = "";
		if(StringUtil.isEmpty(userId)){
			errorMsg = "必输参数:流程申请人userId不能为空";
		}
		if(StringUtil.isEmpty(userName)){
			errorMsg = "必输参数:流程申请人名称userName不能为空";
		}
		if(StringUtil.isEmpty(workFlowTitle)){
			errorMsg = "必输参数:流程名称workFlowTitle不能为空";
		}
		if(StringUtil.isEmpty(businesskey)){
			errorMsg = "必输参数:流程businesskey不能为空，组成格式为:流程key:业务id(流程key和业务id中间用冒号隔开)";
		}

		if(StringUtil.isNotEmpty(errorMsg)){
			throw new ParameterException(errorMsg);
		}

		final String reason = GFJsonUtil.get().getProperty(params, "reason");
		String contentInfoId = GFJsonUtil.get().getProperty(params, "contentInfoId");
		final JSONObject contentInfo = GFJsonUtil.get().getJSONObject(params,"contentInfo");
		String candidate_ids = GFJsonUtil.get().getProperty(params, "candidate_ids");
		String candidate_names = GFJsonUtil.get().getProperty(params, "candidate_names");

		BaseVO baseVO = new BaseVO();
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
		baseVO.setProcessStatus(ProcessStatus.WAITING_FOR_APPROVAL);// 待审批
		baseVO.setDescription(userName + "提出" + workFlowTitle);
		return baseVO;
	}

	/**
	 * 一键删除流程实例
	 *
	 * @param json
	 * @return
	 */
	@RequestMapping("/oneButtonToDeleteProcessInstance")
	public String oneButtonToDeleteProcessInstance(@RequestBody String json,HttpServletRequest request) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			JSONArray processList = GFJsonUtil.get().parseArray(json);
			for (int i = 0; i < processList.size(); i++) {
				JSONObject process = (JSONObject) processList.get(i);
				deleteProcessInstance(process.toString(),request);
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
	public String deleteProcessInstance(@RequestBody String paramsJson,HttpServletRequest request) {

		Map<String, Object> resultMap = new HashMap<>();
		final String processInstanceId = GFJsonUtil.get().getProperty(paramsJson, "processInstanceId");
		if(StringUtil.isEmpty(processInstanceId)){
			resultMap.put("type", "empty");
			resultMap.put("msg", "必输参数:流程实例processInstanceId不能为空");
			String resultJson = GFJsonUtil.get().toJson(resultMap);
			return resultJson;
		}
		final String taskId = GFJsonUtil.get().getProperty(paramsJson, "taskId");
		final String businessKey = GFJsonUtil.get().getProperty(paramsJson, "businessKey");
		final String contentInfoId = GFJsonUtil.get().getProperty(paramsJson, "contentInfoId");
		final String reason = GFJsonUtil.get().getProperty(paramsJson, "reason");
		final String isDeleteHistory = GFJsonUtil.get().getProperty(paramsJson, "isDeleteHistory");
		boolean cascade = StringUtil.isNotEmpty(isDeleteHistory)&& "false".equals(isDeleteHistory)?false:true;

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

		BaseVO baseVO = this.cusTaskService.getBaseVOByTaskIdOrProcessInstanceId("",taskId);

		if(null != contentInfo) {
			baseVO.setContentInfo(contentInfo);
		}
		if(null != comments) {
			List<CommentVO> commentsRequest = getCommentListFromJson(comments);
			baseVO.setComments(commentsRequest);
		}
		if(null != contentInfo || null != comments) {
			Map<String, Object> variables = new HashMap<>();
			variables.put("entity", baseVO);
			this.cusTaskService.setLocalVariable(taskId, variables);
		}
		// 设置流程的start_userId和评论人的id
		Authentication.setAuthenticatedUserId(userId);
		this.cusTaskService.addComment(taskId, processInstanceId,ProcessStatus.ARCHIVE, reason);
	}
	public List<CommentVO> getCommentListFromJson(JSONArray comments) {
		Gson gson = new Gson();
		Type type = new TypeToken<List<CommentVO>>(){}.getType();
		return gson.fromJson(comments.toString(), type);
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
	 */
	@RequestMapping(value = "/showProcessImg")
	public String showProcessImg(@RequestParam("deploymentId") String deploymentId,
								 @RequestParam("resourceType") String resourceType,HttpServletRequest request){
		try {

			if(StringUtil.isEmpty(deploymentId)){
				return "empty：必输参数:流程部署deploymentId不能为空";
			}
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
	 */
	@RequestMapping(value = "/trace")
	public String traceProcess(@RequestParam("pid") String processInstanceId,HttpServletRequest request){
		try {
			if(StringUtil.isEmpty(processInstanceId)){
				return "empty：必输参数:流程实例processInstanceId不能为空";
			}
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
				resultMap.put("msg", "必输参数:流程实例processInstanceId不能为空");
				resultMap.put("type", "empty");
			}else {
			    ProcessInstance instance = this.processService.getProcessInstanceById(processInstanceId);
			    if(instance.isSuspended()) {
                    this.processService.activateProcessInstance(processInstanceId);
                    resultMap.put("msg", "流程激活成功");
                    resultMap.put("type", "success");
                }else {
                    resultMap.put("msg", "流程已处于激活状态，无需激活");
                    resultMap.put("type", "success");
                }
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
	@RequestMapping("/activateProcessDefinition")
	public String activateProcessDefinition(String processDefinitionKey) {
		Map<String, Object> resultMap = new HashMap<>();
		try {

			if (StringUtil.isEmpty(processDefinitionKey)) {
				resultMap.put("msg", "流程processDefinitionKey不能为空");
				resultMap.put("type", "empty");
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
				resultMap.put("msg", "必输参数:流程实例processInstanceId不能为空");
				resultMap.put("type", "empty");
			}else {
                ProcessInstance instance = this.processService.getProcessInstanceById(processInstanceId);
                if(instance.isSuspended()) {
                    resultMap.put("msg", "流程已处于挂起状态，无需挂起");
                    resultMap.put("type", "success");
                }else {
                    this.processService.suspendProcessInstance(processInstanceId);
                    resultMap.put("msg", "流程挂起成功");
                    resultMap.put("type", "success");
                }
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
				resultMap.put("msg", "流程processDefinitionKey不能为空");
				resultMap.put("type", "empty");
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
	 * 查询所有流程节点
	 * @param processDefinitionId 流程定义id，两个只能传一个，最好传该参数
	 * @param processInstanceId 流程实例id,两个只能传一个
	 * @param activityType 为空或不传时查询所有节点；
	 * 					'userTask':用户节点；
	 * 					'exclusiveGateway':网关节点
	 * @return
	 */
	@RequestMapping("/getAllActivities")
	public String getAllActivities(@RequestParam(value = "processDefinitionId",required = false) String processDefinitionId,
								   @RequestParam(value = "processInstanceId",required = false) String processInstanceId,
								   @RequestParam(value = "activityType", required = false) String activityType) {
		Map<String, Object> resultMap = new HashMap<>();
		if(StringUtil.isEmpty(processDefinitionId)&& StringUtil.isEmpty(processInstanceId)){
			resultMap.put("type", "empty");
			resultMap.put("msg", "必传参数不能为空");
		}else if(StringUtil.isNotEmpty(processDefinitionId)&& StringUtil.isNotEmpty(processInstanceId)){
			resultMap.put("type", "error");
			resultMap.put("msg", "参数 processDefinitionId 或 processInstanceId 只能传一个");
		}else {
			try {

				List<Map<String, Object>> activityList = new ArrayList<>();
				List<ActivityImpl> activities;
				if(StringUtil.isNotEmpty(processDefinitionId)){
					activities = this.processService.getActivitiesByProcessDefinition(processDefinitionId,activityType);
				}else {
					activities = this.processService.getActivitiesByProcessInstance(processInstanceId,activityType);
				}
				for (ActivityImpl activity : activities) {
					Map<String, Object> map2 = new HashMap<>();
					map2.put("activityId", activity.getId());
					map2.put("name", activity.getProperty("name"));
					map2.put("type", activity.getProperty("type"));
					map2.put("description", activity.getProperty("documentation"));
					map2.put("processDefinitionId", activity.getProcessDefinition().getId());
					map2.put("deploymentId", activity.getProcessDefinition().getDeploymentId());
					activityList.add(map2);
				}
				System.out.println("所有节点 ：" + activityList);
				resultMap.put("type", "success");
				resultMap.put("data", activityList);
			} catch (Exception e) {
				resultMap.put("type", "error");
				resultMap.put("msg", e.getMessage());
				e.printStackTrace();
			}
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 查询所有流程历史中已执行节点
	 * @param processInstanceId
	 * @param activityType 为空或不传时查询所有节点；
	 * 					'userTask':用户节点；
	 * 					'exclusiveGateway':网关节点
	 * @return
	 */
	@RequestMapping("/getAllFinishedActivities")
	public String getAllFinishedActivities(@RequestParam("processInstanceId") String processInstanceId,@RequestParam(value = "activityType", required = false) String activityType) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			if (StringUtil.isEmpty(processInstanceId)) {
				resultMap.put("msg", "必输参数:流程实例processInstanceId不能为空");
				resultMap.put("type", "empty");
			}else {
				List<HistoricActivityInstance> linkedList = getFinishedActivityInstances(processInstanceId, activityType);

				System.out.println("所有已执行节点 ：" + linkedList);
				resultMap.put("type", "success");
				resultMap.put("data", linkedList);
			}
		} catch (Exception e) {
			resultMap.put("type", "error");
			resultMap.put("msg", e.getMessage());
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	public List<HistoricActivityInstance> getFinishedActivityInstances(@RequestParam("processInstanceId") String processInstanceId, @RequestParam(value = "activityType", required = false) String activityType) {
		List<HistoricActivityInstance> hiActivityInstances = this.processService.getFinishedActivityInstanceList(processInstanceId,activityType);
		// 定义有序map，相同的key,只添加一次
		Map<String, HistoricActivityInstance> map = new LinkedHashMap<>();
		for (HistoricActivityInstance pd : hiActivityInstances) {
			if(!map.containsKey(pd.getActivityId())) {
				map.put(pd.getActivityId(), pd);
			}
		}
		return new LinkedList<>(map.values());
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
