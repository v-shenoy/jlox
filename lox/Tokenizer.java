package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Tokenizer
{
    private final String source;
    private final List<Token> tokens;
    private int line, col, begin, curr;
    private static final Map<String, TokenType> keywords;

    static
    {
        keywords = new HashMap<>();  
        keywords.put("not", TokenType.NOT);                      
        keywords.put("and", TokenType.AND);
        keywords.put("or", TokenType.OR);      
        keywords.put("if", TokenType.IF);      
        keywords.put("else", TokenType.ELSE);                                                          
        keywords.put("true", TokenType.TRUE);                                           
        keywords.put("false", TokenType.FALSE);
        keywords.put("let", TokenType.LET);                                            
        keywords.put("for", TokenType.FOR);                       
        keywords.put("do", TokenType.DO);
        keywords.put("while", TokenType.WHILE);
        keywords.put("nil", TokenType.NIL);                       
        keywords.put("define", TokenType.DEFINE);                       
        keywords.put("break", TokenType.BREAK);                       
        keywords.put("continue", TokenType.CONTINUE);                       
        keywords.put("print", TokenType.PRINT);                     
        keywords.put("return", TokenType.RETURN);
        keywords.put("class", TokenType.CLASS);                    
        keywords.put("self", TokenType.SELF);                      
    }
    
    Tokenizer(String source)
    {
        this.source = source;
        this.tokens = new ArrayList<Token>();
        this.begin = this.curr = this.col = 0;
        this.line = 1;
    }

    List<Token> getTokens()
    {
        return tokens;
    }

    public void scanTokens()
    {
        while(!atEnd())
        {
            begin = curr;
            nextToken();
        }
        tokens.add(new Token(TokenType.END, null, "EOF", line, col+1));
    }

    private void nextToken()
    {
        char c = consume();
        switch(c)
        {
            case '(':
                addToken(TokenType.LPAREN);
                break;
            case ')':
                addToken(TokenType.RPAREN);
                break;
            case ';':
                addToken(TokenType.SEMI_COLON);
                break;
            case '{':
                addToken(TokenType.LBRACE);
                break;
            case '}':
                addToken(TokenType.RBRACE);
                break;
            case '+':
                addToken(TokenType.PLUS);
                break;
            case '-':
                addToken(TokenType.MINUS);
                break;
            case '/':
                if(match('*'))
                {
                    handleMultiComments();
                }
                else
                {
                    addToken(TokenType.DIV);
                }
                break;
            case '%':
                addToken(TokenType.MOD);
                break;
            case '=':
                addToken(TokenType.EQUALS);
                break;
            case '~':
                addToken(TokenType.BIT_NOT);
                break;
            case '&':
                addToken(TokenType.BIT_AND);
                break;
            case '|':
                addToken(TokenType.BIT_OR);
                break;
            case '^':
                addToken(TokenType.BIT_XOR);
                break;
            case '*':
                if(match('*'))
                {
                    addToken(TokenType.EXP);
                }
                else
                {
                    addToken(TokenType.MUL);
                }
                break;
            case ':':
                if(match('='))
                {
                    addToken(TokenType.ASSIGN);
                }
                else
                {
                    Lox.error(line, col, "Unexpected char");
                }
                break;
            case '!':
                if(match('='))
                {
                    addToken(TokenType.NOT_EQUALS);
                }
                else
                {
                    Lox.error(line, col,"Unexpected char");
                }
                break;
            case '>':
                if(match('='))
                {
                    addToken(TokenType.GREATER_EQUALS);
                }
                else
                {
                    addToken(TokenType.GREATER);
                }
                break;
            case '<':
                if(match('='))
                {
                    addToken(TokenType.LESSER_EQUALS);
                }
                else
                {
                    addToken(TokenType.LESSER);
                }
                break;
            case '#':
                handleComments();
                break;
            case '"':
                handleStrings();
                break;
            case ' ':
            case '\t':
            case '\r':
                break;
            case '\n':
                line++;
                col = 0;
                break;
            default:
                if(Character.isDigit(c))
                {
                    handleNumber();
                }
                else if(Character.isLetter(c) || c == '_')
                {
                    handleIdentifier();
                }
                else
                {
                    Lox.error(line, col, "Unknown symbol");
                }
        }
    }

    private void handleMultiComments()
    {
        while(true)
        {
            if(atEnd())
            {
                Lox.error(line, col, "unterminated  comment"); 
                return;              
            }
            if(peek() == '/')
            {
                consume();
                if(match('*'))
                {
                    handleMultiComments();
                }
            }
            else if(peek() == '*')
            {
                consume();
                if(match('/'))
                {
                    return;
                }
            }
            else if(peek() == '\n')
            {
                line++;
                col = 0;
            }
            if(!atEnd())
            {
                consume();
            }
        }
    }

    private void handleComments()
    {
        while(!atEnd())
        {
            if(peek() == '\n')
            {
                break;
            }
            consume();
        }
    }

    private void handleIdentifier()
    {
        while(Character.isLetterOrDigit(peek()) || peek() == '_')
        {
            consume();
        }
        String lexeme = source.substring(begin, curr);
        TokenType type = keywords.get(lexeme); 
        if(type == null)
        {
            addToken(TokenType.ID, lexeme);
        }
        else
        {
            addToken(type);
        }
    }

    private void handleStrings()
    {
        while(peek() != '"')
        {
            if(atEnd() || peek() == '\n')
            {
                Lox.error(line, col, "Unterminated String ");
                break;
            }
            consume();
        }
        consume();
        String lexeme = source.substring(begin+1, curr-1);
        addToken(TokenType.STRING, lexeme);
    }

    private void handleNumber()
    {
        while(Character.isDigit(peek()))
        {
            consume();
        }
        if(peek() == '.' && Character.isDigit(peekNext()))
        {
            consume();
        }
        while(Character.isDigit(peek()))
        {
            consume();
        }
        double val = Double.parseDouble(source.substring(begin, curr));
        addToken(TokenType.NUMBER, val);
    }

    private void addToken(TokenType type) 
    {                
        addToken(type, null);                                
    } 

    private void addToken(TokenType type, Object literal) 
    {
        String lexeme = source.substring(begin, curr);      
        tokens.add(new Token(type, lexeme, literal, line, col));    
    }

    private char peekNext()
    {
        if(curr + 1 >= source.length())
        {
            return '\0';
        }
        return source.charAt(curr+1);
    }

    private char peek()
    {
        if(atEnd())
        {
            return '\0';
        }
        return source.charAt(curr);
    }

    private boolean match(char expected)
    {
        if(atEnd() || source.charAt(curr) != expected)
        {
            return false;
        }
        curr++;
        col++;
        return true;
    }

    private char consume()
    {
        curr++;
        col++;
        return source.charAt(curr-1);
    }

    private boolean atEnd()
    {
        return curr >= source.length();
    }
}