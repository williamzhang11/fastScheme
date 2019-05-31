# hive基本操作
hive中最常用导入数据方式为：
1.加载本地数据

创建数据库
create database userdb;
查看数据库
show databases;
使用数据库
use userdb;
创建表
create table user1 (id int,name string) row format delimited fields terminated by ',';
加载本地数据
load data local inpath '/data/input/a.txt' into table user1;
查看数据
select * from user1;


2.加载hdfs数据
上传数据至hdfs:
hdfs dfs -put /data/input/a.txt /input/user.txt
查看hdfs数据
hdfs dfs -cat /input/user.txt
加载
load data inpath '/input/user.txt' into table user1;

注意：
从hdfs加载数据成功后，hdfs上数据会被移动到hdfs上hive的文件目录下。
如：/user/hive/warehouse/userdb.db/user1/a.txt
    /user/hive/warehouse/userdb.db/user1/user.txt

3.导入mysql/oracle 等关系型数据库数据
