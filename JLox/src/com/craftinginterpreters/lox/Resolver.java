package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import com.craftinginterpreters.lox.Expr.Assign;
import com.craftinginterpreters.lox.Expr.Binary;
import com.craftinginterpreters.lox.Expr.Call;
import com.craftinginterpreters.lox.Expr.Get;
import com.craftinginterpreters.lox.Expr.Grouping;
import com.craftinginterpreters.lox.Expr.Literal;
import com.craftinginterpreters.lox.Expr.Logical;
import com.craftinginterpreters.lox.Expr.Set;
import com.craftinginterpreters.lox.Expr.Super;
import com.craftinginterpreters.lox.Expr.This;
import com.craftinginterpreters.lox.Expr.Unary;
import com.craftinginterpreters.lox.Expr.Variable;
import com.craftinginterpreters.lox.Stmt.Block;
import com.craftinginterpreters.lox.Stmt.Class;
import com.craftinginterpreters.lox.Stmt.Expression;
import com.craftinginterpreters.lox.Stmt.Function;
import com.craftinginterpreters.lox.Stmt.If;
import com.craftinginterpreters.lox.Stmt.Print;
import com.craftinginterpreters.lox.Stmt.Return;
import com.craftinginterpreters.lox.Stmt.Var;
import com.craftinginterpreters.lox.Stmt.While;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
	
	private Interpreter interpreter;
	private final Stack<Map<String, Boolean>> scopes = new Stack<>();
	private FunctionType currentFunction = FunctionType.NONE;
	private final Map<String, Token> unusedVariables = new HashMap<>();
	
	public Resolver(final Interpreter interpreter) {
		this.interpreter = interpreter;
	}
	
	private enum FunctionType {
		NONE,
		FUNCTION,
		INITIALIZER,
		METHOD
	}
	
	private enum ClassType {
		NONE,
		CLASS,
		SUBCLASS
	}
	
	private ClassType currentClass = ClassType.NONE;
	
	public void resolve(final List<Stmt> statements) {
		statements.forEach(statement -> {
			resolve(statement);
		});
	}
	
	private void resolveFunction(final Stmt.Function function, final FunctionType functionType) {
		final FunctionType enclosingFunction = currentFunction;
		currentFunction = functionType;
		beginScope();
		function.params.forEach(param -> {
			declare(param);
			define(param);
		});
		resolve(function.body);
		endScope();
		currentFunction = enclosingFunction;
	}

	@Override
	public Void visitBlockStmt(Block stmt) {
		beginScope();
		resolve(stmt.statements);
		for (final Entry<String, Token> entry : unusedVariables.entrySet()) {
			Lox.error(entry.getValue(), "The variable is never used.");
		}
		endScope();
		return null;
	}
	
	@Override
	public Void visitClassStmt(Class stmt) {
		final ClassType enclosingClass = currentClass;
		currentClass = ClassType.CLASS;
		declare(stmt.name);
		define(stmt.name);
		
		if (stmt.superclass != null && stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
			Lox.error(stmt.superclass.name, "A class can't inherit from itself.");
		}
		
		if (stmt.superclass != null) {
			currentClass = ClassType.SUBCLASS;
			resolve(stmt.superclass);
		}
		
		if (stmt.superclass != null) {
			beginScope();
			scopes.peek().put("super", true);
		}
		
		beginScope();
		scopes.peek().put("this", true);
		
		for (final Stmt.Function method : stmt.methods) {
			FunctionType declaration = FunctionType.METHOD;
			if (method.name.lexeme.equals("init")) {
				declaration = FunctionType.INITIALIZER;
			}
			resolveFunction(method, declaration);
		}
		
		endScope();
		
		if (stmt.superclass != null) {
			endScope();
		}
		currentClass = enclosingClass;
		return null;
	}
	
	private void resolve(final Stmt stmt) {
		stmt.accept(this);
	}
	
	private void resolve(final Expr expr) {
		expr.accept(this);
	}
	
	private void beginScope() {
		scopes.push(new HashMap<String, Boolean>());
	}
	
	private void endScope() {
		scopes.pop();
	}
	
	private void declare(final Token name) {
		if (scopes.isEmpty()) return;
		
		final Map<String, Boolean> scope = scopes.peek();
		if (scope.containsKey(name.lexeme)) {
			Lox.error(name, "Already a variable with this name in this scope.");
		}
		scope.put(name.lexeme, false);
	}
	
	private void define(final Token name) {
		if (scopes.isEmpty()) return;
		scopes.peek().put(name.lexeme, true);
	}
	
	private void resolveLocal(final Expr expr, final Token name) {
		for (int i = scopes.size() - 1; i >= 0; i--) {
			if (scopes.get(i).containsKey(name.lexeme)) {
				interpreter.resolve(expr, scopes.size() - 1 - i);
				return;
			}
		}
	}

	@Override
	public Void visitExpressionStmt(Expression stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitFunctionStmt(Function stmt) {
		declare(stmt.name);
		define(stmt.name);
		
		resolveFunction(stmt, FunctionType.FUNCTION);
		return null;
	}

	@Override
	public Void visitIfStmt(If stmt) {
		resolve(stmt.condition);
		resolve(stmt.thenBranch);
		if (stmt.elseBranch != null) {
			resolve(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Void visitPrintStmt(Print stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitReturnStmt(Return stmt) {
		if (currentFunction == FunctionType.NONE) {
			Lox.error(stmt.keyword, "Can't return from top-level code.");
		}
		if (stmt.value != null) {
			if (currentFunction == FunctionType.INITIALIZER) {
				Lox.error(stmt.keyword, "Can't return a value from an initializer.");
			}
			resolve(stmt.value);
		}
		return null;
	}

	@Override
	public Void visitVarStmt(Var stmt) {
		declare(stmt.name);
		if (stmt.initializer != null) {
			resolve(stmt.initializer);
		}
		define(stmt.name);
		unusedVariables.put(stmt.name.lexeme, stmt.name);
		return null;
	}

	@Override
	public Void visitWhileStmt(While stmt) {
		resolve(stmt.condition);
		resolve(stmt.body);
		return null;
	}

	@Override
	public Void visitAssignExpr(Assign expr) {
		resolve(expr.value);
		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitBinaryExpr(Binary expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitCallExpr(Call expr) {
		resolve(expr.callee);
		
		expr.arguments.forEach(argument -> {
			resolve(argument);
		});
		return null;
	}
	
	@Override
	public Void visitGetExpr(Get expr) {
		resolve(expr.object);
		return null;
	}

	@Override
	public Void visitGroupingExpr(Grouping expr) {
		resolve(expr.expression);
		return null;
	}

	@Override
	public Void visitLiteralExpr(Literal expr) {
		return null;
	}

	@Override
	public Void visitLogicalExpr(Logical expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}
	
	@Override
	public Void visitSetExpr(Set expr) {
		resolve(expr.value);
		resolve(expr.object);
		return null;
	}
	
	@Override
	public Void visitSuperExpr(Super expr) {
		if (currentClass == ClassType.NONE) {
			Lox.error(expr.keyword, "Can't use 'super' outside of a class.");
		} else if (currentClass != ClassType.SUBCLASS) {
			Lox.error(expr.keyword, "Can't use 'super' in a class with no superclass.");
		}
		resolveLocal(expr, expr.keyword);
		return null;
	}
	
	@Override
	public Void visitThisExpr(This expr) {
		if (currentClass == ClassType.NONE) {
			Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
			return null;
		}
		resolveLocal(expr, expr.keyword);
		return null;
	}


	@Override
	public Void visitUnaryExpr(Unary expr) {
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitVariableExpr(Variable expr) {
		if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
			Lox.error(expr.name, "Can't read local variable in its own initializer.");
		}
		
		resolveLocal(expr, expr.name);
		if (unusedVariables.get(expr.name.lexeme) != null) {
			unusedVariables.remove(expr.name.lexeme);
		}
		return null;
	}

	public Interpreter getInterpreter() {
		return interpreter;
	}
}
