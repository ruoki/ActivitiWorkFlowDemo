package com.zr.activiti.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * JsonUtil:<br/>
 * 用 Gson 将bean转换Json确保数据的正确，<br/>
 * 使用 FastJson 将Json转换Bean
 * @author Administrator
 *
 */
public class JsonUtil {

	private static volatile JsonUtil instance;

    private JsonUtil() {}

    public static JsonUtil get() {
        if (instance == null) {
            synchronized (JsonUtil.class) {
                if (instance == null) {
                    instance = new JsonUtil();
                }
            }
        }
        return instance;
    }
    
    /**
     * 用Gson 将Object转换成json
     * @param obj
     * @return
     */
    public String toJson(Object obj) {
    	Gson gson = new Gson();
		String json = gson.toJson(obj);
		return json;
    }

    /**
     * 用Gson批量添加json属性
     * @param jsonObj
     * @param resultMap
     * @return
     */
	public JsonObject addProperty(JsonObject jsonObj, Map<String, Object> resultMap) {
		Iterator<String> resultKeyIterator = resultMap.keySet().iterator();
		while (resultKeyIterator.hasNext()) {
			String key = (String) resultKeyIterator.next();
			Object value = resultMap.get(key);
			addProperty(jsonObj,key,value);
		}
		return jsonObj;
	}

	/**
	 * 用Gson添加json属性
	 * @param jsonObj
	 * @param key
	 * @param value
	 */
	public void addProperty(JsonObject jsonObj, String key, Object value) {
		boolean isContains = jsonObj.has(key);
		if(isContains)jsonObj.remove(key);
		jsonObj.addProperty(key, new Gson().toJson(value));
	}
	
	/**
	 * 用fastJson将json转为bean;
	 * @param json
	 * @param clazz
	 * @return
	 */
	public <T> T parseJson(String json,Class<T> clazz) {
		return JSON.parseObject(json, clazz);
	}
	
	/**
	 * 将字符串转换为json数组
	 * @param jsonStr
	 * @return
	 */
	public JSONArray parseArray(String jsonStr) {
		return JSON.parseArray(jsonStr);
	}

	/**
	 * 用fastJson获取json属性
	 * @param json
	 * @param paramKey
	 * @return
	 */
    public String getProperty(String json, String paramKey) {
    	JSONObject obj = JSON.parseObject(json);
	   	return getProperty(obj, paramKey);
	}

    /**
     * 用fastJson获取json属性
     * @param obj
     * @param paramKey
     * @return
     */
	public String getProperty(JSONObject obj, String paramKey) {
		Set<String> set = obj.keySet();
	   	if(set.contains(paramKey)) {
	   		return obj.get(paramKey) == null ? "" : obj.get(paramKey).toString();
	   	}else {
	   		return "";
	   	}
	}

    /**
     * 用fastJson获取json 对象属性
     * @param json
     * @param paramKey
     * @return
     */
	public JSONObject getJSONObject(String json, String paramKey) {
		JSONObject obj = JSON.parseObject(json);
    	Set<String> set = obj.keySet();
	   	if(set.contains(paramKey)) {
	   		return obj.get(paramKey) == null ? null : (JSONObject)obj.get(paramKey);
	   	}else {
	   		return null;
	   	}
	}

	/**
	 * 用fastJson获取json 数组属性
	 * @param json
	 * @param paramKey
	 * @return
	 */
	public JSONArray getJSONArray(String json, String paramKey) {
		JSONObject obj = JSON.parseObject(json);
    	Set<String> set = obj.keySet();
	   	if(set.contains(paramKey)) {
	   		return obj.get(paramKey) == null ? null : (JSONArray)obj.get(paramKey);
	   	}else {
	   		return null;
	   	}
	}
	
    /**
     * 用fastJson去除json 某属性
     * @param json
     * @param key
     * @return
     */
    public String remove(String json,String key) {
    	JSONObject obj = JSON.parseObject(json);
    	Set<String> set = obj.keySet();
	   	if(set.contains(key)) {
	   		set.remove(key);
	   		json = obj.toString();
	   	}
	   	return json;
    }

}
