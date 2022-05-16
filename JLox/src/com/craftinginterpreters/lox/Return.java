package com.craftinginterpreters.lox;

public class Return extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final Object value;
	
	public Return(final Object value) {
		super(null, null, false, false);
		this.value = value;
	}
}
