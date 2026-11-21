package com.shao.execute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author shaojunying
 */
public class JavaClassExecutor {


    private static final String STANDARD_OUTPUT = "java/lang/System";
    private static final String SELF_DEFINED_OUTPUT = "com/shao/execute/HackSystem";

    /**
     * 执行字节码，并返回执行过程中的输出
     */
    public String execute(byte[] bytes) {
        // 替换字节码中的System
        ClassModifier modifier = new ClassModifier(bytes);
        bytes = modifier.modifyUTF8Constant(STANDARD_OUTPUT, SELF_DEFINED_OUTPUT);
        // 运行该字节码
        MyClassLoader myClassLoader = new MyClassLoader();
        Class<?> clazz = myClassLoader.loadByte(bytes);

        // 执行main方法
        try {
            Method mainMethod = clazz.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[]{});
        } catch (IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // 这是被执行类内部未捕获的异常，写入到其输出流中
            e.getCause().printStackTrace(HackSystem.err);
        }

        String output = HackSystem.getBufferString();
        HackSystem.clearBuffer();
        return output;
    }

}
