package com.zr.activiti.utils;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.impl.persistence.entity.UserEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;


import org.springframework.stereotype.Component;

import com.zr.activiti.dao.UserDAO;

@Component
public class CustomUserEntityManager extends UserEntityManager {

	@Autowired
	private UserDAO userDao;
//	@Autowired
//	private MisRoleDao misRoleDao;
//	@Autowired
//	private MisUserRoleDao misUserRoleDao;

	@Override
	public User findUserById(String userId) {
		System.out.println("CustomUserEntityManager  findUserById userId:" + userId);
		if (userId == null)
			return null;
		try {
			UserEntity userEntity = new UserEntity();
			com.zr.activiti.entity.User customUser = userDao.getUserById(userId);
			if (customUser == null)
				return null;

			userEntity.setId(customUser.getUserId().toString());
			userEntity.setFirstName(customUser.getUserName());
			userEntity.setPassword(customUser.getPassword());
			userEntity.setRevision(1);
			return userEntity;
		} catch (EmptyResultDataAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<Group> findGroupsByUser(String userId) {
		System.out.println("CustomUserEntityManager  findGroupsByUser userId:" + userId);
		if (userId == null)
			return null;
		com.zr.activiti.entity.User customUser = userDao.getUserById(userId);
		if (customUser == null)
			return null;

//		List<MisRole> roleList = new ArrayList<MisRole>();
//		System.out.println("CustomUserEntityManager  findGroupsByUser userId:" + customUser.getUserId());
//		List<MisUserRole> misUserRoleList = misUserRoleDao.findByUserId(customUser.getUserId());
//		for (MisUserRole misUserRole : misUserRoleList) {
//			final String roleId = misUserRole.getRoleId();
//			
//			boolean isExitRole = misRoleDao.findRoleById(roleId) != null && misRoleDao.findRoleById(roleId).size()>0;
//			MisRole role = isExitRole ? misRoleDao.findRoleById(roleId).get(0) : null;
//			if(role != null)
//				roleList.add(role);
//		}
		List<Group> groupEntitys = new ArrayList<Group>();

//		for (MisRole role : roleList) {
//			GroupEntity groupEntity = toActivitiGroup(role);
//			groupEntitys.add(groupEntity);
//		}
		return groupEntitys;
	}

//	public static GroupEntity toActivitiGroup(MisRole role) {
//		GroupEntity groupEntity = new GroupEntity();
//		groupEntity.setRevision(1);
//		groupEntity.setType("assignment");
//
//		groupEntity.setId(role.getCode());
//		groupEntity.setName(role.getName());
//		return groupEntity;
//	}

}
