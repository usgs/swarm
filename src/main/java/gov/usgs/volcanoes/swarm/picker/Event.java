package gov.usgs.volcanoes.swarm.picker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Event {
  private static final Logger LOGGER = LoggerFactory.getLogger(Event.class);
  private final Map<String, EventChannel> channels;
  private final List<EventObserver> observers;

  public Event() {
    channels = new LinkedHashMap<String, EventChannel>();
    observers = new ArrayList<EventObserver>();
  }

  public void setPhase(String channel, Phase phase) {
    EventChannel eChan = channels.get(channel);
    if (eChan == null) {
      eChan = new EventChannel();
      channels.put(channel, eChan);
    }
    eChan.setPhase(phase);
    if (eChan.isEmpty()) {
      channels.remove(channel);
    }
    notifyObservers();
  }

  public void clearPhase(String channel, Phase.PhaseType type) {
    EventChannel eChan = channels.get(channel);
    if (eChan != null) {
      eChan.clearPhase(type);
      if (eChan.isEmpty()) {
        channels.remove(channel);
      }
      notifyObservers();
    }
  }

  public Phase getPhase(String channel, Phase.PhaseType type) {
    EventChannel eChan = channels.get(channel);
    if (eChan != null) {
      return eChan.getPhase(type);
    } else {
      return null;
    }
  }

  public Map<String, EventChannel> getChannels() {
    return channels;
  }

  public void addObserver(EventObserver observer) {
    observers.add(observer);
  }

  public void notifyObservers() {
    for (EventObserver observer : observers) {
      observer.updateEvent();
    }
  }

  public void remove(String channel) {
    channels.remove(channel);
    notifyObservers();
  }

  public boolean isEmpty() {
    return channels.isEmpty();
  }

  public void setCoda(String channel, long time) {
    EventChannel eChan = channels.get(channel);
    if (eChan != null) {
      eChan.setCoda(time);
    }
  }

  public long coda(String channel) {
    EventChannel eChan = channels.get(channel);
    return eChan.getCodaTime();
  }
}
