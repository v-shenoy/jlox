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
}