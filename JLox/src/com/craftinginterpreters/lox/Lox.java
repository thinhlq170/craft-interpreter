package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 
 */

/**
 * @author Admin
 *
 */
public class Lox {
	private static final int APPLICATION_ERROR = 65;
	private static final int ILLEGAL_ARGUMENT_ERROR = 64;
	private static final int APPLICATION_RUNTIME_ERROR = 70;

	public static boolean hadError = false;
	public static boolean hadRuntimeError = false;

	private static final Interpreter interpreter = new Interpreter();

	public static void main(final String[] args) throws IOException {
		if (args.length > 1) {
			System.out.println("Usage: jlox [script]");
			System.exit(ILLEGAL_ARGUMENT_ERROR);
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPromt();
		}
	}

	private static void runFile(final String path) throws IOException {
		final byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));
		if (hadError)
			System.exit(APPLICATION_ERROR);
		if (hadRuntimeError)
			System.exit(APPLICATION_RUNTIME_ERROR);
	}

	private static void runPromt() throws IOException {
		try (final InputStreamReader input = new InputStreamReader(System.in);
				final BufferedReader reader = new BufferedReader(input)) {
			for (;;) {
				System.out.print("> ");
				final String line = reader.readLine();
				if (line == null)
					break;
				run(line);
				hadError = false;
			}
		}
//		final InputStreamReader input = new InputStreamReader(System.in);
//		final BufferedReader reader = new BufferedReader(input);
//		for (;;) {
//			System.out.print("> ");
//			final String line = reader.readLine();
//			if (line == null) break;
//			run(line);
//			hadError = false;
//		}
//		input.close();
//		reader.close();
	}

	private static void run(final String source) {

		if (hadError == true)
			System.exit(APPLICATION_ERROR);

		final Scanner scanner = new Scanner(source);
		final List<Token> tokens = scanner.scanTokens();
		final Parser parser = new Parser(tokens);
		final List<Stmt> statements = parser.parse();

		if (hadError)
			return;
		
		final Resolver resolver = new Resolver(interpreter);
		resolver.resolve(statements);
		
		// Stop if there was a resolution error
		if (hadError)
			return;
		
		interpreter.interpret(statements);
	}

	public static void error(final int line, final String message) {
		report(line, "", message);
	}

	private static void report(final int line, final String where, final String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}

	public static void error(final Token token, final String message) {
		if (token.type == TokenType.EOF) {
			report(token.line, " at end", message);
		} else {
			report(token.line, " at '" + token.lexeme + "'", message);
		}
	}

	public static void runtimeError(final RuntimeError error) {
		System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
		hadRuntimeError = true;
	}
}
