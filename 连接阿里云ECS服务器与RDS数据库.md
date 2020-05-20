## 连接阿里云ECS服务器与RDS数据库

#### 1、连接ECS服务器

购买ECS之后，会分配给你一个实例：
![1585060440035](C:\Users\lifeline张\AppData\Roaming\Typora\typora-user-images\1585060440035.png)

点进去之后，可以设置连接密码，用户名是默认值，不用管。

![1585060534027](C:\Users\lifeline张\AppData\Roaming\Typora\typora-user-images\1585060534027.png)

设置密码后就可以通过xshell访问了

#### 2、连接RDS数据库

有数据库实例之后， 可以设置白名单，也就是说可以让哪些ip访问你；这个ip地址可以设置为0.0.0.0/0，表示所有的人都可以访问你。

![1585060666783](C:\Users\lifeline张\AppData\Roaming\Typora\typora-user-images\1585060666783.png)

然后去账号管理中创建账号并且分配权限。

完了之后使用navicate，在ssh中主机填写ecs服务器ip，用户名和密码填写服务器登录的；然后去常规里面，主机写内网字符串，用户名和密码写自己上边建的，即可连接

![1585060882203](C:\Users\lifeline张\AppData\Roaming\Typora\typora-user-images\1585060882203.png)



#### 3、ECS服务器开放端口

服务器开放端口操作如下：

![1585141839216](C:\Users\lifeline张\AppData\Roaming\Typora\typora-user-images\1585141839216.png)

点击本实例安全组，然后点击配置规则，然后添加安全组规则，![1585141887061](C:\Users\lifeline张\AppData\Roaming\Typora\typora-user-images\1585141887061.png)

端口写你想要开放的端口，授权对象写0.0.0.0/0，然后就可以在浏览器上访问你开放的这个端口了。



#### 4、kibana启动的问题

针对云服务器，kibana配置文件修改要注意：server,host和elasticsearch.hosts中的ip要改成内网ip，否则会报错：Error: listen EADDRNOTAVAIL: address not available