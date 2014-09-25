package com.droidlogic.others;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;

import com.droidlogic.database.DBManager;

public class RuntasticFunctions
{
	private static final HttpClient httpClient = new DefaultHttpClient();

	public static RuntasticData connectRuntastic(Context ctx)
	{

		SharedPreferences settings = ctx.getSharedPreferences(
				MyFunctions.PREFS_NAME, 0);
		String userName = settings.getString("runUser", null), password = settings
				.getString("runPass", null);

		if (userName == null || password == null)
		{
			return null;
		}
		else
		{
			HttpClientContext httpContext = HttpClientContext.create();
			CookieStore cookieStore = new BasicCookieStore();

			BasicClientCookie cookie = new BasicClientCookie("hrm_ad", "1");
			cookie.setVersion(0);
			cookie.setDomain("www.runtastic.com");
			cookie.setPath("/");

			cookieStore.addCookie(cookie);
			httpContext.setCookieStore(cookieStore);

			HttpPost httpPost = new HttpPost(
					"https://www.runtastic.com/en/d/users/sign_in.json");
			String authToken;
			try
			{
				StringTokenizer st;
				String fullUserName;
				String userId;
				int auxIndex;
				HttpGet get = new HttpGet("https://www.runtastic.com");
				HttpResponse response = httpClient.execute(get, httpContext), responseUserId;
				String relicId;
				InputStream is = new BufferedInputStream(response.getEntity()
						.getContent());
				String replyString = getResponse(new BufferedReader(
						new InputStreamReader(is)));
				relicId = replyString.substring(replyString.indexOf("={xpid:"));
				relicId = relicId.substring(8, 24);
				authToken = replyString;
				is.close();
				authToken = authToken
						.substring(authToken.indexOf("csrf-param"));
				authToken = authToken.substring(29, 73);
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("user[email]",
						userName));
				nameValuePairs.add(new BasicNameValuePair("user[password]",
						password));
				nameValuePairs.add(new BasicNameValuePair("authenticity-token",
						authToken));
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				httpPost.addHeader("Accept", "*/*");
				httpPost.addHeader("Accept-Encoding", "");
				httpPost.addHeader("Accept-Language", "en-US,en;q=0.8,es;q=0.6");
				httpPost.addHeader("Accept-Charset", "utf-8");
				httpPost.addHeader(
						"User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36");
				httpPost.addHeader("X-Requested-With", "XMLHttpRequest");

				httpPost.addHeader("Origin", "https://www.runtastic.com");
				httpPost.addHeader("Referer", "https://www.runtastic.com");
				httpPost.addHeader("X-CSRF-Token", authToken);
				httpPost.addHeader("X-NewRelic-ID", relicId);

				response = httpClient.execute(httpPost, httpContext);

				is = new BufferedInputStream(response.getEntity().getContent());
				// final JSONObject reply = MyFunctions.parse(is);
				final JSONObject reply = MyFunctions.parse(is);
				is.close();
				// System.out.println("[+] Login response: " +
				// reply.toString());
				int responseCode = response.getStatusLine().getStatusCode();
				boolean success;
				if (responseCode == 200)
				{
					success = reply.getBoolean("success");
					if (success)
					{
						Pattern pat = Pattern
								.compile(
										"(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
										Pattern.DOTALL);
						Matcher matcher = pat
								.matcher(reply.getString("update"));
						matcher.find();
						matcher.find();
						st = new StringTokenizer(matcher.group().substring(8),
								"/");
						for (int i = 0; i < 3; i++)
						{
							st.nextToken();
						}
						fullUserName = st.nextToken();
						get = new HttpGet("https://www.runtastic.com/en/users/"
								+ fullUserName + "/import");
						responseUserId = httpClient.execute(get, httpContext);

						is = new BufferedInputStream(responseUserId.getEntity()
								.getContent());
						userId = getResponse(new BufferedReader(
								new InputStreamReader(is)));
						is.close();
						auxIndex = userId.indexOf("\"id\":") + 5;
						userId = userId.substring(auxIndex, auxIndex + 8);

						return new RuntasticData(authToken, userId, userName,
								password, fullUserName, relicId, httpContext);
					}
					else
					{
						return null;
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return null;
		}
	}

	public static String getResponse(BufferedReader in)
	{
		try
		{
			StringBuilder out = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null)
			{
				out.append(line);
			}
			return out.toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static String uploadRuntasticWorkout(Context ctx, Track t)
	{
		try
		{
			RuntasticData data;
			if ((data = MyFunctions.getRuntasticData()) == null)
			{
				data = connectRuntastic(ctx);
				MyFunctions.setRuntasticData(data);
			}
			if (data != null)
			{
				String fileName = t.getTitle() + "_"
						+ MyFunctions.random.nextLong() + ".gpx";
				HttpPost httpPost = new HttpPost(
						"https://www.runtastic.com/import/upload_session?authenticity_token="
								+ URLEncoder.encode(data.getAuthToken(),
										"UTF-8") + "&qqfile=" + fileName);

				httpPost.addHeader("Accept", "*/*");
				 httpPost.addHeader("Accept-Encoding", "");
				 httpPost.addHeader("Accept-Language", "en-US,en;q=0.8,es;q=0.6");
				httpPost.addHeader(
						"User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36");
				httpPost.addHeader("X-Requested-With", "XMLHttpRequest");

				httpPost.addHeader("X-File-Name", fileName);
				httpPost.addHeader(HttpHeaders.CONTENT_TYPE,
						"application/octet-stream");
				httpPost.addHeader("X-NewRelic-ID", data.getRelicId());

				httpPost.addHeader("Origin", "https://www.runtastic.com");
				httpPost.addHeader(
						"Referer",
						"https://www.runtastic.com/en/users/"
								+ data.getCompleteUserName() + "/import_iframe");

				EntityBuilder builder = EntityBuilder.create();
				builder.setBinary(t.exportAsGPX().getBytes());
				httpPost.setEntity(builder.build());
				HttpResponse response = httpClient.execute(httpPost,
						data.getHttpContext());
				InputStream is = new BufferedInputStream(response.getEntity()
						.getContent());
				final JSONObject reply = MyFunctions.parse(is);
				is.close();

				int responseCode = response.getStatusLine().getStatusCode();
				boolean success;
				if (responseCode == 200)
				{
					if (reply.has("success"))
					{
						success = reply.getBoolean("success");
						if (success)
						{
							StringTokenizer st = new StringTokenizer(
									reply.toString(), "\'");
							st.nextToken();
							String trackId=st.nextToken();
							return trackId;
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static boolean deleteTrackRuntastic(Context ctx, String trackId)
	{
		try
		{
			RuntasticData data;
			if ((data = MyFunctions.getRuntasticData()) == null)
			{
				data = connectRuntastic(ctx);
				MyFunctions.setRuntasticData(data);
			}
			if (data != null)
			{
				HttpDelete delete = new HttpDelete(
						"https://www.runtastic.com/en/users/"
								+ data.getCompleteUserName()
								+ "/sport-sessions/" + trackId);

				delete.addHeader(
						"Accept",
						"*/*;q=0.5, text/javascript, application/javascript, application/ecmascript, application/x-ecmascript");
				 delete.addHeader("Accept-Encoding", "");
				 delete.addHeader("Accept-Language", "en-US,en;q=0.8,es;q=0.6");
				delete.addHeader(
						"User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36");
				delete.addHeader("X-Requested-With", "XMLHttpRequest");

				delete.addHeader(
						"Referer",
						"https://www.runtastic.com/en/users/"
								+ data.getCompleteUserName()
								+ "/sport-sessions");
				delete.addHeader("X-CSRF-Token", data.getAuthToken());
				delete.addHeader("X-NewRelic-ID", data.getRelicId());

				HttpResponse response = httpClient.execute(delete,
						data.getHttpContext());
				InputStream is = new BufferedInputStream(response.getEntity()
						.getContent());
				String reply = getResponse(new BufferedReader(
						new InputStreamReader(is)));
				is.close();

				int responseCode = response.getStatusLine().getStatusCode();
				if (responseCode == 200)
				{
					return true;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	private static List<String> getRuntasticIds(RuntasticData data)
	{
		String response;
		InputStream is;
		int index;
		List<String> res = new ArrayList<String>();
		try
		{
			HttpGet get = new HttpGet("https://www.runtastic.com/en/users/"
					+ data.getCompleteUserName() + "/sport-sessions");
			HttpResponse webResponse = httpClient.execute(get,
					data.getHttpContext());
			is = new BufferedInputStream(webResponse.getEntity().getContent());
			response = getResponse(new BufferedReader(new InputStreamReader(is)));
			is.close();
			index = response.indexOf("var index_data = [") + 18;
			response = response.substring(index);
			response = response.substring(0, response.indexOf(';') - 1);
			Pattern pat = Pattern.compile("[0-9]{9}", Pattern.DOTALL);
			Matcher matcher = pat.matcher(response.toString());
			while (matcher.find())
			{
				res.add(matcher.group());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return res;
	}

	public static Track downloadFromRuntastic(RuntasticData data, String trackId)
	{
		try
		{
			HttpGet get = new HttpGet("https://www.runtastic.com/en/users/"
					+ data.getCompleteUserName() + "/sport-sessions/" + trackId
					+ ".gpx");
			get.addHeader("Accept",
					"application/json, text/javascript, */*; q=0.01");
			get.addHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			get.addHeader("Accept-Encoding", "");
			get.addHeader("Accept-Language", "en-US,en;q=0.8,es;q=0.6");
			get.addHeader(
					"User-Agent",
					"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36");
			get.addHeader("Referer", "https://www.runtastic.com/en/users/"
					+ data.getCompleteUserName() + "/sport-sessions/" + trackId);

			HttpResponse response = httpClient.execute(get,
					data.getHttpContext());
			InputStream is = new BufferedInputStream(response.getEntity()
					.getContent());
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] byteArrayAux = new byte[8192];
			int lenRead;
			while ((lenRead = is.read(byteArrayAux)) != -1)
			{
				buffer.write(byteArrayAux, 0, lenRead);
			}
			is.close();
			return new Track(null, new String(buffer.toByteArray(), "UTF-8"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static DownloadedTracks downloadRuntasticWorkouts(Context ctx,
			List<Track> localTracks, DBManager dbManager)
	{
		List<String> ids;
		List<Track> tracks = new ArrayList<Track>(localTracks);
		Track track;
		long rowId;
		int indexTrack;
		boolean isDownloaded;
		try
		{
			RuntasticData data;
			if ((data = MyFunctions.getRuntasticData()) == null)
			{
				data = connectRuntastic(ctx);
				MyFunctions.setRuntasticData(data);
			}
			if (data != null)
			{
				ids = getRuntasticIds(data);
				for (String id : ids)
				{
					isDownloaded = false;
					for (Track t : localTracks)
					{
						if (t.getRuntasticWorkoutId().equals(id))
						{
							t.setUploadedToRuntastic(true);
							isDownloaded = true;
							break;
						}
					}

					if (!isDownloaded)
					{
						track = downloadFromRuntastic(data, id);
						if ((indexTrack = localTracks.indexOf(track)) >= 0)
						{
							track = localTracks.get(indexTrack);
							track.setUploadedToRuntastic(true);
							track.setRuntasticWorkoutId(id);
							dbManager.updateServersIdOnly(track.getID(),
									track.getEndomondoWorkoutId(),
									track.getRuntasticWorkoutId());
						}
						else
						{
							track.setUploadedToRuntastic(true);
							track.setRuntasticWorkoutId(id);
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
					if (!t.getRuntasticWorkoutId().equals("")
							&& t.isUploadedToRuntastic() == false)
					{
						dbManager.updateServersIdOnly(t.getID(),
								t.getEndomondoWorkoutId(),
								t.getRuntasticWorkoutId());
					}
				}
				return new DownloadedTracks(tracks, true);
			}
			else
			{
				return new DownloadedTracks(tracks, false);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			new DownloadedTracks(tracks, false);
		}
		return new DownloadedTracks(tracks, false);
	}
}
