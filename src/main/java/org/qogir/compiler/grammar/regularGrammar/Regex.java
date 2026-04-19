package org.qogir.compiler.grammar.regularGrammar;

/**
 * 正则表达式定义对象。
 * <p>
 * 采用 {@code name := regex} 形式：
 * {@code name} 为模式名（可用于词法分析中的 token 类型），
 * {@code regex} 为对应正则表达式字符串。
 */
public class Regex {
    private final String name;
    private final String regex;
    private final int priority;

    /**
     * 构造一个正则表达式定义。
     *
     * @param name 模式名
     * @param regex 正则表达式文本
     * @param priority 优先级，值越小优先级通常越高（由调用方约定）
     */
    public Regex(String name, String regex, int priority){
        this.name = name;
        this.regex = regex;
        this.priority = priority;
    }

    /**
     * 获取模式名。
     *
     * @return 模式名
     */
    public String getName() {
        return this.name;
    }

    /**
     * 获取正则表达式文本。
     *
     * @return 正则表达式字符串
     */
    public String getRegex() {
        return this.regex;
    }

    /**
     * 获取模式优先级。
     *
     * @return 优先级整数
     */
    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return this.name + " := " + this.regex;
    }
}
