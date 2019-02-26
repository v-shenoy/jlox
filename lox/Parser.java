package lox;

import java.util.List;

class Parser
{
    private final List<Token> tokens;
    private int curr = 0;

    private static class ParseError extends RuntimeException {}

    Parser(List<Token> tokens)  
    {
        this.tokens = tokens;
    }

    Expr parse()
    {
        try
        {
            return expression();
        }
        catch(ParseError error)
        {
            return null;
        }
    }

    private Expr expression()
    {
        return equality();
    }

    private Expr equality()
    {
        Expr left = comparison();
        while(match(TokenType.NOT_EQUALS, TokenType.EQUALS))
        {
            Token op = previous();
            Expr right = comparison();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr comparison()
    {
        Expr left = addition();
        while(match(TokenType.GREATER, TokenType.GREATER_EQUALS, TokenType.LESSER, TokenType.LESSER_EQUALS))
        {
            Token op = previous();
            Expr right = addition();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr addition()
    {
        Expr left = multiplication();
        while(match(TokenType.PLUS, TokenType.MINUS))
        {
            Token op = previous();
            Expr right = multiplication();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr multiplication()
    {
        Expr left = modulo();
        while(match(TokenType.MUL, TokenType.DIV))
        {
            Token op = previous();
            Expr right = modulo();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr modulo()
    {
        Expr left = exponentiation();
        while(match(TokenType.MOD))
        {
            Token op = previous();
            Expr right = exponentiation();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr exponentiation()
    {
        Expr left = unary();
        while(match(TokenType.EXP))
        {
            Token op = previous();
            Expr right = unary();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr unary()
    {
        if(match(TokenType.BIT_NOT, TokenType.MINUS))
        {
            Token op = previous();
            Expr right = unary();
            return new Expr.Unary(op, right);
        }
        return primary();
    }

    private Expr primary()
    {
        Expr expr = null;
        if(match(TokenType.FALSE))
        {
            expr = new Expr.Literal(false);
        }
        else if(match(TokenType.TRUE))
        {
            expr = new Expr.Literal(true);
        }
        else if(match(TokenType.NIL))
        {
            expr = new Expr.Literal(null);
        }
        else if(match(TokenType.NUMBER, TokenType.STRING))
        {
            expr = new Expr.Literal(previous().literal);
        }
        else if(match(TokenType.LPAREN))
        {
            Expr grp = expression();
            consume(TokenType.RPAREN, "Unmatched ')'");
            expr = new Expr.Grouping(grp);
        }
        if(expr != null)
        {
            return expr;
        }
        throw error(peek(), "Expect expression"); 
    }

    private Token consume(TokenType type, String message)
    {
        if(check(type))
        {
            return advance();
        }
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message)
    {
        Lox.error(token, message);
        return new ParseError();
    }

    private void sync()
    {
        advance();
        while(!atEnd())
        {
            if(previous().type == TokenType.SEMI_COLON)
            {
                return;
            }
        }
        switch(peek().type)
        {
            // Enums don't need to be qualified in switch cases apparently
            case CLASS:
            case DEFINE:
            case LET:
            case FOR:
            case WHILE:
            case DO:
            case IF:  
            case PRINT:
                return;
        }   
        advance();
    }

    private boolean match(TokenType... types)
    {
        for(TokenType type : types)
        {
            if(check(type))
            {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType expected)
    {
        if(atEnd())
        {
            return false;
        }
        return peek().type == expected;
    }

    private Token advance()
    {
        if(!atEnd())
        {
            curr++;
        }
        return previous();
    }

    private boolean atEnd()
    {
        return peek().type == TokenType.END;
    }
    
    private Token peek()
    {
        return tokens.get(curr);
    }

    private Token previous()
    {
        return tokens.get(curr-1);
    }
}