package bufmgr;

import chainexception.*;

public class InvalidBufferException extends ChainException {

	private static final long serialVersionUID = 1L;

	public InvalidBufferException(Exception e, String name) {
		super(e, name);
	}

}
