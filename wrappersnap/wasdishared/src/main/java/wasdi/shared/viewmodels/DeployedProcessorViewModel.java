package wasdi.shared.viewmodels;

public class DeployedProcessorViewModel {
	private String processorId;
	private String processorName;
	private String processorVersion;
	private String processorDescription;
	private String imgLink;
	private String publisher;
	private String paramsSample = "";
	private int isPublic = 0;
	private int iTimeoutMs = 1000*60*60*3;
	private String type = "";
	
	public String getParamsSample() {
		return paramsSample;
	}
	public void setParamsSample(String paramsSample) {
		this.paramsSample = paramsSample;
	}
	public String getProcessorId() {
		return processorId;
	}
	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}
	public String getProcessorName() {
		return processorName;
	}
	public void setProcessorName(String processorName) {
		this.processorName = processorName;
	}
	public String getProcessorVersion() {
		return processorVersion;
	}
	public String getImgLink() {
		return imgLink;
	}
	public void setImgLink(String imgLink) {
		this.imgLink = imgLink;
	}
	public String getPublisher() {
		return publisher;
	}
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	public void setProcessorVersion(String processorVersion) {
		this.processorVersion = processorVersion;
	}
	public String getProcessorDescription() {
		return processorDescription;
	}
	public void setProcessorDescription(String processorDescription) {
		this.processorDescription = processorDescription;
	}
	public int getIsPublic() {
		return isPublic;
	}
	public void setIsPublic(int isPublic) {
		this.isPublic = isPublic;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public int getiTimeoutMs() {
		return iTimeoutMs;
	}
	public void setiTimeoutMs(int iTimeoutMs) {
		this.iTimeoutMs = iTimeoutMs;
	}
	
}
