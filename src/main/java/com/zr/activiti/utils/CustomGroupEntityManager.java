package com.zr.activiti.utils;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.GroupEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zr.activiti.dao.UserDAO;
import com.zr.activiti.entity.User;


@Component
public class CustomGroupEntityManager extends GroupEntityManager {

	@Autowired
	private UserDAO userDao;
//	@Autowired
//	private MisRoleDao misRoleDao;
//	@Autowired
//	private MisUserRoleDao misUserRoleDao;

	@Override
	public List<Group> findGroupsByUser(String userId) {
		if (userId == null)
			return null;
		System.out.println("CustomGroupEntityManager findGroupsByUser userId:" + userId);
		User customUser = userDao.getUserById(userId);
		if (customUser == null)
			return null;

//		List<MisUserRole> misUserRoleList = misUserRoleDao.findByUserId(customUser.getUserId());
		List<Group> groups = new ArrayList<>();
		GroupEntity groupEntity;
//		for (MisUserRole userRole : misUserRoleList) {
//
//			final String roleId = userRole.getRoleId();
//			boolean isExitRole = misRoleDao.findRoleById(roleId) != null && misRoleDao.findRoleById(roleId).size()>0;
//			MisRole role = isExitRole ? misRoleDao.findRoleById(roleId).get(0) : null;
//				
//			groupEntity = new GroupEntity();
//			groupEntity.setRevision(1);
//			if (role != null) {
//				groupEntity.setType("assignment");
//				groupEntity.setId(role.getCode());
//				groupEntity.setName(role.getName());
//				groups.add(groupEntity);
//			}
//		}
		return groups;
	}

}
