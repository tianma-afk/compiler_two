package org.qogir.simulation.logger;

import org.qogir.compiler.FA.State;
import org.qogir.simulation.logger.dto.StateMiniTableRowDTO;
import org.qogir.simulation.logger.dto.Type;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class StateMinLogger {
    //constructionLogger will be clear when used:StateMinimization.minimize()
    public static ConstructionLogger<StateMiniTableRowDTO> constructionLogger = new ConstructionLogger<>();

    /**
     * Record the conversion process from DFA to minDFA
     *
        * @param groupSet 状态分组
        * @param ch 当前划分字符
     */
    public void getStateMinLogger(HashMap<Integer, HashMap<Integer, State>> groupSet, Character ch) {
        addStep(groupSet, ch + "");
    }

    public static void resetParam() {
        StateMiniTableRowDTO.count = 0;
        if (!constructionLogger.getStepQueue().isEmpty()) {
            constructionLogger.getStepQueues().add(constructionLogger.getStepQueue());
        }
        constructionLogger.setStepQueue(new ArrayDeque<>());

    }


    /**
     * Record the Initial step from DFA to minDFA
     *
        * @param groupSet 状态分组
     */
    public void setInitialStep(HashMap<Integer, HashMap<Integer, State>> groupSet) {
        //Initial
        HashMap<Integer, HashMap<Integer, State>> newGroupSet = new HashMap<>();
        HashMap<Integer, State> temp = new HashMap<>();
        for (Integer i : groupSet.keySet()) {
            temp.putAll(groupSet.get(i));
        }
        newGroupSet.put(0, temp);
        addStep(newGroupSet, "Initial");
    }

    /**
     * Record the AcceptOrNot step from DFA to minDFA
     *
        * @param groupSet 状态分组
     */
    public void setAcceptOrNotStep(HashMap<Integer, HashMap<Integer, State>> groupSet) {
        addStep(groupSet, "AcceptOrNot");
    }

    public static void reset() {
        constructionLogger = new ConstructionLogger<>();
        StateMiniTableRowDTO.count = 0;
    }

    public Integer checkType(HashMap<Integer, State> stateHashMap) {
        for (State s : stateHashMap.values()) {
            if (s.getType() == 2 || s.getType() == 20) {
                return Type.AcceptOrInitial.ordinal();
            }
        }
        return Type.NoAccept.ordinal();
    }

    public void setFirstTwoSteps(HashMap<Integer, HashMap<Integer, State>> groupSet) {
        setInitialStep(groupSet);
        setAcceptOrNotStep(groupSet);
    }

    public void addStep(HashMap<Integer, HashMap<Integer, State>> groupSet, String split) {
        StateMiniTableRowDTO stateMiniTableRowDTO = new StateMiniTableRowDTO();
        for (Integer g : groupSet.keySet()) {
            Set<Integer> sids = new HashSet<>();
            for (State s : groupSet.get(g).values()) {
                sids.add(Integer.parseInt(s.getSid()));
            }
            stateMiniTableRowDTO.addStateSet(sids, checkType(groupSet.get(g)));
        }
        stateMiniTableRowDTO.setSplitBy(split);
        constructionLogger.addStep(stateMiniTableRowDTO);
    }

}
