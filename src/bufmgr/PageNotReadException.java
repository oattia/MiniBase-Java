package bufmgr;

import chainexception.*;

public class PageNotReadException extends ChainException {

	private static final long serialVersionUID = 1L;

	public PageNotReadException(Exception e, String name) {
		super(e, name);
	}

}
