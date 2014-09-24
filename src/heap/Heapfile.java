package heap;

import java.io.IOException;

import bufmgr.BufMgr;
import diskmgr.DB;
import global.GlobalConst;
import global.PageId;
import global.RID;
import global.SystemDefs;

public class Heapfile implements GlobalConst {

	private String fileName;
	private BufMgr bufMgr;
	private DB db;
	private PageId headerID;

	/**
	 * Initialize. A null name produces a temporary heapfile which will be
	 * deleted by the destructor. If the name already denotes a file, the file
	 * is opened; otherwise, a new empty file is created.
	 */
	public Heapfile(String fName) throws IOException {
		if (fName != null) {
			fileName = fName;
		} else {
			// What does 'temporary heapfile which will be deleted by the
			// destructor' mean?
			// Should we terminate here ?
		}

		// Must use system provided tools.
		bufMgr = SystemDefs.JavabaseBM;
		db = SystemDefs.JavabaseDB;

		headerID = null;

		try {
			headerID = db.get_file_entry(fileName);
			HeaderHFPage header = null;
			if (headerID == null) {
				header = new HeaderHFPage();
				headerID = bufMgr.newPage(header, 1);
				header.initHeader(headerID);
				db.add_file_entry(fileName, headerID);
				bufMgr.unpinPage(headerID, true);
				bufMgr.flushPage(headerID);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Insert record into file, return its Rid.
	 * 
	 * Parameters: recPtr - pointer of the record recLen - the length of the
	 * record.
	 */
	public RID insertRecord(byte[] byteArray)
			throws InvalidSlotNumberException, InvalidTupleSizeException,
			SpaceNotAvailableException, IOException {

		if (byteArray.length > MINIBASE_PAGESIZE)
			throw new SpaceNotAvailableException();

		try {
			HeaderHFPage header = new HeaderHFPage();
			bufMgr.pinPage(headerID, header, false);
			header.readHPageIn(header, headerID);

			// ******************************************\\
			// search for pages with enough space \\
			// ******************************************\\

			PageId pageWithSpace = header.getFirstPageSpace();
			RID toRet = null;
			HFPage temp = null;

			while (pageWithSpace != null && pageWithSpace.pid != -1) {
				temp = new HFPage();
				bufMgr.pinPage(pageWithSpace, temp, false);

				if (temp.available_space() >= byteArray.length) {
					// enough empty space found
					toRet = temp.insertRecord(byteArray);
					bufMgr.unpinPage(pageWithSpace, true);
					header.setRecCount(header.getRecCount() + 1);
					bufMgr.unpinPage(headerID, true);
					return toRet;
				}

				bufMgr.unpinPage(pageWithSpace, false);
				pageWithSpace = temp.getNextPage();
			}

			// ******************************************\\
			// no page with enough space found \\
			// ******************************************\\

			HFPage newPage = new HFPage();
			PageId newPageId = bufMgr.newPage(newPage, 1);
			newPage.init(newPageId, newPage);
			newPage.insertRecord(byteArray);

			if (newPage.available_space() > 0) { // put in pagesWithSpace
													// linkedList
				PageId pid = header.getLastPageSpace();

				if (pid != null) {
					// there are pages with space
					bufMgr.pinPage(pid, temp, false);
					temp.setNextPage(newPageId);
					newPage.setPrevPage(temp.getCurPage());
					bufMgr.unpinPage(pid, true);
					bufMgr.flushPage(pid);
				} else {
					// there were no pages with space
					header.setFirstPageSpace(newPageId);
					newPage.setPrevPage(header.getCurPage());
					// -------- set newPage as header's first page space
					header.setFirstPageSpace(newPageId);
				}
				// -------- set newPage as header's last page space
				header.setLastPageSpace(newPageId);

			} else { // put in pagesWithNoSpace linkedList

				PageId pid = header.getLastPageNoSpace();

				if (pid != null) {
					// there are pages with space
					bufMgr.pinPage(pid, temp, false);
					temp.setNextPage(newPageId);
					newPage.setPrevPage(temp.getCurPage());
					bufMgr.unpinPage(pid, true);
					bufMgr.flushPage(pid);
				} else {
					// there were no pages with space
					header.setFirstPageNoSpace(newPageId);
					newPage.setPrevPage(header.getCurPage());
					// -------- set newPage as header's first page no space
					header.setFirstPageNoSpace(newPageId);
				}
				// -------- set newPage as header's last page no space
				header.setLastPageNoSpace(newPageId);

			}
			bufMgr.unpinPage(newPageId, true);
			bufMgr.flushPage(newPageId);
			header.setRecCount(header.getRecCount() + 1);

			bufMgr.unpinPage(headerID, true);
			bufMgr.flushPage(headerID);

			return toRet;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Return number of records in file.
	 */

	public int getRecCnt() {
		try {
			HeaderHFPage header = new HeaderHFPage();
			bufMgr.pinPage(headerID, header, false);
			header.readHPageIn(header, headerID);
			int recCount = header.getRecCount();

			bufMgr.unpinPage(headerID, false);
			return recCount;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Initiate a sequential scan.
	 */
	public Scan openScan() {
		return new Scan(headerID, this);
	}

	/**
	 * Delete record from file with given rid.
	 * 
	 * Returns: true record deleted false:record not found
	 */
	public boolean deleteRecord(RID rid) {
		try {
			HFPage page = new HFPage(); // the page containing the record
			PageId pid = rid.pageNo; // the pid of the page containing the
										// record
			HeaderHFPage header = new HeaderHFPage();

			header.readHPageIn(header, headerID);
			bufMgr.pinPage(pid, page, false);
			bufMgr.pinPage(headerID, header, false);

			page.deleteRecord(rid);

			bufMgr.unpinPage(pid, true);
			header.setRecCount(header.getRecCount() - 1); // update recCount
			bufMgr.unpinPage(headerID, true);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Updates the specified record in the heapfile.
	 * 
	 * Parameters: rid: - the record which needs update
	 * 
	 * newtuple: - the new content of the record
	 */
	public boolean updateRecord(RID rid, Tuple newTuple)
			throws InvalidSlotNumberException, InvalidUpdateException,
			InvalidTupleSizeException, Exception {

		if (getRecord(rid).getLength() != newTuple.getLength()) {
			throw new InvalidUpdateException();
		}

		try {
			HFPage page = new HFPage();
			bufMgr.pinPage(rid.pageNo, page, false);
			page.getRecord(rid).getTupleByteArray();
			page.returnRecord(rid).tupleCopy(newTuple);
			bufMgr.unpinPage(rid.pageNo, true);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Read record from file, returning pointer and length.
	 * 
	 * Parameters: rid - Record ID Returns: a Tuple. if Tuple == null, no more
	 * tuple
	 */
	public Tuple getRecord(RID rid) {
		PageId pid = rid.pageNo;
		HFPage p = new HFPage();

		try {
			bufMgr.pinPage(pid, p, true);
			Tuple t = p.getRecord(rid);
			bufMgr.unpinPage(pid, false);
			return t;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Delete the file from the database.
	 */
	public void deleteFile() throws InvalidSlotNumberException,
			InvalidTupleSizeException, IOException {

		try {
			HeaderHFPage header = new HeaderHFPage();
			bufMgr.pinPage(headerID, header, false);
			header.readHPageIn(header, headerID);

			PageId temp = null;
			HFPage tempPage = new HFPage();

			PageId fpns = header.getFirstPageNoSpace();
			temp = fpns;

			while (temp != null) {
				bufMgr.pinPage(temp, tempPage, false);
				PageId next = tempPage.getNextPage();
				bufMgr.unpinPage(temp, false);
				bufMgr.freePage(temp);
				temp = next;
			}

			PageId fps = header.getFirstPageSpace();
			temp = fps;
			while (temp != null) {
				bufMgr.pinPage(temp, tempPage, false);
				PageId next = tempPage.getNextPage();
				bufMgr.unpinPage(temp, false);
				bufMgr.freePage(temp);
				temp = next;
			}

			bufMgr.unpinPage(headerID, false);
			bufMgr.freePage(headerID);

			db.delete_file_entry(fileName);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
