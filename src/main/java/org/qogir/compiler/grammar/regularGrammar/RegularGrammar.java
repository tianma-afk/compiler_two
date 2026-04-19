package org.qogir.compiler.grammar.regularGrammar;

import org.qogir.compiler.util.StringUtil;

import java.util.ArrayList;

/**
 * 正则文法定义。
 * <p>
 * 支持字符集：字母、数字以及 {@code ε}；
 * 支持运算：并集 {@code |}、连接、闭包 {@code *} 与括号分组。
 * <p>
 * 输入需采用 {@code 名称 := 正则表达式} 形式，例如：
 * {@code id := c(a|b)*}。
 */
public class RegularGrammar {
    public ArrayList<Character> symbols = new ArrayList<Character>();//the alphabet for regular grammar
    private final ArrayList<Regex> patterns = new ArrayList<>();

    /**
     * 基于字符串数组构造正则文法。
     *
     * @param regexes 形如 {@code 名称 := 表达式} 的规则数组
     */
    public RegularGrammar(String[] regexes){
        for(String r: regexes){
            String name = r.substring(0, r.lastIndexOf(":=") - 1);
            String regex = r.substring(r.lastIndexOf(":=") + 2);

            StringUtil stringUtil = new StringUtil();
            name = stringUtil.trim(name);
            regex = stringUtil.trim(regex);


            Regex p = new Regex(name, regex, 0);
            this.patterns.add(p);
            for(Character ch : regex.toCharArray()){
                if ((Character.isDigit(ch) || Character.isLetter(ch)) && ch != 'ε' && !symbols.contains(ch)) {
                    this.symbols.add(ch);
                }
            }
        }
    }

    /**
     * 获取文法字母表（不包含 {@code ε}）。
     *
     * @return 字母表字符列表
     */
    public ArrayList<Character> getSymbols(){
        return this.symbols;
    }

    /**
     * 获取文法中的全部正则模式。
     *
     * @return 正则模式列表
     */
    public ArrayList<Regex> getPatterns() {
        return patterns;
    }

    @Override
    public String toString() {
        return "Regular Grammar\n" + "Alphabet:" + symbols.toString() + "\n" + "Regexes:\n" + patterns;
    }
}
