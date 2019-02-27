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
        if(match(TokenType.IF))
        {
            return ifStatement();
        }
        if(match(TokenType.WHILE))
        {
            return whileStatement();
        }
        if(match(TokenType.DO))
        {
            return doWhileStmt();
        }
        if(match(TokenType.FOR))
        {
            return forStmt();
        }
        return expressionStmt();
    }

    private Stmt forStmt()
    {
        consume(TokenType.LPAREN, "Expect '(' after for.");
        Expr init = null;
        if(!match(TokenType.SEMI_COLON))
        {
            init = expression();
            consume(TokenType.SEMI_COLON, "Expect ';' after initializer.");
        }
        Expr cond = null;
        if(!match(TokenType.SEMI_COLON))
        {
            cond = expression();
            consume(TokenType.SEMI_COLON, "Expect ';' after condition.");
        }
        Expr incr = null;
        if(!match(TokenType.RPAREN))
        {
            incr = expression();
            consume(TokenType.RPAREN, "Expect ')' after for expression.");
        }
        Stmt body = statement();
        return new Stmt.For(init, cond, incr, body);
    }

    private Stmt doWhileStmt()
    {
        Stmt body = statement();
        consume(TokenType.WHILE, "Expect 'while' after do block.");
        consume(TokenType.LPAREN, "Expect '(' after while.");
        Expr cond = expression();
        consume(TokenType.RPAREN, "Expect ')' after while condition.");
        consume(TokenType.SEMI_COLON, "Expect ';' after do-while.");
        return new Stmt.DoWhile(cond, body);
    }

    private Stmt whileStatement()
    {
        consume(TokenType.LPAREN, "Expect '(' after while.");
        Expr cond = expression();
        consume(TokenType.RPAREN, "Expect ')' after while condition.");
        Stmt body = statement();
        return new Stmt.While(cond, body);
    }

    private Stmt ifStatement()
    {
        consume(TokenType.LPAREN, "Expect '(' after if.");
        Expr cond = expression();
        consume(TokenType.RPAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if(match(TokenType.ELSE))
        {
            elseBranch = statement();
        }
        return new Stmt.If(cond, thenBranch, elseBranch);
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
        
        Expr expr = logicalOr();
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

    private Expr logicalOr()
    {
        Expr left = logicalAnd();
        while(match(TokenType.OR))
        {
            Token op = previous();
            Expr right = logicalAnd();
            left = new Expr.Logical(left, op, right);
        }
        return left;
    }

    private Expr logicalAnd()
    {
        Expr left = bitwiseOr();
        while(match(TokenType.AND))
        {
            Token op = previous();
            Expr right = bitwiseOr();
            left = new Expr.Logical(left, op, right);
        }
        return left;
    }

    private Expr bitwiseOr()
    {
        Expr left = bitwiseXor();
        while(match(TokenType.BIT_OR))
        {
            Token op = previous();
            Expr right = bitwiseOr();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr bitwiseXor()
    {
        Expr left = bitwiseAnd();
        while(match(TokenType.BIT_XOR))
        {
            Token op = previous();
            Expr right = bitwiseAnd();
            left = new Expr.Binary(left, op, right);
        }
        return left;
    }

    private Expr bitwiseAnd()
    {
        Expr left = equality();
        while(match(TokenType.BIT_AND))
        {
            Token op = previous();
            Expr right = equality();
            left = new Expr.Binary(left, op, right);
        }
        return left;
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
        if(match(TokenType.BIT_NOT, TokenType.NOT, TokenType.MINUS))
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