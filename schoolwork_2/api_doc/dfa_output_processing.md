# DFA 输出处理说明（基于现有代码）

本文档基于 src 下当前实现整理，说明 DFA 的输出处理方式，包括：

- 文本输出（控制台）
- 结构化输出（用于前端展示或接口返回）
- 文件持久化输出（接口保留与格式约定）

## 1. 输出处理总览

当前项目中，DFA 输出主要有三条路径：

1. 文本字符串输出
- 通过 toString 及状态映射字符串方法输出 DFA 信息。

2. DTO/JSON 输出
- 将 DFA 转换为 StateInfosDTO（节点+边）后，再由 JSON 序列化输出。

3. 文件持久化输出
- 提供 exportJson 接口，但当前方法体为空；注释中给出了目标 JSON 格式。

## 2. 文本输出方式

### 2.1 有限自动机通用文本输出

代码位置：
- src/main/java/org/qogir/compiler/FA/FiniteAutomaton.java

方法：
- toString()

输出内容结构：
- Alphabet: [符号表]
- Total edges: 边数量
- Start State: 起始状态 id
- the transitTable is:
  - 每条边一行，格式为 (source:type->target:type@label)

适用场景：
- 控制台调试展示
- 教学过程中的人工核对

### 2.2 DFA 与 NFA 状态集合映射输出

代码位置：
- src/main/java/org/qogir/compiler/grammar/regularGrammar/RDFA.java

方法：
- StateMappingBetweenDFAAndNFAToString()

输出内容结构（逐行）：
- DFA State:{dfaState}	NFA State set:	{nfaState1,nfaState2,...}

适用场景：
- 观察子集构造结果
- 验证某个 DFA 状态由哪些 NFA 状态组成

## 3. 结构化输出方式（DTO/JSON）

### 3.1 DFA 图结构输出

代码位置：
- src/main/java/org/qogir/simulation/util/StateDiagram.java

方法：
- getStateDiagramFromDFA(RDFA rdfa)

返回类型：
- StateInfosDTO

字段结构：
- nodeInfos: ArrayList<NodeInfosDTO>
  - NodeInfosDTO.id: 节点 id
  - NodeInfosDTO.label: 展示标签（使用状态 sid）
  - NodeInfosDTO.type: normal 或 accept
  - NodeInfosDTO.color: 可选字段
- edges: ArrayList<EdgesDTO>
  - EdgesDTO.from: 起点 id
  - EdgesDTO.to: 终点 id
  - EdgesDTO.label: 边标签

输出规则要点：
- 调用前会先执行 rdfa.renumberSID()，用于统一展示序号。
- 会插入一个虚拟起始节点：id = -1，label = start，type = text。
- 若图非空，会增加一条起始引导边：-1 -> firstId，label 为空字符串。
- 多条同起点同终点边会被 mergeSameEdges 合并为一条，label 以逗号拼接。

### 3.2 子集构造过程输出（带 DFA 图快照）

代码位置：
- src/main/java/org/qogir/simulation/logger/SubsetConsLogger.java
- src/main/java/org/qogir/simulation/logger/dto/SubsetConsDTO.java

输出对象：
- SubsetConsDTO
  - tableColsInfos: 构造表列信息
  - subsetConsTableDTOS: 构造表行数据
  - stateInfos: 当前步骤 DFA 图（StateInfosDTO）

序列化位置：
- ConstructionLogger.returnStepQueue()
- ConstructionLogger.returnStepQueues()

序列化方式：
- 使用 Fastjson 进行 JSON 字符串输出。

适用场景：
- 前端逐步骤回放 NFA->DFA 转换过程
- 教学演示

## 4. 文件持久化方式

### 4.1 现状

代码位置：
- src/main/java/org/qogir/compiler/FA/FiniteAutomaton.java

方法：
- exportJson(String filename)

当前状态：
- 方法体为空，尚未启用文件持久化。

### 4.2 现有代码中约定的目标格式

在同文件注释中，已给出 JSON 持久化格式约定：

{
  "start": 0,
  "edges": [
    {
      "source": {"id": 0, "type": 0},
      "target": {"id": 1, "type": 1},
      "label": "a"
    }
  ]
}

文件约定：
- 文件名：filename + .json
- 编码：UTF-8

### 4.3 与当前结构化输出的差异

StateDiagram 输出格式（nodeInfos + edges）与 exportJson 注释格式（start + edges[source/target]）并不相同。

因此若后续实现文件持久化，建议明确二选一：

1. 保持 exportJson 注释格式，作为底层自动机存储格式。
2. 复用 StateInfosDTO 格式，作为前端展示格式并直接落盘。

建议在接口层增加 version 字段，以保证后续格式演进兼容。

## 5. 推荐输出处理流程（与当前代码一致）

1. 通过 Scanner.constructDFA 或 Scanner.constructAllDFA 得到 RDFA。
2. 控制台展示时：使用 dfa.toString() 与 dfa.StateMappingBetweenDFAAndNFAToString()。
3. 前端展示时：使用 StateDiagram.getStateDiagramFromDFA(dfa) 得到 StateInfosDTO。
4. 需要 JSON 字符串时：使用 ConstructionLogger 的 returnStepQueue/returnStepQueues 或业务层统一序列化。
5. 需要文件落盘时：当前需补全 exportJson；格式建议与现有注释或 DTO 格式二选一并固定。

## 6. 小结

- 当前 DFA 输出已经覆盖“文本输出”和“结构化 JSON 输出”。
- 文件持久化接口已预留，但尚未实现。
- 现有实现最稳定的对外结构化格式为 StateInfosDTO（nodeInfos + edges）。
- 若要上线持久化，建议先统一文件格式规范，再补全 exportJson 实现。
