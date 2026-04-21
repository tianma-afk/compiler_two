package org.qogir.compiler.grammar.regularGrammar;

import org.qogir.compiler.FA.FiniteAutomaton;
import org.qogir.compiler.FA.State;
import org.qogir.compiler.util.graph.LabeledDirectedGraph;

import java.util.HashMap;

public class RDFA extends FiniteAutomaton {

    /**
     * holds the maps between DFA states and NFA state sets
     */
    private HashMap<State, HashMap<Integer,State>> StateMappingBetweenDFAAndNFA = new HashMap<>();
    public RDFA(){
        super();
        this.StateMappingBetweenDFAAndNFA = new HashMap<>();
        this.transitTable = new LabeledDirectedGraph<>();
    }

    public RDFA(State startState){
        this.startState = startState;
        this.StateMappingBetweenDFAAndNFA = new HashMap<>();
        this.transitTable = new LabeledDirectedGraph<>();
        this.getTransitTable().addVertex(this.startState);
    }

    public void setStateMappingBetweenDFAAndNFA(State s, HashMap<Integer,State> nfaStates){
        this.StateMappingBetweenDFAAndNFA.put(s,nfaStates);
    }

    public HashMap<State, HashMap<Integer, State>> getStateMappingBetweenDFAAndNFA() {
        return StateMappingBetweenDFAAndNFA;
    }

    public String StateMappingBetweenDFAAndNFAToString() {
        StringBuilder str = new StringBuilder();
        for(State s : this.getStateMappingBetweenDFAAndNFA().keySet()){
            StringBuilder mapping = new StringBuilder();
            for(State ns : this.getStateMappingBetweenDFAAndNFA().get(s).values()){
                mapping.append(ns.toString()).append(",");
            }
            if (!mapping.isEmpty()) {
                mapping.setLength(mapping.length() - 1);
            }

            // 拼接行
            str.append("DFA State:").append(s.toString())
                    .append("\tNFA State set:\t{").append(mapping)
                    .append("}").append("\r\n");
        }
        return str.toString();
    }
}
