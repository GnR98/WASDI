package wasdi.shared.viewmodels;

import java.util.ArrayList;

/**
 * Created by s.adamo on 23/05/2016.
 */
public class NodeGroupViewModel {


    private String nodeName;

    public NodeGroupViewModel(String sNodeName)
    {
        this.setNodeName(sNodeName);
    }

    private ArrayList<BandViewModel> bands;

    public ArrayList<BandViewModel> getBands() {
        return bands;
    }

    public void setBands(ArrayList<BandViewModel> bands) {
        this.bands = bands;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
