parser grammar ScriptsParser;

@header {
package fr.bananasmoothii.scriptcommands.core.antlr4parsing;
}

options {
    tokenVocab=ScriptsLexer;
}


start
	: line* line_only? EOF
	;

line
	: expression NEW_LINE
	| assignment NEW_LINE
	| deletion NEW_LINE
	| if_block
	| for_block
	| while_block
	| try_block
	| switch_block
	| breaker NEW_LINE
	| assertion NEW_LINE
	| thread_expression NEW_LINE
	| thread_block
	| throw_ NEW_LINE
	| NEW_LINE
	;

line_only
	: expression
	| assignment
	| deletion
	| if_block
	| for_block
	| while_block
	| try_block
	| switch_block
	| breaker
	| assertion
	| thread_expression
	| thread_block
	| throw_
	| NEW_LINE
	;

if_block
	: IF comparison block
	  (ELIF comparison block)*
	  (ELSE block)?
	;

for_block
	: FOR VARIABLE IN expression block
	;

while_block
	: WHILE expression block
	;

try_block
	: TRY block catch_block +
	;

catch_block
    : CATCH (varToAssign += VARIABLE ASSIGNMENT)? expression (COMMA expression)* block
    ;

switch_block
    : SWITCH expression NEW_LINE ?
      OPEN_BRACE NEW_LINE ?
      case_block *
      (ELSE block) ?
      CLOSE_BRACE NEW_LINE ?
    ;

case_block
    : CASE expression (COMMA expression) * block
    ;

block
	: NEW_LINE ? OPEN_BRACE NEW_LINE ? line* line_only ? CLOSE_BRACE NEW_LINE ?
	;

breaker
	: BREAK              # break
	| CONTINUE           # continue
	| RETURN expression? # return
	;

expression
	: expression_part
	| log
	| maths
	| comparison
	| mini_if
	;

expression_part
	: expression_between_dots (MAKE_FIRST_ARG_OF function)*
	;

expression_between_dots
	: function                   # exprFunction
    | DECIMAL                      # exprDecimal
    | INTEGER                        # exprInteger
    | (TRUE | FALSE)                   # exprBoolean
    | NONE                               # exprNone
    | TEXT subscription*                   # exprText
    | formatted_text subscription*           # exprFText
	| list subscription*                       # exprList
	| dictionary subscription*                   # exprDictionary
	| OPEN_PAR expression CLOSE_PAR subscription*  # exprParExpr
	;

thread_expression
    : THREAD expression ? // name of the thread
      expression
    ;

thread_block
    : THREAD expression ? // name of the thread
      block
    ;

throw_
    : THROW expression (COLON expression) ?
    ;

function
	: VARIABLE arguments? subscription*
	;

arguments
	: OPEN_PAR ((expression (COMMA expression)* (COMMA splatList)* )? | (splatList (COMMA splatList*))) CLOSE_PAR
	;

list
	: OPEN_BRACKET
	( (expression (COMMA expression)* )?
	| expression FOR VARIABLE IN expression (IF comparison)?
	) CLOSE_BRACKET
	;

dictionary
	: OPEN_BRACKET 
	( (expression ASSIGNMENT expression (COMMA expression ASSIGNMENT expression)* | ASSIGNMENT)
	| expression ASSIGNMENT expression FOR VARIABLE IN expression (IF comparison)?
	) CLOSE_BRACKET
	;

splatList
    : TIMES expression_part
    ;

//splatDict
//    : TIMES TIMES expression_part #splatDict // not supported yet. Not sure if this is going to be supported.
//    ;

assignment
	: GLOBAL? VARIABLE
	  operator = (PLUS | MINUS | TIMES | DIVIDE | FLOOR_DIVIDE | MODULO | POW)?
	  ASSIGNMENT expression
	;

deletion
	: DEL VARIABLE
	;

subscription
	: OPEN_BRACKET (sub1=expression? COLON sub2=expression? | subSingle=expression) CLOSE_BRACKET
	;

single_subscription
    : OPEN_BRACKET expression CLOSE_BRACKET
    ;

comparison
	: comp_molecule ( operator += (AND | OR) comp_molecule )*
	;

comp_molecule
	: comp_atom ( operator += (EQUALS | NOT_EQUALS | GREATER | LESSER | GREATER_OR_EQUALS | LESSER_OR_EQUALS | IN) comp_atom )*
	;

comp_atom
	: NOT ?
	( expression_part
	| OPEN_PAR expression CLOSE_PAR
	| maths
	| log
	)
	;

maths
	: multiplication (operator += (PLUS | MINUS) multiplication)*
	;

multiplication
	: pow (operator += (TIMES | DIVIDE | FLOOR_DIVIDE | MODULO) pow)*
	;

pow
	: left = maths_atom (POW (rigth1=maths_atom | rigth2 = pow))?
	;

maths_atom
	: MINUS maths_atom
	| expression_part
	| OPEN_PAR expression CLOSE_PAR
	| log
	;

mini_if
	: comparison // could be just expression but an expression will be used almost everytime so this will make less deeper parse trees.
	  QUESTION_MARK yes = expression ELSE no = expression
	;

log
    : LOG expression_part
    ;

assertion
    : ASSERT comparison (COLON expression)?
    ;

formatted_text
	: F_TEXT_START f_text_inner F_TEXT_END
	;

f_text_inner
	: f_text_chars (f_text_placeholder (f_text_chars f_text_placeholder)* f_text_chars)?
	;

f_text_chars
	: (chars += F_TEXT_CHAR)*
	;

f_text_placeholder
	: F_TEXT_PH_START expression F_TEXT_PH_END
	;