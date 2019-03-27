package com.zr.activiti.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatUtil {
	public static String format(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}
	
	/**
	 * 获取当前日期字符串
	 * @return
	 */
	public static String getDoDay() {
		Date currentDate = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		return formatter.format(currentDate);
	}
}
