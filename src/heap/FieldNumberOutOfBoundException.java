package heap;

import chainexception.*;

public class FieldNumberOutOfBoundException extends ChainException {

	private static final long serialVersionUID = 1L;

	public FieldNumberOutOfBoundException() {
		super();
	}

	public FieldNumberOutOfBoundException(Exception ex, String name) {
		super(ex, name);
	}

}
