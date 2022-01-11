package com.shao;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;

/**
 * @author shaojunying
 */
public class Main {
    public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        /**
         * 创建文件Test.java, 内容如下所示：
         * package com.shao;
         *
         * public class Test {
         *     public Test() {
         *     }
         *
         *     public void sayHello() {
         *         System.out.println("Hello World！！！");
         *     }
         * }
         * 将生成的class文件放在/Users/shaojunying/Java-Test/com/shao/ 目录下
         */

        // 指定Class文件的绝对路径
        File file = new File("/Users/shaojunying/Java-Test/com/shao/Test.class");
        MyClassLoader myClassLoader = new MyClassLoader();
        byte[] fileContent = Files.readAllBytes(file.toPath());
        Class<?> clazz = myClassLoader.loadByte(fileContent);
        // 调用类中的方法
        clazz.getMethod("sayHello").invoke(clazz.newInstance());
    }
}
