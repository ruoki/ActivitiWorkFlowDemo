package com.zr.workflow.springdemo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/Hello")
public class HelloSpringController {
	String message = "Welcome to Spring MVC!";
	
	@RequestMapping("hello")
	public ModelAndView showMessage(@RequestParam(value="name",required=false,defaultValue="Spring") String name) {
		ModelAndView modelView = new ModelAndView("hellospring");//指定视图
		System.out.println("HelloSpringController showMessage name:"+name+";message:"+message);
		//向视图中添加所要展示或使用的内容，将在页面中使用
		modelView.addObject("message", message);
		modelView.addObject("name", name);
		return modelView;
	}
	
	@RequestMapping("/sayhi")
    public String sayHi(Model model) {
        model.addAttribute("message", "Hello Spring MVC!");
        return "sayhi";
    }
}
