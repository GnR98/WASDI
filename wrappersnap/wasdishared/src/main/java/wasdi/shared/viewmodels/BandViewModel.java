package wasdi.shared.viewmodels;

/**
 * Created by s.adamo on 20/05/2016.
 */
public class BandViewModel {

    public BandViewModel() {

    }
    public BandViewModel(String sBandName)
    {
        this.name = sBandName;
    }

    private String name;
    private Boolean published = false;
    private int width = 0;
    private int height = 0;
    private String layerId;
    private String geoserverBoundingBox;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
	public Boolean getPublished() {
		return published;
	}
	public void setPublished(Boolean published) {
		this.published = published;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public String getLayerId() {
		return layerId;
	}
	public void setLayerId(String layerId) {
		this.layerId = layerId;
	}
	public String getGeoserverBoundingBox() {
		return geoserverBoundingBox;
	}
	public void setGeoserverBoundingBox(String geoserverBoundingBox) {
		this.geoserverBoundingBox = geoserverBoundingBox;
	}
}
