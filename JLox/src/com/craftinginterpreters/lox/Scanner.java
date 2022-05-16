/**
 * 
 */
package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Admin
 *
 */
public class Scanner {
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private static final Map<String, TokenType> keyworks;
	
	static {
		keyworks = new HashMap<String, TokenType>();
		keyworks.put("and", TokenType.AND);
		keyworks.put("class", TokenType.CLASS);
		keyworks.put("else", TokenType.ELSE);
		keyworks.put("false", TokenType.FALSE);
		keyworks.put("for", TokenType.FOR);
		keyworks.put("if", TokenType.IF);
		keyworks.put("nil", TokenType.NIL);
		keyworks.put("or", TokenType.OR);
		keyworks.put("print", TokenType.PRINT);
		keyworks.put("return", TokenType.RETURN);
		keyworks.put("super", TokenType.SUPER);
		keyworks.put("this", TokenType.THIS);
		keyworks.put("true", TokenType.TRUE);
		keyworks.put("var", TokenType.VAR);
		keyworks.put("while", TokenType.WHILE);
		keyworks.put("break", TokenType.BREAK);
		keyworks.put("fun", TokenType.FUN);
	}

	private int start = 0;
	private int current = 0;
	private int line = 1;

	public Scanner(final String source) {
		this.source = source;
	}

	public List<Token> scanTokens() {
		while (!isAtEnd()) {
			// We are at beginning of the next lexeme.
			start = current;
			scanToken();
		}
		tokens.add(new Token(TokenType.EOF, "", null, line));
		return tokens;
	}

	private boolean isAtEnd() {
		return current >= source.length();
	}

	private void scanToken() {

		final char c = advance();
		switch (c) {
			case '(': addToken(TokenType.LEFT_PAREN); break;
			case ')': addToken(TokenType.RIGHT_PAREN); break;
			case '{': addToken(TokenType.LEFT_BRACE); break;
			case '}': addToken(TokenType.RIGHT_BRACE); break;
			case ',': addToken(TokenType.COMMA); break;
			case '.': addToken(TokenType.DOT); break;
			case '-': addToken(TokenType.MINUS); break;
			case '+': addToken(TokenType.PLUS); break;
			case ';': addToken(TokenType.SEMICOLON); break;
			case '*': addToken(TokenType.STAR); break;
			case '!':
				addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
				break;
			case '=':
				addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
				break;
			case '<':
				addToken(match('=') ? TokenType.LESS_EQUAL: TokenType.LESS);
				break;
			case '>':
				addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
				break;
			case '/':
				if (match('/')) {
					// A comment goes until the end of the line.
					while (peek() != '\n' && !isAtEnd()) advance();
				} else if (match('*')) {
					// A comment goes until the end comment is */
					// Make sure that after the comment block we still keep scanning
					while (!(peek() == '*' && peekNext() == '/')) {
						advance();
					}
					advance();
					advance();
				} else {
					addToken(TokenType.SLASH);
				}
				break;
			case ' ':
			case '\r':
			case '\t':
				// ignore whitespace
				break;
			case '\n':
				line++;
				break;
			case '"': string(); break;
			case 'o':
				if (match('r')) {
					addToken(TokenType.OR);
				}
				break;
			default:
				if (isDigit(c)) {
					number();
				} else if (isAlpha(c)) {
					identifier();
				} else {
					Lox.error(line, "Unexpected character.");
				}
				break;
		}
	}
	
	private char advance() {
		return source.charAt(current++);
	}
	
	private void addToken(final TokenType type) {
		addToken(type, null);
	}
	
	private void addToken(final TokenType type, final Object literal) {
		final String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}
	
	private boolean match(final char expected) {
		if (isAtEnd()) return false;
		if (source.charAt(current) != expected) return false;
		
		current++;
		return true;
	}
	
	private char peek() {
		if (isAtEnd()) return '\0';
		return source.charAt(current);
	}
	
	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			if (peek() == '\n') line++;
			advance();
		}
		
		if (isAtEnd()) {
			Lox.error(line, "Unterminated string.");
			return;
		}
		
		// The closing ".
		advance();
		
		// Trim the surrounding quotes.
		final String value = source.substring(start + 1, current - 1);
		addToken(TokenType.STRING, value);
	}
	
	private boolean isDigit(final char c) {
		return c >= '0' && c <= '9';
	}
	
	private void number() {
		while (isDigit(peek())) advance();
		
		// Look for a fraction part
		if (peek() == '.' && isDigit(peekNext())) {
			// Consume the '.'
			advance();
			
			while (isDigit(peek())) advance();
		}
		
		addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
	}
	
	private char peekNext() {
		if (current + 1 >= source.length()) return '\0';
		return source.charAt(current + 1);
	}
	
	private void identifier() {
		while (isAlphaNumeric(peek())) advance();
		
		final String text = source.substring(start, current);
		TokenType type = keyworks.get(text);
		if (type == null) type = TokenType.IDENTIFIER;
		
		addToken(type);
	}
	
	private boolean isAlpha(final char c) {
		return (c >= 'a' && c <= 'z') ||
				(c >= 'A' && c <= 'Z') ||
				c == '_';
	}
	
	private boolean isAlphaNumeric(final char c) {
		return isAlpha(c) || isDigit(c);
	}
}
