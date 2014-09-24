package heap;

import chainexception.*;

public class InvalidTypeException extends ChainException {

	private static final long serialVersionUID = 1L;

	public InvalidTypeException() {
		super();
	}

	public InvalidTypeException(Exception ex, String name) {
		super(ex, name);
	}

}
