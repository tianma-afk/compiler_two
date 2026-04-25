# NFA->DFA 与 DFA 最小化核心算法说明

本文档基于 src 下现有实现整理，覆盖两部分核心算法：

1. Subset 构造法（NFA -> DFA）
2. State-minimization（DFA 最小化）

## 1. Subset 构造法（NFA -> DFA）

### 1.1 对应实现位置

- `org.qogir.compiler.grammar.regularGrammar.SubsetConstruction`
  - `subSetConstruct(TNFA tnfa)`
  - `epsilonClosure(...)`
  - `move(...)`
  - `epsilonClosureWithMove(...)`

### 1.2 算法思想

将 NFA 的“状态集合”视为 DFA 的“单个状态”，使用 BFS 逐步扩展所有可达的状态集合：

- 初始 DFA 状态 = NFA 起始状态的 epsilon 闭包
- 对每个输入字符 c，计算 $\epsilon\text{-closure}(move(T,c))$
- 若得到新集合，则创建新 DFA 状态
- 若某集合包含 NFA 接受状态，则该 DFA 状态标记为接受态

### 1.3 伪代码

```text
Algorithm SubsetConstruct(tnfa):
    dfa <- new RDFA()
    nfaGraph <- tnfa.transitTable

    startSetMap <- epsilonClosures(tnfa.startState, nfaGraph)
    if startSetMap == null:
        startSetMap <- { tnfa.startState.id -> tnfa.startState }

    reset global State.STATE_ID to 0
    dfaStart <- new State(type = START)
    dfa.setStartState(dfaStart)
    dfa.transitTable.addVertex(dfaStart)
    dfa.setStateMappingBetweenDFAAndNFA(dfaStart, startSetMap)

    queue <- empty queue of Set<State>
    dfaStateMap <- empty map: Set<State> -> State
    startSet <- set(startSetMap.values)
    dfaStateMap[startSet] <- dfaStart
    queue.push(startSet)

    alphabet <- getAlphabet(nfaGraph)  // excludes epsilon

    while queue not empty:
        currentSet <- queue.pop()
        currentDfaState <- dfaStateMap[currentSet]

        for each ch in alphabet:
            inputMap <- map each state in currentSet by id
            nextSetMap <- epsilonClosureWithMove(inputMap, ch, nfaGraph)

            if nextSetMap is empty:
                continue

            nextSet <- set(nextSetMap.values)
            nextDfaState <- dfaStateMap[nextSet]

            if nextDfaState not exists:
                nextDfaState <- new State()
                dfa.transitTable.addVertex(nextDfaState)
                dfa.setStateMappingBetweenDFAAndNFA(nextDfaState, nextSetMap)
                dfaStateMap[nextSet] <- nextDfaState
                queue.push(nextSet)

            dfa.transitTable.addEdge(currentDfaState, nextDfaState, ch)

    nfaAccept <- tnfa.acceptingState
    for each (nfaSet, dfaState) in dfaStateMap:
        if nfaAccept in nfaSet:
            dfaState.type <- ACCEPT

    dfa.renumberSID()
    return dfa
```

### 1.4 算法分析

设：

- $N=|Q_N|$：NFA 状态数
- $E=|E_N|$：NFA 边数
- $\Sigma=|\text{alphabet}|$：输入字符种类数
- $D=|Q_D|$：构造出的 DFA 状态数（最坏可达 $2^N$）

分析：

1. 单次 `epsilonClosureWithMove`
- `move` 需遍历边集，代价约 $O(E)$（当前实现为扫全边）
- `epsilonClosure` 对集合中每个状态做 epsilon 扩展，最坏近似 $O(N \cdot E)$（每次扩展内部仍扫全边）

2. 主循环
- 每个 DFA 状态对每个字符都会尝试扩展一次，共约 $D\cdot\Sigma$ 次

综合时间复杂度（按当前实现结构）：

- 近似上界：$O(D\cdot\Sigma\cdot N\cdot E)$

空间复杂度：

- DFA 状态映射与队列存储：$O(D\cdot N)$（每个 DFA 状态对应一个 NFA 子集）
- 结果图存储边：约 $O(D\cdot\Sigma)$

最坏情况下 $D=2^N$，因此空间最坏可写为 $O(2^N\cdot N)$。

## 2. State-minimization（DFA 最小化）

### 2.1 对应实现位置

- `org.qogir.compiler.grammar.regularGrammar.StateMinimization`
  - `minimize(RDFA dfa)`
  - `getSuccessor(...)`
  - `findGroup(...)`

### 2.2 算法思想

采用“不断细分分组直到稳定”的划分细化思路：

1. 初始分组：按状态类型（0/1/2）分组
2. 对每个字符 c，检查同组内状态在 c 下是否跳到同一目标组
3. 若行为不同则拆分
4. 所有字符都无法继续拆分时停止
5. 每个最终分组收缩为一个新状态，重建最小化 DFA

### 2.3 伪代码

```text
Algorithm MinimizeDFA(dfa):
    partitions <- initial split by state.type in {0,1,2}
    alphabet <- getAlphabet(dfa.transitTable)

    changed <- true
    while changed:
        changed <- false

        for each ch in alphabet:
            newPartitions <- []

            for each group in partitions:
                if group.size <= 1:
                    append group to newPartitions
                    continue

                buckets <- empty map: targetGroupIndex -> Set<State>

                for each s in group:
                    next <- getSuccessor(dfa.graph, s, ch)
                    if next == null:
                        idx <- -1
                    else:
                        idx <- indexOf(findGroup(partitions, next))

                    buckets[idx].add(s)

                if bucket count > 1:
                    changed <- true
                    append all bucket sets to newPartitions
                else:
                    append group to newPartitions

            partitions <- newPartitions

    miniDFA <- new RDFA()
    reset global State.STATE_ID to 0

    groupToNewState <- empty map
    for each group in partitions:
        ns <- new State()
        ns.type <- group type priority:
                   ACCEPT > MIDDLE > START
        miniDFA.addVertex(ns)
        miniDFA.setStateMappingBetweenDFAAndNFA(ns, group as id->state map)
        groupToNewState[group] <- ns

    // set start state
    for each group in partitions:
        if dfa.startState in group:
            miniDFA.startState <- groupToNewState[group]

    // rebuild transitions by representative
    for each group in partitions:
        rep <- any state in group
        from <- groupToNewState[group]
        for each ch in alphabet:
            toOld <- getSuccessor(dfa.graph, rep, ch)
            if toOld exists:
                toGroup <- findGroup(partitions, toOld)
                toNew <- groupToNewState[toGroup]
                miniDFA.addEdge(from, toNew, ch)

    return miniDFA
```

### 2.4 算法分析

设：

- $V=|Q_D|$：原 DFA 状态数
- $E=|E_D|$：原 DFA 边数
- $\Sigma$：字符集大小

当前实现中，`getSuccessor` 每次查询都线性扫描全边集，代价约 $O(E)$。

划分细化阶段：

- 外层需要多轮迭代直到稳定，最坏可近似 $O(V)$ 轮
- 每轮对每个字符处理分组中的状态，总状态访问量约 $O(V)$
- 每次状态处理包含一次 `getSuccessor`（$O(E)$）以及一次 `findGroup`（最坏 $O(V)$）

因此细化阶段可近似上界为：

- $O(V\cdot\Sigma\cdot V\cdot(E+V))=O(\Sigma\cdot V^2\cdot(E+V))$

在稠密图 $E=O(V^2)$ 时，可写为 $O(\Sigma\cdot V^4)$ 量级上界。

重建最小 DFA 阶段：

- 建状态约 $O(V)$
- 建边过程约 $O(V\cdot\Sigma\cdot(E+V))$

空间复杂度：

- 分组与映射结构约 $O(V)$
- 新图边集最坏约 $O(V\cdot\Sigma)$
- 总体约 $O(V+V\cdot\Sigma)$

## 3. 实现特点与说明

1. Subset 构造法直接使用 `RDFA` 中“DFA 状态 -> NFA 状态集”的映射，便于教学展示与调试。
2. 最小化算法采用“按字符逐轮细化分组”的可读实现，逻辑直观。
3. 两个算法当前都偏向“可理解性优先”：大量使用边集遍历，便于验证但在大图上性能较保守。
4. `distinguishEquivalentState(RDFA)` 目前为预留方法，实际最小化流程由 `minimize(RDFA)` 完成。
