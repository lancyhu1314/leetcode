package com.suning.fab.loan.domain;

import javax.persistence.Entity;
import javax.persistence.Column;
@Entity(name = "LNSHOLIDAY")
public class TblLnsholiday {
    private String holidaydate = "";

    private String festival = "";

    private String workflag = "";

    public TblLnsholiday() {

   }
    
    public TblLnsholiday(String holidaydate,String workflag,String festival) {
        this.holidaydate = holidaydate;
        this.workflag = workflag;
        this.festival = festival;
   }
    
    @Override
	public int hashCode() {
		return (holidaydate + "|" + festival).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		TblLnsholiday other = (TblLnsholiday) obj;
		
		if (holidaydate == null) {
			if (other.holidaydate != null)
				return false;
		} else if (!holidaydate.equals(other.holidaydate))
			return false;
		
		if (festival == null) {
			if (other.festival != null)
				return false;
		} else if (!festival.equals(other.festival))
			return false;
		
		return true;
	}

    @Column(name = "Holidaydate")
    public String getHolidaydate() {
        return holidaydate;
    }

    public void setHolidaydate(String holidaydate) {
        this.holidaydate = holidaydate == null ? "" : holidaydate.trim();
    }

    @Column(name = "Festival")
    public String getFestival() {
        return festival;
    }

    public void setFestival(String festival) {
        this.festival = festival == null ? "" : festival.trim();
    }

    @Column(name = "Workflag")
    public String getWorkflag() {
        return workflag;
    }

    public void setWorkflag(String workflag) {
        this.workflag = workflag == null ? "" : workflag.trim();
    }
}