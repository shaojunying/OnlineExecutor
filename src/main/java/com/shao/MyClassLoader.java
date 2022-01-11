package com.shao;

/**
 * 自定义类加载器
 *
 * 暴露defineClass方法。
 * 每次获取到最新的Class文件内容，
 * 调用defineClass方法，即可得到最新的Class对象
 *
 * @author shao
 */
public class MyClassLoader extends ClassLoader {

    public MyClassLoader() {
        // 使用当前线程的类加载器作为父类加载器
        // 使用MyClassLoader加载待执行的类之后，还需要使用AppClassLoader加载HackSystem，
        // 所以需要将MyClassLoader的parent指定为AppClassLoader
        super(Thread.currentThread().getContextClassLoader());
    }

    public Class<?> loadByte(byte[] classByte) {
        return defineClass(null, classByte, 0, classByte.length);
    }

}
