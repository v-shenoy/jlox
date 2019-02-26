package lox;

class Interpreter implements Expr.Visitor<Object>
{
    public void interpret(Expr expr)
    {
        try
        {
            Object val = evaluate(expr);
            System.out.println(stringify(val));
        }
        catch(RuntimeError error)
        {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitBinary(Expr.Binary expr)
    {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.op.type)
        {
            case PLUS:
                if(left instanceof String || right instanceof String)
                {
                    return stringify(left) + stringify(right);
                }
                if(left instanceof Double && right instanceof Double)
                {
                    return (double)left + (double)right;
                }
                throw new RuntimeError(expr.op, "Addition operation not supported for operands.");
            case MINUS:
                checkNumbers(expr.op, left, right);
                return (double)left - (double)right;
            case MUL:
                checkNumbers(expr.op, left, right);
                return (double)left * (double)right;
            case DIV:
                checkNumbers(expr.op, left, right);
                if((double)right == 0)
                {
                    throw new RuntimeError(expr.op, "Cannot divide by zero.");
                }
                return (double)left/(double)right;
            case MOD:
                checkNumbers(expr.op, left, right);
                if((double)right == 0)
                {
                    throw new RuntimeError(expr.op, "Cannot divide by zero.");
                }
                return (double)left%(double)right;
            case EXP:
                checkNumbers(expr.op, left, right);
                return Math.pow((double)left, (double)right);
            case GREATER:
                if(left instanceof Double && right instanceof Double)
                {
                    return (double)left > (double)right;
                }
                if(left instanceof String && right instanceof String)
                {
                    return left.toString().compareTo((String)right) > 0;
                }
                throw new RuntimeError(expr.op, "Comparison not supported for operands.");
            case GREATER_EQUALS:
                if(left instanceof Double && right instanceof Double)
                {
                    return (double)left >= (double)right;
                }
                if(left instanceof String && right instanceof String)
                {
                    return left.toString().compareTo((String)right) >= 0;
                }
                throw new RuntimeError(expr.op, "Comparison not supported for operands.");
            case LESSER:
                if(left instanceof Double && right instanceof Double)
                {
                    return (double)left < (double)right;
                }
                if(left instanceof String && right instanceof String)
                {
                    return left.toString().compareTo((String)right) < 0;
                }
                throw new RuntimeError(expr.op, "Comparison not supported for operands.");
            case LESSER_EQUALS:
                if(left instanceof Double && right instanceof Double)
                {
                    return (double)left <= (double)right;
                }
                if(left instanceof String && right instanceof String)
                {
                    return left.toString().compareTo((String)right) <= 0;                    
                }
                throw new RuntimeError(expr.op, "Comparison not supported for operands.");   
            case EQUALS:
                return isEqual(left, right);
            case NOT_EQUALS:
                return !isEqual(left, right);
        }
        return null;
    }

    @Override
    public Object visitUnary(Expr.Unary expr)
    {
        Object right = evaluate(expr.right);
        switch(expr.op.type)
        {
            case MINUS:
                checkNumber(expr.op, right);
                return -(double)right;
            case NOT:
                return !isTruthy(right);
        }
        return null;
    }

    @Override
    public Object visitLiteral(Expr.Literal expr)
    {
        return expr.val;
    }

    @Override
    public Object visitGrouping(Expr.Grouping expr)
    {
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr)
    {
        return  expr.accept(this);
    }

    private boolean isTruthy(Object object)
    {
        if(object == null) 
        {
            return false;
        }
        if(object instanceof Boolean)
        {
            return (boolean) object;
        }
        return true;
    }

    private boolean isEqual(Object a, Object b)
    {
        if(a == null)
        {
            return b == null;
        }
        return a.equals(b);
    }

    private void checkNumber(Token op, Object object)
    {
        if(object instanceof Double)
        {
            return;
        }
        throw new RuntimeError(op, "Operand must be a number");
    }

    private void checkNumbers(Token op, Object a, Object b)
    {
        if(a instanceof Double && b instanceof Double)
        {
            return;
        }
        throw new RuntimeError(op, "Operand must be numbers");
    }

    private String stringify(Object object)
    {
        if(object == null)
        {
            return "nil";
        }
        if(object instanceof Double)
        {
            String num = object.toString();
            if(num.endsWith(".0"))
            {
                num = num.substring(0, num.length()-2);
            }
            return num;
        }
        return object.toString();
    }
}