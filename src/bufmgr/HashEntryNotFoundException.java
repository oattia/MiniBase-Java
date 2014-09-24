package bufmgr;

import chainexception.ChainException;

public class HashEntryNotFoundException extends ChainException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public HashEntryNotFoundException(Exception e, String name) {
		super(e, name);
	}
}
