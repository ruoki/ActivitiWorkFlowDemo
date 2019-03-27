package com.zr.activiti.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.zr.activiti.service.ModelService;
import com.zr.activiti.utils.JsonUtil;


/**
 * 
 * 流程模型
 * 
 *
 */
@RestController
@Scope("prototype")
@RequestMapping("/model")
public class ModelController {
	private final Log log = LogFactory.getLog(getClass());

	@Resource
	private ModelService modelService;

	/**
	 * 创建模型
	 * test:先输入http://localhost:8080/ActivitiWorkFlowDemo/model/create
	 * 再输入：http://localhost:8080/ActivitiWorkFlowDemo/modeler.html?modelId=获取到的modelId
	 */
	@RequestMapping("/create")
	public String create(HttpServletRequest request) {
		Map<String, String> map = new HashMap<>();
		try {
			Model modelData = this.modelService.createModel();
			System.out.println("create projectPath:"+request.getContextPath());
			map.put("resultCode", "success");
			map.put("resultUrl", "/modeler.html?modelId=" + modelData.getId());
		} catch (Exception e) {
			log.info("创建模型失败：");
			map.put("resultCode", "error");
		}
//		String resultJson = JSONObject.toJSONString(map);
		return map.toString();
	}

	/**
	 * 查询
	 * 
	 * @return
	 */
	@RequestMapping("/findAllModel")

	public String findAllModel(HttpServletRequest request) {
		List<Model> resultList = this.modelService.findAllModelList();

		JSONObject resultJson = new JSONObject();
		resultJson.put("data", resultList);
		resultJson.put("projectName", request.getContextPath());
		return resultJson.toString();
	}

	/**
	 * 删除
	 * 
	 * @param modelId
	 * @param request
	 * @return
	 */
	@RequestMapping("/deleteModel")
	public String deleteModel(String modelId) {
		System.out.println("ModelController deleteModel: modelId = " + modelId);
		JSONObject result = new JSONObject();
		try {
			delete(modelId);
			result.put("msg", "删除成功");
			result.put("type", "success");
		} catch (Exception e) {
			result.put("msg", "删除失败");
			result.put("type", "fail");
			e.printStackTrace();
		}
		return result.toString();
	}

	/**
	 * 部署
	 */
	@RequestMapping(value = "/deploy", method = RequestMethod.POST)
	public String deploy(@RequestParam("modelId") String modelId) {
		JSONObject result = new JSONObject();
		try {
			Deployment deployment = deployByModelId(modelId);
			result.put("msg", "部署成功");
			result.put("type", "success");
			result.put("data", deployment.getId());
		} catch (Exception e) {
			result.put("msg", "部署失败");
			result.put("type", "error");
			e.printStackTrace();
		}
		return result.toString();
	}

	@RequestMapping("/onetButtonToDeploy")
	public String onetButtonToDeploy(@RequestBody String json) {
		JSONObject result = new JSONObject();
		try {

			JSONArray models = JsonUtil.get().parseArray(json);
			for (int i = 0; i < models.size(); i++) {
				JSONObject model = (JSONObject) models.get(i);
				String modelId = JsonUtil.get().getProperty(model,"id");
				deployByModelId(modelId);
			}

			result.put("msg", "部署成功");
			result.put("type", "success");
		} catch (Exception e) {
			result.put("msg", "部署失败");
			result.put("type", "error");
			e.printStackTrace();
		}
		return result.toString();
	}

	/**
	 * 一键删除多个model
	 * 
	 * @param json
	 * @return
	 */
	@RequestMapping("/oneButtonToDeleteModel")
	public String oneButtonToDeleteModel(@RequestBody String json) {
		JSONObject result = new JSONObject();
		try {
			System.out.println("ModelController oneButtonToDeleteModel models:" + json);
			JSONArray models = JsonUtil.get().parseArray(json);
			System.out.println("ModelController oneButtonToDeleteModel models.size():" + models.size());
			for (int i = 0; i < models.size(); i++) {
				JSONObject model = (JSONObject) models.get(i);
				String modelId = JsonUtil.get().getProperty(model,"id");
				delete(modelId);
			}

			result.put("msg", "删除成功");
			result.put("type", "success");
		} catch (Exception e) {
			result.put("msg", "删除失败");
			result.put("type", "error");
			e.printStackTrace();
		}
		return result.toString();
	}

	private Deployment deployByModelId(String modelId)
			throws IOException, JsonProcessingException, UnsupportedEncodingException {
		Deployment deployment = this.modelService.deploy(modelId);
		return deployment;
	}

	private void delete(String modelId) {
		this.modelService.delete(modelId);
	}

	/**
	 * 导出model的xml文件
	 */
	@RequestMapping("/export")
	public void export(String modelId, HttpServletResponse response) {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json; charset=utf-8");
		try {
			Map<String, Object> model = this.modelService.export(modelId);

			ByteArrayInputStream dataStream = model.containsKey("dataStream")
					? (ByteArrayInputStream) model.get("dataStream")
					: null;
			String filename = model.containsKey("filename") ? model.get("filename").toString() : null;

			IOUtils.copy(dataStream, response.getOutputStream());
			response.setHeader("Content-Disposition",
					"attachment; filename=" + java.net.URLEncoder.encode(filename, "UTF-8"));
			response.flushBuffer();
		} catch (Exception e) {
			PrintWriter out = null;
			try {
				out = response.getWriter();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			out.write("未找到对应数据");
			e.printStackTrace();
		}
	}

}
