package com.xiu.wordcount;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;


/**
 *KEYIN：输入kv数据对中key的数据类型
 *VALUEIN:输入kv数据对中value的数据类型
 *KEYOUT:输出kv数据针对中key的数据类型
 *VALUEOUT:输出kv数据对中value的数据类型 
 *
 */

public class WordCountMap extends Mapper<LongWritable, Text, Text, IntWritable>{

	/**
	 * map方法提供给map task进程调用的，map task进程是每读取一行文本来调用一次自定义的map方法
	 * map task在调用map方法时，传递参数：
	 * 一行的起始偏移量LongWritable作为key
	 * 一行的文本内容Text作为value
	 */
	@Override
	protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, IntWritable>.Context context)
			throws IOException, InterruptedException {
		String line = value.toString();//拿到一行文本内容，转换成String类型
		String [] words = line.split(" ");//将这行文本切分成单词，以空格切分
		//将单词输出格式<单词，1>
		for(String word : words) {
			context.write(new Text(word), new IntWritable(1));
		}
		
		
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
