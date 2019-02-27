package lox;

import java.util.List;

abstract class Stmt
{
    abstract <T> T accept(Visitor<T> vis);

    interface Visitor<T>
    {
        T visitPrintStmt(Print stmt);
        T visitExprStmt(Expression stmt);
        T visitLetStmt(Let stmt);
        T visitBlockStmt(Block stmt);
        T visitIfStmt(If stmt);
        T visitWhileStmt(While stmt);
        T visitDoWhileStmt(DoWhile stmt);
        T visitForStmt(For stmt);
    }

    static class Expression extends Stmt
    {
        final Expr expr;

        Expression(Expr expr)
        {
            this.expr = expr;
        }

        @Override
        <T> T accept(Visitor<T> vis)
        {
           return vis.visitExprStmt(this);
        }
    }

    static class Print extends Stmt
    {
        final Expr expr;

        Print(Expr expr)
        {
            this.expr = expr;
        }

        @Override
        <T> T accept(Visitor<T> vis)
        {
           return vis.visitPrintStmt(this);
        }
    }
    
    static class Let extends Stmt
    {
        final Token name;
        final Expr initializer;

        Let(Token name, Expr initializer)
        {
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        <T> T accept(Visitor<T> vis)
        {
           return vis.visitLetStmt(this);
        }
    }

    static class Block extends Stmt 
    {
        final List<Stmt> statements;

        Block(List<Stmt> statements) 
        {
            this.statements = statements;
        }
    
        <T> T accept(Visitor<T> vis) 
        {
            return vis.visitBlockStmt(this);
        }
    }

    static class If extends Stmt
    {
        final Expr cond;
        final Stmt thenBranch;
        final Stmt elseBranch;

        If(Expr cond, Stmt thenBranch, Stmt elseBranch)
        {
            this.cond = cond;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        <T> T accept(Visitor<T> vis)
        {
            return vis.visitIfStmt(this);
        }
    }

    static class While extends Stmt
    {
        final Expr cond;
        final Stmt body;

        While(Expr cond, Stmt body)
        {
            this.cond = cond;
            this.body = body;
        }

        <T> T accept(Visitor<T> vis)
        {
            return vis.visitWhileStmt(this);
        }
    }

    static class DoWhile extends Stmt
    {
        final Expr cond;
        final Stmt body;

        DoWhile(Expr cond, Stmt body)
        {
            this.cond = cond;
            this.body = body;
        }

        <T> T accept(Visitor<T> vis)
        {
            return vis.visitDoWhileStmt(this);
        }
    }

    static class For extends Stmt
    {
        final Expr init, cond, incr;
        final Stmt body;

        For(Expr init, Expr cond, Expr incr, Stmt body)
        {
            this.init = init;
            this.cond = cond;
            this.incr = incr;
            this.body = body;
        }

        <T> T accept(Visitor<T> vis)
        {
            return vis.visitForStmt(this);
        }
    }
}