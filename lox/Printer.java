package lox;

class Printer implements Expr.Visitor<String>, Stmt.Visitor<String>
{
    String print(Expr expr)
    {
        return expr.accept(this);
    }

    @Override
    public String visitBinary(Expr.Binary expr)
    {
        return parenthesize(expr.op.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitUnary(Expr.Unary expr)
    {
        return parenthesize(expr.op.lexeme, expr.right);
    }

    @Override
    public String visitLiteral(Expr.Literal expr)
    {
        if(expr.val == null)
        {
            return "nil";
        }
        return expr.val.toString();
    }

    @Override
    public String visitGrouping(Expr.Grouping expr)
    {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitVarExpr(Expr.Variable expr) 
    {
        return expr.name.lexeme;
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) 
    {
        return parenthesize2(":=", expr.name.lexeme, expr.value);
    }

    @Override
    public String visitExprStmt(Stmt.Expression stmt) 
    {
        return parenthesize(";", stmt.expr);
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt)
    {
        return parenthesize("print", stmt.expr);
    }

    @Override
    public String visitLetStmt(Stmt.Let stmt)
    {
        if(stmt.initializer == null) 
        {
            return parenthesize2("let", stmt.name);
        }
        return parenthesize2("let", stmt.name, ":=", stmt.initializer); 
    }

    @Override
    public String visitBlockStmt(Stmt.Block stmt) 
    {
        StringBuilder builder = new StringBuilder();
        builder.append("(block ");
        for (Stmt statement : stmt.statements) {
        builder.append(statement.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    private String parenthesize(String name, Expr... exprs)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for(Expr expr : exprs)
        {
                builder.append(" ");
                builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    private String parenthesize2(String name, Object... parts) 
    {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);

        for (Object part : parts) 
        {
            builder.append(" ");
            if (part instanceof Expr) 
            {
                builder.append(((Expr)part).accept(this));
            } 
            else if(part instanceof Stmt) 
            {
                builder.append(((Stmt) part).accept(this));
            } 
            else if (part instanceof Token) 
            {
                builder.append(((Token) part).lexeme);
            }
            else 
            {
                builder.append(part);
            }
        }
        builder.append(")");
        return builder.toString();
    }
}