package bufmgr;

import chainexception.ChainException;

public class PagePinnedException extends ChainException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PagePinnedException(Exception e, String name) {
		super(e, name);
	}

}
