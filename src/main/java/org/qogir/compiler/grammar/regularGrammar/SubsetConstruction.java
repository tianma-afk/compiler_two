package org.qogir.compiler.grammar.regularGrammar;

import org.qogir.compiler.FA.State;
import org.qogir.compiler.util.graph.LabelEdge;
import org.qogir.compiler.util.graph.LabeledDirectedGraph;

import java.util.*;

/**
 * The subset construction Algorithm for converting an NFA to a DFA.
 * The subset construction Algorithm takes an NFA N as input and output a DFA D accepting the same language as N.
 * The main mission is to eliminate ε-transitions and multi-transitions in NFA and construct a transition table for D.
 * The algorithm can be referred to {@see }
 */
public class SubsetConstruction {

    /**
     * Eliminate all ε-transitions reachable from a single state in NFA through the epsilon closure operation.
     * @param s a single state of NFA
     * @param tb the transition table of NFA
     * @return a set of state reachable from the state s on ε-transition
     */
    private HashMap<Integer, State> epsilonClosures(State s, LabeledDirectedGraph<State> tb){
        if (!tb.vertexSet().contains(s)) { //if vertex s not in the transition table
            return null;
        }

        HashMap<Integer,State> nfaStates = new HashMap<>();
        nfaStates.put(s.getId(),s);
        //广搜
        Queue<State> queue = new LinkedList<>();
        queue.add(s);//添加起始状态
        while (!queue.isEmpty()) {
            State current = queue.poll();
            for (LabelEdge le : tb.edgeSet()){
                if(le.getLabel().equals('ε') && le.getSource().equals(current)){
                    State target = (State) le.getTarget();// 获取新状态
                    if (!nfaStates.containsKey(target.getId())) {//如果新状态不在集合中
                        nfaStates.put(target.getId(), target);
                        queue.offer(target); // 新状态入队，继续寻找它的 ε 边
                    }
                }
            }
        }
        return nfaStates;
    }

    /**
     * Eliminate all ε-transitions reachable from a  state set in NFA through the epsilon closure operation
     * @param ss a state set of NFA
     * @param tb the transition table of NFA
     * @return a set of state reachable from the state set on ε-transition
     */

    public HashMap<Integer, State> epsilonClosure(HashMap<Integer, State> ss, LabeledDirectedGraph<State> tb){
        HashMap<Integer,State> nfaStates = new HashMap<>();
        for(State s : ss.values()){
            nfaStates.putAll(epsilonClosures(s,tb));
        }
        return nfaStates;
    }

    /**
     *
     * @param s
     * @param ch
     * @param tb
     * @return
     */
    private HashMap<Integer,State> moves(State s, Character ch, LabeledDirectedGraph<State> tb){
        HashMap<Integer,State> nfaStates = new HashMap<>();
        for(LabelEdge le : tb.edgeSet()){
            if(le.getLabel().equals(ch) && le.getSource().equals(s)){
                State target = (State) le.getTarget();// 获取新状态
                if (!nfaStates.containsKey(target.getId())) {//如果新状态不在集合中
                    nfaStates.put(target.getId(), target);
                }
            }
        }
        return nfaStates;
    }

    public HashMap<Integer,State> move(HashMap<Integer, State> ss, Character ch, LabeledDirectedGraph<State> tb){
        HashMap<Integer,State> nfaStates = new HashMap<>();
        for(State s : ss.values()){
            nfaStates.putAll(moves(s,ch,tb));
        }
        return nfaStates;
    }

    public HashMap<Integer,State> epsilonClosureWithMove(HashMap<Integer, State> sSet, Character ch, LabeledDirectedGraph<State> tb){
        HashMap<Integer,State> states = new HashMap<>();
        states.putAll(epsilonClosure(move(sSet, ch, tb),tb));
        return states;
    }
    public RDFA subSetConstruct(TNFA tnfa){
        RDFA dfa = new RDFA();
        LabeledDirectedGraph<State> nfaGraph = tnfa.getTransitTable();

        State nfaStart = tnfa.getStartState();
        HashMap<Integer, State> startStateSetMap = epsilonClosures(nfaStart, nfaGraph);
        Map<Set<State>, State> dfaStateMap = new HashMap<>();//DFA状态集合映射 DFA状态集合 -> DFA状态对象

        if (startStateSetMap == null) {//如果起始状态集合为空
            startStateSetMap = new HashMap<>();
            startStateSetMap.put(nfaStart.getId(), nfaStart);
        }

        State.STATE_ID = 0;//重置DFA状态编号
        State dfaStart = new State(); // 创建 DFA 的起始状态对象
        dfaStart.setType(0);
        dfa.setStartState(dfaStart); // 设置为 DFA 的起点
        dfa.getTransitTable().addVertex(dfaStart);
        dfa.setStateMappingBetweenDFAAndNFA(dfaStart, startStateSetMap);

        Queue<Set<State>> queue = new LinkedList<>();
        Set<State> startStateSet = new HashSet<>(startStateSetMap.values());//创建起始状态集合

        dfaStateMap.put(startStateSet, dfaStart);
        queue.offer(startStateSet);

        Set<Character> alphabet = getAlphabet(nfaGraph);//获取NFA的输入符号集
        //BFS
        while (!queue.isEmpty()) {
            // 取出一个 NFA 状态集合 (代表当前的 DFA 状态)
            Set<State> currentNfaSet = queue.poll();
            State currentDfaState = dfaStateMap.get(currentNfaSet);

            // 尝试每一个可能的输入字符
            for (Character ch : alphabet) {
                // 准备 move 函数的输入 (需要转回 HashMap)
                HashMap<Integer, State> currentMapInput = new HashMap<>();
                for (State s : currentNfaSet) {
                    currentMapInput.put(s.getId(), s);
                }

                // 核心公式：T' = ε-closure(move(T, ch))
                HashMap<Integer, State> nextNfaSetMap = epsilonClosureWithMove(currentMapInput, ch, nfaGraph);

                if (nextNfaSetMap == null || nextNfaSetMap.isEmpty()) {
                    continue; // 如果走不通，跳过
                }

                // 得到新的 NFA 状态集合
                Set<State> nextNfaSet = new HashSet<>(nextNfaSetMap.values());

                // 检查这个新集合是否已经对应了一个 DFA 状态
                State nextDfaState = dfaStateMap.get(nextNfaSet);

                if (nextDfaState == null) {
                    // 如果是新状态，创建它
                    nextDfaState = new State();
                    dfa.getTransitTable().addVertex(nextDfaState);

                    // 记录映射关系
                    dfa.setStateMappingBetweenDFAAndNFA(nextDfaState, nextNfaSetMap);

                    // 存入映射表并加入队列，以便后续处理它的出边
                    dfaStateMap.put(nextNfaSet, nextDfaState);
                    queue.offer(nextNfaSet);
                }

                // 5. 添加 DFA 转移边: currentDfaState --ch--> nextDfaState
                dfa.getTransitTable().addEdge(currentDfaState, nextDfaState, ch);
            }
        }

        //标记 DFA 的接受状态
        // 逻辑：如果 DFA 状态对应的 NFA 集合中包含 NFA 的接受状态，则该 DFA 状态也是接受状态
        State nfaAccepting = tnfa.getAcceptingState();
        for (Map.Entry<Set<State>, State> entry : dfaStateMap.entrySet()) {
            if (entry.getKey().contains(nfaAccepting)) {
                entry.getValue().setType(State.ACCEPT);
            }
        }

        //  重新编号 ID，让状态看起来更整洁
        dfa.renumberSID();
        //Add your implementation
        return dfa;
    }
    private Set<Character> getAlphabet(LabeledDirectedGraph<State> graph) {
        //获取NFA的输入符号集
        Set<Character> alphabet = new HashSet<>();
        for (LabelEdge edge : graph.edgeSet()) {
            Character label = edge.getLabel();
            // 排除 ε，只保留实际字符
            if (label != null && !label.equals('ε')) {
                alphabet.add(label);
            }
        }
        return alphabet;
    }

}
