package lox;

abstract class Expr
{
    Expr left, right, expression;
    Token op;
    Object val;
    abstract <T> T accept(Visitor<T> vis);

    interface Visitor<T>
    {
        T visitBinary(Binary expr);
        T visitUnary(Unary expr);
        T visitLiteral(Literal expr);
        T visitGrouping(Grouping expr); 
        T visitVarExpr(Variable expr);
        T visitAssignExpr(Assign expr);
        T visitLogicalExpr(Logical expr);
        T visitConditionalExpr(Conditional expr);
    }
   
    static class Binary extends Expr
    {
        Binary(Expr left, Token op, Expr right)
        {
            this.left = left;
            this.op = op;
            this.right = right;
        }

        @Override
        <T> T accept(Visitor<T> vis)
        {
           return vis.visitBinary(this);
        }
    }

    static class Unary extends Expr
    {
        Unary(Token op, Expr right)
        {
            this.op = op;
            this.right = right;
        }

        @Override
        <T> T accept(Visitor<T> vis)
        {
            return vis.visitUnary(this);
        }
    }

    static class Literal extends Expr
    {
        Literal(Object val)
        {
            this.val = val;
        }

        @Override
        <T> T accept(Visitor<T> vis)
        {
            return vis.visitLiteral(this);
        }
    }

    static class Grouping extends Expr
    {
        Grouping(Expr expression)
        {
            this.expression = expression;
        }

        @Override
        <T> T accept(Visitor<T> vis)
        {
            return vis.visitGrouping(this);
        }
    }
    
    static class Variable extends Expr 
    {
        final Token name;
    
        Variable(Token name) 
        {
            this.name = name;
        }

        @Override
        <T> T accept(Visitor<T> vis)
        {
            return vis.visitVarExpr(this);
        }
    }

    static class Assign extends Expr 
    {       
        final Token name;                      
        final Expr value;

        Assign(Token name, Expr value) 
        {       
            this.name = name;                    
            this.value = value;                  
        }

        <T> T accept(Visitor<T> visitor) 
        {     
            return visitor.visitAssignExpr(this);
        }                                                            
    } 

    static class Logical extends Expr
    {
        final Token op;
        final Expr left, right;

        Logical(Expr left, Token op, Expr right) 
        {       
            this.left = left;
            this.op = op;    
            this.right = right;                
        }

        <T> T accept(Visitor<T> visitor) 
        {     
            return visitor.visitLogicalExpr(this);
        } 
    }

    static class Conditional extends Expr
    {
        final Expr cond, thenBranch, elseBranch;

        Conditional(Expr cond, Expr thenBranch, Expr elseBranch)
        {
            this.cond = cond;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        <T> T accept(Visitor<T> visitor) 
        {     
            return visitor.visitConditionalExpr(this);
        } 
    }
}
 