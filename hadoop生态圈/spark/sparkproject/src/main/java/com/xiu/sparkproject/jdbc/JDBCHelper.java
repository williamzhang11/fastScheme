package com.xiu.sparkproject.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.xiu.sparkproject.conf.ConfigurationManager;
import com.xiu.sparkproject.constant.Constants;
import com.yammer.metrics.core.HealthCheck.Result;

/*
 * JDBC辅助组件
 */
public class JDBCHelper {

	static {
		//第一步：加载JDBC驱动
		try {
			String driver = ConfigurationManager.getProperty(Constants.JDBC_DRIVER);
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//第二步：实现JDBCHelper单例化，为了保证只有一份数据库连接池
	private static JDBCHelper instance = null;
	
	private LinkedList<Connection> datasource = new LinkedList<Connection>();
	private JDBCHelper() {
		//第一步：获取数据库连接池大小
		int datasourcesize = ConfigurationManager.getInteger(
				Constants.JDBC_DATASOURCE_SIZE);
		//第二步：创建指定数量的数据库连接，并放入数据库连接池中
		
		String url = ConfigurationManager.getProperty(Constants.JDBC_URL);
		String user = ConfigurationManager.getProperty(Constants.JDBC_USER);
		String password = ConfigurationManager.getProperty(Constants.JDBC_PASSWORD);
		//第三步：创建数据库连接时，放入数据库连接池
		for(int i=0;i<datasourcesize;i++) {
			try {
				Connection conn = DriverManager.getConnection(url, user, password);
				datasource.push(conn); 	
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	//第四步：提供获取数据库连接的方法，有可能，获取的时候连接用光了，暂时获取不到数据库连接
	//因此需要编码实现一个等待机制，去等待获取数据库连接
	public synchronized Connection getConnection() {
		
		while(datasource.size() ==0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return datasource.poll();
	}
	
	
	public static JDBCHelper getInstance() {
		if(instance == null) {
			synchronized(JDBCHelper.class) {
				if(instance == null) {
					instance = new JDBCHelper();
				}
			}
		}
		
		return instance;
	}
	/**
	 *第五步：开发增删改查方法
	 * 1.执行增删改sql语句方法
	 * 2.执行查询sql语句方法
	 * 3.批量执行sql语句方法 
	 */
	
	/**
	 * 执行增删改sql语句
	 * @param sql
	 * @param params
	 * @return 影响的行数
	 */
	public int executeUpdate(String sql,Object[] params) {
		int rtn = 0;
		Connection conn =null;
		PreparedStatement pstmt = null;
		try {
			conn = getConnection();
			pstmt = conn.prepareStatement(sql);
			
			for(int i=0;i<params.length;i++) {
				pstmt.setObject(i+1, params[i]);
			}
			rtn = pstmt.executeUpdate();
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(conn != null) {
				datasource.push(conn);
			}
		}
		return rtn;
	}
	//执行查询语句
	public void executeQuery(String sql,Object[] params,QueryCallback callback) {
		Connection conn = null;
		PreparedStatement pstmt=null;
		ResultSet rs =null;
		try {
			conn = getConnection();
			pstmt = conn.prepareStatement(sql);
			for(int i=0;i<params.length;i++) {
				pstmt.setObject(i+1, params[i]);
			}
			rs = pstmt.executeQuery();
			callback.process(rs);
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(conn !=null) {
				datasource.push(conn);
			}
		}
		
	}
	/**
	 * 批量执行sql语句
	 * @param sql
	 * @param paramsList
	 * @return
	 */
	public  int[] executeBatch(String sql , List<Object[]> paramsList) {
		int[] rtn=null;
		Connection conn = null;
		PreparedStatement pstmt=null;
		
		try {
			conn = getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(sql);
			for(Object[] params:paramsList) {
				for(int i=0;i<params.length;i++)
				pstmt.setObject(i+1, params[i]);
			}
			pstmt.addBatch();
			
			rtn = pstmt.executeBatch();
			conn.commit();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rtn;
		
		
	}
	public static interface QueryCallback{
		void process(ResultSet rs) throws Exception;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
