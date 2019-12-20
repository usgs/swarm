package gov.usgs.volcanoes.swarm;


import gov.usgs.volcanoes.core.data.file.FileType;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/*
 * @author Tom Parker
 */
public class FileTypeDialog extends SwarmModalDialog {
  private static final long serialVersionUID = 1L;
  private static final JFrame applicationFrame = Swarm.getApplicationFrame();
  private JLabel filename;
  private JList<FileType> fileTypes;
  private JCheckBox assumeSame;
  private boolean cancelled = true;
  private boolean opened = false;

  /**
   * Constructor.
   * @param saveMode true is selecting file type to save to
   */
  public FileTypeDialog(boolean saveMode) {
    super(applicationFrame, "Select File Type");
    setSizeAndLocation();
    if (saveMode) {
      updateUi();
    }
  }

  public void setFilename(String fn) {
    filename.setText(fn);
  }
  
  /**
   * Update UI to remove file types not supported for saving.
   */
  private void updateUi() {
    DefaultListModel<FileType> model = (DefaultListModel<FileType>) fileTypes.getModel();
    model.removeElement(FileType.WIN);
  }
  
  /**
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#createUi()
   */
  @Override
  protected void createUi() {
    super.createUi();
    filename = new JLabel();
    filename.setFont(Font.decode("dialog-BOLD-12"));
    filename.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

    DefaultListModel<FileType> model = new DefaultListModel<FileType>();
    for (FileType ft : FileType.getKnownTypes()) {
      model.addElement(ft);
    }
    fileTypes = new JList<FileType>(model);
    fileTypes.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          if (fileTypes.getSelectedIndex() != -1) {
            okButton.doClick();
          }
        }
      }
    });
    fileTypes.setSelectedValue(FileType.UNKNOWN, true);
    assumeSame = new JCheckBox("Assume all unknown files are of this type", false);
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
    panel.setPreferredSize(new Dimension(300, 200));
    JPanel labelPanel = new JPanel(new GridLayout(3, 1));
    labelPanel.add(new JLabel("Select file type: "));
    labelPanel.add(filename);
    labelPanel.add(new JLabel("Choose 'Cancel' to skip this file or select file type:"));
    panel.add(labelPanel, BorderLayout.NORTH);
    panel.add(new JScrollPane(fileTypes), BorderLayout.CENTER);
    panel.add(assumeSame, BorderLayout.SOUTH);
    mainPanel.add(panel, BorderLayout.CENTER);
  }

  public boolean isAssumeSame() {
    return assumeSame.isSelected();
  }

  public FileType getFileType() {
    return (FileType) fileTypes.getSelectedValue();
  }

  public void wasOk() {
    cancelled = false;
  }

  public void wasCancelled() {
    cancelled = true;
    opened = false;
  }

  /**
   * @see java.awt.Dialog#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean b) {
    if (b) {
      opened = true;
    }
    super.setVisible(b);
  }

  public boolean isOpen() {
    return opened;
  }

  public boolean isCancelled() {
    return cancelled;
  }
}
