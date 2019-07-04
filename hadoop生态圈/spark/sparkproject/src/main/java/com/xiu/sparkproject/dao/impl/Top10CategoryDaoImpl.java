package com.xiu.sparkproject.dao.impl;

import com.xiu.sparkproject.dao.ITop10CategoryDAO;
import com.xiu.sparkproject.domain.Top10Category;
import com.xiu.sparkproject.jdbc.JDBCHelper;

public class Top10CategoryDaoImpl implements ITop10CategoryDAO{

	public void insert(Top10Category category) {
		String sql = "insert into top10_category values(?,?,?,?,?)";
		
		Object[] params = new Object[] {category.getTaskid(),category.getCategoryid(),
				category.getClickCount(),category.getOrderCount(),category.getPayCount()
				};
		
		JDBCHelper jdbcHelper =JDBCHelper.getInstance();
		jdbcHelper.executeUpdate(sql, params);
	}

}

















