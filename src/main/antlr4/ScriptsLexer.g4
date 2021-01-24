lexer grammar ScriptsLexer;

@header {
package fr.bananasmoothii.scriptcommands.core.antlr4parsing;
}

@lexer::members {
	int expressionDepth = 0;
}

IF
	: 'if'
	;

ELIF
	: 'elif'
	;

ELSE
	: 'else'
	;

FOR
	: 'for'
	;

IN
	: 'in'
	;

WHILE
	: 'while'
	;

TRY
	: 'try'
	;

CATCH
	: 'catch'
	;

SWITCH
    : 'switch'
    ;

CASE
    : 'case'
    ;

BREAK
	: 'break'
	;

CONTINUE
	: 'continue'
	;

RETURN
	: 'return'
	;

ASSERT
    : 'assert'
    ;

LOG
    : 'log'
    ;

ASSIGNMENT
	: '='
	;

GLOBAL
	: 'global'
	;

DEL
	: 'del'
	;

THREAD
    : 'thread'
    ;

THROW
    : 'throw'
    ;

F_TEXT_PH_END
	: {_modeStack.contains(1)}? '}' -> popMode // 1 for F_TEXT
	;

OPEN_PAR
	: '(' {expressionDepth++;}
	;

CLOSE_PAR
	: ')' {expressionDepth--;}
	;

OPEN_BRACKET
	: '[' {expressionDepth++;}
	;

CLOSE_BRACKET
	: ']' {expressionDepth--;}
	;

OPEN_BRACE
	: '{'
	;

CLOSE_BRACE
	: '}'
	;

COMMA
	: ','
	;

MAKE_FIRST_ARG_OF
	: '->'
	;

COLON
	: ':'
	;

QUESTION_MARK
	: '?'
	;

fragment DIGIT
	: ('0' .. '9')
	;

fragment LETTER
	: [A-Za-z_\u00e0\u00e2\u00e4\u00e3\u00e5\u0101\u00e6\u00e1\u00e9\u00ea\u0117\u00e8\u0119\u0113\u00eb\u00ff\u00f9\u00fb\u00fa\u00fc\u016b\u00ef\u00ee\u00ec\u012f\u00ed\u012b\u00f2\u00f6\u014d\u00f8\u00f3\u00f4\u0153\u00f5\u00df\u015b\u0161\u0107\u00e7\u010d\u00c0\u00c2\u00c4\u00c3\u00c5\u0100\u00c6\u00c1\u00c9\u00ca\u0116\u00c8\u0118\u0112\u00cb\u0178\u00d9\u00db\u00da\u00dc\u016a\u00cf\u00ce\u00cc\u012e\u00cd\u012a\u00d2\u00d6\u014c\u00d8\u00d3\u00d4\u0152\u00d5\u015a\u0160\u0106\u00c7\u010c] // every letter with accent
	;

NOT
	: 'not'
	;

AND
	: 'and'
	;

OR
	: 'or'
	;

EQUALS
	: '=='
	;

NOT_EQUALS
	: '!='
	;

GREATER
	: '>'
	;

LESSER
	: '<'
	;

GREATER_OR_EQUALS
	: '>='
	;

LESSER_OR_EQUALS
	: '<='
	;

PLUS
	: '+'
	;

MINUS
	: '-'
	;

TIMES
	: '*'
	;

DIVIDE
	: '/'
	;

FLOOR_DIVIDE
	: '//'
	;

MODULO
	: '%'
	;

POW
	: '^'
	;

fragment INTEGER_LIKE
	: DIGIT
	| DIGIT (DIGIT | '_')* DIGIT
	;

INTEGER
	: '-'? INTEGER_LIKE
	;

DECIMAL
	: '-'? INTEGER_LIKE '.' INTEGER_LIKE
	;

TEXT
	: '"' (~'"' | '""')* '"'
	;

F_TEXT_START
	: 'f"' -> pushMode(F_TEXT)
	;

TRUE
	: 'true'
	;

FALSE
    : 'false'
    ;

NONE
	: 'none'
	;

VARIABLE
	: LETTER (LETTER | DIGIT)*
	;

NEW_LINE
	: {expressionDepth <= 0}? ('\n' | ';')+
	;

NEW_LINE_IN_EXPR
	: '\n' -> skip
	;

WHITE_SPACE
	: (' ' | '\t') -> skip
	;

COMMENT
	: '#' ~'\n'* -> skip // should be already handled by the yaml parser
	;


mode F_TEXT;

F_TEXT_PH_START
	: '{=' -> pushMode(DEFAULT_MODE)
	;

F_TEXT_CHAR
	: ~('"' | '{')+
	| '""'
	| '{' // {String after = nextToken().getText()} {after.charAt(0) != '=' && (after.charAt(0) != '"' || after.substring(0, 2).equals("\"\"")) && after.charAt(0) != }?
	| '{=/'
	;

F_TEXT_END
	: '"' -> popMode
	;

