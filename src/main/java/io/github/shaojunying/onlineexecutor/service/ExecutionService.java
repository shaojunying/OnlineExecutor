package com.shao.service;

import com.shao.compile.StringSourceCompiler;
import com.shao.execute.JavaClassExecutor;
import org.springframework.stereotype.Service;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/**
 * @author shaojunying
 */
@Service
public class ExecutorService {

    private static final String NO_OUTPUT = "Nothing...";

    public String executor(String source) {

        // 首先编译代码
        StringSourceCompiler compiler = new StringSourceCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        byte[] bytes;
        try {
            bytes = compiler.compile(source, diagnostics);
        }catch (IllegalArgumentException e){
            // 提取类名失败
            return e.getMessage();
        }
        if (bytes == null) {
            // 编译失败 返回错误信息
            StringBuilder builder = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                builder.append(diagnostic.toString());
                builder.append(System.lineSeparator());
            }
            return builder.toString();
        }

        // 执行代码
        JavaClassExecutor executor = new JavaClassExecutor();
        String output = executor.execute(bytes);

        if (output == null || output.isEmpty()) {
            return NO_OUTPUT;
        }
        return output;
    }

}
