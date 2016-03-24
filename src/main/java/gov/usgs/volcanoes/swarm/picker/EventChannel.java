package gov.usgs.volcanoes.swarm.picker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventChannel {
  private final static Logger LOGGER = LoggerFactory.getLogger(EventChannel.class);
  private Phase pPhase;
  private Phase sPhase;
  private long codaTime;

  private int maxAmplitude;
  private long duration;

  public void setPhase(Phase phase) {
    switch (phase.phaseType) {
      case P:
        pPhase = phase;
        break;
      case S:
        sPhase = phase;
        break;
      default:
        throw new RuntimeException("Unknown phase type.");
    }
  }

  public void clearPhase(Phase.PhaseType type) {
    switch (type) {
      case P:
        pPhase = null;
        codaTime = 0;
        break;
      case S:
        sPhase = null;
        break;
      default:
        throw new RuntimeException("Unknown phase type.");
    }
  }

  public Phase getPhase(Phase.PhaseType type) {
    switch (type) {
      case P:
        return pPhase;
      case S:
        return sPhase;
      default:
        throw new RuntimeException("Unknown phase type.");
    }
  }

  public boolean isEmpty() {
    return pPhase == null && sPhase == null;
  }

  public void setCoda(long time) {
    codaTime = time;
  }

  public long getCodaTime() {
    return codaTime;
  }
}
