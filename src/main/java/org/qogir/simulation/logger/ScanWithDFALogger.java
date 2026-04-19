package org.qogir.simulation.logger;

import org.qogir.compiler.FA.State;
import org.qogir.compiler.grammar.regularGrammar.RDFA;
import org.qogir.compiler.grammar.regularGrammar.Regex;
import org.qogir.simulation.logger.dto.diagramsInfos.ScanDTO;
import org.qogir.simulation.scanner.Token;

import java.util.ArrayDeque;
import java.util.HashMap;

public class ScanWithDFALogger {
    public static ConstructionLogger<ScanDTO> constructionLogger = new ConstructionLogger<>();


    public static void reset() {
        constructionLogger = new ConstructionLogger<>();
        ScanDTO.count = -1;
    }

    /**
     * Record the step when running DFAs
     *
        * @param nextState 当前 DFA 到状态的映射
        * @param RDFARegexHashMap DFA 与正则表达式的映射
        * @param pos 输入位置
     */
    public void getScanStepLogger(HashMap<RDFA, State> nextState, HashMap<RDFA, Regex> RDFARegexHashMap, int pos) {
        if (nextState.isEmpty()) return;
        ScanDTO scanDTO = new ScanDTO();
        for (RDFA rdfa : nextState.keySet()) {
            if (nextState.get(rdfa) != null) {
                scanDTO.addCurStates(RDFARegexHashMap.get(rdfa).getName().substring(5), nextState.get(rdfa).getId());
            } else {
                scanDTO.addCurStates(RDFARegexHashMap.get(rdfa).getName().substring(5), -1);
            }

            scanDTO.setInputPos(pos);

        }

        constructionLogger.addStep(scanDTO);

        //set curElement's Tokens As preElement's Tokens
        setCurTokenAsPre();

    }

    /**
     * Record the start step when begin running DFAs
     *
        * @param currentState 当前 DFA 到状态的映射
        * @param nextState 下一步 DFA 到状态的映射
        * @param RDFARegexHashMap DFA 与正则表达式的映射
     */
    public void getScanStartStepLogger(HashMap<RDFA, State> currentState, HashMap<RDFA, State> nextState, HashMap<RDFA, Regex> RDFARegexHashMap) {
        if (!nextState.isEmpty()) return;
        ScanDTO scanDTO = new ScanDTO();
        for (RDFA rdfa : currentState.keySet()) {
            if (nextState.get(rdfa) != null) {
                scanDTO.addCurStates(RDFARegexHashMap.get(rdfa).getName().substring(5), nextState.get(rdfa).getId());
            } else {
                scanDTO.addCurStates(RDFARegexHashMap.get(rdfa).getName().substring(5), rdfa.getStartState().getId());
            }

            scanDTO.setInputPos(-1);//start state

        }

        constructionLogger.addStep(scanDTO);

        //set curElement's Tokens As preElement's Tokens
        setCurTokenAsPre();

    }

    public void setCurTokenAsPre() {
        ArrayDeque<ScanDTO> stepQueue = constructionLogger.getStepQueue();
        if (stepQueue.size() > 1) {
            ScanDTO cur = constructionLogger.getStepQueue().pollLast();
            ScanDTO pre = constructionLogger.getStepQueue().pollLast();
            ArrayDeque<Token> newTokenQueue = new ArrayDeque<>();
            beanCopy(pre.getTokens(), newTokenQueue);
            cur.setTokens(newTokenQueue);
            constructionLogger.addStep(pre);
            constructionLogger.addStep(cur);
        }
    }

    public void addTokens(ArrayDeque<Token> tokenQueue) {
        if (tokenQueue.isEmpty()) return;
        ScanDTO scanDTO = constructionLogger.getStepQueue().pollLast();
        ArrayDeque<Token> newTokenQueue = new ArrayDeque<>();
        beanCopy(tokenQueue, newTokenQueue);
        scanDTO.setTokens(newTokenQueue);
        constructionLogger.addStep(scanDTO);
    }

    public void addStepForFail() {
        ScanDTO scanDTO = new ScanDTO();
        int inputPos = constructionLogger.getStepQueue().getLast().getInputPos() + 1;
        if (inputPos == 0) {
            ScanDTO temp = constructionLogger.getStepQueue().pollLast();
            inputPos = constructionLogger.getStepQueue().getLast().getInputPos() + 1;
            assert temp != null;
            constructionLogger.getStepQueue().addLast(temp);
        }
        scanDTO.setInputPos(inputPos);
        scanDTO.setTokens(constructionLogger.getStepQueue().getLast().getTokens());
        constructionLogger.addStep(scanDTO);
    }


    public void beanCopy(ArrayDeque<Token> tokenQueue, ArrayDeque<Token> newTokenQueue) {
        for (Token token : tokenQueue) {
            newTokenQueue.add(new Token(token.getTag(), token.getLexeme()));
        }
    }
}
