package com.droidlogic.others;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.location.Location;
import android.text.format.Time;
import android.util.Log;
import android.util.Xml;

public class Track implements Comparable<Track>
{
	private static final String NS_SCHEMA = "http://www.w3.org/2001/XMLSchema-instance";
	private static final String NS_GPX_11 = "http://www.topografix.com/GPX/1/1";
//	private static final String NS_GPX_10 = "http://www.topografix.com/GPX/1/0";
	private static final float MAX_ERROR=0.000004f;
	
	private List<Location> locations;
	private String title = null;
	private boolean endomondo, runtastic, local;
	private Long id = null;
	private String endomondoWorkoutId, runtasticWorkoutId;

	public String getRuntasticWorkoutId()
	{
		return runtasticWorkoutId;
	}

	public void setRuntasticWorkoutId(String runtasticWorkoutId)
	{
		this.runtasticWorkoutId = runtasticWorkoutId;
	}

	public String getEndomondoWorkoutId()
	{
		return endomondoWorkoutId;
	}

	public void setEndomondoWorkoutId(String endomondoWorkoutId)
	{
		this.endomondoWorkoutId = endomondoWorkoutId;
	}

	public Track(String ti, List<Location> locs)
	{
		title = ti;
		locations = locs;
		endomondoWorkoutId = "";
		runtasticWorkoutId = "";
	}

	public Track(Long id, String trackData)
	{
		this.id = id;
		endomondoWorkoutId = "";
		runtasticWorkoutId = "";
		XmlPullParser parser = Xml.newPullParser();
		StringReader reader = new StringReader(trackData);
		List<Location> locs = new ArrayList<Location>();
		try
		{
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(reader);
			parser.nextTag();
			parser.require(XmlPullParser.START_TAG, "", "gpx");
			while (parser.next() != XmlPullParser.END_TAG)
			{
				if (parser.getEventType() != XmlPullParser.START_TAG)
				{
					continue;
				}
				String trk = parser.getName();
				// Starts by looking for the trk tag
				if (trk.equals("trk"))
				{
					while (parser.next() != XmlPullParser.END_TAG)
					{
						if (parser.getEventType() != XmlPullParser.START_TAG)
						{
							continue;
						}
						String trkSeg = parser.getName();
						// Starts by looking for the trk tag
						if (trkSeg.equals("trkseg"))
						{
							while (parser.next() != XmlPullParser.END_TAG)
							{
								if (parser.getEventType() != XmlPullParser.START_TAG)
								{
									continue;
								}
								String trkpt = parser.getName();
								// Starts by looking for the trk tag
								if (trkpt.equals("trkpt"))
								{
									locs.add(readLocation(parser));
								}
								else
								{
									skip(parser);
								}
							}
						}
						else if (trkSeg.equals("name"))
						{
							title = parser.getText();
						}
						else
						{
							skip(parser);
						}
					}
				}
				else
				{
					skip(parser);
				}
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		locations = locs;
	}

	private Location readLocation(XmlPullParser parser)
			throws XmlPullParserException, IOException
	{
		Time timer = new Time();
		Location res = new Location("PFDeporte");
		parser.require(XmlPullParser.START_TAG, "", "trkpt");
		double lat = Double.parseDouble(parser.getAttributeValue(null, "lat")), lon = Double
				.parseDouble(parser.getAttributeValue(null, "lon"));
		long time;
		res.setLatitude(lat);
		res.setLongitude(lon);
		while (parser.next() != XmlPullParser.END_TAG)
		{
			if (parser.getEventType() != XmlPullParser.START_TAG)
			{
				continue;
			}
			String name = parser.getName();
			if (name.equals("time"))
			{
				if (parser.next() == XmlPullParser.TEXT)
				{
					timer.parse3339(parser.getText());
					time = timer.toMillis(false);
					res.setTime(time);
					parser.nextTag();
				}
			}
			else if (name.equals("ele"))
			{
				if (parser.next() == XmlPullParser.TEXT)
				{
					res.setAltitude(Double.parseDouble(parser.getText()));
					parser.nextTag();
				}
			}
			else
			{
				skip(parser);
			}
		}
		return res;
	}

	private void skip(XmlPullParser parser) throws XmlPullParserException,
			IOException
	{
		if (parser.getEventType() != XmlPullParser.START_TAG)
		{
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0)
		{
			switch (parser.next())
			{
				case XmlPullParser.END_TAG:
					depth--;
					break;
				case XmlPullParser.START_TAG:
					depth++;
					break;
			}
		}
	}

	public Track(List<Location> locs)
	{
		locations = locs;
		endomondoWorkoutId = "";
		runtasticWorkoutId = "";
	}

	public String getStartTime()
	{
		Time timer = new Time();
		timer.set(locations.get(0).getTime());
		return timer.format3339(false);
		// return timer.format("%Y-%m-%dT%H:%M:%SZ");
	}

	public String exportAsGPX()
	{
		return (title != null) ? (getGPXFromLocations(title, locations))
				: getGPXFromLocations(locations);
	}

	public static String getGPXFromLocations(List<Location> locations)
	{
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		Time timer = new Time();
		// timer.setToNow();
		String res = null;
		try
		{
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.setPrefix("xsi", NS_SCHEMA);
			serializer.setPrefix("gpx10", NS_GPX_11);
			serializer.text("\n");
			serializer.startTag("", "gpx");
			serializer.attribute(null, "version", "1.1");
			serializer.attribute(null, "creator", "PFDeporte");
			serializer.attribute(NS_SCHEMA, "schemaLocation", NS_GPX_11
					+ " http://www.topografix.com/gpx/1/1/gpx.xsd");
			serializer.attribute(null, "xmlns", NS_GPX_11);

			// metadata
			// serializer.text("\n");
			// serializer.startTag("", "metadata");
			// serializer.text("\n");
			// serializer.startTag("", "time");
			// serializer.text(timer.format("%Y-%m-%dT%H:%M:%SZ"));
			// serializer.endTag("", "time");
			// serializer.text("\n");
			// serializer.endTag("", "metadata");

			// track info
			serializer.text("\n");
			serializer.startTag("", "trk");
			serializer.text("\n");
			// The list of segments in the track, in our case we will register a
			// single segment
			serializer.startTag("", "trkseg");
			serializer.text("\n");
			for (Location l : locations)
			{
				serializer.startTag("", "trkpt");
				serializer.attribute("", "lat",
						Double.toString(l.getLatitude()));
				serializer.attribute("", "lon",
						Double.toString(l.getLongitude()));
				serializer.text("\n");
				serializer.startTag("", "time");
				timer.set(l.getTime());
				// serializer.text(timer.format("%Y-%m-%dT%H:%M:%SZ"));
				serializer.text(timer.format3339(false));
				serializer.endTag("", "time");
				serializer.startTag("", "ele");
				// serializer.text(timer.format("%Y-%m-%dT%H:%M:%SZ"));
				serializer.text(Double.toString(l.getAltitude()));
				serializer.endTag("", "ele");
				serializer.text("\n");
				serializer.endTag("", "trkpt");
				serializer.text("\n");
			}
			serializer.endTag("", "trkseg");
			serializer.text("\n");
			serializer.endTag("", "trk");
			serializer.text("\n");
			serializer.endTag("", "gpx");
			serializer.endDocument();
			res = writer.toString();
			// Log.d(MyFunctions.TAG, "Document: \n" + res);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return res;
	}

	public static String getGPXFromLocations(String title,
			List<Location> locations)
	{
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		Time timer = new Time();
		// timer.setToNow();
		String res = null;
		try
		{
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.setPrefix("xsi", NS_SCHEMA);
			serializer.setPrefix("gpx10", NS_GPX_11);
			serializer.text("\n");
			serializer.startTag("", "gpx");
			serializer.attribute(null, "version", "1.1");
			serializer.attribute(null, "creator", "PFDeporte");
			serializer.attribute(NS_SCHEMA, "schemaLocation", NS_GPX_11
					+ " http://www.topografix.com/gpx/1/1/gpx.xsd");
			serializer.attribute(null, "xmlns", NS_GPX_11);

			// metadata
			// serializer.text("\n");
			// serializer.startTag("", "metadata");
			// serializer.text("\n");
			// serializer.startTag("", "time");
			// serializer.text(timer.format("%Y-%m-%dT%H:%M:%SZ"));
			// serializer.endTag("", "time");
			// serializer.text("\n");
			// serializer.endTag("", "metadata");

			// track info
			serializer.text("\n");
			serializer.startTag("", "trk");
			serializer.text("\n");
			// The list of segments in the track, in our case we will register a
			// single segment
			serializer.startTag("", "name");
			serializer.text(title);
			serializer.endTag("", "name");
			serializer.text("\n");
			serializer.startTag("", "trkseg");
			serializer.text("\n");
			for (Location l : locations)
			{
				serializer.startTag("", "trkpt");
				serializer.attribute("", "lat",
						Double.toString(l.getLatitude()));
				serializer.attribute("", "lon",
						Double.toString(l.getLongitude()));
				serializer.text("\n");
				serializer.startTag("", "time");
				timer.set(l.getTime());
				// serializer.text(timer.format("%Y-%m-%dT%H:%M:%SZ"));
				serializer.text(timer.format3339(false));
				serializer.endTag("", "time");
				serializer.startTag("", "ele");
				// serializer.text(timer.format("%Y-%m-%dT%H:%M:%SZ"));
				serializer.text(Double.toString(l.getAltitude()));
				serializer.endTag("", "ele");
				serializer.text("\n");
				serializer.endTag("", "trkpt");
				serializer.text("\n");
			}
			serializer.endTag("", "trkseg");
			serializer.text("\n");
			serializer.endTag("", "trk");
			serializer.text("\n");
			serializer.endTag("", "gpx");
			serializer.endDocument();
			res = writer.toString();
			Log.d(MyFunctions.TAG, "Document: \n" + res);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return res;
	}

	public String getTitle()
	{
		if (title != null)
		{
			return title;
		}
		return getStartTime();
	}

	public boolean isUploadedToEndomondo()
	{
		return endomondo;
	}

	public boolean isUploadedToRuntastic()
	{
		return runtastic;
	}

	public boolean isInLocalStorage()
	{
		return local;
	}

	public void setInLocalStorage(boolean b)
	{
		local = b;
	}

	public void setUploadedToEndomondo(boolean b)
	{
		endomondo = b;
	}

	public void setUploadedToRuntastic(boolean b)
	{
		runtastic = b;
	}

	@Override
	public boolean equals(Object o)
	{
		Track aux;
		List<Location> auxLocations;
		Location currentLocation, foreignLocation;
		if (o instanceof Track)
		{
			aux = (Track) o;
			auxLocations = aux.getLocations();
			if (aux.getTitle().equals(getTitle()))
			{
				int a = 0;
			}
			if (auxLocations.size() == locations.size())
			{
				if (auxLocations.size() >= 100)
				{
					// We take 40 random points and compare their value
					Random r = new Random();
					int randomNumber;
					for (int i = 0; i < 40; i++)
					{
						randomNumber = r.nextInt(auxLocations.size());
						currentLocation = locations.get(randomNumber);
						foreignLocation = auxLocations.get(randomNumber);
						if (currentLocation.getTime() != foreignLocation.getTime()
								|| Math.abs(round(currentLocation.getLatitude(), 6) - round(foreignLocation.getLatitude(), 6)) >= MAX_ERROR
								|| Math.abs(round(currentLocation.getLongitude(), 6) - round(foreignLocation.getLongitude(), 6)) >= MAX_ERROR)
						{
							return false;
						}
					}
					return true;
				}
				else
				{
					for (int i = 0; i < locations.size(); i++)
					{
						currentLocation = locations.get(i);
						foreignLocation = auxLocations.get(i);
						if (currentLocation.getTime() != foreignLocation.getTime()
								|| Math.abs(round(currentLocation.getLatitude(), 6) - round(foreignLocation.getLatitude(), 6)) >= MAX_ERROR
								|| Math.abs(round(currentLocation.getLongitude(), 6) - round(foreignLocation.getLongitude(), 6)) >= MAX_ERROR)
						{
							return false;
						}
					}
					return true;
				}
			}
		}
		return false;
	}

	public static double round(double value, int places)
	{
		long factor = (long) Math.pow(10, places);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}

	public List<Location> getLocations()
	{
		return locations;
	}

	@Override
	public int compareTo(Track trk)
	{
		Time foreignTimer = new Time(), myTimer = new Time();
		foreignTimer.parse3339(trk.getStartTime());
		myTimer.parse3339(getStartTime());
		if (myTimer.before(foreignTimer))
		{
			return -1;
		}
		else if (myTimer.after(foreignTimer))
		{
			return 1;
		}
		return 0;
	}

	public Long getID()
	{
		return id;
	}

	public void setID(Long i)
	{
		id = i;
	}

	@Override
	public int hashCode()
	{
		long sum = 0;
		int res;
		for (Location l : locations)
		{
			sum += l.getTime() - l.getLatitude() - l.getLongitude();
		}
		// I take the three less significant bytes of sum (8 bytes) to create an
		// int
		res = (int) ((sum & 0xFF) + ((sum >> 8) & 0xFF) + ((sum >> 16) & 0xFF));
		return res;
	}
}