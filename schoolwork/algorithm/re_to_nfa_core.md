# RE 转换为 NFA 核心算法说明

## 1. 项目中 RE -> NFA 的核心算法

结合 src 下实现，RE 到 NFA 的核心流程由以下算法组成：

1. 正则表达式语法树构造（栈驱动解析）
   - 对应实现：ParseRegex.parse、ParseRegex.mergeStackAsOneChild
   - 作用：将正则表达式字符序列解析为 RegexTree，为后续自动机构造提供结构化输入。

2. Thompson 构造法（语法树递归翻译）
   - 对应实现：ThompsonConstruction.translate
   - 子过程：buildConcatNFA、buildUnionNFA、buildStarNFA
   - 作用：将语法树中的基础字符、连接、并集、闭包节点分别翻译为 TNFA，并组合成完整 NFA。

3. 多正则 NFA 合并（总入口 NFA 组装）
   - 对应实现：Scanner.constructNFA（多模式分支）
   - 作用：当文法包含多个正则时，为总 NFA 新建全局开始状态，并通过 epsilon 边连接到各子 NFA 开始状态。

说明：SubsetConstruction（NFA -> DFA）和 StateMinimization（DFA 最小化）属于后续阶段，不属于 RE -> NFA 核心步骤。

## 2. 核心算法伪代码

### 2.1 正则表达式解析为语法树（ParseRegex）

```text
Algorithm ParseRegexToTree(regex):
    queue <- characters(regex) + ['%']
    if queue empty: return null

    stack <- empty stack
    look <- pop(queue)
    if look is illegal start symbol: return null

    push basic-node(look) into stack

    while look != '%':
        look <- pop(queue)

        if look == '*':
            require previous token is basic or ')' structure
            pop previous expression as child
            push star-node(child)

        else if look == '(':
            push left-parenthesis marker

        else if look == ')':
            reduce expression until matching '('
            merge local nodes as concat/union subtree
            push reduced subtree back

        else if look == '|':
            reduce left part in current scope
            build/extend union-node
            push union-node

        else if look is literal or epsilon:
            push basic-node(look)

        else:
            return null

    do final reduction on stack
    remove parenthesis markers
    if more than one top-level node: merge as concat-node
    return RegexTree(root)
```

### 2.2 Thompson 构造法（ThompsonConstruction.translate）

```text
Algorithm Translate(node, root):
    if node is null: return null
    if node == root: reset global state id counter

    switch node.type:
        case BASIC(0):
            create TNFA(start, accept)
            add transition start -node.value-> accept
            return TNFA

        case CONCAT(1):
            childNFAList <- [Translate(child_i, root)]
            return BuildConcatNFA(childNFAList)

        case UNION(2):
            leftNFA <- Translate(leftChild, root)
            rightNFA <- Translate(rightChild, root)
            return BuildUnionNFA(leftNFA, rightNFA)

        case STAR(3):
            innerNFA <- Translate(firstChild, root)
            return BuildStarNFA(innerNFA)

        default:
            return null
```

### 2.3 Thompson 子过程

```text
Algorithm BuildConcatNFA(nfas):
    if nfas empty: return null
    result <- nfas[0]
    for each nfa in nfas[1...]:
        merge transitions(result, nfa)
        add epsilon edge result.accept -> nfa.start
        mark result.accept and nfa.start as middle states
        result.accept <- nfa.accept
    return result
```

```text
Algorithm BuildUnionNFA(nfa1, nfa2):
    result <- new TNFA(newStart, newAccept)
    merge transitions(result, nfa1)
    merge transitions(result, nfa2)
    add epsilon edges:
        newStart -> nfa1.start
        newStart -> nfa2.start
        nfa1.accept -> newAccept
        nfa2.accept -> newAccept
    mark old boundary states as middle
    return result
```

```text
Algorithm BuildStarNFA(nfa):
    result <- new TNFA(newStart, newAccept)
    merge transitions(result, nfa)
    add epsilon edges:
        newStart -> nfa.start
        newStart -> newAccept
        nfa.accept -> nfa.start
        nfa.accept -> newAccept
    mark old boundary states as middle
    return result
```

### 2.4 多正则 NFA 合并（Scanner.constructNFA）

```text
Algorithm ConstructNFAForGrammar(regexList):
    if regexList size == 1:
        return ThompsonNFA(regexList[0])

    subNFAs <- [ThompsonNFA(regex_i)]
    result <- new TNFA(globalStart, globalAccept)

    for each sub in subNFAs:
        adjust sub.start state type
        merge transitions(result, sub)
        add epsilon edge globalStart -> sub.start

    set alphabet for result
    return result
```

## 3. 算法分析

设：
- n 为正则表达式长度（字符数）
- m 为语法树节点数（通常 m = O(n)）

### 3.1 语法树构造（ParseRegex）

- 时间复杂度：
  - 平均情况下约为 O(n)
  - 代码中对左括号匹配存在一次队列遍历检查，在最坏嵌套场景可达 O(n^2)
- 空间复杂度：O(n)
  - 主要来自字符队列、解析栈与中间子栈。

### 3.2 Thompson 构造（Translate + 三个子过程）

- 时间复杂度：O(m)
  - 每个语法树节点只会被递归处理一次。
  - 每次组合操作（连接/并集/闭包）仅增加常数条 epsilon 边或做线性合并。
- 空间复杂度：O(m)
  - 自动机状态数、边数与递归栈规模均与节点数线性相关。

### 3.3 多正则合并（constructNFA 多模式）

设有 k 个正则，第 i 个正则对应子 NFA 规模为 m_i。

- 时间复杂度：O(sum(m_i))
  - 需要遍历并合并所有子 NFA 的迁移边。
- 空间复杂度：O(sum(m_i))
  - 总 NFA 存储全部子 NFA 状态与边，并增加 k 条从全局开始状态出发的 epsilon 边。

## 4. 实现特点小结

1. 该项目采用“先建语法树，再递归 Thompson 翻译”的两阶段设计，结构清晰。
2. 连接、并集、闭包均显式封装为独立子过程，便于教学演示与日志记录。
3. 多正则场景通过“全局开始状态 + epsilon 分发”统一到一个 NFA，便于后续统一子集构造。