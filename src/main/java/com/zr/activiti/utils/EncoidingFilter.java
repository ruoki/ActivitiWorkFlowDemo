package com.zr.activiti.utils;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * 编码过滤器
 * @author Administrator
 *
 */
public class EncoidingFilter implements Filter {

	private String encoding="";

	/**
	 * 根据web.xml文件的配置进行初始化
	 */
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
        this.encoding = arg0.getInitParameter("Encoding");
	}

	/**
	 * 过滤方法  是否往下执行
	 */
	public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain chain)
			throws IOException, ServletException {
		// TODO Auto-generated method stub
        HttpServletRequest request=(HttpServletRequest)arg0;
        HttpServletResponse response=(HttpServletResponse)arg1;
        
        request.setCharacterEncoding(encoding);
        response.setCharacterEncoding(encoding);

        //过滤通行证
        chain.doFilter(request, response);

	}

	public void destroy() {
		// TODO Auto-generated method stub

	}

}
