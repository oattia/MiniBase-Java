package bufmgr;

import global.PageId;
import java.util.LinkedList;

public class LoveHateReplacer extends Replacer {

	// private LinkedList<BufDescr> HatedList;
	// private List<BufDescr> LovedList;
	//
	// private Queue<BufDescr> HatedcandsQueue;
	// private Stack<BufDescr> LovedcandsStack;

	private LRUReplacer HateObj;
	private MRUReplacer LoveObj;

	public LoveHateReplacer(BufMgr bufMgr) {
		bufDescr = bufMgr.getBufDescr();
		directory = bufMgr.getDirectory();
		HateObj = new LRUReplacer(bufMgr);
		LoveObj = new MRUReplacer(bufMgr);

		// HatedList = new LinkedList<>();
		// LovedList = new LinkedList<>();
		// HatedcandsQueue = (Queue<BufDescr>) HatedList;
		// LovedcandsStack = (Stack<BufDescr>) LovedList;
	}

	@Override
	public int chooseVictim() throws Exception {

		Integer index;
		// check hated victim first
		try {

			index = HateObj.chooseVictim();

		}
		// check Loved victim first
		catch (Exception e) {

			index = LoveObj.chooseVictim();

		}
		// error
		if (index == null || index.equals(new Integer(-1))) {
			throw new Exception();
		} else {
			return index;
		}
	}

	@Override
	public void updateCandsPinned(PageId pgid) {

		boolean loved = false;
		BufDescr current = bufDescr[directory.get(pgid.pid)];

		if (current.isLoved()) {
			// loved => set it in the stack
			LoveObj.updateCandsPinned(pgid);
		} else {
			// not loved => ask if it is already loved by another user if loved
			// by another user DO NOTHING else set it in the hated queue

			for (int i = 0; i < LoveObj.getList().size(); i++) {
				BufDescr check = (BufDescr) LoveObj.getList().get(i);
				if (check.getPagenumber().equals(pgid.pid)) {
					loved = true;
				}
			}
			if (!loved) {
				HateObj.updateCandsPinned(pgid);
			}

		}

	}

	@Override
	public void updateCandsUnpinned(PageId pgid) {

		boolean FoundInHatedList = false;

		if (bufDescr[directory.get(pgid)].isLoved()) {
			LinkedList<BufDescr> HatedList = (LinkedList<BufDescr>) HateObj
					.getList();
			for (int i = 0; i < HatedList.size(); i++) {
				BufDescr victim = (BufDescr) HatedList.get(i);
				if (victim.getPin_count() == 0) {
					HatedList.remove(i);
					FoundInHatedList = true;
					break;
				}
			}

			if (FoundInHatedList) {
				LoveObj.push(bufDescr[directory.get(pgid.pid)]);
			}
		}

	}
}
