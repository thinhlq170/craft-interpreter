package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
	public final String name;
	private Map<String, LoxFunction> methods;
	private LoxClass superClass;

	public LoxClass(final String name, final LoxClass superClass, final Map<String, LoxFunction> methods) {
		this.superClass = superClass;
		this.name = name;
		this.methods = methods;
	}

	public LoxFunction findMethod(final String name) {
		if (methods.containsKey(name)) {
			return methods.get(name);
		}
		return null;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int arity() {
		final LoxFunction initializer = findMethod("init");
		if (initializer == null)
			return 0;
		return initializer.arity();
	}

	@Override
	public Object call(final Interpreter interpreter, final List<Object> arguments) {
		final LoxInstance instance = new LoxInstance(this);
		final LoxFunction initializer = findMethod("init");
		if (initializer != null) {
			initializer.bind(instance).call(interpreter, arguments);
		}
		return instance;
	}

	public Map<String, LoxFunction> getMethods() {
		return methods;
	}

	public LoxClass getSuperClass() {
		return superClass;
	}
}
