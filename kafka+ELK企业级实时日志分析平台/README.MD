# Zookeeper集群+kafka集群+elk集群


## zookeeper集群搭建
1.准备虚拟机，下载zookeeper，并上传至虚拟机，配置搭建jdk环境
|技术|包路径|
| :------ | :------ |
|  192.168.246.128       |   zk      |
|  192.168.246.129       |   zk      |
|  192.168.246.130       |   zk      |
2.将安装包解压至/usr/local/zookeeper/
```
tar -zxvf zookeeper-3.4.14.tar.gz
cd /usr/local/zookeeper/zookeeper-3.4.14
mkdir logs && mkdir data //创建日志文件的存放位置,数据存放位置
cd conf && cp zoo_sample.cfg zoo.cfg
vi zoo.cfg
```
3.修改3台虚拟机的配置文件

```
dataDir=/usr/local/zookeeper/zookeeper-3.4.14/data
dataLogDir=/usr/local/zookeeper/zookeeper-3.4.14/logs

server.1=192.168.246.128:2888:3888
server.2=192.168.246.129:2888:3888
server.3=192.168.246.130:2888:3888


#server.1,这个1是服务器标识，代表第几号服务器，这个标识写到下面的myid文件中

#192.168.246.128，为集群中的ip地址，第一个端口是master和slave之间的通信端口，默认2888，第二各端口是leader选举
#端口，集群刚启动时的选举或leader挂掉后，进行新的选举端口默认，3888

```
#配置文件解释

```
tickTime=2000
zookeeper服务器之间或客户端与服务器之间维持心跳的时间间隔，即每tickTime时间发送一个心跳
initLimit=10
用来配置zookeeper接收客户端初始化连接时最长能忍受多少心跳时间间隔数，当超过10个tickTime后，zookeeper服务器
还没收到客户端返回信息，表明这个客户端连接失败。总长2000*10=20秒。注意是初始化时的连接
syncLimit=5
标识leader和follower之间发送消息，请求和应答时间长度，最长不超过多个个tickTime时间长度
总长2000*5=10秒。注意是正常连接请求

clientPort=2181
这个端口是客户端连接zookeeper服务器的端口，zookeeper监听这个端口，接受客户端的访问请求。

dataDir=/usr/local/zookeeper/zookeeper-3.4.14/data
快照日志存储路径
dataLogDir=/usr/local/zookeeper/zookeeper-3.4.14/logs
事务日志的存储路径，如果不配置默认存储到dataDir指定的目录，这样会影响性能。
```


4.创建myid文件
```
#server1
echo "1" > /usr/local/zookeeper/zookeeper-3.4.14/data/myid
#server2
echo "1" > /usr/local/zookeeper/zookeeper-3.4.14/data/myid
#server3
echo "1" > /usr/local/zookeeper/zookeeper-3.4.14/data/myid
```
5.启动服务
```
1.3个节点全部启动服务
cd /usr/local/zookeeper/zookeeper-3.4.14/bin
./zkServer.sh start
2.全部启动成功后查看服务状态
./zkServer.sh status
leader:
ZooKeeper JMX enabled by default
Using config: /usr/local/zookeeper/zookeeper-3.4.14/bin/../conf/zoo.cfg
Mode: leader

follower:
ZooKeeper JMX enabled by default
Using config: /usr/local/zookeeper/zookeeper-3.4.14/bin/../conf/zoo.cfg
Mode: follower
```
zk集群中有一个leader,多个follower

## kafka集群搭建

1.准备
搭建好zookeeper集群
下载kafka
|虚拟机ip|角色|
| :------ | :------ |
|  192.168.246.128       |   kafka      |
|  192.168.246.129       |   kafka      |
|  192.168.246.130       |   kafka      |


2.将安装包解压至
```
/usr/local/kafka/kafka_2.12-2.1.1
tar -zxvf kafka_2.12-2.1.1.tgz

/usr/local/kafka/kafka_2.12-2.1.1/config
#kafka中虽然自带zk集群，但还是建议使用独立的集群
vi server.properties

```
2.修改配置文件

```
#server1
broker.id=0//当前机器在kafka集群中的唯一标识
#server2
broker.id=1//当前机器在kafka集群中的唯一标识
#server3
broker.id=2//当前机器在kafka集群中的唯一标识

#设置zookeeper的连接端口
zookeeper.connect=192.168.246.128:2181,192.168.246.129:2181,192.168.246.130:2181
port:9092 默认，对外提供服务的端口

#server1
listeners=PLAINTEXT://192.168.246.128:9092
#server2
listeners=PLAINTEXT://192.168.246.129:9092
#server3
listeners=PLAINTEXT://192.168.246.130:9092
#消息存放目录
log.dirs=/usr/local/kafka/kafka_2.12-2.1.1/logs

default.replication.factor=3#每个分区的副本个数

num.partitions=3，分区数


```
3.启动集群
```
cd /usr/local/kafka/kafka_2.12-2.1.1/bin
./kafka-server-start.sh -daemon ../config/server.properties

验证：
jps
出现kafka说明启动成功

```


4.创建topic验证
```
查看创建的topic 
./kafka-topics.sh --list --zookeeper localhost:2181
创建topic
./kafka-topics.sh --create --zookeeper 192.168.246.128:2181 --replication-factor 3 --partitions 3 --topic test1
创建发布者
./kafka-console-producer.sh --broker-list 192.168.246.128:9092 --topic test1
创建订阅者
./kafka-console-consumer.sh --bootstrap-server 192.168.246.128:9092 --topic test1 --from-beginning

查看节点状态
./kafka-topics.sh --describe --zookeeper localhost:2181 --topic test1
```
5.kafka日志说明
cd /usr/local/kafka/kafka_2.12-2.1.1/logs

```
server.log #kafka的运行日志
state-change.log#kafka是用zookeeper保存状态的，可能会切换，切换的日志保存在这
controller.log#kafka选择一个节点作为controller

```
目前的zk信息查看
```
使用客户端进入zk
./zkCli.sh -server 127.0.0.1:2181

查看目录 ls /
[cluster, controller_epoch, controller, brokers, zookeeper, admin, isr_change_notification, consumers, log_dir_event_notification, latest_producer_id_block, config]

上面的显示结果中：只有zookeeper是，zookeeper原生的，其他都是Kafka创建的


[zk: 127.0.0.1:2181(CONNECTED) 5] get /brokers/ids/0
{"listener_security_protocol_map":{"PLAINTEXT":"PLAINTEXT"},"endpoints":["PLAINTEXT://192.168.246.128:9092"],"jmx_port":-1,"host":"192.168.246.128","timestamp":"1555262677763","port":9092,"version":4}
cZxid = 0x10000001a
ctime = Mon Apr 15 01:24:38 CST 2019
mZxid = 0x10000001a
mtime = Mon Apr 15 01:24:38 CST 2019
pZxid = 0x10000001a
cversion = 0
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x100004289530000
dataLength = 200
numChildren = 0
[zk: 127.0.0.1:2181(CONNECTED) 6] 

```
## elk搭建
elk相关软件上传至/usr/local/elk
### elasticsearch安装

```
1.
tar -zxvf elasticsearch-7.0.0-linux-x86_64.tar.gz 
2.用户组和用户创建，因为elasticsearch不能使用root启动

groupadd elsearch
useradd -g elsearch elsearch
然后设置目录为elsearch 用户所属

chown -R elsearch:elsearch /usr/local/elk/elasticsearch-7.0.0/
3.创建日志存储目录位置以及数据存储目录
mkdir -p /var/elasticsearch/data/
mkdir -p /var/elasticsearch/logs/

chown -R elsearch:elsearch  /var/elasticsearch/data/
chown -R elsearch:elsearch  /var/elasticsearch/logs/

4.设置系统参数
配置系统最大文件数
vi /etc/security/limits.conf

elsearch       hard        nofile        65536 
elsearch       soft        nofile        65536
*               soft       nproc         4096
*               hard       nproc         4096



 
 
内存太小需要修改
vi /usr/local/elk/elasticsearch-7.0.0/config/jvm.options
将-Xmx1g改成-Xmx512m
将-Xms1g改成-Xms512m
-Xms512m
-Xmx512m
 
调整虚拟内存最大map数量，默认是65536，调整最大的文件数量
vi /etc/sysctl.conf
在文件最底下增加：
vm.max_map_count=262144
fs.file-max=65536
使生效并查看值：sysctl -p

5.elasticsearch配置
/usr/local/elk/elasticsearch-7.0.0/config/elasticsearch.yml 

cluster.name: my-application
node.name: node-1#server1
node.name: node-1#server2
path.data: /var/elasticsearch/data
path.logs: /var/elasticsearch/logs

network.host: 192.168.246.128#server1

network.host: 192.168.246.129 #server1
http.port: 9200
discovery.seed_hosts: ["192.168.246.128", "192.168.246.129"]
cluster.initial_master_nodes: ["node-1", "node-2"]

6.启动

su elsearch

cd /usr/local/elk/elasticsearch-7.0.0/bin
./elasticsearch -d


7.验证：
curl 192.168.246.128:9200/_cluster/health?pretty
```

### logstash安装

```
1.解压
/usr/local/elk
tar -zxvf logstash-7.0.0.tar.gz 
2.配置参数
cd /usr/local/elk/logstash-7.0.0/config
cp logstash-sample.conf logstash-test.conf
3.根据样例编写logstash配置文件

input {
  kafka {
        bootstrap_servers => ["192.168.246.128:9092,192.168.246.129:9092,192.168.246.130:9092"]
        topics => ["test"]
        group_id => "test"
  }
}

output {
  elasticsearch {
    hosts => ["192.168.246.128:9200"]
    index => "logstash-%{[fields][document_type]}-%{+YYYY.MM.dd}"
  }
}

4.检查格式
./logstash -f ../config/logstash-test.conf --configtest --verbose


5.启动
cd /usr/local/elk/logstash-7.0.0/bin
 ./logstash -f ../config/logstash-test.conf 

 ./logstash  -daemon ../config/logstash-test.conf 

 nohup /usr/local/elk/logstash-7.0.0/bin/logstash -c /usr/local/elk/logstash-7.0.0/config/logstash-test.conf  /usr/local/elk/logstash-7.0.0/logs/logstash.log &



```

### kibana安装
```
1.解压
    cd /usr/local/elk
    tar -zxvf kibana-7.0.0-linux-x86_64.tar.gz
2.配置
vi /usr/local/elk/kibana-7.0.0-linux-x86_64/config/kibana.yml

server.port: 5601
server.host: "192.168.246.128"
elasticsearch.hosts: ["http://192.168.246.128:9200"]
3.启动
nohup /usr/local/elk/kibana-7.0.0-linux-x86_64/bin/kibana -c /usr/local/elk/kibana-7.0.0-linux-x86_64/config/kibana.yml > /dev/null 2>&1 &


```




