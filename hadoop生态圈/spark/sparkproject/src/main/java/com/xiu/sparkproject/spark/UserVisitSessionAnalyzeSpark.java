package com.xiu.sparkproject.spark;

import java.util.Date;
import java.util.Iterator;

import org.apache.hadoop.hdfs.server.datanode.tail_jsp;
import org.apache.hadoop.hive.metastore.StatObjectConverter;
import org.apache.hadoop.hive.ql.parse.HiveParser.rowFormat_return;
import org.apache.spark.Accumulator;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.catalyst.expressions.aggregate.Final;
import org.apache.spark.sql.hive.HiveContext;

import com.alibaba.fastjson.JSONObject;
import com.xiu.sparkproject.conf.ConfigurationManager;
import com.xiu.sparkproject.constant.Constants;
import com.xiu.sparkproject.dao.ISessionAggrStatDAO;
import com.xiu.sparkproject.dao.ITaskDAO;
import com.xiu.sparkproject.domain.SessionAggrStat;
import com.xiu.sparkproject.domain.Task;
import com.xiu.sparkproject.factory.DAOFactory;
import com.xiu.sparkproject.test.MockData;
import com.xiu.sparkproject.util.DateUtils;
import com.xiu.sparkproject.util.NumberUtils;
import com.xiu.sparkproject.util.ParamUtils;
import com.xiu.sparkproject.util.StringUtils;
import com.xiu.sparkproject.util.ValidUtils;

import scala.Tuple2;


/**
 * 用户访问session分析Spark作业
 * @author Administrator
 *
 */
public class UserVisitSessionAnalyzeSpark {

	public static void main(String[] args) {
		
	args = new String[] {"2"};
		
		//构建spark上下文
	SparkConf conf = new SparkConf()
			.setAppName(Constants.SPARK_APP_NAME_SESSION)
			.setMaster("local");
	//构建
		
	JavaSparkContext sc = new JavaSparkContext(conf);
	 SQLContext sqlContext = getSQLContext(sc.sc());
	 
	mockData(sc,sqlContext);
	ITaskDAO taskDAO = DAOFactory.getTaskDAO();
	long taskid = ParamUtils.getTaskIdFromArgs(args);
	Task task = taskDAO.findById(taskid);
	JSONObject taskParam = JSONObject.parseObject(task.getTaskParam());
	
	JavaRDD<Row> actionRDD = getActionRDDByDateRange(sqlContext, taskParam);
	//首先将行为数据，按照session_id进行groupByKey分组
	//此时的数据的粒度就是session粒度了，然后将session粒度的数据与用户信息数据，进行join,就可以 获取到session粒度的数据 
	//同时数据里面包含了session对应的user信息 
	 //获取的数据是<sessionid,(sessionid,searchKeywords,clickCategoryIds,age,professional)>
	JavaPairRDD<String, String > sessionid2AggrInfoRDD = aggregateBySession(sqlContext, actionRDD);
	System.out.println(sessionid2AggrInfoRDD.count());
	for(Tuple2<String, String> tuple : sessionid2AggrInfoRDD.take(10)) {
		System.out.println(tuple._2);
	}
	
	//接着针对session粒度的聚合数据，按照使用者指定的筛选参数进行数据过滤
	//相当于自己编写的算子要访问外面的任务参数对象
	//匿名内部类（算子函数），访问外部对象，要给外部对象使用final修饰的
	//同时进行过滤和统计
	Accumulator<String>sessionAggrStatAccumulator = sc.accumulator("", 
			new SessionAggrStatAccumulator());
	
	
	
	JavaPairRDD<String, String> filteredSessionid2AgrInfoRDD = 
			filterSessionAndAggrStat(
					sessionid2AggrInfoRDD, taskParam,sessionAggrStatAccumulator);
	
	System.out.println(filteredSessionid2AgrInfoRDD.count());
	for(Tuple2<String, String> tuple : filteredSessionid2AgrInfoRDD.take(10)) {
		System.out.println(tuple._2);
	} 	
	filteredSessionid2AgrInfoRDD.count();
	calcuteAndPersistAggrStat(sessionAggrStatAccumulator.value(),task.getTaskid());
	/**
	 * session聚合统计，统计出访问时长和步长，各个区间session数量占总session数量的比例
	 * 不重构，实现思路：
	 * 1.actionRDD,映射成<sessionid,Row>格式
	 * 2.按sessionid聚合，计算出每个session的访问时长和访问步长
	 * 3.遍历新生产的RDD,将每个session的访问时长和访问步长，去更新自定义Accumulator中对应的值
	 * 4.使用自定义Accumulator中统计值，计算各区间比例	
	 * 5.将最后计算结果写入表中
	 * 
	 * 重构思路：
	 * 1.不要去生成任何新的RDD(处理上亿条数据)
	 * 2.不要去单独遍历一遍session数据
	 * 3.可以在进行session聚合时，直接计算出每个session的访问步长和时长
	 * 4.进行过滤时，本来就要遍历所有聚合session信息，可以在某个session通过筛选条件后将其
	 * 访问时长和步长，累加到自定义Accumulator上
	 * 准则：
	 * 1.尽量少生成RDD
	 * 2.尽量少对RDD进行算子操作，如果有可能，尽量在一个算子里实现多个需要做的功能
	 * 3.尽量少对rdd进行shuffle算子操作，如groupbykey,reducebykey,sortbykey
	 * 
	 */
	
	
	sc.close();
		
		
		
	
	
	
	
		
		
		
	}	
	/**
	 * 获取SQLContext
	 * 如果是在本地环境，生成SQLContext
	 * 如果是在生产环境，生成hiveContext
	 * @param sc
	 * @return
	 */
	private static SQLContext getSQLContext(SparkContext sc) {
		
		boolean local = ConfigurationManager.getBoolean(Constants.SPARK_LOCAL);
		if(local) {
			return new SQLContext(sc);
		}else {
			return new HiveContext(sc);
		}
		
	}
	
	private static void mockData(JavaSparkContext sc,SQLContext sqlContext) {
		boolean local = ConfigurationManager.getBoolean(Constants.SPARK_LOCAL);		
		if(local) {
			MockData.mock(sc, sqlContext);
		}
	}
	/**
	 * 获取指定范围内容的用户指定行为数据
	 * @param sqlContext
	 * @param taskParam
	 * @return
	 */
	private static JavaRDD<Row> getActionRDDByDateRange(
			SQLContext sqlContext,JSONObject taskParam){
		
		String startDate = ParamUtils.getParam(taskParam, Constants.PARAM_START_DATE);
		String endDate = ParamUtils.getParam(taskParam, Constants.PARAM_END_DATE);
		
		String sql = "select *"
				+ "from user_visit_action "
				+ "where date >= '"+startDate+"'"+"and date <= '"+endDate+"'";
		DataFrame actionDF = sqlContext.sql(sql);
		
		return actionDF.javaRDD();
	}
	/**
	 * 对行为数据按session粒度进行分组聚合
	 * @param actionRDD 行为数据RDD
	 * @return session粒度聚合数据
	 */
	private static JavaPairRDD<String, String> aggregateBySession(
			SQLContext sqlContext,JavaRDD<Row>actionRDD){
		//现在actionRDD中的元素是Row,一个row就是一行用户访问行为记录，比如一次点击或搜索
		//现在需要将row映射成<session,row>的格式 
		
		JavaPairRDD<String, Row> session2ActionRDD = actionRDD.mapToPair(
				/**
				 * PariFunction
				 * 第一个参数，函数的输入
				 * 第二，三参数，函数的输出（Tuple）,分别是Tuple第一个第二个值
				 */
				new PairFunction<Row, String, Row>() {

					public Tuple2<String, Row> call(Row row) throws Exception {
						// TODO Auto-generated method stub
						return new Tuple2<String, Row>(row.getString(2), row);
					}
		});
		
		//对行为数据按session粒度进行分组
		JavaPairRDD<String, Iterable<Row>> session2ActionsRDD = 
				session2ActionRDD.groupByKey();
		//对每一个session分组进行聚合，将session中所有的搜索词和点击品类都聚合起来 
		//获取的数据格式如下<userid,parAffrInfo(sessionid,searchKeyWords,clickCategoryIds)>
		JavaPairRDD<Long, String> userid2PartAggrInfoRDD= session2ActionsRDD.mapToPair(
				new PairFunction<Tuple2<String,Iterable<Row>>, Long, String>() {

					public Tuple2<Long, String> call(Tuple2<String, Iterable<Row>> tuple) throws Exception {
						// TODO Auto-generated method stub
						String sessionid = tuple._1;
						Iterator<Row> iterator = tuple._2.iterator();
						
						StringBuffer searchKeywordsBuffer = new StringBuffer("");
						StringBuffer clickCategoryIdsBuffer = new StringBuffer("");
						
						Long userid =null;
						
						Date startTime = null;
						Date endTime = null;
						int stepLength = 0;
						
						
						//遍历session所有的访问行为
						while(iterator.hasNext()) {
							//提取每个访问行为的搜索词字段和点击品类字段
							Row row = iterator.next();
							if(userid == null) {
								userid=row.getLong(1);
							}
							String searchKeyword = row.getString(5);
							Long clickCategoryId = row.getLong(6);
							//并不是每一行访问行为都有searchkeyword和clickcategoryId2个字段的
							//只有搜索行为有searchkeyword
							//只有点击品类行为有clickCategoryId字段
							//所以任何一行行为数据都不可能有2个字段都有，可能出现null值的
							//决定是否将搜索词或点击品类id拼接到字符串中去
							//首先要满足不能是null值
							//其次，之前字符串中还没有搜索词或点击品类id
							
							if(clickCategoryId !=null) {
								if(!clickCategoryIdsBuffer.toString().contains(String.valueOf(clickCategoryId))) {
									clickCategoryIdsBuffer.append(clickCategoryId+",");
								}
							}
							//计算session开始和结束时间
							Date actionTime = DateUtils.parseTime(row.getString(4));
							if(startTime == null) {
								startTime = actionTime;
							}
							if(endTime == null) {
								endTime = actionTime;
							}
							if(actionTime.before(startTime)) {
								startTime = actionTime;
							}
							if(actionTime.after(endTime)) {
								endTime = actionTime;
							}
							
							//计算session访问步长
							stepLength ++;
							
							
							if(StringUtils.isNotEmpty(searchKeyword)) {
								if(!searchKeywordsBuffer.toString().contains(searchKeyword)) {
									searchKeywordsBuffer.append(searchKeyword+",");
								}
							}
						}
						String searchKeywords = StringUtils.trimComma(searchKeywordsBuffer.toString());
						
						String clickCategoryIds = StringUtils.trimComma(clickCategoryIdsBuffer.toString());
						
						long visitLength = (endTime.getTime()-startTime.getTime())/1000;//秒
						
						
						
						//返回的数据即，<sessionid,parAggrI>
						//但这一步聚合完，需要将每一行数据跟对应的用户信息聚合
						//如果与用户信息聚合，那么key不应该是sessionid
						//应该是userid才能跟<userid,row>格式的用户信息进行聚合
						//如果返回<session,partAffrInfo>还得再做一次maptoPair算子将RDD映射成<userId,partAggrInfo>格式，就多次一举
						//因此可以直接返回的数据格式就是<userid,partAggrInfo>,然后跟用户信息join时，将partAggrInfo关联
						//上userInfo，然后直接返回Tuple的key设置成sessionid
						//最后的数据格式，还是<sessionid,fullAggrInfo>
						//聚合数据用什么样格式拼接？统一定义key=value|key=value
						String partAggrInfo = Constants.FIELD_SESSION_ID +"="+ sessionid + "|"
						+Constants.FIELD_SEARCH_KEYWORDS + "="+searchKeywords+"|" 
						+Constants.FIELD_CLICK_CATEGORY_IDS+"="+clickCategoryIds
						+Constants.FIELD_VISIT_LENGTH+"="+visitLength
						+Constants.FIELD_STEP_LENGTH+"="+stepLength
						;
						return new Tuple2<Long, String>(userid, partAggrInfo);
					}
		});
		
		//查询所有用户数据,并映射成<userid,row>格式
		String sql = "select *from user_info";
		JavaRDD<Row> userInfoRDD = sqlContext.sql(sql).javaRDD();
		JavaPairRDD<Long, Row> userid2InfoRDD = userInfoRDD.mapToPair(
				new PairFunction<Row, Long, Row>() {

					public Tuple2<Long, Row> call(Row row) throws Exception {
						// TODO Auto-generated method stub
						return new Tuple2<Long, Row>(row.getLong(0), row);
					}
		});
		//将session粒度聚合数据与用户信息j进行join
		JavaPairRDD<Long, Tuple2<String, Row>> userid2FullInfoRDD = userid2PartAggrInfoRDD.join(userid2InfoRDD);
		//对join起来的数据进行拼接，并且返回<sessionid,fullAggrInfo>格式数据
		JavaPairRDD<String, String> sessionid2FullAffrInfoRDD = userid2FullInfoRDD.mapToPair(
				new PairFunction<Tuple2<Long,Tuple2<String,Row>>, String, String>() {

					public Tuple2<String, String> call(
							Tuple2<Long, Tuple2<String, Row>> tuple)
									throws Exception {
						// TODO Auto-generated method stub
						String partAggrInfo = tuple._2._1;
						Row userInfoRow = tuple._2._2;
						String sessionid = StringUtils.getFieldFromConcatString(
								partAggrInfo, "\\|", Constants.FIELD_SESSION_ID);
						
						int age = userInfoRow.getInt(3);
						String professional = userInfoRow.getString(4);
						String city = userInfoRow.getString(5);
						String sex = userInfoRow.getString(6);
						
						String fullAggrInfo = partAggrInfo+"|"
								+Constants.FIELD_AGE+"="+age+"|"
								+Constants.FIELD_PROFESSIONAL+"="+professional+"|"
								+Constants.FIELD_CITY+"="+city+"|"
								+Constants.FIELD_SEX+"="+sex;
						
						
						
						
						return new Tuple2<String, String>(sessionid, fullAggrInfo);
					}
					
		});
		return sessionid2FullAffrInfoRDD;
	}
	/**
	 * 过滤session数据
	 * @param sessionid2AggrInfoRDD
	 * @return
	 */
	private static JavaPairRDD<String, String> filterSessionAndAggrStat(
			JavaPairRDD<String, String> sessionid2AggrInfoRDD,
			final JSONObject taskParam,
			final Accumulator<String>sessionAggrStatAccumulator){
		
		String startAge = ParamUtils.getParam(taskParam, Constants.PARAM_START_AGE);
		String endAge = ParamUtils.getParam(taskParam, Constants.PARAM_END_AGE);
		String proferrionals = ParamUtils.getParam(taskParam, Constants.PARAM_PROFESSIONALS);
		String cities = ParamUtils.getParam(taskParam, Constants.PARAM_CIRYS);
		String sex = ParamUtils.getParam(taskParam, Constants.PARAM_SEX);
		String keywords = ParamUtils.getParam(taskParam, Constants.PARAM_KEYWORDS);
		String categoryIds = ParamUtils.getParam(taskParam, Constants.PARAM_CATEGORY_IDS);
		
		String _parameter = (startAge!=null ? Constants.PARAM_START_AGE + "=" +startAge+"|":"")
				+ (endAge!=null ? Constants.PARAM_END_AGE + "=" +endAge+"|":"")
				+ (proferrionals!=null ? Constants.PARAM_PROFESSIONALS + "=" +proferrionals+"|":"")
				+ (cities!=null ? Constants.PARAM_CIRYS + "=" +cities+"|":"")
				+ (sex!=null ? Constants.PARAM_SEX + "=" +sex+"|":"")
				+ (keywords!=null ? Constants.PARAM_KEYWORDS + "=" +keywords+"|":"")
				+ (categoryIds!=null ? Constants.PARAM_CATEGORY_IDS + "=" +categoryIds:"");
		
		if(_parameter.endsWith("\\|")) {
			_parameter = _parameter.substring(0,_parameter.length() -1);
		}
		final String parameter = _parameter;
		
		JavaPairRDD<String, String> filteredSessionid2AggrInfoRDD = sessionid2AggrInfoRDD.filter(
				new Function<Tuple2<String,String>, Boolean>() {
			
			public Boolean call(Tuple2<String, String> tuple) throws Exception {
				// TODO Auto-generated method stub
				//首先从tuple中获取聚合数据
				String aggrInfo = tuple._2;
				//接着依此按照筛选条件进行过滤
				//按照年龄范围进行过滤（startAge,endAge）
				//按照年龄范围过滤
				if(!ValidUtils.between(aggrInfo, Constants.FIELD_AGE, parameter, Constants.PARAM_START_AGE, Constants.PARAM_END_AGE)) {
					
					return false;
					
				}
				//职业范围过滤
				if(!ValidUtils.in(aggrInfo, Constants.FIELD_PROFESSIONAL, parameter, Constants.PARAM_PROFESSIONALS)) {
					
					return false;
					
				}
				//城市范围过滤
				if(!ValidUtils.in(aggrInfo, Constants.FIELD_CITY, parameter, Constants.PARAM_CIRYS)) {
					
					return false;
					
				}
				//性别过滤
				if(!ValidUtils.equal(aggrInfo, Constants.FIELD_SEX, parameter, Constants.PARAM_SEX)) {
					
					return false;
					
				}
				//按照搜索词过滤
				if(!ValidUtils.in(aggrInfo, Constants.FIELD_SEARCH_KEYWORDS, parameter, Constants.PARAM_KEYWORDS)) {
					
					return false;
					
				}
				//按照品类id过滤
				if(!ValidUtils.in(aggrInfo, Constants.FIELD_CLICK_CATEGORY_IDS, parameter, Constants.PARAM_CATEGORY_IDS)) {
					
					return false;
					
				}
				
				sessionAggrStatAccumulator.add(Constants.SESSION_COUNT);
				//计算出session的访问时长和访问步长范围进行累加
				long visitLength = Long.valueOf(StringUtils.getFieldFromConcatString(
						aggrInfo, "\\|", Constants.FIELD_VISIT_LENGTH)); 
				long stepLength = Long.valueOf(StringUtils.getFieldFromConcatString(
						aggrInfo, "\\|", Constants.FIELD_STEP_LENGTH));  
				
				calculateVisitLength(visitLength); 
				calculateStepLength(stepLength);  
				
				return true;
			}
			private void calculateVisitLength(long visitLength) {
				if(visitLength >=1 && visitLength<=3) {
					sessionAggrStatAccumulator.add(Constants.TIME_PERIOD_1s_3s);
				}else if(visitLength >=4 && visitLength <= 6) {
					sessionAggrStatAccumulator.add(Constants.TIME_PERIOD_4s_6s);  
				} else if(visitLength >=7 && visitLength <= 9) {
					sessionAggrStatAccumulator.add(Constants.TIME_PERIOD_7s_9s);  
				} else if(visitLength >=10 && visitLength <= 30) {
					sessionAggrStatAccumulator.add(Constants.TIME_PERIOD_10s_30s);  
				} else if(visitLength > 30 && visitLength <= 60) {
					sessionAggrStatAccumulator.add(Constants.TIME_PERIOD_30s_60s);  
				} else if(visitLength > 60 && visitLength <= 180) {
					sessionAggrStatAccumulator.add(Constants.TIME_PERIOD_1m_3m);  
				} else if(visitLength > 180 && visitLength <= 600) {
					sessionAggrStatAccumulator.add(Constants.TIME_PERIOD_3m_10m);  
				} else if(visitLength > 600 && visitLength <= 1800) {  
					sessionAggrStatAccumulator.add(Constants.TIME_PERIOD_10m_30m);  
				} else if(visitLength > 1800) {
					sessionAggrStatAccumulator.add(Constants.TIME_PERIOD_30m);  
				} 
			}
			
			/**
			 * 计算访问步长范围
			 * @param stepLength
			 */
			private void calculateStepLength(long stepLength) {
				if(stepLength >= 1 && stepLength <= 3) {
					sessionAggrStatAccumulator.add(Constants.STEP_PERIOD_1_3);  
				} else if(stepLength >= 4 && stepLength <= 6) {
					sessionAggrStatAccumulator.add(Constants.STEP_PERIOD_4_6);  
				} else if(stepLength >= 7 && stepLength <= 9) {
					sessionAggrStatAccumulator.add(Constants.STEP_PERIOD_7_9);  
				} else if(stepLength >= 10 && stepLength <= 30) {
					sessionAggrStatAccumulator.add(Constants.STEP_PERIOD_10_30);  
				} else if(stepLength > 30 && stepLength <= 60) {
					sessionAggrStatAccumulator.add(Constants.STEP_PERIOD_30_60);  
				} else if(stepLength > 60) {
					sessionAggrStatAccumulator.add(Constants.STEP_PERIOD_60);    
				}
			}
			
		});
		
		return filteredSessionid2AggrInfoRDD;
	}
	
	private static void calcuteAndPersistAggrStat(String value,long taskid) {
		// 从Accumulator统计串中获取值
				long session_count = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.SESSION_COUNT));  
				
				long visit_length_1s_3s = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.TIME_PERIOD_1s_3s));  
				long visit_length_4s_6s = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.TIME_PERIOD_4s_6s));
				long visit_length_7s_9s = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.TIME_PERIOD_7s_9s));
				long visit_length_10s_30s = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.TIME_PERIOD_10s_30s));
				long visit_length_30s_60s = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.TIME_PERIOD_30s_60s));
				long visit_length_1m_3m = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.TIME_PERIOD_1m_3m));
				long visit_length_3m_10m = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.TIME_PERIOD_3m_10m));
				long visit_length_10m_30m = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.TIME_PERIOD_10m_30m));
				long visit_length_30m = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.TIME_PERIOD_30m));
				
				long step_length_1_3 = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.STEP_PERIOD_1_3));
				long step_length_4_6 = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.STEP_PERIOD_4_6));
				long step_length_7_9 = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.STEP_PERIOD_7_9));
				long step_length_10_30 = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.STEP_PERIOD_10_30));
				long step_length_30_60 = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.STEP_PERIOD_30_60));
				long step_length_60 = Long.valueOf(StringUtils.getFieldFromConcatString(
						value, "\\|", Constants.STEP_PERIOD_60));
				
				// 计算各个访问时长和访问步长的范围
				double visit_length_1s_3s_ratio = NumberUtils.formatDouble(
						(double)visit_length_1s_3s / (double)session_count, 2);  
				double visit_length_4s_6s_ratio = NumberUtils.formatDouble(
						(double)visit_length_4s_6s / (double)session_count, 2);  
				double visit_length_7s_9s_ratio = NumberUtils.formatDouble(
						(double)visit_length_7s_9s / (double)session_count, 2);  
				double visit_length_10s_30s_ratio = NumberUtils.formatDouble(
						(double)visit_length_10s_30s / (double)session_count, 2);  
				double visit_length_30s_60s_ratio = NumberUtils.formatDouble(
						(double)visit_length_30s_60s / (double)session_count, 2);  
				double visit_length_1m_3m_ratio = NumberUtils.formatDouble(
						(double)visit_length_1m_3m / (double)session_count, 2);
				double visit_length_3m_10m_ratio = NumberUtils.formatDouble(
						(double)visit_length_3m_10m / (double)session_count, 2);  
				double visit_length_10m_30m_ratio = NumberUtils.formatDouble(
						(double)visit_length_10m_30m / (double)session_count, 2);
				double visit_length_30m_ratio = NumberUtils.formatDouble(
						(double)visit_length_30m / (double)session_count, 2);  
				
				double step_length_1_3_ratio = NumberUtils.formatDouble(
						(double)step_length_1_3 / (double)session_count, 2);  
				double step_length_4_6_ratio = NumberUtils.formatDouble(
						(double)step_length_4_6 / (double)session_count, 2);  
				double step_length_7_9_ratio = NumberUtils.formatDouble(
						(double)step_length_7_9 / (double)session_count, 2);  
				double step_length_10_30_ratio = NumberUtils.formatDouble(
						(double)step_length_10_30 / (double)session_count, 2);  
				double step_length_30_60_ratio = NumberUtils.formatDouble(
						(double)step_length_30_60 / (double)session_count, 2);  
				double step_length_60_ratio = NumberUtils.formatDouble(
						(double)step_length_60 / (double)session_count, 2);  
				
				// 将统计结果封装为Domain对象
				SessionAggrStat sessionAggrStat = new SessionAggrStat();
				sessionAggrStat.setTaskid(taskid);
				sessionAggrStat.setSession_count(session_count);  
				sessionAggrStat.setVisit_length_1s_3s_ratio(visit_length_1s_3s_ratio);  
				sessionAggrStat.setVisit_length_4s_6s_ratio(visit_length_4s_6s_ratio);  
				sessionAggrStat.setVisit_length_7s_9s_ratio(visit_length_7s_9s_ratio);  
				sessionAggrStat.setVisit_length_10s_30s_ratio(visit_length_10s_30s_ratio);  
				sessionAggrStat.setVisit_length_30s_60s_ratio(visit_length_30s_60s_ratio);  
				sessionAggrStat.setVisit_length_1m_3m_ratio(visit_length_1m_3m_ratio); 
				sessionAggrStat.setVisit_length_3m_10m_ratio(visit_length_3m_10m_ratio);  
				sessionAggrStat.setVisit_length_10m_30m_ratio(visit_length_10m_30m_ratio); 
				sessionAggrStat.setVisit_length_30m_ratio(visit_length_30m_ratio);  
				sessionAggrStat.setStep_length_1_3_ratio(step_length_1_3_ratio);  
				sessionAggrStat.setStep_length_4_6_ratio(step_length_4_6_ratio);  
				sessionAggrStat.setStep_length_7_9_ratio(step_length_7_9_ratio);  
				sessionAggrStat.setStep_length_10_30_ratio(step_length_10_30_ratio);  
				sessionAggrStat.setStep_length_30_60_ratio(step_length_30_60_ratio);  
				sessionAggrStat.setStep_length_60_ratio(step_length_60_ratio);  
				
				// 调用对应的DAO插入统计结果
				ISessionAggrStatDAO sessionAggrStatDAO = DAOFactory.getSessionAggrStatDAO();
				sessionAggrStatDAO.insert(sessionAggrStat);  
	}
	
}













