package com.shao;

import com.shao.execute.ClassModifier;
import com.shao.execute.HackSystem;
import com.shao.execute.MyClassLoader;

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

        // 替换java/lang/System为自定义的com/shao/HackSystem
        ClassModifier classModifier = new ClassModifier(fileContent);
        byte[] bytes = classModifier.modifyUTF8Constant("java/lang/System", "com/shao/execute/HackSystem");

        Class<?> clazz = myClassLoader.loadByte(bytes);
        // 调用类中的方法
        clazz.getMethod("sayHello").invoke(clazz.newInstance());
        clazz.getMethod("sayHello").invoke(clazz.newInstance());

        System.out.println("从HackSystem中获取的字符串: \n" + HackSystem.getBufferString());
    }
}
