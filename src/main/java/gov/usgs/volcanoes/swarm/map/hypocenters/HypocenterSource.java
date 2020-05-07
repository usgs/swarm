package gov.usgs.volcanoes.swarm.map.hypocenters;

public enum HypocenterSource {
  NONE("None", null), HR_SIG("1 Hour - Significant",
      "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_hour.quakeml"), HR_45(
          "1 Hour - M4.5+",
          "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_hour.quakeml"), HR_25(
              "1 Hour - M2.5+",
              "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_hour.quakeml"), HR_1(
                  "1 Hour - M1.0+",
                  "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/1.0_hour.quakeml"), HR_ALL(
                      "1 Hour - All",
                      "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_hour.quakeml"), DAY_SIG(
                          "1 Day - Significant",
                          "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_day.quakeml"), DAY_45(
                              "1 Day - M4.5+",
                              "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.quakeml"), DAY_25(
                                  "1 Day - M2.5+",
                                  "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_day.quakeml"), DAY_1(
                                      "1 Day - M1.0+",
                                      "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/1.0_day.quakeml"), DAY_ALL(
                                          "1 Day - All",
                                          "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.quakeml"), WEEK_SIG(
                                              "1 Week - Significant",
                                              "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_week.quakeml"), WEEK_45(
                                                  "1 Week - M4.5+",
                                                  "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.quakeml"), WEEK_25(
                                                      "1 Week - M2.5+",
                                                      "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.quakeml"), WEEK_1(
                                                          "1 Week - M1.0+",
                                                          "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/1.0_week.quakeml"), WEEK_ALL(
                                                              "1 Week - All",
                                                              "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_week.quakeml"), MONTH_SIG(
                                                                  "1 Month - Significant",
                                                                  "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_month.quakeml"), MONTH_45(
                                                                      "1 Month - M4.5+",
                                                                      "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_month.quakeml"), MONTH_25(
                                                                          "1 Month - M2.5+",
                                                                          "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_month.quakeml"), MONTH_1(
                                                                              "1 Month - M1.0+",
                                                                              "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/1.0_month.quakeml"), MONTH_ALL(
                                                                                  "1 Month - All",
                                                                                  "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_month.quakeml");

  private String label;
  private String url;

  private HypocenterSource(String label, String url) {
    this.label = label;
    this.url = url;
  }

  public String toString() {
    return label;
  }

  public String getUrl() {
    return url;
  }

  /**
   * Get hypocenter source from url.
   * 
   * @param hypocenterUrl url
   * @return
   */
  public static HypocenterSource fromUrl(String hypocenterUrl) {
    for (HypocenterSource source : HypocenterSource.values()) {
      if (source.url.equals(hypocenterUrl)) {
        return source;
      }
    }
    throw new RuntimeException("Cannot find hypo source for " + hypocenterUrl);
  }
}
