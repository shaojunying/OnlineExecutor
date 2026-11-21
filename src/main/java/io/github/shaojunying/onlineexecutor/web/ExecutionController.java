package com.shao.controller;

import com.shao.execute.ClassModifier;
import com.shao.execute.HackSystem;
import com.shao.execute.MyClassLoader;
import com.shao.compile.StringSourceCompiler;
import com.shao.service.ExecutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author shaojunying
 */
@Controller
public class ExecutorController {

    private final ExecutorService executorService;

    private static final String DEFAULT_CODE =
            "public class Run {\n"
            + "    public static void main(String[] args) {\n"
            + "        System.out.println(111);\n"
            + "    }\n"
            + "}";

    @Autowired
    public ExecutorController(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @RequestMapping("executor")
    public String page(Model model) {
        model.addAttribute("code", DEFAULT_CODE);
        return "executor";
    }

    @RequestMapping(value = "run")
    public String run(@RequestParam String code, Model model) {
        String output = executorService.executor(code);

        model.addAttribute("code", code);
        model.addAttribute("output", output);
        return "executor";
    }

}
