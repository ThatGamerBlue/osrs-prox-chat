package com.thatgamerblue.osrs.proxchat.server.config;

/**
 * Possible results of a config file read
 */
public enum ConfigState
{
	/**
	 * Config read was successful, continue loading
	 */
	SUCCESS,
	/**
	 * Config file was actually a directory, abort loading
	 */
	IS_DIRECTORY,
	/**
	 * Config read failed for an unknown reason, abort loading
	 */
	FAILURE,
	/**
	 * Created a new config file, abort loading and alert user to change the password
	 */
	CREATED_NEW;
}
