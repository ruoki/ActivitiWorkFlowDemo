package com.zr.activiti.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.zr.activiti.dao.UserDAO;
import com.zr.activiti.entity.User;
import com.zr.activiti.service.UserService;
import com.zr.activiti.utils.UUIDUtil;

public class TestUserService {
	
	static UserService userService;
	

	@Test
	public void testUserDAO() {
		//在当前上下文中获得Spring容器
        ApplicationContext ctx=new ClassPathXmlApplicationContext("spring/spring-mybatis.xml");
        UserDAO userDAO=ctx.getBean(UserDAO.class);
        

		String userId = UUIDUtil.uuid();
    	System.out.println("TestUserService testUserDAO userId:"+userId);
        User user1 =new User();
        user1.setUserId(userId);
        user1.setUserCode("zhangsan");
        user1.setUserName("张三");
        System.out.println("插入user:"+userDAO.add(user1));
        //访问数据库
        List<User> users=userDAO.getAllUsers();
        for (User user : users) {
            System.out.println("user:"+user);
        }
        assertNotNull(users);
	}
	
	
	@BeforeClass
	public static void before() {
		//在当前上下文中获得Spring容器，首先确认spring-mybatis.xml文件中已经有扫描UserService的配置，
		//因为目前spring-mybatis.xml只配置了com.zr.activiti.dao包，如果要测试UserService中的方法，则加上:
		//<context:component-scan base-package="com.zr.activiti"></context:component-scan>
        ApplicationContext ctx=new ClassPathXmlApplicationContext("spring/spring-mybatis.xml");
        userService=ctx.getBean(UserService.class);
	}
	
	
	@Test
	public void testGetAllUsers() {
		List<User> users = userService.getAllUsers();
		assertNotNull(users);
	}
	

    @Test
    public void testAdd() {
		String userId = UUIDUtil.uuid();
    	System.out.println("TestUserService testAdd userId:"+userId);
        User user=new User();
        user.setUserId(userId);
        user.setUserCode("zhangsan");
        user.setUserName("张三");
        try {
            assertEquals(1, userService.add(user));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDelete() {
        assertEquals(1, userService.delete("123"));
    }

    @Test
    public void testDeleteStringArray() {
        String[] ids={"123"};
        assertEquals(3, userService.delete(ids));
    }

    @Test
    public void testUpdate() {
		String userId = UUIDUtil.uuid();
        User user=new User();
        user.setUserId(userId);
        user.setUserCode("zhangsan");
        user.setUserName("张三");
        try {
            assertEquals(1, userService.update(user));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testGetBookById(){
        assertNotNull(userService.getUserByCode("zhangsan"));
    }
    
    @Test
    public void testAddDouble(){
        //因为userId相同，添加第二个人会失败，用于测试事务
		String userId = UUIDUtil.uuid();
        User user=new User();
        user.setUserId(userId);
        user.setUserCode("wangwu");
        user.setUserName("王五");
//		String userId2 = UUIDUtil.uuid();
        User user2=new User();
        user.setUserId(userId);
        user.setUserCode("lisi");
        user.setUserName("李四");
        assertEquals(2, userService.add(user, user2));
    }
}
