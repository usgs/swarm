package gov.usgs.volcanoes.swarm.picker;

import gov.usgs.volcanoes.swarm.picker.Phase.Builder;
import gov.usgs.volcanoes.swarm.picker.Phase.FirstMotion;

public class Phase {
  public static enum Onset {
    i, e
  }

  public static enum PhaseType {
    P, S
  }

  public static enum FirstMotion {
    UP, DOWN, COMPRESSION, DIALATION, POOR_UP, POOR_DOWN, POOR_COMPRESSION, POOR_DIALATION, NOISY, NOT_READABLE;
  }

  public final Onset onset;
  public final PhaseType phaseType;
  public final FirstMotion firstMotion;
  public final int weight;
  public final long time;

  public static class Builder {
    private String channel;
    private Onset onset;
    private PhaseType phaseType;
    private FirstMotion firstMotion;
    private int weight;
    private long time;

    public Builder() {}
    public Builder(Phase phase) {
      onset = phase.onset;
      phaseType = phase.phaseType;
      firstMotion = phase.firstMotion;
      weight = phase.weight;
      time = phase.time;
    }
    
    public Builder onset(Onset onset) {
      this.onset = onset;
      return this;
    }

    public Builder phaseType(PhaseType phaseType) {
      this.phaseType = phaseType;
      return this;
    }

    public Builder firstMotion(FirstMotion firstMotion) {
      this.firstMotion = firstMotion;
      return this;
    }

    public Builder weight(int weight) {
      this.weight = weight;
      return this;
    }

    public Builder time(long time) {
      this.time = time;
      return this;
    }

    public Phase build() {
      return new Phase(this);
    }

  }

  private Phase(Builder builder) {
    onset = builder.onset;
    phaseType = builder.phaseType;
    firstMotion = builder.firstMotion;
    weight = builder.weight;
    time = builder.time;
  }

  public String tag() {
    StringBuffer sb = new StringBuffer();
    sb.append(onset).append(phaseType).append(weight);
    
    return sb.toString();
  }
}
