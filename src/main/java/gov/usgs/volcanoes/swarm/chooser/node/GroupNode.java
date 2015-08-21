package gov.usgs.volcanoes.swarm.chooser.node;

import javax.swing.Icon;

import gov.usgs.volcanoes.swarm.Icons;

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
