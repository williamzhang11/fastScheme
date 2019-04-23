# 服务注册中心，
提供服务注册与发现功能，主要组件Eureka
#设为false，表示不能向注册中心注册自己
eureka.client.register-with-eureka=false
#由于注册中心注册时维护服务实例，不需要检索服务，设置false
eureka.client.fetch-registry=false
#指定注册中心地址
eureka.client.serviceUrl.defaultZone=http://localhost:${server.port}/eureka

高可用原理是：单机的Eureka注册中心向其他注册中心注册自己，可以形成互相注册的注册中心，实现高可用

```
#server1
spring.application.name=eureka-server
server.port=1111
eureka.instance.hostname=localhost
eureka.client.serviceUrl.defaultZone=http://localhost:1112/eureka/,http://localhost:1113/eureka/
eureka.server.enable-self-preservation=false

#server2
spring.application.name=eureka-server
server.port=1112
eureka.instance.hostname=localhost
eureka.client.serviceUrl.defaultZone=http://localhost:1111/eureka/,http://localhost:1113/eureka/
eureka.server.enable-self-preservation=false
#server3
spring.application.name=eureka-server
server.port=1113
eureka.instance.hostname=localhost
eureka.client.serviceUrl.defaultZone=http://localhost:1111/eureka/,http://localhost:1112/eureka/
eureka.server.enable-self-preservation=false

```