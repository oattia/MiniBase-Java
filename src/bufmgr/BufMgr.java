package bufmgr;

import java.io.IOException;
import java.util.HashMap;
import chainexception.ChainException;
import diskmgr.FileIOException;
import diskmgr.InvalidPageNumberException;
import diskmgr.Page;
import global.PageId;
import global.SystemDefs;

public class BufMgr {

	private byte[][] buffPool;
	private BufDescr[] bufDescr;
	private HashMap<Integer, Integer> directory;

	private int nextBufID;
	private boolean full;

	private Replacer replacer;

	/**
	 * Create the BufMgr object Allocate pages (frames) for the buffer pool in
	 * main memory and make the buffer manager aware that the replacement policy
	 * is specified by replaceArg (i.e. FIFO, LRU, MRU, love/hate)
	 * 
	 * @param numbufs
	 *            number of buffers in the buffer pool
	 * @param replaceArg
	 *            name of the buffer replacement policy
	 */
	public BufMgr(int numBufs, String replaceArg) {
		buffPool = new byte[numBufs][];
		bufDescr = new BufDescr[numBufs];
		directory = new HashMap<Integer, Integer>();
		nextBufID = 0;
		full = false;

		if (replaceArg == null || replaceArg.equalsIgnoreCase("CLOCK")) {
			replacer = new FIFOReplacer(this);
		} else if (replaceArg.equalsIgnoreCase("MRU")) {
			replacer = new MRUReplacer(this);
		} else if (replaceArg.equalsIgnoreCase("LRU")) {
			replacer = new LRUReplacer(this);
		} else if (replaceArg.equalsIgnoreCase("Love/Hate")) {
			replacer = new LoveHateReplacer(this);
		} else {
			replacer = new FIFOReplacer(this);
		}
	}

	/**
	 * Pin a page First check if this page is already in the buffer pool. If it
	 * is, increment the pin_count and return pointer to this page.
	 * 
	 * 
	 * If the pin_count was 0 before the call, the page was a replacement
	 * candidate, but is no longer a candidate.
	 * 
	 * 
	 * If the page is not in the pool, choose a frame (from the set of
	 * replacement candidates) to hold this page, read the page (using the
	 * appropriate method from diskmgr package) and pin it.
	 * 
	 * 
	 * Also, must write out the old page in chosen frame if it is dirty before
	 * reading new page. (You can assume that emptyPage == false for this
	 * assignment.)
	 * 
	 * @param pgid
	 *            page number in the minibase.
	 * @param page
	 *            the pointer point to the page.
	 * @param emptyPage
	 *            true (empty page), false (non­empty page).
	 */
	public void pinPage(PageId pgid, Page page, boolean emptyPage)
			throws Exception, ChainException {

		if (directory.get(pgid.pid) != null) {
			BufDescr current = bufDescr[directory.get(pgid.pid)];
			current.incrPinCount();
			page.setpage(buffPool[directory.get(pgid.pid)]);
		} else {
			Page temp = new Page();
			if (full) {
				int index = replacer.chooseVictim();

				if (bufDescr[index].isDirtybit())
					flushPage(new PageId(bufDescr[index].getPagenumber()));

				readFromDisk(pgid, temp, index);
			} else {
				readFromDisk(pgid, temp, nextBufID);
				updateNextBufID();
			}
			page.setpage(temp.getpage());
		}
		replacer.updateCandsPinned(pgid);
	}

	private void readFromDisk(PageId pgid, Page page, int index)
			throws InvalidPageNumberException, FileIOException, IOException {

		SystemDefs.JavabaseDB.read_page(pgid, page);

		if (bufDescr[index] != null)
			directory.remove(bufDescr[index].getPagenumber());

		buffPool[index] = page.getpage();
		bufDescr[index] = new BufDescr(pgid.pid);
		directory.put(pgid.pid, index);
	}

	/**
	 * Unpin a page specified by a pageId.
	 * 
	 * This method should be called with dirty == true if the client has
	 * modified the page. If so, this call should set the dirty bit for this
	 * frame. Further, if pin_count > 0, this method should decrement it. If
	 * pin_count = 0 before this call, throw an exception to report error. (for
	 * testing purposes, we ask you to throw an exception named
	 * PageUnpinnedExcpetion in case of error.)
	 * 
	 * @param pgid
	 *            page number in the minibase
	 * @param dirty
	 *            the dirty bit of the frame.
	 */
	public void unpinPage(PageId pgid, boolean dirty) throws ChainException {

		// printDirectory("UN-PIN PAGE - FIRST");

		if (directory.get(pgid.pid) != null) {
			BufDescr currentDes = bufDescr[directory.get(pgid.pid)];

			if (dirty) {
				try {
					flushPage(new PageId(currentDes.getPagenumber()));
				} catch (IOException e) {
					// e.printStackTrace();
				}
			}
			if (currentDes.getPin_count() > 0) {
				currentDes.decrPinCount();
				replacer.updateCandsUnpinned(pgid);
			} else {
				throw new PageUnpinnedException(null, null);
			}
		} else {
			throw new HashEntryNotFoundException(null, null);
		}

		// printDirectory("UN-PIN PAGE - FINISH");
	}

	/**
	 * Allocate new page(s).
	 * 
	 * Call DB Object to allocate a run of new pages and find a frame in the
	 * buffer pool for the first page and pin it.
	 * 
	 * If buffer is full, i.e., you can't find a frame for the first page, ask
	 * DB to deallocate all these pages, and return null.
	 * 
	 * @param firstPage
	 *            the address of the first page.
	 * @param howmany
	 *            total number of allocated new pages.
	 * 
	 * @return the first page id of the new pages. null, if error.
	 * @throws Exception
	 */

	public PageId newPage(Page firstPage, int howmany) throws Exception {
		if (!full) {
			PageId currentPageID = new PageId();
			SystemDefs.JavabaseDB.allocate_page(currentPageID, howmany);
			pinPage(currentPageID, firstPage, false);
			return currentPageID;
		} else {
			return null;
		}
	}

	/**
	 * This method should be called to delete a page that is on disk. This
	 * routine must call the method in diskmgr package to deallocate the page.
	 * 
	 * @param pgid
	 *            the page number in the database.
	 * @throws IOException
	 */
	public void freePage(PageId pgid) throws ChainException, IOException {
		if (directory.get(pgid.pid) != null) {
			if (bufDescr[directory.get(pgid.pid)].getPin_count() > 1)
				throw new PagePinnedException(null, null);

			if (full) {
				full = false;
				// printDirectory("freePage");
			}
			if (nextBufID > 0) {
				int toMove = --nextBufID;
				replacer.removeFromCands(pgid);
				swapLast(pgid, toMove);
			}
		}
		SystemDefs.JavabaseDB.deallocate_page(pgid);
	}

	private void swapLast(PageId pgid, int toMove) {
		int toFree = directory.get(pgid.pid);

		directory.remove(pgid.pid);

		if (toFree == toMove) {
			buffPool[toMove] = null;
			bufDescr[toMove] = null;
		} else {
			buffPool[toFree] = buffPool[toMove];
			buffPool[toMove] = null;

			bufDescr[toFree] = bufDescr[toMove];
			bufDescr[toMove] = null;

			directory.put(bufDescr[toFree].getPagenumber(), toFree);
		}

	}

	/**
	 * Used to flush a particular page of the buffer pool to disk. This method
	 * calls the write_page method of the diskmgr package.
	 * 
	 * @param pgid
	 *            the page number in the database.
	 * @throws IOException
	 * @throws FileIOException
	 * @throws InvalidPageNumberException
	 */
	public void flushPage(PageId pgid) throws InvalidPageNumberException,
			FileIOException, IOException {
		if (directory.get(pgid.pid) != null) {
			if (bufDescr[directory.get(pgid.pid)].isDirtybit()) {
				Page toWrite = new Page(buffPool[directory.get(pgid.pid)]);
				SystemDefs.JavabaseDB.write_page(pgid, toWrite);
				bufDescr[directory.get(pgid.pid)].setDirtybit(false);
			}
		}
	}

	public int getNumUnpinnedBuffers() {
		int counter = 0;

		for (int i = 0; i < bufDescr.length; i++) {
			if (bufDescr[i] == null || bufDescr[i].getPin_count() == 0) {
				counter++;
			}
		}
		return counter;
	}

	public void flushAllPages() throws InvalidPageNumberException,
			FileIOException, IOException {
		for (int i = 0; i < buffPool.length; i++) {
			if (bufDescr[i] != null) {
				flushPage(new PageId(bufDescr[i].getPagenumber()));
			} else {
				break;
			}
		}
	}

	private void updateNextBufID() {
		if (nextBufID < buffPool.length)
			nextBufID++;

		if (nextBufID >= buffPool.length) {
			full = true;
		}
	}

	public BufDescr[] getBufDescr() {
		return bufDescr;
	}

	public HashMap<Integer, Integer> getDirectory() {
		return directory;
	}

	public boolean isFull() {
		return full;
	}

	public int getNumBuffers() {
		return bufDescr.length;
	}
}
