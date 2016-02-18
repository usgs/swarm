package gov.usgs.volcanoes.swarm.picker;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class Event {
  private List<Phase> phases;
  
  public Event() {
    phases = new ArrayList<Phase>();
  }
  
  public void addPhase(Phase phase) {
    phases.add(phase);
  }

  public void removePhase(Phase phase) {
    phases.remove(phase);
  }

  public ListIterator<Phase> phasesIt() {
    return phases.listIterator();
  }

}
