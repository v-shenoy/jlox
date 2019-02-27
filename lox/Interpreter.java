package lox;

import java.util.List;   

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>
{
    private Environment environment = new Environment();

    public void interpret(List<Stmt> stmts)
    {
        try
        {
            for(Stmt stmt : stmts)
            {
                execute(stmt);
            }
        }
        catch(RuntimeError error)
        {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Void visitExprStmt(Stmt.Expression stmt)
    {
        evaluate(stmt.expr);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt)
    {
        Object val = evaluate(stmt.expr);
        System.out.println(stringify(val));
        return null;
    }

    @Override                                     
    public Void visitLetStmt(Stmt.Let stmt) 
    {     
        Object value = null;                        
        if (stmt.initializer != null) {             
            value = evaluate(stmt.initializer);       
        }
        environment.define(stmt.name, stmt.name.lexeme, value);
        return null;                                
    }
    
    @Override                                                     
    public Void visitBlockStmt(Stmt.Block stmt) 
    {                 
        executeBlock(stmt.statements, new Environment(environment));
        return null;                                                
    }

    private void executeBlock(List<Stmt> stmts, Environment env)
    {
        Environment previous = this.environment;
        try
        {
            this.environment = env;
            for(Stmt stmt : stmts)
            {
                execute(stmt);
            }
        }
        finally
        {
            this.environment = previous;
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
            case OR:
                return isTruthy(left) || isTruthy(right);
            case AND:
                return isTruthy(left) && isTruthy(right);
            case BIT_AND:
                if(isInteger(left) && isInteger(right))
                {
                    Double l = (Double)left;
                    Double r = (Double)right;
                    int result = l.intValue() & r.intValue();
                    return (double)result;
                }
                throw new RuntimeError(expr.op, "Operand must be integers");
            case BIT_XOR:
                if(isInteger(left) && isInteger(right))
                {
                    Double l = (Double)left;
                    Double r = (Double)right;
                    int result = l.intValue() ^ r.intValue();
                    return (double)result;
                }
                throw new RuntimeError(expr.op, "Operand must be integers");
            case BIT_OR:
                if(isInteger(left) && isInteger(right))
                {
                    Double l = (Double)left;
                    Double r = (Double)right;
                    int result = l.intValue() | r.intValue();
                    return (double)result;
                }
                throw new RuntimeError(expr.op, "Operand must be integers");
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
            case BIT_NOT:
                if(isInteger(right))
                {
                    Double val = (Double)right;
                    return (double)(~val.intValue());
                }
            throw new RuntimeError(expr.op, "Operand must be an integer");
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

    @Override                                            
    public Object visitVarExpr(Expr.Variable expr) 
    {
        return environment.get(expr.name);                 
    }

    private Object evaluate(Expr expr)
    {
        return  expr.accept(this);
    }

    @Override                                        
    public Object visitAssignExpr(Expr.Assign expr) 
    {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);          
        return value;                                  
    }

    private void execute(Stmt stmt) 
    {
        stmt.accept(this);             
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

    private boolean isInteger(Object object)
    {
        if(object instanceof Double)
        {
            double val = (double)object;
            return !Double.isInfinite(val) && (Math.floor(val) == val);
        }
        return false;
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