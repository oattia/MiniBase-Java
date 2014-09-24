package bufmgr;

import chainexception.*;

public class InvalidFrameNumberException extends ChainException {

	private static final long serialVersionUID = 1L;

	public InvalidFrameNumberException(Exception e, String name) {
		super(e, name);
	}

}
