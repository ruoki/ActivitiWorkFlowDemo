package com.zr.activiti.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

import com.alibaba.fastjson.JSONObject;
import com.zr.activiti.utils.DateFormatUtil;


public class BaseVO implements Serializable {

	private static final long serialVersionUID = 6165121688276341503L;

	// 业务类型
	public final static String REQUIREMENT = "requirement";// 需求确认
	
	/**
	 * 任务通过的流程描述 
	 */
	public final static String SUB_DESCRIPTION_PASS = "已被通过";

	/** 待审批 */
	public static final String WAITING_FOR_APPROVAL = "WAITING_FOR_APPROVAL";
	/** 审批成功：通过 */
	public static final String APPROVAL_SUCCESS = "APPROVAL_SUCCESS";
	/** 审批失败：驳回 */
	public static final String APPROVAL_FAILED = "APPROVAL_FAILED";
	/** 归档 */
	public static final String FILED = "FILED";
	/** 结束 */
	public static final String CANCEL = "END";
	protected int id;

	protected String createId;// 创建人
	protected String createName;
	protected String createTime;// 流程的创建时间
	protected String endTime;// 流程的结束时间
	protected String reason;
	protected String owner;// 拥有者
	// 申请的标题
	protected String title;
	// 业务类型
	protected String businessType;
	protected String deploymentId;// 流程部署id
	protected String processInstanceId;// 流程实例id
	protected String deleteReason;// 删除原因
	protected String handledTaskId;// 已处理任务id
	protected String handledTaskDefinitionKey;// 已处理节点
	protected String handledTaskName;// 已处理节点名称
	protected String assignedId;// 已处理节点的受理人
	protected String assignedName;// 已处理节点的受理人
	protected String toHandleTaskId;// 当前节点的任务id
	protected String taskDefinitionKey;// 当前节点key
	protected String toHandleTaskName;// 当前节点点名
	protected String assign;// 当前节点的受理人
	protected String assignName;// 当前节点的受理人
	protected String description;// 描述
	protected String businessKey;// 对应业务的id
	protected String startTime;// 任务的开始时间
	protected String operateTime;// 任务的结束时间(处理时间)
	protected String claimTime;// 任务的签收时间
	protected boolean isEnd;// 流程是否结束
	protected boolean isSuspended;// 是否挂起
	protected String processDefinitionId;// 流程定义id
	protected String processDefinitionName;// 流程名称
	protected String processDefinitionKey;// 流程key，任务跳转用
	protected String processStatus;// 流程状态：待审批、审批通过、审批退回、归档、结束
	protected int version;// 流程版本号
	protected JSONObject contentInfo;// 流程的业务相关
	private String candidate_ids;//下一节点执行人,多个用逗号隔开
	private String candidate_names;//下一节点执行人,多个用逗号隔开

	private List<CommentVO> comments;//评论列表
	
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

	public String getEndTime() {
		if (this.endTime == null) {
			Date endDate = (null == historicProcessInstance) ? null : historicProcessInstance.getEndTime();
			endTime = endDate == null ? "" : DateFormatUtil.format(endDate);
		}
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
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
		if (this.deploymentId == null) {
			this.deploymentId = (null == processInstance) ? "" : processInstance.getDeploymentId();
		}
		return deploymentId;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public String getProcessInstanceId() {
		if (this.processInstanceId == null || "".equals(this.processInstanceId)) {
			if (historicTaskInstance != null) {
				this.processInstanceId = historicTaskInstance.getProcessInstanceId();
			} else if (processInstance != null) {
				this.processInstanceId = processInstance.getId();
			} else {
				this.processInstanceId = (null == historicProcessInstance) ? "" : historicProcessInstance.getId();
			}
		}
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
		if (this.handledTaskId == null) {
			this.handledTaskId = (null == historicTaskInstance) ? "" : historicTaskInstance.getId();
		}
		return handledTaskId;
	}

	public void setHandledTaskId(String handledTaskId) {
		this.handledTaskId = handledTaskId;
	}

	public String getHandledTaskDefinitionKey() {
		if (this.handledTaskDefinitionKey == null) {
			this.handledTaskDefinitionKey = (null == historicTaskInstance) ? ""
					: historicTaskInstance.getTaskDefinitionKey();
		}
		return handledTaskDefinitionKey;
	}

	public void setHandledTaskDefinitionKey(String handledTaskDefinitionKey) {
		this.handledTaskDefinitionKey = handledTaskDefinitionKey;
	}

	public String getHandledTaskName() {
		if (this.handledTaskName == null) {
			this.handledTaskName = (null == historicTaskInstance) ? "" : historicTaskInstance.getName();
		}
		return handledTaskName;
	}

	public void setHandledTaskName(String handledTaskName) {
		this.handledTaskName = handledTaskName;
	}

	public String getAssignedId() {
		if (this.assignedId == null) {
			this.assignedId = (null == historicTaskInstance) ? "" : historicTaskInstance.getAssignee();
		}
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

	public String getAssign() {
		if (this.assign == null) {
			this.assign = (null == task) ? "" : task.getAssignee();
		}
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

	public String getStartTime() {
		if (this.startTime == null) {
			Date startDate = null;
			if (historicTaskInstance != null) {
				startDate = historicTaskInstance.getStartTime();
			} else {
				startDate = (null == historicProcessInstance) ? null : historicProcessInstance.getStartTime();
			}
			startTime = startDate == null ? "" : DateFormatUtil.format(startDate);
		}
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getOperateTime() {
		if (this.operateTime == null) {
			Date operateDate = (null == historicTaskInstance) ? null : historicTaskInstance.getEndTime();
			operateTime = operateDate == null ? "" : DateFormatUtil.format(operateDate);
		}
		return operateTime;
	}

	public void setOperateTime(String operateTime) {
		this.operateTime = endTime;
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
		return isEnd;
	}

	public void setEnd(boolean isEnd) {
		this.isEnd = isEnd;
	}

	public boolean isSuspended() {
		this.isSuspended = (null == processInstance) ? isSuspended : processInstance.isSuspended();
		return isSuspended;
	}

	public void setSuspended(boolean isSuspended) {
		this.isSuspended = isSuspended;
	}

	public String getProcessDefinitionId() {
		if (processDefinitionId == null) {
			if (processInstance != null) {
				this.processDefinitionId = processInstance.getProcessDefinitionId();
			} else {
				this.processDefinitionId = (null == historicProcessInstance) ? ""
						: historicProcessInstance.getProcessDefinitionId();

			}
		}
		return processDefinitionId;
	}

	public void setProcessDefinitionId(String processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
	}

	public String getProcessDefinitionName() {
		if (processDefinitionName == null) {
			this.processDefinitionName = (null == processDefinition) ? "" : processDefinition.getName();
		}
		return processDefinitionName;
	}

	public void setProcessDefinitionName(String processDefinitionName) {
		this.processDefinitionName = processDefinitionName;
	}

	public String getProcessDefinitionKey() {
		if (processDefinitionKey == null) {
			this.processDefinitionKey = (null == processDefinition) ? "" : processDefinition.getKey();
		}
		return processDefinitionKey;
	}

	public void setProcessDefinitionKey(String processDefinitionKey) {
		this.processDefinitionKey = processDefinitionKey;
	}

	public String getProcessStatus() {
		return processStatus;
	}

	public void setProcessStatus(String processStatus) {
		this.processStatus = processStatus;
	}

	public int getVersion() {
		if(version <= 0) {
			version = (null == processDefinition) ? 0 : processDefinition.getVersion();
		}
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

	// 流程任务
	public Task task;

	// 运行中的流程实例
	protected ProcessInstance processInstance;

	// 历史的流程实例
	protected HistoricProcessInstance historicProcessInstance;

	// 历史任务
	protected HistoricTaskInstance historicTaskInstance;

	// 流程定义
	protected ProcessDefinition processDefinition;


	public void setTask(Task task) {
		this.task = task;
	}

	public void setProcessInstance(ProcessInstance processInstance) {
		this.processInstance = processInstance;
	}

	public void setHistoricProcessInstance(HistoricProcessInstance historicProcessInstance) {
		this.historicProcessInstance = historicProcessInstance;
	}

	public void setProcessDefinition(ProcessDefinition processDefinition) {
		this.processDefinition = processDefinition;
	}

	public void setHistoricTaskInstance(HistoricTaskInstance historicTaskInstance) {
		this.historicTaskInstance = historicTaskInstance;
	}

	@Override
	public String toString() {
		return "BaseVO [id=" + id + ", createId=" + createId + ", createName=" + createName + ", createTime="
				+ createTime + ", endTime=" + endTime + ", reason=" + reason + ", owner=" + owner + ", title=" + title
				+ ", businessType=" + businessType + ", deploymentId=" + deploymentId + ", processInstanceId="
				+ processInstanceId + ", deleteReason=" + deleteReason + ", handledTaskId=" + handledTaskId
				+ ", handledTaskDefinitionKey=" + handledTaskDefinitionKey + ", handledTaskName=" + handledTaskName
				+ ", assignedId=" + assignedId + ", assignedName=" + assignedName + ", toHandleTaskId=" + toHandleTaskId
				+ ", taskDefinitionKey=" + taskDefinitionKey + ", toHandleTaskName=" + toHandleTaskName + ", assign="
				+ assign + ", assignName=" + assignName + ", description=" + description + ", businessKey="
				+ businessKey + ", startTime=" + startTime + ", operateTime=" + operateTime + ", claimTime=" + claimTime
				+ ", isEnd=" + isEnd + ", isSuspended=" + isSuspended + ", processDefinitionId=" + processDefinitionId
				+ ", processDefinitionName=" + processDefinitionName + ", processDefinitionKey=" + processDefinitionKey
				+ ", processStatus=" + processStatus + ", version=" + version + ", contentInfo=" + contentInfo
				+ ", candidate_ids=" + candidate_ids + ", candidate_names=" + candidate_names + ", comments=" + comments
				+ "]";
	}

}
