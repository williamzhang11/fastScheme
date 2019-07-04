package com.xiu.sparkproject.spark;

import org.apache.spark.AccumulatorParam;

import com.xiu.sparkproject.constant.Constants;
import com.xiu.sparkproject.util.StringUtils;

/**
 * session聚合统计accumulator
 * zero ，用于数据初始化，返回初始化所有范围区间的数据都是0
 * 
 * addInPlace,addAccumulator基本是一样的
 * v1是初始化的连接串
 * v2是在遍历session时，判断出某个session对应的区间,要做的事找到v2对应的value，然后+1，更回到连接串
 * @author william
 *
 */
public class SessionAggrStatAccumulator implements AccumulatorParam<String>{

	public String addInPlace(String v1, String v2) {
		// TODO Auto-generated method stub
		return add(v1, v2);
	}

	public String zero(String v) {
		return Constants.SESSION_COUNT + "=0|"
				+ Constants.TIME_PERIOD_1s_3s + "=0|"
				+ Constants.TIME_PERIOD_4s_6s + "=0|"
				+ Constants.TIME_PERIOD_7s_9s + "=0|"
				+ Constants.TIME_PERIOD_10s_30s + "=0|"
				+ Constants.TIME_PERIOD_30s_60s + "=0|"
				+ Constants.TIME_PERIOD_1m_3m + "=0|"
				+ Constants.TIME_PERIOD_3m_10m + "=0|"
				+ Constants.TIME_PERIOD_10m_30m + "=0|"
				+ Constants.TIME_PERIOD_30m + "=0|"
				+ Constants.STEP_PERIOD_1_3 + "=0|"
				+ Constants.STEP_PERIOD_4_6 + "=0|"
				+ Constants.STEP_PERIOD_7_9 + "=0|"
				+ Constants.STEP_PERIOD_10_30 + "=0|"
				+ Constants.STEP_PERIOD_30_60 + "=0|"
				+ Constants.STEP_PERIOD_60 + "=0";
	}

	public String addAccumulator(String v1, String v2) {
		// TODO Auto-generated method stub
		return add(v1, v2);
	}
	
	/**
	 * session统计计算逻辑
	 * @param v1 连接串
	 * @param v2 范围区间
	 * @return 更新以后的连接串
	 */
	private String add(String v1, String v2) {
		// 校验：v1为空的话，直接返回v2
		if(StringUtils.isEmpty(v1)) {
			return v2;
		}
		
		// 使用StringUtils工具类，从v1中，提取v2对应的值，并累加1
		String oldValue = StringUtils.getFieldFromConcatString(v1, "\\|", v2);
		if(oldValue != null) {
			// 将范围区间原有的值，累加1
			int newValue = Integer.valueOf(oldValue) + 1;
			// 使用StringUtils工具类，将v1中，v2对应的值，设置成新的累加后的值
			return StringUtils.setFieldInConcatString(v1, "\\|", v2, String.valueOf(newValue));  
		}
		
		return v1;
	}

}
