package com.zr.workflow.activiti.util;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * GFJsonUtil:<br/>
 * 用 Gson 将bean转换Json确保数据的正确，<br/>
 * 使用 FastJson 将Json转换Bean
 * @author zhourq
 *
 */
public class GFJsonUtil {
	private Logger log = LoggerFactory.getLogger(GFJsonUtil.class);

	private static volatile GFJsonUtil instance;

	private GFJsonUtil() {}

	public static GFJsonUtil get() {
		if (instance == null) {
			synchronized (GFJsonUtil.class) {
				if (instance == null) {
					instance = new GFJsonUtil();
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
		String json = toJson(obj,null);
		return json;
	}
	/**
	 * 将Object转换成json：
	 * @param obj
	 * @param type 转换方式
	 * @return
	 */
	public String toJson(Object obj,String type) {
		String json = "";
		if(StringUtil.isEmpty(type)) {
			json = toJsonByFastJson(obj);
		}else {
			json = toJsonByGson(obj);
		}
		return json;
	}

	/**
	 * 用Gson 将Object转换成json：
	 * 实体类中需要打印的字段需要加上注解@Expose
	 * @param obj
	 * @return
	 */
	public String toJsonByGson(Object obj) {
		GsonBuilder gsonBuilder = new GsonBuilder();  
		//		gsonBuilder.serializeNulls(); //序列化null值
		//fixed Gson.toJson happened IllegalArgumentException: class xxx declares multiple JSON fields named key
		//		gsonBuilder.excludeFieldsWithoutExposeAnnotation(); 只序列化Expose注解字段
		Gson gson = gsonBuilder.create();
		String json = gson.toJson(obj);
		return json;
	}

	/**
	 * 用Gson批量添加json属性
	 * @param jsonObj
	 * @param resultMap
	 * @return
	 */
	//	public JsonObject addProperty(JsonObject jsonObj, Map<String, Object> resultMap) {
	//		Iterator<String> resultKeyIterator = resultMap.keySet().iterator();
	//		while (resultKeyIterator.hasNext()) {
	//			String key = (String) resultKeyIterator.next();
	//			Object value = resultMap.get(key);
	//			addProperty(jsonObj,key,value);
	//		}
	//		return jsonObj;
	//	}

	/**
	 * 用Gson添加json属性
	 * @param jsonObj
	 * @param key
	 * @param value
	 */
	//	public void addProperty(JsonObject jsonObj, String key, Object value) {
	//		boolean isContains = jsonObj.has(key);
	//		if(isContains)jsonObj.remove(key);
	//		jsonObj.addProperty(key, toJson(value));
	/**
	 * 验证一个字符串是否是合法的JSON串
	 * 
	 * @param input 要验证的字符串
	 * @return true-合法 ，false-非法
	 */
	public boolean validate(String json) {
		if (StringUtils.isBlank(json)) {
			return false;
		}
		try {
			new JsonParser().parse(json);
			return true;
		} catch (JsonParseException e) {
			log.error("json格式错误");
			return false;
		}
	}
	/**
	 * 用FastJson 将Object转换成json：
	 * @param obj
	 * @return
	 */
	public String toJsonByFastJson(Object obj) {
		String json = JSON.toJSONString(obj,true);
		return json;
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
		if(null == obj) return "";
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
