package gov.usgs.volcanoes.swarm.map.hypocenters;

public class Hypocenter {
  
  /* 
   * {
   *    "type":"FeatureCollection",
   *    "metadata":
   *        {
   *            "generated":1457314920000,
   *            "url":"http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_day.geojson",
   *            "title":"USGS Magnitude 2.5+ Earthquakes, Past Day",
   *            "status":200,
   *            "api":"1.5.0",
   *            "count":28
   *          },
   *          "features":[
   *            {
   *                "type":"Feature",
   *                "properties":{
   *                    "mag":4,
   *                    "place":"28km NW of Fairview, Oklahoma",
   *                    "time":1457311416660,
   *                    "updated":1457314429000,
   *                    "tz":-360,
   *                    "url":"http://earthquake.usgs.gov/earthquakes/eventpage/us10004vsc",
   *                    "detail":"http://earthquake.usgs.gov/earthquakes/feed/v1.0/detail/us10004vsc.geojson",
   *                    "felt":10,
   *                    "cdi":4.7,
   *                    "mmi":4.65,
   *                    "alert":"green",
   *                    "status":"reviewed",
   *                    "tsunami":0,
   *                    "sig":251,
   *                    "net":"us",
   *                    "code":"10004vsc",
   *                    "ids":",us10004vsc,",
   *                    "sources":",us,",
   *                    "types":",dyfi,general-link,geoserve,losspager,nearby-cities,origin,phase-data,shakemap,tectonic-summary,",
   *                    "nst":null,
   *                    "dmin":0.034,
   *                    "rms":0.64,
   *                    "gap":43,
   *                    "magType":"mb_lg",
   *                    "type":"earthquake",
   *                    "title":"M 4.0 - 28km NW of Fairview, Oklahoma"
   *                  },
   *                  "geometry":{
   *                    "type":"Point",
   *                    "coordinates":[-98.7015,36.4561,3.16]
   *                    },
   *                    "id":"us10004vsc"
   *                 },
   * */
   */
  public final double lat;
  public final double lon;

  public Hypocenter(double lat, double lon) {
    this.lat = lat;
    this.lon = lon;
  }
}
