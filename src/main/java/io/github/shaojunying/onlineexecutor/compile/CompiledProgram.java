package io.github.shaojunying.onlineexecutor.compile;

import java.util.Map;

/**
 * Class files produced from one source file.
 *
 * @param mainClassName  binary name of the class to start, for example {@code demo.Main}
 * @param classes        class file bytes by binary name, including nested and anonymous classes
 */
public record CompiledProgram(String mainClassName, Map<String, byte[]> classes) {

    public CompiledProgram {
        classes = Map.copyOf(classes);
    }
}
