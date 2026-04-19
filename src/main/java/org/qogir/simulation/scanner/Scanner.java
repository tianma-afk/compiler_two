package org.qogir.simulation.scanner;

import org.qogir.compiler.FA.State;
import org.qogir.compiler.grammar.regularGrammar.*;
import org.qogir.compiler.util.graph.LabelEdge;

import java.util.HashMap;

/**
 * 正则文法扫描器与自动机构造入口。
 * <p>
 * 提供从正则表达式到语法树、NFA、DFA 及最小化 DFA 的构造流程。
 */
public class Scanner {

    /**
     * 正则文法定义。
     */
    private final RegularGrammar rg;

    /**
     * Hold regex trees for each regex
     */
    private final HashMap<Regex, RegexTree> regexToRegexTree = new HashMap<>();

    /**
     * Hold NFAs for each regex
     */
    private final HashMap<Regex, TNFA> RegexToNFA = new HashMap<>();

    /**
     * Hold NFA sets for each DFA State
     */
    private final HashMap<State, HashMap<Integer, State>> DFAToNFAs = new HashMap<>();

    /**
     * Hold equivalent state sets for minimized DFA
     */
    private final HashMap<State, HashMap<Integer, State>> MinDFAToDFAs = new HashMap<>();

    public Scanner(RegularGrammar rg){
        this.rg = rg;
    }

    public Scanner(String[] regexes){
        rg = new RegularGrammar(regexes);
    }

    public HashMap<Regex, RegexTree> getRegexToRegexTree() {
        return regexToRegexTree;
    }

    public HashMap<Regex, TNFA> getRegexToNFA() {
        return RegexToNFA;
    }

    public HashMap<State, HashMap<Integer, State>> getDFAToNFAs() {
        return DFAToNFAs;
    }

    public HashMap<State, HashMap<Integer, State>> getMinDFAToDFAs() {
        return MinDFAToDFAs;
    }

    /**
     * 基于 {@link ParseRegex#parse()} 将单个正则表达式构建为语法树。
     *
     * @param r 正则表达式
     * @return 对应语法树 {@link RegexTree}
     */
    public RegexTree constructRegexTree(Regex r){
        ParseRegex parser = new ParseRegex(r);
        RegexTree tree = parser.parse();
        regexToRegexTree.put(r,tree);
        //System.out.println("***tree***:"+JSON.toJSONString(tree));
        return tree;
    }


    /**
     * 为当前文法中的所有正则表达式构建语法树。
     * <p>
     * 内部调用 {@link #constructRegexTree(Regex)}，也可用于仅包含一个正则的文法。
     *
     * @return 文法中每个正则对应的语法树映射
     */

    public HashMap<Regex,RegexTree> constructRegexTrees(){
        if(rg == null)
            return null;
        for(Regex r: rg.getPatterns()){
            regexToRegexTree.put(r,constructRegexTree(r));
        }
        return regexToRegexTree;
    }

    /**
     * 为单个正则表达式构建 TNFA。
     * <p>
     * 构建过程基于 McNaughton-Yamada-Thompson 算法
     * {@link ThompsonConstruction#translate(RegexTreeNode, RegexTreeNode)}。
     *
     * @param r 正则表达式
     * @return 对应 TNFA
     */
    public TNFA constructRegexNFA(Regex r) {
        RegexTree tree = constructRegexTree(r);
        ThompsonConstruction thompsonConstruction = new ThompsonConstruction();
        TNFA nfa = thompsonConstruction.translate(tree.getRoot(), tree.getRoot());

        setAlphabetForNfa(r, nfa);

        return nfa;
    }

    /**
     * 根据正则表达式内容提取并设置 NFA 字母表（不包含 ε）。
     *
     * @param r 正则表达式
     * @param nfa 目标 NFA
     */
    public void setAlphabetForNfa(Regex r, TNFA nfa) {
        //add logger queue
        //ThompsonLogger.resetParam();
        for (Character ch : r.getRegex().toCharArray()) {
            if ((Character.isDigit(ch) || Character.isLetter(ch)) && ch != 'ε' && !nfa.getAlphabet().contains(ch)) {
                nfa.getAlphabet().add(ch);
            }
        }
    }

    /**
     * This private method is used to construct an DFA for a regex.
     * The construction is based on subset construction algorithm {@link SubsetConstruction#subSetConstruct}.
     *
     * @param r a regex
     * @return An DFA
     */
    public RDFA constructRegexDFA(Regex r) {
        TNFA nfa = constructRegexNFA(r);
        SubsetConstruction subsetConstruction = new SubsetConstruction();
        RDFA dfa = subsetConstruction.subSetConstruct(nfa);
        dfa.setAlphabet(nfa.getAlphabet());
        return dfa;
    }

    /**
     * Construct NFAs for all regexes in a regular grammar.
     * @return An NFA set
     */
    public HashMap<Regex,TNFA> constructAllNFA(){
        HashMap<Regex, TNFA> rToNfa = new HashMap<>();
        for(Regex r : rg.getPatterns()){
            rToNfa.put(r, constructRegexNFA(r));
        }
        return rToNfa;
    }

    /**
     * Construct minDFAs for all regexes in a regular grammar.
     *
     * @return a DFA set
     */
    public HashMap<Regex, RDFA> constructAllDFA() {
        HashMap<Regex, RDFA> rtodfa = new HashMap<>();
        for (Regex r : rg.getPatterns()) {
            rtodfa.put(r, minimizeDFA(constructRegexDFA(r)));
        }
        return rtodfa;
    }

    /**
     * Construct minDFAs for all regexes in a regular grammar.
     *
     * @param rg a RegularGrammar
     * @return a DFA set
     */
    public HashMap<Regex, RDFA> constructAllDFA(RegularGrammar rg) {
        HashMap<Regex, RDFA> rtodfa = new HashMap<>();
        for (Regex r : rg.getPatterns()) {
            rtodfa.put(r, minimizeDFA(constructRegexDFA(r)));
        }
        return rtodfa;
    }

    /**
     * construct an DFA with an NFA
     * @param nfa an NFA
     * @return an DFA
     */
    public RDFA constructDFA(TNFA nfa) {
        SubsetConstruction sc = new SubsetConstruction();
        RDFA dfa = sc.subSetConstruct(nfa);
        dfa.setAlphabet(nfa.getAlphabet());
        return dfa;
    }

    /**
     * Minimize an DFA by State Minimization algorithm {@link StateMinimization#minimize}
     * @param dfa an DFA
     * @return an DFA
     */
    public RDFA minimizeDFA(RDFA dfa){
        StateMinimization stateMinimization = new StateMinimization();
        RDFA miniDFA = stateMinimization.minimize(dfa);
        miniDFA.setAlphabet(dfa.getAlphabet());
        return miniDFA;
    }

    /**
     * Construct an NFA for a regular grammar.
     * @return An NFA
     */
    public TNFA constructNFA(){
        if(rg.getPatterns().size() == 1){
            Regex r = rg.getPatterns().get(0);
            TNFA nfa = constructRegexNFA(r);
            if(nfa != null)
                this.RegexToNFA.put(r, nfa);
            nfa.setAlphabet(rg.getSymbols());
            return nfa;
        }
        else if(rg.getPatterns().size() > 1) {
            for (Regex r : rg.getPatterns()) {
                TNFA nfa = constructRegexNFA(r);
                if (nfa != null)
                    this.RegexToNFA.put(r, nfa);
            }
            TNFA nfa = new TNFA();
            for (TNFA tn : this.RegexToNFA.values()) {
                if(tn.getStartState().getType() != State.ACCEPTANDSTART){
                    tn.getStartState().setType(State.MIDDLE);
                }
                else{
                    tn.getStartState().setType(State.ACCEPT);
                }
                nfa.getTransitTable().merge(tn.getTransitTable());

                nfa.getTransitTable().addEdge(new LabelEdge(nfa.getStartState(), tn.getStartState(), 'ε'));
            }
            nfa.setAlphabet(rg.getSymbols());
            return nfa;
        }
        return null;
    }

    /**
     * construct DFA for a regular grammar
     * @return a DFA
     */
    public RDFA constructDFA(){
        TNFA nfa = constructNFA();
        SubsetConstruction subsetConstruction = new SubsetConstruction();
        RDFA dfa = subsetConstruction.subSetConstruct(nfa);
        dfa.setAlphabet(rg.symbols);
        return dfa;
    }
}
