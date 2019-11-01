/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.openstack.manager.internal.properties;

import org.apache.commons.logging.LogConfigurationException;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.cps.CpsProperties;
import dev.galasa.openstack.manager.OpenstackManagerException;

/**
 * OpenStack Domain name
 * <p>
 * The Openstack Domain name that the manager will authenticate against and create compute resources under.
 * This property is required as no default is available. 
 * </p><p>
 * The property is:-<br><br>
 * openstack.server.domain.name=xxxxx 
 * </p>
 * <p>
 * There is no default
 * </p>
 * 
 * @author Michael Baylis
 *
 */
public class OpenStackDomainName extends CpsProperties {
	
	public static String get() throws ConfigurationPropertyStoreException, OpenstackManagerException, LogConfigurationException {
		return getStringNulled(OpenstackPropertiesSingleton.cps(), 
				               "server", 
				               "domain.name");
	}

}
