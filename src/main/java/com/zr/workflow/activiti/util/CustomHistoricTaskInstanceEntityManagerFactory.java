package com.zr.workflow.activiti.util;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 历史任务管理
 * 这里的属性需要与spring-activiti.xml中的该类的property一致
 * CustomHistoricTaskInstanceEntityManager与spring-activiti.xml中的该类的property一致（除了第一个字母小写），
 * @author zhourq
 *
 */
@Service
public class CustomHistoricTaskInstanceEntityManagerFactory implements SessionFactory {

	@Resource
	private CustomHistoricTaskInstanceEntityManager customHistoricTaskInstanceEntityManager;


	@Override
	public Class<?> getSessionType() {
		return HistoricTaskInstanceEntityManager.class;
	}

	@Override
	public Session openSession() {
		return customHistoricTaskInstanceEntityManager;
	}


	@Autowired
	public void setCustomHistoricTaskInstanceEntityManager(CustomHistoricTaskInstanceEntityManager customHistoricTaskInstanceEntityManager) {
		this.customHistoricTaskInstanceEntityManager = customHistoricTaskInstanceEntityManager;
	}

}
