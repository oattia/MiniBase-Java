package bufmgr;

import chainexception.ChainException;

public class BufferPoolExceededException extends ChainException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BufferPoolExceededException(Exception e, String name) {
		super(e, name);
	}

}
