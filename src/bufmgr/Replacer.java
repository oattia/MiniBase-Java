package bufmgr;

import global.PageId;

import java.util.HashMap;
import java.util.List;

public abstract class Replacer {

	protected BufDescr[] bufDescr;
	protected HashMap<Integer, Integer> directory;
	protected List<BufDescr> candidates;
	
	public abstract int chooseVictim() throws Exception;

	public abstract void updateCandsPinned(PageId pgid);

	public abstract void updateCandsUnpinned(PageId pgid);

	public void removeFromCands(PageId pgid) {
		candidates.remove(bufDescr[directory.get(pgid.pid)]);
	}
}
