package com.zr.workflow.activiti.controller;

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
import com.zr.workflow.activiti.service.ModelService;
import com.zr.workflow.activiti.util.GFJsonUtil;

/**
 * 流程模型
 * @author zhourq
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
		Map<String, String> resultMap = new HashMap<>();
		try {
			Model modelData = this.modelService.createModel();
			System.out.println("create projectPath:"+request.getContextPath());
			resultMap.put("resultCode", "success");
			resultMap.put("resultUrl", "/modeler.html?modelId=" + modelData.getId());
		} catch (Exception e) {
			log.info("创建模型失败：");
			resultMap.put("resultCode", "error");
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 查询
	 * 
	 * @return
	 */
	@RequestMapping("/findAllModel")
	public String findAllModel(HttpServletRequest request) {
		List<Model> resultList = this.modelService.findAllModelList();

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("data", resultList);
		resultMap.put("projectName", request.getContextPath());
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
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
		Map<String, Object> resultMap = new HashMap<>();
		try {
			delete(modelId);
			resultMap.put("msg", "删除成功");
			resultMap.put("type", "success");
		} catch (Exception e) {
			resultMap.put("msg", "删除失败");
			resultMap.put("type", "fail");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 部署
	 */
	@RequestMapping(value = "/deploy", method = RequestMethod.POST)
	public String deploy(@RequestParam("modelId") String modelId) {
		Map<String, String> resultMap = new HashMap<>();
		try {
			Deployment deployment = deployByModelId(modelId);
			resultMap.put("msg", "部署成功");
			resultMap.put("type", "success");
			resultMap.put("data", deployment.getId());
		} catch (Exception e) {
			resultMap.put("msg", "部署失败");
			resultMap.put("type", "error");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	@RequestMapping("/onetButtonToDeploy")
	public String onetButtonToDeploy(@RequestBody String json) {
		Map<String, String> resultMap = new HashMap<>();
		try {
			JSONArray models = GFJsonUtil.get().parseArray(json);
			for (int i = 0; i < models.size(); i++) {
				JSONObject model = (JSONObject) models.get(i);
				String modelId = GFJsonUtil.get().getProperty(model,"id");
				deployByModelId(modelId);
			}

			resultMap.put("msg", "部署成功");
			resultMap.put("type", "success");
		} catch (Exception e) {
			resultMap.put("msg", "部署失败");
			resultMap.put("type", "error");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
	}

	/**
	 * 一键删除多个model
	 * 
	 * @param json
	 * @return
	 */
	@RequestMapping("/oneButtonToDeleteModel")
	public String oneButtonToDeleteModel(@RequestBody String json) {
		Map<String, String> resultMap = new HashMap<>();
		try {
			JSONArray models = GFJsonUtil.get().parseArray(json);
			for (int i = 0; i < models.size(); i++) {
				JSONObject model = (JSONObject) models.get(i);
				String modelId = GFJsonUtil.get().getProperty(model,"id");
				delete(modelId);
			}

			resultMap.put("msg", "删除成功");
			resultMap.put("type", "success");
		} catch (Exception e) {
			resultMap.put("msg", "删除失败");
			resultMap.put("type", "error");
			e.printStackTrace();
		}
		String resultJson = GFJsonUtil.get().toJson(resultMap);
		return resultJson;
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
