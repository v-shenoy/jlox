package lox;

import java.util.Map;
import java.util.HashMap;

class Environment
{
    private final Map<String, Object> values = new HashMap<>();
    final Environment enclosing;


    Environment() 
    {                     
        enclosing = null;                 
    }

    Environment(Environment enclosing)
    {
        this.enclosing = enclosing;
    }

    void define(Token varToken, String name, Object value)
    {
        if(values.containsKey(name))
        {
            throw new RuntimeError(varToken, "Variable '" + name + "' already exists");
        }
        values.put(name, value);
    }

    void assign(Token name, Object value)
    {
        if(values.containsKey(name.lexeme))
        {
            values.put(name.lexeme, value);
            return;
        }
        if (enclosing != null) 
        {         
            enclosing.assign(name, value); 
            return;                        
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'");
    }

    Object get(Token name)
    {
        if(values.containsKey(name.lexeme))
        {
            return values.get(name.lexeme);
        }
        if(enclosing != null) 
        {
            return enclosing.get(name);
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}