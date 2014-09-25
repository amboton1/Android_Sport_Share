package com.droidlogic.others;

import java.util.List;

public class DownloadedTracks
{
	private List<Track> tracks;
	private boolean result;
	
	public DownloadedTracks(List<Track> t, boolean res)
	{
		tracks=t;
		result=res;
	}

	public List<Track> getTracks()
	{
		return tracks;
	}

	public void setTracks(List<Track> tracks)
	{
		this.tracks = tracks;
	}

	public boolean isOK()
	{
		return result;
	}

	public void setResult(boolean result)
	{
		this.result = result;
	}
	
}
