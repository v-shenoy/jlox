package lox;

class Printer implements Expr.Visitor<String>
{
    String print(Expr expr)
    {
        return expr.accept(this);
    }

    @Override
    public String visitBinary(Expr.Binary expr)
    {
        return paranthesize(expr.op.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitUnary(Expr.Unary expr)
    {
        return paranthesize(expr.op.lexeme, expr.right);
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
        return paranthesize("group", expr.expression);
    }

    private String paranthesize(String name, Expr... exprs)
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
}