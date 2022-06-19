/**
 * 
 */
package com.craftinginterpreters.lox;

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

/**
 * @author Admin
 *
 */
public class AstPrinter implements Expr.Visitor<String> {

	@Override
	public String visitBinaryExpr(Binary expr) {
		return parenthesize(expr, expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitGroupingExpr(Grouping expr) {
		return parenthesize(expr, "group", expr.expression);
	}

	@Override
	public String visitLiteralExpr(Literal expr) {
		if (expr.value == null) return "nil";
		return expr.value.toString();
	}

	@Override
	public String visitUnaryExpr(Unary expr) {
		return parenthesize(expr, expr.operator.lexeme, expr.right);
	}
	
	
//	private String parenthesize(final String name, final Expr... exprs) {
//		final StringBuilder builder = new StringBuilder();
//		
//		builder.append("(").append(name);
//		for (final Expr expr : exprs) {
//			builder.append(" ");
//			builder.append(expr.accept(this));
//		}
//		builder.append(")");
//		
//		return builder.toString();
//	}
	
	private String parenthesize(Expr expression, final String name, final Expr... exprs) {
		final StringBuilder builder = new StringBuilder();
		final boolean isUnary = expression instanceof Unary;
		if (isUnary) {
			builder.append(name);
			for (final Expr expr : exprs) {
				builder.append(expr.accept(this));
			}
		} else {
			for (final Expr expr : exprs) {
				builder.append(expr.accept(this));
				builder.append(" ");
			}
			builder.append(name);
		}
		
		
		return builder.toString();
	}
	
	String print(final Expr expr) {
		return expr.accept(this);
	}

	@Override
	public String visitVariableExpr(Variable expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visitAssignExpr(Assign expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visitLogicalExpr(Logical expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visitCallExpr(Call expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visitGetExpr(Get expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visitSetExpr(Set expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visitThisExpr(This expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visitSuperExpr(Super expr) {
		// TODO Auto-generated method stub
		return null;
	}

	
	// test for RPN
//	public static void main(final String[] args) {
//		final Expr expression = new Expr.Binary(
//				new Expr.Unary(new Token(TokenType.MINUS, "-", null, 1), 
//								new Expr.Literal(123)),
//				new Expr.Grouping(
//						new Expr.Literal(45.67)),
//				new Token(TokenType.STAR, "*", null, 1));
//		System.out.println(new AstPrinter().print(expression));
//	}

}
