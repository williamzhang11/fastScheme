Hive安装步骤
```
192.168.192.130上安装hive
1.下载安装包，并解压
2.配置环境变量

export HIVE_HOME=/data/soft/hive-3.1.1
export PATH=:$HIVE_HOME/bin

3.验证
hive --version
4.修改配置
cd /data/soft/hive-3.1.1/conf
cp hive-default.xml.template hive-site.xml
删除之前的配置添加
<configuration>
<property>
<name>javax.jdo.option.ConnectionURL</name>
<value>jdbc:mysql://192.168.137.96:3306/hive</value>
</property>
<property>
<name>javax.jdo.option.ConnectionDriverName</name>
<value>com.mysql.jdbc.Driver</value>
</property>
<property>
<name>javax.jdo.option.ConnectionUserName</name>
<value>root</value>
</property>
<property>
<name>javax.jdo.option.ConnectionPassword</name>
<value>123456</value>
</property>
<property>
<name>hive.metastore.schema.verification</name>
<value>false</value>
</property>
</configuration>
5.进入mysql数据库，创建hive数据：
create database hive;
6.上传mysql包到/data/soft/hive-3.1.1/lib/
mysql-connector-java-5.1.47.jar
7.进入hive安装目录bin目录下：
执行schematool -dbType mysql -initSchema
初始化成功

8.启动hive
hive


```

参考：https://www.cnblogs.com/lihao7/p/10117020.html
https://blog.csdn.net/genus_yang/article/details/87938796