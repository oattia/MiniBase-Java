package bufmgr;

import global.PageId;
import java.util.LinkedList;
import java.util.Queue;

import chainexception.ChainException;

/**
 * This replacer only accounts for frames with ZERO pin_count.
 */
public class FIFOReplacer extends Replacer {

	private Queue<BufDescr> cands;

	@SuppressWarnings("unchecked")
	public FIFOReplacer(BufMgr bufMgr) {
		bufDescr = bufMgr.getBufDescr();
		directory = bufMgr.getDirectory();
		candidates = new LinkedList<>();
		cands = (Queue<BufDescr>) candidates;
	}

	@Override
	public int chooseVictim() throws Exception, ChainException {
		BufDescr current = cands.poll();
		if (current != null) {
			Integer toReplace = current.getPagenumber();
			int toReplaceIndex = directory.get(toReplace);
			return toReplaceIndex;
		} else {
			throw new BufferPoolExceededException(null, null);
		}
	}

	/**
	 * This will remove the frame with the given PageId from Cands.
	 */
	@Override
	public void updateCandsPinned(PageId pgid) {
		removeFromCands(pgid);
	}

	@Override
	public void updateCandsUnpinned(PageId pgid) {
		BufDescr currentDesc = bufDescr[directory.get(pgid.pid)];
		if (currentDesc.getPin_count() == 0) {
			cands.offer(currentDesc);
		}
	}
}