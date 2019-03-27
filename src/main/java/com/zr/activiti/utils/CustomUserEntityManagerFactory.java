package com.zr.activiti.utils;

import javax.annotation.Resource;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.entity.UserIdentityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 自定义用户管理，用户session
 * 这里的属性需要与spring-activiti.xml中的该类的property一致
 * 不能使用identityService
 * @author Administrator
 *
 */
@Service
public class CustomUserEntityManagerFactory implements SessionFactory {
	
	@Resource
	private CustomUserEntityManager customUserEntityManager;
	

	@Override
	public Class<?> getSessionType() {
		return UserIdentityManager.class;
	}

	@Override
	public Session openSession() {
		return customUserEntityManager;
	}

	
	@Autowired
	public void setCustomUserEntityManager(CustomUserEntityManager customUserEntityManager) {
		this.customUserEntityManager = customUserEntityManager;
	}

}
