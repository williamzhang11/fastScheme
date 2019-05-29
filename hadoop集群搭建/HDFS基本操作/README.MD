# HDFS 基本操作
```
查看目录操作：
hdfs dfs -ls /
创建目录：
hdfs dfs -mkdir /input
上传文件：
hdfs dfs -put /root/a.txt /input
查看文件：
hdfs dfs -cat /input/a.txt
删除文件：
 hdfs dfs -rm  /input/a.txt

```