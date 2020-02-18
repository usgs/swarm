package gov.usgs.volcanoes.swarm.rsam;


public enum CountsPeriods {
  TEN_S(10, "Ten Seconds"), ONE_M(60, "One Minute"), TEN_M(10 * 60, "Ten Minutes");

  private int periodS;
  private String string;

  private CountsPeriods(int period, String string) {
    this.periodS = period;
    this.string = string;
  }

  public String toString() {
    return string;
  }

  public int getPeriodS() {
    return periodS;
  }

  public static CountsPeriods fromS(int s) {
    for (CountsPeriods p : CountsPeriods.values()) {
      if (p.periodS == s) {
        return p;
      }
    }

    return null;
  }
}
