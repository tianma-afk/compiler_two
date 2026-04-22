package org.qogir.compiler.grammar.regularGrammar.scanner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.junit.Test;
import org.qogir.compiler.grammar.regularGrammar.RegularGrammar;
import org.qogir.compiler.grammar.regularGrammar.TNFA;
import org.qogir.simulation.logger.ThompsonLogger;
import org.qogir.simulation.scanner.Scanner;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AutoTest_NFA {

    @Test
    public void testWithJsonFile() throws Exception {
        // 使用 Fastjson 读取 JSON 文件
        Map<String, List<String>> config;

        try (InputStream is = AutoTest_NFA.class.getResourceAsStream("/test_regex.json")) {
            assertNotNull("test_regex.json 不存在", is);
            // 方法1：将 InputStream 转为 String 再解析
            String jsonContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            config = JSON.parseObject(jsonContent, new TypeReference<Map<String, List<String>>>() {});
        }

        List<String> regexList = config.get("regexes");
        assertNotNull("regexes 不能为空", regexList);
        String[] regexes = regexList.toArray(String[]::new);

        // 可选：打印读取到的正则表达式
        System.out.println("读取到的正则表达式：");
        for (String regex : regexes) {
            System.out.println("  " + regex);
        }
        System.out.println();

        // test defining a regular grammar
        RegularGrammar rg = new RegularGrammar(regexes);
        System.out.println(rg);

        // test building a grammar for the grammar
        Scanner scanner = new Scanner(rg);

        // test constructing the regex tree
        System.out.println(scanner.constructRegexTrees().toString());

        // test constructing the NFA
        TNFA nfa = scanner.constructNFA();
        assertNotNull("NFA 构造失败", nfa);
        System.out.println(nfa.toString());
    }

    // 如果需要更详细的测试（包含日志）
    @Test
    public void testWithJsonFileAndLoggers() throws Exception {
        // 重置日志器
        ThompsonLogger.reset();

        // 读取 JSON 配置
        Map<String, List<String>> config;
        try (InputStream is = AutoTest_NFA.class.getResourceAsStream("/test_regex.json")) {
            assertNotNull("test_regex.json 不存在", is);
            String jsonContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            config = JSON.parseObject(jsonContent, new TypeReference<Map<String, List<String>>>() {});
        }

        List<String> regexList = config.get("regexes");
        assertNotNull("regexes 不能为空", regexList);
        String[] regexes = regexList.toArray(String[]::new);

        // 执行测试
        RegularGrammar rg = new RegularGrammar(regexes);
        Scanner scanner = new Scanner(rg);
        TNFA nfa = scanner.constructNFA();
        assertNotNull("NFA 构造失败", nfa);

        System.out.println("#############ThompsonLogger#############");
        System.out.println(ThompsonLogger.constructionLogger.returnStepQueues());
        System.out.println();
    }

    @Test
    public void testInvalidRegexesWithJsonFile() throws Exception {
        Map<String, List<String>> config;
        try (InputStream is = AutoTest_NFA.class.getResourceAsStream("/test_regex.json")) {
            assertNotNull("test_regex.json 不存在", is);
            String jsonContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            config = JSON.parseObject(jsonContent, new TypeReference<Map<String, List<String>>>() {});
        }

        List<String> invalidRegexList = config.get("invalidRegexes");
        assertNotNull("invalidRegexes 不能为空", invalidRegexList);

        for (String invalidRegex : invalidRegexList) {
            try {
                RegularGrammar rg = new RegularGrammar(new String[]{invalidRegex});
                Scanner scanner = new Scanner(rg);
                scanner.constructNFA();
                fail("非法正则未触发异常: " + invalidRegex);
            } catch (Exception e) {
                System.out.println("捕获到预期异常: " + invalidRegex + " -> " + e.getMessage());
            }
        }
    }
}