package heap;

import chainexception.*;

public class InvalidTupleSizeException extends ChainException {

	private static final long serialVersionUID = 1L;

	public InvalidTupleSizeException() {
		super();
	}

	public InvalidTupleSizeException(Exception ex, String name) {
		super(ex, name);
	}

}
