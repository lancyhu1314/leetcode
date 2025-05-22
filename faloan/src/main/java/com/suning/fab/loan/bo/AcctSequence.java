package com.suning.fab.loan.bo;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
@Scope("singleton")
@Service
public class AcctSequence {
	AtomicInteger startSeqno = new AtomicInteger(0);
	AtomicInteger currentSeqno = new AtomicInteger(0);
	/**
	 * @return the startSeqno
	 */
	public AtomicInteger getStartSeqno() {
		return startSeqno;
	}
	/**
	 * @param startSeqno the startSeqno to set
	 */
	public void setStartSeqno(AtomicInteger startSeqno) {
		this.startSeqno = startSeqno;
	}
	/**
	 * @return the currentSeqno
	 */
	public AtomicInteger getCurrentSeqno() {
		return currentSeqno;
	}
	/**
	 * @param currentSeqno the currentSeqno to set
	 */
	public void setCurrentSeqno(AtomicInteger currentSeqno) {
		this.currentSeqno = currentSeqno;
	}

}
