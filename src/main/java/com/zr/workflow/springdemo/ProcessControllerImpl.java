package com.zr.workflow.springdemo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.zr.workflow.activiti.controller.ProcessController;
import com.zr.workflow.activiti.controller.TaskController;
import com.zr.workflow.activiti.service.CusProcess;
import com.zr.workflow.activiti.util.GFJsonUtil;
import com.zr.workflow.activiti.util.StringUtil;
import com.zr.workflow.springdemo.entity.User;
import com.zr.workflow.springdemo.service.ActivitiProcessService;
import com.zr.workflow.springdemo.service.UserService;

/**
 * 流程开放接口，实现ProcessController
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/process")
public class ProcessControllerImpl extends ProcessController {
	public static final String REQUIRECONFIRMATION_PROCESS = "requireConfirmationProcess";// 需求确认流程
	public static final String REQUIRECONFIRMATION_MULTI_PROCESS = "requireConfirmationProcessMultiInstance";// 需求确认流程
	public static final String MONTHLYREPORT_PERSON_PROCESS = "monthlyreportForPersonProcess";// 周报（人力外包）
	public static final String MONTHLYREPORT_PROJECT_PROCESS = "monthlyreportForProjectProcess";// 周报（项目外包/行内人员）
	public static final String REQUIREMENTCONFIRMATION_PROCESS = "requirementConfirmProcess";// 需求提交流程
	public static final String PROJECTCHANGE_PROCESS = "projectChangeProcess";// 需求变更流程
	public static final String REQUIREMENTSPLIT_PROCESS = "requirementSplitProcess";// 需求拆分流程

//	@Resource
//	private RequirementService requirementService;
//	@Resource
//	private MonthlyReportService monthlyReportService;
	@Resource
	private UserService userService;
	@Resource
	private ActivitiProcessService activitiProcess;

//	@Resource
//	private WeeklyReportController reportController;
//	@Resource
//	private MisDemandController demandController;
//	@Resource
//	private WeeklyReportController reportControllder;
	@Resource
	private TaskController taskControllder;

//	@Resource
//	MisProjectFirstTrialController projectFirstTrialController;

	public CusProcess getProcess(String processKey){
		switch (processKey) {
		//		case MONTHLYREPORT_PERSON_PROCESS:
		//		case MONTHLYREPORT_PROJECT_PROCESS:
		//			return monthlyReportService;
		//		case REQUIREMENTCONFIRMATION_PROCESS:
		//		case REQUIREMENTSPLIT_PROCESS:
		//			return requirementService;
		default:
			return null;
		}
	}

	/**
	 * 正常在项目集成 Activiti 时实现此handleController
	 */
	public void handleController(HttpServletRequest request, String procDefKey,
			JSONObject contentInfo) {
		switch (procDefKey) {
		//		case MONTHLYREPORT_PERSON_PROCESS:
		//		case MONTHLYREPORT_PROJECT_PROCESS:
		//			reportController.saveReport(request, contentInfo.toString());
		//			break;

		}
	}

	/**
	 * 正常在项目集成Activiti时实现此updateContentInfo
	 */
	public void updateContentInfo(HttpServletRequest request, JSONObject contentInfo,
			String processKey, boolean isAutoCompleteNextActiviti) {
		switch (processKey) {
		//		case MONTHLYREPORT_PERSON_PROCESS:
		//		case MONTHLYREPORT_PROJECT_PROCESS:
		//			if(isAutoCompleteNextActiviti) {//归档保存周报
		//				contentInfo.put("reportStatus", "0");
		//			}
		//			reportControllder.updateReport(request, contentInfo.toString());
		//			break;

		}
	}


	/**
	 * 可实现
	 */
	public void deleteContentInfo(String contentInfoId, String procDefKey) throws Exception {
		if (StringUtil.isEmpty(contentInfoId))
			return;

		switch (procDefKey) {
		//		case MONTHLYREPORT_PERSON_PROCESS:
		//		case MONTHLYREPORT_PROJECT_PROCESS:
		//			reportController.delete(contentInfoId);
		//			break;
		//		case REQUIREMENTCONFIRMATION_PROCESS:
		//		case REQUIREMENTSPLIT_PROCESS:
		//			demandController.deleteDemandRecord(contentInfoId);
		//			projectFirstTrialController.deleteByDemandId(contentInfoId);
		//			break;

		}
	}


	public String getUserNames(String userIds) {
		String userName = "";
		List<User> users = this.userService.findUsersByIds(userIds);
		for (User user : users) {
			userName += user.getUserName() + ",";
		}
		return userName;
	}
	

	/**
	 * 激活流程实例并执行任务
	 * 
	 * @param processInstanceId
	 * @return
	 */
	@RequestMapping("/activateAndExcuteProcessInstance")
	public String activateAndExcuteProcessInstance(String processInstanceId, HttpServletRequest request) {
		Map<String, Object> resultMap = new HashMap<>();
		try {
			if (StringUtil.isEmpty(processInstanceId)) {
				resultMap.put("msg", "param processInstanceId can not be empty");
				resultMap.put("type", "fail");
			}else {
				activateProcessInstance(processInstanceId);
				HashMap<String, String> currentActivitiInfo = activitiProcess.getCurrentActiviti(processInstanceId);
				taskControllder.handleTask(currentActivitiInfo.get("currentActiviti"), request);
				activitiProcess.deleteActiviti(processInstanceId);
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
	 * 挂起流程实例并保存当前节点信息,（指的是已启动且未归档的流程)<br/>
	 * 挂起后，获取不到待办事宜（目前获取的是激活状态下的待办）
	 * 
	 * @param processInstanceId
	 * @return
	 */
	@RequestMapping("/suspendAndSaveProcessInstance")
	public String suspendAndSaveProcessInstance(@RequestBody String json) {

		Map<String, Object> resultMap = new HashMap<>();
		try {

			String processInstanceId = GFJsonUtil.get().getProperty(json, "processInstanceId");
			
			if (StringUtil.isEmpty(processInstanceId)) {
				resultMap.put("msg", "param processInstanceId can not be empty");
				resultMap.put("type", "fail");
			}else {
				activitiProcess.saveCurrentActiviti(processInstanceId,json);
				suspendProcessInstance(processInstanceId);
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
