package gov.usgs.swarm.chooser.node;

import gov.usgs.swarm.Icons;

import javax.swing.Icon;

public class GroupNode extends ChooserNode {
    private static final long serialVersionUID = 1L;
    private String name;

    public GroupNode(String n) {
        name = n;
    }

    public Icon getIcon() {
        return Icons.wave_folder;
    }

    public String getLabel() {
        return name;
    }
    
    public String getToolTip() {
        return null;
    }
    
    public String getName() {
        return name;
    }
}
