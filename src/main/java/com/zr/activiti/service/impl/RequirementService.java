package com.zr.activiti.service.impl;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.zr.activiti.entity.BaseVO;
import com.zr.activiti.entity.CusUserTask;
import com.zr.activiti.service.CusProcess;
import com.zr.activiti.service.UserService;
import com.zr.activiti.utils.StringUtil;


/**
 * 需求确认/需求拆分流程service
 * 
 * @author Administrator
 *
 */
@Service
public class RequirementService implements CusProcess{
	public static final String ARCHITECTUREMANAGERPROJECT = "architectureManagerProject";// 架构经理立项
	public static final String PRODUCTINNOVATIONCENTERAUDIT = "productInnovationCenterAudit";// 需求初审
	
//	@Resource
//	private MisRoleService misRoleService;
	@Resource
	private UserService userService;

	
	public void setUserTaskAssgine(BaseVO baseVO, CusUserTask cusUserTask,String assigneeExpression) throws Exception {
		final String taskDefKey = cusUserTask.getTaskDefKey();
		final String activitiType = cusUserTask.getActivityType();
//		final String businessKey = baseVO.getBusinessKey();
		final String userId = baseVO.getCreateId();
//		final String projectId = businessKey.contains(":") ? businessKey.split(":")[1] : "";
		
		boolean isCandidateUserTask = ARCHITECTUREMANAGERPROJECT.equals(taskDefKey) ||PRODUCTINNOVATIONCENTERAUDIT.equals(taskDefKey) || taskDefKey.contains("pmo");
		if(CusUserTask.TYPE_NORMAL.equals(activitiType) && !isCandidateUserTask){
			setAssignee(cusUserTask, taskDefKey, baseVO,assigneeExpression,userId);
		} else  {
			setCandidateUsers(baseVO, cusUserTask, taskDefKey);
		}
	}


	/**
	 * 设置指定执行人
	 * @param cusUserTask 
	 * @param taskDefKey activiti的节点key
	 * @param projectId 项目id
	 * @param assigneeExpression 流程中设置的指定人变量表达式
	 * @param userId 流程的发起人
	 * @throws Exception
	 */
	private void setAssignee(CusUserTask cusUserTask, final String taskDefKey, final BaseVO baseVO, String assigneeExpression, String userId)
			throws Exception {
		Map<String, String> candidateUser = new HashMap<>();
		cusUserTask.setTaskType(CusUserTask.TYPE_ASSIGNEE);
		
		if(StringUtil.isEmpty(assigneeExpression)) {
			String user_id = baseVO.getCandidate_ids();
			String userName = baseVO.getCandidate_names();
			candidateUser.put("candidateIds", user_id);
			candidateUser.put("candidateNames", userName);
		}else {
			if(assigneeExpression.contains("applyuserid")) {
				candidateUser = userService.getUserFromMisUser(userId);
			}
		}
		
		String candidateIds = candidateUser.get("candidateIds");
		String candidateNames = candidateUser.get("candidateNames");
		cusUserTask.setCandidate_ids(candidateIds);
		cusUserTask.setCandidate_name(candidateNames);
	}


	/**
	 * 设置候选人(多个人)
	 * @param baseVO
	 * @param cusUserTask
	 * @param taskDefKey
	 */
	private void setCandidateUsers(BaseVO baseVO, CusUserTask cusUserTask, final String taskDefKey) {
		
		cusUserTask.setTaskType(CusUserTask.TYPE_CANDIDATEUSER);
		

		String candidateIds = baseVO.getCandidate_ids();
		String candidateNames = baseVO.getCandidate_names();
//		if(taskDefKey.contains("pmo")) {
//			System.out.println("RequirementService setCandidateUsers getPMO");
//			Map<String, String> candidateUser = getCandidateUserByRole("pmo");
//			candidateIds = candidateUser.get("candidateIds");
//			candidateNames = candidateUser.get("candidateNames");
//		}else if(PRODUCTINNOVATIONCENTERAUDIT.equals(taskDefKey)) {
//			System.out.println("RequirementService setCandidateUsers getProCrePrincipal");
//			Map<String, String> candidateUser = getCandidateUserByRole("proCrePrincipal");//产创受理组
//			candidateIds = candidateUser.get("candidateIds");
//			candidateNames = candidateUser.get("candidateNames");
//		}
		
		System.out.println("RequirementService setCandidateUsers candidateNames:"+candidateNames);

		cusUserTask.setCandidate_ids(candidateIds);
		cusUserTask.setCandidate_name(candidateNames);
	}


	/**
	 * 设置PMO节点的候选人:角色为PMO的人
	 * 
	 * @param roleCode
	 * @return
	 */
//	public Map<String, String> getCandidateUserByRole(final String roleCode) {
		
//		List<MisRole> roleList = misRoleService.findRoleByCode(roleCode);
//		MisRole misRole = roleList == null || roleList.size() == 0 ? null : roleList.get(0);
//		Map<String, String> candidateUser = misRole == null ? null : userService.getUserFromUserRole(misRole.getId());
//		return candidateUser;
//	}


}
