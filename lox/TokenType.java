package lox;

enum TokenType
{
    // Grouping
    LPAREN, RPAREN, SEMI_COLON, LBRACE, RBRACE, COMMA, 
    // Arithmetic
    PLUS, MINUS, MUL, DIV, ASSIGN, MOD, EXP,

    // Logical
    EQUALS, NOT_EQUALS, GREATER, GREATER_EQUALS, LESSER, 
    LESSER_EQUALS,

    // Boolean
    NOT, AND, OR,

    // Bitwise
    BIT_NOT, BIT_AND, BIT_OR, BIT_XOR,

    // Literals
    ID, STRING, NUMBER,

    // Reserved words
    IF, ELSE, LET, DEFINE, FOR, WHILE, DO, RETURN, 
    TRUE, FALSE, NIL, BREAK, CONTINUE, PRINT, CLASS,
    SUPER, SELF, QUESTION, COLON,

    // Others
    END
}