package heap;

import chainexception.*;

public class InvalidSlotNumberException extends ChainException {

	private static final long serialVersionUID = 1L;

	public InvalidSlotNumberException() {
		super();
	}

	public InvalidSlotNumberException(Exception ex, String name) {
		super(ex, name);
	}

}
