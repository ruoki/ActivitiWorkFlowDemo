package com.zr.workflow.springdemo.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zr.workflow.springdemo.entity.User;

/**
 * 用户数据访问接口
 * @author Administrator
 *
 */
public interface UserDAO {

	public List<User> getAllUsers();

    /**
     * 查询（模糊查询）
     * @param map
     * @return集合
     */
    public List<User> findLike(User user);
    
	public User getUserById(@Param("userId")String userId);
	public User getUserByCode(@Param("userCode")String userCode);
	
	public int add(User user);
	public int delete(@Param("userId")String userId);
	public int deleteAll();
	public int update(User user);
	
	
	public List<User> findUsersByIds(@Param("userId")String userIds);
}
