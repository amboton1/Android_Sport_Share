package com.droidlogic.others;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class MyFunctions
{
	public static final String TAG = "pfdeporte", PREFS_NAME = "prefsFile";
	public static final Random random = new Random();
	public static final int MAX_WORKOUTS = 60000;
	private static RuntasticData runtasticData;
	
	public static RuntasticData getRuntasticData()
	{
		return runtasticData;
	}

	public static void setRuntasticData(RuntasticData runtasticData)
	{
		MyFunctions.runtasticData = runtasticData;
	}

	public static float getDistance(List<Location> locations)
	{
		float res = 0;
		if (locations != null)
		{
			for (int i = 0; i < locations.size() - 1; i++)
			{
				res += locations.get(i).distanceTo(locations.get(i + 1));
			}
		}
		return res;
	}

	public static long getTime(List<Location> locations)
	{
		long res = 0;
		if (locations != null)
		{
			for (int i = 0; i < locations.size() - 1; i++)
			{
				res += Math.abs(locations.get(i + 1).getTime()
						- locations.get(i).getTime());
			}
		}
		return res;
	}

	public static List<LatLng> locationsToLatLng(List<Location> locations)
	{
		if (locations != null && locations.size() > 0)
		{
			List<LatLng> res = new ArrayList<LatLng>();
			Location aux;
			for (int i = 0; i < locations.size(); i++)
			{
				aux = locations.get(i);
				res.add(locationToLatLng(aux));
			}
			return res;
		}
		return null;
	}

	public static LatLng locationToLatLng(Location l)
	{
		return new LatLng(l.getLatitude(), l.getLongitude());
	}

	public static JSONObject parse(InputStream in) throws JSONException
	{
		final Scanner s = new Scanner(in);
		final JSONObject o = new JSONObject(s.useDelimiter("\\A").next());
		s.close();
		return o;
	}

	public static JSONObject parseKVP(BufferedReader in) throws IOException,
			JSONException
	{
		JSONObject obj = new JSONObject();
		int lineno = 0;
		String s;
		while ((s = in.readLine()) != null)
		{
			int c = s.indexOf('=');
			if (c == -1)
			{
				obj.put("_" + Integer.toString(lineno), s);
			}
			else
			{
				obj.put(s.substring(0, c), s.substring(c + 1));
			}
			lineno++;
		}
		return obj;
	}

	public static void writePost(OutputStream o, Map<String, String> map)
			throws IOException
	{
		boolean first = true;
		DataOutputStream out = new DataOutputStream(o);
		for (String k : map.keySet())
		{
			if (first)
			{
				first = false;
			}
			else
			{
				out.writeByte('&');
			}
			out.writeBytes(URLEncoder.encode(k, "UTF-8"));
			out.writeByte('=');
			out.writeBytes(URLEncoder.encode(map.get(k), "UTF-8"));
		}
	}
}
