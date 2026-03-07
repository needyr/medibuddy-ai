package cn.needy.javaai.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-05 10:10
 **/

@Component
public class CalculatorTools {
    @Tool(
            name = "加法运算",
            value = "将两个参数a和b参数相加并且返回运算结果"
    )
    public double sum(@ToolMemoryId String memoryId, double a, double b) {
        System.out.println("调用sum方法, memoryId" + memoryId);
        return a + b;
    }

    @Tool(
            name = "平方根运算"
    )
    public double squareRoot(@ToolMemoryId String memoryId, double a) {
        System.out.println("调用squareRoot方法, memoryId" + memoryId);
        return Math.sqrt(a);
    }
}
