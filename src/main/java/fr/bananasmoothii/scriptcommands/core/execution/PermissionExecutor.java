package fr.bananasmoothii.scriptcommands.core.execution;

import fr.bananasmoothii.scriptcommands.core.antlr4parsing.PermissionBaseVisitor;
import fr.bananasmoothii.scriptcommands.core.antlr4parsing.PermissionParser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PermissionExecutor extends PermissionBaseVisitor<Boolean> {
    private Permissible player;

    public PermissionExecutor(Permissible player) {
        this.player = player;
    }

    @Override
    public Boolean visitStart(PermissionParser.StartContext ctx) {
        if (player.isOp() || player.hasPermission("*"))
            return true;
        return visit(ctx.block());
    }

    @Override
    public Boolean visitBlock(PermissionParser.BlockContext ctx) {
        if (ctx.operator.size() == 0)
            return visit(ctx.atom(0));
        boolean before = executeIf(ctx.atom(0), ctx.operator.get(0), ctx.atom(1));
        for (int i = 2; i < ctx.atom().size(); i++) {
            before = executeIf(before, ctx.operator.get(i - 1), ctx.atom(i));
        }
        return before;
    }

    /**
     * Also checks for wildcards
     */
    @Override
    public Boolean visitRealPermission(PermissionParser.RealPermissionContext ctx) {
        System.out.println("checking if player has " + ctx.getText() + " (first try)");
        if (player.hasPermission(ctx.getText())) {
            System.out.println("yes");
            return true;
        }
        ArrayList<String> permissionWords = new ArrayList<>();
        for (TerminalNode terminalNode: ctx.PERMISSION_WORD()) {
            permissionWords.add(terminalNode.getText());
        }
        while (permissionWords.size() > 0) {
            permissionWords.remove(permissionWords.size() - 1);
            System.out.println("checking if player has " + joinWithEnd(permissionWords) + "*");
            if (player.hasPermission(joinWithEnd(permissionWords) + "*")) {
                System.out.println("yes");
                return true;
            }
        }
        System.out.println("no");
        return false;
    }

    @Override
    public Boolean visitParenthesis(PermissionParser.ParenthesisContext ctx) {
        return visit(ctx.block());
    }

    public boolean executeIf(@NotNull PermissionParser.AtomContext a, @NotNull Token andOr, @NotNull PermissionParser.AtomContext b) {
        return executeIf(visit(a), andOr, b);
    }
    public boolean executeIf(boolean a, @NotNull Token andOr, @NotNull PermissionParser.AtomContext b) {
        if (andOr.getType() == PermissionParser.AND) {
            if (!a)
                return false;
            return visit(b);
        }
        if (a)
            return true;
        return visit(b);
    }

    private String joinWithEnd(ArrayList<String> list) {
        StringBuilder result = new StringBuilder();
        for (String string: list) {
            result.append(string)
                    .append(".");
        }
        return result.toString();
    }
}
