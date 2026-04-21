package org.qogir.compiler.grammar.regularGrammar;

import org.qogir.compiler.FA.State;

import java.util.ArrayList;

/**
 * 基于 McNaughton-Yamada-Thompson 算法将正则表达式语法树转换为 TNFA。
 * <p>
 * 本类的核心入口为 {@link #translate(RegexTreeNode, RegexTreeNode)}，
 * 根据节点类型递归构建基础、连接、并集和闭包四类 NFA 结构。
 */
public class ThompsonConstruction {
    private static final Character EPSILON = 'ε';

    /**
     * 将语法树中的某个节点递归翻译为 TNFA。
     * <p>
     * 当 {@code node == root} 时会重置状态编号，以保证同一次构造过程中的状态编号连续。
     *
     * @param node 当前待翻译的语法树节点
     * @param root 语法树根节点
     * @return 当前节点对应的 TNFA；当输入节点为空或类型不支持时返回 {@code null}
     */
    public TNFA translate(RegexTreeNode node, RegexTreeNode root) {
        if (node == null) return null;

        // 先重置，再创建任何 TNFA
        if (node == root) {
            State.STATE_ID = 0;//重置DFA状态编号
        }

        switch (node.getType()) {
            case 0: // 叶子节点：单个字符 (a, b, c...)
                TNFA tnfa = new TNFA();
                State start = tnfa.getStartState();
                State accept = tnfa.getAcceptingState();
                tnfa.getTransitTable().addEdge(start, accept, node.getValue());
                return tnfa;

            case 1: // 连接节点 (-)
                ArrayList<RegexTreeNode> Childs = new ArrayList<>();
                RegexTreeNode Child = (RegexTreeNode) node.getFirstChild();
                while(Child != null) {
                    Childs.add(Child);
                    Child = (RegexTreeNode) Child.getNextSibling();
                }
                ArrayList<TNFA> nfas = new ArrayList<>();
                for (RegexTreeNode child : Childs) {
                    TNFA nfa = translate(child, root);
                    nfas.add(nfa);
                }
                return buildConcatNFA(nfas);

            case 2: // 选择节点 (|)
                RegexTreeNode leftPart = (RegexTreeNode) node.getFirstChild();
                RegexTreeNode rightPart = (RegexTreeNode) node.getLastChild();

                TNFA leftNFA = translate(leftPart, root);
                TNFA rightNFA = translate(rightPart, root);
                return buildUnionNFA(leftNFA, rightNFA);

            case 3: // 闭包节点 (*)
                RegexTreeNode child = (RegexTreeNode) node.getFirstChild();
                TNFA childNFA = translate(child, root);
                return buildStarNFA(childNFA);

            default:
                System.err.println("Unknown node type: " + node.getType());
                return null;
        }
    }

    /**
     * 构建连接（Concatenation）NFA。
     * <p>
     * 连接规则为将前一段 NFA 的接受状态通过 ε 边连接到后一段 NFA 的开始状态。
     *
     * @param nfas 需要按顺序连接的 NFA 列表
     * @return 连接后的 NFA；当输入列表为空时返回 {@code null}
     */
    private TNFA buildConcatNFA(ArrayList<TNFA> nfas) {
        if(nfas.isEmpty()){
            return null;
        }
        if(nfas.size() == 1){
            return nfas.get(0);
        }
        TNFA nfa1 = nfas.get(0);
        for(int i = 1; i < nfas.size(); i++) {
            TNFA nfa2 = nfas.get(i);
            State nfa1Accept = nfa1.getAcceptingState();
            State nfa2Start = nfa2.getStartState();
            State nfa2Accept = nfa2.getAcceptingState();

            // 合并 nfa2 到 nfa1
            nfa1.getTransitTable().merge(nfa2.getTransitTable());

            // 添加 ε 连接：nfa1接受状态 → nfa2开始状态
            nfa1.getTransitTable().addEdge(nfa1Accept, nfa2Start, EPSILON);

            // 更新状态类型
            nfa1Accept.setType(State.MIDDLE);
            nfa2Start.setType(State.MIDDLE);

            // 更新 nfa1 的接受状态为 nfa2 的接受状态
            nfa1.setAcceptingState(nfa2Accept);
        }
        return nfa1;
    }

    /**
     * 构建并集（Union / Or）NFA。
     *
     * @param nfa1 左分支 NFA
     * @param nfa2 右分支 NFA
     * @return 并集后的 NFA
     */
    private TNFA buildUnionNFA(TNFA nfa1, TNFA nfa2) {
        TNFA result = new TNFA();
        State newStart = result.getStartState();
        State newAccept = result.getAcceptingState();

        // 合并两个 NFA
        result.getTransitTable().merge(nfa1.getTransitTable());
        result.getTransitTable().merge(nfa2.getTransitTable());

        // 添加 ε 边
        result.getTransitTable().addEdge(newStart, nfa1.getStartState(), EPSILON);
        result.getTransitTable().addEdge(newStart, nfa2.getStartState(), EPSILON);
        result.getTransitTable().addEdge(nfa1.getAcceptingState(), newAccept, EPSILON);
        result.getTransitTable().addEdge(nfa2.getAcceptingState(), newAccept, EPSILON);

        // 更新旧状态类型
        nfa1.getStartState().setType(State.MIDDLE);
        nfa2.getStartState().setType(State.MIDDLE);
        nfa1.getAcceptingState().setType(State.MIDDLE);
        nfa2.getAcceptingState().setType(State.MIDDLE);

        return result;
    }

    /**
     * 构建 Kleene 闭包（Star）NFA。
     *
     * @param nfa 需要进行闭包运算的子 NFA
     * @return 闭包后的 NFA
     */
    private TNFA buildStarNFA(TNFA nfa) {
        TNFA result = new TNFA();
        State newStart = result.getStartState();
        State newAccept = result.getAcceptingState();

        // 合并子 NFA
        result.getTransitTable().merge(nfa.getTransitTable());

        // 添加 ε 边
        result.getTransitTable().addEdge(newStart, nfa.getStartState(), EPSILON);
        result.getTransitTable().addEdge(newStart, newAccept, EPSILON);
        result.getTransitTable().addEdge(nfa.getAcceptingState(), nfa.getStartState(), EPSILON);
        result.getTransitTable().addEdge(nfa.getAcceptingState(), newAccept, EPSILON);

        // 更新旧状态类型
        nfa.getStartState().setType(State.MIDDLE);
        nfa.getAcceptingState().setType(State.MIDDLE);

        return result;
    }
}
