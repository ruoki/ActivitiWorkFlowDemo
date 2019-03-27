package com.zr.activiti.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


@Service
public class ModelService{

	@Resource
	private RepositoryService repositoryService;
	
	public Model createModel() throws UnsupportedEncodingException {
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectNode editorNode = objectMapper.createObjectNode();
		editorNode.put("id", "canvas");
		editorNode.put("resourceId", "canvas");
		ObjectNode stencilSetNode = objectMapper.createObjectNode();
		stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
		editorNode.put("stencilset", stencilSetNode);
		Model modelData = repositoryService.newModel();

		ObjectNode modelObjectNode = objectMapper.createObjectNode();
		modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, "lutiannan");
		modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
		String description = "lutiannan---";
		modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
		modelData.setMetaInfo(modelObjectNode.toString());
		modelData.setName("lutiannan");
		modelData.setKey("12313123");

		//保存模型
		repositoryService.saveModel(modelData);
		repositoryService.addModelEditorSource(modelData.getId(), editorNode.toString().getBytes("utf-8"));
		return modelData;
	}
	
	public List<Model> findAllModelList() {
		List<Model> resultList =  repositoryService.createModelQuery().orderByCreateTime().desc().list();
		return resultList;
	}


	public Deployment deploy(String modelId)
			throws IOException, JsonProcessingException, UnsupportedEncodingException {
		System.out.println("ModelService deploy cache dir:"+System.getProperty("java.io.tmpdir"));
		ImageIO.setUseCache(false);
//		ImageIO.setCacheDirectory(cacheDirectory);
		Model modelData = repositoryService.getModel(modelId);
 		ObjectNode modelNode = (ObjectNode) new ObjectMapper().readTree(repositoryService.getModelEditorSource(modelData.getId()));
 		byte[] bpmnBytes = null;
 		BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
 		bpmnBytes = new BpmnXMLConverter().convertToXML(model);
 		String resourceName = modelData.getName();
		String processName = resourceName + ".bpmn20.xml";
		Deployment deployment = repositoryService.createDeployment().enableDuplicateFiltering().name(resourceName).name(resourceName).addString(processName, new String(bpmnBytes,"utf-8")).deploy();
		return deployment;
	}
	

	public void delete(String modelId) {
		repositoryService.deleteModel(modelId);
	}

	public Map<String,Object> export(String modelId) throws IOException, JsonProcessingException {
		Map<String,Object> modelMap = new HashMap<>();
		Model modelData = repositoryService.getModel(modelId);
		 BpmnJsonConverter jsonConverter = new BpmnJsonConverter();
		 //获取节点信息
		 byte[] arg0 = repositoryService.getModelEditorSource(modelData.getId());
		 JsonNode editorNode = new ObjectMapper().readTree(arg0);
		 //将节点信息转换为xml
		 BpmnModel bpmnModel = jsonConverter.convertToBpmnModel(editorNode);
		 BpmnXMLConverter xmlConverter = new BpmnXMLConverter();
		 byte[] bpmnBytes = xmlConverter.convertToXML(bpmnModel);

		 ByteArrayInputStream dataStream = new ByteArrayInputStream(bpmnBytes);
//           String filename = bpmnModel.getMainProcess().getId() + ".bpmn20.xml";
		 String filename = modelData.getName() + ".bpmn20.xml";

		 modelMap.put("filename", filename);
		 modelMap.put("dataStream", dataStream);
		return modelMap;
	}

}
