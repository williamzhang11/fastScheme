# hive基本操作
hive中最常用导入数据方式为：
1.加载本地数据

创建数据库
create database userdb;
查看数据库
show databases;
使用数据库
use userdb;
创建内部表
create table user1 (id int,name string) row format delimited fields terminated by ',';
创建外部表
create external table user2 (id int,name string) row format delimited fields terminated by ',';
创建分区表：
静态分区：
create table par_tab (name String, nation string) partitioned by(sex string) row format delimited fields terminated by ',';
加载本地数据到分区表：
load data local inpath '/data/input/par_tab.txt' into table par_tab partition (sex='man');
动态分区：
查看表结构 desc par_tab
查看hdfs上目录（创建分区表时，系统在hive数据库默认路径下创建一个目录（表名称），再创建目录的子目录sex=man(分区名)，
最后在分区名下存放实际的数据文件）：
hdfs dfs -cat /user/hive/warehouse/userdb.db/par_tab/sex=man/par_tab.txt
插入另一组数据，分区名为sex=woman,则hdfs下会表名称下会创建另一个子目录sex=woman
创建静态分区表包含多分区：
create table par_tab_muilt(name string,nation string) partitioned by (sex string,dt string) row format delimited fields terminated by ',';
load data local inpath '/data/input/par_tab.txt' into table par_tab_muilt partition (sex='man',dt='2019-06-06');

动态分区：根据查询得到的数据动态分配到分区
set hive.exec.dynamic.partition=true;
创建分区表：
create table par_dnm2 (name string,nation string) partitioned by (sex string,dt string) row format delimited fields terminated by ',';
插入数据
hive> insert overwrite table par_dnm2 partition(sex='man',dt)
    > select name,nation,dt from par_tab_muilt;

https://www.cnblogs.com/yongjian/archive/2017/03/29/6640951.html

加载本地数据：
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




1.内部表，外部表，分区表，分桶表区别
内部表：数据存储在hive数据仓库目录下，删除表时，除了删除元数据，还会删除实际表文件
外部表：数据不存储在hive的数据仓库目录下，删除表时，只删除元数据，并不删除实际表文件
分区表: 和RDMS分区概念类似，将一张表的数据安装分区规则分为多个目录存储，这样可通过指定分区来提高查询速度
内部表和外部表都可以使用分区功能，成为内部分区表或外部分区表
分桶表：在表或分区的基础上，按某一列的值将记录进行分桶存放，即分文件存放，也就是将大表变成小表


