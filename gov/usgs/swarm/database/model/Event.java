package gov.usgs.swarm.database.model;

import gov.usgs.swarm.Swarm;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Event object that holds information about an event.
 * 
 * @author Chirag Patel
 */
@Entity
public class Event
{
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private Integer id;
	
	private String eventLabel;
	
	
	private String eventType;

	public Event()
	{
	}

	public Event(String userLabel, String eventType)
	{
		super();
		this.eventLabel = userLabel;
		this.eventType = eventType;
	}

	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public String getEventLabel()
	{
		return eventLabel;
	}

	public void setEventLabel(String eventLabel)
	{
		this.eventLabel = eventLabel;
	}

	public String getEventType()
	{
		return eventType;
	}

	public void setEventType(String eventType)
	{
		this.eventType = eventType;
	}
	
	
	public static final EntityManager entityManager() {
		EntityManager em = Swarm.em;
        if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

	
	/**
	 * Saves and Event object to the database
	 * 
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
	 * Returns an Event object given the event Id
	 * 
	 * @param id : ID of event object
	 * @return
	 */
	public static Event find(Integer id) {
		return entityManager().find(Event.class, id);
	}
	
	
	/**
	 * Gets all Event objects of a particular event type
	 * 
	 * @param type : Specified event type
	 * @return
	 */
	public static List<Event> listByType(String type) {
		if(type != null && type.trim().length() > 0){
			return entityManager().createQuery("SELECT o FROM Event o where o.eventType = :type", Event.class).setParameter("type", type).getResultList();
		}else{
			return entityManager().createQuery("SELECT o FROM Event o", Event.class).getResultList();

		}
    }
	
}