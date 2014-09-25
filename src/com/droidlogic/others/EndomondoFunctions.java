package com.droidlogic.others;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.droidlogic.database.DBManager;

public class EndomondoFunctions
{
	private static final String ENDOMONDO_AUTH_URL = "https://api.mobile.endomondo.com/mobile/auth",
			ENDOMONDO_UPLOAD_URL = "http://api.mobile.endomondo.com/mobile/track",
			ENDOMONDO_DOWNLOAD_WORKOUT_URL = "https://api.mobile.endomondo.com/mobile/api/workout/get",
			ENDOMONDO_DOWNLOAD_WORKOUTS_URL = "https://api.mobile.endomondo.com/mobile/api/workouts",
			ENDOMONDO_DELETE_WORKOUT_URL = "http://www.endomondo.com/mobile/api/workout/delete";

	public static DownloadedTracks getEndomondoTracks(Context ctx,
			List<Track> localTracks, DBManager dbManager)
	{
		EndomondoData connectionData = connectEndomondo(ctx);
		if (connectionData != null)
		{
			return new DownloadedTracks(downloadEndomondoWorkouts(
					connectionData, localTracks, dbManager), true);
		}
		else
		{
			return new DownloadedTracks(localTracks, false);
		}
	}

	public static EndomondoData connectEndomondo(Context activity)
	{
		SharedPreferences settings = activity.getSharedPreferences(
				MyFunctions.PREFS_NAME, 0);
		SharedPreferences.Editor editor;
		String deviceId = settings.getString("deviceId", UUID.randomUUID().toString());
		String endoUser = settings.getString("endoUser", ""), endoPass = settings
				.getString("endoPass", ""), userId = settings.getString(
				"userId", ""), authToken = settings.getString("authToken", "");

		if (endoUser.equals("") || endoPass.equals(""))
		{
			return null;
		}

		if (!userId.equals("") && !authToken.equals(""))
		{
			Log.d(MyFunctions.TAG, "[+] Already connected, reusing data");
			return new EndomondoData(userId, authToken, endoUser, endoPass,
					deviceId);
		}
		else
		{
			HttpURLConnection conn = null;
			try
			{
				String login = ENDOMONDO_AUTH_URL;
				Map<String, String> kv = new HashMap<String, String>();
				kv.put("email", endoUser);
				kv.put("password", endoPass);
				kv.put("v", "2.4");
				kv.put("action", "pair");
				kv.put("deviceId", deviceId);
				kv.put("country", "N/A");

				conn = (HttpURLConnection) new URL(login).openConnection();
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				conn.addRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");

				OutputStream wr = new BufferedOutputStream(
						conn.getOutputStream());
				MyFunctions.writePost(wr, kv);
				wr.flush();
				wr.close();

				BufferedReader in = new BufferedReader(new InputStreamReader(
						conn.getInputStream()));
				JSONObject res = MyFunctions.parseKVP(in);
				conn.disconnect();

				int responseCode = conn.getResponseCode();
				String amsg = conn.getResponseMessage();
				Log.d(MyFunctions.TAG, "[+] Response code: " + responseCode
						+ ", msg=" + amsg + ", res=" + res.toString());
				if (responseCode == 200
						&& "OK".contentEquals(res.getString("_0"))
						&& res.has("authToken") && res.has("userId"))
				{
					authToken = res.getString("authToken");
					userId = res.getString("userId");
					editor = settings.edit();
					editor.putString("authToken", authToken);
					editor.putString("userId", userId);
					editor.putString("deviceId", deviceId);
					editor.commit();
				}
				else
				{
					return null;
				}

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			if (conn != null)
			{
				conn.disconnect();
			}
			return new EndomondoData(userId, authToken, endoUser, endoPass,
					deviceId);
		}
	}

	public static List<Track> downloadEndomondoWorkouts(EndomondoData data,
			List<Track> localTracks, DBManager dbManager)
	{
		StringBuffer url = new StringBuffer();
		url.append(ENDOMONDO_DOWNLOAD_WORKOUTS_URL + "?authToken="
				+ data.getAuthToken());
		url.append("&maxResults=" + MyFunctions.MAX_WORKOUTS);

		List<String> ids = new ArrayList<String>();
		List<Track> tracks = new ArrayList<Track>(localTracks);
		Track track;
		int indexTrack;
		HttpURLConnection conn = null;
		String id;
		long rowId;
		boolean isDownloaded;

		try
		{
			conn = (HttpURLConnection) new URL(url.toString()).openConnection();
			conn.setRequestMethod("GET");
			final InputStream in = new BufferedInputStream(
					conn.getInputStream());
			final JSONObject reply = MyFunctions.parse(in);
			int responseCode = conn.getResponseCode();
			// String amsg = conn.getResponseMessage();
			conn.disconnect();

			if (responseCode == 200)
			{
				JSONArray arr = reply.getJSONArray("data");
				for (int i = 0; i < arr.length(); i++)
				{
					JSONObject o = arr.getJSONObject(i);
					id = o.getString("id");
					ids.add(id);
					isDownloaded = false;
					for (Track t : localTracks)
					{
						if (t.getEndomondoWorkoutId().equals(id))
						{
							t.setUploadedToEndomondo(true);
							isDownloaded = true;
							break;
						}
					}

					if (!isDownloaded)
					{
						track = downloadEndomondoWorkout(data, id);
						if ((indexTrack = localTracks.indexOf(track)) >= 0)
						{
							track = localTracks.get(indexTrack);
							track.setUploadedToEndomondo(true);
							track.setEndomondoWorkoutId(id);
							dbManager.updateServersIdOnly(track.getID(),
									track.getEndomondoWorkoutId(),
									track.getRuntasticWorkoutId());
						}
						else
						{
							track.setUploadedToEndomondo(true);
							track.setEndomondoWorkoutId(id);
							track.setInLocalStorage(true);
							rowId = dbManager.insert(track.exportAsGPX(),
									track.getStartTime(),
									track.getEndomondoWorkoutId(),
									track.getRuntasticWorkoutId());
							track.setID(rowId);
							tracks.add(track);
						}
					}
				}
				// Now we have to change ID from tracks that are not in
				// endomondo server anymore.
				for (Track t : localTracks)
				{
					if (!t.getEndomondoWorkoutId().equals("")
							&& t.isUploadedToEndomondo() == false)
					{
						dbManager.updateServersIdOnly(t.getID(),
								t.getEndomondoWorkoutId(),
								t.getRuntasticWorkoutId());
					}
				}
				return tracks;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return localTracks;
		}
		return localTracks;
	}

	public static Track downloadEndomondoWorkout(EndomondoData data,
			String workoutId)
	{
		StringBuffer url = new StringBuffer();
		url.append(ENDOMONDO_DOWNLOAD_WORKOUT_URL + "?authToken="
				+ data.getAuthToken());
		url.append("&workoutId=" + workoutId);
		url.append("&fields=points");

		List<Location> locations = null;
		Location location;
		SimpleDateFormat format;
		HttpURLConnection conn = null;
		try
		{
			conn = (HttpURLConnection) new URL(url.toString()).openConnection();
			conn.setRequestMethod("GET");
			final InputStream in = new BufferedInputStream(
					conn.getInputStream());
			final JSONObject reply = MyFunctions.parse(in);
			int responseCode = conn.getResponseCode();
			// String amsg = conn.getResponseMessage();
			conn.disconnect();

			if (responseCode == 200 && reply.has("points"))
			{
				JSONArray arr = reply.getJSONArray("points");
				JSONObject point;
				locations = new ArrayList<Location>();
				for (int i = 0; i < arr.length(); i++)
				{
					point = arr.getJSONObject(i);
					location = new Location("Endomondo");
					if (point.has("alt"))
					{
						location.setAltitude(Double.parseDouble(point
								.getString("alt")));
					}

					if (point.has("lat") && point.has("lng"))
					{
						location.setLatitude(Double.parseDouble(point
								.getString("lat")));
						location.setLongitude(Double.parseDouble(point
								.getString("lng")));
					}

					if (point.has("time"))
					{
						format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
								Locale.US);
						format.setTimeZone(TimeZone.getTimeZone("UTC"));
						location.setTime((format.parse(point.getString("time")
								.substring(0, 19))).getTime());
					}
					locations.add(location);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return new Track(locations);
	}

	public static boolean removeEndomondoWorkout(Context ctx, String workoutId)
	{
		EndomondoData data = connectEndomondo(ctx);
		StringBuffer url = new StringBuffer();
		url.append(ENDOMONDO_DELETE_WORKOUT_URL + "?authToken="
				+ data.getAuthToken());
		url.append("&workoutId=" + workoutId);
		HttpURLConnection conn = null;

		try
		{
			conn = (HttpURLConnection) new URL(url.toString()).openConnection();
			conn.setRequestMethod("GET");
			final InputStream in = new BufferedInputStream(
					conn.getInputStream());
			// JSONObject res = MyFunctions.parse(in);
			MyFunctions.parse(in);
			int responseCode = conn.getResponseCode();
			// String amsg = conn.getResponseMessage();
			conn.disconnect();

			if (responseCode == 200)
			{
				Log.d(MyFunctions.TAG, "[+] Workout removed!");
				return true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public static boolean uploadEndomondoWorkout(Context ctx, Track t)
	{
		EndomondoData data = connectEndomondo(ctx);

		HttpURLConnection conn = null;
		try
		{
			StringWriter writer = new StringWriter();
			String workoutId = data.getDeviceId() + "-"
					+ Long.toString(generateRandomWorkoutId());
			// System.err.println("workoutId: " + workoutId);

			writeEndomondoCompressedTrack(writer, t);
			StringBuffer url = new StringBuffer();
			List<Location> locations = t.getLocations();
			url.append(ENDOMONDO_UPLOAD_URL + "?authToken="
					+ data.getAuthToken());
			url.append("&workoutId=" + workoutId);
			url.append("&sport=" + 0);
			url.append("&duration=" + MyFunctions.getTime(locations) / 1000f);
			url.append("&distance=" + MyFunctions.getDistance(locations) / 1000f);
			url.append("&gzip=true");
			url.append("&extendedResponse=true");

			conn = (HttpURLConnection) new URL(url.toString()).openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.addRequestProperty("Content-Type", "application/octet-stream");
			OutputStream out = new GZIPOutputStream(new BufferedOutputStream(
					conn.getOutputStream()));
			out.write(writer.getBuffer().toString().getBytes());
			out.flush();
			out.close();

			BufferedReader in = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			JSONObject res = MyFunctions.parseKVP(in);
			conn.disconnect();

			Log.d(MyFunctions.TAG, "res: " + res.toString());

			int responseCode = conn.getResponseCode();
			if (responseCode == 200 && "OK".contentEquals(res.getString("_0")))
			{
				Log.d(MyFunctions.TAG, "[+] Workout was uploaded!");
				return true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	private static void writeEndomondoCompressedTrack(StringWriter writer,
			Track t)
	{
		double distance = 0;
		Location lastLoc = null;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		List<Location> locations;
		Location l;
		try
		{
			locations = t.getLocations();
			for (int i = 0; i < locations.size(); i++)
			{
				l = locations.get(i);
				if (lastLoc != null)
				{
					distance += l.distanceTo(lastLoc);
				}
				lastLoc = l;

				// # timestamp;
				// # type (2=start, 3=end, 0=pause, 1=resume);
				// # latitude;
				// # longitude;
				// #;
				// #;
				// # alt;
				// # hr;

				writer.write(simpleDateFormat.format(new Date(l.getTime())));
				if (i == 0)
				{
					writer.write(";2;");
				}
				else if (i == locations.size() - 1)
				{
					writer.write(";3;");
				}
				else
				{
					writer.write(";;");
				}
				writer.write(Double.toString(l.getLatitude()));
				writer.write(';');
				writer.write(Double.toString(l.getLongitude()));
				writer.write(';');
				writer.write(Double.toString(distance / 1000)); // in km
				writer.write(";;;;");
				writer.append('\n');
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static long generateRandomWorkoutId()
	{
		return MyFunctions.random.nextLong();
	}
}
