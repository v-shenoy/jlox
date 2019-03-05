package lox;

import java.util.List;

class LoxFunction implements LoxCallable
{
    private final Stmt.Function decl;
    private final Environment closure;
    private final boolean isInitializer;

    LoxFunction(Stmt.Function decl, Environment closure, boolean isInitializer)
    {
        this.decl = decl;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    @Override
    public int arity()
    {
        return decl.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args)
    {
        Environment env = new Environment(closure);
        for(int i=0;i<decl.params.size();i++)
        {
            env.define(decl.params.get(i).lexeme, args.get(i));
        }
        try
        {
            interpreter.executeBlock(decl.body, env);
        }
        catch(Return returnExpr)
        {
            if(isInitializer)
            {
                return closure.getAt(0, "self");
            }
            return returnExpr.value;
        }
        if(isInitializer)
        {
            return closure.getAt(0, "self");
        }
        return null;
    }

    public LoxFunction bind(LoxInstance instance)
    {
        Environment env = new Environment(closure);
        env.define("self", instance);
        return new LoxFunction(decl, env, isInitializer);
    }

    @Override                                       
    public String toString()
     {                      
        return "<fn " + decl.name.lexeme + ">";
    }
}