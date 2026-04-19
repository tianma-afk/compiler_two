package org.qogir.compiler.grammar.regularGrammar;

import org.qogir.compiler.util.tree.DefaultTreeNode;

import java.io.Serial;

/**
 * 正则表达式语法树节点。
 * <p>
 * 节点类型约定：
 * 0-基本字符；1-连接；2-并集；3-闭包；4-左括号；5-右括号。
 */
public class RegexTreeNode extends DefaultTreeNode {
    @Serial
    private static final long serialVersionUID = 8199272493386097880L;
    public static int Node_ID = 0; //assign Node_ID to a state

    /**
     * Every state has a unique id which can not be modified.
     */
    private final int id;//state id
    private Character value;
    private int type; //0-basic；1-concatenation；2-union； 3-kleene closure; 4-leftParenthesis; 5-rightParenthesis

    /**
     * 创建一个不带子节点的语法树节点。
     *
     * @param ch 节点字符值
     * @param t 节点类型
     */
    public RegexTreeNode(Character ch, int t) {
        super();
        value = ch;
        type = t;
        id = Node_ID++;
    }

    /**
     * 创建一个带有子节点与兄弟节点关系的语法树节点。
     *
     * @param v 节点字符值
     * @param type 节点类型
     * @param firstChild 第一个子节点
     * @param nextSibling 下一个兄弟节点
     */
    public RegexTreeNode(char v, int type, RegexTreeNode firstChild, RegexTreeNode nextSibling){
        super(firstChild, nextSibling);
        this.value = v;
        this.type = type;
        id = Node_ID++;
    }

    /**
     * 设置节点类型。
     *
     * @param type 节点类型编码
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * 设置节点字符值。
     *
     * @param value 节点字符
     */
    public void setValue(Character value) {
        this.value = value;
    }

    /**
     * 获取节点字符值。
     *
     * @return 节点字符
     */
    public Character getValue() {
        return value;
    }

    /**
     * 获取节点唯一编号。
     *
     * @return 节点编号
     */
    public Integer getId() {
        return id;
    }

    /**
     * 获取节点类型编码。
     *
     * @return 节点类型
     */
    public int getType() {
        return type;
    }

    /**
     * 获取当前节点的最后一个子节点。
     *
     * @return 最后一个子节点；若无子节点则返回 {@code null}
     */
    public RegexTreeNode getLastChild() {

        RegexTreeNode theNode = (RegexTreeNode) this.getFirstChild();
        if (theNode != null) { //the firstChild is not the last child.
            while (theNode.getNextSibling() != null) {
                theNode = (RegexTreeNode) theNode.getNextSibling();
            }
        }
        return theNode;
    }


    /**
     * 返回节点简要字符串，格式为 value:type。
     *
     * @return 节点字符串表示
     */
    @Override
    public String toString() {
        return this.value + ":" + this.type;
    }
}
