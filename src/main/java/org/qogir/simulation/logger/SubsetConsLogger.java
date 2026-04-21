package org.qogir.simulation.logger;

import org.qogir.compiler.FA.State;
import org.qogir.compiler.grammar.regularGrammar.RDFA;
import org.qogir.simulation.logger.dto.SubsetConsDTO;
import org.qogir.simulation.logger.dto.SubsetConsTableColsInfo;
import org.qogir.simulation.util.StateDiagram;

import java.util.*;

public class SubsetConsLogger {
    public static ConstructionLogger<SubsetConsDTO> constructionLogger = new ConstructionLogger<>();


    /**
     * Record the conversion process from nfa to dfa:subSetConstruct
     *
     * @param dfa
     */
    public static void getSubsetConsLogger(RDFA dfa, Character ch, State SourceDfaState, HashMap<Integer, State> TargetNfaStateSet) {
        addStep(dfa, ch, SourceDfaState, TargetNfaStateSet);
    }


    public static void addStep(RDFA dfa, Character ch, State SourceDfaState, HashMap<Integer, State> TargetNfaStateSet) {
        SubsetConsDTO subsetConsDTO = new SubsetConsDTO();
        //构造表表头
        subsetConsDTO.setTableColsInfos(setTableColsInfos(dfa.getAlphabet()));

        //当前步骤的状态图
        subsetConsDTO.setStateInfos(StateDiagram.getStateDiagramFromDFA(dfa));

        //构造表数据
        subsetConsDTO.setSubsetConsTableDTOS(createTableData(dfa, ch, SourceDfaState, TargetNfaStateSet));


        constructionLogger.addStep(subsetConsDTO);
    }

    public static String createStateSet(HashMap<Integer, State> hashMap) {
        if (hashMap.isEmpty()) return "-";
        ArrayList<String> stateSets = new ArrayList<>();
        for (State s : hashMap.values()) {
            stateSets.add(s.getSid());
        }
        Collections.sort(stateSets);
        String states = stateSets.toString();
        states = states.replaceAll("\\[", "{").replaceAll("]", "}");
        return states;
    }

    public static ArrayList<Map<String, String>> createTableData(RDFA dfa, Character ch, State SourceDfaState, HashMap<Integer, State> TargetNfaStateSet) {
        //整张表数据（多行）
        ArrayList<Map<String, String>> subsetConsTableDTOS;

        if (constructionLogger.getStepQueue().size() == 0) {
            //第一步
            subsetConsTableDTOS = new ArrayList<>();
            Map<String, String> rowData = new HashMap<>();
            rowData.put("I", createStateSet(dfa.getStateMappingBetweenDFAAndNFA().get(SourceDfaState)));

            //将新行加入表中
            subsetConsTableDTOS.add(rowData);

        } else {
            //复制上一步的表数据
            subsetConsTableDTOS = new ArrayList<>();
            for (Map<String, String> s : constructionLogger.getStepQueue().getLast().getSubsetConsTableDTOS()) {
                subsetConsTableDTOS.add(new HashMap<>(s));
            }

            //一行表数据结束，换一行
            if (subsetConsTableDTOS.get(subsetConsTableDTOS.size() - 1).size() == dfa.getAlphabet().size() + 1) {
                //I字段补上
                Map<String, String> rowData = new HashMap<>();
                rowData.put("I", createStateSet(dfa.getStateMappingBetweenDFAAndNFA().get(SourceDfaState)));
                //将新行加入表中
                subsetConsTableDTOS.add(rowData);

            } else {
                //取出表中未完成的行继续添加数据(将原来那行删掉)
                Map<String, String> rowData = new HashMap<>(subsetConsTableDTOS.get(subsetConsTableDTOS.size() - 1));
                rowData.put("I" + ch, createStateSet(TargetNfaStateSet));
                subsetConsTableDTOS.remove(subsetConsTableDTOS.size() - 1);
                //将新行加入表中
                subsetConsTableDTOS.add(rowData);
            }


        }
        return subsetConsTableDTOS;
    }

    public static ArrayList<SubsetConsTableColsInfo> setTableColsInfos(ArrayList<Character> symbols) {
        ArrayList<SubsetConsTableColsInfo> subsetConsTableColsInfos = new ArrayList<>();
        int count = 1;
        for (Character ch : symbols) {
            subsetConsTableColsInfos.add(new SubsetConsTableColsInfo(count++, "I" + ch, "I" + ch));
        }
        return subsetConsTableColsInfos;
    }

    public static void resetParam() {
        if (!constructionLogger.getStepQueue().isEmpty()) {
            constructionLogger.getStepQueues().add(constructionLogger.getStepQueue());
        }
        constructionLogger.setStepQueue(new ArrayDeque<>());

    }


    public static void reset() {
        constructionLogger = new ConstructionLogger<>();
    }
}
