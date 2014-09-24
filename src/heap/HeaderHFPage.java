package heap;

import java.util.LinkedList;
import java.util.Queue;
import diskmgr.Page;
import global.Convert;
import global.PageId;
import global.RID;

public class HeaderHFPage extends HFPage {

	private Queue<RID> container;

	private static final int qSize = 5;

	/*
	 * Order within the queue (and within the data array):
	 */
	private static final int FPS = 0;
	private static final int FPNS = 1;
	private static final int LPS = 2;
	private static final int LPNS = 3;
	private static final int REC = 4;

	public HeaderHFPage() {
		super();
	}

	public void initHeader(PageId pid) {
		try {
			init(pid, this);
			container = new LinkedList<RID>();
			byte[] f = new byte[Integer.SIZE / 8];
			Convert.setIntValue(-1, 0, f);
			for (int i = 0; i < qSize - 1; i++)
				container.offer(insertRecord(f));

			Convert.setIntValue(0, 0, f);
			container.offer(insertRecord(f));
		} catch (Exception e) {
			// TODO
		}
	}

	public void readHPageIn(Page p, PageId pid) {
		try {
			available_space();
			container = new LinkedList<RID>();
			RID temp = firstRecord();
			for (int i = 0; i < qSize; i++) {
				container.offer(temp);
				temp = nextRecord(temp);
			}
		} catch (Exception e) {
			// TODO
		}
	}

	// /////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////
	// ////////////////////////////////////////////////////////////////////

	private void setInfo(int pos, int nValue) {
		try {
			byte[] f = new byte[Integer.SIZE / 8];
			for (int i = 0; i < qSize; i++) {
				RID top = container.poll();
				int val = Convert.getIntValue(0, getRecord(top)
						.getTupleByteArray());
				deleteRecord(top);
				if (i == pos) {
					Convert.setIntValue(nValue, 0, f);
					container.offer(insertRecord(f));
				} else {
					Convert.setIntValue(val, 0, f);
					container.offer(insertRecord(f));
				}
			}
		} catch (Exception e) {
			// TODO
		}
	}

	// /////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////
	// ////////////////////////////////////////////////////////////////////

	public void setFirstPageSpace(PageId pid) {
		setInfo(FPS, pid.pid);
	}

	public void setFirstPageNoSpace(PageId pid) {
		setInfo(FPNS, pid.pid);
	}

	public void setLastPageSpace(PageId pid) {
		setInfo(LPS, pid.pid);
	}

	public void setLastPageNoSpace(PageId pid) {
		setInfo(LPNS, pid.pid);
	}

	public void setRecCount(int recCount) {
		setInfo(REC, recCount);
	}

	// /////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////
	// ////////////////////////////////////////////////////////////////////

	private PageId getPage(int pos) {
		try {
			LinkedList<RID> temp = (LinkedList<RID>) container;

			PageId pid = new PageId(Convert.getIntValue(0,
					getRecord(temp.get(pos)).getTupleByteArray()));
			if (pid.pid == -1)
				return null;
			return pid;
		} catch (Exception e) {
			// TODO
			return null;
		}
	}

	public PageId getFirstPageSpace() {
		return getPage(FPS);
	}

	public PageId getFirstPageNoSpace() {
		return getPage(FPNS);
	}

	public PageId getLastPageSpace() {
		return getPage(LPS);
	}

	public PageId getLastPageNoSpace() {
		return getPage(LPNS);
	}

	public int getRecCount() {
		try {
			LinkedList<RID> temp = (LinkedList<RID>) container;
			Tuple t = getRecord(temp.get(REC));
			int x = Convert.getIntValue(0, t.getTupleByteArray());
			return x;
		} catch (Exception e) {
			// TODO
			return 0;
		}
	}
}
