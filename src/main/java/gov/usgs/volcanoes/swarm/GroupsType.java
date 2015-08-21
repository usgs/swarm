package gov.usgs.volcanoes.swarm;

/**
 * The groups type.
 * 
 * @author Kevin Frechette (ISTI)
 * @see #NETWORK
 * @see #NETWORK_AND_SITE
 * @see #SITE
 */
public enum GroupsType
{
	/** Group by network only. */
	NETWORK,
	/** Group by network and site. */
	NETWORK_AND_SITE,
	/** Group by site only. */
	SITE
}
