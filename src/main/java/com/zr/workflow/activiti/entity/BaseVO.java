package com.zr.workflow.activiti.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

import com.alibaba.fastjson.JSONObject;
import com.zr.workflow.activiti.util.DateFormatUtil;


public class BaseVO implements Serializable {

	private static final long serialVersionUID = 6165121688276341503L;

	/**
	 * 任务通过的流程描述 
	 */
	public final static String SUB_DESCRIPTION_PASS = "已被通过";
	private int id;

	private String createId;// 创建人
	private String createName;// 创建人
	private String createTime;// 流程的创建时间
	private String processEndTime;// 流程的结束时间
	private String reason;// 申请理由
	private String owner;// 拥有者
	private String title;// 申请的标题
	private String businessType;// 业务类型
	private String deploymentId;// 流程部署id
	private String processInstanceId;// 流程实例id
	private String deleteReason;// 删除原因
	private String handledTaskId;// 已处理任务id
	private String handledTaskDefinitionKey;// 已处理节点
	private String handledTaskName;// 已处理节点名称
	private String handledActivitiType;// 已处理节点类型：M:多实例;N:普通节点
	private String assignedId;// 已处理节点的受理人
	private String assignedName;// 已处理节点的受理人
	private String toHandleTaskId;// 当前节点的任务id
	private String taskDefinitionKey;// 当前节点key
	private String toHandleTaskName;// 当前节点名
	private String toHandleActivitiType;// 当前节点类型：M:多实例;N:普通节点
	private String assign;// 当前节点的受理人
	private String assignName;// 当前节点的受理人
	private String description;// 描述
	private String businessKey;// 对应业务的id
	private String taskStartTime;// 任务的开始时间
	private String operateTime;// 任务的结束时间(处理时间)
	private String claimTime;// 任务的签收时间
	private boolean end;// 流程是否结束
	private boolean suspended;// 是否挂起
	private String processDefinitionId;// 流程定义id
	private String processDefinitionName;// 流程名称
	private String processDefinitionKey;// 流程key，任务跳转用
	private String processStatus;// 流程状态：待审批、审批通过、审批退回、归档、结束
	private String delegationState;//流程委托状态
	private int version;// 流程版本号
	private JSONObject contentInfo;// 流程的业务相关
	private String candidate_ids;//下一节点执行人,多个用逗号隔开
	private String candidate_names;//下一节点执行人,多个用逗号隔开

	private List<CommentVO> comments;//评论列表
	private int total;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCreateId() {
		return createId;
	}

	public void setCreateId(String createId) {
		this.createId = createId;
	}

	public String getCreateName() {
		return createName;
	}

	public void setCreateName(String createName) {
		this.createName = createName;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getOwner() {
		if (this.owner == null) {
			this.owner = (null == task) ? "" : task.getOwner();
		}
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public String getProcessEndTime() {
		if (this.processEndTime == null) {
			Date endDate = (null == historicProcessInstance) ? null : historicProcessInstance.getEndTime();
			processEndTime = endDate == null ? "" : DateFormatUtil.format(endDate);
		}
		return processEndTime;
	}

	public void setProcessEndTime(String processEndTime) {
		this.processEndTime = processEndTime;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBusinessType() {
		return businessType;
	}

	public void setBusinessType(String businessType) {
		this.businessType = businessType;
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public String getDeleteReason() {
		if (this.deleteReason == null) {
			this.deleteReason = (null == historicTaskInstance) ? "" : historicTaskInstance.getDeleteReason();
		}
		return deleteReason;
	}

	public void setDeleteReason(String deleteReason) {
		this.deleteReason = deleteReason;
	}

	public String getHandledTaskId() {
		return handledTaskId;
	}

	public void setHandledTaskId(String handledTaskId) {
		this.handledTaskId = handledTaskId;
	}

	public String getHandledTaskDefinitionKey() {
		return handledTaskDefinitionKey;
	}

	public void setHandledTaskDefinitionKey(String handledTaskDefinitionKey) {
		this.handledTaskDefinitionKey = handledTaskDefinitionKey;
	}

	public String getHandledTaskName() {
		return handledTaskName;
	}

	public void setHandledTaskName(String handledTaskName) {
		this.handledTaskName = handledTaskName;
	}


	public String getHandledActivitiType() {
		return handledActivitiType;
	}

	public void setHandledActivitiType(String handledActivitiType) {
		this.handledActivitiType = handledActivitiType;
	}

	public String getAssignedId() {
		return assignedId;
	}

	public void setAssignedId(String assignedId) {
		this.assignedId = assignedId;
	}

	public String getAssignedName() {
		return assignedName;
	}

	public void setAssignedName(String assignedName) {
		this.assignedName = assignedName;
	}

	public String getToHandleTaskId() {
		if (this.toHandleTaskId == null) {
			this.toHandleTaskId = (null == task) ? "" : task.getId();
		}
		return toHandleTaskId;
	}

	public void setToHandleTaskId(String toHandleTaskId) {
		this.toHandleTaskId = toHandleTaskId;
	}

	public String getTaskDefinitionKey() {
		if (this.taskDefinitionKey == null) {
			this.taskDefinitionKey = (null == task) ? "" : task.getTaskDefinitionKey();
		}
		return taskDefinitionKey;
	}

	public void setTaskDefinitionKey(String taskDefinitionKey) {
		this.taskDefinitionKey = taskDefinitionKey;
	}

	public String getToHandleTaskName() {
		if (this.toHandleTaskName == null) {
			this.toHandleTaskName = (null == task) ? "" : task.getName();
		}
		return toHandleTaskName;
	}

	public void setToHandleTaskName(String toHandleTaskName) {
		this.toHandleTaskName = toHandleTaskName;
	}

	public String getToHandleActivitiType() {
		return toHandleActivitiType;
	}

	public void setToHandleActivitiType(String toHandleActivitiType) {
		this.toHandleActivitiType = toHandleActivitiType;
	}

	public String getAssign() {
		return assign;
	}

	public void setAssign(String assign) {
		this.assign = assign;
	}

	public String getAssignName() {
		return assignName;
	}

	public void setAssignName(String assignName) {
		this.assignName = assignName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public void setBusinessKey(String businessKey) {
		this.businessKey = businessKey;
	}

	public String getTaskStartTime() {
		return taskStartTime;
	}

	public void setTaskStartTime(Date startDate) {
		this.taskStartTime = startDate == null ? "" : DateFormatUtil.format(startDate);
	}

	public String getOperateTime() {
		if (this.operateTime == null) {
			Date operateDate = (null == historicTaskInstance) ? null : historicTaskInstance.getEndTime();
			if(null == operateDate) {
				operateDate = (null == historicProcessInstance) ? null : historicProcessInstance.getEndTime();
			}
			operateTime = operateDate == null ? "" : DateFormatUtil.format(operateDate);
		}
		return operateTime;
	}

	public void setOperateTime(String operateTime) {
		this.operateTime = operateTime;
	}

	public String getClaimTime() {
		if (this.claimTime == null) {
			Date claimDate = (null == historicTaskInstance) ? null : historicTaskInstance.getClaimTime();
			claimTime = claimDate == null ? "" : DateFormatUtil.format(claimDate);
		}
		return claimTime;
	}

	public void setClaimTime(String claimTime) {
		this.claimTime = claimTime;
	}

	public boolean isEnd() {
		return end;
	}

	public void setEnd(boolean end) {
		this.end = end;
	}

	public boolean isSuspended() {
		this.suspended = (null == processInstance) ? suspended : processInstance.isSuspended();
		return suspended;
	}

	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}

	public String getProcessDefinitionId() {
		return processDefinitionId;
	}

	public void setProcessDefinitionId(String processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
	}

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

	public void setProcessDefinitionName(String processDefinitionName) {
		this.processDefinitionName = processDefinitionName;
	}

	public String getProcessDefinitionKey() {
		return processDefinitionKey;
	}

	public void setProcessDefinitionKey(String processDefinitionKey) {
		this.processDefinitionKey = processDefinitionKey;
	}

	public String getProcessStatus() {
		return processStatus;
	}

	public void setProcessStatus(ProcessStatus processStatus) {
		this.processStatus = null != processStatus ? processStatus.toString() : "";
	}


	public String getDelegationState() {
		return delegationState;
	}

	public void setDelegationState(String delegationState) {
		this.delegationState = delegationState;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public JSONObject getContentInfo() {
		return contentInfo;
	}

	public void setContentInfo(JSONObject contentInfo) {
		this.contentInfo = contentInfo;
	}

	public String getCandidate_ids() {
		return null == candidate_ids ? null : candidate_ids.replaceAll("\\[", "").replaceAll("\"", "").replaceAll("\\]", "");
	}

	public void setCandidate_ids(String candidate_ids) {
		this.candidate_ids = candidate_ids;
	}

	public String getCandidate_names() {
		return null == candidate_names ? null : candidate_names.replaceAll("\\[", "").replaceAll("\"", "").replaceAll("\\]", "");
	}

	public void setCandidate_names(String candidate_names) {
		this.candidate_names = candidate_names;
	}

	public void setComments(List<CommentVO> commentList) {
		this.comments = commentList;
	}

	public List<CommentVO> getComments(){
		return comments;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getTotal() {
		return total;
	}
	// 流程任务
	private Task task;

	// 运行中的流程实例
	private ProcessInstance processInstance;

	// 历史的流程实例
	private HistoricProcessInstance historicProcessInstance;

	// 历史任务
	private HistoricTaskInstance historicTaskInstance;


	public void setTask(Task task) {
		this.task = task;
	}

	public void setProcessInstance(ProcessInstance processInstance) {
		this.processInstance = processInstance;
	}

	public void setHistoricProcessInstance(HistoricProcessInstance historicProcessInstance) {
		this.historicProcessInstance = historicProcessInstance;
	}

	public void setHistoricTaskInstance(HistoricTaskInstance historicTaskInstance) {
		this.historicTaskInstance = historicTaskInstance;
	}

	@Override
	public String toString() {
		return "BaseVO [id=" + id + ", createId=" + createId + ", createName=" + createName + ", createTime="
				+ createTime + ", processEndTime=" + processEndTime + ", reason=" + reason + ", owner=" + owner + ", title=" + title
				+ ", businessType=" + businessType + ", deploymentId=" + deploymentId + ", processInstanceId="
				+ processInstanceId + ", deleteReason=" + deleteReason + ", handledTaskId=" + handledTaskId
				+ ", handledTaskDefinitionKey=" + handledTaskDefinitionKey + ", handledTaskName=" + handledTaskName
				+ ", assignedId=" + assignedId + ", assignedName=" + assignedName + ", toHandleTaskId=" + toHandleTaskId
				+ ", taskDefinitionKey=" + taskDefinitionKey + ", toHandleTaskName=" + toHandleTaskName + ", assign="
				+ assign + ", assignName=" + assignName + ", description=" + description + ", businessKey="
				+ businessKey + ", taskStartTime=" + taskStartTime + ", operateTime=" + operateTime + ", claimTime=" + claimTime
				+ ", end=" + end + ", suspended=" + suspended + ", processDefinitionId=" + processDefinitionId
				+ ", processDefinitionName=" + processDefinitionName + ", processDefinitionKey=" + processDefinitionKey
				+ ", processStatus=" + processStatus + ", version=" + version + ", contentInfo=" + contentInfo
				+ ", candidate_ids=" + candidate_ids + ", candidate_names=" + candidate_names + ", comments=" + comments
				+ "]";
	}

}
