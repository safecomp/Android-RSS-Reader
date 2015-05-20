package com.safecomp.rssreader;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

public class MainActivity extends Activity {

	ListView feedList;
	ProgressBar listEmpty;
	LinkedList<String> titles;
	LinkedList<String> links;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		feedList = (ListView) findViewById(R.id.feedList);
		listEmpty = (ProgressBar) findViewById(R.id.listEmpty);

		feedList.setEmptyView(listEmpty);
		titles = new LinkedList<String>();
		links = new LinkedList<String>();
		RSSReader rr = new RSSReader();
		rr.execute("");
	}

	class MyAdapter extends ArrayAdapter {
		LinkedList<String> titles;
		LinkedList<String> links;

		public MyAdapter(Context context, int rID, LinkedList<String> titles,
				LinkedList<String> links) {
			super(context, rID, titles);
			this.titles = titles;
			this.links = links;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			LayoutInflater inflater = getLayoutInflater();
			View v = inflater.inflate(R.layout.list_item, parent, false);

			Button btnItem = (Button) v.findViewById(R.id.btnItem);
			btnItem.setText(Html.fromHtml("<a href='" + links.get(position)+ "'>" + titles.get(position) + "</a>"));
			btnItem.setMovementMethod(LinkMovementMethod.getInstance());

			return v;
		}
	}

	class RSSReader extends AsyncTask<String, Integer, Integer> {

		@Override
		protected Integer doInBackground(String... arg0) {
			read();
			return null;
		}

		@Override
		protected void onPostExecute(Integer result) {
			Log.i("rss", "done");

			MyAdapter adapter = new MyAdapter(MainActivity.this,
					R.layout.list_item, titles, links);
			feedList.setAdapter(adapter);

			adapter.notifyDataSetChanged();
			super.onPostExecute(result);
		}

	}

	public void read() {
		try {
			URL url = new URL("http://www.safecomp.ir/rss.xml");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			// conn.setReadTimeout(10000 /* milliseconds */);
			// conn.setConnectTimeout(15000 /* milliseconds */);
			// conn.setRequestMethod("GET");
			// conn.setDoInput(true);
			// Starts the query
			// conn.connect();
			Log.i("rss", "connected ");

			InputStream stream = conn.getInputStream();
			Log.i("rss", "stream " + stream + "");
			XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory
					.newInstance();
			XmlPullParser myparser = xmlFactoryObject.newPullParser();
			myparser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			myparser.setInput(stream, null);
			parseXML(myparser);
			stream.close();
		} catch (Exception e) {
			Log.i("rss", "read Error " + e.getMessage());
		}
	}

	public void parseXML(XmlPullParser parser) {
		Log.i("rss", "start Reading");
		String title, link;
		String tagName;
		int eventType;
		try {
			// skip <rss> tag!
			parser.next();

			// next tag should be <channel>
			eventType = parser.next();

			while (eventType != XmlPullParser.END_DOCUMENT) {

				tagName = parser.getName();

				if (eventType == XmlPullParser.START_TAG) {
					if (tagName.equals("channel")) {
						readChannel(parser);
					} else {
						// current tag is not <channel> , assume it
						// <non-channel> , find </non-channel>
						goToEndTag(parser);
					}
				}
				eventType = parser.next();
			}
		} catch (Exception e) {
			Log.i("rss", "rss " + e.getMessage());
		}

	}

	void goToEndTag(XmlPullParser parser) {
		try {
			int depth = 1;
			while (depth != 0) {
				switch (parser.next()) {
				case XmlPullParser.END_TAG:
					depth--;
					break;
				case XmlPullParser.START_TAG:
					depth++;
					break;
				}
			}
		} catch (Exception e) {
		}
	}

	public void readChannel(XmlPullParser parser) {
		Log.i("rss", "reading channel");
		int eventType;
		String tagName;
		try {
			eventType = parser.next();
			while (eventType != XmlPullParser.END_TAG) {
				tagName = parser.getName();
				if (eventType == XmlPullParser.START_TAG) {
					if (tagName.equals("item")) {
						readItem(parser);
					} else {
						goToEndTag(parser);
					}
				}
				eventType = parser.next();
			}

		} catch (Exception e) {
			Log.i("rss", "channel error " + e.getMessage());
		}
	}

	public void readItem(XmlPullParser parser) {
		int eventType;
		String tagName;
		String title = "", link = "";
		Log.i("rss", "reading Item");
		try {
			eventType = parser.next();
			while (eventType != XmlPullParser.END_TAG) {
				tagName = parser.getName();
				if (eventType == XmlPullParser.START_TAG) {
					if (tagName.equals("title")) {
						if (parser.next() == XmlPullParser.TEXT) {
							title = parser.getText();
							titles.add(title);
						}
					} else if (tagName.equals("link")) {
						if (parser.next() == XmlPullParser.TEXT) {
							link = parser.getText();
							links.add(link);
							Log.i("rss", title + " [" + link + "]");
						}

					}
					goToEndTag(parser);

				}

				eventType = parser.next();
			}

		} catch (Exception e) {
			Log.i("rss", "item error " + e.getMessage());
		}
	}

}
