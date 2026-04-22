package org.qogir.compiler.grammar.regularGrammar;

import java.util.ArrayDeque;
import java.util.Stack;

/**
 * 将正则表达式解析为语法树（{@link RegexTree}）的实现。
 */

public class ParseRegex {
    private final ArrayDeque<Character> queue = new ArrayDeque<>();

    /**
     * 使用给定正则表达式构造解析器。
     * <p>
     * 输入串会被拆分为字符序列，并在末尾附加 `%` 作为结束标记。
     *
     * @param regex 正则表达式对象
     */
    public ParseRegex(Regex regex) {
        char[] ch = regex.getRegex().toCharArray();
        for (char c : ch) {
            this.queue.add(c);
        }

        //%-end of the regex
        if (!this.queue.isEmpty())
            this.queue.add('%');
    }

    /**
     * 将当前正则表达式解析为语法树。
     * <p>
     * 解析过程中使用栈维护中间节点，并依据字符类型处理：
     * 基本字符、连接、并集、括号与闭包等运算符。
     *
     * @return 解析后的语法树；当表达式非法时返回 {@code null}
     */
    public RegexTree parse() {

        //Add your implementation :
        //Some error handling code has already been implemented here, but it's incomplete.
        //Please complete the implementation.
        //You can also choose to remove the existing error handling code and implement it from scratch instead.

        if (this.queue.isEmpty())
            return null;


        RegexTree tree = new RegexTree();
        //reset node id
        RegexTreeNode.Node_ID = 0;

        Stack<RegexTreeNode> stack = new Stack<>();
        //lookahead char
        char look = this.queue.poll();

        if (!Character.isLetter(look) && look != '(' && !Character.isDigit(look)) { //look != 'ε' &&
            //The first char must be a letter, ε or '('
            System.out.println("not a legal regex!(It must begin with a letter,ε or (,)");
            return null;
        } else if (look == '%') {
            System.out.println("a NULL regex!");
            return null;
        }

        int t;
        if (Character.isDigit(look) || Character.isLetter(look) || look == 'ε') {
            t = 0;
            RegexTreeNode node = new RegexTreeNode(look, t, null, null);
            stack.push(node);
            look = this.queue.poll();
        }
        boolean LeftParenthesisCheck = true;
        while (look != '%') {
            if (look == '*') {
                RegexTreeNode knode;
                t = stack.peek().getType();
                if (t == 0) {// is basic before *
                    knode = new RegexTreeNode('*', 3, stack.pop(), null);
                } else if (t == 5) { //is a ')' before *
                    stack.pop(); //pop ')'
                    knode = new RegexTreeNode('*', 3, stack.pop(), null);
                    stack.pop(); //pop '('
                } else { //is other char before *, not legal
                    System.out.println("not a legal regex!(It must be basic characters or ')' before *)");
                    return null;
                }
                stack.push(knode);
            }
            else if (look == '(') {
                if (LeftParenthesisCheck)
                {
                    int Count = 1;
                    for (char tempc : this.queue) {
                        if (tempc == '(') { // left parenthesis '('
                            Count++;
                        } else if (tempc == ')') { // right parenthesis ')'
                            Count--;
                        }
                    }
                    if (Count > 0) {
                        System.out.println("not a legal regex!(')' is missing.)");
                        return null;
                    }
                    LeftParenthesisCheck=false;
                }
                RegexTreeNode lnode = new RegexTreeNode('(', 4, null, null);
                stack.push(lnode);
                //how about the case of "...(..." (right parenthesis is missing)
            }
            else if (look == ')') {
                if (stack.isEmpty()) {
                    return null;
                }

                //t = stack.peek().getType();
                if (stack.peek().getType() == 4) { // case: ()
                    stack.pop();
                } else {
                    Stack<RegexTreeNode> rstack = new Stack<>();

                    while (!stack.isEmpty() && stack.peek().getType() != 4) {
                        if (stack.peek().getType() == 0 || stack.peek().getType() == 1 || stack.peek().getType() == 3) {//basic,kleene or concatenation(the case of conca exist?)
                            rstack.push(stack.pop());
                        } else if (stack.peek().getType() == 2) {//union, case (stack|?)
                            if (rstack.size() == 0) { //case (stack|)
                                System.out.println("not a legal regex '|)'");
                                return null;
                            }

                            // case (?|...)
                            RegexTreeNode unode = stack.pop();
                            if (rstack.size() > 1) { //case (stack|...)
                                RegexTreeNode cnode = new RegexTreeNode('-', 1, null, null);
                                cnode = mergeStackAsOneChild(cnode, rstack);//
                                unode.getLastChild().setNextSibling(cnode);
                            } else { //size = 1, case (stack|.)
                                unode.getLastChild().setNextSibling(rstack.pop());
                            }
                            rstack.push(unode);

                        } else if (stack.peek().getType() == 5) { //case ...(stack))
                            stack.pop(); //pop ')'
                            rstack.push(stack.pop());
                            stack.pop(); // pop '('
                        }
                    }

                    if (stack.isEmpty()) {
                        System.out.println("not a legal regex ('(' is missing.)");
                        return null;
                    } else if (stack.peek().getType() == 4) { //case (rstack)
                        // 1) convert the nodes in rstack into one node
                        // 2) push the converted node and the right-parenthesis node
                        if (!rstack.isEmpty()) {
                            if (rstack.size() > 1) {
                                RegexTreeNode cnode = new RegexTreeNode('-', 1, null, null);
                                cnode = mergeStackAsOneChild(cnode, rstack);
                                stack.push(cnode);

                            } else { //rstack.size = 1
                                stack.push(rstack.pop());
                            }
                            RegexTreeNode rnode = new RegexTreeNode(')', 5, null, null);
                            stack.push(rnode);
                        } else {
                            stack.pop();
                        }
                    }
                }
            }
            else if (look == '|') {
                t = stack.peek().getType();
                if (t == 4 || t == 2) {
                    System.out.println("not a legal regex('(| or ||')");
                    return null;
                }

                RegexTreeNode unode;
                Stack<RegexTreeNode> ustack = new Stack<>();
                while (!stack.isEmpty() && stack.peek().getType() != 2 && stack.peek().getType() != 4) {
                    if (stack.peek().getType() == 5) {
                        stack.pop();
                        ustack.push(stack.pop());
                        stack.pop();
                    } else {
                        ustack.push(stack.pop());
                    }
                }

                if (stack.isEmpty() || stack.peek().getType() == 4) {
                    unode = new RegexTreeNode('|', 2, null, null);
                    RegexTreeNode firstChildNode;
                    if (ustack.size() == 1) {
                        firstChildNode = ustack.pop();
                    } else if (ustack.size() > 1) {
                        RegexTreeNode cnode = new RegexTreeNode('-', 1, null, null);
                        cnode = mergeStackAsOneChild(cnode, ustack);
                        firstChildNode = cnode;
                    } else {
                        System.out.println("not a legal regex(in considering look='|')");
                        return null;
                    }
                    unode.setFirstChild(firstChildNode);
                } else { //type=2
                    unode = stack.pop();
                    RegexTreeNode lastNode = unode.getLastChild();
                    if (ustack.size() == 1) {
                        lastNode.setNextSibling(ustack.pop());
                    } else {
                        RegexTreeNode cnode = new RegexTreeNode('-', 1, null, null);
                        cnode = mergeStackAsOneChild(cnode, ustack);
                        lastNode.setNextSibling(cnode);
                    }
                }
                stack.push(unode);
            }
            else if (Character.isDigit(look) || Character.isLetter(look) || look == 'ε') {
                RegexTreeNode bnode = new RegexTreeNode(look, 0, null, null);
                stack.push(bnode);
            }
            else {
                System.out.println("not a legal regex("+look+"is an unknown character)");
                return null;
            }

            look = this.queue.poll();
        }

        //if look == '%'
        if (stack.isEmpty()) {
            return null;
        }
        Stack<RegexTreeNode> pstack = new Stack<>();
        while (!stack.isEmpty() && stack.peek().getType() != 2) {
            pstack.push(stack.pop());
        }

        if (stack.isEmpty()) {
            while (!pstack.isEmpty())
                stack.push(pstack.pop());
        } else if (stack.peek().getType() == 2) {
            if (pstack.peek().getType() == 5) {
                while (!pstack.isEmpty()) {
                    stack.push(pstack.pop());
                }
            } else {
                RegexTreeNode unode;
                unode = stack.pop();
                if (pstack.size() == 1) {
                    unode.getLastChild().setNextSibling(pstack.pop());
                } else {
                    RegexTreeNode cnode = new RegexTreeNode('-', 1, null, null);
                    cnode = mergeStackAsOneChild(cnode, pstack);
                    unode.getLastChild().setNextSibling(cnode);
                }
                stack.push(unode);
            }
        }

        // merge the nodes in stack as one node
        Stack<RegexTreeNode> treeStack = new Stack<>();
        while (!stack.isEmpty()) {
            if (stack.peek().getType() == 4 || stack.peek().getType() == 5) {
                stack.pop();
            } else
                treeStack.push(stack.pop());
        }

        if (treeStack.isEmpty()) {
            return null;
        }

        if (treeStack.size() > 1) {
            RegexTreeNode treeNode = new RegexTreeNode('-', 1, null, null);
            treeNode = mergeStackAsOneChild(treeNode, treeStack);
            tree.setRoot(treeNode);
        } else {
            tree.setRoot(treeStack.pop());
        }
        return tree;
    }

    /**
     * 将栈中的多个节点按顺序合并为同一父节点的子节点链。
     *
     * @param pt 目标父节点
     * @param stack 待合并的节点栈
     * @return 合并完成后的父节点
     */
    public RegexTreeNode mergeStackAsOneChild(RegexTreeNode pt, Stack<RegexTreeNode> stack) {
        RegexTreeNode temp1 = stack.pop();
        RegexTreeNode firstChild = temp1;
        while (!stack.isEmpty()) {
            RegexTreeNode temp2 = stack.pop();
            temp1.setNextSibling(temp2);
            temp1 = (RegexTreeNode) temp1.getNextSibling();
        }
        if (pt.getFirstChild() == null) {
            pt.setFirstChild(firstChild);
        } else {
            RegexTreeNode temp = pt.getLastChild();
            temp.setNextSibling(firstChild);
        }
        return pt;
    }
}
