# hadoop集群搭建

## 部署情况

```
使用3台机器:
hostname	hserver1         hserver2          hserver3
IP     	 192.168.192.128    192.168.192.129     192.168.192.130 
HDFS     NameNode           					SecondaryNameNode
HDFS 	 DataNode            DataNode	         DataNode
YARN              			ResourceManager
YARN	 NodeManager		NodeManager			NodeManager
												HistoryServer	   
```

目录规划：
```
#hadoop临时目录hadoop.tmp.dir
/data/hadoop/tmp

#hadoop的NameNode节点保存元数据的目录dfs.namenode.name.dir
/data/hadoop/hdfs/name

#hadoop的DataNode节点保存数据的目录dfs.datanode.data.dir
/data/hadoop/hdfs/data

```

下载hadoop
https://www.apache.org/dyn/closer.cgi/hadoop/common/hadoop-3.2.0/hadoop-3.2.0.tar.gz

## 安装步骤
### 机器间无密码登陆配置
```
1.查看机器名称
hostname

2.修改3台机器名称
hostname hserver1
hostname hserver2
hostname hserver3
3.分别修改/etc/hosts文件
vi /etc/hosts
192.168.192.128 hserver1
192.168.192.129 hserver2
192.168.192.130 hserver3
4.3台机器分别生成密钥
ssh-keygen  -t   rsa
因为账号是root,密钥文件保存在/root/.ssh/下，查看
ls /root/.ssh/

5.hserver1上生成authorized_keys
touch  /root/.ssh/authorized_keys
ls   /root/.ssh/
6.将hserver1上的/root/.ssh/id_rsa.pub文件内容，hserver2上的/root/.ssh/id_rsa.pub文件内容，hserver3上的/root/.ssh/id_rsa.pub文件内容复制到这个authorized_keys文件中

scp authorized_keys 192.168.192.129:/root/.ssh
scp authorized_keys 192.168.192.130:/root/.ssh

7.测试ssh无密码登陆
ssh   hserver2
ssh   hserver3


```

### jdk安装

### hadoop安装
```
1.上传hadoop至/data/soft/
2.解压tar -zxvf hadoop-3.2.0.tar.gz 
3.配置hadoop环境变量
vi /etc/profile
export HADOOP_HOME=/data/soft/hadoop-3.2.0
export PATH=$HADOOP_HOME/bin:$PATH
export LD_LIBRARY_PATH=$HADOOP_HOME/lib/native
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
source /etc/profile
4.配置hadoopJDK路径，定义集群操作用户，在hadoop-env.sh添加
cd /data/soft/hadoop-3.2.0/etc/hadoop
vi hadoop-env.sh
export JAVA_HOME=/data/soft/jdk1.8.0_211

export HDFS_NAMENODE_USER=root
export HDFS_DATANODE_USER=root
export HDFS_SECONDARYNAMENODE_USER=root
export YARN_RESOURCEMANAGER_USER=root
export YARN_NODEMANAGER_USER=root

export HADOOP_PID_DIR=/data/hadoop/pids
export HADOOP_LOG_DIR=/data/hadoop/logs
5.配置core-site.xml
<configuration>
        <property>
                  <name>fs.defaultFS</name>
                  <value>hdfs://hserver1:8020</value>
        </property>
        <property>
                  <name>hadoop.tmp.dir</name>
                  <value>/data/hadoop/tmp</value>
        </property>
</configuration>

fs.defaultFS为NameNode的地址，hadoop.tmp.dir为hadoop临时目录的地址
6.配置hdfs-site.xml
hserver3:50090
<configuration>
   <property>
             <name>dfs.namenode.secondary.http-address</name>
             <value></value>
   </property>
   <property>
             <name>dfs.replication</name>
             <value>2</value>
             <description>设置副本数</description>
   </property>
   <property>
             <name>dfs.namenode.name.dir</name>
             <value>file:/data/hadoop/hdfs/name</value>
             <description>设置存放NameNode的文件路径</description>
   </property>
   <property>
             <name>dfs.datanode.data.dir</name>
             <value>file:/data/hadoop/hdfs/data</value>
             <description>设置存放DataNode的文件路径</description>
   </property>
</configuration>
dfs.namenode.secondary.http-address是指定secondaryNameNode的http访问地址和端口号，因为在规划中，我们将hadoopSvr4规划为SecondaryNameNode服务。

7.配置workers
vi workers 
hserver1
hserver2
hserver3
8.配置yarn-site.xml
<configuration>
    <property>
       <name>yarn.nodemanager.aux-services</name>
       <value>mapreduce_shuffle</value>
   </property>
   <property>
      <name>yarn.nodemanager.localizer.address</name>
      <value>0.0.0.0:8140</value>
   </property>
   <property>
       <name>yarn.resourcemanager.hostname</name>
       <value>hserver2</value>
   </property>
   <property>
       <name>yarn.log-aggregation-enable</name>
       <value>true</value>
   </property>
   <property>
       <name>yarn.log-aggregation.retain-seconds</name>
       <value>604800</value>
   </property>
   <property>
       <name>yarn.log.server.url</name>
       <value>http://hserver3:19888/jobhistory/logs</value>
   </property>
</configuration>

根据规划yarn.resourcemanager.hostname这个指定resourcemanager服务器指向hserver2。
yarn.log-aggregation-enable是配置是否启用日志聚集功能。
yarn.log-aggregation.retain-seconds是配置聚集的日志在HDFS上最多保存多长时间。

9.配置mapred-site.xml

<configuration>
   <property>
       <name>mapreduce.framework.name</name>
       <value>yarn</value>
   </property>
   <property>
       <name>yarn.app.mapreduce.am.env</name>
       <value>HADOOP_MAPRED_HOME=/data/soft/hadoop-3.2.0</value>
   </property>
   <property>
       <name>mapreduce.map.env</name>
       <value>HADOOP_MAPRED_HOME=/data/soft/hadoop-3.2.0</value>
   </property>
   <property>
       <name>mapreduce.reduce.env</name>
       <value>HADOOP_MAPRED_HOME=/data/soft/hadoop-3.2.0</value>
   </property>
   <property>
       <name>mapreduce.jobhistory.address</name>
       <value>hserver3:10020</value>
   </property>
   <property>
       <name>mapreduce.jobhistory.webapp.address</name>
       <value>hserver3:19888</value>
   </property>
</configuration>

mapreduce.framework.name设置mapreduce任务运行在yarn上。
mapreduce.jobhistory.address是设置mapreduce的历史服务器安装在hserver3机器上。
mapreduce.jobhistory.webapp.address是设置历史服务器的web页面地址和端口号。
10.复制Hadoop配置好的包到其他Linux主机
11.格式化NameNode
hdfs namenode -format
```
### 启动集群
```
1.hserver1上启动HDFS：
./data/soft/hadoop-3.2.0/sbin/start-dfs.sh
2.hserver2启动YARN
./data/soft/hadoop-3.2.0/sbin/start-yarn.sh
3.启动日志服务
hserver3上运行jobhistoryserver服务
mapred --daemon start historyserver
4.查看HDFS页面
http://192.168.192.128:9870
5.查看yarn页面
http://192.168.192.129:8088

6.运行wordcount程序
 hadoop jar /data/soft/hadoop-3.2.0/share/hadoop/mapreduce/hadoop-mapreduce-examples-3.2.0.jar wordcount /input/a.txt /output
hadoop jar /data/soft/hadoop-3.2.0/share/hadoop/mapreduce/mywordcount.jar  com.xiu.wordcount.MyFirstMapReduce /input/input.txt /output3



```
参考：https://blog.csdn.net/wangkai_123456/article/details/87185339#1hadoopSvr1hadoopSvr3hadoopSvr4Hadoop_80
https://blog.csdn.net/ITBigGod/article/details/80216785







