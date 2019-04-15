package com.zr.workflow.utils;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zr.workflow.activiti.util.CustomUserEntityManager;
import com.zr.workflow.springdemo.dao.UserDAO;

@Component
public class CustomUserEntityManagerIml extends CustomUserEntityManager {

	@Autowired
	private UserDAO userDao;
//	@Autowired
//	private MisRoleDao misRoleDao;
//	@Autowired
//	private MisUserRoleDao misUserRoleDao;

	public UserEntity getUserEntity(String userId) {
		UserEntity userEntity = new UserEntity();
		com.zr.workflow.springdemo.entity.User customUser = userDao.getUserById(userId);
		if (customUser == null)
			return null;

		userEntity.setId(customUser.getUserId().toString());
		userEntity.setFirstName(customUser.getUserName());
		userEntity.setPassword(customUser.getPassword());
		userEntity.setRevision(1);
		return userEntity;
	}


	public List<Group> getGroupEntitys(String userId) {
		List<Group> groupEntitys = new ArrayList<Group>();
		com.zr.workflow.springdemo.entity.User customUser = userDao.getUserById(userId);
		if (customUser == null)
			return null;

		//	List<MisUserRole> misUserRoleList = misUserRoleDao.findByUserId(customUser.getUserId());
			GroupEntity groupEntity;
		//	for (MisUserRole userRole : misUserRoleList) {
		//
		//		final String roleId = userRole.getRoleId();
		//		boolean isExitRole = misRoleDao.findRoleById(roleId) != null && misRoleDao.findRoleById(roleId).size()>0;
		//		MisRole role = isExitRole ? misRoleDao.findRoleById(roleId).get(0) : null;
		//			
				groupEntity = new GroupEntity();
				groupEntity.setRevision(1);
		//		if (role != null) {
		//			groupEntity.setType("assignment");
		//			groupEntity.setId(role.getCode());
		//			groupEntity.setName(role.getName());
		//			groupEntitys.add(groupEntity);
		//		}
		//	}
		return groupEntitys;
	}

}
