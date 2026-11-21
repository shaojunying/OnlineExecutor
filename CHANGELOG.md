# 更新日志

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。日期取自 git 提交记录。

## [1.0.0] - 2026-11-21

### 新增

- 新增 3 种运行模式：`in-process`、`process`、`docker`，用 `executor.mode` 切换，默认为 `process`。
- 新增子进程模式：用户代码在子 JVM 中运行，限制超时、堆内存和输出大小，超时后强制结束进程。
- 新增 Docker 模式：容器无网络、文件系统只读，并限制内存、CPU 和进程数。
- 新增 JSON API `POST /api/executions` 和 `GET /api/runtime`。错误响应使用 RFC 9457 格式。
- 新增标准输入（stdin）。
- 新增并发上限。没有空闲名额时，返回 HTTP 429。
- 新增 `/actuator/health` 健康检查。
- 新增 Maven Wrapper、GitHub Actions CI（Java 21 和 25）、Dockerfile、LICENSE。

### 变更

- 升级到 Java 21 和 Spring Boot 4.2。
- 包名改为 `io.github.shaojunying.onlineexecutor`。
- 前端改为单页应用：代码编辑器换成 CodeMirror，静态资源打包进 jar，不依赖 CDN。
- 编译错误显示行号和列号。点击错误，编辑器跳到对应行。
- 旧地址 `/executor` 永久重定向到 `/`。

### 修复

- 修复：代码中有 lambda 或字符串拼接时，`ClassModifier` 抛出 `ArrayIndexOutOfBoundsException`。原因有 2 个：常量池只支持 tag 1–12；Long 和 Double 各占两个槽位，代码没有多跳一格。
- 修复：用 `java -jar` 启动后，同进程模式找不到 `HackSystem`。
- 修复：并发请求共用一个输出缓冲区，用户会看到别人的输出。
- 修复：用户代码调用 `System.exit` 会结束整个服务。
- 修复：死循环会一直占用请求线程。
- 修复：`public class Run{`（类名后没有空格）和注释中的 "class " 会导致类名解析错误。
- 修复：有内部类或匿名类时，只保留了最后一个 class 文件，运行时报 `NoClassDefFoundError`。
- 修复：没有 `main` 方法时，错误信息只写到服务端日志，用户看到的是 "Nothing..."。
- 修复：替换成非 ASCII 字符串时，`ClassModifier` 写入的是字符数，不是字节数。

## [0.8] - 2025-07-26

- 重新设计前端界面和 README。

## [0.7] - 2022-01-13

- 新增：用 Spring Boot 和 Spring MVC 搭建网站。

## [0.6] - 2022-01-13

- 新增：收集编译错误信息。
- 修复：删除 `ByteUtils` 中无效的注释。

## [0.5] - 2022-01-12

- 新增：动态编译。在内存中把源代码字符串直接编译为字节数组。

## [0.4] - 2022-01-11

- 新增：替换 class 文件常量池中的字符串常量。

## [0.3] - 2022-01-11

- 新增：`HackSystem`，用于重定向程序输出。

## [0.2] - 2022-01-11

- 新增：自定义 ClassLoader，同一个类可以重复加载。

## [0.1] - 2022-01-11

- 新增：用 `URLClassLoader` 加载外部类。

[1.0.0]: https://github.com/shaojunying/OnlineExecutor/releases/tag/v1.0.0
