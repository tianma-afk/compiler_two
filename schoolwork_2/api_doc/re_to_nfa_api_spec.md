# REtoNFA 接口设计规范（基于现有代码）

本文档根据 `src/main/java` 下当前实现整理，目标是描述 RE（Regular Expression）到 NFA（Thompson NFA）的接口规范，并给出与 NFAtoDFA 阶段的对接约定。

## 1. 模块范围与职责

### 1.1 REtoNFA 模块边界

- 输入：正则文法定义（`String[]` 或 `RegularGrammar`）
- 中间表示：`RegexTree`（语法树）
- 输出：`TNFA`

### 1.2 主要类（按职责）

- 文法与模式定义
  - `org.qogir.compiler.grammar.regularGrammar.RegularGrammar`
  - `org.qogir.compiler.grammar.regularGrammar.Regex`
- 正则解析（RE -> 语法树）
  - `org.qogir.compiler.grammar.regularGrammar.ParseRegex`
  - `org.qogir.compiler.grammar.regularGrammar.RegexTree`
  - `org.qogir.compiler.grammar.regularGrammar.RegexTreeNode`
- Thompson 构造（语法树 -> TNFA）
  - `org.qogir.compiler.grammar.regularGrammar.ThompsonConstruction`
  - `org.qogir.compiler.grammar.regularGrammar.TNFA`
- 上层编排入口
  - `org.qogir.simulation.scanner.Scanner`

## 2. 对外接口规范（REtoNFA）

以下为“外部调用推荐入口”。

### 2.1 文法构造接口

#### 2.1.1 `RegularGrammar(String[] regexes)`

- 作用：将多条规则构造成正则文法对象。
- 输入格式约定：每条规则应为 `name := regex`。
- 字母表提取规则：仅收集字母与数字字符，排除 `ε`。
- 输出：`RegularGrammar` 实例，内部包含
  - `patterns: ArrayList<Regex>`
  - `symbols: ArrayList<Character>`

注意事项（基于当前实现）：
- 名称截取使用 `r.substring(0, r.lastIndexOf(":=") - 1)`。
- 因此建议使用带空格写法 `name := regex`，避免规则名截断风险。

#### 2.1.2 `ArrayList<Regex> getPatterns()`

- 作用：获取文法中的全部正则模式。
- 返回：按输入顺序构造的 `Regex` 列表。

#### 2.1.3 `ArrayList<Character> getSymbols()`

- 作用：获取文法字母表。
- 返回：不含 `ε` 的字符集合（以 `ArrayList` 表示）。

### 2.2 单正则 RE->NFA 接口

#### 2.2.1 `RegexTree constructRegexTree(Regex r)`（`Scanner`）

- 作用：将单个正则表达式解析为语法树。
- 内部流程：`new ParseRegex(r).parse()`。
- 输出：`RegexTree`；并缓存到 `regexToRegexTree`。

输入前置条件：
- `r != null`
- `r.getRegex()` 非空，且字符集符合当前语法定义。

失败语义：
- 若解析失败，`parse()` 返回 `null`，此处将缓存 `null`。

#### 2.2.2 `TNFA constructRegexNFA(Regex r)`（`Scanner`）

- 作用：将单个正则表达式转换为 TNFA。
- 内部流程：
  1. `constructRegexTree(r)`
  2. `new ThompsonConstruction().translate(tree.getRoot(), tree.getRoot())`
  3. `setAlphabetForNfa(r, nfa)`

输出：`TNFA`

失败语义：
- 当前实现未对 `tree == null` 做保护，非法正则可能触发空指针异常。

#### 2.2.3 `void setAlphabetForNfa(Regex r, TNFA nfa)`（`Scanner`）

- 作用：根据正则文本提取字母表并写入 NFA。
- 提取规则：仅字母和数字；排除 `ε`。

### 2.3 文法级 RE->NFA 接口

#### 2.3.1 `HashMap<Regex, TNFA> constructAllNFA()`（`Scanner`）

- 作用：对文法中每个 `Regex` 分别构建 `TNFA`。
- 返回：`Regex -> TNFA` 映射。

#### 2.3.2 `TNFA constructNFA()`（`Scanner`）

- 作用：构建“文法总 NFA”（支持单正则和多正则）。

规则：
- 单正则：直接返回该正则对应 TNFA，并设置文法字母表。
- 多正则：
  - 先构建每个子 TNFA。
  - 新建总 TNFA，使用总起始状态通过 `ε` 边连接各子 TNFA 起始状态。
  - 合并子 TNFA 的迁移表。

输出约定：
- 返回值为 `TNFA`。
- `nfa.getAlphabet()` 最终设置为 `rg.getSymbols()`。

失败语义（当前实现）：
- 当单正则子构造失败时，后续 `nfa.setAlphabet(...)` 存在空指针风险。

## 3. 核心内部接口规范（供模块维护）

以下接口对实现维护重要，通常不作为业务层直接调用入口。

### 3.1 正则解析器接口

#### 3.1.1 `ParseRegex(Regex regex)`

- 作用：初始化解析器，将正则串写入字符队列并追加结束符 `%`。

#### 3.1.2 `RegexTree parse()`

- 作用：将正则串解析为语法树。
- 支持要素：
  - 基本字符：字母、数字、`ε`
  - 运算：`|`、连接、`*`、括号
- 返回：
  - 成功：`RegexTree`
  - 失败：`null`（并打印错误信息）

#### 3.1.3 `RegexTreeNode mergeStackAsOneChild(RegexTreeNode pt, Stack<RegexTreeNode> stack)`

- 作用：将栈内多个节点按顺序挂到同一父节点子链。

### 3.2 Thompson 构造接口

#### 3.2.1 `TNFA translate(RegexTreeNode node, RegexTreeNode root)`

- 作用：递归将语法树翻译为 TNFA。
- 节点类型映射：
  - `0`：基础字符
  - `1`：连接
  - `2`：并集
  - `3`：闭包
- 编号约定：当 `node == root` 时，重置 `State.STATE_ID = 0`。
- 返回：
  - 成功：对应 TNFA
  - 失败：`null`

### 3.3 TNFA 数据接口

`TNFA` 继承 `FiniteAutomaton`，核心访问点：

- `State getStartState()`（继承）
- `State getAcceptingState()`
- `void setAcceptingState(State acceptingState)`
- `LabeledDirectedGraph<State> getTransitTable()`（继承）
- `ArrayList<Character> getAlphabet()`（继承）

结构约束（实现约定）：
- TNFA 默认创建“唯一接受状态”，并标记为 `State.ACCEPT`。
- 边标签使用 `Character`，空转移使用 `ε`。

## 4. 数据模型与状态约定

### 4.1 状态类型

`State` 类型常量：

- `State.START = 0`
- `State.MIDDLE = 1`
- `State.ACCEPT = 2`
- `State.ACCEPTANDSTART = 20`

### 4.2 迁移表结构

- 类型：`LabeledDirectedGraph<State>`
- 边：`LabelEdge(source, target, label)`
- 常用能力：
  - `addVertex`
  - `addEdge`
  - `merge`
  - `vertexSet/edgeSet`

## 5. 失败语义与调用方约束

### 5.1 解析阶段（`ParseRegex.parse`）

- 对非法输入采用“打印错误 + 返回 `null`”。
- 常见失败场景：
  - 首字符非法
  - 括号不匹配
  - `|`、`*` 位置非法
  - 未知字符

### 5.2 构造阶段（`Scanner`）

- `constructRegexNFA` 未显式处理 `tree == null`，调用方应保证输入合法。
- `constructNFA` 在单正则分支中对 `null` NFA 的保护不完整，调用方应在上游拦截非法规则。

### 5.3 推荐调用约束

- 规则文本使用 `name := regex` 规范写法。
- 在调用 `constructRegexNFA/constructNFA` 前，建议先做规则合法性检查。
- 若需要稳定错误处理语义，建议调用方统一捕获运行时异常并映射为业务错误码。

## 6. 与 NFAtoDFA 的接口对接规范

虽然本文聚焦 REtoNFA，但现有代码中 NFAtoDFA 直接消费 `TNFA`，对接点如下：

### 6.1 对接入口

- `RDFA constructDFA(TNFA nfa)`（`Scanner`）
- 内部调用：`SubsetConstruction.subSetConstruct(tnfa)`

### 6.2 REtoNFA 向下游保证

REtoNFA 输出的 `TNFA` 需满足：

- 具备可访问的起始状态与接受状态。
- 迁移表完整可遍历（`getTransitTable().edgeSet()`）。
- 字母表已设置（`setAlphabetForNfa` 或 `constructNFA` 中设置）。
- 使用 `ε` 边表示空转移，供子集构造计算 epsilon-closure。

## 7. 标准调用示例

```java
String[] regexes = new String[]{"id := c(a|b)*"};
RegularGrammar rg = new RegularGrammar(regexes);
Scanner scanner = new Scanner(rg);

// 单正则 RE -> NFA
Regex r = rg.getPatterns().get(0);
TNFA nfa = scanner.constructRegexNFA(r);

// 文法级 RE -> NFA
TNFA grammarNfa = scanner.constructNFA();

// NFA -> DFA 对接
RDFA dfa = scanner.constructDFA(grammarNfa);
```

## 8. 版本说明

- 文档版本：v1（按当前仓库实现整理）
- 适用代码路径：`src/main/java/org/qogir/compiler/grammar/regularGrammar` 与 `src/main/java/org/qogir/simulation/scanner/Scanner.java`
