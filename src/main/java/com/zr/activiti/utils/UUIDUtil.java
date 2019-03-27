package com.zr.activiti.utils;

import java.util.UUID;

public class UUIDUtil {
	public static String uuid() {
		String uuid = UUID.randomUUID().toString();
		return uuid.replaceAll("-", "");
	}
}
