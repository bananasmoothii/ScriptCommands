package fr.bananasmoothii.scriptcommands.core.antlr4parsing;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ParserDumper {
    private PrintWriter writer;

    public ParserDumper() {
        File file = new File("parsing dump.log");

        try {
            this.writer = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void dumpParsing(String name, String scripts, Lexer lexer, Parser parser, ParseTree parseTree) {
        writer.println("\n\n\n--------[ " + name + " ]-------");
        writer.println("\nHere is your formatted script:\n");
        writer.println(scripts);
        writer.println("\nHere are the tokens. A token can be a comma, a number... \"EOF\" means End Of File ;)\n");
        printTokens(lexer);
        writer.println("\nAnd here is the (roller coaster of the) paring rules\n");
        printRules(parser, parseTree);
        writer.close();
    }

    /**
     * Prints the tokens of a lexer (and resets it)
     * @see ParserDumper#printRules(Parser, ParseTree)
     */
    public void printTokens(Lexer lexer) {
        Vocabulary vocabulary = lexer.getVocabulary();
        lexer.reset();
        for (Token token: lexer.getAllTokens()) {
            this.writer.println(token.getLine() + ":" + token.getCharPositionInLine() + " '" + token.getText() + "' " +
                    vocabulary.getSymbolicName(token.getType()));
        }
    }

    /**
     * Pretty-prints a {@link ParseTree}
     * @see ParserDumper#printTokens(Lexer)
     */
    public void printRules(Parser parser, ParseTree tree) {
        this.writer.println(Antlr4TreeUtils.toPrettyTree(tree, Arrays.asList(parser.getRuleNames())));
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }


    public void setWriter(PrintStream printStream) {
        setWriter(new PrintWriter(new OutputStreamWriter(printStream, StandardCharsets.UTF_8))); // doesn't work
    }

}
