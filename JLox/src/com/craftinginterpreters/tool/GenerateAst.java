/**
 * 
 */
package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * @author Admin
 *
 */
public class GenerateAst {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage: generate_ast <output_directory>");
			System.exit(64);
		}
		String outputDir = args[0];
		defineAst(outputDir, "Expr", Arrays.asList(
				"Assign : Token name, Expr value",
				"Binary : Expr left, Token operator, Expr right",
				"Call	: Expr callee, Token paren, List<Expr> arguments",
				"Grouping : Expr expression", 
				"Literal : Object value",
				"Logical : Expr left, Token operator, Expr right",
				"Unary : Token operator, Expr right",
				"Variable : Token name"
				));
		defineAst(outputDir, "Stmt", Arrays.asList(
				"Block		: List<Stmt> statements",
				"Expression	: Expr expression",
				"Function	: Token name, List<Token> params," +
				" List<Stmt> body",
				"If			: Expr condition, Stmt thenBranch," +
							" Stmt elseBranch",
				"Print		: Expr expression",
				"Return		: Token keyword, Expr value",
				"Var		: Token name, Expr initializer",
				"While		: Expr condition, Stmt body"
				));
	}

	private static void defineAst(final String outputDir, final String baseName, final List<String> types)
			throws IOException {
		final String path = outputDir + "/" + baseName + ".java";
		try (final PrintWriter writer = new PrintWriter(path, "UTF-8")) {
			writer.println("package com.craftinginterpreters.lox;");
			writer.println();
			// writing import here.
			writer.println();
			writer.println("abstract class " + baseName + " {");
			
			defineVisitor(writer, baseName, types);
			
			// The AST class
			for(final String type : types) {
				final String className = type.split(":")[0].trim();
				final String fields = type.split(":")[1].trim();
				defineType(writer, baseName, className, fields);
			}
			
			// The base accept() method.
			writer.println();
			writer.println("	abstract <R> R accept(Visitor<R> visitor);");

			writer.println("}");
		}
	}
	
	private static void defineType(final PrintWriter writer, final String baseName, final String className, final String fieldList) {
		writer.println("	static class " + className + " extends " + baseName + " {");
		
		// Constructor
		writer.println("		" + className + "(" + fieldList + ") {");
		
		// Store parameters in fields.
		final String[] fields = fieldList.split(", ");
		for (final String field : fields) {
			final String name = field.split(" ")[1];
			writer.println("			this." + name + " = " + name + ";");
		}
		
		writer.println("		}");
		
		// Visitor pattern.
		writer.println();
		writer.println("		@Override");
		writer.println("		<R> R accept(final Visitor<R> visitor) {");
		writer.println("			return visitor.visit" + className + baseName + "(this);");
		writer.println("		}");
		
		// Fields.
		writer.println();
		for (final String field : fields) {
			writer.println("		final " + field + ";");
		}
		
		writer.print("	}");
		writer.println();
	}
	
	private static void defineVisitor(final PrintWriter writer, final String baseName, final List<String> types) {
		writer.println("	interface Visitor<R> {");
		
		for (final String type : types) {
			final String typeName = type.split(":")[0].trim();
			writer.println("		R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
		}
		writer.println("	}");
	}
}
