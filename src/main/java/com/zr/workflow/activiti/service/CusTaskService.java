package com.zr.workflow.activiti.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.DelegationState;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.zr.workflow.activiti.controller.ProcessController;
import com.zr.workflow.activiti.entity.BaseVO;
import com.zr.workflow.activiti.entity.CommentVO;
import com.zr.workflow.activiti.entity.CusUserTask;
import com.zr.workflow.activiti.entity.Page;
import com.zr.workflow.activiti.entity.ProcessStatus;
import com.zr.workflow.activiti.util.ApplicationContextHandler;
import com.zr.workflow.activiti.util.DeleteHistoricTaskInstanceCmdImpl;
import com.zr.workflow.activiti.util.ProcessDefinitionCache;
import com.zr.workflow.activiti.util.StringUtil;

/**
 * 任务节点处理service
 * @author zhourq
 *
 */
@Service
public class CusTaskService {
	@Resource
	private RepositoryService repositoryService;
	@Resource
	private RuntimeService runtimeService;
	@Resource
	private HistoryService historyService;
	@Resource
	private ManagementService managementService;
	@Resource
	private TaskService taskService;
	@Resource
	private CusUserTaskService userTaskService;

    @Resource
    private ProcessService processService;
    @Resource
    private ProcessController processControllder;

    /**
     * 查询待办任务
     *
     * @param userId
     * @param userName
     * @param page
     * @param processDefKeys
     * @return
     */
    public List<BaseVO> findTodoTask(String userId, String userName, Page<BaseVO> page, List<String> processDefKeys) {

        Map<String, Object> processInfoMap = getRunTasksFromDataBase(userId,page,processDefKeys);
        List<Task> tasks = (List<Task>) processInfoMap.get("processList");
        int listSize = (int) processInfoMap.get("size");

        List<BaseVO> taskList = new ArrayList<>();
        for (Task task : tasks) {
            String processInstanceId = task.getProcessInstanceId();
            ProcessInstance processInstance = this.runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            boolean isSuspended = processInstance.isSuspended();
            if(isSuspended)continue;//获取激活状态下的流程实例

            BaseVO base = processService.getBaseVOFromRu_Variable(processInstanceId);
            if (null == base) continue;
            setBaseVO(base, userName,listSize, task,null,processInstance, null);
            taskList.add(base);
        }
        return taskList;
    }

    /**
     * 查询运行时任务列表
     * @param userId 用户id
     * @param page
     * @param processDefKeys
     * @return
     */
    private Map<String, Object> getRunTasksFromDataBase(final String userId, Page<BaseVO> page,List<String> processDefKeys) {

        TaskQuery taskQuery;
        if(null == processDefKeys || processDefKeys.size() == 0) {
            taskQuery = this.taskService.createTaskQuery().taskCandidateOrAssigned(userId).orderByTaskCreateTime().desc();
        }else {
            taskQuery = this.taskService.createTaskQuery().taskCandidateOrAssigned(userId).processDefinitionKeyIn(processDefKeys).orderByTaskCreateTime().desc();
        }
        List<Task> tasks;
        int listSize = (int) taskQuery.count();
        if(null != page && listSize > 0) {//分页
            int[] indexs = this.processService.getIndex(page,listSize);
            tasks = taskQuery.listPage(indexs[0], indexs[1]);
        }else{
            tasks = taskQuery.list();
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("size",listSize);
        resultMap.put("processList",tasks);
        return resultMap;
    }

    /**
     * 办理完第1个任务“提交申请”
     *
     * @param instanceId
     * @param isPass
     * @param baseVO
     * @param variables
     * @return
     * @throws Exception
     */
    public List<String> excuteFirstTask(String instanceId, String isPass,BaseVO baseVO, Map<String, Object> variables){
        Task task = getTaskByProcessInstanceId(instanceId).get(0);
        baseVO.setTaskDefinitionKey(task.getTaskDefinitionKey());

        ProcessStatus handleFlag = checkIsPassFirstTask(isPass, task.getTaskDefinitionKey(), variables);
        baseVO.setProcessStatus(handleFlag);

        List<CommentVO> commentList = getComments(baseVO.getProcessInstanceId());

        Map<String, String> users = userTaskService.getNextUserTaskCandidateUsers(baseVO, "");
        if(null != users) {
            for (CommentVO commentVO : commentList) {
                commentVO.setNextAssign(users.get("candidate_ids"));
                commentVO.setNextAssignName(users.get("candidate_name"));
            }
            //		}else {
            //			variables.put("autoComplete", true);//如果下一节点执行人为空，则直接通过
        }
        baseVO.setComments(commentList);
        List<String> userList = handleTask(task.getId(),baseVO.getCreateId(),baseVO.getCreateName(),handleFlag, "发起申请", baseVO, variables,false);
        return userList;
    }



    /**
     * 第一个任务是否审核通过
     *
     * @param isPassStr
     * @param taskDefinitionKey
     * @param variables
     */
    private ProcessStatus checkIsPassFirstTask(String isPassStr, String taskDefinitionKey,
                                               Map<String, Object> variables) {
        if (StringUtil.isEmpty(isPassStr)) return null;
        final String variableKey = new StringBuilder().append("isPass_").append(taskDefinitionKey).toString();
        variables.put(variableKey, isPassStr);
        if ("true".equals(isPassStr)) {
            return ProcessStatus.APPROVAL_SUCCESS;
        } else {
            return ProcessStatus.APPROVAL_FAILED;
        }
    }

    /**
     * 处理任务
     * @return
     */
    public List<String> handleTask(String taskId,String userId,String userName,ProcessStatus handleFlag, String content, BaseVO baseVO,
                                   Map<String, Object> variables, boolean isDelegateAutoHandle) {
        Task task = getTaskByTaskId(taskId);

        //多实例节点未全部通过时不保存上一个节点信息
        boolean notSetPreNodeInfo = (handleFlag != null && ProcessStatus.APPROVAL_SUCCESS.equals(handleFlag.toString()))
                &&(!baseVO.getDescription().contains("已同意 ") && !baseVO.getDescription().contains(BaseVO.SUB_DESCRIPTION_PASS));
        if(!notSetPreNodeInfo) {
            baseVO.setHandledTaskId(taskId);
            baseVO.setAssignedId(userId);
            baseVO.setAssignedName(userName);
            baseVO.setHandledTaskDefinitionKey(task.getTaskDefinitionKey());
            baseVO.setHandledTaskName(task.getName());
            final String handledActivitiType = getActivitiType(baseVO.getBusinessKey(), task.getTaskDefinitionKey());
            baseVO.setHandledActivitiType(handledActivitiType);
            if (DelegationState.PENDING == task.getDelegationState() && !isDelegateAutoHandle) {
                baseVO.setDescription(userName+"已完成委托的任务");
            }
        }
        variables.put("entity", baseVO);

        String processInstanceId = task.getProcessInstanceId();
        if (DelegationState.PENDING == task.getDelegationState()) {
            Authentication.setAuthenticatedUserId(task.getAssignee());
            handleFlag = ProcessStatus.TASK_RESOLVED;
            addComment(taskId,processInstanceId, handleFlag, content);
            resolveTask(taskId,processInstanceId, variables);
            if(isDelegateAutoHandle) {
                completeTask(taskId, variables);
                checkAutoCompleteTask(taskId, task, variables);
            }
        }else {
            // 设置流程的start_userId和评论人的id
            Authentication.setAuthenticatedUserId(userId);
            addComment(taskId,processInstanceId, handleFlag, content);
            completeTask(taskId, variables);
            checkAutoCompleteTask(taskId, task, variables);
        }
        List<String> userList = getNextNodeAssigneInfos(baseVO.getProcessInstanceId());
        return userList;
    }


    /**
     * 根据流程实例查询待办任务,可能是多实例的会签点，可能有多个任务
     * @param instanceId
     * @return
     */
    public List<Task> getTaskByProcessInstanceId(String instanceId) {
        List<Task> taskList = taskService.createTaskQuery().processInstanceId(instanceId).list();
        return taskList;
    }

    /**
     * 根据任务id获取task
     *
     * @param taskId
     * @return
     */
    public Task getTaskByTaskId(String taskId) {
        Task task = this.taskService.createTaskQuery().taskId(taskId).singleResult();
        return task;
    }

    /**
     * 获取下一节点的执行人
     * @param processInstanceId
     * @return
     */
    private List<String> getNextNodeAssigneInfos(String processInstanceId) {
        List<String> userList = new ArrayList<>();
        List<Task> toDotaskList = getTaskByProcessInstanceId(processInstanceId);// 获取该流程的待办任务,可能是多实例的会签点，可能有多个执行人多个任务
        if (null != toDotaskList && toDotaskList.size() > 0) {
            final String userIdsStr = getCandidateIdsOfTask(toDotaskList);
            String[] userIds = new String[1];
            if (userIdsStr.contains(",")) {
                userIds = userIdsStr.split(",");
            }else {
                userIds[0] = userIdsStr;
            }
            for (int i = 0; i < userIds.length; i++) {
                userList.add(userIds[i]);
            }
        }
        return userList;
    }

    /**
     * 执行任务
     * taskService.complete(taskId, variables)方法是将variables存到execution中
     * @param taskId
     *            任务id
     * @param variables
     *            流程遍量
     */
    private void completeTask(String taskId, Map<String, Object> variables) {
        this.taskService.complete(taskId, variables);
    }

    /**
     * 自动执行(通过)
     *
     * @param preTaskId 任务id
     * @param preTask 任务
     * @param variables 流程变量
     */
    private void checkAutoCompleteTask(String preTaskId, Task preTask, Map<String, Object> variables) {
        try {
            final String processInstanceId = preTask.getProcessInstanceId();
            Object autoCompleteObj = processService.getRunVariable("autoComplete", processInstanceId);
            boolean autoComplete = autoCompleteObj == null ? false : (boolean) autoCompleteObj;

            List<Task> tasks = taskService.createTaskQuery()// 查询出本流程实例中当前仅有的一个任务
                    .processInstanceId(processInstanceId).list();
            if(tasks.size() > 1 )autoComplete = false;
            Task task = tasks.get(0);
            if (autoComplete) {
                String nextTaskId = task.getId();
                if (!preTaskId.equals(nextTaskId)) {
                    boolean isArchive = ProcessDefinitionCache.ARCHIVE.equals(task.getTaskDefinitionKey());
                    if(isArchive) {
                        variables.put("autoComplete", false);
                    }

                    completeTask(nextTaskId, variables);
                    //检测是否需要自动执行归档流程
                    Task archiveTask = getArchiveNode(processInstanceId);
                    boolean isArchiveNode = archiveTask != null;
                    if(isArchiveNode) {
                        checkAutoCompleteTask(nextTaskId, archiveTask, variables);
                    }
                }
            }
        } catch (Exception e) {// 有可能是最后一个节点（取消申请），无法继续自动执行下一个节点
            e.printStackTrace();
        }
    }

    /**
     * 归档节点
     * @param processInstanceId
     * @return
     */
    public Task getArchiveNode(String processInstanceId) {

        List<Task> tasks = taskService.createTaskQuery()// 查询出本流程实例中当前仅有的一个任务
                .processInstanceId(processInstanceId).list();
        if(tasks.size() > 1 || tasks.size() < 1) {
            return null;
        }
        Task task = tasks.get(0);
        boolean isArchiveNode = ProcessDefinitionCache.ARCHIVE.equals(task.getTaskDefinitionKey());
        if(isArchiveNode) {
            return task;
        }
        return null;
    }

    /**
     * 设置本地变量：设置实体与任务绑定
     *
     * @param taskId
     * @param variables
     */

    public void setLocalVariable(String taskId, Map<String, Object> variables) {
        if (variables == null) return;
        for (String variableName : variables.keySet()) {
            if ("entity".equals(variableName)) {
                this.taskService.setVariableLocal(taskId, variableName, variables.get(variableName));
            }
        }
    }

    /**
     * 设置流程变量
     *
     * @param taskId
     * @param variables
     */

    public void setVariable(String taskId, Map<String, Object> variables) {
        if (variables == null) return;
        for (String variableName : variables.keySet()) {
            if ("entity".equals(variableName)) {
                this.taskService.setVariable(taskId, variableName, variables.get(variableName));
            }
        }
    }

    /**
     * 添加评论
     *
     * @param taskId
     * @param processInstanceId
     * @param handleFlag
     * @param content
     */
    public void addComment(String taskId,String processInstanceId, ProcessStatus handleFlag, String content) {
        if (StringUtil.isEmpty(content)) {
            content = getComment(handleFlag);
        }
        this.taskService.addComment(taskId, processInstanceId, content);
    }



    /**
     * 删除评论
     * @param commentId
     */
    public void deleteComment(String commentId){
        this.taskService.deleteComment(commentId);
    }

    /**
     * 删除该流程的所有评论
     * @param taskId
     * @param processInstanceId
     */
    public void deleteComments(String taskId,String processInstanceId){
        this.taskService.deleteComments(taskId, processInstanceId);
    }

    /**
     * 获取默认意见
     * @param status
     * @return
     */
    private String getComment(ProcessStatus status){
        if (status == null) {
            status = ProcessStatus.APPROVAL_SUCCESS;
        }
        return status.getContent();
    }

    /**
     * 已完成的任务
     *
     * @param userId
     * @param userName
     * @param dataType 数据类型:默认获取所有的已办事宜，"lastet":获取最新的已办事宜
     * @param processDefKeys
     * @return
     * @throws Exception
     */
    public List<BaseVO> findDoneTask(String userId, String userName, Page<BaseVO> page, String dataType, List<String> processDefKeys){
        List<BaseVO> doneTaskList = new ArrayList<>();

        Map<String, Object> processInfoMap = getHistoryTasksFromDataBase(userId,page,dataType,processDefKeys);
        List<HistoricTaskInstance> hTaskInstanceList = (List<HistoricTaskInstance>) processInfoMap.get("processList");
        int listSize = (int) processInfoMap.get("size");

        for (HistoricTaskInstance historicTaskInstance : hTaskInstanceList) {
            String processInstanceId = historicTaskInstance.getProcessInstanceId();
            HistoricProcessInstance historicProcessInstance = processService.getHisProcessInstanceByInstanceId(processInstanceId);

            BaseVO base;
            if (null != historicProcessInstance.getEndTime()) {
                base = processService.getBaseVOFromHistoryVariable(processInstanceId);
                base.setEnd(true);
                base.setDeleteReason(historicProcessInstance.getDeleteReason());
            }else {
                base = processService.getBaseVOFromRu_Variable(processInstanceId);
            }
            if (null == base) continue;
            setBaseVO(base, userName,listSize, null,historicTaskInstance,null, historicProcessInstance);
            doneTaskList.add(base);
        }
        return doneTaskList;
    }

    /**
     * 查询历史任务列表
     * @param userId 用户id
     * @param page
     * @param dataType 数据类型:默认获取所有的已办事宜，"lastet":每个流程实例只获取最新的已办事宜
     * @param processDefKeys
     * @return
     */
    private Map<String, Object> getHistoryTasksFromDataBase(final String userId, Page<BaseVO> page,String dataType, List<String> processDefKeys) {
        HistoricTaskInstanceQuery hTaskInstanceQuery = getAllHisTaskListByInvolvedUser(userId,processDefKeys);
        List<HistoricTaskInstance> hTaskInstanceList;
        int listSize;
        if(StringUtil.isNotEmpty(dataType)) {
            hTaskInstanceList = hTaskInstanceQuery.list();
            hTaskInstanceList = getLastetHisTaskListByInvolvedUser(hTaskInstanceList);

            //过滤之后进行分页，否则总记录数不正确
            listSize = hTaskInstanceList.size();
            if(null != page && listSize > 0) {//分页
                int[] indexs = this.processService.getIndex(page,listSize);
                hTaskInstanceList = hTaskInstanceList.subList(indexs[0], indexs[1]);
            }
        }else{
            listSize = (int) hTaskInstanceQuery.count();
            if(null != page && listSize > 0) {//分页
                int[] indexs = this.processService.getIndex(page,listSize);
                hTaskInstanceList = hTaskInstanceQuery.listPage(indexs[0], indexs[1]);
            }else{
                hTaskInstanceList = hTaskInstanceQuery.list();
            }
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("size",listSize);
        resultMap.put("processList",hTaskInstanceList);
        return resultMap;
    }

    /**
     * 获取参与者的所有已办事宜
     * @param userId
     * @param processDefKeys
     * @return
     */
    private HistoricTaskInstanceQuery getAllHisTaskListByInvolvedUser(final String userId, List<String> processDefKeys) {
        HistoricTaskInstanceQuery hTaskInstanceQuery;
        if(null == processDefKeys || processDefKeys.size() == 0) {
            hTaskInstanceQuery = historyService.createHistoricTaskInstanceQuery().taskInvolvedUser(userId)
                    /*.finished()*/.orderByTaskCreateTime().desc();
        }else {
            hTaskInstanceQuery = historyService.createHistoricTaskInstanceQuery().taskInvolvedUser(userId)
                    /*.finished()*/.processDefinitionKeyIn(processDefKeys).orderByTaskCreateTime().desc();
        }
        return hTaskInstanceQuery;
    }

    /**
     * 获取参与者每条流程实例的最新已办事宜，根据流程实例id进行过滤
     * @param hTaskAssigneeList
     * @return
     */
    private List<HistoricTaskInstance> getLastetHisTaskListByInvolvedUser(List<HistoricTaskInstance> hTaskAssigneeList) {
        hTaskAssigneeList = distinct(hTaskAssigneeList);
        return hTaskAssigneeList;
    }

    /**
     * 根据流程实例id去除重复数据
     * @param list
     * @return
     */
    public List<HistoricTaskInstance> distinct(List<HistoricTaskInstance> list){
        list = distinctByFilter(list);
//		list = distinctByCollect(list);
//		list = distinctByRemove(list);
        return list;
    }

    private List<HistoricTaskInstance> distinctByFilter(List<HistoricTaskInstance> list) {
        list = list.stream().filter(distinctBy(HistoricTaskInstance::getProcessInstanceId))
                .filter(taskInstance ->null != taskInstance.getEndTime())
                .collect(Collectors.toList());
        return list;
    }

    public static <T> Predicate<T> distinctBy(Function<? super T,?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private List<HistoricTaskInstance> distinctByCollect(List<HistoricTaskInstance> list) {
        list = list.stream().collect(
                Collectors.collectingAndThen(
                        Collectors.toCollection(
                                ()->new TreeSet<>(
                                        Comparator.comparing(HistoricTaskInstance::getProcessInstanceId)
                                )
                        ), ArrayList::new));
        return list;
    }

    private List<HistoricTaskInstance> distinctByRemove(List<HistoricTaskInstance> list) {
        for (int i = 0 ; i < list.size() - 1; i++){
            for (int j = list.size() - 1 ; j>i; j--){
                if (list.get(j).getProcessInstanceId().equals(list.get(i).getProcessInstanceId())){
                    list.remove(j);
                }
            }
        }
        return list;
    }

    /**
     * 填充实体类
     * @param base
     * @param userName
     * @param task
     * @param historicTaskInstance
     * @param processInstance
     * @param historicProcessInstance
     */
    public void setBaseVO(BaseVO base, String userName,int listSize, Task task, HistoricTaskInstance historicTaskInstance,
                          ProcessInstance processInstance, HistoricProcessInstance historicProcessInstance) {

        String proDefId = "";
        String processInstanceId = "";
        Date taskStartTime = null;
        String owner = "";
        String handledTaskDefinitionKey = "";
        String handledTaskId = "";
        String handledTaskName = "";
        if(null != task) {
            proDefId = task.getProcessDefinitionId();
            processInstanceId = task.getProcessInstanceId();
            taskStartTime = task.getCreateTime();
            owner = task.getOwner();
        }else if(null != historicTaskInstance) {
            proDefId = historicTaskInstance.getProcessDefinitionId();
            processInstanceId = historicTaskInstance.getProcessInstanceId();
            taskStartTime = historicTaskInstance.getCreateTime();
            owner = historicTaskInstance.getOwner();
            handledTaskDefinitionKey = historicTaskInstance.getTaskDefinitionKey();
            handledTaskId = historicTaskInstance.getId();
            handledTaskName = historicTaskInstance.getName();

            final String handledActivitiType = getActivitiType(base.getBusinessKey(), handledTaskDefinitionKey);
            base.setHandledTaskDefinitionKey(handledTaskDefinitionKey);
            base.setHandledTaskId(handledTaskId);
            base.setHandledTaskName(handledTaskName);
            base.setHandledActivitiType(handledActivitiType);

        }else if(null != processInstance) {
            proDefId = processInstance.getProcessDefinitionId();
            processInstanceId = processInstance.getId();
        }else if(null != historicProcessInstance) {
            proDefId = historicProcessInstance.getProcessDefinitionId();
            processInstanceId = historicProcessInstance.getId();
        }
        String userIds = "";
        List<Task> toDotaskList = getTaskByProcessInstanceId(processInstanceId);// 获取该流程的待办任务,可能是多实例的会签点，可能有多个执行人多个任务
        if (null != toDotaskList && toDotaskList.size() > 0) {// 指定为空的情况下，表明该节点为会签节点，直接显示其候选组名
            if(null == task) {
                task = toDotaskList.get(0);
            }
            userIds = getCandidateIdsOfTask(toDotaskList);
        }

        base.setAssign(userIds);//未操作者
        base.setProcessInstanceId(processInstanceId);
        base.setHistoricTaskInstance(historicTaskInstance);
        base.setHistoricProcessInstance(historicProcessInstance);
        base.setProcessInstance(processInstance);
        base.setOwner(owner);
        if(null != task) {//下一节点的相关信息
            final String toHandleActivitiType = getActivitiType(base.getBusinessKey(), task.getTaskDefinitionKey());
            base.setToHandleActivitiType(toHandleActivitiType);
            if(null == taskStartTime)taskStartTime = task.getCreateTime();
            base.setTask(task);
            base.setTaskDefinitionKey(task.getTaskDefinitionKey());
            base.setToHandleTaskName(task.getName());
            base.setToHandleTaskId(task.getId());
            base.setSuspended(task.isSuspended());
            final DelegationState processStatus = task.getDelegationState();
            base.setDelegationState(null == processStatus ? "" : processStatus.toString());
        }
        base.setTaskStartTime(taskStartTime);
        base.setTotal(listSize);
        setUserNameAndComments(base,userName);
        ProcessDefinition process = processService.findProcessDefinitionById(proDefId);
        if(null != process) {
            base.setProcessDefinitionKey(process.getKey());
            base.setProcessDefinitionId(process.getId());
            base.setProcessDefinitionName(process.getName());
            base.setVersion(process.getVersion());
            base.setDeploymentId(process.getDeploymentId());
        }
    }


    /**
     * 补充BaseVO数据，评论列表中执行人名称为空，需要自行实现
     *
     * @param base
     * @param userName
     */
    public void setUserNameAndComments(BaseVO base, String userName){
        String assignedId = base.getAssignedId();
        String nextAssign = base.getAssign();
        String assignedName = base.getAssignedName();
        String nextAssignName = base.getAssignName();

        if (StringUtil.isEmpty(assignedName) && !StringUtil.isEmpty(assignedId)) {
            assignedName = getUserNamesFromLocal(assignedId);
            base.setAssignedName(assignedName);
        }

        if (StringUtil.isEmpty(nextAssignName) && !StringUtil.isEmpty(nextAssign)) {
            nextAssignName = getUserNamesFromLocal(nextAssign);
            if(StringUtil.isEmpty(nextAssignName)) {
                base.setAssignName(base.getCandidate_names());
            }else {
                base.setAssignName(nextAssignName);
            }
        }

        String description = base.getDescription();
        if(base.isEnd()) {
            final String workFloWTitle = StringUtil.isEmpty(base.getTitle()) ? "请求" : base.getTitle();
            description = base.getCreateName()+" 的"+workFloWTitle+ BaseVO.SUB_DESCRIPTION_PASS;
        }
        if (StringUtil.isNotEmpty(userName) && description.contains(userName)) {
            description = description.replaceAll(userName, "您");
        }
        //加上这段代码时，前端接收时"orginManagerId":null 会变成："orginManagerId":"\"null\"",再次向后台传入时json解析时报语法错误（关联需求流程点击同意时）
//        final String contentInfo = null == base.getContentInfo()?"":base.getContentInfo().toString();
//        if(StringUtil.isNotEmpty(contentInfo)) {
//            base.setContentInfo(JsonLibUtil.get().parseObject(contentInfo));
//        }
        base.setDescription(description);
        List<CommentVO> commentList = getCommentList(base);
        base.setComments(commentList);

    }

    public List<CommentVO> getCommentList(BaseVO base) {
        List<CommentVO> commentList = new ArrayList<>();
        try {
            commentList = this.getComments(base.getProcessInstanceId());
            List<CommentVO> historyCommentList = base.getComments();

            int curCommentLength = commentList.size();
            int hisCommentLength = historyCommentList.size();

            if((curCommentLength == hisCommentLength)) {//有撤回操作
                int i;
                for (i = 0; i<commentList.size(); i++) {
                    CommentVO commentVO = commentList.get(i);
                    commentVO.setUserName(getUserNamesFromLocal(commentVO.getUserId()));
                    if(i != 0) {
                        setOtherCommentNextAssign(historyCommentList, commentVO, i);
                    }else {
                        setFirstCommentNextAssign(base, commentVO);
                    }
                }
            }else {
                int i;
                for (i = 0; i<commentList.size(); i++) {
                    CommentVO commentVO = commentList.get(i);
                    String userName = getUserNamesFromLocal(commentVO.getUserId());
                    if(StringUtil.isEmpty(userName)){
                        userName = base.getAssignedName();
                    }
                    commentVO.setUserName(userName);
                    if(i != 0) {
                        int j = i-1;
                        setOtherCommentNextAssign(historyCommentList, commentVO, j);
                    }else {
                        setFirstCommentNextAssign(base, commentVO);
                    }
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println("评论列表："+commentList);
        return commentList;
    }


    /**
     * 设置最新一条评论的下一接收人
     * @param base
     * @param commentVO
     */
    private void setFirstCommentNextAssign(BaseVO base, CommentVO commentVO) {
        if(base.isEnd()) {
            commentVO.setNextAssignName("流程结束");
        }else {
            final String nextAssign = base.getAssign();
            String nextAssignName = base.getAssignName();
            commentVO.setNextAssign(nextAssign);
            if(StringUtil.isEmpty(nextAssignName) && StringUtil.isNotEmpty(nextAssign)) {
                nextAssignName = getUserNamesFromLocal(nextAssign);
            }
            commentVO.setNextAssignName(nextAssignName);
        }
    }


    /**
     * 设置除第一个之外的评论的下一接收人
     * @param historyCommentList
     * @param commentVO
     * @param j
     */
    private void setOtherCommentNextAssign(List<CommentVO> historyCommentList, CommentVO commentVO, int j) {
        CommentVO historyComment;
        if (j < historyCommentList.size()) {
            historyComment = historyCommentList.get(j);
            final String nextAssign = historyComment.getNextAssign();
            String nextAssignName = historyComment.getNextAssignName();
            commentVO.setNextAssign(nextAssign);
            if(StringUtil.isEmpty(nextAssignName) && StringUtil.isNotEmpty(nextAssign)) {
                nextAssignName = getUserNamesFromLocal(nextAssign);
            }
            commentVO.setNextAssignName(nextAssignName);
        }
    }

    /**
     * 根据userId查找userName,存在多个用逗号分隔
     *
     * @param userIds
     * @return
     */
    public String getUserNamesFromLocal(String userIds){
        String userName = "";
        if (StringUtil.isEmpty(userIds))
            return userName;
        userName = this.processControllder.getUserNames(userIds);
        if (userName.contains(",")) {
            userName = userName.substring(0, userName.length() - 1);
        }
        return userName;
    }



    private String getActivitiType(String businessKey, String taskDefinitionKey) {
        CusUserTask userTask = null;
        try {
            userTask = userTaskService.findByProcAndTask(businessKey, taskDefinitionKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String activitiType = null == userTask ? "":userTask.getActivityType();
        activitiType = StringUtil.isEmpty(activitiType) ? "N" : activitiType;
        return activitiType;
    }

    /**
     * 获取任务task的候选人
     */
    public String getCandidateIdsOfTask(List<Task> taskList) {
        String candidateUserIds = "";
        for (Task task : taskList) {
            if(task.getAssignee() != null) {
                candidateUserIds = candidateUserIds.concat(task.getAssignee()+",");//只能获取多实例的执行人，无法获取候选人
            }else {
                List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
                for (IdentityLink identityLink : identityLinks) {
                    candidateUserIds = candidateUserIds.concat(identityLink.getUserId()+",");
                }
            }
        }
        candidateUserIds = StringUtil.cropTail(candidateUserIds);
        return candidateUserIds;
    }

    /**
     * 获取一个任务的评论
     *
     * @param taskId
     * @return
     * @throws Exception
     */
    public List<CommentVO> getCommentsByTaskId(String taskId){
        List<Comment> comments = this.taskService.getTaskComments(taskId);
        List<CommentVO> commnetList = sortAndFormatComments(comments);
        return commnetList;
    }

    /**
     * 获取一个流程的全部评论
     *
     * @param processInstanceId
     * @return
     */
    public List<CommentVO> getComments(String processInstanceId) {
        List<Comment> comments = this.taskService.getProcessInstanceComments(processInstanceId);
        List<CommentVO> commnetList = sortAndFormatComments(comments);
        return commnetList;
    }

    private List<CommentVO> sortAndFormatComments(List<Comment> comments) {
        comments.sort((o1, o2) -> {
            try {
                if (o1 == null || o2 == null)
                    return -1;
                Date dt1 = o1.getTime();
                Date dt2 = o2.getTime();
                if (dt1 == null || dt2 == null)
                    return -1;
                if (dt1.getTime() >= dt2.getTime()) {
                    return -1;
                } else {
                    return 1;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        });
        List<CommentVO> commnetList = new ArrayList<>();
        for (Comment comment : comments) {
            CommentVO vo = new CommentVO();
            vo.setId(comment.getId());
            vo.setUserId(comment.getUserId());
            vo.setContent(comment.getFullMessage());
            vo.setTime(comment.getTime());
            vo.setTaskId(comment.getTaskId());
            vo.setProcessInstanceId(comment.getProcessInstanceId());
            //			vo.setUserName(getUserName(comment.getUserId()));
            commnetList.add(vo);
        }
        return commnetList;
    }

    /**
     * 根据任务id或流程实例id查询实体对象，如果是办结事宜，参数为流程实例id，否则为任务id
     */
    public BaseVO getBaseVOByTaskIdOrProcessInstanceId(String userName,String queryId) {
        BaseVO base;
        Task task = getTaskByTaskId(queryId);
        if (task == null) {
            base = processService.getBaseVOFromHistoryVariable(queryId);
            if(base != null) {
                HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(queryId).singleResult();
                if (historicTaskInstance != null) {
                    setBaseVO(base, userName,1, null,historicTaskInstance,null, null);
                } else {
                    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                            .processInstanceId(queryId).singleResult();
                    setBaseVO(base, userName,1, null,null,null, historicProcessInstance);
                }
            }
        } else {
            base = processService.getBaseVOFromRu_Variable(task.getProcessInstanceId());
            if (null != base) {
                setBaseVO(base, userName,1, task,null,null, null);
            }
        }
        return base;
    }

    public void setAssigness(String taskId, String userId) {
        taskService.setAssignee(taskId,userId);
    }

    /**
     * 设置任务候选人
     * @param taskId
     * @param userIds
     */
    public void addCandidateUsers(String taskId,String userIds){
        String[] assigneeIds = userIds.split(",");
        List<String> assigneeList = Arrays.asList(assigneeIds);
        if(null == assigneeList)return;
        for (String candidateUser : assigneeList) {
            taskService.addCandidateUser(taskId,candidateUser);
        }
    }

    /**
     * 删除任务候选人
     * @param taskId
     * @param userId
     */
    public void deleteCandidateUser(String taskId,String userId){
        taskService.deleteCandidateUser(taskId,userId);
    }

    /**
     * userId认领任务 <br/>
     * task.setAssignee(userId)
     * 可用于候选组与候选用户，不能用于多实例节点
     * 认领以后，这个用户就会成为任务的执行人 ， 任务会从组的其他成员的任务列表中消失
     *
     * @param taskId 签收的taskid
     * @param userId 签收人id
     * @param userName 签收人名称
     * @param msg 意见
     * @param hasComments 是否加入评论列表
     * @param baseVO 流程实体类
     * @throws Exception
     */

    public void claim(String taskId, String userId,String userName,String msg,boolean hasComments,BaseVO baseVO) throws Exception {
        Authentication.setAuthenticatedUserId(userId);
        this.taskService.claim(taskId, userId);
        //设置当前任务的拥有者，因为在转发任务后，再认领，再执行任务时会先执行被委托完成操作，task会执行setAssignee(task.getOwner());
        this.taskService.setOwner(taskId, userId);//会往评论表插入一条记录

        if(hasComments) {
            Task task = getTaskByTaskId(taskId);
            String processInstanceId = task.getProcessInstanceId();
            ProcessStatus handleFlag = ProcessStatus.TASK_CLAIMED;
            addComment(taskId,processInstanceId, handleFlag, msg);

            Map<String, Object> variables = new HashMap<>();
            baseVO.setProcessStatus(handleFlag);
            baseVO.setDescription("任务已被"+userName+"认领");
            variables.put("entity", baseVO);
            this.processService.setVariables(processInstanceId,variables);
        }
        baseVO.setCandidate_ids(userId);
        baseVO.setCandidate_names(userName);
        userTaskService.updateUserTaskAssignee(baseVO, false, baseVO.getTaskDefinitionKey());
    }


    /**
     * 委托任务给userId，<br/>
     * 是将任务节点分给其他人处理，等其他人处理好之后，委派任务会自动回到委派人的任务中 <br/>
     * setDelegationState(DelegationState.PENDING);<br/>
     * task.setOwner(task.getAssignee());<br/>
     * task.setAssignee(userId)<br/>
     * <br/>
     * @param taskId 被委托的任务id
     * @param fromUserId 委托人id
     * @param toUserId 被委托人id
     * @param msg 意见
     * @param variables 流程变量
     */
    public void delegateTask(String taskId, String fromUserId,String toUserId,String msg,Map<String, Object> variables) {
        Task task = getTaskByTaskId(taskId);
        if(task == null) {
            throw new ActivitiObjectNotFoundException("任务"+taskId+"不存在");
        }
        this.taskService.delegateTask(taskId, toUserId);
        if(variables.size() > 0) {
            Authentication.setAuthenticatedUserId(fromUserId);
            String processInstanceId = task.getProcessInstanceId();
            addComment(taskId,processInstanceId, ProcessStatus.TASK_PENDING, msg);
            this.processService.setVariables(processInstanceId,variables);
        }
    }

    /**
     * 被委派人办理任务后
     * 正在运行的任务表中被委派人办理任务后hr的任务会回到委派人xxhr ，历史任务表中也一样<br/>
     *
     * setDelegationState(DelegationState.RESOLVED);<br/>
     * setAssignee(task.getOwner());<br/>
     * <br/>
     * 注意:taskService.resolveTask(taskId, variables)方法是将variables存到Task域中，为了保证每次获取到的实体变量都是流程最新的，<br/>
     * 我们需要调用processService.setVariables(task.getProcessInstanceId(),variables);将variables存到execution中
     * @param taskId
     * @param variables
     */
    private void resolveTask(String taskId,String processInstanceId, Map<String, Object> variables/*,String content*/) {

//        Task task = getTaskByTaskId(taskId);
//        Authentication.setAuthenticatedUserId(task.getAssignee());
//
//        if (StringUtil.isEmpty(content)) {
//            content = getComment(ProcessStatus.TASK_RESOLVED);
//        }
//        String processInstanceId = task.getProcessInstanceId();
//        addComment(taskId, processInstanceId, content);

        this.processService.setVariables(processInstanceId,variables);
        this.taskService.resolveTask(taskId, variables);
    }





    /**
     * 撤回任务，一次只能撤回一个节点
     */
    public Integer revoke(String userId,String backToTaskId,String backToActivitiType,String backFromTaskId,String backFromActivitiType) throws Exception {
        int revokeFlag = recall(userId,backToTaskId,backToActivitiType,backFromTaskId,backFromActivitiType);
        return revokeFlag;
    }

    /**
     * 撤回流程
     * @param userId
     * @param backToTaskId
     * @param backToActivitiType
     * @param backFromTaskId
     * @param backFromActivitiType
     * @return
     */
    private int recall(String userId,String backToTaskId,String backToActivitiType,String backFromTaskId,String backFromActivitiType) {

        HistoricTaskInstance hisTaskInstanceBackTo = this.historyService.createHistoricTaskInstanceQuery().taskId(backToTaskId).singleResult();
        HistoricTaskInstance hisTaskInstanceBackFrom = this.historyService.createHistoricTaskInstanceQuery().taskId(backFromTaskId).singleResult();
        if(null == hisTaskInstanceBackTo || null == hisTaskInstanceBackFrom) return -1;

        boolean result1 = isProcessInstanceEnd(hisTaskInstanceBackTo.getProcessInstanceId());
        if(result1) return 1;
        boolean result2 = isCommitTaskBy(userId, backToTaskId, hisTaskInstanceBackTo.getAssignee());
        if(!result2) return 2;
        boolean result3 = isNextTaskComplete(backFromTaskId, backFromActivitiType, hisTaskInstanceBackFrom.getTaskDefinitionKey());
        if(result3) return 3;


        Map<String, Object> variables = runtimeService.getVariables(hisTaskInstanceBackTo.getProcessInstanceId());
        if("M".equals(backToActivitiType)) {//多实例节点,重置节点通过的条件
            resetMembers(variables,hisTaskInstanceBackTo.getTaskDefinitionKey());
        }
        System.out.println(variables);


        JdbcTemplate jdbcTemplate = ApplicationContextHandler.getBean(JdbcTemplate.class);

        List<HistoricTaskInstance> backToHTIList = null;
        if("M".equals(backToActivitiType)) {//多实例节点
            /*该节点有多少个实例删多少个*/
            Object obj = processService.getHistoryVariable("nrOfInstances",hisTaskInstanceBackTo.getProcessInstanceId());
            int members = obj == null ? 0 : (int) obj;

            backToHTIList = this.historyService.createHistoricTaskInstanceQuery().taskDefinitionKey(hisTaskInstanceBackTo.getTaskDefinitionKey()).orderByTaskCreateTime().desc().list();
            backToHTIList = backToHTIList.subList(0, members);
        }else {
            backToHTIList = this.historyService.createHistoricTaskInstanceQuery().taskId(backToTaskId).list();
        }

        for (HistoricTaskInstance historicTaskInstance : backToHTIList) {
            deleteHisEGA(hisTaskInstanceBackTo, jdbcTemplate);
            //删除历史行为
            jdbcTemplate.update("delete from ACT_HI_ACTINST where task_id_=?", hisTaskInstanceBackTo.getId());
            deleteHistoricTasks(historicTaskInstance);
        }

        // 取得流程定义
        ProcessDefinitionEntity definitionEntity = (ProcessDefinitionEntity) repositoryService
                .getProcessDefinition(hisTaskInstanceBackTo.getProcessDefinitionId());
        System.out.println(definitionEntity);

        ActivityImpl currActivity = definitionEntity.findActivity(hisTaskInstanceBackFrom.getTaskDefinitionKey());
        ActivityImpl destActiviti = definitionEntity.findActivity(hisTaskInstanceBackTo.getTaskDefinitionKey());
        System.out.println(destActiviti);
        updateNextUserTaskActivitiTransition(userId,hisTaskInstanceBackFrom.getProcessInstanceId(),hisTaskInstanceBackFrom.getTaskDefinitionKey(),hisTaskInstanceBackFrom.getId(),currActivity,destActiviti,variables,"", jdbcTemplate);
        return 0;
    }

    /**
     * 判断流程是否已结束
     * @param processInstanceId
     * @return
     */
    private boolean isProcessInstanceEnd(String processInstanceId) {
        // 取得流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if(null == processInstance) return true;
        return false;
    }

    /**
     * 判断任务是否为当前用户提交
     * @param userId
     * @param backToTaskId
     * @param backToTaskAssignee
     * @return
     */
    private boolean isCommitTaskBy(String userId, String backToTaskId, String backToTaskAssignee) {
        //检测任务是否为该用户提交
        if(StringUtil.isEmpty(backToTaskAssignee)) {//当前节点如果是会签节点，用户作为候选人参与时，assignee为null
            List<Comment> comments = this.taskService.getTaskComments(backToTaskId);
            System.out.println("revokeTask comments:"+comments);
            backToTaskAssignee = (null != comments && comments.size()>0) ? comments.get(0).getUserId() : null;
        }
        if(!userId.equals(backToTaskAssignee)) {
            return false;
        }
        return true;
    }

    /**
     * 判断撤回的目标hisTaskInstanceBackFrom任务是否已执行
     * @param backFromTaskId
     * @param backFromActivitiType
     * @param taskDefinitionKey
     * @return
     */
    private boolean isNextTaskComplete(String backFromTaskId, String backFromActivitiType,
                                       String taskDefinitionKey) {
        //获取下一节点是否已经执行
        List<HistoricTaskInstance> htiList = null;
        int completeCount = 0;
        if("M".equals(backFromActivitiType)) {//多实例节点
            htiList = this.historyService.createHistoricTaskInstanceQuery().taskDefinitionKey(taskDefinitionKey).list();
        }else {
            htiList = this.historyService.createHistoricTaskInstanceQuery().taskId(backFromTaskId).list();
        }
        if(null != htiList) {
            for (HistoricTaskInstance hti : htiList) {//多实例节点由多个任务，全部执行完成才认为已经通过
                if("completed".equals(hti.getDeleteReason())){
                    completeCount += 1;
                }
            }
        }
        if(completeCount > 0 && completeCount == htiList.size()) {
            return true;
        }
        return false;
    }
    /**
     * 重置多实例任务节点同意人数或驳回人数流程变量
     * @param variables
     * @param taskDefinitionKey
     */
    private void resetMembers(Map<String, Object> variables, String taskDefinitionKey) {
        variables.put("backMembers"+"_"+taskDefinitionKey, 0);
        variables.put("agreeMembers"+"_"+taskDefinitionKey, 0);
    }

    /**
     *
     * @param backFromTaskId
     * @param fromUserId
     * @param destTaskKey
     * @param msg
     * @param variables
     * @throws Exception
     */
    public void rollBackToAssignActivitiKey(String backFromTaskId, String fromUserId, String destTaskKey, String msg, Map<String, Object> variables) throws Exception {
        Task backFromTask = getTaskByTaskId(backFromTaskId);
        boolean suspend = backFromTask.isSuspended();
        if(suspend){
            throw new Exception("抱歉！该流程已经挂起，请先激活再进行回退操作~");
        }
        HistoricTaskInstance hiTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(backFromTaskId).finished().singleResult();
        if(hiTaskInstance != null) {
            throw new Exception("任务已结束，不能进行回退操作");
        }
        if(backFromTask == null) {
            throw new Exception("要驳回的任务不存在");
        }

        try {
            String nextUserActivityId = ProcessDefinitionCache.get().getNextActivitiId(backFromTask.getProcessInstanceId(),backFromTask.getTaskDefinitionKey(),"false");
            if(nextUserActivityId.equals(destTaskKey)) {
                backToLastActivity(fromUserId,backFromTask,variables,msg);
            }else {
                jumpToActivity(backFromTask,destTaskKey,fromUserId,variables,msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 正常后退一个节点
     * @param fromUserId
     * @param task
     * @param variables
     * @param msg
     */
    private void backToLastActivity(String fromUserId, Task task, Map<String, Object> variables, String msg) {
        final String variableKey = "isPass" + "_" + task.getTaskDefinitionKey();
        variables.put(variableKey, "false");
        completeTaskAndAddComment(fromUserId,task.getId(),task.getProcessInstanceId(),variables,msg);
    }

    private void completeTaskAndAddComment(String fromUserId,String taskId,String processInstanceId, Map<String, Object> variables, String msg) {
        Authentication.setAuthenticatedUserId(fromUserId);
        addComment(taskId,processInstanceId, ProcessStatus.APPROVAL_FAILED, msg);
        completeTask(taskId, variables);
    }

    /**
     * 退回，跳转到指定节点
     * @param backFromTask
     * @param destTaskKey
     * @param fromUserId
     * @param variables
     * @param msg
     */
    private void jumpToActivity(Task backFromTask, String destTaskKey, String fromUserId, Map<String, Object> variables, String msg) throws Exception {
        ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity)((RepositoryServiceImpl)repositoryService).getDeployedProcessDefinition(backFromTask.getProcessDefinitionId());
        //当前活动节点
        ActivityImpl backFromActivity = processDefinition.findActivity(backFromTask.getTaskDefinitionKey());

        //目标活动节点
        ActivityImpl destActivity = processDefinition.findActivity(destTaskKey);
        if(destActivity == null) {
            throw new Exception("要退回的节点不存在");
        }
        JdbcTemplate jdbcTemplate = ApplicationContextHandler.getBean(JdbcTemplate.class);
        deleteHistoricActivitiAndTask(backFromTask, destTaskKey,jdbcTemplate);
        updateNextUserTaskActivitiTransition(fromUserId,backFromTask.getProcessInstanceId(),backFromTask.getTaskDefinitionKey(),backFromTask.getId(),backFromActivity,destActivity,variables,msg, jdbcTemplate);
    }

    /**
     * 删除历史节点和历史任务
     * @param backFromTask
     * @param destTaskKey
     * @param jdbcTemplate
     */
    private void deleteHistoricActivitiAndTask(Task backFromTask, String destTaskKey,JdbcTemplate jdbcTemplate) {
        List<HistoricActivityInstance> finishActivities = this.processService.getFinishedActivityInstanceList(backFromTask.getProcessInstanceId(),"");
        int destTaskIndex = 0;
        for (int i = 0;i<finishActivities.size();i++){
            if(destTaskKey.equals(finishActivities.get(i).getActivityId())){
                destTaskIndex = i;
                break;
            }
        }

        int toDeleteActivitySize = finishActivities.size() - destTaskIndex;//删除历史任务表中当前节点到目标节点中间的节点数
        List<HistoricActivityInstance> toDeleteActivities = this.processService.getFinishedActivityInstanceListLimit(backFromTask.getProcessInstanceId(),"",toDeleteActivitySize);
        List<HistoricTaskInstance> toDeleteTasks = this.historyService.createHistoricTaskInstanceQuery().processInstanceId(backFromTask.getProcessInstanceId()).taskDefinitionKey(destTaskKey).orderByTaskCreateTime().desc().list();

        //删除历史Activiti
        for (HistoricActivityInstance activityInstance:toDeleteActivities){
            if(null != activityInstance.getTaskId()){//有taskId则按taskId删除
                jdbcTemplate.update("delete from ACT_HI_ACTINST where task_id_=? ", activityInstance.getTaskId());
            }else {//主要处理网关节点
                jdbcTemplate.update("delete from ACT_HI_ACTINST where act_id_=? and PROC_INST_ID_=?", activityInstance.getActivityId(), activityInstance.getProcessInstanceId());
            }
            if("userTask".equals(activityInstance.getActivityType())) {
                List<HistoricTaskInstance> nextActivityTaskList = this.historyService.createHistoricTaskInstanceQuery().processInstanceId(activityInstance.getProcessInstanceId()).taskDefinitionKey(activityInstance.getActivityId()).orderByTaskCreateTime().desc().list();
                toDeleteTasks.addAll(nextActivityTaskList);
            }
        }

        //删除历史task
        Map<String, HistoricTaskInstance> map = new LinkedHashMap<>();
        // 遍历相同的id，替换最新的值
        for (HistoricTaskInstance historicTaskInstance : toDeleteTasks) {
            map.put(historicTaskInstance.getId(), historicTaskInstance);
        }
        List<HistoricTaskInstance> toDeleteTaskList = new LinkedList<>(map.values());
        for (HistoricTaskInstance historicTaskInstance : toDeleteTaskList) {
            deleteHistoricTasks(historicTaskInstance);
        }
    }

//	private void addToDeleteActivitiesHis(int toDeleteActivitySize,String processInstanceId, String destTaskKey, List<HistoricTaskInstance> toDeleteTasks) throws Exception {
//		for (int i =1;i<toDeleteActivitySize;i++){
//			String nextUserActivityId = ProcessDefinitionCache.get().getNextActivitiId(processInstanceId,destTaskKey,"true");
//			List<HistoricTaskInstance> nextActivityTaskList = this.historyService.createHistoricTaskInstanceQuery().taskDefinitionKey(nextUserActivityId).orderByTaskCreateTime().desc().list();
//			toDeleteTasks.addAll(nextActivityTaskList);
//
//			addToDeleteActivitiesHis(toDeleteActivitySize-1,processInstanceId,nextUserActivityId,toDeleteTasks);
//		}
//	}

    /**
     * 删除当前节点的历史信息
     * @param hisTaskInstanceBackTo
     */
    private void deleteHistoricTasks(HistoricTaskInstance hisTaskInstanceBackTo) {
        deleteHistoricTaskInstance(hisTaskInstanceBackTo.getId());
    }

    /**
     * 删除上一步活动的网关节点
     * @param hisTaskInstanceBackTo
     * @param jdbcTemplate
     */
    private void deleteHisEGA(HistoricTaskInstance hisTaskInstanceBackTo, JdbcTemplate jdbcTemplate) {
        ActivityImpl nextNodeInfo;
        try {
            // 取得上一步活动的网关节点
            nextNodeInfo = ProcessDefinitionCache.get().getNextNodeInfo(repositoryService,runtimeService,hisTaskInstanceBackTo.getProcessInstanceId(),hisTaskInstanceBackTo.getTaskDefinitionKey());
            if(null != nextNodeInfo) {
                HistoricActivityInstance exclusiveGatewayActivity = this.historyService.createHistoricActivityInstanceQuery().activityId(nextNodeInfo.getId()).singleResult();
                if(null != exclusiveGatewayActivity) {
                    //删除网关节点的历史行为
                    jdbcTemplate.update("delete from ACT_HI_ACTINST where act_id_=? and PROC_INST_ID_=?", exclusiveGatewayActivity.getActivityId(),exclusiveGatewayActivity.getProcessInstanceId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将backFromActivity任务后面的方向清空，把destActivity任务拼接到原来的判断网关，然后结束backFromTask任务
     * @param fromUserId
     * @param processInstanceId
     * @param backFromTaskDefKey
     * @param backFromTaskId
     * @param backFromActivity
     * @param destActivity
     * @param variables
     * @param msg
     * @param jdbcTemplate
     */
    private void updateNextUserTaskActivitiTransition(String fromUserId,String processInstanceId,String backFromTaskDefKey,String backFromTaskId,ActivityImpl backFromActivity,ActivityImpl destActivity,Map<String, Object> variables,String msg,
                                                      JdbcTemplate jdbcTemplate) {

        //所有的出口集合
        List<PvmTransition> pvmTransitions = backFromActivity.getOutgoingTransitions();
        List<PvmTransition> oriPvmTransitions = new ArrayList<>();
        for (PvmTransition pvmTransition : pvmTransitions) {
            oriPvmTransitions.add(pvmTransition);
        }
        //清除所有出口
        pvmTransitions.clear();

        //建立新的出口
        List<TransitionImpl> transitionImpls = new ArrayList<>();
        TransitionImpl newTransition = backFromActivity.createOutgoingTransition();
        newTransition.setDestination(destActivity);
        transitionImpls.add(newTransition);

        //结束backFromTask任务
        List<Task> currentTaskList = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(backFromTaskDefKey).list();
        for (Task currTask : currentTaskList) {
            taskService.claim(currTask.getId(), null);
            if(backFromTaskId.equals(currTask.getId())) {
                completeTaskAndAddComment(fromUserId, currTask.getId(),processInstanceId, variables, msg);//执行多任务节点时只添加一条评论
            }else {
                taskService.complete(currTask.getId(), variables);
            }
            deleteHistoricTaskInstance(currTask.getId());
            //删除历史行为
            jdbcTemplate.update("delete from ACT_HI_ACTINST where task_id_=?", currTask.getId());
        }

        // 恢复方向
        for (TransitionImpl tempTransition : transitionImpls) {
            backFromActivity.getOutgoingTransitions().remove(tempTransition);
        }

        for (PvmTransition pvmTransition : oriPvmTransitions) {
            pvmTransitions.add(pvmTransition);
        }
    }

    /**
     * 删除历史任务表
     * @param taskId
     */
    private void deleteHistoricTaskInstance(String taskId) {
//		historyService.deleteHistoricTaskInstance(taskId);//会级联删除评论
        managementService.executeCommand(new DeleteHistoricTaskInstanceCmdImpl(taskId));
    }
}
