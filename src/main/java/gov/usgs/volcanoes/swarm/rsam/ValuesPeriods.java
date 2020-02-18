package gov.usgs.volcanoes.swarm.rsam;


public enum ValuesPeriods {
  ONE_M(60, "One Minute"), TEN_M(10 * 60, "Ten Minutes"), ONE_H(60 * 60,
      "One Hour"), ONE_D(24 * 60 * 60, "One Day");

  private int periodS;
  private String string;

  private ValuesPeriods(int period, String string) {
    this.periodS = period;
    this.string = string;
  }

  public String toString() {
    return string;
  }

  public int getPeriodS() {
    return periodS;
  }

  public static ValuesPeriods fromS(int s) {
    for (ValuesPeriods p : ValuesPeriods.values()) {
      if (p.periodS == s) {
        return p;
      }
    }

    return null;
  }
}
