package bufmgr;

import chainexception.*;

public class HashOperationException extends ChainException {

	private static final long serialVersionUID = 1L;

	public HashOperationException(Exception e, String name) {
		super(e, name);
	}

}
