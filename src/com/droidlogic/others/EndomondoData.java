package com.droidlogic.others;

public class EndomondoData
{
	private String userId, authToken, username, password, deviceId;
	
	public EndomondoData(String uId, String at, String user, String pass, String devId)
	{
		userId=uId;
		authToken=at;
		username=user;
		password=pass;
		deviceId=devId;
	}
	
	public String getUserId()
	{
		return userId;
	}
	
	public String getAuthToken()
	{
		return authToken;
	}
	
	public String getUsername()
	{
		return username;
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public String getDeviceId()
	{
		return deviceId;
	}
}
