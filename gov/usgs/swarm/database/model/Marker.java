package gov.usgs.swarm.database.model;

import gov.usgs.swarm.Swarm;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.TypedQuery;


/**
 * A Marker object that holds information about markers(p and s components, coda's, azimuths, particle motion) placed on
 * wave component
 * 
 * @author Chirag Patel
 */
@Entity
public class Marker implements Serializable {

    public static final Color P_MARKER_COLOR = new Color(100);
    public static final Color S_MARKER_COLOR = new Color(212);
    public static final Color CODA_MARKER_COLOR = new Color(333);
    public static final Color AZIMUTH_MARKER_COLOR = new Color(440);
    public static final Color PARTICLE_MARKER_COLOR = new Color(505);

    public static final String P_MARKER_LABEL = "Phase(P)";
    public static final String S_MARKER_LABEL = "Phase(S)";
    public static final String CODA_MARKER_LABEL = "Coda";
    public static final String AZIMUTH_MARKER_LABEL = "Azimuth";
    public static final String PARTICLE_MARKER_LABEL = "Particle Motion";

    @Id @GeneratedValue(strategy = GenerationType.TABLE) private Integer id;

    private String fileName;
    private String filePath;
    private String fileType;
    private Timestamp markerTime;
    private String upDownUnknown;
    private String ip_ep;
    private String is_es;
    private String markerType;
    private Integer fileIndex;
    private String station;
    private Integer weight;

    @Column(name = "attempt_id") private Integer attempt;

    public Marker() {

    }

    public static final EntityManager entityManager() {
        EntityManager em = Swarm.em;
        if (em == null)
            throw new IllegalStateException(
                    "Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Timestamp getMarkerTime() {
        return markerTime;
    }

    public void setMarkerTime(Timestamp markerTime) {
        this.markerTime = markerTime;
    }

    public String getUpDownUnknown() {
        return upDownUnknown;
    }

    public void setUpDownUnknown(String upDownUnknown) {
        this.upDownUnknown = upDownUnknown;
    }

    public String getIp_ep() {
        return ip_ep;
    }

    public void setIp_ep(String ip_ep) {
        this.ip_ep = ip_ep;
    }

    public String getIs_es() {
        return is_es;
    }

    public void setIs_es(String is_es) {
        this.is_es = is_es;
    }

    public String getMarkerType() {
        return markerType;
    }

    public void setMarkerType(String markerType) {
        this.markerType = markerType;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getFileIndex() {
        return fileIndex;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public void setFileIndex(Integer fileIndex) {
        this.fileIndex = fileIndex;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    /**
     * Saves a {@link Marker} object to the database
     */
    public void persist() {
        if (entityManager().getTransaction().isActive()) {
            entityManager().getTransaction().commit();
        }
        entityManager().getTransaction().begin();
        entityManager().persist(this);
        entityManager().getTransaction().commit();
    }

    /**
     * Returns a {@link Marker} object given the Id
     * 
     * 
     * @param id
     *            : ID of Marker Object
     * @return
     */
    public static Marker find(Integer id) {
        return entityManager().find(Marker.class, id);
    }

    /**
     * Deletes a {@link Marker} from the database
     */
    public void delete() {
        if (entityManager().getTransaction().isActive()) {
            entityManager().getTransaction().commit();
        }
        entityManager().getTransaction().begin();
        Marker attached = entityManager().find(Marker.class, id);
        entityManager().remove(attached);
        entityManager().getTransaction().commit();
    }

    /**
     * Gets all {@link Marker} objects related to a specifed Attempt from the database
     * 
     * @param id
     *            : ID of specified {@link Attempt} object
     * @return
     */
    public static List<Marker> listByAttempt(Integer id) {
        return entityManager().createQuery("SELECT o FROM Marker o where o.attempt = :id", Marker.class)
                .setParameter("id", id).getResultList();
    }

    /**
     * Gets all {@link Marker} objects of a particular type associated with a particular Attempt and station <br />
     * Marker types includes :
     * <ul>
     * <li>Phase(P)</li>
     * <li>Phase(S)</li>
     * <li>Coda</li>
     * <li>Azimuth</li>
     * <li>Particle Motion</li>
     * </ul>
     * 
     * @param id
     *            : ID of associated {@link Attempt} object
     * @param station
     *            : station component
     * @param markerType
     *            : specified marker type
     * @return
     */
    public static List<Marker> listByStationAndTypeAndAttempt(Integer id, String station, String markerType) {
        return entityManager()
                .createQuery(
                        "SELECT o FROM Marker o where o.attempt = :id and o.station = :station and o.markerType = :markerType",
                        Marker.class).setParameter("id", id).setParameter("station", station)
                .setParameter("markerType", markerType).getResultList();
    }

    /**
     * List all station components associated with a particular Attempt
     * 
     * 
     * @param id
     *            : ID of associated {@link Attempt} object
     * @return
     */
    public static List<String> listStationByAttempt(Integer id) {
        return entityManager()
                .createQuery("SELECT o.station FROM Marker o where o.attempt = :id group by o.station", String.class)
                .setParameter("id", id).getResultList();
    }

    /**
     * Gets all filePaths/fileTypes for wave files that is associated with a specified Attempt
     * 
     * 
     * @param id
     *            : ID of associated {@link Attempt} object
     * @return
     */
    public static List<Object[]> listMarkerByAttempt(Integer id) {
        return entityManager()
                .createQuery("SELECT o.filePath, o.fileType FROM Marker o where o.attempt = :id group by o.filePath",
                        Object[].class).setParameter("id", id).getResultList();
    }

    /**
     * 
     * Gets all {@link Marker} objects associated with a specified channel index, wavefile and attempt
     * 
     * @param fileIndex
     *            : Channel index of wave gotten from wave file
     * @param filePath
     *            : Path to file containing wave data
     * @param id
     *            : ID of associated {@link Attempt} object
     * @return
     */
    public static List<Marker> listByFileAndIndexAndAttempt(Integer fileIndex, String filePath, Integer id) {
        return entityManager()
                .createQuery(
                        "SELECT o FROM Marker o where o.filePath = :filePath and  o.fileIndex = :fileIndex and o.attempt = :id",
                        Marker.class).setParameter("id", id).setParameter("filePath", filePath)
                .setParameter("fileIndex", fileIndex).getResultList();
    }

    /**
     * Gets all {@link Attempt} objects that contains P-markers(associated with a specified event, start and end time
     * within wave frame if specified)
     * 
     * 
     * @param eventType
     *            : Event type
     * @param fromTime
     *            : Start time for Wave
     * @param endTime
     *            : End time for wave
     * @return
     */
    public static List<Attempt> getAttemptsByPMarker(String eventType, Timestamp fromTime, Timestamp endTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT o, a, e  FROM Marker as o, Attempt as a, Event as e where o.markerType =:markerType  and o.attempt = a.id and a.event = e.id ");

        if (eventType != null && eventType.trim().length() > 0) {
            sb.append(" and e.eventType =:eventType ");
        }
        if (fromTime != null) {
            sb.append(" and o.markerTime >=:fromTime ");
        }
        if (endTime != null) {
            sb.append(" and o.markerTime <=:endTime ");
        }

        sb.append(" group by o.attempt, o.id");
        TypedQuery<Object[]> attemptQuery = entityManager().createQuery(sb.toString(), Object[].class);
        attemptQuery.setParameter("markerType", P_MARKER_LABEL);
        if (eventType != null && eventType.trim().length() > 0) {
            attemptQuery.setParameter("eventType", eventType);
        }
        if (fromTime != null) {
            attemptQuery.setParameter("fromTime", fromTime);
        }
        if (endTime != null) {
            attemptQuery.setParameter("endTime", endTime);
        }

        List<Object[]> results = attemptQuery.getResultList();
        List<Attempt> attempts = new ArrayList<Attempt>();
        for (Object[] data : results) {
            attempts.add((Attempt)data[1]);
        }

        return attempts;
    }

    public static List<Attempt> getAttemptsBySMarker(String eventType, Timestamp fromTime, Timestamp endTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT o, a, e  FROM Marker as o, Attempt as a, Event as e where o.markerType =:markerType  and o.attempt = a.id and a.event = e.id ");

        if (eventType != null && eventType.trim().length() > 0) {
            sb.append(" and e.eventType =:eventType ");
        }
        if (fromTime != null) {
            sb.append(" and o.markerTime >=:fromTime ");
        }
        if (endTime != null) {
            sb.append(" and o.markerTime <=:endTime ");
        }

        sb.append(" group by o.attempt, o.id");
        TypedQuery<Object[]> attemptQuery = entityManager().createQuery(sb.toString(), Object[].class);
        attemptQuery.setParameter("markerType", S_MARKER_LABEL);
        if (eventType != null && eventType.trim().length() > 0) {
            attemptQuery.setParameter("eventType", eventType);
        }
        if (fromTime != null) {
            attemptQuery.setParameter("fromTime", fromTime);
        }
        if (endTime != null) {
            attemptQuery.setParameter("endTime", endTime);
        }

        List<Object[]> results = attemptQuery.getResultList();
        List<Attempt> attempts = new ArrayList<Attempt>();
        for (Object[] data : results) {
            attempts.add((Attempt)data[1]);
        }

        return attempts;
    }

    public static List<Attempt> getAttemptsByCODAMarker(String eventType, Timestamp fromTime, Timestamp endTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT o, a, e  FROM Marker as o, Attempt as a, Event as e where o.markerType =:markerType  and o.attempt = a.id and a.event = e.id ");

        if (eventType != null && eventType.trim().length() > 0) {
            sb.append(" and e.eventType =:eventType ");
        }
        if (fromTime != null) {
            sb.append(" and o.markerTime >=:fromTime ");
        }
        if (endTime != null) {
            sb.append(" and o.markerTime <=:endTime ");
        }

        sb.append(" group by o.attempt, o.id");
        TypedQuery<Object[]> attemptQuery = entityManager().createQuery(sb.toString(), Object[].class);
        attemptQuery.setParameter("markerType", CODA_MARKER_LABEL);
        if (eventType != null && eventType.trim().length() > 0) {
            attemptQuery.setParameter("eventType", eventType);
        }
        if (fromTime != null) {
            attemptQuery.setParameter("fromTime", fromTime);
        }
        if (endTime != null) {
            attemptQuery.setParameter("endTime", endTime);
        }

        List<Object[]> results = attemptQuery.getResultList();
        List<Attempt> attempts = new ArrayList<Attempt>();
        for (Object[] data : results) {
            attempts.add((Attempt)data[1]);
        }

        return attempts;
    }

    public static List<Attempt> getAttemptsByAZIMUTHMarker(String eventType, Timestamp fromTime, Timestamp endTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT o, a, e  FROM Marker as o, Attempt as a, Event as e where o.markerType =:markerType  and o.attempt = a.id and a.event = e.id ");

        if (eventType != null && eventType.trim().length() > 0) {
            sb.append(" and e.eventType =:eventType ");
        }
        if (fromTime != null) {
            sb.append(" and o.markerTime >=:fromTime ");
        }
        if (endTime != null) {
            sb.append(" and o.markerTime <=:endTime ");
        }

        sb.append(" group by o.attempt, o.id");
        TypedQuery<Object[]> attemptQuery = entityManager().createQuery(sb.toString(), Object[].class);
        attemptQuery.setParameter("markerType", AZIMUTH_MARKER_LABEL);
        if (eventType != null && eventType.trim().length() > 0) {
            attemptQuery.setParameter("eventType", eventType);
        }
        if (fromTime != null) {
            attemptQuery.setParameter("fromTime", fromTime);
        }
        if (endTime != null) {
            attemptQuery.setParameter("endTime", endTime);
        }

        List<Object[]> results = attemptQuery.getResultList();
        List<Attempt> attempts = new ArrayList<Attempt>();
        for (Object[] data : results) {
            attempts.add((Attempt)data[1]);
        }

        return attempts;
    }

    public static List<Attempt> getAttemptsByPARTICLEMarker(String eventType, Timestamp fromTime, Timestamp endTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT o, a, e  FROM Marker as o, Attempt as a, Event as e where o.markerType =:markerType  and o.attempt = a.id and a.event = e.id ");

        if (eventType != null && eventType.trim().length() > 0) {
            sb.append(" and e.eventType =:eventType ");
        }
        if (fromTime != null) {
            sb.append(" and o.markerTime >=:fromTime ");
        }
        if (endTime != null) {
            sb.append(" and o.markerTime <=:endTime ");
        }

        sb.append(" group by o.attempt, o.id");
        TypedQuery<Object[]> attemptQuery = entityManager().createQuery(sb.toString(), Object[].class);
        attemptQuery.setParameter("markerType", PARTICLE_MARKER_LABEL);
        if (eventType != null && eventType.trim().length() > 0) {
            attemptQuery.setParameter("eventType", eventType);
        }
        if (fromTime != null) {
            attemptQuery.setParameter("fromTime", fromTime);
        }
        if (endTime != null) {
            attemptQuery.setParameter("endTime", endTime);
        }

        List<Object[]> results = attemptQuery.getResultList();
        List<Attempt> attempts = new ArrayList<Attempt>();
        for (Object[] data : results) {
            attempts.add((Attempt)data[1]);
        }

        return attempts;
    }

    /**
     * Gets all {@link Attempt} Objects that contains P markers with specified location and depth within a specified
     * depth range
     * 
     * 
     * @param minDepth
     *            : Minimum depth for depth range
     * @param maxDepth
     *            : Maximum depth for depth range
     * @param longLat
     *            : Specified Location information(included longitude and latitude information)
     * @return
     */
    public static List<Attempt> getAttemptsByPMarkerAndLocationAndDepth(Double minDepth, Double maxDepth,
            Point2D.Double longLat) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT o, a, e  FROM Marker as o, Attempt as a, Event as e where o.markerType =:markerType  and o.attempt = a.id and a.event = e.id ");
        if (minDepth != null) {
            sb.append(" and a.depth >=:minDepth ");
        }
        if (maxDepth != null) {
            sb.append(" and a.depth >=:maxDepth ");
        }
        if (longLat != null) {
            sb.append(" and a.longitude =:longitude and a.latitude =:latitude ");
        }

        sb.append("  group by o.attempt, o.id");
        TypedQuery<Object[]> attemptQuery = entityManager().createQuery(sb.toString(), Object[].class);
        attemptQuery.setParameter("markerType", P_MARKER_LABEL);
        if (minDepth != null) {
            attemptQuery.setParameter("minDepth", minDepth);
        }
        if (maxDepth != null) {
            attemptQuery.setParameter("maxDepth", maxDepth);
        }
        if (longLat != null) {
            attemptQuery.setParameter("latitude", longLat.x);
            attemptQuery.setParameter("longitude", longLat.x);
        }

        List<Object[]> results = attemptQuery.getResultList();
        List<Attempt> attempts = new ArrayList<Attempt>();
        for (Object[] data : results) {
            attempts.add((Attempt)data[1]);
        }

        return attempts;
    }

}