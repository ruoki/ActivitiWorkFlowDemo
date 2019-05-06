package com.zr.workflow.activiti.entity;

import java.io.Serializable;
import java.util.Date;

import com.zr.workflow.activiti.util.DateFormatUtil;

public class CommentVO implements Serializable {
	private static final long serialVersionUID = 3549695946267239515L;

	private String id;
	private String userId;
	// 评论人
	private String userName;

	// 评论内容
	private String content;

	private String taskId;
	private String processInstanceId;

	// 评论时间
	private String time;
	private String nextAssign;
	private String nextAssignName;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
	public String getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = DateFormatUtil.format(time);
	}

	public String getNextAssign() {
		return nextAssign;
	}
	public void setNextAssign(String nextAssign) {
		this.nextAssign = nextAssign;
	}
	public String getNextAssignName() {
		return nextAssignName;
	}
	public void setNextAssignName(String nextAssignName) {
		this.nextAssignName = nextAssignName;
	}

	@Override
	public String toString() {
		return "CommentVO [id=" + id + ", userId=" + userId + ", userName=" + userName + ", content=" + content
				+ ", taskId=" + taskId + ", processInstanceId=" + processInstanceId + ", time=" + time + ", nextAssign="
				+ nextAssign + ", nextAssignName=" + nextAssignName + "]";
	}

}
