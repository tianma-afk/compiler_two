package org.qogir.compiler.grammar.regularGrammar;

import org.qogir.compiler.FA.FiniteAutomaton;
import org.qogir.compiler.FA.State;

/**
 * Thompson NFA（TNFA）模型。
 * <p>
 * 继承自 {@link FiniteAutomaton}，并遵循 McNaughton-Yamada-Thompson 构造约束：
 * 每个 TNFA 仅有一个接受状态，且该接受状态无出边。
 */

public class TNFA extends FiniteAutomaton {

    private State acceptingState = new State();

    /**
     * 创建一个默认 TNFA。
     * <p>
     * 默认接受状态会被标记为 {@link State#ACCEPT} 并加入迁移图。
     */
    public TNFA(){
        super();
        acceptingState.setType(State.ACCEPT);
        this.transitTable.addVertex(acceptingState);
    }

    /**
     * 使用指定接受状态创建 TNFA。
     *
     * @param acceptingState 接受状态
     */
    public TNFA(State acceptingState){
        super();
        this.acceptingState = acceptingState;
        this.acceptingState.setType(State.ACCEPT);
        this.transitTable.addVertex(this.acceptingState);
    }

    /**
     * 获取 TNFA 的接受状态。
     *
     * @return 接受状态
     */
    public State getAcceptingState() {
        return acceptingState;
    }

    /**
     * 设置 TNFA 的接受状态。
     *
     * @param acceptingState 新的接受状态
     */
    public void setAcceptingState(State acceptingState) {
        this.acceptingState = acceptingState;
    }
}
