package org.linphone.setup;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.linphone.LinphoneActivity;
import org.linphone.mediastream.Log;
import org.linphone.vtcsecure.g;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Created by 3537 on 1/21/2016.
 */
public class CDNProviders {

	final static String PROVIDERS_KEY = "providers_key";
	final static String SELECTED_PROVIDER_NAME = "selected_provider_name";
	private static CDNProviders instance;
	private Context context;
	private int providersCount;
	private ArrayList<Provider> providers;
	private Provider selectedProvider;


	private SharedPreferences sharedPreferences;


	public static CDNProviders getInstance() {
		if (instance == null) {
			instance = new CDNProviders();
			instance.load();
		}
		return instance;
	}

	public CDNProviders() {
		providers = new ArrayList<Provider>();
		if (null != LinphoneActivity.ctx) {
			context = LinphoneActivity.ctx;
			sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
			load();

		}
	}


	private void load() {
		//Load cached providers and their domains
		String json = sharedPreferences.getString(PROVIDERS_KEY, "");
		if (json.length() > 0)
			updateProviders(json, false);
	}

	public void setSelectedProvider(int possition) {
		setSelectedProvider(providers.get(possition));
	}

	public Provider getSelectedProvider() {
		return selectedProvider;
	}

	public int getSelectedProviderPosition() {
		int position = 0;
		if (getSelectedProvider() != null)
			for (int i = 0; i < getProvidersCount(); i++) {
				if (providers.get(i).getName().equals(getSelectedProvider().getName())) {
					position = i;
					break;
				}
			}
		return position;
	}

	public int getProviderPossition(String domain)
	{
		if(providers==null || providers.size() ==0)
			return -1;
		int poss = -1;
		int i = 0;
		for (Provider pItem : providers) {
			if (pItem.getDomain().equals(domain))
			{
				poss = i;
				break;
			}
			i++;
		}
		return  poss;
	}


	public Provider getProvider(int poss) {
		return providers.get(poss);
	}

	public void setSelectedProvider(Provider provider) {
		for (Provider provider1 : providers) {
			if (provider.equals(provider1)) {
				this.selectedProvider = provider1;
				try {
					SharedPreferences.Editor editor = sharedPreferences.edit();
					editor.putString(SELECTED_PROVIDER_NAME, provider1.getName()).apply();
				}catch(Throwable e){
						e.printStackTrace();
				}
				break;
			}
		}
	}

	public ArrayList<Provider> getProviders() {
		return providers;// (ArrayList) providers.clone();
	}

	public boolean updateProviders(String jsonString, boolean save) {
		try {
			updateProviders(new JSONArray(jsonString));

		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
		if (save) {
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString(PROVIDERS_KEY, jsonString);
			editor.commit();
		}
		return true;


	}
	private static final Object locker = new Object();
	private void updateProviders(JSONArray jsonArray) {
		synchronized (locker) {
			providers.clear();
			g.domain_image_hash = new HashMap<String, Uri>();
			Provider tmp;
			for (int i = 0; i < jsonArray.length(); i++) {
				tmp = new Provider();
				try {

					//change domains to valid domains, if not listed on the server that way
					if (((JSONObject) jsonArray.get(i)).getString("domain").equals("zvrs.vatrp.net")) {
						tmp.domain = "208.94.16.87";
					} else if (((JSONObject) jsonArray.get(i)).getString("domain").equals("psip-lb.staging.purple.us")) {
						tmp.domain = "psip-lb.test.purple.us";
					} else {
						tmp.domain = ((JSONObject) jsonArray.get(i)).getString("domain");
					}

					tmp.name = ((JSONObject) jsonArray.get(i)).getString("name");
					tmp.icon = ((JSONObject) jsonArray.get(i)).getString("icon");
					tmp.icon2x = ((JSONObject) jsonArray.get(i)).getString("icon2x");
					//tmp.port=srvLookup

					providers.add(tmp);


				} catch (JSONException e) {
					e.printStackTrace();
				}


			}

			updateProvidersDefaultPorts();
		}

		//Add default ports from srvLookup after all providers are popluated to prevent cross threading
//		for (int i = 0; i < jsonArray.length(); i++) {
//			Log.d("cdnprovidersss : " + providers.size() + "  of json   " + jsonArray.length());
//			updateProvidersDefaultPorts();
//		}

	}

	public void updateProvidersDefaultPorts(){
		Thread thread = new Thread(new Runnable() {
			public void run() {
				synchronized (locker) {
					for (int position = 0; position < providers.size(); position++) {
						providers.get(position);
						String recordName = "_sip._tcp." + providers.get(position).domain;
						Lookup lookup = null;
						try {
							lookup = new Lookup(recordName, Type.SRV);
							Record recs[] = lookup.run();
							providers.get(position).port = ((SRVRecord) recs[0]).getPort();
							Log.d("Setting Provider Port " + providers.get(position).domain + " port to " + providers.get(position).port);
						} catch (Throwable e) {
							providers.get(position).port = 5060;
							Log.d("No port found for provider " + providers.get(position).domain + " setting default port to " + providers.get(position).port);
							//e.printStackTrace();

						}
					}
				}
			}
		});
		thread.start();
	}

	public int getProvidersCount() {
		providersCount = providers.size();
		return providersCount;
	}


	public class Provider {
		private String domain, icon2x, icon, name;
		private int port;

		public String getDomain() {
			return domain;
		}

		public String getIcon2x() {
			return icon2x;
		}

		public String getIcon() {
			return icon;
		}

		public String getName() {
			return name;
		}

		public int getPort() {
			return port;
		}
	}
}
