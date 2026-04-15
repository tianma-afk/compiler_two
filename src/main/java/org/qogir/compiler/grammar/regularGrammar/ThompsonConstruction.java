package org.qogir.compiler.grammar.regularGrammar;

import org.qogir.compiler.FA.State;
import org.qogir.compiler.util.graph.LabelEdge;
import org.qogir.compiler.util.tree.DefaultTreeNode;

import java.util.ArrayDeque;

public class ThompsonConstruction {
    private static final Character EPSILON = 'ε';

    public TNFA translate(RegexTreeNode node, RegexTreeNode root) {
        if (node == null) return null;

        // 先重置，再创建任何 TNFA
        if (node == root) {
            State.STATE_ID = 4;
        }

        switch (node.getType()) {
            case 0: // 叶子节点：单个字符 (a, b, c...)
                TNFA tnfa = new TNFA();
                State start = tnfa.getStartState();
                State accept = tnfa.getAcceptingState();
                tnfa.getTransitTable().addEdge(start, accept, node.getValue());
                return tnfa;

            case 1: // 连接节点 (-)
                RegexTreeNode leftChild = (RegexTreeNode) node.getFirstChild();
                RegexTreeNode rightChild = (RegexTreeNode) node.getLastChild();

                TNFA nfa1 = translate(leftChild, root);
                TNFA nfa2 = translate(rightChild, root);
                return buildConcatNFA(nfa1, nfa2);

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
     * 2. 构建连接 NFA (Concatenation)
     * 结构: [NFA1] --ε--> [NFA2]
     */
    private TNFA buildConcatNFA(TNFA nfa1, TNFA nfa2) {
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

        return nfa1;
    }

    /**
     * 3. 构建选择 NFA (Union / Or)
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
     * 4. 构建闭包 NFA (Kleene Star)
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
