package heap;

import chainexception.ChainException;
import global.GlobalConst;
import global.PageId;
import global.RID;
import global.SystemDefs;

/**
 * A Scan object is created ONLY through the function openScan of a HeapFile. It
 * supports the getNext interface which will simply retrieve the next record in
 * the heapfile. An object of type scan will always have pinned one directory
 * page of the heapfile.
 */
public class Scan implements GlobalConst {

	// Iterator index
	private RID nextRID;

	// NO CHANGES
	private Heapfile hf;
	private PageId fps;
	private PageId fpns;

	private boolean finished;

	private HFPage toBeScanned;

	/**
	 * The constructor pins the first directory page in the file and initializes
	 * its private data members from the private data member from hf
	 * 
	 * Parameters: hf - A HeapFile object
	 */

	public Scan(PageId headId, Heapfile hfile) {
		try {
			hf = hfile;
			finished = false;

			HeaderHFPage header = new HeaderHFPage();
			SystemDefs.JavabaseBM.pinPage(headId, header, false);
			header.readHPageIn(header, headId);
			PageId fpnsCand = header.getFirstPageNoSpace();
			PageId fpsCand = header.getFirstPageSpace();
			SystemDefs.JavabaseBM.unpinPage(headId, false);

			int pageIdFound = 0;
			if (fpnsCand == null && fpsCand == null) {
				finished = true;
			} else if (fpnsCand == null) {
				fps = fpsCand;
				pageIdFound = fps.pid;
			} else if (fpsCand == null) {
				fpns = fpnsCand;
				pageIdFound = fpns.pid;
			} else {
				fpns = fpnsCand;
				fps = fpsCand;
				pageIdFound = fpns.pid;
			}

			if (!finished) {
				// Set pageScanned by non -1 pageId
				PageId pageScanned = new PageId(pageIdFound);
				toBeScanned = new HFPage();
				SystemDefs.JavabaseBM.pinPage(pageScanned, toBeScanned, false);
				RID frstRec = toBeScanned.firstRecord();
				// Set lastRIDScanned by firstRecord() of fpns
				nextRID = frstRec;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieve the next record in a sequential scan
	 * 
	 * Parameters: rid - Record ID of the record.
	 * 
	 * Returns: the Tuple of the retrieved record.
	 */
	public Tuple getNext(RID rid) {
		if (!finished) {
			Tuple res = hf.getRecord(nextRID);
			rid.pageNo = nextRID.pageNo;
			rid.slotNo = nextRID.slotNo;
			setNextRID();
			return res;
		} else {
			// This must be done to un-pin the page when the scan returns null
			// because the records are done (finished) and the scan is still not
			// closed
			if (nextRID != null) {
				try {
					SystemDefs.JavabaseBM.unpinPage(nextRID.pageNo, false);
				} catch (ChainException e) {
					// DO NOTHING
				}
			}
			return null;
		}
	}

	private void setNextRID() {
		try {
			RID nextCand = toBeScanned.nextRecord(nextRID);
			if (nextCand != null) {
				nextRID = nextCand;
			} else {
				PageId nextPage = toBeScanned.getNextPage();
				if (nextPage.pid != -1) {
					// Not the last page in the list
					SystemDefs.JavabaseBM.unpinPage(nextRID.pageNo, false);
					SystemDefs.JavabaseBM.pinPage(nextPage, toBeScanned, false);
					nextRID = toBeScanned.firstRecord();
				} else {
					// The last page in the list
					if (toBeScanned.available_space() == 0) {
						// if in fpns, search in fps
						if (fps != null) {
							SystemDefs.JavabaseBM.unpinPage(nextRID.pageNo,
									false);
							SystemDefs.JavabaseBM.pinPage(fps, toBeScanned,
									false);
							nextRID = toBeScanned.firstRecord();
						} else {
							// DONE WITH ALL FPNS, FPS DOES NOT EXIST.
							finished = true;
						}
					} else {
						// if in fps, close scan.
						finished = true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Position the scan cursor to the record with the given rid.
	 * 
	 * Parameters: rid - Record ID of the given record
	 * 
	 * Returns: true if successful, false otherwise.
	 */
	public boolean position(RID rid) {
		nextRID = rid;
		finished = false;
		if (getNext(new RID()) == null) {
			finished = true;
			return false;
		} else {
			return true;
		}

	}

	/**
	 * Closes the Scan object
	 */
	public void closescan() {
		finished = true;
		hf = null;
		fps = null;
		fpns = null;
		toBeScanned = null;
		// This must be left in case someone closes the scan before finishing
		// all records
		try {
			if (nextRID != null)
				SystemDefs.JavabaseBM.unpinPage(nextRID.pageNo, false);
		} catch (ChainException e) {
			// DO NOTHING
		}
		nextRID = null;
	}

}
