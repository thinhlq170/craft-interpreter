package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.craftinginterpreters.lox.Expr.Binary;
import com.craftinginterpreters.lox.Expr.Get;
import com.craftinginterpreters.lox.Expr.Grouping;
import com.craftinginterpreters.lox.Expr.Literal;
import com.craftinginterpreters.lox.Expr.Logical;
import com.craftinginterpreters.lox.Expr.Set;
import com.craftinginterpreters.lox.Expr.Super;
import com.craftinginterpreters.lox.Expr.This;
import com.craftinginterpreters.lox.Expr.Unary;
import com.craftinginterpreters.lox.Expr.Variable;
import com.craftinginterpreters.lox.Stmt.Class;
import com.craftinginterpreters.lox.Stmt.Function;
import com.craftinginterpreters.lox.Stmt.If;
import com.craftinginterpreters.lox.Stmt.While;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

	public final Environment globals = new Environment();
	private Environment environment = globals;
	private final Map<Expr, Integer> locals = new HashMap<>();
	
	public Interpreter() {
		globals.define("clock", new LoxCallable() {

			/*
			 * The implementation of call() calls the corresponding Java function and
			 * converts the result to a double value in seconds.
			 */
			@Override
			public Object call(final Interpreter interpreter, final List<Object> arguments) {
				return (double) System.currentTimeMillis() / 1000.0;
			}

			/* The clock() function takes no arguments, so its arity is zero */
			@Override
			public int arity() {
				return 0;
			}

			@Override
			public String toString() {
				return "<native fn>";
			}
		});
	}

	public void interpret(final List<Stmt> statements) {
		try {
			statements.forEach(statement -> {
				execute(statement);
			});

		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}

	@Override
	public Object visitBinaryExpr(final Binary expr) {
		final Object left = evaluate(expr.left);
		final Object right = evaluate(expr.right);

		switch (expr.operator.type) {
		case GREATER:
			checkNumberOperands(expr.operator, left, right);
			return (double) left > (double) right;
		case GREATER_EQUAL:
			checkNumberOperands(expr.operator, left, right);
			return (double) left >= (double) right;
		case LESS:
			checkNumberOperands(expr.operator, left, right);
			return (double) left < (double) right;
		case LESS_EQUAL:
			checkNumberOperands(expr.operator, left, right);
			return (double) left <= (double) right;
		case BANG_EQUAL:
			return !isEqual(left, right);
		case EQUAL_EQUAL:
			return isEqual(left, right);
		case MINUS:
			checkNumberOperands(expr.operator, left, right);
			return (double) left - (double) right;
		case PLUS:
			if (left instanceof Double && right instanceof Double) {
				return (double) left + (double) right;
			}
			if (left instanceof String && right instanceof String) {
				return (String) left + (String) right;
			}
			if (left instanceof Double && right instanceof String) {
				return stringify(left) + (String) right;
			}
			if (left instanceof String && right instanceof Double) {
				return (String) left + stringify(right);
			}
			throw new RuntimeError(expr.operator, "Operand must be two numbers or two strings");
		case SLASH:
			checkNumberOperands(expr.operator, left, right);
			return (double) left / (double) right;
		case STAR:
			checkNumberOperands(expr.operator, left, right);
			return (double) left * (double) right;
		default:
			break;
		}
		// Unreachable.
		return null;
	}
	
	@Override
	public Object visitCallExpr(Expr.Call expr) {
		final Object callee = evaluate(expr.callee);
		
		final List<Object> arguments = new ArrayList<>();
		
		expr.arguments.forEach(argument -> arguments.add(evaluate(argument)));
		
		if (!(callee instanceof LoxCallable)) {
			throw new RuntimeError(expr.paren, "Can only call functions and classes.");
		}
		
		final LoxCallable function = (LoxCallable)callee;
		
		if (arguments.size() != function.arity()) {
			throw new RuntimeError(expr.paren, "Expected "
					+ function.arity() + " arguments but got " +
					arguments.size() + ".");
		}
		
		return function.call(this, arguments);
	}
	
	@Override
	public Object visitGetExpr(Get expr) {
		final Object object = evaluate(expr.object);
		if (object instanceof LoxInstance) {
			return ((LoxInstance) object).get(expr.name);
		}
		throw new RuntimeError(expr.name, "Only instances have properties.");
	}

	@Override
	public Object visitGroupingExpr(final Grouping expr) {
		return evaluate(expr.expression);
	}

	@Override
	public Object visitLiteralExpr(final Literal expr) {
		return expr.value;
	}
	
	@Override
	public Object visitLogicalExpr(final Logical expr) {
		final Object left = evaluate(expr.left);
		
		if (expr.operator.type == TokenType.OR) {
			if (isTruthy(left)) return left;
		} else {
			if (!isTruthy(left)) return left;
		}
		return evaluate(expr.right);
	}
	
	@Override
	public Object visitSetExpr(Set expr) {
		final Object object = evaluate(expr.object);
		
		if (!(object instanceof LoxInstance)) {
			throw new RuntimeError(expr.name, "Only instances have fields.");
		}
		
		final Object value = evaluate(expr.value);
		((LoxInstance) object).set(expr.name, value);
		return value;
	}
	
	@Override
	public Object visitSuperExpr(Super expr) {
		final int distance = locals.get(expr);
		final LoxClass superclass = (LoxClass)environment.getAt(distance, "super");
		final LoxInstance object = (LoxInstance) environment.getAt(distance - 1, "this");
		final LoxFunction method = superclass.findMethod(expr.method.lexeme);
		if (method == null) {
			throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
		}
		return method.bind(object);
	}
	
	@Override
	public Object visitThisExpr(This expr) {
		return lookUpVariable(expr.keyword, expr);
	}

	@Override
	public Object visitUnaryExpr(final Unary expr) {
		final Object right = evaluate(expr.right);

		switch (expr.operator.type) {
		case MINUS:
			checkNumberOperand(expr.operator, right);
			return -(double) right;
		case BANG:
			return !isTruthy(right);
		default:
			break;
		}
		// Unreachable.
		return null;
	}

	@Override
	public Object visitVariableExpr(final Expr.Variable expr) {
		return lookUpVariable(expr.name, expr);
	}
	
	private Object lookUpVariable(final Token name, final Expr expr) {
		final Integer distance = locals.get(expr);
		if (distance != null) {
			return environment.getAt(distance, name.lexeme);
		}
		return globals.get(name);
	}

	private void checkNumberOperand(final Token operator, final Object operand) {
		if (operand instanceof Double)
			return;
		throw new RuntimeError(operator, "Operand must be a number.");
	}

	private void checkNumberOperands(final Token operator, final Object left, final Object right) {
		if (left instanceof Double && right instanceof Double)
			return;
		throw new RuntimeError(operator, "Operand must be numbers");
	}

	private Object evaluate(final Expr expr) {
		return expr.accept(this);
	}

	private void execute(final Stmt stmt) {
		stmt.accept(this);
	}
	
	public void resolve(final Expr expr, final int depth) {
		locals.put(expr, depth);
	}

	public void executeBlock(final List<Stmt> statements, final Environment environment) {

		final Environment previous = this.environment;

		try {
			this.environment = environment;

			statements.forEach(statement -> {
				execute(statement);
			});
		} finally {
			this.environment = previous;
		}
	}

	@Override
	public Void visitBlockStmt(final Stmt.Block stmt) {
		executeBlock(stmt.statements, new Environment(environment));
		return null;
	}
	
	@Override
	public Void visitClassStmt(Class stmt) {
		Object superclass = null;
		if (stmt.superclass != null) {
			superclass = evaluate(stmt.superclass);
			if (!(superclass instanceof LoxClass)) {
				throw new RuntimeError(stmt.superclass.name, "Superclass must be a class");
			}
		}
		
		environment.define(stmt.name.lexeme, null);
		
		if (stmt.superclass != null) {
			environment = new Environment(environment);
			environment.define("super", superclass);
		}

		final Map<String, LoxFunction> methods = new HashMap<>();
		for (final Stmt.Function method : stmt.methods) {
			final LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
			methods.put(method.name.lexeme, function);
		}

		final LoxClass klass = new LoxClass(stmt.name.lexeme, (LoxClass)superclass, methods);
		
		if (superclass != null) {
			environment = environment.enclosing;
		}
		
		environment.assign(stmt.name, klass);
		return null;
	}

	@Override
	public Void visitExpressionStmt(final Stmt.Expression stmt) {
		final Object value = evaluate(stmt.expression);
		checkUninitializedVariable(value, stmt.expression);
		return null;
	}
	
	@Override
	public Void visitFunctionStmt(Function stmt) {
		final LoxFunction function = new LoxFunction(stmt, environment, false);
		environment.define(stmt.name.lexeme, function);
		return null;
	}
	
	@Override
	public Void visitIfStmt(If stmt) {
		if (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.thenBranch);
		} else if (stmt.elseBranch != null) {
			execute(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Void visitPrintStmt(final Stmt.Print stmt) {
		final Object value = evaluate(stmt.expression);
		checkUninitializedVariable(value, stmt.expression);
		System.out.println(stringify(value));
		return null;
	}
	
	@Override
	public Void visitReturnStmt(final Stmt.Return stmt) {
		Object value = null;
		if (stmt.value != null) {
			value = evaluate(stmt.value);
		}
		
		throw new com.craftinginterpreters.lox.Return(value);
	}

	@Override
	public Void visitVarStmt(final Stmt.Var stmt) {
		Object value = null;
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}

		environment.define(stmt.name.lexeme, value);
		return null;
	}
	
	@Override
	public Void visitWhileStmt(final While stmt) {
		while (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.body);
		}
		return null;
	}

	@Override
	public Object visitAssignExpr(final Expr.Assign expr) {
		final Object value = evaluate(expr.value);
		final Integer distance = locals.get(expr);
		if (distance != null) {
			environment.assignAt(distance, expr.name, value);
		} else {
			globals.assign(expr.name, value);
		}
		return value;
	}

	/*
	 * Lox follows Ruby’s simple rule: false and nil are falsey, and everything else
	 * is truthy
	 */
	private boolean isTruthy(final Object object) {
		if (object == null)
			return false;
		if (object instanceof Boolean)
			return (boolean) object;
		return true;
	}

	private boolean isEqual(final Object a, final Object b) {
		if (a == null && b == null)
			return false;
		if (a == null)
			return false;

		return a.equals(b);
	}

	private String stringify(final Object object) {
		if (object == null)
			return "nil";
		if (object instanceof Double) {
			String text = object.toString();
			if (text.endsWith(".0")) {
				text = text.substring(0, text.length() - 2);
			}
			return text;
		}
		return object.toString();
	}
	
	private static void checkUninitializedVariable(final Object value, final Expr expression) {
		if (value == null && expression instanceof Expr.Variable) {
			final Variable var = (Variable) expression;
			throw new RuntimeError(var.name, "Cannot print uninitialized variable");
		}
	}
}
