package gov.usgs.swarm.rsam;

import gov.usgs.math.BinSize;

public enum countsPeriods {
    ONE_M(60, "One Minute"), TEN_M(10 * 60, "Ten Minutes");
    
    private int period;
    private String string;
    
    private countsPeriods(int period, String string) {
        this.period = period;
        this.string = string;
    }
    
    public String toString() {
        return string;
    }
    
    public int getPeriod() {
        return period;
    }
}