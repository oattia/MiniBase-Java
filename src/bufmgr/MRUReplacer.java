package bufmgr;

import global.PageId;
import java.util.List;
import java.util.Stack;

public class MRUReplacer extends Replacer {

	private Stack<BufDescr> cands;

	public MRUReplacer(BufMgr bufMgr) {
		bufDescr = bufMgr.getBufDescr();
		directory = bufMgr.getDirectory();
		candidates = new Stack<>();
		cands = (Stack<BufDescr>) candidates;
	}

	// remove from the container
	@Override
	public int chooseVictim() throws Exception {
		Integer position = -1;
		for (int i = candidates.size() - 1; i >= 0; i--) {
			BufDescr victim = candidates.get(i);
			if (victim.getPin_count() == 0) {
				candidates.remove(i);
				position = directory.get(victim.getPagenumber());
				break;
			}
		}
		if (position == null || position.equals(new Integer(-1)))
			throw new BufferPoolExceededException(null, null);
		else
			return position;
	}

	// add to the linked list
	@Override
	public void updateCandsPinned(PageId pgid) {
		BufDescr current = bufDescr[directory.get(pgid.pid)];
		cands.remove(current);
		cands.push(current);
	}

	@Override
	public void updateCandsUnpinned(PageId pgid) {
		// Does NOTHING.
	}

	public List<BufDescr> getList() {
		return candidates;
	}

	public void push(BufDescr bufDescr) {
		cands.push(bufDescr);
	}
}
