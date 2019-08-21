package com.zr.workflow.activiti.entity;

public enum ProcessStatus {

    /** 待审批 */
    WAITING_FOR_APPROVAL("已重新申请"),
    /** 审批成功：通过 */
    APPROVAL_SUCCESS("同意"),
    /** 审批失败：驳回 */
    APPROVAL_FAILED("退回"),
    /** 委托 */
    TASK_PENDING("已委托"),
    /** 委托完成 */
    TASK_RESOLVED("委托人已完成任务"),
    /** 认领 */
    TASK_CLAIMED("已认领"),
    /** 归档 */
    ARCHIVE("已归档"),
    /** 结束 */
    CANCEL("已取消申请");

    private String content;
    ProcessStatus(String content){
        this.content = content;
    }

    public String getContent(){
        return content;
    }
}
