package com.zr.workflow.activiti.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.ActivitiTaskAlreadyClaimedException;
import org.activiti.engine.task.Task;
import org.apache.avalon.framework.parameters.ParameterException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zr.workflow.activiti.entity.BaseVO;
import com.zr.workflow.activiti.entity.CommentVO;
import com.zr.workflow.activiti.entity.CusUserTask;
import com.zr.workflow.activiti.entity.Page;
import com.zr.workflow.activiti.entity.ProcessStatus;
import com.zr.workflow.activiti.service.CusTaskService;
import com.zr.workflow.activiti.service.CusUserTaskService;
import com.zr.workflow.activiti.service.ProcessService;
import com.zr.workflow.activiti.util.GFJsonUtil;
import com.zr.workflow.activiti.util.ProcessDefinitionCache;
import com.zr.workflow.activiti.util.StringUtil;

/**
 * 任务节点相关web类
 * @author zhourq
 *
 */
@RestController
@RequestMapping("/task")
public class TaskController {
	@Resource
	private CusTaskService cusTaskService;
	@Resource
	private ProcessService processService;
	@Resource
	private CusUserTaskService cusUserTaskService;
	@Resource
	private ProcessController processControllder;


    /**
     * 根据流程实例id查询流程实例
     *
     * @param processInstanceId 指定流程实例id,必输
     * @return
     */
    @RequestMapping(value = "/findProcessInstanceByInstanceId")
    public String findProcessInstanceByInstanceId(@RequestParam("processInstanceId") String processInstanceId) {

        Map<String, Object> resultMap = new HashMap<>();
        try {
            if(StringUtil.isEmpty(processInstanceId)){
                resultMap.put("type", "empty");
                resultMap.put("msg", "必输参数：指定流程实例processInstanceId不能为空");
            }else {
                BaseVO processInstance = this.processService.findProcessInstanceByInstanceId("",processInstanceId);
                System.out.println("流程实例为：" + processInstance);
                if(null == processInstance){
                    resultMap.put("type", "error");
                    resultMap.put("msg", "指定流程不存在");
                }else {
                    resultMap.put("type", "success");
                    resultMap.put("data", processInstance);
                }
            }
        } catch (Exception e) {
            resultMap.put("type", "error");
            resultMap.put("msg", e.getMessage());
            e.printStackTrace();
        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }

    /**
     * 所有流程实例
     *
     * @param page 当前第几页,非必输
     * @param rows 每页显示数据数,非必输
     * @param processDefKeys 指定流程定义ids,非必输
     * @return
     */
    @RequestMapping(value = "/findAllProcess")
    public String findAllProcess(@RequestParam(value = "page", required = false) Integer page,
                                 @RequestParam(value = "rows", required = false) Integer rows,
                                 @RequestParam(value = "processDefKeys", required = false) String processDefKeys) {

        Map<String, Object> resultMap = new HashMap<>();
        try {
            List<BaseVO> processList = getTaskList("allProcess",page, rows, processDefKeys, "", "","");
            System.out.println("所有流程实例：" + processList);
            resultMap.put("type", "success");
            resultMap.put("totalSize", (processList!= null && processList.size() > 0) ? processList.get(0).getTotal() : 0);
            resultMap.put("data", processList);
        } catch (Exception e) {
            resultMap.put("type", "error");
            resultMap.put("msg", e.getMessage());
            e.printStackTrace();
        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }

    /**
     * 我的请求
     *
     * @param page 当前第几页,非必输
     * @param rows 每页显示数据数,非必输
     * @param processDefKeys 指定流程定义ids,非必输
     * @param userId 用户id,必输
     * @param userName 用户名,必输
     * @return
     */
    @RequestMapping(value = "/myAllProcess")
    public String myAllProcess(@RequestParam(value = "page", required = false) Integer page,
                               @RequestParam(value = "rows", required = false) Integer rows,
                               @RequestParam(value = "processDefKeys", required = false) String processDefKeys,
                               @RequestParam("userId") String userId,
                               @RequestParam("userName") String userName) {

        Map<String, Object> resultMap = new HashMap<>();
        try {
            judgeQueryParams(userId,userName);
            List<BaseVO> processList = getTaskList("myProcess",page, rows, processDefKeys, userId, userName,"");
            System.out.println("我的请求：" + processList);
            resultMap.put("type", "success");
            resultMap.put("totalSize", (processList!= null && processList.size() > 0) ? processList.get(0).getTotal() : 0);
            resultMap.put("data", processList);
        } catch (ParameterException e) {
            resultMap.put("type", "empty");
            resultMap.put("msg", e.getMessage());
        } catch (Exception e) {
            resultMap.put("type", "error");
            resultMap.put("msg", e.getMessage());
            e.printStackTrace();

        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }
    /**
     * 查询待办任务
     *
     * @param page 当前第几页,非必输
     * @param rows 每页显示数据数,非必输
     * @param processDefKeys 指定流程定义ids,非必输
     * @param userId 用户id,必输
     * @param userName 用户名,必输
     * @return
     */
    @RequestMapping(value = "/todoTask")
    public String findToDoTask(@RequestParam(value = "page", required = false) Integer page,
                               @RequestParam(value = "rows", required = false) Integer rows,
                               @RequestParam(value = "processDefKeys", required = false) String processDefKeys,
                               @RequestParam("userId") String userId,
                               @RequestParam("userName") String userName) {

        Map<String, Object> resultMap = new HashMap<>();
        try {
            judgeQueryParams(userId,userName);
            List<BaseVO> taskList = getTaskList("todo",page, rows, processDefKeys, userId, userName,"");

            System.out.println("待办任务：" + taskList);
            resultMap.put("type", "success");
            resultMap.put("totalSize", (taskList!= null && taskList.size() > 0) ? taskList.get(0).getTotal() : 0);
            resultMap.put("data", taskList);
        } catch (ParameterException e) {
            resultMap.put("type", "empty");
            resultMap.put("msg", e.getMessage());
        } catch (Exception e) {
            resultMap.put("type", "error");
            resultMap.put("msg", e.getMessage());
            e.printStackTrace();
        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }

    /**
     * 查看已办任务列表<br/>
     * @param page 当前第几页,非必输
     * @param rows 每页显示数据数,非必输
     * @param processDefKeys 指定流程定义ids,非必输
     * @param userId 用户id,必输
     * @param userName 用户名,必输
     * @param dataType 数据类型:默认获取所有的已办事宜，"lastet":获取最新的已办事宜,非必输
     * @return
     */
    @RequestMapping(value = "/doneTask")
    public String findDoneTask(@RequestParam(value = "page", required = false) Integer page,
                               @RequestParam(value = "rows", required = false) Integer rows,
                               @RequestParam(value = "processDefKeys", required = false) String processDefKeys,
                               @RequestParam(value = "dataType", required = false) String dataType,
                               @RequestParam("userId") String userId,
                               @RequestParam("userName") String userName) {

        Map<String, Object> resultMap = new HashMap<>();
        try {

            judgeQueryParams(userId,userName);
            List<BaseVO> taskList = getTaskList("done",page, rows, processDefKeys, userId, userName,dataType);

            System.out.println("已办任务 ：" + taskList);
            resultMap.put("type", "success");
            resultMap.put("totalSize", (taskList!= null && taskList.size() > 0) ? taskList.get(0).getTotal() : 0);
            resultMap.put("data", taskList);
        } catch (ParameterException e) {
            resultMap.put("type", "empty");
            resultMap.put("msg", e.getMessage());
        } catch (Exception e) {
            resultMap.put("type", "error");
            resultMap.put("msg", e.getMessage());
            e.printStackTrace();
        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }

    /**
     * 查看办结任务列表
     *
     * @param page 当前第几页,非必输
     * @param rows 每页显示数据数,非必输
     * @param processDefKeys 指定流程定义ids,非必输
     * @param userId 用户id,必输
     * @param userName 用户名,必输
     * @return
     */
    @RequestMapping(value = "/finishedTask")
    public String findFinishedTask(@RequestParam(value = "page", required = false) Integer page,
                                   @RequestParam(value = "rows", required = false) Integer rows,
                                   @RequestParam(value = "processDefKeys", required = false) String processDefKeys,
                                   @RequestParam("userId") String userId,
                                   @RequestParam("userName") String userName) {
        Map<String, Object> resultMap = new HashMap<>();
        try {

            judgeQueryParams(userId,userName);

            List<BaseVO> taskList = getTaskList("finished",page, rows, processDefKeys, userId, userName,"");

            System.out.println("办结任务：" + taskList);
            resultMap.put("type", "success");
            resultMap.put("totalSize", (taskList!= null && taskList.size() > 0) ? taskList.get(0).getTotal() : 0);
            resultMap.put("data", taskList);
        } catch (ParameterException e) {
            resultMap.put("type", "empty");
            resultMap.put("msg", e.getMessage());
        } catch (Exception e) {
            resultMap.put("type", "error");
            resultMap.put("msg", e.getMessage());
            e.printStackTrace();

        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }


    /**
     * 必输参数非空校验
     * @param userId
     * @param userName
     * @throws ParameterException
     */
    private void judgeQueryParams(String userId,String userName) throws ParameterException {
        String errorMsg = "";
        if(StringUtil.isEmpty(userId)){
            errorMsg = "必输参数:查询人userId不能为空";
        }
        if(StringUtil.isEmpty(userName)){
            errorMsg = "必输参数:查询人名称userName不能为空";
        }
        if(StringUtil.isNotEmpty(errorMsg)){
            throw new ParameterException(errorMsg);
        }
    }


    /**
     * 获取事宜列表
     * @param taskType 任务类型
     * @param page 第几页
     * @param rows 一页显示的行数
     * @param processDefKeys 指定流程id
     * @param userId 指定用户名
     * @param userName 指定用户名
     * @param dataType 数据类型:默认获取所有的已办事宜，"lastet":获取最新的已办事宜,非必输
     * @return
     */
    private List<BaseVO> getTaskList(String taskType,Integer page, Integer rows, String processDefKeys, String userId,String userName,String dataType){
        Page<BaseVO> p = this.initPage(page, rows);
        List<String> processDefKeyList = this.getProcessDefKeysFromJson(processDefKeys);
        List<BaseVO> processList = new ArrayList<>();
        switch (taskType){
            case "allProcess":
                processList = this.processService.findAllProcessInstances(p,userName,processDefKeyList);
                break;
            case "myProcess":
                processList = this.processService.findMyProcessInstances(p, userId,userName,processDefKeyList);
                break;
            case "todo":
                processList = this.cusTaskService.findTodoTask(userId,userName, p,processDefKeyList);
                break;
            case "done":
                processList = this.cusTaskService.findDoneTask(userId,userName, p,dataType,processDefKeyList);
                break;
            case "finished":
                processList = this.processService.findFinishedProcessInstances(p, userId,userName, true,processDefKeyList);
                break;
        }
        return processList;
    }


    /**
     * 初始化分页
     * @param page
     * @param rows
     * @return
     */
    public Page<BaseVO> initPage(Integer page, Integer rows) {
        Page<BaseVO> p = (null == page || null == rows) ? null : new Page<>(page, rows);
        return p;
    }


    public List<String> getProcessDefKeysFromJson(String processDefKeys) {
        List<String> processDefKeyList = new ArrayList<>();
        if(StringUtil.isNotEmpty(processDefKeys)) {
            JSONArray jsonArray = GFJsonUtil.get().parseArray(processDefKeys);
            processDefKeyList = jsonArray.toJavaList(String.class);
        }
        return processDefKeyList;
    }


    /**
     * 查询单个事宜详情
     *
     * @param taskId 任务id<br/>
     * @return
     */
    @RequestMapping("/flowDetail")
    public String flowDetail(@RequestParam("taskId") String taskId) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            if(StringUtil.isEmpty(taskId)) {
                resultMap.put("type", "error");
                resultMap.put("msg", "taskId must not be empty");
            }else {
                BaseVO baseVO = cusTaskService.getBaseVOByTaskIdOrProcessInstanceId("",taskId);
                System.out.println("任务详情：" + baseVO);
                resultMap.put("type", "success");
                resultMap.put("data", baseVO);
            }
        } catch (Exception e) {
            resultMap.put("type", "error");
            resultMap.put("msg", e.getMessage());
            e.printStackTrace();
        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }

    /**
     * 执行任务节点（通过/退回）
     *
     * @param json:{
     *            userId:'',//流程执行者id,必输<br/>
     *            userName:'',//流程执行者名称,必输<br/>
     *            workFlowTitle:'',//流程标题，可为空<br/>
     *            taskId:'',//任务id，不能为空<br/>
     *            isPass:'',//是否通过：true为通过，false为退回，注意：传了该参数就不要传reapply或end<br/>
     *            reapply:'',//是否重新申请：true为重新申请，false为取消申请（已废），注意：传了该参数就不要传isPass或end<br/>
     *            end:'',//是否归档，注意：传了该参数就不要传isPass或reapply<br/>
     *            content:'',//评论内容，可为空<br/>
     *            candidate_ids:'',//指定下一节点执行人id（多个人用逗号分隔），不需要动态指定时不传<br/>
     *            candidate_names:'',//指定下一节点执行人name，不需要动态指定时不传<br/>
     *            isChangeData:'',//true or false,下一节点执行人是否有改变,为true时不能传candidate_ids或candidate_names，否则以candidate_ids为准
     *            					//场景：下一节点执行人动态与业务挂钩时需要动态更新下一节点执行人；
     *            					//示例请看周报（项目外包/行内人员）中的重新申请节点，新增或减少项目，都会影响下一节点的执行人，此时，只用传isChangeData:'true',
     *            					//后台会获取各项目的项目经理为下一节点的执行人
     *            taskDefinitionKey:'',//当前节点key，指定下一节点执行人时必传，不需要动态指定时不传,非必输<br/>
     *            contentInfo:{},//与流程无关的业务信息<br/>
     *            comments:[],//评论列表,需要保持评论列表中的下一接收人，必输<br/>
     *            isDelegateAutoHandle:'',//委托后是否由委托人直接执行，如果为true，被委托人完成任务后，流程流向下一节点，而非原拥有者,非必输
     *            isAutoComplete:'',//是否自动执行下一节点，默认为false
     *            isAutoArchive:'',//是否自动归档，默认自动归档，传false则不会自动归档,非必输
     *             }
     */
    @RequestMapping("/handle")
    public String handleTask(@RequestBody String json, HttpServletRequest request) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            judgeParams(json,"流程执行人");

            String userId = GFJsonUtil.get().getProperty(json, "userId");
            String userName = GFJsonUtil.get().getProperty(json, "userName");
            String taskId = GFJsonUtil.get().getProperty(json, "taskId");

            BaseVO baseVO = getBaseVO(json, taskId);
            try {

                Map<String, Object> variables = new HashMap<>();

                Map<String,Object> resultMap1 = checkUpdateNextNodeAssignees(request,json, baseVO, variables);
                final String businessKey = resultMap1.get("businessKey")!=null ? resultMap1.get("businessKey").toString() : "";
                ProcessStatus processStatus = resultMap1.get("processStatus")!=null ? (ProcessStatus) resultMap1.get("processStatus") : null;

                String content = GFJsonUtil.get().getProperty(json, "content");
                String isDelegateAutoHandleStr = GFJsonUtil.get().getProperty(json, "isDelegateAutoHandle");
                boolean isDelegateAutoHandle = "true".equals(isDelegateAutoHandleStr) ? true : false;
                List<String> nextAssignes = this.cusTaskService.handleTask(taskId, userId, userName, processStatus, content, baseVO, variables, isDelegateAutoHandle);

                deleteExtraContentInfo(baseVO.getContentInfo().toString(), processStatus, businessKey, request);
                resultMap = responseData(baseVO, processStatus, nextAssignes);
            } catch (ActivitiObjectNotFoundException e) {
                deleteComment(baseVO);
                resultMap.put("msg", "此任务不存在，请联系管理员！");
                resultMap.put("type", "notFound");
            } catch (Exception e) {
                e.printStackTrace();
                deleteComment(baseVO);
                resultMap.put("msg", e.getMessage());
                resultMap.put("type", "error");
            }
        } catch (ParameterException e) {
            resultMap.put("type", "empty");
            resultMap.put("msg", e.getMessage());
        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }

    /**
     * 必输参数非空校验
     * @param json
     * @param role 角色
     * @throws ParameterException
     */
    private void judgeParams(String json,String role) throws ParameterException {
        if (json.isEmpty()) {
            throw new ParameterException("任务办理失败，必输参数为空！");
        }
        String userId = GFJsonUtil.get().getProperty(json, "userId");
        String userName = GFJsonUtil.get().getProperty(json, "userName");
        String taskId = GFJsonUtil.get().getProperty(json, "taskId");
        String errorMsg = "";
        if(StringUtil.isEmpty(taskId)){
            errorMsg = "必输参数:当前任务taskId不能为空";
        }
        if(StringUtil.isEmpty(userId)){
            errorMsg = "必输参数:"+role+"userId不能为空";
        }
        if(StringUtil.isEmpty(userName)){
            errorMsg = "必输参数:"+role+"名称userName不能为空";
        }
        if(StringUtil.isNotEmpty(errorMsg)){
            throw new ParameterException(errorMsg);
        }
    }

    /**
     * 从requestParams中获取任务是通过还是退回/重新申请/归档操作
     * @param json
     * @param baseVO
     * @param variables
     * @return
     * @throws Exception
     */
    private Map<String,Object> getCondition(String json, BaseVO baseVO, Map<String, Object> variables) throws Exception {

        String isPassStr = GFJsonUtil.get().getProperty(json, "isPass");
        String userName = GFJsonUtil.get().getProperty(json, "userName");
        String taskId = GFJsonUtil.get().getProperty(json, "taskId");
        Map<String,Object> resultMap = checkIsPassTask(isPassStr, taskId, userName, baseVO, variables);
        String condition = resultMap.get("condition")!=null ? resultMap.get("condition").toString() : "";

        if (StringUtil.isEmpty(condition)) {
            String reapplyStr = GFJsonUtil.get().getProperty(json, "reapply");
            resultMap = checkIsReapplyTask(reapplyStr, baseVO, variables);
            condition = resultMap.get("condition")!=null ? resultMap.get("condition").toString() : "";
        }
        if (StringUtil.isEmpty(condition)) {
            String isEnd = GFJsonUtil.get().getProperty(json, "end");
            String content = GFJsonUtil.get().getProperty(json, "content");
            resultMap = checkIsEndTask(isEnd,baseVO,content, variables);
        }
        return resultMap;
    }

    /**
     * 处理任务后返回参数
     * @param baseVO
     * @param handleFlag
     * @param nextAssignes
     * @return
     */
    private Map<String, Object> responseData(BaseVO baseVO, ProcessStatus handleFlag, List<String> nextAssignes) {
        Map<String, Object> resultMap = new HashMap<>();
        //多实例节点未全部通过时
        boolean notSetPreNodeInfo = (handleFlag != null && ProcessStatus.APPROVAL_SUCCESS.equals(handleFlag.toString()))
                && (!baseVO.getDescription().contains("已同意 ") && !baseVO.getDescription().contains(BaseVO.SUB_DESCRIPTION_PASS));
        if (notSetPreNodeInfo) {
            resultMap.put("msg", "已审核！");
        } else {
            resultMap.put("msg", "当前任务执行完毕");
        }

        List<Task> toDotaskList = this.cusTaskService.getTaskByProcessInstanceId(baseVO.getProcessInstanceId());// 获取该流程的待办任务,可能是多实例的会签点，可能有多个执行人多个任务

        List<Map<String, String>> listTask = new ArrayList<>();
        List<CommentVO> commentList = new ArrayList<>();
        if (toDotaskList.size() > 0) {
            Map<String, String> taskInfo = new HashMap<>();
            for (Task task : toDotaskList) {
                taskInfo.put("taskId", task.getId());
                taskInfo.put("taskDefinitionKey", task.getTaskDefinitionKey());
                taskInfo.put("taskName", task.getName());
                listTask.add(taskInfo);
            }
            commentList = this.cusTaskService.getCommentList(baseVO);
        }
        resultMap.put("type", "success");
        resultMap.put("nextAssignes", nextAssignes);
        resultMap.put("listTask", listTask);
        resultMap.put("comments", commentList);
        return resultMap;
    }

	/**
	 * 删除该条评论
	 * @param baseVO
	 */
	private void deleteComment(BaseVO baseVO) {
		try {
			List<CommentVO> commentList = this.cusTaskService.getComments(baseVO.getProcessInstanceId());
			if(null != commentList && commentList.size()>0) {
				this.cusTaskService.deleteComment(commentList.get(0).getId());
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * 从请求参数中获取最新的实体对象
	 * @param params
	 * @param taskId
	 * @return
	 */
	private BaseVO getBaseVO(String params,String taskId) {
        String userName = GFJsonUtil.get().getProperty(params, "userName");
		String candidate_ids = GFJsonUtil.get().getProperty(params, "candidate_ids");
		String candidate_names = GFJsonUtil.get().getProperty(params, "candidate_names");
		String currentTaskActivitiId = GFJsonUtil.get().getProperty(params, "taskDefinitionKey");
		final JSONObject contentInfo = GFJsonUtil.get().getJSONObject(params,"contentInfo");
		final JSONArray comments = GFJsonUtil.get().getJSONArray(params,"comments");
		final String workFlowTitle = GFJsonUtil.get().getProperty(params,"workFlowTitle");

        BaseVO baseVO = this.cusTaskService.getBaseVOByTaskIdOrProcessInstanceId(userName,taskId);
		baseVO = null == baseVO ? new BaseVO() : baseVO;
		
		if(StringUtil.isNotEmpty(currentTaskActivitiId)) {
			baseVO.setTaskDefinitionKey(currentTaskActivitiId);// 当前节点key
		}
		if(StringUtil.isNotEmpty(workFlowTitle)){
			baseVO.setTitle(workFlowTitle);
		}
		baseVO.setCandidate_ids(candidate_ids);// 动态指定下一节点执行人id，多个人用逗号分隔
		baseVO.setCandidate_names(candidate_names);// 动态指定下一节点执行人name

		baseVO.setAssign(candidate_ids);
		baseVO.setAssignName(candidate_names);

		if(contentInfo != null) {
			baseVO.setContentInfo(contentInfo);
		}
        if(comments != null) {
            List<CommentVO> commentsRequest = this.processControllder.getCommentListFromJson(comments);
            baseVO.setComments(commentsRequest);
        }
        return baseVO;
    }

    /**
     * 流程取消后删除除流程实例外的信息，这样可以重新启动该流程
     * @param contentInfo 业务内容
     * @param handleFlag 流程处理类型
     * @param businessKey 业务key
     * @throws Exception
     */
    private void deleteExtraContentInfo(final String contentInfo, ProcessStatus handleFlag,
			final String businessKey,HttpServletRequest request) throws Exception {
        if (ProcessStatus.CANCEL.equals(handleFlag.toString())) {
			final String contentInfoId = GFJsonUtil.get().getProperty(contentInfo, "contentInfoId");
			processControllder.deleteProcessInstance(null, businessKey, contentInfoId, "",true,request);
		}
	}

    /**
     * 是否审核通过
     *
     * @param isPass
     * @param taskId
     * @param userName
     * @param baseVO
     * @param variables
     * @throws Exception
     */
    private Map<String,Object> checkIsPassTask(String isPass,String taskId,String userName, BaseVO baseVO,
                                               Map<String, Object> variables) throws Exception {
        ProcessStatus processStatus = null;
        if (StringUtil.isNotEmpty(isPass)) {
            final String variableKey = new StringBuilder().append("isPass_").append(baseVO.getTaskDefinitionKey()).toString();
            variables.put(variableKey, isPass);

            final String workFloWTitle = StringUtil.isEmpty(baseVO.getTitle()) ? "请求" : baseVO.getTitle();
            String description;
            if ("false".equals(isPass)) {
                setBackMembers(variables, baseVO);
                processStatus = ProcessStatus.APPROVAL_FAILED;
                description = new StringBuilder().append(baseVO.getCreateName())
                        .append(" 的")
                        .append(workFloWTitle)
                        .append("被")
                        .append(userName)
                        .append("驳回,需修改后重新提交！")
                        .toString();
            } else {
                setAgreeMembers(variables, baseVO.getBusinessKey(),baseVO.getProcessInstanceId(),baseVO.getTaskDefinitionKey());
                processStatus = ProcessStatus.APPROVAL_SUCCESS;
                Task currentTask = this.cusTaskService.getTaskByTaskId(taskId);
                int totalMembers = getMember("nrOfInstances", currentTask.getExecutionId());
                int agreeMembers = getAgreeMember(baseVO);
                if ((totalMembers == 0) || ((totalMembers - agreeMembers) == 1)) {
                    String nextActivitiId = ProcessDefinitionCache.get().getNextActivitiId(baseVO,isPass);
                    if(ProcessDefinitionCache.ARCHIVE.equals(nextActivitiId)) {//到归档前一个节点才算真正通过
                        description = new StringBuilder().append(baseVO.getCreateName())
                                .append(" 的")
                                .append(workFloWTitle)
                                .append(BaseVO.SUB_DESCRIPTION_PASS)
                                .toString();
                    }else {
                        description = new StringBuilder().append(userName)
                                .append(" 已同意 ")
                                .append(baseVO.getCreateName())
                                .append("的")
                                .append(workFloWTitle)
                                .toString();
                    }
                } else {
                    description = new StringBuilder().append(userName)
                            .append(" 已审核 ")
                            .append(baseVO.getCreateName())
                            .append("的")
                            .append(workFloWTitle)
                            .toString();
                }
            }
            baseVO.setProcessStatus(processStatus);
            baseVO.setDescription(description);
        }
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("condition",isPass);
        resultMap.put("processStatus",processStatus);
        return resultMap;
    }

	/**
	 * 获取同意人数
	 * @param baseVO
	 * @return
	 */
	private int getAgreeMember(BaseVO baseVO) {
		int agreeMembers = 0;
		agreeMembers = getMember("agreeMembers"+"_"+baseVO.getTaskDefinitionKey(), baseVO.getProcessInstanceId());
		return agreeMembers;
	}

	/**
	 * 设置驳回人数
	 * 
	 * @param variables
	 * @param baseVO
	 * @throws Exception 
	 */
	private void setBackMembers(Map<String, Object> variables, BaseVO baseVO) throws Exception {

		final String processInstanceId = baseVO.getProcessInstanceId();
		final String taskDefinitionKey = baseVO.getTaskDefinitionKey();
		final String businessKey = baseVO.getBusinessKey();
		CusUserTask userTask = cusUserTaskService.findByProcAndTask(businessKey, taskDefinitionKey);
		if(CusUserTask.TYPE_MULTI.equals(userTask.getActivityType())) {
			setMemberToVariable("backMembers"+"_"+taskDefinitionKey, variables, processInstanceId);
		}
	}

	/**
	 * 设置同意人数
	 * 
	 * @param variables
	 * @param businessKey
	 * @param processInstanceId
	 * @param taskDefinitionKey 
	 * @throws Exception 
	 */
	private void setAgreeMembers(Map<String, Object> variables,String businessKey, String processInstanceId, String taskDefinitionKey) throws Exception {
		CusUserTask userTask = cusUserTaskService.findByProcAndTask(businessKey, taskDefinitionKey);
		if(CusUserTask.TYPE_MULTI.equals(userTask.getActivityType())) {
			setMemberToVariable("agreeMembers"+"_"+taskDefinitionKey, variables, processInstanceId);
		}
	}

	private void setMemberToVariable(String variableKey, Map<String, Object> variables, String processInstanceId) {
		int agreeMembers = getMember(variableKey, processInstanceId);
		variables.put(variableKey, agreeMembers + 1);
	}

	private int getMember(String variableKey, String processInstanceId) {
		Object obj = processService.getRunVariable(variableKey, processInstanceId);
		int members = obj == null ? 0 : (int) obj;
		return members;
	}

    /**
     * 是否重新申请
     *
     * @param isReapply
     * @param baseVO
     * @param variables
     */
    private Map<String,Object> checkIsReapplyTask(String isReapply, BaseVO baseVO, Map<String, Object> variables) {
        ProcessStatus processStatus = null;
        if (StringUtil.isNotEmpty(isReapply)) {
            final String variableKey = new StringBuilder().append("reapply_").append(baseVO.getTaskDefinitionKey()).toString();
            variables.put(variableKey, isReapply);

            final String workFloWTitle = StringUtil.isEmpty(baseVO.getTitle()) ? "请求" : baseVO.getTitle();
            String description;
            if ("true".equals(isReapply)) {
                processStatus = ProcessStatus.WAITING_FOR_APPROVAL;
                description = new StringBuilder().append(baseVO.getCreateName())
                        .append(" 的")
                        .append(workFloWTitle)
                        .append("已重新调整")
                        .toString();
            } else {
                processStatus = ProcessStatus.CANCEL;
                description = new StringBuilder().append(baseVO.getCreateName())
                        .append(" 的")
                        .append(workFloWTitle)
                        .append("已取消")
                        .toString();
            }
            baseVO.setProcessStatus(processStatus);
            baseVO.setDescription(description);
        }
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("condition",isReapply);
        resultMap.put("processStatus",processStatus);
        return resultMap;
    }

    /**
     * 是否结束流程
     *
     * @param isEnd
     * @param baseVO
     * @param content 意见
     * @param variables
     */
    private Map<String,Object> checkIsEndTask(String isEnd, BaseVO baseVO,String content,
                                              Map<String, Object> variables) throws Exception {
        ProcessStatus processStatus = null;
        if (StringUtil.isNotEmpty(isEnd) && "true".equals(isEnd)) {
            final String taskDefinitionKey = baseVO.getTaskDefinitionKey();
            final String businessKey = baseVO.getBusinessKey();
            resetMembers(variables, businessKey,taskDefinitionKey);
            processStatus = ProcessStatus.ARCHIVE;
            baseVO.setProcessStatus(processStatus);

            final String workFloWTitle = StringUtil.isEmpty(baseVO.getTitle()) ? "请求" : baseVO.getTitle();

            final String description1 = new StringBuilder().append(baseVO.getCreateName())
                    .append(" 的")
                    .append(workFloWTitle)
                    .append("已归档")
                    .toString();
            final String description2 = new StringBuilder().append(content).append(",").append(baseVO.getCreateName())
                    .append(" 的")
                    .append(workFloWTitle)
                    .append("已归档")
                    .toString();
            final String description = StringUtil.isEmpty(content) ? description1 : description2;
            baseVO.setDescription(description);
        }
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("condition",isEnd);
        resultMap.put("processStatus",processStatus);
        return resultMap;
    }

    /**
     * 判断下一节点是否更新执行人和是否需要自动执行
     * @param request
     * @param json
     * @param baseVO
     * @param variables 流程变量
     * @return
     * @throws Exception
     */
    private Map<String,Object> checkUpdateNextNodeAssignees(HttpServletRequest request,String json,  BaseVO baseVO,Map<String, Object> variables) throws Exception {
        final String businessKey = baseVO.getBusinessKey();

        Map<String,Object> resultMap = getCondition(json, baseVO, variables);//网关的判断条件值，true或false
        String condition = resultMap.get("condition")!=null ? resultMap.get("condition").toString() : "";
        ProcessStatus processStatus = resultMap.get("processStatus")!=null ? (ProcessStatus) resultMap.get("processStatus") : null;

        String nextActivitiId = ProcessDefinitionCache.get().getNextActivitiId(baseVO,condition);
        checkIsAutoCompleteNextActiviti(request, json, baseVO, variables, businessKey, nextActivitiId);

        if("false".equals(condition)) {//退回
            baseVO.setCandidate_ids("");
            baseVO.setCandidate_names("");
        }
        final String isChangeDataStr = GFJsonUtil.get().getProperty(json,"isChangeData");//下一节点执行人是否有改变
        boolean isChangeData = "true".equals(isChangeDataStr) ? true : false;//数据改变了更新下一节点执行人

        updateNextTaskAssigness(baseVO, variables, businessKey, nextActivitiId, isChangeData);

        Map<String,Object> resultMap1 = new HashMap<>();
        resultMap1.put("businessKey",businessKey);
        resultMap1.put("processStatus",processStatus);
        return resultMap1;
    }

    /**
     * 判断是否自动执行下一节点
     * @param request
     * @param json
     * @param baseVO
     * @param variables
     * @param businessKey
     * @param nextActivitiId
     * @throws Exception
     */
    private void checkIsAutoCompleteNextActiviti(HttpServletRequest request, String json, BaseVO baseVO, Map<String, Object> variables, String businessKey, String nextActivitiId) throws Exception {
        String isAutoCompleteStr = GFJsonUtil.get().getProperty(json, "isAutoComplete");
        boolean isAutoComplete = "true".equals(isAutoCompleteStr) ? true : false;//是否自动执行下一节点，默认为false

        String isAutoArchiveStr = GFJsonUtil.get().getProperty(json, "isAutoArchive");
        boolean isAutoArchive = "false".equals(isAutoArchiveStr) ? false : true;//是否自动归档，默认自动归档，传false则不会自动归档

        final String description = baseVO.getDescription();
        boolean isAutoCompleteNextActiviti = false;
        boolean isArchiveNode = ProcessDefinitionCache.ARCHIVE.equals(nextActivitiId)&& description.contains(BaseVO.SUB_DESCRIPTION_PASS) && isAutoArchive;
        if(isArchiveNode || isAutoComplete) {
            variables.put("autoComplete", true);
            if(isAutoComplete && !isArchiveNode) {
                cusUserTaskService.autoPass(variables,baseVO.getProcessInstanceId(), nextActivitiId);
            }
            if(isAutoArchive) {
                isAutoCompleteNextActiviti = true;
            }else {
                isAutoCompleteNextActiviti = false;
            }
        }

        final String procDefKey = businessKey.contains(":") ? businessKey.split(":")[0] : "";

        JSONObject jsonObject = baseVO.getContentInfo();
        if (jsonObject != null) {
            this.processControllder.updateContentInfo(request, jsonObject, procDefKey, isAutoCompleteNextActiviti);//先更新业务信息，再变更各节点的执行人
        }
    }

    /**
     * 更新下一节点执行人：更新act_cus_user_task表
     * @param baseVO
     * @param variables
     * @param businessKey
     * @param nextActivitiId
     * @param isChangeData
     * @throws Exception
     */
    private void updateNextTaskAssigness(BaseVO baseVO, Map<String, Object> variables, String businessKey, String nextActivitiId, boolean isChangeData) throws Exception {
        this.processControllder.updateNextCusUserTaskAssigness(baseVO, nextActivitiId, isChangeData);

        CusUserTask userTask = cusUserTaskService.findByProcAndTask(businessKey, nextActivitiId);

        if(null != userTask && CusUserTask.TYPE_MULTI.equals(userTask.getActivityType())) {
            String candidate_ids = userTask.getCandidate_ids();
            String[] candidate_users = candidate_ids.split(",");
            List<String> assigneeList = new ArrayList<>();
            if (null != candidate_users) {
                assigneeList = Arrays.asList(candidate_users);
            }
            if (assigneeList.size() < 1) {//节点的执行人为空时自动跳过该节点
                cusUserTaskService.autoPass(variables,baseVO.getProcessInstanceId(), nextActivitiId);
            }

            variables.put("assigneeList" + "_" + nextActivitiId, assigneeList);
            resetMembers(variables, businessKey,nextActivitiId);
        }
    }


    /**
     * 重置多实例任务节点同意人数或驳回人数流程变量
     * @param variables
     * @param businessKey
     * @param taskDefinitionKey
     * @throws Exception
     */
    private void resetMembers(Map<String, Object> variables, String businessKey, String taskDefinitionKey) throws Exception {
        variables.put("backMembers"+"_"+taskDefinitionKey, 0);
        variables.put("agreeMembers"+"_"+taskDefinitionKey, 0);
    }

    /**
     * 更新节点执行人 <br/>
     * @param json:{
     *            taskId:'',//需要更新执行人的节点任务id，不能为空<br/>
     * 			  businessKey:'',//业务key,不能为空<br/>
     * 			  taskDefinitionKey:'',//需要更新执行人的节点key,不能为空<br/>
     *            deleteUserId:'',//被删除的userId,可为空<br/>
     *            candidate_ids:'',//更新的执行人id,多个用逗号分隔，不能为空<br/>
     *            candidate_names:'',//更新的执行人name,多个用逗号分隔，不能为空<br/>
     *            }
     * @return
     */
    @RequestMapping("/updateTaskAssigness")
    public String updateTaskAssigness(@RequestBody String json) {

        Map<String, Object> resultMap = new HashMap<>();
        try {
            judgeParamsOfUpdateTaskAssigness(json);
            String taskId = GFJsonUtil.get().getProperty(json, "taskId");
            try {
                String businessKey = GFJsonUtil.get().getProperty(json, "businessKey");
                BaseVO baseVO = getBaseVO(json,taskId);

                String userIds = baseVO.getCandidate_ids();
                if (StringUtil.isNotEmpty(userIds)) {
                    Task task = cusTaskService.getTaskByTaskId(taskId);
                    if(task != null && task.getAssignee() != null){//普通节点
                        this.cusTaskService.setAssigness(taskId, userIds);
                    }else {//会签节点
                        String deleteUserId = GFJsonUtil.get().getProperty(json, "deleteUserId");
                        if (StringUtil.isNotEmpty(deleteUserId)) {
                            this.cusTaskService.deleteCandidateUser(taskId, deleteUserId);
                        }
                        this.cusTaskService.addCandidateUsers(taskId, userIds);
                    }

                    //更新act_cus_user_task表
                    Map<String, Object> variables = new HashMap<>();
                    updateNextTaskAssigness(baseVO, variables, businessKey, baseVO.getTaskDefinitionKey(), false);
                    resultMap.put("type", "success");
                    resultMap.put("msg", "任务的执行人更新成功！");
                }else {
                    resultMap.put("type", "empty");
                    resultMap.put("msg", "必输参数:执行人candidate_ids不能为空");
                }
            } catch (ActivitiObjectNotFoundException e) {
                resultMap.put("type", "notFound");
                resultMap.put("msg", "此任务不存在！任务执行人更新失败！");
            } catch (Exception e) {
                resultMap.put("type", "error");
                resultMap.put("msg", "任务执行人更新失败！请联系管理员！");
            }
        } catch (ParameterException e) {
            resultMap.put("type", "empty");
            resultMap.put("msg", e.getMessage());
        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }


    /**
     * 必输参数非空校验
     * @param json
     * @throws ParameterException
     */
    private void judgeParamsOfUpdateTaskAssigness(String json) throws ParameterException {
        if (json.isEmpty()) {
            throw new ParameterException("任务执行人更新失败，必输参数为空！");
        }
        String candidate_ids = GFJsonUtil.get().getProperty(json, "candidate_ids");
        String candidate_names = GFJsonUtil.get().getProperty(json, "candidate_names");
        String taskDefinitionKey = GFJsonUtil.get().getProperty(json, "taskDefinitionKey");
        String taskId = GFJsonUtil.get().getProperty(json, "taskId");
        String businessKey = GFJsonUtil.get().getProperty(json, "businessKey");
        String errorMsg = "";
        if(StringUtil.isEmpty(taskId)){
            errorMsg = "必输参数:需要更新执行人的节点任务taskId不能为空";
        }
        if(StringUtil.isEmpty(candidate_ids)){
            errorMsg = "必输参数:执行人candidate_ids不能为空";
        }
        if(StringUtil.isEmpty(candidate_names)){
            errorMsg = "必输参数:执行人名称candidate_names不能为空";
        }
        if(StringUtil.isEmpty(taskDefinitionKey)){
            errorMsg = "必输参数:需要更新执行人的节点key不能为空";
        }
        if(StringUtil.isEmpty(businessKey)){
            errorMsg = "必输参数:流程的businessKey不能为空";
        }
        if(StringUtil.isNotEmpty(errorMsg)){
            throw new ParameterException(errorMsg);
        }
    }


    /**
     * userId认领任务 <br/>
     * task.setAssignee(userId)<br/>
     *
     * @param json:{
     * 			  userId:'',//认领人id,不能为空<br/>
     * 			  userName:'',//认领人名称,不能为空<br/>
     *            taskId:'',//任务id，不能为空<br/>
     *            content:'',//评论内容，可为空<br/>
     *            contentInfo:{},//业务内容，如果需要更新页面的业务信息，则传该参数，否则可不传<br/>
     *            comments:[],//评论列表,需要保持评论列表中的下一接收人
     *            }
     * @return
     */
    @RequestMapping("/claim")
    public String claim(@RequestBody String json) {

        Map<String, Object> resultMap = new HashMap<>();
        try {
            judgeParams(json,"认领人");

            String taskId = GFJsonUtil.get().getProperty(json, "taskId");
            String userId = GFJsonUtil.get().getProperty(json, "userId");
            String userName = GFJsonUtil.get().getProperty(json, "userName");

            try {
                String msg = GFJsonUtil.get().getProperty(json, "content");
                boolean hasComments = GFJsonUtil.get().containsKey(json, "comments");
                BaseVO baseVO = null;
                if(hasComments) {
                    baseVO = getBaseVO(json,taskId);
                }
                this.cusTaskService.claim(taskId, userId,userName,msg,hasComments,baseVO);
                resultMap.put("type", "success");
                resultMap.put("msg", "任务认领成功！");
            } catch (ActivitiObjectNotFoundException e) {

                resultMap.put("type", "notFound");
                resultMap.put("msg", "此任务不存在！任务认领失败！");
            } catch (ActivitiTaskAlreadyClaimedException e) {
                resultMap.put("type", "claimed");
                resultMap.put("msg", "此任务已被其他成员认领！请刷新页面重新查看！");
            } catch (Exception e) {
                resultMap.put("type", "error");
                resultMap.put("msg", "任务认领失败！请联系管理员！");
            }
        } catch (ParameterException e) {
            resultMap.put("type", "empty");
            resultMap.put("msg", e.getMessage());
        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }

    /**
     * 委托任务给userId，<br/>
     * 委托操作不会出现在已办事项里
     * task.setOwner(task.getAssignee());<br/>
     * task.setAssignee(userId)
     *
     * @param json:{
     * 			  fromUserId:'',//委托人id，不能为空<br/>
     *            toUserId:'',//委托的接收人id，不能为空<br/>
     *            taskId:'',//任务id，不能为空<br/>
     *            content:'',//评论内容，可为空<br/>
     *            contentInfo:{},//业务内容，页面反显，非必输<br/>
     *            comments:[],//评论列表,需要保持评论列表中的下一接收人
     *            }
     *
     * @return
     */
    @RequestMapping("/delegate")
    public String delegateTask(@RequestBody String json) {

        Map<String, Object> resultMap = new HashMap<>();
        try {
            judgeParams(json);

            String taskId = GFJsonUtil.get().getProperty(json, "taskId");
            String fromUserId = GFJsonUtil.get().getProperty(json, "fromUserId");
            String toUserId = GFJsonUtil.get().getProperty(json, "toUserId");
            String msg = GFJsonUtil.get().getProperty(json, "content");
            try {
                Map<String, Object> variables = new HashMap<>();
                boolean hasComments = GFJsonUtil.get().containsKey(json, "comments");
                if(hasComments) {
                    BaseVO baseVO = getBaseVO(json,taskId);
                    if(ProcessStatus.TASK_CLAIMED.equals(baseVO.getProcessStatus())) {
                        baseVO.setProcessStatus(ProcessStatus.APPROVAL_SUCCESS);
                        String description = baseVO.getDescription();
                        description = description.replaceAll("认领", "审核");
                        baseVO.setDescription(description);
                    }
                    variables.put("entity", baseVO);
                }

                this.cusTaskService.delegateTask(taskId, fromUserId,toUserId,msg,variables);

                resultMap.put("type", "success");
                resultMap.put("msg", "任务委托成功！");
            } catch (ActivitiObjectNotFoundException e) {
                e.printStackTrace();
                resultMap.put("type", "empty");
                resultMap.put("msg", e);
            } catch (Exception e) {
                e.printStackTrace();
                resultMap.put("type", "error");
                resultMap.put("msg", e);
            }
        } catch (ParameterException e) {
            resultMap.put("type", "empty");
            resultMap.put("msg", e.getMessage());
        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }

    /**
     * 必输参数非空校验
     * @param json
     * @throws ParameterException
     */
    private void judgeParams(String json) throws ParameterException {
        if (json.isEmpty()) {
            throw new ParameterException("任务办理失败，必输参数为空！");
        }

        String taskId = GFJsonUtil.get().getProperty(json, "taskId");
        String fromUserId = GFJsonUtil.get().getProperty(json, "fromUserId");
        String toUserId = GFJsonUtil.get().getProperty(json, "toUserId");

        String errorMsg = "";
        if(StringUtil.isEmpty(taskId)){
            errorMsg = "必输参数:当前任务号taskId不能为空";
        }
        if(StringUtil.isEmpty(fromUserId)){
            errorMsg = "必输参数:委托人fromUserId不能为空";
        }
        if(StringUtil.isEmpty(toUserId)){
            errorMsg = "必输参数:委托的接收人toUserId不能为空";
        }

        if(StringUtil.isNotEmpty(errorMsg)){
            throw new ParameterException(errorMsg);
        }
    }


    /**
     * 撤销任务<br/>
     * 	  userId:撤回的执行人id
     backToTaskId:撤回的目标任务号taskId,<br/>
     backToActivitiType:撤回的目标节点类型：M:多实例节点,N:普通用户节点,<br/>
     backFromTaskId:被撤回的任务id,<br/>
     backFromActivitiType:被撤回的节点类型：M:多实例节点,N:普通用户节点,<br/>
     *
     * @return
     * @throws Exception
     */
    @RequestMapping("/revoke")
    public String revoke(@RequestParam("userId") String userId,
                         @RequestParam("backToTaskId") String backToTaskId,
                         @RequestParam("backToActivitiType") String backToActivitiType,
                         @RequestParam("backFromTaskId") String backFromTaskId,
                         @RequestParam("backFromActivitiType") String backFromActivitiType) {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            judgeParams(userId,backToTaskId,backToActivitiType,backFromTaskId,backFromActivitiType);

            Integer revokeFlag = this.cusTaskService.revoke(userId,backToTaskId,backToActivitiType, backFromTaskId,backFromActivitiType);
            if (revokeFlag == 0) {
                resultMap.put("type", "success");
                resultMap.put("msg", "撤销任务成功！");
            } else if (revokeFlag == 1) {
                resultMap.put("type", "error");
                resultMap.put("msg", "撤销任务失败 - [ 此审批流程已结束! ]");
            } else if (revokeFlag == 2) {
                resultMap.put("type", "error");
                resultMap.put("msg", "撤销任务失败 - [ 该任务非当前用户提交，无法撤回! ]");
            } else if (revokeFlag == 3) {
                resultMap.put("type", "error");
                resultMap.put("msg", "撤销任务失败 - [ 下一结点已经通过,不能撤销! ]");
            } else if (revokeFlag == -1) {
                resultMap.put("type", "error");
                resultMap.put("msg", "撤销任务失败 - [ 未找到目标任务，无法撤回! ]");
            }else {
                resultMap.put("type", "error");
                resultMap.put("msg", "撤销任务失败 - [ 内部错误！ ]");
            }
        } catch (ParameterException e) {
            resultMap.put("type", "empty");
            resultMap.put("msg", e.getMessage());
        } catch (Exception e) {
            resultMap.put("type", "error");
            resultMap.put("msg", "撤销任务失败 - [ 内部错误！ ]");
            e.printStackTrace();
        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }

    /**
     * 必输参数非空校验
     * @param userId
     * @param backToTaskId
     * @param backToActivitiType
     * @param backFromTaskId
     * @param backFromActivitiType
     * @throws ParameterException
     */
    private void judgeParams(String userId, String backToTaskId, String backToActivitiType, String backFromTaskId, String backFromActivitiType) throws ParameterException {
        String errorMsg = "";
        if(StringUtil.isEmpty(userId)){
            errorMsg = "必输参数:撤回的执行人userId不能为空";
        }
        if(StringUtil.isEmpty(backToTaskId)){
            errorMsg = "必输参数:撤回的目标任务号backToTaskId不能为空";
        }
        if(StringUtil.isEmpty(backToActivitiType)){
            errorMsg = "必输参数:撤回的目标节点类型backToActivitiType不能为空";
        }
        if(StringUtil.isEmpty(backFromTaskId)){
            errorMsg = "必输参数:被撤回的任务号backFromTaskId不能为空";
        }
        if(StringUtil.isEmpty(backFromActivitiType)){
            errorMsg = "必输参数:被撤回的节点类型backFromActivitiType不能为空";
        }
        if(StringUtil.isNotEmpty(errorMsg)){
            throw new ParameterException(errorMsg);
        }
    }

    /**
     * 退回到指定节点
     * @param json:{
     * 			  fromUserId:'',//处理人id，不能为空<br/>
     *            processInstanceId：''，//流程实例id，不能为空<br/>
     *            taskId:'',//任务id，不能为空<br/>
     *            destTaskKey:'',//退回至目标节点Key，不能为空<br/>
     *            processInstanceId:'',//流程实例id，不能为空<br/>
     *            content:'',//评论内容，可为空<br/>
     *            contentInfo:{},//业务内容，页面反显，非必输<br/>
     *            comments:[],//评论列表,需要保持评论列表中的下一接收人
     *            }
     * @return
     */
    @RequestMapping("/backTo")
    public String backTo(@RequestBody String json) {

        Map<String, Object> resultMap = new HashMap<>();
        try {
            judgeBackToParams(json);

            String taskId = GFJsonUtil.get().getProperty(json, "taskId");
            String processInstanceId = GFJsonUtil.get().getProperty(json, "processInstanceId");
            String fromUserId = GFJsonUtil.get().getProperty(json, "fromUserId");
            String fromUserName = GFJsonUtil.get().getProperty(json, "fromUserName");
            String destTaskKey = GFJsonUtil.get().getProperty(json, "destTaskKey");

            String msg = GFJsonUtil.get().getProperty(json, "content");
            Map<String, Object> variables = new HashMap<>();
            boolean hasComments = GFJsonUtil.get().containsKey(json, "comments");
            if (hasComments) {
                BaseVO baseVO = getBaseVO(json, taskId);
                baseVO.setProcessStatus(ProcessStatus.APPROVAL_FAILED);
                final String workFloWTitle = StringUtil.isEmpty(baseVO.getTitle()) ? "请求" : baseVO.getTitle();
                final String description = new StringBuilder().append(baseVO.getCreateName())
                        .append(" 的")
                        .append(workFloWTitle)
                        .append("被")
                        .append(fromUserName)
                        .append("驳回,需修改后重新提交！")
                        .toString();
                baseVO.setDescription(description);
                variables.put("entity", baseVO);
            }
            List<Task> currentTasklist = this.cusTaskService.getTaskByProcessInstanceId(processInstanceId);
            if(currentTasklist != null && currentTasklist.size()>0){
                final String currentTaskKey = currentTasklist.get(0).getTaskDefinitionKey();
                if(destTaskKey.equals(currentTaskKey)){
                    resultMap.put("type", "error");
                    resultMap.put("msg", "该流程正处于 "+currentTasklist.get(0).getName()+" 节点，无需退回！");
                }else{
                    this.cusTaskService.rollBackToAssignActivitiKey(taskId, fromUserId, destTaskKey, msg, variables);
                    resultMap.put("type", "success");
                    resultMap.put("msg", "任务退回成功！");
                }
            }else{
                resultMap.put("type", "error");
                resultMap.put("msg", "退回失败，流程"+processInstanceId+"不存在或已归档！");
            }
        } catch (ParameterException e) {
            resultMap.put("type", "empty");
            resultMap.put("msg", e.getMessage());
        } catch (Exception e) {
            resultMap.put("type", "error");
            resultMap.put("msg", e.getMessage());
            e.printStackTrace();
        }
        String resultJson = GFJsonUtil.get().toJson(resultMap);
        return resultJson;
    }


    /**
     * 必输参数非空校验
     * @param json
     * @throws ParameterException
     */
    private void judgeBackToParams(String json) throws ParameterException {
        if (json.isEmpty()) {
            throw new ParameterException("任务办理失败，必输参数为空！");
        }

        String taskId = GFJsonUtil.get().getProperty(json, "taskId");
        String processInstanceId = GFJsonUtil.get().getProperty(json, "processInstanceId");
        String fromUserId = GFJsonUtil.get().getProperty(json, "fromUserId");
        String fromUserName = GFJsonUtil.get().getProperty(json, "fromUserName");
        String destTaskKey = GFJsonUtil.get().getProperty(json, "destTaskKey");
        String errorMsg = "";
        if(StringUtil.isEmpty(processInstanceId)){
            errorMsg = "必输参数:当前流程实例processInstanceId不能为空";
        }
        if(StringUtil.isEmpty(taskId)){
            errorMsg = "必输参数:当前任务号taskId不能为空";
        }
        if(StringUtil.isEmpty(fromUserId)){
            errorMsg = "必输参数:当前任务处理人fromUserId不能为空";
        }
        if(StringUtil.isEmpty(fromUserName)){
            errorMsg = "必输参数:当前任务处理人fromUserName不能为空";
        }
        if(StringUtil.isEmpty(destTaskKey)){
            errorMsg = "必输参数:退回的目标节点destTaskKey不能为空";
        }
        if(StringUtil.isNotEmpty(errorMsg)){
            throw new ParameterException(errorMsg);
        }
    }

}
