package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
	public final Environment enclosing;
	private final Map<String, Object> values = new HashMap<String, Object>();
	
	public Environment() {
		enclosing = null;
	}
	
	public Environment(final Environment enclosing) {
		this.enclosing = enclosing;
	}
	
	public void define(final String name, final Object value) {
		values.put(name, value);
	}
	
	public Environment ancestor(final int distance) {
		Environment environment = this;
		for (int i = 0; i < distance; i++) {
			environment = environment.enclosing;
		}
		return environment;
	}
	
	public Object getAt(final int distance, final String name) {
		return ancestor(distance).values.get(name);
	}
	
	public void assignAt(final int distance, final Token name, final Object value) {
		ancestor(distance).values.put(name.lexeme, value);
	}
	
	public Object get(final Token name) {
		if (values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}
		
		if (enclosing != null) {
			return enclosing.get(name);
		}
		
		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}
	
	public void assign(final Token name, final Object value) {
		if (values.containsKey(name.lexeme)) {
			values.put(name.lexeme, value);
			return;
		}
		
		if (enclosing != null) {
			enclosing.assign(name, value);
			return;
		}
		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}
}
