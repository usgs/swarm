package gov.usgs.volcanoes.swarm.chooser.node;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

public abstract class AbstractChooserNode extends DefaultMutableTreeNode {
  private static final long serialVersionUID = 1L;
  protected String label;
  protected Icon icon;

  public Icon getIcon() {
    return icon;
  }

  public String getLabel() {
    return label;
  }

  public String getToolTip() {
    return null;
  }

  public String toString() {
    return label;
  }
}
