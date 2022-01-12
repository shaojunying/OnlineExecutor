# OnlineExecutor
Java代码在线执行器
> 参考《深入理解Java虚拟机》9.3 实战：自己动手实现远程执行功能
# 功能
## Done
- [x] 自定义[ClassLoader](src/main/java/com/shao/MyClassLoader.java)从而实现重复加载同一个类
- [x] 自定义[HackSystem](src/main/java/com/shao/HackSystem.java)用于替换默认的输出流
- [x] 编写[ClassModifier](src/main/java/com/shao/ClassModifier.java)用于将字节码中的`java/lang/System`替换为`com/shao/HackSystem`
- [x] 编写[StringSourceCompiler](src/main/java/com/shao/StringSourceCompiler.java)，实现了动态编译功能。在内存中直接将源代码字符串编译为字节数组
## TODO
- [ ] 搭建网页
- [ ] 完善HackSystem的并发同步问题
## 如何运行

## 运行截图

# 技术细节
TODO
