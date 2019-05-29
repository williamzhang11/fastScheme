package com.xiu.wordcount;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/* 
 * KEYIN：对应mapper阶段输出的key类型 
 * VALUEIN：对应mapper阶段输出的value类型 
 * KEYOUT：reduce处理完之后输出的结果kv对中key的类型 
 * VALUEOUT：reduce处理完之后输出的结果kv对中value的类型 
 */
public class WordCountReduce extends Reducer<Text, IntWritable, Text, IntWritable>{
	
	/**
	 *reduce方法提供给reduce task进行调用
	 *reduce task  会将shuffle阶段分发过来的大量kv 数据进行聚合，聚合机制是相同的key的kv对聚合为一组
	 *然后reduce task对每一组聚合kv调用一次自定义的reduce方法
	 *如<hello,1><hello,1><hello,1><tom,1><tom,1><tom,1>
	 *hello组会调用一次reduce方法进行处理，tom组也会调用一次reduce方法进行处理
	 *调用时传递的参数：
	 *key:一组kv中的key
	 *values:一组kv中所有value的迭代器
	 */
	@Override
	protected void reduce(Text arg0, Iterable<IntWritable> arg1,
			Reducer<Text, IntWritable, Text, IntWritable>.Context arg2) throws IOException, InterruptedException {
		
		int count =0;
		for(IntWritable value :arg1) {
			count += value.get();
		}
		//输出这个单词的统计结果
		arg2.write(arg0, new IntWritable(count));
		
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
