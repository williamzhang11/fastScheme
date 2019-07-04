package com.xiu.sparkproject.conf;
/**
 * 配置管理组件
 * @author william
 *
 */








import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationManager {
	
	private static Properties prop = new Properties();
	
	static {
		try {
			InputStream in = ConfigurationManager.class.getClassLoader().
					getResourceAsStream("my.properties");
			prop.load(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String getProperty(String key) {
		return prop.getProperty(key);
	}
	
	public static Integer getInteger(String key) {
		String value = prop.getProperty(key);
		try {
			return Integer.valueOf(value);
		}catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static Boolean getBoolean(String key) {
		 String value = getProperty(key);
		 try {
			 return Boolean.valueOf(value);
		 }catch (Exception e) {
			 e.printStackTrace();
		}
		 return false;
	}
	
	
}
