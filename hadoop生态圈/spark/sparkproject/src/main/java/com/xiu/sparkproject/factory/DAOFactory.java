package com.xiu.sparkproject.factory;

import com.xiu.sparkproject.dao.ISessionAggrStatDAO;
import com.xiu.sparkproject.dao.ISessionDetailDAO;
import com.xiu.sparkproject.dao.ISessionRandomExtractDAO;
import com.xiu.sparkproject.dao.ITaskDAO;
import com.xiu.sparkproject.dao.ITop10CategoryDAO;
import com.xiu.sparkproject.dao.impl.SessionAggrStatDAOImpl;
import com.xiu.sparkproject.dao.impl.SessionDetailDAOImpl;
import com.xiu.sparkproject.dao.impl.SessionRandomExtractDAOImpl;
import com.xiu.sparkproject.dao.impl.TaskDAOImpl;
import com.xiu.sparkproject.dao.impl.Top10CategoryDaoImpl;

/**
 * DAO工厂类
 * @author Administrator
 *
 */
public class DAOFactory {

	/**
	 * 获取任务管理DAO
	 * @return DAO
	 */
	public static ITaskDAO getTaskDAO() {
		return new TaskDAOImpl();
	}
	
	public static ISessionAggrStatDAO getSessionAggrStatDAO() {
		return new SessionAggrStatDAOImpl();
	}
	
	public static ISessionRandomExtractDAO getSessionRandomExtractDAO() {
		
		return new SessionRandomExtractDAOImpl();
	}
	
	public static ISessionDetailDAO getSessionDetailDAO() {
		return new SessionDetailDAOImpl();
	}
	
	public static ITop10CategoryDAO getTop10CategoryDAO() {
		return new Top10CategoryDaoImpl();
	}
	
}
