package bufmgr;

import chainexception.ChainException;

public class PageUnpinnedException extends ChainException {

	private static final long serialVersionUID = 1L;

	public PageUnpinnedException(Exception e, String name) {
		super(e, name);
	}

}
