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

grammar Permission;

@header {
package fr.bananasmoothii.scriptcommands.core.antlr4parsing;
}

start
    : block EOF
    ;

block
    : atom (operator += (AND | OR) atom)*
    ;

atom
    : PERMISSION_WORD (DOT PERMISSION_WORD)*  # realPermission
    | OPEN_PAR block CLOSE_PAR                # parenthesis
    ;

AND: 'and';

OR: 'or';

OPEN_PAR: '(';

CLOSE_PAR: ')';

DOT: '.';

PERMISSION_WORD: ~[ \n\r ().]+;

SPACE: [ \n\r ]+ -> skip;
