package gov.usgs.volcanoes.swarm.picker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Event {
  private static final Logger LOGGER = LoggerFactory.getLogger(Event.class);
  private final Map<String, EventChannel> channels;
  private final List<EventObserver> observers;

  public Event() {
    channels = new HashMap<String, EventChannel>();
    observers = new ArrayList<EventObserver>();
  }

  public void setPhase(String channel, Phase phase) {
    EventChannel eChan = channels.get(channel);
    if (eChan == null) {
      eChan = new EventChannel();
      channels.put(channel, eChan);
    }
    eChan.setPhase(phase);
    notifyObservers();
  }

  public void clearPhase(String channel, Phase.PhaseType type) {
    EventChannel eChan = channels.get(channel);
    eChan.clearPhase(type);
    notifyObservers();
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
}
