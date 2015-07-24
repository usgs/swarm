package gov.usgs.swarm.rsam;


public enum ValuesPeriods {
    ONE_M(60, "One Minute"), TEN_M(10 * 60, "Ten Minutes");
    
    private int period;
    private String string;
    
    private ValuesPeriods(int period, String string) {
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