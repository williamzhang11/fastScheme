package com.xiu.wordcount;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * Hello world!
 *
 */
public class MyFirstMapReduce 
{
	
	public static void main(String[] args) throws IOException, ReflectiveOperationException, InterruptedException {
		
	Configuration configuration = new Configuration();
	
	Job wordCountJob = Job.getInstance();
	//指定本job所在jar包
	wordCountJob.setJarByClass(MyFirstMapReduce.class);
	
	wordCountJob.setMapperClass(WordCountMap.class);
	wordCountJob.setReducerClass(WordCountReduce.class);
	//设置map阶段输出kv数据类型
	wordCountJob.setMapOutputKeyClass(Text.class);
	wordCountJob.setMapOutputValueClass(IntWritable.class);
	//设置最终输出kv数据类型
	wordCountJob.setOutputKeyClass(Text.class);
	wordCountJob.setOutputValueClass(IntWritable.class);
	//设置要处理的文本数据所存放的路径
	FileInputFormat.setInputPaths(wordCountJob, args[0]);
	FileOutputFormat.setOutputPath(wordCountJob, new Path(args[1]));
	//提交job给hadoop集群
	wordCountJob.waitForCompletion(true);
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
		 
	}
	
	
}
