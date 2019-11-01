/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.openstack.manager.internal.json;

import com.google.gson.annotations.SerializedName;

public class Server {

	public String  id; // NOSONAR
	public String  name; // NOSONAR
	public String  imageRef; // NOSONAR
	public String  flavorRef; // NOSONAR
	public String  networks; // NOSONAR
	public String  availability_zone; // NOSONAR
	public String  adminPass; // NOSONAR
	public String  key_name; // NOSONAR
	
	@SerializedName("OS-EXT-STS:power_state")
	public Integer power_state; // NOSONAR
	
	@SerializedName("OS-EXT-STS:task_state")
	public String task_state; // NOSONAR
	
	public GalasaMetadata metadata; // NOSONAR
	
}
