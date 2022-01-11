package com.shao;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author shaojunying
 */
public class Main {
    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
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
        File file = new File("/Users/shaojunying/Java-Test");
        URL url = file.toURI().toURL();

        ClassLoader loader = new URLClassLoader(new URL[]{url});

        Class<?> clazz = loader.loadClass("com.shao.Test");

        // 调用类中的方法
        clazz.getMethod("sayHello").invoke(clazz.newInstance());
    }
}
