package gov.usgs.volcanoes.swarm.picker;

public class EventChannel {
  private Phase pPhase;
  private Phase sPhase;
  
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
}
