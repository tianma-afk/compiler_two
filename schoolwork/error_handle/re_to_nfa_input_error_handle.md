# RE 转 NFA 输入错误处理说明

本文档基于 src 下现有实现整理，重点说明 RE 转 NFA 过程中与输入相关的两类错误处理。

## 3.4 输入错误处理

### 3.4.1 应用非法符号的错误
【正则表达式中出现不符合规定的符号】

#### 合规符号范围（按当前实现）
- 基本符号：字母、数字、ε
- 运算符：|、*、(、)

#### 代码中的判定位置
- 文件：src/main/java/org/qogir/compiler/grammar/regularGrammar/ParseRegex.java
- 入口判定：parse() 开始阶段会检查首字符。
  - 首字符若不是字母、数字或 (，则判定非法并输出提示后返回 null。
  - 对空输入（或等价空队列）返回 null。

#### 当前处理方式
- 主要采用：输出错误信息（System.out.println）+ 返回 null。
- 典型提示信息：
  - not a legal regex!(It must begin with a letter,ε or (,)
  - a NULL regex!

#### 现状中的重要风险
- 对“中间位置出现非法字符”缺少统一拒绝分支。
  - 在 parse() 的主循环中，字符只对 *, (, ), |, 基本字符/ε 进行分支处理。
  - 若出现其他字符（例如 #、@、空格、中文标点），当前实现没有显式报错分支，字符会被跳过，可能导致后续构造结果偏离预期。

---

### 3.4.2 违背正则表达式结构规则的错误
【正确使用合规的符号定义正则表达式，但定义的正则表达式违背了结构规则】

#### 代码中的判定位置
- 文件：src/main/java/org/qogir/compiler/grammar/regularGrammar/ParseRegex.java
- 主要结构规则检查包括：

1. 闭包 * 的前驱合法性检查
- 规则：* 前面必须是“基本字符”或“右括号封闭的子表达式”。
- 非法时输出：not a legal regex!(It must be basic characters or ')' before *)

2. 括号匹配检查
- 缺失右括号：在首次遇到 ( 时，会扫描剩余队列计数；若右括号不足，报错并返回 null。
  - 提示：not a legal regex!(')' is missing.)
- 缺失左括号：处理 ) 归约时若未找到匹配的 (，报错并返回 null。
  - 提示：not a legal regex ('(' is missing.)

3. 并运算 | 的结构检查
- 禁止出现 (| 或 || 这类位置错误：
  - 提示：not a legal regex('(| or ||')
- 禁止出现 |) 这类“右侧缺操作数”场景：
  - 提示：not a legal regex '|)'
- 在 | 的归约阶段，如果左侧无法形成合法子表达式，也会报错返回 null：
  - 提示：not a legal regex(in considering look='|')

#### 当前处理方式
- 同样采用：输出错误信息（System.out.println）+ 返回 null。

#### 链路级风险（与结构错误直接相关）
- 文件：src/main/java/org/qogir/simulation/scanner/Scanner.java
- constructRegexNFA() 中直接使用 tree.getRoot() 调用 ThompsonConstruction.translate()。
- 当 parse() 因结构错误返回 null 时，这里缺少空值保护，可能触发空指针异常（NullPointerException），导致错误从“可控提示”升级为运行时异常。

---

## 小结
- 当前项目已经对“首字符非法、括号不匹配、| 和 * 的结构错误”等核心输入问题做了基础防护。
- 主要不足在于：
  - 中间非法字符未统一报错；
  - 上层构造链路对 parse() 的 null 返回值缺少一致的空值保护。
- 因此，在 RE 转 NFA 阶段，建议把“解析失败”作为显式错误状态向上抛出或统一封装，避免进入后续构造流程。