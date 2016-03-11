package gov.usgs.volcanoes.swarm.wave;



import java.awt.Color;

import javax.swing.JComponent;

import gov.usgs.plot.data.Wave;
import gov.usgs.plot.decorate.FrameDecorator;
import gov.usgs.plot.render.wave.SliceWaveRenderer;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;

public abstract class WavePanel extends JComponent {

  protected static SwarmConfig swarmConfig;
  /**
   * X pixel location of where the main plot axis should be located on the
   * component.
   */
  protected int xOffset = 60;
  /**
   * Y pixel location of where the main plot axis should be located on the
   * component.
   */
  protected int yOffset = 20;
  /** The amount of padding space on the right side. */
  protected int rightWidth = 20;
  /** The amount of padding space on the bottom. */
  protected int bottomHeight = 20;
  protected FrameDecorator decorator;
  protected SliceWaveRenderer waveRenderer;
  protected Wave wave;
  protected double startTime;
  protected double endTime;
  protected WaveViewSettings settings;
  protected String channel;
  /**
   * The data source to use for zoom drags. This should probably be moved from
   * this class to follow a stricter interpretation of MVC.
   */
  protected SeismicDataSource source;
  /**
   * A flag to indicate wheter the plot should display a title. Currently used
   * when the plot is on the clipboard or monitor.
   */
  protected boolean displayTitle;
  protected Color backgroundColor;
  protected double[] translation;

  public WavePanel() {
    super();
  }

  public WavePanel(WavePanel p) {
    swarmConfig = SwarmConfig.getInstance();
    channel = p.channel;
    source = p.source;
    startTime = p.startTime;
    endTime = p.endTime;
//    bias = p.bias;
//    maxSpectraPower = p.maxSpectraPower;
//    maxSpectrogramPower = p.maxSpectrogramPower;
//    translation = new double[8];
//    if (p.translation != null)
//      System.arraycopy(p.translation, 0, translation, 0, 8);
//    timeSeries = p.timeSeries;
//    allowDragging = p.allowDragging;
    settings = new WaveViewSettings(p.settings);
    wave = p.wave;
    displayTitle = p.displayTitle;
    backgroundColor = p.backgroundColor;
//    setupMouseHandler();
//    processSettings();

  }

  /**
   * Gets the translation info for this panel. The translation info is used to
   * convert from pixel coordinates on the panel into time or data
   * coordinates.
   * 
   * @return the transformation information
   */
  public double[] getTranslation() {
    return translation;
  }

}
