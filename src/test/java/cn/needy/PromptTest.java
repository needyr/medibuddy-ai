package cn.needy;

import cn.needy.javaai.assistant.SeparateChatAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @program: java-ai-langchain4j
 * @description:
 * @author: yeguobingfen
 * @create: 2026-03-04 14:30
 **/

@SpringBootTest
public class PromptTest {
    @Autowired
    private SeparateChatAssistant separateChatAssistant;
    @Test
    public void testPrompt(){
        String res = separateChatAssistant.chat(3,"我是谁？");
        System.out.println(res);
    }

    @Test
    public void testPrompt2(){
        String res = separateChatAssistant.chat(4,"今天几号？");
        System.out.println(res);
    }

    @Test
    public void testPrompt3(){
        String res = separateChatAssistant.chat2(6,"你是谁，今天几号？");
        System.out.println(res);
    }

    @Test
    public void testPrompt4() {
        //获取用户信息
        String username = "张三";
        int age = 18;

        String res = separateChatAssistant.chat3(20, "我是谁？我多大了", username, age);
        System.out.println( res);
    }



    @Test
    //编码
    //给定一个字符串以及拼接符，输出反序的字符串。比如 输入"p1,p2,p3"，分割符号是逗号，输出"p3,p2,p1"。（要求：用java基本类型实现，不要用集合类型的类库）
    /**
     * @param needReverseValue 需要反序的字符串
     * @param delimiter 分割符号
     */
    public static String reverseDelimitedString(String needReverseValue, String delimiter) {
        // 判空
        if (needReverseValue == null || needReverseValue.isEmpty()) {
            return needReverseValue;
        }

        if (delimiter == null || delimiter.isEmpty()) {
            return needReverseValue;
        }

        String result = "";
        int delimLen = delimiter.length();
        int len = needReverseValue.length();

        // 从后向前遍历，逐个提取片段并拼接到结果中
        int end = len;
        boolean firstSegment = true;

        while (end > 0) {
            // 向前查找分隔符
            int start = end - delimLen;
            while (start >= 0) {
                // 检查从 start 开始是否匹配分隔符
                boolean match = true;
                for (int k = 0; k < delimLen; k++) {
                    if (needReverseValue.charAt(start + k) != delimiter.charAt(k)) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    // 找到分隔符，提取片段
                    start = start + delimLen;
                    break;
                }
                start--;
            }

            if (start < 0) {
                start = 0;
            }

            // 添加分隔符
            if (!firstSegment) {
                result += delimiter;
            }

            // 提取当前片段并追加到结果
            for (int i = start; i < end; i++) {
                result += needReverseValue.charAt(i);
            }

            firstSegment = false;
            end = start - delimLen;
        }

        return result;
    }


    @Test
    public void testReverseDelimitedString_NormalCase() {
        assertEquals("p3,p2,p1", reverseDelimitedString("p1,p2,p3", ","));
        assertEquals("c,b,a", reverseDelimitedString("a,b,c", ","));
        assertEquals("3-2-1", reverseDelimitedString("1-2-3", "-"));
    }

    @Test
    public void testReverseDelimitedString_SingleElement() {
        assertEquals("single", reverseDelimitedString("single", ","));
        assertEquals("hello", reverseDelimitedString("hello", "-"));
        assertEquals("test", reverseDelimitedString("test", "::"));
    }

    @Test
    public void testReverseDelimitedString_TwoElements() {
        assertEquals("b,a", reverseDelimitedString("a,b", ","));
        assertEquals("2-1", reverseDelimitedString("1-2", "-"));
        assertEquals("world:hello", reverseDelimitedString("hello:world", ":"));
    }

    @Test
    public void testReverseDelimitedString_MultiCharDelimiter() {
        assertEquals("c::b::a", reverseDelimitedString("a::b::c", "::"));
        assertEquals("step3->step2->step1", reverseDelimitedString("step1->step2->step3", "->"));
        assertEquals("third||second||first", reverseDelimitedString("first||second||third", "||"));
    }

    @Test
    public void testReverseDelimitedString_NullInput() {
        assertNull(reverseDelimitedString(null, ","));
        assertNull(reverseDelimitedString(null, "-"));
    }

    @Test
    public void testReverseDelimitedString_EmptyInput() {
        assertEquals("", reverseDelimitedString("", ","));
        assertEquals("", reverseDelimitedString("", "-"));
    }

    @Test
    public void testReverseDelimitedString_NullDelimiter() {
        assertEquals("test", reverseDelimitedString("test", null));
        assertEquals("abc", reverseDelimitedString("abc", null));
    }

    @Test
    public void testReverseDelimitedString_EmptyDelimiter() {
        assertEquals("test", reverseDelimitedString("test", ""));
        assertEquals("hello", reverseDelimitedString("hello", ""));
    }

    @Test
    public void testReverseDelimitedString_WithSpaces() {
        assertEquals(" c , b , a", reverseDelimitedString("a , b , c", ","));
        assertEquals("item3 item2 item1", reverseDelimitedString("item1 item2 item3", " "));
    }

    @Test
    public void testReverseDelimitedString_RepeatedDelimiters() {
        assertEquals("c,,b,,a", reverseDelimitedString("a,,b,,c", ",,"));
        assertEquals("..c..b..a", reverseDelimitedString("a..b..c", ".."));
    }

    @Test
    public void testReverseDelimitedString_LongString() {
        String input = "part1|part2|part3|part4|part5";
        String expected = "part5|part4|part3|part2|part1";
        assertEquals(expected, reverseDelimitedString(input, "|"));
    }

    @Test
    public void testReverseDelimitedString_SpecialCharacters() {
        assertEquals("z*y*x", reverseDelimitedString("x*y*z", "*"));
        assertEquals("c?b?a", reverseDelimitedString("a?b?c", "?"));
        assertEquals("!c!b!a", reverseDelimitedString("a!b!c", "!"));
    }

    @Test
    public void testReverseDelimitedString_Numbers() {
        assertEquals("30,20,10", reverseDelimitedString("10,20,30", ","));
        assertEquals("999-888-777", reverseDelimitedString("777-888-999", "-"));
    }

    @Test
    public void testReverseDelimitedString_MixedContent() {
        assertEquals("itemC,itemB,itemA", reverseDelimitedString("itemA,itemB,itemC", ","));
        assertEquals("test-999-test-888-test-777", reverseDelimitedString("test-777-test-888-test-999", "-"));
    }
}
