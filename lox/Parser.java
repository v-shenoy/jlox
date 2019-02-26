package lox;

import java.util.List;
import java.util.ArrayList;

class Parser
{
    private final List<Token> tokens;
    private int curr = 0;

    private static class ParseError extends RuntimeException {}

    Parser(List<Token> tokens)  
    {
        this.tokens = tokens;
    }

    List<Stmt> parse()
    {
        try
        {
            List<Stmt> statements = new ArrayList<>();
            while(!atEnd()) 
            {                      
                statements.add(declaration());
            }
            return statements;
        }
        catch(ParseError error)
        {
            return null;
        }
    }

    private Stmt declaration()
    {
        try
        {
            if(match(TokenType.LET))
            {
                return varDeclaration();
            }
            return statement();
        }
        catch(ParseError error)
        {
            sync();
            return null;
        }
    }

    private Stmt varDeclaration()
    {
        Token name = consume(TokenType.ID, "Expect variable name");
        Expr initializer = null;
        if(match(TokenType.ASSIGN))
        {
            initializer = expression(); 
        }
        consume(TokenType.SEMI_COLON, "Expect ';' after variable declaration.");
        return new Stmt.Let(name, initializer); 
    }

    private Stmt statement()
    {
        if(match(TokenType.PRINT))
        {
            return printStmt();
        }
        if(match(TokenType.LBRACE))
        {
            return new Stmt.Block(block());
        }
        return expressionStmt();
    }

    private List<Stmt> block()
    {
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !atEnd()) 
        {     
            statements.add(declaration());                
        }                                               
        consume(TokenType.RBRACE, "Expect '}' after block.");
        return statements;
    }

    private Stmt printStmt()
    {
        Expr val = expression();
        consume(TokenType.SEMI_COLON, "Expect ';' at end of print statement.");
        return new Stmt.Print(val);
    }

    private Stmt expressionStmt()
    {
        Expr val = expression();
        consume(TokenType.SEMI_COLON, "Expect ';' at end of expression.");
        return new Stmt.Expression(val);
    }

    private Expr expression()
    {
        return assignment();
    }

    private Expr assignment()
    {
        
        Expr expr = equality();
        if(match(TokenType.ASSIGN))
        {
            Token equals = previous();
            Expr value = assignment();
            if(expr instanceof Expr.Variable)
            {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
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
        else if(match(TokenType.ID))
        {
            return new Expr.Variable(previous());
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