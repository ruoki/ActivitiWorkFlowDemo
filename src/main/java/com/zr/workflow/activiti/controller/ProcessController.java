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
import com.google.gson.reflect.TypeToken;
import com.zr.workflow.activiti.entity.BaseVO;
import com.zr.workflow.activiti.entity.CommentVO;
import com.zr.workflow.activiti.entity.CusUserTask;
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
	protected CusUserTaskService userTaskService;


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

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("data", processList);
		resultMap.put("projectPath", request.getContextPath());
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 删除已部署的流程
	 * 
	 * @param deploymentId
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
	 * @param json
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

			JSONObject jsonObject = baseVO.getContentInfo();
			String contentInfoId = "";
			if (jsonObject != null) {
				contentInfoId = GFJsonUtil.get().getProperty(jsonObject, "contentInfoId");
			}
			deleteProcessInstance(processInstanceId, businessKey, contentInfoId, "",true,request);
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
	 *            businesskey:'',//业务key，用来绑定流程，命名规则：[流程名称:业务id（唯一）]<br/>
	 *            					比如人力外包周报为：monthlyreportForPersonProcess:113(userId):20180120(startTime):20180127(endTime)<br/>
	 *            reason:'',//申请理由<br/>
	 *            candidate_ids:'',//指定下一节点执行人id<br/>
	 *            candidate_names:'',//指定下一节点执行人name<br/>
	 *            contentInfo:'',//与流程无关的业务信息<br/>
	 *             }
	 */
	@RequestMapping(value = "/start", method = RequestMethod.POST)
	public String startProcess(@RequestParam("params") String params, HttpServletRequest request) {
		System.out.println("ProcessController  startProcess==========paramJson==" + params);
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

					initUserTaskAssignees(baseVO);//初始化流程已有的节点执行人信息

					Map<String, Object> variables = genareteVariable(baseVO);//初始化必要的流程变量

					instance = processService.startWorkFlow(baseVO, variables);//启动流程

					baseVO.setProcessInstanceId(instance.getId());

					//执行第一个任务
					String isPass = GFJsonUtil.get().getProperty(params, "isPass");
					List<String> nextAssignes = cusTaskService.excuteFirstTask(instance.getId(),isPass, baseVO, variables);

					System.out.println("流程启动成功");
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
	 * 例如下面的requirementService即为实现了CusProcess接口的自定义类，自行实现
	 * 
	 * @param baseVO
	 * @throws Exception
	 */
	private void initUserTaskAssignees(BaseVO baseVO) throws Exception {
		final String processKey = baseVO.getBusinessKey().split("\\:")[0];
		switch (processKey) {
		//		case ProcessController.REQUIREMENTSPLIT_PROCESS:
		//			this.userTaskService.initProcessUserTaskInfo(baseVO,requirementService);
		//			break;
		default://默认从前端页面获取candidate_ids和candidate_names进行设置节点执行人
			this.userTaskService.initProcessUserTaskInfo(baseVO,null);
			break;
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
		System.out.println("ProcessController deleteProcessInFo instance:"+instance);
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
	 *            userId:'',//流程创建者id<br/>
	 *            userName:'',//流程创建者名称<br/>
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
			final String procDefKey = businesskey.contains(":") ? businesskey.split(":")[0] : businesskey;
			if (ProcessController.MONTHLYREPORT_PERSON_PROCESS.equals(procDefKey)
					|| ProcessController.MONTHLYREPORT_PROJECT_PROCESS.equals(procDefKey)) {
				//			contentInfoId = UUIDUtil.uuid();
				//			contentInfo.put("contentInfoId", contentInfoId);
				//			contentInfo.put("reportId", contentInfoId);
			}else {
				contentInfo.put("contentInfoId", contentInfoId);
			}
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
	 * @param params:{
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
	public String deleteStartedProcess(@RequestParam("params") String paramsJson,HttpServletRequest request) {
		System.out.println("ProcessController  deleteStartedProcess==========params==" + paramsJson);

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

		BaseVO baseVO = (BaseVO) this.cusTaskService.getBaseVOByTaskIdOrProcessInstanceId(taskId);

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
		System.out.println("ProcessController deleteProcessInstance businessKey:" + businessKey + ";contentInfoId:"
				+ contentInfoId + ";processInstanceId:" + processInstanceId);
		if (StringUtil.isNotEmpty(processInstanceId)) {
			this.processService.deleteProcessInstance(processInstanceId, reason, cascade);
		}

		if(cascade) {//级联删除才删除业务信息，否则视为结束流程
			this.userTaskService.deleteByProcDefKey(businessKey);
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
	 * @param request
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
		System.out.println("saveToFile processImagesRoot:"+processImagesRoot+"/"+fileName);
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
		System.out.println("ProcessController  activateProcessInstance==========processInstanceId==" + processInstanceId);
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
	 * @param processInstanceId
	 * @return
	 */
	@RequestMapping("/activateProcessDefination")
	public String activateProcessDefination(String processDefinitionKey) {
		System.out.println("ProcessController  activateProcessDefination==========processDefinitionKey==" + processDefinitionKey);
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
		System.out.println("ProcessController  suspengProcess==========processInstanceId==" + processInstanceId);

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
	 * @param processInstanceId
	 * @return
	 */
	@RequestMapping("/suspendProcessDefination")
	public String suspendProcessDefination(String processDefinitionKey) {
		System.out.println("ProcessController  suspendProcessDefination==========processDefinitionKey==" + processDefinitionKey);

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
}
