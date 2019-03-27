package com.zr.activiti.entity;

/**
 * 用户实体
 * @author Administrator
 *
 */
public class User {
	private String userId;
	private String userCode;
	private String userName;
	private String password;
	
	
	public User() {
		super();
	}
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getUserCode() {
		return userCode;
	}
	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return "User [userId=" + userId + ", userCode=" + userCode + ", userName=" + userName + ", password=" + password
				+ "]";
	}
	
	
}
