package com.zr.activiti.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 字符串工具类
 * @author 
 *
 */
public class StringUtil {


    /**
     * 判断是否是空
     * @param str
     * @return
     */
    public static boolean isEmpty(Object str){
        if(str==null || "".equals(str.toString().trim())){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 判断是否不是空
     * @param str
     * @return
     */
    public static boolean isNotEmpty(Object obj){
        if((obj != null) && !"".equals(obj.toString().trim())){
            return true;
        }else{
            return false;
        }
    }
    
    /**
     * 格式化模糊查询
     * @param str
     * @return
     */
    public static String formatLike(String str){
        if(isNotEmpty(str)){
            return "%"+str+"%";
        }else{
            return null;
        }
    }
    

	/**
	 * 去掉最后一位
	 * 
	 * @param str
	 * @return
	 */
	public static String cropTail(String str) {
		str = str.length() < 1 ? "" : str.substring(0, str.length() - 1);
		return str;
	}
	

	/**
	 * 将 ids 和 names 去掉末尾的逗号并存入map中
	 * @param ids
	 * @param names
	 * @return
	 */
	public static Map<String, String> strToMap(String ids, String names) {
		ids = StringUtil.cropTail(ids);
		names = StringUtil.cropTail(names);
		Map<String, String> candidateUser = new HashMap<>();
		candidateUser.put("candidateIds", ids);
		candidateUser.put("candidateNames", names);
		return candidateUser;
	}
}