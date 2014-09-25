package com.droidlogic.others;

import org.apache.http.client.protocol.HttpClientContext;

public class RuntasticData
{
	private String authToken, userName, password, userId, completeUserName, relicId;
	private HttpClientContext httpContext;
	
	public RuntasticData(String at, String uid, String un, String p, String cu, 
			String rel, HttpClientContext hc)
	{
		authToken=at;
		userId=uid;
		userName=un;
		password=p;
		completeUserName=cu;
		relicId=rel;
		httpContext=hc;
	}

	public HttpClientContext getHttpContext()
	{
		return httpContext;
	}

	public void setHttpContext(HttpClientContext httpContext)
	{
		this.httpContext = httpContext;
	}

	public String getRelicId()
	{
		return relicId;
	}


	public void setRelicId(String relicId)
	{
		this.relicId = relicId;
	}


	public String getAuthToken()
	{
		return authToken;
	}

	public void setAuthToken(String authToken)
	{
		this.authToken = authToken;
	}

	public String getUserName()
	{
		return userName;
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getUserId()
	{
		return userId;
	}

	public void setUserId(String userId)
	{
		this.userId = userId;
	}

	public String getCompleteUserName()
	{
		return completeUserName;
	}

	public void setCompleteUserName(String completeUserName)
	{
		this.completeUserName = completeUserName;
	}
	
}
