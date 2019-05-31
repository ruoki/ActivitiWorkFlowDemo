package com.zr.workflow.activiti.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import com.zr.workflow.activiti.entity.BaseVO;
import com.zr.workflow.activiti.entity.Page;
import com.zr.workflow.activiti.util.ProcessDefinitionCache;
import com.zr.workflow.activiti.util.StringUtil;


/**
 * 流程处理service
 * @author zhourq
 *
 */
@Service
public class ProcessService{
	@Resource
	private RepositoryService repositoryService;
	@Resource
	private RuntimeService runtimeService;
	@Resource
	private HistoryService historyService;
	@Resource
	private CusTaskService cusTaskService;

	/**
	 * 查询已部署的流程定义列表
	 */
	public List<ProcessDefinition> findDeployedProcessList() {
		List<ProcessDefinition> processList = repositoryService.createProcessDefinitionQuery()
				.orderByProcessDefinitionVersion().asc()
				.list();
		return processList;
	}
	
	/**
	 * 查询已部署的最新版本（该版本指的是流程模型的版本）流程定义列表
	 */
	public List<ProcessDefinition> findLastetDeployedProcessList() {
		List<ProcessDefinition> list = findDeployedProcessList();
		// 定义有序map，相同的key,添加map值后，后面的会覆盖前面的值
		Map<String, ProcessDefinition> map = new LinkedHashMap<String, ProcessDefinition>();
		// 遍历相同的key，替换最新的值
		for (ProcessDefinition pd : list) {
			map.put(pd.getKey(), pd);
		}
		List<ProcessDefinition> linkedList = new LinkedList<ProcessDefinition>(map.values());
		
		return linkedList;
	}

	/**
	 * 根据流程定义id查询已部署的流程定义对象
	 *
	 * @param processDefinitionId
	 *            流程定义ID
	 * @return
	 */
	public ProcessDefinition findProcessDefinitionById(String processDefinitionId) {
		ProcessDefinition processDefinition = this.repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(processDefinitionId).singleResult();
		return processDefinition;
	}

	/**
	 * 根据流程定义id查询已部署的流程定义对象
	 *
	 * @param processDefKey
	 *            流程定义ID
	 * @return
	 */
	public ProcessDefinition findProcessDefinitionByKey(String processDefKey) {
		List<ProcessDefinition> proceDefList = this.repositoryService.createProcessDefinitionQuery()
				.processDefinitionKey(processDefKey).orderByProcessDefinitionVersion().desc().list();
		ProcessDefinition processDefinition = null == proceDefList || proceDefList.size() < 1 ? null : proceDefList.get(0);
		return processDefinition;
	}

	/**
	 * 根据流程部署id查询已部署的流程定义对象
	 *
	 * @param deploymentId
	 *            部署id
	 * @return
	 */
	public ProcessDefinition findProcessDefinitionByDeploymentId(String deploymentId) {
		ProcessDefinition processDefinition = this.repositoryService.createProcessDefinitionQuery()
				.deploymentId(deploymentId).singleResult();
		return processDefinition;
	}


	public void deleteDeployedProcess(String deploymentId) throws Exception {
		// 普通删除，如果当前规则下有正在执行的流程，则抛出异常
		// repositoryService.deleteDeployment(deploymentId);
		// 级联删除，会删除和当前规则相关的所有信息，正在执行的信息，也包括历史信息
		repositoryService.deleteDeployment(deploymentId, true);
	}

	/**
	 * 启动流程
	 * @param baseVO
	 * @param variables
	 * @return
	 * @throws Exception
	 */
	public ProcessInstance startWorkFlow(BaseVO baseVO, Map<String, Object> variables) throws Exception {

		final String businesskey = baseVO.getBusinessKey();// 设置业务key
		final String procDefKey = businesskey.split("\\:")[0];
		// 根据流程定义KEY查询最新版本的流程定义
		ProcessDefinitionEntity procDef = (ProcessDefinitionEntity) findProcessDefinitionByKey(procDefKey);
		if (procDef == null) {
			throw new RuntimeException("流程定义KEY为" + procDefKey + "流程定义未找到，请重新发布");
		}
		Authentication.setAuthenticatedUserId(baseVO.getCreateId());// 设置流程的发起人start_userId

		ProcessInstance instance = runtimeService.startProcessInstanceByKey(procDefKey, businesskey, variables);

		return instance;
	}

	/**
	 * 根据流程实例id查询该流程实例对象
	 * 
	 * @param processInstanceId
	 * @return
	 */
	public ProcessInstance getProcessInstanceById(String processInstanceId) {
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		return processInstance;

	}

	/**
	 * 根据部署id查询流程实例(可能启动了多个实例，所以返回列表)
	 * @param deploymentId 部署id
	 * @return
	 */
	public List<HistoricProcessInstance> getPIsByDeploymentId(String deploymentId){
		HistoricProcessInstanceQuery historQuery = historyService.createHistoricProcessInstanceQuery().deploymentId(deploymentId).orderByProcessInstanceStartTime().desc();
		if (historQuery == null)
			return new ArrayList<>();
		List<HistoricProcessInstance> list = historQuery.list();
		return list;
	}

	/**
	 * 根据businesskey查询流程实例
	 * @param businesskey 业务key
	 * @return
	 */
	public List<HistoricProcessInstance> getPIsByBusinessKey(String businesskey){
		HistoricProcessInstanceQuery historQuery = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(businesskey).orderByProcessInstanceStartTime().desc();
		if (historQuery == null)
			return new ArrayList<>();
		List<HistoricProcessInstance> list = historQuery.list();
		return list;
	}


	/**
	 * 查询所有流程实例（包括结束的和正在运行的）<br/>
	 * 
	 * @return
	 * @throws Exception
	 */

	public List<BaseVO> findAllProcessInstances(Page<BaseVO> page) throws Exception {

		HistoricProcessInstanceQuery historQuery = historyService.createHistoricProcessInstanceQuery()
				.orderByProcessInstanceStartTime().desc();

		List<BaseVO> processList = queryHistoryProcessInstanceList(page, historQuery);
		return processList;
	}

	/**
	 * 查询我的流程实例（包括结束的和正在运行的）<br/>
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<BaseVO> findMyProcessInstances(Page<BaseVO> page, String userCode) throws Exception {

		HistoricProcessInstanceQuery historQuery = historyService.createHistoricProcessInstanceQuery()
				.startedBy(userCode).orderByProcessInstanceStartTime().desc();

		List<BaseVO> processList = queryHistoryProcessInstanceList(page, historQuery);
		return processList;
	}

	/**
	 * 查询已结束的流程实例<br/>
	 * @return
	 */
	public List<BaseVO> findFinishedProcessInstances(Page<BaseVO> page, String userCode, boolean isInvolved) {
		HistoricProcessInstanceQuery historQuery = null;
		if (isInvolved) {
			historQuery = getHisProInsQueryInvolvedUser(userCode);
		} else {
			historQuery = getHisProInsQueryStartedBy(userCode);
		}

		List<BaseVO> processList = queryHistoryProcessInstanceList(page, historQuery);
		return processList;
	}


	/**
	 * 查询指定用户发起的流程 （流程历史 用户发起 ）
	 * 
	 * @param userCode
	 * @return
	 */
	private HistoricProcessInstanceQuery getHisProInsQueryStartedBy(String userCode) {
		HistoricProcessInstanceQuery historQuery = historyService.createHistoricProcessInstanceQuery()
				.startedBy(userCode).finished().orderByProcessInstanceEndTime().desc();
		return historQuery;
	}

	/**
	 * 查询指定用户参与的流程信息 （流程历史 用户参与 ）
	 * 
	 * @param userCode
	 * @return
	 */
	private HistoricProcessInstanceQuery getHisProInsQueryInvolvedUser(String userCode) {
		HistoricProcessInstanceQuery historQuery = historyService.createHistoricProcessInstanceQuery()
				.involvedUser(userCode).finished().orderByProcessInstanceEndTime().desc();
		return historQuery;
	}


	private List<BaseVO> queryHistoryProcessInstanceList(Page<BaseVO> page, HistoricProcessInstanceQuery historQuery) {
		if (historQuery == null)
			return new ArrayList<>();

		List<HistoricProcessInstance> list = new ArrayList<>();
		list = historQuery.list();
		if(null != page) {//分页
			//			Integer totalSum = historQuery.list().size();
			//			int[] pageParams = page.getPageParams(totalSum);
			//			list = historQuery.listPage(pageParams[0], pageParams[1]);
			int[] pageParams = page.getPageParams(list.size());
			list = list.subList(pageParams[0], pageParams[0]+pageParams[1]);
		}

		List<BaseVO> processList = castProcessToBaseVo(list);
		return processList;
	}

	private List<BaseVO> castProcessToBaseVo(List<HistoricProcessInstance> list) {
		List<BaseVO> processList = new ArrayList<>();
		for (HistoricProcessInstance historicProcessInstance : list) {
			String processInstanceId = historicProcessInstance.getId();
			BaseVO base = null;
			if (null != historicProcessInstance.getEndTime()) {
				base = getBaseVOFromHistoryVariable(processInstanceId);
				base.setDeleteReason(historicProcessInstance.getDeleteReason());
				base.setEnd(true);
			}else {
				base = (BaseVO) getBaseVOFromRu_Variable(processInstanceId);
			}
			HistoricTaskInstance historicTaskInstance = null;
			historicTaskInstance = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).orderByHistoricTaskInstanceEndTime().desc().list().get(0);
			if (null == base) continue;
			cusTaskService.setBaseVO(base,null,historicTaskInstance,null, historicProcessInstance);
			processList.add(base);
		}
		return processList;
	}

	/**
	 * 根据流程实例id获取历史流程实例
	 * @param processInstanceId
	 * @return
	 */
	public HistoricProcessInstance getHisProcessInstanceByInstanceId(String processInstanceId) {
		return historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).orderByProcessInstanceEndTime().desc().singleResult();
	}


	public void deleteProcessInstance(String processInstanceId, String deleteReason) throws Exception {
		deleteProcessInstance(processInstanceId, deleteReason, false);
	}

	/**
	 * 删除流程实例，可级联删除
	 */
	public void deleteProcessInstance(String processInstanceId, String deleteReason, boolean cascade) throws Exception {
		boolean isEnd = isProcessEnd(processInstanceId);
		if (!isEnd) {
			runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
		}
		if (cascade) {
			historyService.deleteHistoricProcessInstance(processInstanceId);
		}
	}

	/**
	 * 判断流程是否已经结束
	 * 
	 * @param processInstanceId
	 * @return
	 */
	public boolean isProcessEnd(String processInstanceId) {
		return historyService.createHistoricProcessInstanceQuery().finished()
				.processInstanceId(processInstanceId).count() > 0;
	}

	/**
	 * 获取当前流程下的key为entity的variable<br/>
	 */
	public BaseVO getBaseVOFromRu_Variable(String processInstanceId) {
		BaseVO base = (BaseVO) getRunVariable("entity", processInstanceId);
		return base;
	}

	public Object getRunVariable(String variableKey, String processInstanceId) {
		boolean hasKey = this.runtimeService.hasVariable(processInstanceId, variableKey);
		if (!hasKey)
			return null;
		Object obj = this.runtimeService.getVariable(processInstanceId, variableKey);
		return obj;
	}

	/**
	 * 根据流程实例id获取所有的流程变量
	 * @param processInstanceId
	 * @return
	 */
	public Map<String, Object> getRunVariables(String processInstanceId) {
		Map<String, Object> obj = this.runtimeService.getVariables(processInstanceId);
		return obj;
	}


	/**
	 * 设置流程变量
	 * @param processInstanceId
	 * @param variables
	 */
	public void setVariables(String processInstanceId, Map<String, Object> variables) {
		if (variables != null) {
			this.runtimeService.setVariables(processInstanceId,variables);
		}
	}

	/**
	 * 获取历史流程变量entity对象
	 * @param processInstanceId
	 * @return
	 */
	public BaseVO getBaseVOFromHistoryVariable(String processInstanceId) {

		BaseVO base = (BaseVO) getHistoryVariable("entity",processInstanceId);
		return base;
	}

	/**
	 * 获取历史流程变量
	 * @param variableKey
	 * @param processInstanceId
	 * @return
	 */
	public Object getHistoryVariable(String variableKey, String processInstanceId) {

		List<HistoricVariableInstance> listVar = this.historyService.createHistoricVariableInstanceQuery().variableName(variableKey)
				.processInstanceId(processInstanceId).list();

		Object obj = null;
		if(listVar.size() > 0) {
			listSort(listVar);
			obj = listVar.get(0).getValue();
		}
		return obj;
	}

	/**
	 * 对历史变量列表按照创建时间排序
	 * 
	 * @param list
	 */
	private static void listSort(List<HistoricVariableInstance> list) {
		Collections.sort(list, new Comparator<HistoricVariableInstance>() {

			public int compare(HistoricVariableInstance o1, HistoricVariableInstance o2) {
				try {
					Date dt1 = o1.getCreateTime();
					Date dt2 = o2.getCreateTime();
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
			}
		});
	}

	/**
	 * 显示图片-通过部署ID，不带流程跟踪
	 * 
	 * @param resourceType
	 * @param processDefinitionId
	 * @return
	 * @throws Exception
	 */
	public InputStream getDiagramByProDefinitionId_noTrace(String resourceType, String deploymentId) throws Exception {
		ProcessDefinition processDefinition = findProcessDefinitionByDeploymentId(deploymentId);
		String resourceName = "";
		if (resourceType.equals("png") || resourceType.equals("image")) {
			resourceName = processDefinition.getDiagramResourceName();
		} else if (resourceType.equals("xml")) {
			resourceName = processDefinition.getResourceName();
		}
		InputStream resourceAsStream = null;
		if (StringUtil.isNotEmpty(resourceName)) {
			resourceAsStream = repositoryService.getResourceAsStream(deploymentId, resourceName);
		}
		return resourceAsStream;
	}

	/**
	 * 获取流程图像，已执行节点和流程线高亮显示
	 */
	public InputStream getActivitiProccessImage(String processInstanceId) throws Exception {
		System.out.println("[开始]-获取流程图图像 processInstanceId：" + processInstanceId);
		try {
			// 获取历史流程实例
			HistoricProcessInstance historicProcessInstance = getHisProcessInstanceByInstanceId(processInstanceId);
			if (historicProcessInstance == null) {
				throw new Exception();
			} else {
				List<HistoricActivityInstance> historicActivityInstanceList = getHistoricActivityInstanceList(
						processInstanceId);
				// 已执行的节点ID集合
				List<String> executedActivityIdList = new ArrayList<String>();
				int index = 1;
				for (HistoricActivityInstance activityInstance : historicActivityInstanceList) {
					executedActivityIdList.add(activityInstance.getActivityId());
					System.out.println("第[" + index + "]个已执行节点=" + activityInstance.getActivityId() + " : "
							+ activityInstance.getActivityName());
					index++;
				}

				// 获取流程定义
				ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) ProcessDefinitionCache.get().getProcessDefination(historicProcessInstance.getProcessDefinitionId());
				// 获取流程图图像字符流
				InputStream imageStream = com.zr.workflow.activiti.util.ProcessDiagramGenerator
						.generateDiagram(processDefinition, "png", executedActivityIdList);
				System.out.println("[完成]-获取流程图图像");
				return imageStream;
			}
		} catch (Exception e) {
			System.out.println("【异常】-获取流程图失败！" + e.getMessage());
			throw new Exception(e);
		}
	}

	/**
	 * 获取流程历史中已执行节点，并按照节点在流程中执行先后顺序排序
	 * @param processInstanceId
	 * @return
	 */
	private List<HistoricActivityInstance> getHistoricActivityInstanceList(String processInstanceId) {
		List<HistoricActivityInstance> historicActivityInstanceList = historyService
				.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId)
				.orderByHistoricActivityInstanceId().asc().list();
		historicActivityInstanceList.sort(new Comparator<HistoricActivityInstance>() {

			public int compare(HistoricActivityInstance o1, HistoricActivityInstance o2) {
				try {
					Date dt1 = o1.getEndTime();
					Date dt2 = o2.getEndTime();
					if (dt1 == null || dt2 == null)
						return 1;
					if (dt1.getTime() >= dt2.getTime()) {
						return 1;
					} else {
						return -1;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return 0;
			}
		});
		return historicActivityInstanceList;
	}


	/**
	 * 激活流程实例
	 */
	public void activateProcessInstance(String processInstanceId) {
		runtimeService.activateProcessInstanceById(processInstanceId);
	}

	/**
	 * 根据流程key激活流程定义
	 * @param processDefinitionKey
	 */
	public void activateProcessDefination(String processDefinitionKey) {
		repositoryService.activateProcessDefinitionByKey(processDefinitionKey, true, null);
	}

	/**
	 * 根据一个流程实例的id挂起该流程实例
	 */
	public void suspendProcessInstance(String processInstanceId) {
		runtimeService.suspendProcessInstanceById(processInstanceId);
	}

	/**
	 * 根据流程key暂停流程定义
	 */
	public void suspendProcessDefinition(String processDefinitionKey) {
		//        repositoryService.suspendProcessDefinitionByKey(processDefinitionKey);//这时可以继续运行，报错：流程已被挂起
		//根据流程定义的key暂停一个流程定义,并且级联挂起该流程定义下的流程实例,这样流程无法运行
		repositoryService.suspendProcessDefinitionByKey(processDefinitionKey, true, null);
	}
}
