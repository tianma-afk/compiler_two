package org.qogir.compiler.grammar.regularGrammar.scanner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.junit.Test;
import org.qogir.compiler.grammar.regularGrammar.RDFA;
import org.qogir.compiler.grammar.regularGrammar.Regex;
import org.qogir.compiler.grammar.regularGrammar.RegularGrammar;
import org.qogir.compiler.grammar.regularGrammar.TNFA;
import org.qogir.simulation.logger.ScanWithDFALogger;
import org.qogir.simulation.logger.StateMinLogger;
import org.qogir.simulation.logger.SubsetConsLogger;
import org.qogir.simulation.logger.ThompsonLogger;
import org.qogir.simulation.scanner.Scanner;
import org.qogir.simulation.util.StateMini;

import java.util.HashMap;

public class ScannerTest {
    @Test
    public void testConstructDFA() {
        String[] regexes = new String[]{"regex1 := c(a|b)*"};
        //test defining a regular grammar
        RegularGrammar rg = new RegularGrammar(regexes);
        System.out.println(rg);

        //test building a grammar for the grammar
        Scanner scanner = new Scanner(rg);
        TNFA tnfa = scanner.constructNFA();
        System.out.println(tnfa);
        System.out.println("Show the DFA:");
        //test constructing the DFA
        RDFA dfa = scanner.constructDFA(tnfa);
        System.out.println(dfa.StateMappingBetweenDFAAndNFAToString());
        System.out.println(dfa.toString());

    }

    public static void main(String[] args) {


        String[] regexes = new String[]{"regex0 := c(a|b*"};//"regex1 := c(a|b)*","regex2 := d(f|ea*(g|h))b","c(a|b)*","a|b", "ab*", "d(f|e)","d(f|ea*(g|h))b","c(a|b)*"

        //test defining a regular grammar
        RegularGrammar rg = new RegularGrammar(regexes);

        System.out.println(rg);
        //test building a grammar for the grammar
        Scanner scanner = new Scanner(rg);

        //test constructing the regex tree
        System.out.println(scanner.constructRegexTrees().toString());

        //System.out.println("Show the NFA:");
        //test constructing the NFA
        System.out.println(scanner.constructNFA().toString());

        System.out.println("Show the DFA:");
        //test constructing the DFA

        System.out.println(scanner.constructDFA(scanner.constructNFA()).toString());
        //System.out.println("Show the miniDFA:");
        //test minimizing the DFA
        //State.STATE_ID = 0;
        System.out.println(scanner.minimizeDFA(scanner.constructDFA(scanner.constructNFA())).toString());

    }

    @Test
    public void testConstructMinDFA() {
        String[] regexes = new String[]{"regex1 := (a|b)c*"};
        //test defining a regular grammar
        RegularGrammar rg = new RegularGrammar(regexes);
        System.out.println(rg);

        //test building a grammar for the grammar
        Scanner scanner = new Scanner(rg);
        TNFA tnfa = scanner.constructNFA();
        System.out.println("Show the minimizeDFA:");
        System.out.println(tnfa);

        System.out.println("Show the minimizeDFA:");
        //test constructing the minimizeDFA
        System.out.println(scanner.minimizeDFA(scanner.constructDFA(tnfa)).toString());
    }

    @Test
    public void testConstructMultipleDFA() {
        ThompsonLogger.reset();
        StateMinLogger.reset();
        ScanWithDFALogger.reset();
        SubsetConsLogger.reset();

        String[] regexes = new String[]{"regex0 := a|ε", "regex1 := c(a|b)*", "regex2 := (c(a|b)*)*(d|ε)f*c"};
        //String[] regexes = new String[]{"regex0 := a|ε", "regex1 := c(a|b)*","regex2 := (c(a|b)*)*(d|ε)f*c"};
        //test defining a regular grammar
        RegularGrammar rg = new RegularGrammar(regexes);
        //System.out.println(rg);

        //test building a grammar for the grammar
        Scanner scanner = new Scanner(rg);
        HashMap<Regex, RDFA> regexRDFAHashMap = scanner.constructAllDFA();
/*        System.out.println("Show each DFA:");
        for (RDFA rdfa : regexRDFAHashMap.values()) {
            System.out.println(rdfa.toString());
        }*/
        System.out.println("#############ThompsonLogger#############");
        System.out.println(ThompsonLogger.constructionLogger.returnStepQueues());
        System.out.println();

        System.out.println("#############StateMinLogger#############");
        System.out.println(JSON.toJSONString(StateMini.turnToStateMiniDTO(StateMinLogger.constructionLogger, regexRDFAHashMap, rg), SerializerFeature.DisableCircularReferenceDetect));
        System.out.println();

        System.out.println("#############SubsetConsLogger#############");
        System.out.println(SubsetConsLogger.constructionLogger.returnStepQueues());
        System.out.println();
    }
}