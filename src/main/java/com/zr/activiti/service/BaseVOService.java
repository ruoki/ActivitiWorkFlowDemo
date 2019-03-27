package com.zr.activiti.service;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.zr.activiti.dao.BaseVODao;
import com.zr.activiti.entity.BaseVO;

/**
 * 流程处理service
 * 
 * @author Administrator
 *
 */
@Service
public class BaseVOService {

	@Resource
	BaseVODao baseVODao;

	
	public void save(BaseVO baseVO) {
		baseVODao.save(baseVO);
	}
	
	
	public void update(BaseVO baseVO) {
		baseVODao.update(baseVO);
	}

	/**
	 * 根据projectId从数据库中查询实体
	 */
	
	public List<BaseVO> getByBusinessKey(String businessKey) {
		return baseVODao.getByBusinessKey(businessKey);
	}

	/**
	 * 根据userId从数据库中查询实体
	 */
	
	public List<BaseVO> getByUserId(String userId) {
		return baseVODao.getByUserId(userId);
	}


	
	public void deleteByUserId(String userId) throws Exception {
		this.baseVODao.deleteByUserId(userId);
	}

	
	public void deleteByProcessInstanceId(String processInstanceId) throws Exception {
		this.baseVODao.deleteByProcessInstanceId(processInstanceId);
	}

	
	public void deleteByBusinessKey(String businessKey) throws Exception {
		this.baseVODao.deleteByBusinessKey(businessKey);
	}


	
	public Integer deleteAll() throws Exception {
		return this.baseVODao.deleteAll();
	}
}
