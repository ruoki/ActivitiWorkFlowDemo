package com.zr.workflow.activiti.entity;

import java.io.Serializable;

/**
 * 业务用户信息关联至工作流用户任务表
 * @author zhourq
 *
 */
public class CusUserTask implements Serializable {

	private static final long serialVersionUID = 8889804050417208965L;

	/**
	 * 当前Activiti节点类型：普通用户任务节点
	 */
	public static final String TYPE_NORMAL = "N";
	/**
	 * 当前Activiti节点类型：多实例任务节点
	 */
	public static final String TYPE_MULTI = "M";

	/**受理人(唯一)*/
	public static final String TYPE_ASSIGNEE = "assignee";
	/**候选人(多个)*/
	public static final String TYPE_CANDIDATEUSER = "candidateUser";
	/**候选组（多个）*/
	public static final String TYPE_CANDIDATEGROUP = "candidateGroup";

	private Integer id;

	private String procDefKey;//流程id

	private String procDefName;//流程名称

	private String taskDefKey;//节点id

	private String taskName;//节点名称

	private String activityType;//当前Activiti节点类型：N-普通用户任务节点;M-多实例任务节点

	private String taskType;		//1.assignee.受理人(唯一) 1.candidateUser候选人(多个) 2.candidateGroup候选组（多个）

	private String candidate_name; 	//受理人或候选人

	private String candidate_ids;

	private String group_id; 	//候选人组

	private String group_name;


	public String getProcDefKey() {
		return procDefKey;
	}
	public void setProcDefKey(String procDefKey) {
		this.procDefKey = procDefKey;
	}
	public String getProcDefName() {
		return procDefName == null || procDefName.equals("") ? "" : procDefName;
	}
	public void setProcDefName(String procDefName) {
		this.procDefName = procDefName;
	}
	public String getTaskDefKey() {
		return taskDefKey == null ? "" : taskDefKey;
	}
	public void setTaskDefKey(String taskDefKey) {
		this.taskDefKey = taskDefKey;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getTaskType() {
		return taskType == null ? "" : taskType;
	}
	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}
	public String getTaskName() {
		return taskName == null ? "" : taskName;
	}
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	public String getActivityType() {
		return activityType;
	}
	public void setActivityType(String activityType) {
		this.activityType = activityType;
	}
	public String getCandidate_name() {
		return candidate_name == null ? "" : candidate_name;
	}
	public void setCandidate_name(String candidate_name) {
		this.candidate_name = candidate_name;
	}
	public String getCandidate_ids() {
		return candidate_ids == null ? "" : candidate_ids;
	}
	public void setCandidate_ids(String candidate_ids) {
		this.candidate_ids = candidate_ids;
	}


	public String getGroup_id() {
		return group_id;
	}
	public void setGroup_id(String group_id) {
		this.group_id = group_id;
	}
	public String getGroup_name() {
		return group_name;
	}
	public void setGroup_name(String group_name) {
		this.group_name = group_name;
	}

	@Override
	public String toString() {
		return "CusUserTask [id=" + id + ", procDefKey=" + procDefKey + ", procDefName=" + procDefName + ", taskDefKey="
				+ taskDefKey + ", taskName=" + taskName + ", activityType=" + activityType + ", taskType=" + taskType
				+ ", candidate_name=" + candidate_name + ", candidate_ids=" + candidate_ids + ", group_id=" + group_id
				+ ", group_name=" + group_name + "]";
	}
}
