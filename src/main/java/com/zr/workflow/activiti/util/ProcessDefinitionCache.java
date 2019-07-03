package com.zr.workflow.activiti.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.activiti.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.activiti.engine.impl.javax.el.ExpressionFactory;
import org.activiti.engine.impl.javax.el.ValueExpression;
import org.activiti.engine.impl.juel.ExpressionFactoryImpl;
import org.activiti.engine.impl.juel.SimpleContext;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.apache.commons.lang.StringUtils;

import com.zr.workflow.activiti.entity.BaseVO;

/**
 * 流程定义缓存
 *
 * @author
 */
public class ProcessDefinitionCache {
	public static final String ARCHIVE = "archive";// 归档

	private static Map<String, ProcessDefinition> map;

	private static Map<String, List<ActivityImpl>> activities;

	private static Map<String, ActivityImpl> singleActivity;

	private static RepositoryService repositoryService;
	private static RuntimeService runtimeService;

	private static volatile ProcessDefinitionCache instance;
	private ProcessDefinitionCache() {
		map = new HashMap<>();
		activities = new HashMap<>();
		singleActivity = new HashMap<>();
	}
	public static ProcessDefinitionCache get() {
		if (instance == null) {
			synchronized (ProcessDefinitionCache.class) {
				if (instance == null) {
					instance = new ProcessDefinitionCache();
				}
			}
		}
		return instance;
	}
	public void setRepositoryService(RepositoryService repositoryService) {
		ProcessDefinitionCache.repositoryService = repositoryService;
	}

	public ProcessDefinition getProcessDefination(String processDefinitionId) {
		ProcessDefinition processDefinition = map.get(processDefinitionId);

		if (processDefinition == null) {
			if (null == repositoryService) {
				repositoryService = ApplicationContextHandler.getBean(RepositoryService.class);
			}
			processDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
					.getDeployedProcessDefinition(processDefinitionId);
			if (processDefinition != null) {
				put(processDefinitionId, processDefinition);
			}
		}
		return processDefinition;
	}

	public void put(String processDefinitionId, ProcessDefinition processDefinition) {
		map.put(processDefinitionId, processDefinition);
		ProcessDefinitionEntity pde = (ProcessDefinitionEntity) processDefinition;
		activities.put(processDefinitionId, pde.getActivities());
		for (ActivityImpl activityImpl : pde.getActivities()) {
			singleActivity.put(processDefinitionId + "_" + activityImpl.getId(), activityImpl);
		}
	}

	/**
	 * 根据流程定义id获取所有的任务节点
	 * 
	 * @param processDefinitionId
	 * @return
	 */
	public List<ActivityImpl> getActivities(String processDefinitionId) {
		if (activities != null && activities.size() > 0 && null != activities.get(processDefinitionId)) {
			return activities.get(processDefinitionId);
		} else {
			ProcessDefinitionEntity pde = (ProcessDefinitionEntity) getProcessDefination(processDefinitionId);
			return pde.getActivities();
		}
	}

	public ActivityImpl getActivity(String processDefinitionId, String activityId) {
		ProcessDefinition processDefinition = getProcessDefination(processDefinitionId);
		if (processDefinition != null) {
			ActivityImpl activityImpl = singleActivity.get(processDefinitionId + "_" + activityId);
			return activityImpl;
		}
		return null;
	}

	public String getActivityName(String processDefinitionId, String activityId) {
		ActivityImpl activity = getActivity(processDefinitionId, activityId);
		if (activity != null) {
			return activity.getProperty("name").toString();
		}
		return null;
	}


	/**
	 * 获取节点的网关条件信息
	 * @param repositoryService1
	 * @param runtimeService1 
	 * 
	 * @param processInstanceId
	 *            任流程Id信息
	 * @param currentActivitiId
	 *            当前流程节点Id
	 * @return 下一个节点信息
	 * @throws Exception
	 */
	public ActivityImpl getNextNodeInfo(RepositoryService repositoryService1, RuntimeService runtimeService1, String processInstanceId, String currentActivitiId) throws Exception {
		try {
			ActivityImpl resultActivityImpl = null;
			setRepositoryService(repositoryService1);
			// 获取流程发布Id信息
			String definitionId = runtimeService1.createProcessInstanceQuery().processInstanceId(processInstanceId)
					.singleResult().getProcessDefinitionId();
			List<ActivityImpl> activitiList = getActivities(definitionId);
			String tempActivitiId = null;
			// 遍历所有节点信息
			for (ActivityImpl activityImpl : activitiList) {
				tempActivitiId = activityImpl.getId();
				if (currentActivitiId.equals(tempActivitiId)) {
					// 获取下一个节点信息
					resultActivityImpl = nextDefinition(activityImpl);
					break;
				}
			}
			return resultActivityImpl;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * 获取网关节点信息
	 * @param activityImpl 流程节点信息
	 * @return
	 */
	private ActivityImpl nextDefinition(ActivityImpl activityImpl) {
		// 如果遍历节点为用户任务并且节点不是当前节点信息
		if ("exclusiveGateway".equals(activityImpl.getProperty("type"))) {// 当前节点为exclusiveGateway
			return activityImpl;
		} else {
			// 获取节点所有流向线路信息
			List<PvmTransition> outTransitions = activityImpl.getOutgoingTransitions();
			PvmActivity ac = null;
			for (PvmTransition tr : outTransitions) {
				ac = tr.getDestination(); // 获取线路的终点节点
				// 如果流向线路为排他网关
				if ("exclusiveGateway".equals(ac.getProperty("type"))) {
					return (ActivityImpl) ac;
				}
			}
			return null;
		}
	}


	/**
	 * 获取下一节点key
	 * @param baseVO
	 * @param condition
	 * @return
	 * @throws Exception
	 */
	public String getNextActivitiId(BaseVO baseVO,String condition) throws Exception {
		final String processInstanceId = baseVO.getProcessInstanceId();
		String currentActivitiId = baseVO.getTaskDefinitionKey();
		if (StringUtil.isEmpty(processInstanceId) || StringUtil.isEmpty(currentActivitiId))
			return "";
		String nextTaskDefKey = getNextActivitiId(processInstanceId,currentActivitiId,condition);
		return nextTaskDefKey;
	}

	/**
	 * 获取下一节点key
	 * @param processInstanceId 流程id
	 * @param currentActivitiId 当前节点id
	 * @param condition 网关条件
	 * @return
	 * @throws Exception
	 */
	public String getNextActivitiId(String processInstanceId,String currentActivitiId,String condition) throws Exception {
		if (StringUtil.isEmpty(processInstanceId) || StringUtil.isEmpty(currentActivitiId))
			return "";

		TaskDefinition nextTaskDefinition = getNextTaskInfo(processInstanceId, currentActivitiId, condition);
		if (nextTaskDefinition == null)
			return "";
		String nextTaskDefKey = nextTaskDefinition.getKey();
		return nextTaskDefKey;
	}


	/**
	 * 获取下一个用户任务信息
	 * 
	 * @param processInstanceId
	 *            流程Id
	 * @param currentActivitiId
	 *            当前流程节点Id
	 * @param condition
	 *            排他网关条件
	 * @return 下一个用户任务用户组信息
	 * @throws Exception
	 */
	public TaskDefinition getNextTaskInfo(String processInstanceId, String currentActivitiId, String condition) throws Exception {
		try {
			TaskDefinition task = null;

			if (null == runtimeService) {
				runtimeService = ApplicationContextHandler.getBean(RuntimeService.class);
			}
			// 获取流程发布Id信息
			String definitionId = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId)
					.singleResult().getProcessDefinitionId();
			List<ActivityImpl> activitiList = getActivities(definitionId);

			// ExecutionEntity execution = (ExecutionEntity)
			// runtimeService.createProcessInstanceQuery()
			// .processInstanceId(processInstanceId).singleResult();
			// 当前流程节点Id信息
			// String currentActivitiId = execution.getActivityId();

			String tempActivitiId = null;
			// 遍历所有节点信息
			for (ActivityImpl activityImpl : activitiList) {
				tempActivitiId = activityImpl.getId();
				if (currentActivitiId.equals(tempActivitiId)) {
					// 获取下一个节点信息
					task = nextTaskDefinition(activityImpl, tempActivitiId, condition, processInstanceId);
					break;
				}
			}
			return task;
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 下一个任务节点信息,
	 * 
	 * 如果下一个节点为用户任务则直接返回,
	 * 
	 * 如果下一个节点为排他网关, 获取排他网关Id信息, 根据排他网关Id信息和execution获取流程实例排他网关Id为key的变量值,
	 * 根据变量值分别执行排他网关后线路中的el表达式, 并找到el表达式通过的线路后的用户任务
	 * 
	 * @param activityImpl 流程节点信息
	 * @param currentActivitiId 当前流程节点Id信息
	 * @param elString 排他网关顺序流线段判断条件
	 * @param processInstanceId 流程实例Id信息
	 * @return
	 */
	private TaskDefinition nextTaskDefinition(ActivityImpl activityImpl, String currentActivitiId, String elString,
			String processInstanceId) {

		TaskDefinition taskDefinition = null;
		// 如果遍历节点为用户任务并且节点不是当前节点信息
		if ("userTask".equals(activityImpl.getProperty("type")) && !currentActivitiId.equals(activityImpl.getId())) {
			taskDefinition = getUserTask(activityImpl);
			return taskDefinition;
		} else if ("exclusiveGateway".equals(activityImpl.getProperty("type"))) {// 当前节点为exclusiveGateway
			taskDefinition = getNextTaskOfGateway(activityImpl, currentActivitiId, elString, processInstanceId);
			return taskDefinition;
		} else {
			// 获取节点所有流向线路信息
			List<PvmTransition> outTransitions = activityImpl.getOutgoingTransitions();

			PvmActivity ac = null;
			for (PvmTransition tr : outTransitions) {
				ac = tr.getDestination(); // 获取线路的终点节点
				// 如果流向线路为排他网关
				if ("exclusiveGateway".equals(ac.getProperty("type"))) {
					taskDefinition = getNextTaskOfGateway(ac, currentActivitiId, elString, processInstanceId);
					return taskDefinition;
				} else if ("userTask".equals(ac.getProperty("type"))) {
					taskDefinition = getUserTask((ActivityImpl) ac);
					return taskDefinition;
				} else {
				}
			}
			return null;
		}
	}

	/**
	 * 获取用户任务节点
	 * 
	 * @param activityImpl 流程节点信息
	 * @return
	 */
	private TaskDefinition getUserTask(ActivityImpl activityImpl) {

		// 获取该节点下一个节点信息
		TaskDefinition taskDefinition = null;
		if (activityImpl.getActivityBehavior() instanceof UserTaskActivityBehavior) {
			taskDefinition = ((UserTaskActivityBehavior) activityImpl.getActivityBehavior())
					.getTaskDefinition();

		} else if (activityImpl.getActivityBehavior() instanceof ParallelMultiInstanceBehavior) {

			ParallelMultiInstanceBehavior multiInstanceBehavior = (ParallelMultiInstanceBehavior) activityImpl.getActivityBehavior();
			ActivityBehavior innerActivityBehavior = multiInstanceBehavior.getInnerActivityBehavior();
			if (innerActivityBehavior instanceof UserTaskActivityBehavior) {
				taskDefinition = ((UserTaskActivityBehavior) innerActivityBehavior).getTaskDefinition();
			}
		}
		return taskDefinition;
	}

	/**
	 * 获取排他网关的下一节点
	 * 
	 * @param activityImpl
	 * @param activityId
	 * @param processInstanceId
	 * @return
	 */
	private TaskDefinition getNextTaskOfGateway(PvmActivity activityImpl, String activityId, String elString,
			String processInstanceId) {
		Object s;
		List<PvmTransition> outTransitions = activityImpl.getOutgoingTransitions();

		// 如果网关路线判断条件为空信息
		// if (StringUtils.isEmpty(elString)) {
		// 获取流程启动时设置的网关判断条件信息
		// elString = getGatewayCondition(activityImpl.getId(), processInstanceId);
		// }
		// 如果排他网关只有一条线路信息
		if (outTransitions.size() == 1) {
			return nextTaskDefinition((ActivityImpl) outTransitions.get(0).getDestination(), activityId, elString,
					processInstanceId);
		} else if (outTransitions.size() > 1) { // 如果排他网关有多条线路信息
			Object el = elString;
//			if (!ProcessControllerImpl.MONTHLYREPORT_PERSON_PROCESS.equalsIgnoreCase(procDefKey)
//					&& !ProcessControllerImpl.MONTHLYREPORT_PROJECT_PROCESS.equalsIgnoreCase(procDefKey)) {
//				el = "true".equals(elString) ? true : false;
//			}

			for (PvmTransition tr1 : outTransitions) {
				s = tr1.getProperty("conditionText"); // 获取排他网关线路判断条件信息
				// 判断el表达式是否成立
				if (isCondition(activityImpl.getId(), StringUtils.trim(s.toString()), el)) {
					return nextTaskDefinition((ActivityImpl) tr1.getDestination(), activityId, elString,
							processInstanceId);
				}

			}
		}
		return null;
	}


	/**
	 * 查询流程启动时设置排他网关判断条件信息
	 * 
	 * @param gatewayId 排他网关Id信息, 流程启动时设置网关路线判断条件key为网关Id信息
	 * @param processInstanceId 流程实例Id信息
	 * @return
	 */
	public String getGatewayCondition(String gatewayId, String processInstanceId) {

		if (null == runtimeService) {
			runtimeService = ApplicationContextHandler.getBean(RuntimeService.class);
		}
		Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).singleResult();
		Object object = runtimeService.getVariable(execution.getId(), gatewayId);
		return object == null ? "" : object.toString();
	}

	/**
	 * 根据key和value判断el表达式是否通过信息
	 * 
	 * @param key el表达式key信息
	 * @param el el表达式信息
	 * @param value el表达式传入值信息
	 * @return
	 */
	private boolean isCondition(String key, String el, Object value) {
		ExpressionFactory factory = new ExpressionFactoryImpl();
		SimpleContext context = new SimpleContext();
		context.setVariable(key, factory.createValueExpression(value, Object.class));
		ValueExpression e = factory.createValueExpression(context, el, boolean.class);
		return (Boolean) e.getValue(context);
	}


}
