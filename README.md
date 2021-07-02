# XCloud-Shell
A command line framework based on Java/SpringBoot enables your application to have the same function as spark shell.

English version goes [here](README.md).

## Quick start

### Maven dependencies
- SpringBoot project only relies:
```
<!-- https://mvnrepository.com/artifact/com.wl4g/xcloud-shell-springboot -->
<dependency>
  <groupId>com.wl4g</groupId>
  <artifactId>xcloud-shell-springboot</artifactId>
  <version>${latest.version}</version>
</dependency>
<!-- https://mvnrepository.com/artifact/com.wl4g/xcloud-shell-cli -->
<dependency>
  <groupId>com.wl4g</groupId>
  <artifactId>xcloud-shell-cli</artifactId>
  <version>${latest.version}</version>
</dependency>
```

- Java project only relies:
```
<!-- https://mvnrepository.com/artifact/com.wl4g/xcloud-shell-core -->
<dependency>
  <groupId>com.wl4g</groupId>
  <artifactId>xcloud-shell-core</artifactId>
  <version>${latest.version}</version>
</dependency>
<!-- https://mvnrepository.com/artifact/com.wl4g/xcloud-shell-cli -->
<dependency>
  <groupId>com.wl4g</groupId>
  <artifactId>xcloud-shell-cli</artifactId>
  <version>${latest.version}</version>
</dependency>
```

[Custom commands example](xcloud-shell-example/src/main/java/com/wl4g/shell/example/console/ExampleConsole.java)

## Source code compilation (recommended for secondary development)
```
cd xcloud-shell
mvn clean install -DskipTests -T 2C
```

## Startup CLI

### Mode1
Specify the port of the service, and then run as a client (applicable to the client mode, usually temporarily used to connect to the application service):

```
java -Dservpoint=127.0.0.1:60103 -Dprompt=my-shell -Dtimeout=5000 -jar xcloud-shell-cli-${version}-executable.jar
```

In the command above -dservpoint represents the service listening address and port to connect to.

### Mode2
Specify the name of the service and run it directly as a client (for local mode, usually as a built-in console for application services):

```
java -Dservname=shell-example -Dprompt=my-shell -Dtimeout=5000 -jar xcloud-shell-cli-${version}-executable.jar
```

In the above command, the `-Dservname` indicates the application name of the server(`spring.application.name` is used by default for springboot application). It will be automatically checked locally according to servname Find the service port to establish a connection (pay attention to case). You can also use [Mode1](#Mode1) to display the specified service endpoint with -dservpoint, where `-Dprompt` is used to set up the shell
Command line prompt of console, `-Dtimeout` specifies the timeout time for waiting results to return (default: `180_000`ms), and can also print debugging information using `-Dxdebug`.


## Features  
> Before you can test the sample command, you must run the example server first: [com.wl4g.ShellExample](xcloud-shell-example/src/main/java/com/wl4g/ShellExample.java)   

##### 1. Connect with serverpoint (choose one of servname)  
![Connect using serverpoint](shots/servpoint_connect.png)  

##### 2. Help commands  
![help帮助](shots/help.png)

##### 3. Support common shortcut keys, such as tab automatic completion, Ctrl ++A cursor jump to the beginning of line, Ctrl+E cursor jump to the end of line, and Ctrl+C exit console (follow GNU)  
![Tab automatic completion](shots/tab.png)

##### 4. Support authentication protection  
![Support authentication protection](shots/auth.png)  

##### 5. Support ACL access control  
![Support ACL access control](shots/acl.png)  

##### 6. Support real-time progress display and forced interruption  
![Support real-time progress display and forced interruption](shots/progress_interrupt.png)

##### 7. Support concurrency control lock  (Source refer: [ShellMethod#lock()](xcloud-shell-common/src/main/java/com/wl4g/shell/common/annotation/ShellMethod.java))   
![支持实时进度显示与强制中断](shots/concurrent_lock.png)

## Built-in commands
|long-opt|short-opt|Description|
|-|-|-|
|clear|cls|Cleanup console|
|exit/quit|ex/qu|Exting console|
|history|his|Show history commands (Default save to: $USER_HOME/.wl4g/shell/history)|
|stacktrace|st|Show last error stacktrace information|
|help|he|Show help usage information|
|login|lo|Request login(for example, When `spring.xcloud.shell.acl.enabled=true`, all command execution must log in, otherwise there is no need to login.)|
