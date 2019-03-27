package com.zr.activiti.utils;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.zr.activiti.entity.User;


public class LocalSessions {
	private static Map<String, HttpSession> session_cache = new HashMap<>();

	public static void put(String sessionId, HttpSession session) {
		session_cache.put(sessionId, session);

	}

	public static HttpSession get(String sessionId) {
		return session_cache.get(sessionId);

	}

	public static void remove(String sessionId) {
		session_cache.remove(sessionId);

	}

	public static User getCurrentUser(HttpServletRequest request) {
		HttpSession session = request.getSession();
		User loginUser = (User) session.getAttribute("LOGIN_USER");
		return loginUser;
	}

}
