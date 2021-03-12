package wasdi.shared.viewmodels;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by p.campanella on 25/10/2016.
 */
public class WorkspaceListInfoViewModel {
    private String workspaceId;
    private String workspaceName;
    private String ownerUserId;
    private List<String> sharedUsers = new ArrayList<>();
    // 	reports if the node on which the workspace is hosted is active
    private boolean isNodeActive; 

    public boolean isNodeActive() {
		return isNodeActive;
	}

	public void setNodeActive(boolean isNodeActive) {
		this.isNodeActive = isNodeActive;
	}

	public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public List<String> getSharedUsers() {
        return sharedUsers;
    }

    public void setSharedUsers(List<String> sharedUsers) {
        this.sharedUsers = sharedUsers;
    }
}
