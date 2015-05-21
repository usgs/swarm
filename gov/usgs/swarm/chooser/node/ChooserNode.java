package gov.usgs.swarm.chooser.node;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

abstract public class ChooserNode extends DefaultMutableTreeNode {
    private static final long serialVersionUID = 1L;

    abstract public Icon getIcon();

    abstract public String getLabel();

    abstract public String getToolTip();
    
    public String toString() {
        return "chooserNode";
    }
}
