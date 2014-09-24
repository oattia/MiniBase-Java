package bufmgr;

import global.PageId;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * This replacer accounts for all frames disregarding their pin_count.
 */
public class LRUReplacer extends Replacer {

	private Queue<BufDescr> cands;

	@SuppressWarnings("unchecked")
	public LRUReplacer(BufMgr bufMgr) {
		bufDescr = bufMgr.getBufDescr();
		directory = bufMgr.getDirectory();
		candidates = new LinkedList<>();
		cands = (Queue<BufDescr>) candidates;
	}

	@Override
	public int chooseVictim() throws Exception {
		Integer position = -1;
		for (int i = 0; i < candidates.size(); i++) {
			BufDescr victim = candidates.get(i);
			if (victim.getPin_count() == 0) {
				candidates.remove(i);
				position = directory.get(victim.getPagenumber());
				break;
			}
		}
		if (position == null || position.equals(new Integer(-1))) {
			throw new BufferPoolExceededException(null, null);
		} else {
			return position;
		}
	}

	@Override
	public void updateCandsPinned(PageId pgid) {
		BufDescr current = bufDescr[directory.get(pgid.pid)];
		cands.remove(current);
		cands.offer(current);
	}

	@Override
	public void updateCandsUnpinned(PageId pgid) {
		// Does NOTHING.
	}

	public List<BufDescr> getList() {
		return candidates;
	}

}
