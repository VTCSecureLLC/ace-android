package org.linphone.setup;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.linphone.LinphoneActivity;
import org.linphone.LinphoneManager;
import org.linphone.LinphonePreferences;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.PayloadType;
import org.linphone.mediastream.Log;
import org.linphone.vtcsecure.Utils;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

import de.timroes.axmlrpc.AuthenticationManager;


/**
 * Created by Vardan on 12/17/2015.
 */
public class JsonConfig {
	private final static String FILE_NAME = "json_config";
	private String _json;
	private String _request_url;
	private int _version;
	private int _expiration_time;
	private String _configuration_auth_password;//user manual
	private String _configuration_auth_expiration;
	private int _sip_registration_maximum_threshold;
	private String[] _sip_register_usernames;


	public String getSipAuthUsername() {
		return _sip_auth_username;
	}

	public String getSipAuthPassword() {
		return _sip_auth_password;
	}

	private String _sip_auth_username;
	private String _sip_auth_password;
	private String _sip_register_domain;
	private int _sip_register_port;
	private String _sip_register_transport;
	private boolean _enable_echo_cancellation;
	private boolean _enable_video;
	private boolean _enable_rtt;
	private boolean _enable_adaptive_rate;
	private ArrayList<String> _enabled_codecs;
	private String _bwLimit;
	private int _upload_bandwidth;
	private int _download_bandwidth;
	private boolean _enable_stun;
	private String _stun_server;
	private boolean _enable_ice;
	private String _logging;
	private String _sip_mwi_uri;
	private String _sip_videomail_uri;
	private String _video_resolution_maximum;
	private int _video_preferred_frames_per_second;


	public void applySettings(LinphoneAddress.TransportType transport_type, String port) {
		//Apply default or autoconfig app settings here.
		applyAudioCodecs();
		applyVideoCodecs();
		applyOtherConfig(transport_type, port);
	}


	public void saveFile() {
		if (_json == null)
			return;
		File file = new File(LinphoneManager.getInstance().getContext().getFilesDir(), FILE_NAME);
		if (file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}

		FileOutputStream outputStream;

		try {
			outputStream = LinphoneManager.getInstance().getContext().openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
			outputStream.write(_json.getBytes());
			outputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void applyAudioCodecs() {

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		for (final PayloadType pt : lc.getAudioCodecs()) {
			try {
				lc.enablePayloadType(pt, _enabled_codecs.contains(pt.getMime()));
			} catch (LinphoneCoreException e) {
				e.printStackTrace();
			}

		}
	}

	private void applyVideoCodecs() {
		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		for (final PayloadType pt : lc.getVideoCodecs()) {
			try {
				lc.enablePayloadType(pt, _enabled_codecs.contains(pt.getMime()));
			} catch (LinphoneCoreException e) {
				e.printStackTrace();
			}
		}
	}


	private void applyOtherConfig(LinphoneAddress.TransportType transport_type, String port) {

		//If changes made at login, use those instead of the jsonconfig changes.
		_sip_register_port = Integer.parseInt(port);
		if(transport_type==LinphoneAddress.TransportType.LinphoneTransportTcp){
			_sip_register_transport="tcp";
		}else{
			_sip_register_transport="tls";
		}

		LinphonePreferences mPrefs = LinphonePreferences.instance();
		int n = mPrefs.getDefaultAccountIndex();
		if (n < 0)
			return;
		if (_sip_register_transport != null && _sip_register_transport.length() > 0)
			mPrefs.setAccountTransport(n, _sip_register_transport);// need to be chacked
		if (_sip_register_domain != null && _sip_register_domain.length() > 0)
			mPrefs.setAccountDomain(n, _sip_register_domain);
		if (_expiration_time > 0)
			mPrefs.setExpires(n, _expiration_time + "");

		String proxy = "<sip:" + _sip_register_domain;
		if (_sip_register_port > 0) {
			proxy += ":" + _sip_register_port;
		}
		proxy += ";transport=" + mPrefs.getAccountTransportString(n).toLowerCase() + ">";
		mPrefs.setAccountProxy(n, proxy);

		mPrefs.setEchoCancellation(_enable_echo_cancellation);
		mPrefs.enableVideo(_enable_video, _enable_video);
		LinphoneManager.getInstance().setRttEnabled(_enable_rtt);
		mPrefs.enableAdaptiveRateControl(_enable_adaptive_rate);
		if (_enable_stun && _stun_server != null)
			mPrefs.setStunServer(_stun_server);
		else
			mPrefs.setStunServer("");
		mPrefs.setIceEnabled(_enable_ice);
		if (_logging != null && _logging.toLowerCase().equals("debug"))
			mPrefs.setDebugEnabled(true);

		mPrefs.setPreferredVideoSize(_video_resolution_maximum);


		if (_video_preferred_frames_per_second==0) {
			_video_preferred_frames_per_second=30;
		}
		mPrefs.setPreferredVideoFps(_video_preferred_frames_per_second);

		LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
		if (lc != null) {
			if (_upload_bandwidth > 0)
				lc.setUploadBandwidth(_upload_bandwidth);
			if (_download_bandwidth > 0)
				lc.setDownloadBandwidth(_download_bandwidth);
			if (_bwLimit != null)
				lc.setVideoPreset(_bwLimit);
		}

		//VATRP-3143 set ipv6 enabled by default.
		mPrefs.useIpv6(true);
		//VATRP-3143 set ice enabled by default.
		mPrefs.setIceEnabled(true);
		//VATRP-
		mPrefs.setStunServer(_sip_register_domain);
	}


	private static JsonConfig parseJson(String json, String _request_url) throws JSONException {

		JsonConfig config = new JsonConfig();
		JSONObject ob = new JSONObject(json);
		config._json = json;
		config._request_url = _request_url;
		config._version = ob.getInt("version");
		config._expiration_time = ob.getInt("expiration_time");
		if (!ob.isNull("configuration_auth_password"))
			config._configuration_auth_password = Utils.removeExtraQuotesFromStringIfPresent(ob.getString("configuration_auth_password"));
		if (!ob.isNull("configuration_auth_expiration"))
			config._configuration_auth_expiration = Utils.removeExtraQuotesFromStringIfPresent(ob.getString("configuration_auth_expiration"));

		if (!ob.isNull("sip_registration_maximum_threshold"))
			config._sip_registration_maximum_threshold = ob.getInt("sip_registration_maximum_threshold");
		if (!ob.isNull("sip_auth_username"))
			config._sip_auth_username = Utils.removeExtraQuotesFromStringIfPresent(ob.getString("sip_auth_username"));
		if (!ob.isNull("sip_auth_password"))
			config._sip_auth_password = Utils.removeExtraQuotesFromStringIfPresent(ob.getString("sip_auth_password"));
		if (!ob.isNull("sip_register_domain"))
			config._sip_register_domain = Utils.removeExtraQuotesFromStringIfPresent(ob.getString("sip_register_domain"));

		//If changes made at login, use those instead of the jsonconfig changes.
		config._sip_register_port = ob.getInt("sip_register_port");
		if (!ob.isNull("sip_register_transport"))
			config._sip_register_transport = Utils.removeExtraQuotesFromStringIfPresent(ob.getString("sip_register_transport"));



		config._enable_echo_cancellation = ob.getBoolean("enable_echo_cancellation");
		config._enable_video = ob.getBoolean("enable_video");
		config._enable_rtt = ob.getBoolean("enable_rtt");
		config._enable_adaptive_rate = ob.getBoolean("enable_adaptive_rate");
		if (!ob.isNull("bwLimit"))// is not used
			config._bwLimit = Utils.removeExtraQuotesFromStringIfPresent(ob.getString("bwLimit"));
		config._upload_bandwidth = ob.getInt("upload_bandwidth");
		config._download_bandwidth = ob.getInt("download_bandwidth");
		config._enable_stun = ob.getBoolean("enable_stun");
		config._stun_server = Utils.removeExtraQuotesFromStringIfPresent(ob.getString("stun_server"));
		config._enable_ice = ob.getBoolean("enable_ice");
		if (!ob.isNull("logging"))
			config._logging = Utils.removeExtraQuotesFromStringIfPresent(ob.getString("logging")); // enabled debug
		if (!ob.isNull("sip_mwi_uri"))
			config._sip_mwi_uri = Utils.removeExtraQuotesFromStringIfPresent(ob.getString("sip_mwi_uri"));
		if (!ob.isNull("sip_videomail_uri")) // not used
			config._sip_videomail_uri = Utils.removeExtraQuotesFromStringIfPresent(ob.getString("sip_videomail_uri"));
		if (!ob.isNull("video_resolution_maximum"))
			config._video_resolution_maximum = Utils.removeExtraQuotesFromStringIfPresent(ob.getString("video_resolution_maximum"));//prefared res

		//fps
		if (!ob.isNull("video_preferred_frames_per_second")) {
			config._video_preferred_frames_per_second = ob.getInt("video_preferred_frames_per_second");//prefared res
		}else{
			config._video_preferred_frames_per_second=30;
		}

		JSONArray jsonArray = ob.getJSONArray("sip_register_usernames");// not used
		config._sip_register_usernames = new String[jsonArray.length()];// codec mapping is required
		for (int i = 0; i < jsonArray.length(); i++)
			config._sip_register_usernames[i] = Utils.removeExtraQuotesFromStringIfPresent(jsonArray.getString(i));

		jsonArray = ob.getJSONArray("enabled_codecs");
		config._enabled_codecs = new ArrayList<String>();
		for (int i = 0; i < jsonArray.length(); i++)
			config._enabled_codecs.add(Utils.removeExtraQuotesFromStringIfPresent(jsonArray.getString(i).replace(".", "")));// as we don't have mapping yet

		if (config._enabled_codecs.contains("G711"))// as we don't have mapping yet
		{
			config._enabled_codecs.add("PCMA");
			config._enabled_codecs.add("PCMU");
		}
		return config;

	}

	public static void refreshConfig(String dns_srv_uri, String username, String password, ConfigListener listener) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			new ConfigUpdater(dns_srv_uri, username, password, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			new ConfigUpdater(dns_srv_uri, username, password, listener).execute();
		}
	}

	public final static String json = "{\n" +
			"  \"version\": 1,\n" +
			"  \"expiration_time\": 280,\n" +
			"  \"configuration_auth_password\": \"\",\n" +
			"  \"configuration_auth_expiration\": -1,\n" +
			"  \"sip_registration_maximum_threshold\": 10,\n" +
			"  \"sip_register_usernames\": [],\n" +
			"  \"sip_auth_username\": \"\",\n" +
			"  \"sip_auth_password\": \"\",\n" +
			"  \"sip_register_domain\": \"bc1.vatrp.net\",\n" +
			"  \"sip_register_port\": 5060,\n" +
			"  \"sip_register_transport\": \"tcp\",\n" +
			"  \"enable_echo_cancellation\": \"true\",\n" +
			"  \"enable_video\": \"true\",\n" +
			"  \"enable_rtt\": \"true\",\n" +
			"  \"enable_adaptive_rate\": \"true\",\n" +
			"  \"enabled_codecs\": [\"H.264\",\"H.263\",\"VP8\",\"G.722\",\"G.711\"],\n" +
			"  \"bwLimit\": \"high-fps\",\n" +
			"  \"upload_bandwidth\": 660,\n" +
			"  \"download_bandwidth\": 660,\n" +
			"  \"enable_stun\": \"false\",\n" +
			"  \"stun_server\": \"\",\n" +
			"  \"enable_ice\": \"false\",\n" +
			"  \"logging\": \"info\",\n" +
			"  \"sip_mwi_uri\": \"\",\n" +
			"  \"sip_videomail_uri\": \"\",\n" +
			"  \"video_resolution_maximum\": \"cif\"\n" +
			"}";


	private static class ConfigUpdater extends AsyncTask<Void, Void, JsonConfig> {

		ConfigListener listener;
		String request_url;
		String query_url;
		String username, password;
		String errorMsg;

		public ConfigUpdater(String url, String username, String password, ConfigListener listener) {
			this.username = username;
			this.password = password;
			this.listener = listener;
			query_url = url;
			errorMsg = "Failed to Login";
		}


		@Override
		protected JsonConfig doInBackground(Void... params) {
			Record[] records;// = new Record[0];
			try {
				records = new Lookup(query_url, Type.SRV).run();
			} catch (TextParseException e) {
				e.printStackTrace();
				return null;
			}
			if(records != null && records.length > 0) {
				for (Record record : records) {
					SRVRecord srv = (SRVRecord) record;

					String hostname = srv.getTarget().toString().replaceFirst("\\.$", "");

					request_url = "https://" + hostname + "/config/v1/config.json";
					Log.d("Auto Config request_url: "+request_url);
				}
//				if (request_url == null) {
//					try {
//						return parseJson(json, request_url);
//					} catch (JSONException e) {
//						return null;
//					}
//				}

				try {
					String reponse_str = getFromHttpURLConnection();
					Log.d("Auto Config JSON: "+reponse_str);
					return parseJson(reponse_str, request_url);
				} catch (Throwable e){
					Log.d("Issue parsing json");
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
			try {
				LinphoneActivity.instance().display_all_core_values(lc, "Pre-AutoConfig");
			}catch(Throwable e){
				e.printStackTrace();
				Log.d("can't display core values");
			}
			Log.d("Starting autoconfig download");
			LinphoneActivity.instance().generic_ace_loading_dialog = new ProgressDialog(LinphoneActivity.instance());
			LinphoneActivity.instance().generic_ace_loading_dialog.setCancelable(true);
			LinphoneActivity.instance().generic_ace_loading_dialog.setMessage("Loading Auto Configuration Based on User...");
			LinphoneActivity.instance().generic_ace_loading_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			LinphoneActivity.instance().generic_ace_loading_dialog.setProgress(0);
			LinphoneActivity.instance().generic_ace_loading_dialog.show();

		}
		@Override
		protected void onPostExecute(JsonConfig res) {
			super.onPostExecute(res);
			if (res != null) {
				if (listener != null) {
					listener.onParsed(res);
					LinphoneActivity.instance().generic_ace_loading_dialog.cancel();
//					new AlertDialog.Builder(LinphoneActivity.instance())
//							.setMessage("Configuration Loaded Successfully")
//							.setTitle("Auto-Configuration")
//							.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//								@Override
//								public void onClick(DialogInterface dialogInterface, int i) {
//								}
//							}).show();
				}
			} else {
				if (listener != null) {
					listener.onFailed(errorMsg);
					LinphoneActivity.instance().generic_ace_loading_dialog.cancel();
//					new AlertDialog.Builder(LinphoneActivity.instance())
//							.setMessage("Configuration Not Loaded")
//							.setTitle("Auto-Configuration")
//							.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//								@Override
//								public void onClick(DialogInterface dialogInterface, int i) {
//								}
//							}).show();
				}
			}

		}


		String getFromHttpURLConnection() throws ProtocolException, IOException {


			HttpURLConnection conn = null;


			URL url = new URL(request_url);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoInput(true);

			conn.setRequestProperty("Content-type", "application/json");
			AuthenticationManager authenticationManager = new AuthenticationManager();
			authenticationManager.setAuthData(username, password);
			authenticationManager.setAuthentication(conn);

			InputStream is; // = conn.getInputStream();

			if (conn.getResponseCode() == 200) {
				is = conn.getInputStream();
				BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
				String line;
				StringBuffer response = new StringBuffer();
				while ((line = rd.readLine()) != null) {
					response.append(line);
				}
				rd.close();
				is.close();

				return response.toString();
			} else {
				errorMsg = "UnAuthorized";
			}


			return "";
		}

		String downloadDigest(URL url) {
			return "";
		}
	}

	public static interface ConfigListener {
		public void onParsed(JsonConfig config);

		public void onFailed(String reason);

	}


}