#nginx+keepalived高可用集群

nginx可用来做负载均衡，达到分流的目的，但是nginx存在单点故障的问题
如果nginx服务挂了，整个服务也就挂了。而keepalived是一个高性能服务器，高可用/热备解决方案，可以解决单点故障的问题。

![image](https://github.com/williamzhang11/fastScheme/blob/master/keepalived%2Bnginx%E9%AB%98%E5%8F%AF%E7%94%A8%E9%9B%86%E7%BE%A4/image/keepalivenginx.jpg)

## nginx搭建
```
1.安装Nginx源
rpm -ivh http://nginx.org/packages/centos/7/noarch/RPMS/nginx-release-centos-7-0.el7.ngx.noarch.rpm

安装该rpm后，我们就能在/etc/yum.repos.d/ 目录中看到一个名为nginx.repo 的文件。
2.安装Nginx
yum install -y nginx
3.Nginx默认目录

whereis nginx

(1) Nginx配置路径：/etc/nginx/
(2) PID目录：/var/run/nginx.pid
(3) 错误日志：/var/log/nginx/error.log
(4) 访问日志：/var/log/nginx/access.log
(5) 默认站点目录：/usr/share/nginx/html

事实上，只需知道Nginx配置路径，其他路径均可在/etc/nginx/nginx.conf 以及/etc/nginx/conf.d/default.conf 中查询到

```
常用命令：
```
1.启动
nginx
2.测试Nginx配置是否正确
nginx -t
3.优雅重启
nginx -s reload
4.查看nginx的进程号：
ps -ef |grep nginx
5.nginx服务停止
nginx -s stop
kill -9 pid
```
配置

/etc/nginx/conf.d/default.conf
负载均衡配置
```
upstream site{

        server 192.168.246.128:8080;
        server 192.168.246.129:8080;
        server 192.168.246.130:8080;
}

server{
	listen       80;
	
	    location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
        proxy_pass http://site;
    }
}



```

##安装keepalived
1.手动编译安装包
```
下载安装包,上传到虚拟机（/usr/local/src）
https://www.keepalived.org/download.html

cd /usr/local/src

安装依赖
yum install gcc gcc-c++ make automake autoconf libtool pcre pcre-devel zlib zlib-devel openssl openssl-devel

解压缩 
tar -zxvf keepalived-2.0.15.tar.gz

cd /usr/local/src/keepalived-2.0.15
./configure --prefix=/usr/local/keepalived

make && make install

```
2.编写nginx监控脚本
nginx_check.sh

```

#!/bin/bash
counter=$(ps -C nginx --no-header | wc -l)
if [ "${counter}" = "0" ]; then
    nginx
    sleep 2
    counter=$(ps -C nginx --no-header | wc -l)
    if [ "${counter}" = "0" ]; then
        /etc/init.d/keepalived stop
    fi
fi
```

2.keepalived实现nginx的高可用配置
```

1.复制/usr/local/keepalived/sbin/keepalived到/usr/sbin下
cp /usr/local/keepalived/sbin/keepalived /usr/sbin/

2.keepalived默认会读取/etc/keepalived/keepalived.conf配置文件
mkdir /etc/keepalived
cp /usr/local/keepalived/etc/keepalived/keepalived.conf /etc/keepalived/
3.复制sysconfig文件到/etc/sysconfig下
cp /usr/local/keepalived/sbin/keepalived /etc/sysconfig/
4.
cp /usr/local/keepalived/sbin/keepalived /etc/init.d/
chmod 755 /etc/init.d/keepalived

```
3.配置
```
vi /etc/keepalived/keepalived.conf
修改：
#vrrp_strict
vrrp_instance VI_1 {
    state MASTER/BACKUP
    interface ens33  //网卡配置



virtual_ipaddress {
        192.168.246.200
        #192.168.200.16
        #192.168.200.17
        #192.168.200.18
    }



```

4.验证
service keepalived
ps -aux|grep keepalived
