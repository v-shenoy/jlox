package lox;

import java.io.BufferedReader;                               
import java.io.IOException;                                  
import java.io.InputStreamReader;                            
import java.nio.charset.Charset;                             
import java.nio.file.Files;                                  
import java.nio.file.Paths;                                  
import java.util.List;

public class Lox
{
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String args[]) throws IOException
    {
        if(args.length > 1)
        {
            System.out.println("Usage: ./lox [path-to-file]");
            System.exit(64);
        }
        else if(args.length == 1)
        {
            runFile(args[0]);
        }
        else
        {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException
    {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if(hadError)
        {
            System.exit(65);
        }
        if(hadRuntimeError)
        {
            System.exit(70);    
        }
    }

    private static void runPrompt() throws IOException
    {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(input);

        while(true)
        {
            System.out.print("lox> ");
            run(br.readLine());
            hadError = false;
            hadRuntimeError = false;
        }
    }

    private static void run(String source)
    {
        Tokenizer tokenizer = new Tokenizer(source);
        tokenizer.scanTokens();
        List<Token> tokens = tokenizer.getTokens();

        if(!hadError)
        {
            Parser parser = new Parser(tokens);
            Expr expr = parser.parse();
            interpreter.interpret(expr);
        }
    }

    static void error(int line, int col, String message)
    {
        report(line, col, "", message); 
    }

    private static void report(int line, int col, String where, String message)
    {
        System.err.println("[line " + line +", col " + col + "] Error" + where + ":" + message);
        hadError = true;
    }

    static void error(Token token, String message)
    {
        if(token.type == TokenType.END)
        {
            report(token.line, token.col, " at end", message);
        }
        else
        {
            report(token.line, token.col, " at ''" + token.lexeme + "''", message);
        }
    }

    public static void runtimeError(RuntimeError error) 
    {
        System.err.println(error.getMessage() + ": [line " + error.token.line + " col " + error.token.col + "]");   
        hadRuntimeError = true;                     
    }
}