package bufmgr;

public class BufDescr {

	private Integer pagenumber;
	private long pin_count;
	private boolean dirtybit;
	private boolean loved;

	public BufDescr(Integer id) {
		this.pagenumber = id;
		this.pin_count = 1;
		this.dirtybit = false;
		this.loved = false;
	}

	public Integer getPagenumber() {
		return this.pagenumber;
	}

	public void setPagenumber(Integer pagenumber) {
		this.pagenumber = pagenumber;
	}

	public long getPin_count() {
		return this.pin_count;
	}

	public void setPin_count(long pin_count) {
		if (pin_count >= 0)
			this.pin_count = pin_count;
	}

	public boolean isDirtybit() {
		return this.dirtybit;
	}

	public void setDirtybit(boolean dirtybit) {
		this.dirtybit = dirtybit;
	}

	public void incrPinCount() {
		this.pin_count++;
	}

	public void decrPinCount() {
		if (this.pin_count != 0) {
			this.pin_count--;
		}
	}

	public boolean isLoved() {
		return this.loved;
	}

	public void setLoved(boolean loved) {
		if (!loved)
			this.loved = loved;
	}
}
