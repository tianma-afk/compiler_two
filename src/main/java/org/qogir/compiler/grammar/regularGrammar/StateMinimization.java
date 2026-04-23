package org.qogir.compiler.grammar.regularGrammar;


import org.qogir.compiler.FA.State;
import org.qogir.compiler.util.graph.LabelEdge;
import org.qogir.compiler.util.graph.LabeledDirectedGraph;

import java.util.*;

public class StateMinimization {

    /**
     * Distinguish non-equivalent states in the given DFA.
     *
     * @param dfa the original dfa.
     * @return distinguished equivalent state groups
     */
    public HashMap<Integer, HashMap<Integer, State>> distinguishEquivalentState(RDFA dfa) {
        //Add your implementation

        HashMap<Integer,HashMap<Integer, State>> groupSet = new HashMap<>(); // group set
        return groupSet;
    }

    public RDFA minimize(RDFA dfa) {
//      Map<HashMap<Integer, State>, State> dfaStateMap = new HashMap<>();// DFA状态映射表 旧状态集合 -> 新状态
        List<Set<State>> stateSetList = new ArrayList<>();//用于分组
        LabeledDirectedGraph<State> originalGraph = dfa.getTransitTable();

        //initialize
        for (int i = 0; i <= 2; i++){
            Set<State> group = new HashSet<>();
            for(State s: originalGraph.vertexSet()){
                if(s.getType() == i){
                    group.add(s);
                }
            }
            if (!group.isEmpty()) stateSetList.add(group);
        }
        Set<Character> alphabet = getAlphabet(originalGraph);

        //分离集合
        boolean isChanged = true;
        while (isChanged) {
            isChanged = false;
            List<Set<State>> newStateSetList = new ArrayList<>();
            for(Set<State> currentGroup: stateSetList){
                //如果集合中只有一个元素，无法再分组
                if(currentGroup.size() <= 1){
                    newStateSetList.add(currentGroup);
                    continue;
                }

                //获取状态集合的“行为签名”，用于存储和划分不同组的状态
                Map<List<Integer>, Set<State>> splitBuckets = new HashMap<>();
                for (State s : currentGroup) {
                    // 计算状态 s 的“行为签名”
                    List<Integer> signature = new ArrayList<>(); //自身类型+状态集编号

                    for (char c : alphabet) {
                        State nextState = getSuccessor(originalGraph, s, c);
                        int targetGroupIndex = -1; // -1 表示死路（没有跳转）
                        if (nextState != null) {
                            // 如果不是空的，找到这个状态的状态集合编号
                            targetGroupIndex = stateSetList.indexOf(findGroup(stateSetList, nextState));
                        }
                        signature.add(targetGroupIndex);
                    }
                    // 添加状态 s 到对应的组 （循环后会把这个集合全部s都添加进去）
                    splitBuckets.computeIfAbsent(signature, k -> new HashSet<>()).add(s);
                }
                // 如果拆分出的桶多于 1 个，说明 currentGroup 需要拆分
                if (splitBuckets.size() > 1) {
                    isChanged = true;
                    // 将拆分后的每个桶加入新的划分列表
                    newStateSetList.addAll(splitBuckets.values());
                } else {
                    // 不需要拆分，保持原样
                    newStateSetList.add(currentGroup);
                }
            }
            //更新
            stateSetList = newStateSetList;
        }

        //处理边
        RDFA miniDFA = new RDFA();
        State.STATE_ID = 0;
        Map<Set<State>, State> groupToNewState = new HashMap<>();//旧状态集合 -> 新状态
        //创建新状态
        for(Set<State> group: stateSetList){
            State newState = new State();

            int newType = 0; // 默认为 0
            // 1. 先检查有没有最高优先级的（终态）
            boolean hasFinal = group.stream().anyMatch(s -> s.getType() == 2);
            if (hasFinal) {
                newType = 2;
            }
            // 2. 如果没有终态，再检查有没有次级优先级的（Type 1）
            else {
                boolean hasType1 = group.stream().anyMatch(s -> s.getType() == 1);
                if (hasType1) {
                    newType = 1;
                }
            }
            newState.setType(newType);
            miniDFA.getTransitTable().addVertex(newState);
            HashMap<Integer, State> groupMap = new HashMap<>();
            for(State s : group) groupMap.put(s.getId(), s);
            miniDFA.setStateMappingBetweenDFAAndNFA(newState, groupMap);

            groupToNewState.put(group, newState);
        }

        // 2. 确定起始状态
        State originalStart = dfa.getStartState();
        for (Set<State> group : stateSetList) {
            if (group.contains(originalStart)) {
                miniDFA.setStartState(groupToNewState.get(group));
                break;
            }
        }

        // 3. 添加边
        for(Set<State> group : stateSetList){
            State currentNewState = groupToNewState.get(group);//新状态
            State representative = group.iterator().next();//状态集合的代表（状态集合的第一个元素）
            for (char c : alphabet) {
                State nextOriginal = getSuccessor(originalGraph, representative, c);//下一个状态（原始）
                if (nextOriginal != null) {
                    Set<State> targetGroup = findGroup(stateSetList, nextOriginal);
                    if (targetGroup != null) {
                        State targetNewState = groupToNewState.get(targetGroup);
                        miniDFA.getTransitTable().addEdge(currentNewState, targetNewState, c);
                    }
                }
            }
        }
        return miniDFA;
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

    // 获取状态 s 在输入 c 下的后继状态
    private State getSuccessor(LabeledDirectedGraph<State> graph, State s, char c) {
        for(LabelEdge le : graph.edgeSet()){
            if(le.getSource().equals(s) && le.getLabel().equals(c)){
                State target = (State) le.getTarget();// 获取新状态
                return target;
            }
            else continue;
        }
        return null;
    }

    // 在划分列表中找到包含状态 s 的那个组
    private Set<State> findGroup(List<Set<State>> partitions, State s) {
        for (Set<State> group : partitions) {
            if (group.contains(s)) {
                return group;
            }
        }
        return null;
    }
    //    /**
//     * Used for showing the distinguishing process of state miminization algorithm
//     *
//     * @param stepQueue holds all distinguishing steps
//     * @param GroupSet  is the set of equivalent state groups
//     * @param memo      remarks
//     */
//    public void recordDistinguishSteps(ArrayDeque<String> stepQueue, HashMap<Integer, HashMap<Integer, State>> GroupSet, String memo) {
//        String str = "";
//        str = GroupSetToString(GroupSet);
//        str += ":" + memo;
//        stepQueue.add(str);
//        System.out.println(stepQueue);
//    }
//
//    /**
//     * Display the equivalent state groups
//     *
//     * @param stepQueue
//     */
//    public void showDistinguishSteps(ArrayDeque<String> stepQueue) {
//        int step = 0;
//        String str = "";
//        while (!stepQueue.isEmpty()) {
//            str = stepQueue.poll();
//            System.out.println("Step" + step++ + ":\t" + str + "\r");
//        }
//    }

    private String GroupSetToString(HashMap<Integer,HashMap<Integer, State>> GroupSet){
        String str = "";
        for( Integer g: GroupSet.keySet()){
            String tmp = GroupToString(GroupSet.get(g));
            str += g +  ":" + tmp + "\t" ;
        }
        return str;
    }

    private String GroupToString(HashMap<Integer, State> group){
        String str = "";
        for(Integer k : group.keySet()){
            str += group.get(k).getId() + ":" + group.get(k).getType() + ",";
        }
        if(str.length()!=0) str = str.substring(0,str.length()-1);
        str = "{" + str + "}";
        return str;
    }
}
