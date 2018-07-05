package gov.usgs.volcanoes.swarm.event;

import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.Version;
import gov.usgs.volcanoes.swarm.heli.HelicorderViewPanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Right click menu for picks. 
 * 
 * @author Diana Norgaard
 */
public class TagMenu extends JPopupMenu  {


  private static final String settingsFileName = "EventClassifications.config";
  private static final long serialVersionUID = 8681764007165352268L;

  protected static final int transparency = 180;
  protected static final Color defaultColor = new Color(255, 69, 0, transparency); 
  protected static HashMap<String, Color> colors = new HashMap<String, Color>();
  
  private String eventFileName;

  protected static String[] classifications;
  private static String[] defaultClassifications = {
      "VT",
      "VT - distal",
      "VT - proximal",
      "LP",
      "VLP",
      "Hybrid",
      "Explosion",
      "Tremor",
      "Lahar",
      "Pyroclastic Flow",
      "Regional",
      "Rock Fall",
      "Teleseism",
      "Ice quake",
      "Noise",
      "Cultural",
      "Unclassified"
  };
  
  static {
    try {
      ArrayList<String> list = new ArrayList<String>();
      FileReader fr = new FileReader(settingsFileName);
      BufferedReader br = new BufferedReader(fr);
      String line;
      while ((line = br.readLine()) != null) {
        String[] tmp = line.trim().split(",");
        String classification = tmp[0].trim();
        list.add(classification);
        Color color = defaultColor;
        if (tmp.length > 1) {
          color = Color.decode(tmp[1].trim());
        }
        colors.put(classification, color);
      }
      br.close();
      fr.close();
      classifications = new String[list.size()];
      list.toArray(classifications);
    } catch (Exception e) {
      System.err
          .println("Error reading EventClassifications.config. Using default classifications.");
      classifications = defaultClassifications;
      for (String c : defaultClassifications) {
        colors.put(c, defaultColor);        
      }
    }


  }
  
  private HelicorderViewPanel hvp;
  private double j2k;
  
  /**
   * Constructor.
   */
  public TagMenu(HelicorderViewPanel hvp) {
    super("Tag Menu");
    this.hvp = hvp;
    createMenu();
    eventFileName = SwarmConfig.getInstance().lastPath + "/Swarm" + Version.POM_VERSION
        + "_Events_" + SwarmConfig.getInstance().getUser() + "_" + System.currentTimeMillis()
        + ".csv";
  }

  /**
   * Browse for file to save event tags to.
   */
  public void browseForEventFileName() {
    final FileFilter filter = new FileNameExtensionFilter("CSV (.csv)", "CSV");

    JFileChooser chooser = new JFileChooser();
    chooser.setCurrentDirectory(new File(SwarmConfig.getInstance().lastPath));
    if (eventFileName != null) {
      chooser.setSelectedFile(new File(eventFileName));
    }
    if (filter != null) {
      chooser.setFileFilter(filter);
    }
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(false);
    int result = JFileChooser.CANCEL_OPTION;
    chooser.setDialogTitle("Open File...");
    result = chooser.showOpenDialog(Swarm.getApplicationFrame());
    if (result == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      SwarmConfig.getInstance().lastPath = file.getParent();
      eventFileName = file.getAbsolutePath();
      loadEventFile();
    } else {
      hvp.getFrame().disableTag();
    }
  }
  
  /**
   * Load tags from event file.
   */
  private void loadEventFile() {
    FileReader fr;
    try {
      fr = new FileReader(eventFileName);
      BufferedReader br = new BufferedReader(fr);
      String line;
      while ((line = br.readLine()) != null) {
        TagData tag = new TagData(line);
        hvp.getTagData().add(tag);
      }
      br.close();
      fr.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      //e.printStackTrace();
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  /**
   * Write tag data to file.
   */
  public synchronized void write() {
    FileWriter fw;
    try {
      fw = new FileWriter(eventFileName, false);
      BufferedWriter bw = new BufferedWriter(fw);
      ArrayList<TagData> tagData = hvp.getTagData();
      Collections.sort(tagData, new TagData());
      for (TagData data : tagData) {
        bw.write(data.toString() + "\n");
      }
      bw.close();
      fw.close();
    } catch (IOException e) {
      JOptionPane.showMessageDialog(Swarm.getApplicationFrame(), e.getMessage(),
          "Error writing tags", JOptionPane.ERROR_MESSAGE);
    }
  }
  
  /**
   * Create right click menu for pick.
   */
  private void createMenu() {
    createClassificationMenus();
    this.addSeparator();
    createClearMenu();
  }
 
  /**
   * Classification menus.
   */
  private void createClassificationMenus() {
    for (final String c : classifications) {
      JMenuItem menuItem = new JMenuItem(c);
      menuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          TagData tag = new TagData(hvp.getSettings().channel, j2k, c);
          hvp.getTagData().add(tag);
          hvp.repaint();
          WaveViewPanel wvp = hvp.getInsetPanel();
          if (wvp != null) {
            wvp.repaint();
          }
          write();
        }
      });

      this.add(menuItem);
    }
  }
  
  /**
   * Clear menu.
   */
  private void createClearMenu() {
    JMenuItem menuItem = new JMenuItem("Clear");
    menuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        double mindiff = Double.MAX_VALUE;
        TagData deleteTag = null;
        for (TagData tag : hvp.getTagData()) { // get closest within
          if (tag.channel.equals(hvp.getSettings().channel)) {
            double diff = Math.abs(j2k - tag.startTime);
            if (diff < 60 && diff < mindiff) {
              mindiff = diff;
              deleteTag = tag;
            }
          }
        }
        if (deleteTag != null) {
          int result = JOptionPane.showConfirmDialog(hvp, "Delete event " + deleteTag + "?",
              "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
          if (result == JOptionPane.YES_OPTION) {
            hvp.getTagData().remove(deleteTag);
            hvp.repaint();
            WaveViewPanel wvp = hvp.getInsetPanel();
            if (wvp != null) {
              wvp.repaint();
            }
            write();
          }
        } else {
          JOptionPane.showMessageDialog(hvp, "No event to delete.");

        }
      }
    });

    this.add(menuItem);
  }
  
  /**
   * Get currently set time as J2K.
   * 
   * @return j2k time
   */
  public double getJ2k() {
    return j2k;
  }

  /**
   * Set currently selected time as J2K.
   * 
   * @param j2k time
   */
  public void setJ2k(double j2k) {
    this.j2k = j2k;
  }



}
