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
