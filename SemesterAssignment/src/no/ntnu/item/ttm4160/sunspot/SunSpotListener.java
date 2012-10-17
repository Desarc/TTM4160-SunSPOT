package no.ntnu.item.ttm4160.sunspot;


/**
 * Interface for a SunSpotApplicaton listener (for button actions)
 *
 */
public interface SunSpotListener {

	/**
	 * A button is pressed.
	 * @param action
	 */
	public void actionReceived(String action);
	
}
