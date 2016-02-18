package gov.usgs.volcanoes.swarm.picker;

public class Phase {
  public static enum Onset {
    i, p
  }

  public static enum PhaseType {
    P, S
  }

  public static enum FirstMotion {
    UP, DOWN, COMPRESSION, DIALATION, POOR_UP, POOR_DOWN, POOR_COMPRESSION, POOR_DIALATION, NOISY, NOT_READABLE;
  }

  public final String channel;
  public final Onset onset;
  public final PhaseType phaseType;
  public final FirstMotion firstMotion;
  public final int weight;
  public final long time;
  public final int maxAmplitude;
  public final long duration;

  public static class Builder {
    private String channel;
    private Onset onset;
    private PhaseType phaseType;
    private FirstMotion firstMotion;
    private int weight;
    private long time;
    private int maxAmplitude;
    private long duration;

    public Builder() {}

    public Builder channel(String channel) {
      this.channel = channel;
      return this;
    }

    public Builder onset(Onset onset) {
      this.onset = onset;
      return this;
    }

    public Builder phaseType(PhaseType phaseType) {
      this.phaseType = phaseType;
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

    public Builder maxAmplitude(int maxAmplitude) {
      this.maxAmplitude = maxAmplitude;
      return this;
    }

    public Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    public Phase build() {
      return new Phase(this);
    }

  }

  private Phase(Builder builder) {
    channel = builder.channel;
    onset = builder.onset;
    phaseType = builder.phaseType;
    firstMotion = builder.firstMotion;
    weight = builder.weight;
    time = builder.time;
    maxAmplitude = builder.maxAmplitude;
    duration = builder.duration;
  }

  public String tag() {
    StringBuffer sb = new StringBuffer();
    sb.append(onset).append(phaseType).append(weight);
    
    return sb.toString();
  }
}
