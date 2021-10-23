/*
 *    Copyright 2020 ScriptCommands
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

parser grammar ScriptsParser;

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
	| throw_ NEW_LINE
	| block_assignment
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
	| throw_
	| block_assignment
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
    : CATCH (varToAssign = VARIABLE ASSIGNMENT)? expression (COMMA expression)* block
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
	| thread_expression
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
    | TEXT get_from_list*                   # exprText
    | formatted_text get_from_list*           # exprFText
	| list get_from_list*                       # exprList
	| dictionary get_from_list*                   # exprDictionary
	| OPEN_PAR expression CLOSE_PAR get_from_list*  # exprParExpr
	;

// TODO: implement in executor
thread_expression
    : THREAD
      (IN ? expression) ? // name of the thread if "in" is not specified, else or name of the thread group
      expression
    ;

thread_block
    : THREAD
      (IN ? expression) ? // name of the thread if "in" is not specified, else or name of the thread group
      block
    ;

throw_
    : THROW expression (COLON expression) ?
    ;

function
	: VARIABLE arguments? get_from_list*
	;

arguments
	: OPEN_PAR
	( (expression | simpleAssignment | splatList | splatDict)
	  (COMMA (expression | simpleAssignment | splatList | splatDict))*
	)? CLOSE_PAR
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

splatDict
    : TIMES TIMES expression_part
    ;

simpleAssignment
    : VARIABLE ASSIGNMENT expression
    ;

assignment
	: GLOBAL? VARIABLE
	  operator = (PLUS | MINUS | TIMES | DIVIDE | FLOOR_DIVIDE | MODULO | POW)?
	  ASSIGNMENT expression
	;

block_assignment // could be named "thread_assignment", but I think I will use the same for lamdas
    : GLOBAL? VARIABLE
      operator = (PLUS | MINUS | TIMES | DIVIDE | FLOOR_DIVIDE | MODULO | POW)?
      ASSIGNMENT
      value = thread_block
    ;

deletion
	: DEL VARIABLE
	;

get_from_list
	: OPEN_BRACKET (nb1=expression? COLON nb2=expression? | nbSingle=expression) CLOSE_BRACKET
	;

comparison
	: comp_molecule ( operator += (AND | OR) comp_molecule )*
	;

comp_molecule
	: comp_atom
	( ( operator += (EQUALS | NOT_EQUALS | GREATER | LESSER | GREATER_OR_EQUALS | LESSER_OR_EQUALS) comp_atom )*
	| operator += IN comp_atom
	);

comp_atom
	: NOT ?
	( expression_part
	| OPEN_PAR expression CLOSE_PAR
	| maths
	| log
	);

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
	: comparison // could be just expression but an expression will be used almost everytime so this will make less deeper parse trees, plus a comparison can contain only an expression
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