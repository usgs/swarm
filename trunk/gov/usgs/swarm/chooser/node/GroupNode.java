package gov.usgs.swarm.chooser.node;

import gov.usgs.swarm.Icons;

import javax.swing.Icon;

public class GroupNode extends AbstractChooserNode {
    private static final long serialVersionUID = 1L;
    private String name;

    public GroupNode(String n) {
        name = n;
        label = name;
        icon = Icons.wave_folder;
    }

    public String getName() {
        return name;
    }
}
