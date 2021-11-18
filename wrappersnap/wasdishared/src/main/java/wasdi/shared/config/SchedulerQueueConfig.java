package wasdi.shared.config;

/**
 * Single Scheduler Queue configuration
 * 
 * @author p.campanella
 *
 */
public class SchedulerQueueConfig {
	
	/**
	 * Name of the Queue
	 */
	public String name;
	
	/**
	 * Max number of elements in the queue
	 */
	public String maxQueue;
	/**
	 * Default Queue timeout in ms
	 */
	public String timeoutMs;
	
	/**
	 * Comma separated Operation Types supported by this queue
	 */
	public String opTypes;
	
	/**
	 * Operation subtype: to be valorized, OpTypes must contain only one operation
	 * and subtype must be a valid sub type of OpType
	 */
	public String opSubType;
	
	/**
	 * Flag to enable or disable this queue
	 */
	public String enabled;
}
