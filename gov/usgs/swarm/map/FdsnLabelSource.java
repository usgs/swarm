package gov.usgs.swarm.map;

import java.awt.geom.Point2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.stream.XMLStreamException;

import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.fdsnws.FDSNEventQuerier;
import edu.sc.seis.seisFile.fdsnws.FDSNEventQueryParams;
import edu.sc.seis.seisFile.fdsnws.FDSNWSException;
import edu.sc.seis.seisFile.fdsnws.quakeml.Event;
import edu.sc.seis.seisFile.fdsnws.quakeml.EventIterator;
import edu.sc.seis.seisFile.fdsnws.quakeml.Magnitude;
import edu.sc.seis.seisFile.fdsnws.quakeml.Origin;
import edu.sc.seis.seisFile.fdsnws.quakeml.QuakeMLTagNames;
import edu.sc.seis.seisFile.fdsnws.quakeml.Quakeml;
import gov.usgs.proj.GeoRange;
import gov.usgs.util.Time;

/*
 * A class to retrieve events from IRIS for plotting on the map.
 * 
 * @author: Tom Parker
 */
public class FdsnLabelSource implements LabelSource {


    public List<? extends ClickableGeoLabel> getLabels() {

        FDSNEventQueryParams queryParams = new FDSNEventQueryParams();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        MapFrame mapFrame = MapFrame.getInstance();
        if (mapFrame == null)
            return null;
        GeoRange range = mapFrame.getRange();
        
        float minLat = (float)range.getSouth();
        float maxLat = (float)range.getNorth();
        float minLon = (float)range.getWest();
        float maxLon = (float)range.getEast();
        
        Date endTime = new Date(System.currentTimeMillis());
        Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
        
        queryParams.area(minLat, maxLat, minLon, maxLon)
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setMaxDepth(100)
                .setMinMagnitude(1)
                .setOrderBy(FDSNEventQueryParams.ORDER_TIME_ASC);
        
        FDSNEventQuerier querier = new FDSNEventQuerier(queryParams);
//System.err.println("::" + querier.getConnectionUri());
        Quakeml quakeml = null;
        try {
            quakeml = querier.getQuakeML();
        } catch (FDSNWSException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        if (!quakeml.checkSchemaVersion()) {
            System.out.println("");
            System.out.println("WARNING: XmlSchema of this document does not match this code, results may be incorrect.");
            System.out.println("XmlSchema (code): " + QuakeMLTagNames.CODE_MAIN_SCHEMA_VERSION);
            System.out.println("XmlSchema (doc): " + quakeml.getSchemaVersion());
        }
        
        List<Hypocenter> hypos = new ArrayList<Hypocenter>();
        EventIterator eIt = quakeml.getEventParameters().getEvents();
        try {
            while (eIt.hasNext()) {
                Event e = eIt.next();
                Origin o = e.getOriginList().get(0);
                Magnitude m = e.getMagnitudeList().get(0);
                System.out.println(o.getLatitude()+"/"+o.getLongitude()+" "+m.getMag().getValue()+" "+m.getType()+" "+o.getTime().getValue());
              Hypocenter h = new Hypocenter();
              double lat = o.getLatitude().getValue();
              double lon = o.getLongitude().getValue();
              h.location = new Point2D.Double(lon, lat);
              h.text = "M" + m.getMag().getValue();
              h.depth = o.getDepth().getValue();
              h.time = Time.parse(Time.FDSN_TIME_FORMAT, o.getTime().getValue());
              hypos.add(h);

            }
        } catch (XMLStreamException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SeisFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return hypos;
    }
}
