package com.zr.workflow.springdemo.service;

import java.util.HashMap;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.zr.workflow.springdemo.dao.MisActivitiProcessDao;


@Service("ActivitiProcessService")
public class ActivitiProcessService {
	@Resource
	private MisActivitiProcessDao misActivitiProcessDao;

	public int saveCurrentActiviti(String processInstanceId, String json) {
		return misActivitiProcessDao.saveCurrentActiviti(processInstanceId,json);
	}
	
	public HashMap<String, String> getCurrentActiviti(String processInstanceId) {
		return misActivitiProcessDao.getCurrentActiviti(processInstanceId);
	}

	public void deleteActiviti(String processInstanceId) {
		misActivitiProcessDao.deleteActiviti(processInstanceId);
		
	}

}
