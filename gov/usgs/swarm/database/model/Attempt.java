package gov.usgs.swarm.database.model;

import gov.usgs.swarm.Swarm;

import java.io.IOException;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.eclipse.persistence.internal.helper.SerializationHelper;

/**
 * A Attempt object that holds information about markers(p and s components, coda's, azimuths, particle motion) placed on wave component as well 
 * as calculated location to events
 * 
 * @author Chirag Patel
 */
@Entity
public class Attempt {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private Integer id;

	private Boolean preferred;
	private Double longitude;
	private Double latitude;
	private Long duration;
	private Double depth;
	private String hypoInputFilePath;
	private String hypoInputArchiveFilePath;

	@Lob
	private byte[] hypoResults;
	
	@Column(name="event_id")
	private Integer event;
	
	
	public Attempt() {
	}

	public Attempt(Boolean preferred, Long duration) {
		super();
		this.preferred = preferred;
		this.duration = duration;
	}

	public Attempt(Integer attemptId, Boolean preferred, Double longitude,
			Double latitude, Long duration) {
		super();
		this.id = attemptId;
		this.preferred = preferred;
		this.longitude = longitude;
		this.latitude = latitude;
		this.duration = duration;
	}

	public static final EntityManager entityManager() {
		EntityManager em = Swarm.em;
		if (em == null)
			throw new IllegalStateException(
					"Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
		return em;
	}

	
	
	
	public HypoResults getHypoResultsAsObject() {
        try {
			return (HypoResults) SerializationHelper.deserialize(this.hypoResults);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not read hypo results");
		}
    }

    public void setHypoResultsAsBytes(HypoResults reults) throws IOException {
        this.hypoResults = SerializationHelper.serialize((Serializable) reults);
        System.out.println( this.hypoResults.length);
    }
	
    

	public Double getDepth() {
		return depth;
	}

	public void setDepth(Double depth) {
		this.depth = depth;
	}

	public byte[] getHypoResults() {
		return hypoResults;
	}

	public void setHypoResults(byte[] hypoResults) {
		this.hypoResults = hypoResults;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Boolean getPreferred() {
		return preferred;
	}

	public void setPreferred(Boolean preferred) {
		this.preferred = preferred;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Long getDuration() {
		return duration;
	}
	
	

	public String getHypoInputArchiveFilePath() {
		return hypoInputArchiveFilePath;
	}

	public void setHypoInputArchiveFilePath(String hypoInputArchiveFilePath) {
		this.hypoInputArchiveFilePath = hypoInputArchiveFilePath;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public Integer getEvent() {
		return event;
	}

	public void setEvent(Integer event) {
		this.event = event;
	}

	public String getHypoInputFilePath() {
		return hypoInputFilePath;
	}

	public void setHypoInputFilePath(String hypoInputFilePath) {
		this.hypoInputFilePath = hypoInputFilePath;
	}

	/**
	 * Saves an Attempt object to the database
	 */
	public void persist() {
		if(entityManager().getTransaction().isActive()){
			entityManager().getTransaction().commit();
		}
		entityManager().getTransaction().begin();
		entityManager().persist(this);
		entityManager().getTransaction().commit();
	}

	/**
	 * Gets a specified Attempt object given the attempt ID
	 * 
	 * @param id : Id of Attempt Object
	 * @return
	 */
	public static Attempt find(Integer id) {
		return entityManager().find(Attempt.class, id);
	}

	
	/**
	 * 
	 * Deletes a specified Attempt object given the attempt ID
	 * 
	 * 
	 * @param id : ID of Attempt Object
	 */
	public static void delete(Integer id) {
		if(entityManager().getTransaction().isActive()){
			entityManager().getTransaction().commit();
		}
		entityManager().getTransaction().begin();
		Attempt attempt = Attempt.find(id);
		entityManager().remove(attempt);
		entityManager().getTransaction().commit();
	}

}