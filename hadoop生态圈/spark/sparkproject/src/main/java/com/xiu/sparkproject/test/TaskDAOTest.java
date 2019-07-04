package com.xiu.sparkproject.test;

import com.xiu.sparkproject.dao.ITaskDAO;
import com.xiu.sparkproject.domain.Task;
import com.xiu.sparkproject.factory.DAOFactory;

/**
 * 任务管理DAO测试类
 * @author Administrator
 *
 */
public class TaskDAOTest {
	
	public static void main(String[] args) {
		ITaskDAO taskDAO = DAOFactory.getTaskDAO();
		Task task = taskDAO.findById(2);
		System.out.println(task.getTaskName());  
	}
	
}
