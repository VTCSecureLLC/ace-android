package org.linphone;
/*
ChatListFragment.java
Developed pursuant to contract FCC15C0008 as open source software under GNU General Public License version 2.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.mediastream.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Sylvain Berfini
 */
public class ChatListFragment extends Fragment implements OnClickListener, OnItemClickListener {
	private LayoutInflater mInflater;
	private List<String> mConversations, mDrafts;
	private ListView chatList;
	private TextView ok, noChatHistory;
	private ImageView clearFastChat;
	private EditText fastNewChat;
	private boolean isEditMode = false;
	private boolean useLinphoneStorage;
	private ChatListAdapter chatListAdapter;
	private List<String> findedAddresses;
	private TextView sendMessageView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		mInflater = inflater;

		View view = inflater.inflate(R.layout.chatlist, container, false);
		chatList = (ListView) view.findViewById(R.id.chatList);
		chatList.setOnItemClickListener(this);
		if(LinphonePreferences.instance().areAnimationsEnabled()) {
			LayoutAnimationController lac = new LayoutAnimationController(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_right_to_left), 0.1f); //0.5f == time between appearance of listview items.
			chatList.setLayoutAnimation(lac);
		}
		registerForContextMenu(chatList);

		noChatHistory = (TextView) view.findViewById(R.id.noChatHistory);

		ok = (TextView) view.findViewById(R.id.ok);
		ok.setOnClickListener(this);

		clearFastChat = (ImageView) view.findViewById(R.id.image_add_new_conversation);
		clearFastChat.setOnClickListener(this);

		sendMessageView = (TextView) view.findViewById(R.id.label_chatlist_send_message);
		chatListAdapter = new ChatListAdapter(useLinphoneStorage);
		return view;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		searchContact(view);
	}

	private void hideAndDisplayMessageIfNoChat() {
		if (mConversations.size() == 0 && mDrafts.size() == 0) {
			noChatHistory.setVisibility(View.VISIBLE);
			chatList.setVisibility(View.GONE);
		} else {
			noChatHistory.setVisibility(View.GONE);
			chatList.setVisibility(View.VISIBLE);
			chatList.setAdapter(chatListAdapter);
		}
	}

	public void refresh() {
		mConversations = LinphoneActivity.instance().getChatList();
		mDrafts = LinphoneActivity.instance().getDraftChatList();
		mConversations.removeAll(mDrafts);
		hideAndDisplayMessageIfNoChat();
	}

	private boolean isVersionUsingNewChatStorage() {
		try {
			Context context = LinphoneActivity.instance();
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode >= 2200;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();

		sendMessageView.setVisibility(View.GONE);
		//Check if the is the first time we show the chat view since we use liblinphone chat storage
		useLinphoneStorage = getResources().getBoolean(R.bool.use_linphone_chat_storage);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.instance());
		boolean updateNeeded = prefs.getBoolean(getString(R.string.pref_first_time_linphone_chat_storage), true);
		updateNeeded = updateNeeded && !isVersionUsingNewChatStorage();
		if (useLinphoneStorage && updateNeeded) {
			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
				private ProgressDialog pd;
				@Override
				protected void onPreExecute() {
					pd = new ProgressDialog(LinphoneActivity.instance());
					pd.setTitle(getString(R.string.wait));
					pd.setMessage(getString(R.string.importing_messages));
					pd.setCancelable(false);
					pd.setIndeterminate(true);
					pd.show();
				}
				@Override
				protected Void doInBackground(Void... arg0) {
					try {
						if (importAndroidStoredMessagedIntoLibLinphoneStorage()) {
							prefs.edit().putBoolean(getString(R.string.pref_first_time_linphone_chat_storage), false).commit();
							LinphoneActivity.instance().getChatStorage().restartChatStorage();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					return null;
				}
				@Override
				protected void onPostExecute(Void result) {
					pd.dismiss();
				}
			};
			task.execute((Void[])null);
		}

		if (LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().selectMenu(FragmentsAvailable.CHATLIST);
			LinphoneActivity.instance().updateChatListFragment(this);

			if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
				LinphoneActivity.instance().hideStatusBar();
			}
			LinphoneActivity.instance().unreadMessageBadge();
		}

		refresh();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, v.getId(), 0, getString(R.string.delete));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		if (info == null || info.targetView == null) {
			return false;
		}
		String sipUri = (String) info.targetView.getTag();

		LinphoneActivity.instance().removeFromChatList(sipUri);
		mConversations = LinphoneActivity.instance().getChatList();
		mDrafts = LinphoneActivity.instance().getDraftChatList();
		mConversations.removeAll(mDrafts);
		hideAndDisplayMessageIfNoChat();
		return true;
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();

		if (id == R.id.image_add_new_conversation) {
			String sipUri = fastNewChat.getText().toString();
			creatSipUri(sipUri);
			fastNewChat.setText("");
		}
		else if (id == R.id.ok) {
			ok.setVisibility(View.GONE);
			isEditMode = false;
			hideAndDisplayMessageIfNoChat();
		}
	}

	private void creatSipUri(String sipUri) {

		if (sipUri.equals("")) {
			LinphoneActivity.instance().displayContacts(true);
		} else {
			if (!LinphoneUtils.isSipAddress(sipUri)) {
				if (LinphoneManager.getLc().getDefaultProxyConfig() == null) {
					return;
				}
				sipUri = sipUri + "@" + LinphoneManager.getLc().getDefaultProxyConfig().getDomain();
			}
			if (!LinphoneUtils.isStrictSipAddress(sipUri)) {
				sipUri = "sip:" + sipUri;
			}
			LinphoneActivity.instance().displayChat(sipUri);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		String sipUri = (String) view.getTag();
		fastNewChat.setText("");

		if (LinphoneActivity.isInstanciated() && !isEditMode) {
			LinphoneActivity.instance().displayChat(sipUri);
		} else if (LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().removeFromChatList(sipUri);
			LinphoneActivity.instance().removeFromDrafts(sipUri);

			mConversations = LinphoneActivity.instance().getChatList();
			mDrafts = LinphoneActivity.instance().getDraftChatList();
			mConversations.removeAll(mDrafts);
			hideAndDisplayMessageIfNoChat();

			LinphoneActivity.instance().updateMissedChatCount(false);
		}
	}

	private boolean importAndroidStoredMessagedIntoLibLinphoneStorage() {
		Log.w("Importing previous messages into new database...");
		try {
			ChatStorage db = LinphoneActivity.instance().getChatStorage();
			List<String> conversations = db.getChatList();
			for (int j = conversations.size() - 1; j >= 0; j--) {
				String correspondent = conversations.get(j);
				LinphoneChatRoom room = LinphoneManager.getLc().getOrCreateChatRoom(correspondent);
				for (ChatMessage message : db.getMessages(correspondent)) {
					LinphoneChatMessage msg = room.createLinphoneChatMessage(message.getMessage(), message.getUrl(), message.getStatus(), Long.parseLong(message.getTimestamp()), true, message.isIncoming());
					if (message.getImage() != null) {
						String path = saveImageAsFile(message.getId(), message.getImage());
						if (path != null)
							msg.setExternalBodyUrl(path);
					}
					msg.store();
				}
				db.removeDiscussion(correspondent);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	private String saveImageAsFile(int id, Bitmap bm) {
		try {
			String path = Environment.getExternalStorageDirectory().toString();
			if (!path.endsWith("/"))
				path += "/";
			path += "Pictures/";
			File directory = new File(path);
			directory.mkdirs();

			String filename = getString(R.string.picture_name_format).replace("%s", String.valueOf(id));
			File file = new File(path, filename);

			OutputStream fOut = null;
			fOut = new FileOutputStream(file);

			bm.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
			fOut.flush();
			fOut.close();

			return path + filename;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	class ChatListAdapter extends BaseAdapter {
		private boolean useNativeAPI;

		ChatListAdapter(boolean useNativeAPI) {
			this.useNativeAPI = useNativeAPI;
		}

		public int getCount() {
			return mConversations.size() + mDrafts.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}
		private int lastPosition = -1;
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = null;

			if (convertView != null) {
				view = convertView;
			} else {
				view = mInflater.inflate(R.layout.item_chat, parent, false);

			}
			String contact;
			boolean isDraft = false;
			if (position >= mDrafts.size()) {
				contact = mConversations.get(position - mDrafts.size());
			} else {
				contact = mDrafts.get(position);
				isDraft = true;
			}
			view.setTag(contact);
			int unreadMessagesCount = LinphoneActivity.instance().getChatStorage().getUnreadMessageCount(contact);

			LinphoneAddress address;
			try {
				address = LinphoneCoreFactory.instance().createLinphoneAddress(contact);
			} catch (LinphoneCoreException e) {
				Log.e("Chat view cannot parse address",e);
				return view;
			}
			Contact lContact = ContactsManager.getInstance().findContactWithAddress(getActivity().getContentResolver(), address);

			String message = "";
			Long time = null;
			if (useNativeAPI) {
				LinphoneChatRoom chatRoom = LinphoneManager.getLc().getOrCreateChatRoom(contact);
				LinphoneChatMessage[] history = chatRoom.getHistory(20);
				if (history != null && history.length > 0) {
					for (int i = history.length - 1; i >= 0; i--) {
						LinphoneChatMessage msg = history[i];
						if (msg.getText() != null && msg.getText().length() > 0 && msg.getFileTransferInformation() == null) {
							message = msg.getText();
							time = msg.getTime();
							break;
						}
					}
				}
			} else {
				List<ChatMessage> messages = LinphoneActivity.instance().getChatMessages(contact);
				if (messages != null && messages.size() > 0) {
					int iterator = messages.size() - 1;
					ChatMessage lastMessage = null;

					while (iterator >= 0) {
						lastMessage = messages.get(iterator);
						if (lastMessage.getMessage() == null) {
							iterator--;
						} else {
							iterator = -1;
						}
					}
					if (lastMessage != null){
						time = Long.valueOf(lastMessage.getTimestamp());
					}
					message = (lastMessage == null || lastMessage.getMessage() == null) ? "" : lastMessage.getMessage();
					if (message.contains("@@info@@ ")) {
						message = message.replace("@@info@@ ", "" );
					}
				}
			}
			TextView lastMessageView = (TextView) view.findViewById(R.id.image_item_chat_message_context);
			lastMessageView.setText(message);

			TextView sipUri = (TextView) view.findViewById(R.id.image_item_chat_user_name);
			sipUri.setSelected(true); // For animation

			ImageView userImage = (ImageView) view.findViewById(R.id.image_item_chat_user_photo);
			TextView messageTime = (TextView) view.findViewById(R.id.label_item_chat_get_message_time);

			TextView badgeCount = (TextView) view.findViewById(R.id.label_item_chat_badge);

			if (time != null) {
				displayMessageTime(messageTime, time);
			}

			if (getResources().getBoolean(R.bool.only_display_username_if_unknown)) {
				sipUri.setText(lContact == null ? address.getUserName() : lContact.getName());
			} else {
				sipUri.setText(lContact == null ? address.asStringUriOnly() : lContact.getName());
			}

			if (lContact != null) {
				displayContactPicture(userImage, lContact);
			}

			if (isDraft) {
				view.findViewById(R.id.draft).setVisibility(View.VISIBLE);
			}

			if (unreadMessagesCount > 0) {
				lastMessageView.setTextColor(getResources().getColor(R.color.main_app_color));
				badgeCount.setVisibility(View.VISIBLE);
				badgeCount.setText(String.valueOf(unreadMessagesCount));
			} else {
				lastMessageView.setTextColor(Color.WHITE);
				badgeCount.setVisibility(View.GONE);
			}

			if(LinphonePreferences.instance().areAnimationsEnabled()) {
				Animation animation = AnimationUtils.loadAnimation(LinphoneActivity.ctx, (position > lastPosition) ? R.anim.slide_in_right_to_left : R.anim.slide_in_left_to_right);
				view.startAnimation(animation);
			}

			lastPosition = position;
			return view;
		}
	}

	private void displayMessageTime(TextView view, long time) {

		Calendar calendar = Calendar.getInstance();
		int currentDay = calendar.get(Calendar.DAY_OF_YEAR);
		long currentDayInMillis = calendar.getTimeInMillis();

		calendar.setTimeInMillis(time);

		int lastDay = calendar.get(Calendar.DAY_OF_YEAR);

		long sevenDay = 604800000L; //7*24*60*60*1000

		if (currentDay != lastDay){
			if (currentDayInMillis - time > sevenDay) {
				view.setText(new SimpleDateFormat("MM/dd").format(time));
			} else {
				view.setText(new SimpleDateFormat("EEE").format(time));
			}
		} else {
			String startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time));
			StringTokenizer tk = new StringTokenizer(startTime);
			String date = tk.nextToken();
			String time1 = tk.nextToken();

			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
			SimpleDateFormat sdfs = new SimpleDateFormat("hh:mm a");
			Date dt;
			try {
				dt = sdf.parse(time1);

				view.setText(sdfs.format(dt));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	private void displayContactPicture(ImageView imageView, Contact contact) {

		String rawContactId = null;
		try {
			rawContactId = ContactsManager.getInstance().findRawContactID(LinphoneActivity.instance().getContentResolver(), String.valueOf(contact.getID()));
		} catch(Throwable e){
			e.printStackTrace();
		}

		if (contact.getPhoto() != null) {
			imageView.setImageBitmap(contact.getPhoto());
		} else if (contact.getPhotoUri() != null) {
			imageView.setImageURI(contact.getPhotoUri());
		} else if(ContactsManager.picture_exists_in_storage_for_contact(rawContactId)) {
			imageView.setImageBitmap(ContactsManager.get_bitmap_by_contact_resource_id(rawContactId));
		}
	}

	private void searchContact(View view) {
		fastNewChat = (EditText) view.findViewById(R.id.newFastChat);
		findedAddresses = new ArrayList<String>();
		fastNewChat.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				searchUser(s.toString());
			}
			LinphoneAddress addressSearch;
			private void searchUser(final String keyword) {
				refresh();
				if (!TextUtils.isEmpty(keyword)) {
					for (int i = 0; i < mConversations.size(); i++) {
						String user = mConversations.get(i);


						try {
							addressSearch = LinphoneCoreFactory.instance().createLinphoneAddress(user);
						} catch (LinphoneCoreException e) {
							Log.e("Chat view cannot parse address",e);
						}
						Contact lContact = ContactsManager.getInstance().findContactWithAddress(getActivity().getContentResolver(), addressSearch);
						String searchBy = "";
						String searchedUser = user.substring(user.indexOf(":") + 1, user.indexOf("@"));
						if (lContact != null) {
							searchBy = lContact.getName();
						}
						if (searchBy.toLowerCase().contains(keyword.toLowerCase()) || searchedUser.toLowerCase().contains(keyword.toLowerCase())) {
							findedAddresses.add(user);
						}
					}
					mConversations.clear();
					mConversations.addAll(findedAddresses);
					chatListAdapter.notifyDataSetChanged();
				}
				if (keyword.length() == 0){
					sendMessageView.setVisibility(View.GONE);
				} else {
					sendMessageView.setVisibility(View.VISIBLE);
					showSendMessageView(keyword);
				}
				findedAddresses.clear();
			}
		});
	}

	private void showSendMessageView(final String keyword){

		final SpannableStringBuilder sb = new SpannableStringBuilder("Send message to   " + keyword);

		final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);
		sb.setSpan(bss, 18, 18 + keyword.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

		sendMessageView.setText(sb);

		sendMessageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				creatSipUri(keyword);
				fastNewChat.setText("");
			}
		});
	}
}



