package lox;

import java.util.List;   
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>
{
    public Environment globals = new Environment();
    private Environment environment = globals;
    /* Sentinel value to seperate it from null, since
    null might represent nil. */
    private static Object unitialized = new Object();
    private static Map<Expr, Integer> locals = new HashMap<>();

    Interpreter()
    {
        globals.define("clock", new LoxCallable(){
            @Override
            public int arity()
            {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arg)
            {
                return (double) System.currentTimeMillis()/1000;
            }

            @Override
            public String toString()
            {
                return "<native fn>";
            }
        });
    }

    void resolve(Expr expr, int depth)
    {
        locals.put(expr, depth);
    }

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
    public Void visitBreakStmt(Stmt.Break stmt)
    {
        throw new Jump(JumpType.BREAK);
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt)
    {
        throw new Jump(JumpType.CONTINUE);
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
        Object value = unitialized;                        
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

    public void executeBlock(List<Stmt> stmts, Environment env)
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
    public Void visitIfStmt(Stmt.If stmt)
    {
        if(isTruthy(evaluate(stmt.cond)))
        {
            execute(stmt.thenBranch);
        }
        else if(stmt.elseBranch != null)
        {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt)
    {
        while(isTruthy(evaluate(stmt.cond)))
        {
            try
            {
                execute(stmt.body);
            }
            catch(Jump jump)
            {
                if(jump.type == JumpType.BREAK)
                {
                    break;
                }
                else
                {
                    continue;
                }
            }
        }
        return null;
    }

    @Override
    public Void visitDoWhileStmt(Stmt.DoWhile stmt)
    {
        do
        {
            try
            {
                execute(stmt.body);

            }
            catch(Jump jump)
            {
                if(jump.type == JumpType.BREAK)
                {
                    break;
                }
                else
                {
                    continue;
                }
            }
        } while(isTruthy(evaluate(stmt.cond)));
        return null;
    }

    @Override
    public Void visitForStmt(Stmt.For stmt)
    {
        if(stmt.init != null)
        {
            evaluate(stmt.init);
        }
        while(true)
        {
            if(stmt.cond != null)
            {
                if(!isTruthy(evaluate(stmt.cond)))
                {
                    break;
                }
            }
            try
            {
                execute(stmt.body);
            }  
            catch(Jump jump)
            {
                if(jump.type == JumpType.BREAK)
                {
                    break;
                }
                else
                {
                    if(stmt.incr != null)
                    {
                        evaluate(stmt.incr);
                    }
                    continue;
                }
            } 
            if(stmt.incr != null)
            {
                evaluate(stmt.incr);
            }
        }
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt)
    {
        LoxFunction func = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, func);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt)
    {
        Object value = null;
        if(stmt.expr != null)
        {
            value = evaluate(stmt.expr);
        }        
        throw new Return(value);
    }

    @Override
    public Void visitSwitchStmt(Stmt.Switch stmt)
    {
        Object cond = evaluate(stmt.cond);
        int index = stmt.exprs.indexOf(cond);
        if(index == -1)
        {
            index = stmt.exprs.indexOf("default");
        }
        if(index != -1)
        {   
            try
            {
                for(int i=index;i<stmt.branches.size();i++)
                {
                    execute(stmt.branches.get(i));
                } 
            }
            catch(Jump jump)
            {
                if(jump.type == JumpType.BREAK)
                {
                    return null;
                }
            } 
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr)
    {
        Object left = evaluate(expr.left);
        if(expr.op.type == TokenType.OR)
        {
            if(isTruthy(left))
            {
               return true; 
            }
            return isTruthy(evaluate(expr.right));
        }
        else
        {
            if(!isTruthy(left))
            {
                return false;
            }
            return isTruthy(evaluate(expr.right));
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
            case COMMA:
                return right;
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
        Object value = lookUpVariables(expr.name, expr);     
        if(value == unitialized)
        {
            throw new RuntimeError(expr.name, "Variable must be initialized before use");
        }            
        return value;
    }

    private Object lookUpVariables(Token name, Expr expr)
    {
        Integer distance = locals.get(expr);
        if(distance != null)
        {
            return environment.getAt(distance, name);
        }
        else
        {
            return globals.get(name);
        }
    }

    @Override
    public Object visitCallExpr(Expr.Call expr)
    {
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        if(!(callee instanceof LoxCallable))
        {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }
        for(Expr arg : expr.args)
        {
            arguments.add(evaluate(arg));
        }
        LoxCallable function = (LoxCallable)callee;
        if(arguments.size() != function.arity())
        {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got "
            + arguments.size() + ".");
        }
        return function.call(this, arguments);
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

    @Override
    public Object visitConditionalExpr(Expr.Conditional expr)
    {
        if(isTruthy(evaluate(expr.cond)))
        {
           return evaluate(expr.thenBranch);
        }
        else
        {
            return evaluate(expr.elseBranch);
        }
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