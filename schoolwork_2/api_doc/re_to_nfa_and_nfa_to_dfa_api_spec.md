# REtoNFA 与 NFAtoDFA 接口规范（基于现有代码）

本文档依据 src 下现有实现整理，目标是统一说明两段转换链路的接口设计：

- REtoNFA：正则表达式到 Thompson NFA
- NFAtoDFA：Thompson NFA 到 DFA（子集构造）

## 1. 模块边界与主调用链

### 1.1 模块边界

- 输入侧：RegularGrammar / Regex
- 中间表示：RegexTree、TNFA
- 输出侧：RDFA

### 1.2 主调用链（推荐入口）

由 Scanner 提供统一编排入口：

1. constructRegexTree(Regex)
2. constructRegexNFA(Regex)
3. constructDFA(TNFA) 或 constructRegexDFA(Regex)

文法级入口：

- constructNFA()
- constructDFA()
- constructAllDFA()

## 2. REtoNFA 接口规范

### 2.1 文法与正则定义接口

#### 2.1.1 RegularGrammar(String[] regexes)

- 作用：解析规则数组并构造文法。
- 输入约定：每条规则建议使用 name := regex。
- 输出：
  - patterns: ArrayList<Regex>
  - symbols: ArrayList<Character>（仅字母和数字，排除 ε）

注意：
- 现有实现对规则名截取依赖 := 前空格，建议严格使用 name := regex 形式。

#### 2.1.2 getPatterns() / getSymbols()

- getPatterns：返回文法中的 Regex 列表。
- getSymbols：返回文法字符表（不含 ε）。

### 2.2 语法树构造接口

#### 2.2.1 RegexTree constructRegexTree(Regex r)

- 所在类：Scanner
- 内部实现：ParseRegex.parse()
- 返回：RegexTree

前置条件：
- r 非空
- r.getRegex() 内容符合当前语法支持范围

失败语义：
- ParseRegex.parse() 失败时返回 null。

#### 2.2.2 HashMap<Regex, RegexTree> constructRegexTrees()

- 所在类：Scanner
- 作用：为文法中的所有 Regex 批量构建语法树。
- 返回：Regex 到 RegexTree 映射。

### 2.3 NFA 构造接口

#### 2.3.1 TNFA constructRegexNFA(Regex r)

- 所在类：Scanner
- 内部流程：
  1. constructRegexTree(r)
  2. ThompsonConstruction.translate(tree.getRoot(), tree.getRoot())
  3. setAlphabetForNfa(r, nfa)
- 返回：单正则对应 TNFA

失败语义：
- 若 tree 为 null，当前实现缺少空值保护，可能触发空指针异常。

#### 2.3.2 void setAlphabetForNfa(Regex r, TNFA nfa)

- 所在类：Scanner
- 作用：根据正则文本提取 NFA 字母表。
- 规则：仅字母和数字，排除 ε。

#### 2.3.3 HashMap<Regex, TNFA> constructAllNFA()

- 所在类：Scanner
- 作用：批量构建所有正则的 TNFA。
- 返回：Regex 到 TNFA 的映射。

#### 2.3.4 TNFA constructNFA()

- 所在类：Scanner
- 作用：构建文法级 NFA。

规则：
- 单正则：返回该正则 TNFA。
- 多正则：
  - 逐个构建子 TNFA。
  - 创建总起始状态。
  - 以 ε 边连接总起始状态到各子 TNFA 起始状态。
  - 合并各子迁移图。

输出约束：
- 返回 TNFA，alphabet 最终设置为 rg.getSymbols()。

## 3. NFAtoDFA 接口规范

### 3.1 转换入口接口

#### 3.1.1 RDFA constructDFA(TNFA nfa)

- 所在类：Scanner
- 内部调用：SubsetConstruction.subSetConstruct(nfa)
- 返回：RDFA

后置处理：
- dfa.setAlphabet(nfa.getAlphabet())

#### 3.1.2 RDFA constructRegexDFA(Regex r)

- 所在类：Scanner
- 内部流程：
  1. constructRegexNFA(r)
  2. subSetConstruct(nfa)
  3. setAlphabet
- 返回：单正则 DFA

#### 3.1.3 RDFA constructDFA()

- 所在类：Scanner
- 内部流程：
  1. constructNFA()
  2. subSetConstruct(nfa)
  3. dfa.setAlphabet(rg.symbols)
- 返回：文法级 DFA

#### 3.1.4 HashMap<Regex, RDFA> constructAllDFA()

- 所在类：Scanner
- 作用：对每个正则构建 DFA，并执行 minimizeDFA。
- 返回：Regex 到最小化 DFA 的映射。

### 3.2 子集构造核心接口

所在类：SubsetConstruction

#### 3.2.1 RDFA subSetConstruct(TNFA tnfa)

- 作用：执行 NFA 到 DFA 的子集构造。
- 核心逻辑：
  - 起始集合：epsilonClosures(nfaStart)
  - 迁移公式：T' = epsilonClosure(move(T, ch))
  - 新集合创建新 DFA 状态并建边
  - 若集合包含 NFA 接受状态，则对应 DFA 状态标记为 ACCEPT
- 返回：RDFA

内部状态约定：
- 构造开始时重置 State.STATE_ID = 0
- 结束前调用 dfa.renumberSID() 以整理展示 sid

#### 3.2.2 HashMap<Integer, State> epsilonClosure(...)

- public 方法签名：epsilonClosure(HashMap<Integer, State> ss, LabeledDirectedGraph<State> tb)
- 作用：对状态集合求 ε 闭包。

#### 3.2.3 HashMap<Integer, State> move(...)

- public 方法签名：move(HashMap<Integer, State> ss, Character ch, LabeledDirectedGraph<State> tb)
- 作用：对状态集合执行字符迁移。

#### 3.2.4 HashMap<Integer, State> epsilonClosureWithMove(...)

- public 方法签名：epsilonClosureWithMove(HashMap<Integer, State> sSet, Character ch, LabeledDirectedGraph<State> tb)
- 作用：先 move，再 ε 闭包。

## 4. 数据模型接口规范

### 4.1 TNFA

关键接口：

- getStartState()
- getAcceptingState()
- setAcceptingState(State)
- getTransitTable()
- getAlphabet() / setAlphabet(...)

结构约束：

- 默认包含一个接受状态，类型为 ACCEPT。
- 空迁移统一使用字符 ε。

### 4.2 RDFA

关键接口：

- getStartState()
- getTransitTable()
- getAlphabet() / setAlphabet(...)
- setStateMappingBetweenDFAAndNFA(State, HashMap<Integer, State>)
- getStateMappingBetweenDFAAndNFA()

补充输出接口：

- StateMappingBetweenDFAAndNFAToString()
  - 用于查看 DFA 状态与 NFA 状态集合映射。

### 4.3 迁移表

统一类型：LabeledDirectedGraph<State>

- 顶点集合：vertexSet()
- 边集合：edgeSet()
- 建边：addEdge(source, target, label)
- 图合并：merge(...)

## 5. 失败语义与调用约束

### 5.1 REtoNFA 阶段

- ParseRegex.parse() 解析失败时返回 null，并打印错误信息。
- Scanner.constructRegexNFA() 对 null tree 未做保护，调用方需保证输入合法。

### 5.2 NFAtoDFA 阶段

- subSetConstruct 假设 tnfa 非空且迁移表可遍历。
- 若输入 NFA 异常（如空引用），上层调用可能产生运行时异常。

### 5.3 调用建议

1. 在进入 constructRegexNFA 前先做正则合法性校验。
2. 在批量转换时对单条失败做隔离处理，避免影响整体流程。
3. 在输出给展示层前统一执行 sid 重编号（当前子集构造已执行）。

## 6. 端到端示例

```java
String[] regexes = new String[]{"id := c(a|b)*"};
RegularGrammar rg = new RegularGrammar(regexes);
Scanner scanner = new Scanner(rg);

// RE -> NFA
Regex r = rg.getPatterns().get(0);
TNFA nfa = scanner.constructRegexNFA(r);

// NFA -> DFA
RDFA dfa = scanner.constructDFA(nfa);

// 文法级一次完成
RDFA grammarDfa = scanner.constructDFA();
```

## 7. 文档适用范围

- 代码版本：当前仓库 src 实现
- 主要包路径：
  - org.qogir.compiler.grammar.regularGrammar
  - org.qogir.simulation.scanner
