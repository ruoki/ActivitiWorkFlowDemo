package com.zr.workflow.springdemo;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.zr.workflow.activiti.controller.ProcessController;
import com.zr.workflow.activiti.service.CusProcess;
import com.zr.workflow.activiti.util.StringUtil;
import com.zr.workflow.springdemo.entity.User;
import com.zr.workflow.springdemo.service.UserService;

/**
 * 流程开放接口，实现ProcessController
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/process")
public class ProcessControllerImpl extends ProcessController {
	//	public static final String REQUIRECONFIRMATION_PROCESS = "requireConfirmationProcess";// 需求确认流程
	//	public static final String REQUIRECONFIRMATION_MULTI_PROCESS = "requireConfirmationProcessMultiInstance";// 需求确认流程
	//	public static final String MONTHLYREPORT_PERSON_PROCESS = "monthlyreportForPersonProcess";// 周报（人力外包）
	//	public static final String MONTHLYREPORT_PROJECT_PROCESS = "monthlyreportForProjectProcess";// 周报（项目外包/行内人员）
	//	public static final String REQUIREMENTCONFIRMATION_PROCESS = "requirementConfirmProcess";// 需求确认流程
	//	public static final String REQUIREMENTSPLIT_PROCESS = "requirementSplitProcess";// 需求拆分流程

	@Resource
	private UserService userService;

	//	@Resource
	//	private RequirementService requirementService;
	//	@Resource
	//	private MonthlyReportService monthlyReportService;
	//
	//	@Resource
	//	private WeeklyReportController reportController;
	//	@Resource
	//	private MisDemandController demandController;
	//	@Resource
	//	private WeeklyReportController reportControllder;

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

}
