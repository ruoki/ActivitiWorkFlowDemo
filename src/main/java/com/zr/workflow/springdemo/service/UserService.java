package com.zr.workflow.springdemo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zr.workflow.activiti.util.StringUtil;
import com.zr.workflow.springdemo.dao.UserDAO;
import com.zr.workflow.springdemo.entity.User;
/**
 * service类注解为IOC组件
 * @author Administrator
 *
 */
@Service
public class UserService {
	@Resource
	UserDAO userDAO;
	
	public List<User> getAllUsers(){
		return userDAO.getAllUsers();
	}

	public List<User> findLike(User user) {
		// TODO Auto-generated method stub
		return userDAO.findLike(user);
	}


	public User getUserById(String userId){
		return userDAO.getUserById(userId);
	}

	public User getUserByCode(String userCode){
		return userDAO.getUserByCode(userCode);
	}
	

    public int add(User user) throws Exception {
    	System.out.println("UserService add userCode:"+user.getUserCode());
        if(user.getUserId()==null||user.getUserId().equals("")){
            throw new Exception("userId不能为空");
        }
        return userDAO.add(user);
    }
    
    @Transactional
    public int add(User user1,User userBak){
        int rows=0;
        rows=userDAO.add(user1);
        rows=userDAO.add(userBak);
        return rows;
    }


    /**
     * 多删除
     */
    public int delete(String[] userIds){
        int rows=0;
        for (String idStr : userIds) {
            rows+=delete(idStr);
        }
        return rows;
    }

    public int delete(String userId) {
        return userDAO.delete(userId);
    }
    
    public int update(User entity) {
        return userDAO.update(entity);
    }
    
	public List<User> findUsersByIds(String userIds) {
		return userDAO.findUsersByIds(userIds);
	}
	

	/**
	 * 根据用户id查询mis_user表获取用户信息
	 * 
	 * @param userId
	 * @return
	 */
	public Map<String, String> getUserFromMisUser(String userId) {
		if (StringUtil.isEmpty(userId))
			return new HashMap<>();
		
		User manager = getUserById(userId);
		String user_id = manager == null ? "" : manager.getUserId();
		String userName = manager == null ? "" : manager.getUserName();

		Map<String, String> candidateUser = new HashMap<>();
		candidateUser.put("candidateIds", user_id);
		candidateUser.put("candidateNames", userName);
		return candidateUser;
	}
	


	/**
	 * 根据角色id从系统角色表查询用户信息
	 * 
	 * @param roleId
	 * @return
	 */
//	public Map<String, String> getUserFromUserRole(String roleId) {
//		System.out.println("getUserFromUserRole  roleId:" + roleId);
//		String candidateIds = "";
//		String candidateNames = "";
//		// 系统角色用户表根据角色id查询用户
//		List<MisUserRole> misUserRoleList = misUserRoleService.findByRoleId(roleId);
//		for (MisUserRole misUserRole : misUserRoleList) {
//			if (null != misUserRole.getUserId()) {
//				Map<String, String> user = getUserFromMisUser(misUserRole.getUserId());
//				candidateIds = candidateIds.concat(user.get("candidateIds")).concat(",");
//				candidateNames = candidateNames.concat(user.get("candidateNames")).concat(",");
//			}
//		}
//
//		Map<String, String> candidateUser = StringUtil.strToMap(candidateIds, candidateNames);
//		return candidateUser;
//	}


	

}
