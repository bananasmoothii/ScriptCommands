package fr.bananasmoothii.scriptcommands.core.antlr4parsing;

import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;

import java.util.List;

/**
Totally copy-pasted from StackOverflow. (<a href="https://stackoverflow.com/questions/50064110/antlr4-java-pretty-print-parse-tree-to-stdout">https://stackoverflow.com/questions/50064110/antlr4-java-pretty-print-parse-tree-to-stdout</a>).
I did modified it.
*/
public class Antlr4TreeUtils {

    /** Platform dependent end-of-line marker */
    public static final String Eol = System.lineSeparator();
    /** The literal indent char(s) used for pretty-printing */
    public static String Indents = "|";
    private static int level;

    private Antlr4TreeUtils() {}

    /**
     * Pretty print out a whole tree. {@link Trees#getNodeText} is used on the node payloads to get the text
     * for the nodes. (Derived from Trees.toStringTree(....))
     */
    public static String toPrettyTree(Tree t, List<String> ruleNames) {
        level = 0;
        return process(t, ruleNames).replaceAll("(\\" + Indents + ")+(\\n|$)", "").replaceAll("\\r?\\n\\r?\\n", Eol);
    }

    private static String process(Tree t, List<String> ruleNames) {
        if (t.getChildCount() == 0) return Utils.escapeWhitespace(Trees.getNodeText(t, ruleNames), false);
        StringBuilder sb = new StringBuilder();
        sb.append(lead(level));
        level++;
        String s = Utils.escapeWhitespace(Trees.getNodeText(t, ruleNames), false);
        sb.append(s).append(": ");
        for (int i = 0; i < t.getChildCount(); i++) {
            sb.append(process(t.getChild(i), ruleNames));
        }
        level--;
        sb.append(lead(level));
        return sb.toString();
    }

    private static String lead(int level) {
        StringBuilder sb = new StringBuilder();
        if (level > 0) {
            sb.append(Eol);
            for (int cnt = 0; cnt < level; cnt++) {
                sb.append(Indents);
            }
        }
        return sb.toString();
    }
}