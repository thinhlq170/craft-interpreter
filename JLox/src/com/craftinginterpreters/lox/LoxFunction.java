package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
	
	private final Stmt.Function declaration;
	private final Environment closure;
	private final boolean isInitializer;
	
	public LoxFunction(final Stmt.Function declaration, final Environment closure, final boolean isInitializer) {
		this.isInitializer = isInitializer;
		this.declaration = declaration;
		this.closure = closure;
	}
	
	public LoxFunction bind(final LoxInstance instance) {
		final Environment environment = new Environment(closure);
		environment.define("this", instance);
		return new LoxFunction(declaration, environment, isInitializer);
	}

	@Override
	public int arity() {
		return declaration.params.size();
	}
	
	@Override
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}

	@Override
	public Object call(final Interpreter interpreter, final List<Object> arguments) {
		final Environment environment = new Environment(closure);
		
		for (int i = 0; i < declaration.params.size(); i++) {
			environment.define(declaration.params.get(i).lexeme, arguments.get(i));
		}
		
		try {
			interpreter.executeBlock(declaration.body, environment);
		} catch (final Return returnValue) {
			if (isInitializer) return closure.getAt(0, "this");
			return returnValue.value;
		}
		
		if (isInitializer) return closure.getAt(0, "this");
		return null;
	}

	public Environment getClosure() {
		return closure;
	}
}
