package com.vinayemani.devsearch;

/**
 * BadInputFileException is thrown when data in an input file is badly formed.
 *  
 * @author Vinay E.
 *
 */
public class BadInputFileException extends Exception {
	private static final long serialVersionUID = -6549342602576547940L;
	
	public BadInputFileException(String message) {
		super(message);
	}
}
