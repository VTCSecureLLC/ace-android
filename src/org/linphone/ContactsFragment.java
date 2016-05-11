package org.linphone;
/*
ContactsFragment.java
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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AlphabetIndexer;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import org.linphone.compatibility.Compatibility;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.PresenceActivityType;
import org.linphone.mediastream.Log;

import java.util.List;

import io.App;

/**
 * @author Sylvain Berfini
 */
@SuppressLint("DefaultLocale")
public class ContactsFragment extends Fragment implements OnClickListener, OnItemClickListener {
	private LayoutInflater mInflater;
	private ListView contactsList;
	private TextView import_export_Contacts, allContacts, aceFavorites, newContact, noFavoriteContact, noContact;
	private boolean onlyDisplayAceFavorites;
	private int lastKnownPosition;
	private AlphabetIndexer indexer;
	private boolean editOnClick = false, editConsumed = false, onlyDisplayChatAddress = false;
	private String sipAddressToAdd;
	private ImageView clearSearchField;
	private EditText searchField;
	private Cursor searchCursor;

	private static ContactsFragment instance;
	
	static final boolean isInstanciated() {
		return instance != null;
	}

	public static final ContactsFragment instance() {
		return instance;
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
		mInflater = inflater;
        View view = inflater.inflate(R.layout.contacts_list, container, false);
        
        if (getArguments() != null) {
	        editOnClick = getArguments().getBoolean("EditOnClick");
	        sipAddressToAdd = getArguments().getString("SipAddress");
	        
	        onlyDisplayChatAddress = getArguments().getBoolean("ChatAddressOnly");
        }
        
        noFavoriteContact = (TextView) view.findViewById(R.id.noFavoriteContact);
        noContact = (TextView) view.findViewById(R.id.noContact);
        
        contactsList = (ListView) view.findViewById(R.id.contactsList);
        contactsList.setOnItemClickListener(this);

		if(LinphonePreferences.instance().areAnimationsEnabled()) {
			LayoutAnimationController lac = new LayoutAnimationController(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_right_to_left), 0.1f); //0.5f == time between appearance of listview items.
			contactsList.setLayoutAnimation(lac);
		}


        allContacts = (TextView) view.findViewById(R.id.allContacts);
        allContacts.setOnClickListener(this);

		import_export_Contacts = (TextView) view.findViewById(R.id.import_export_Contacts);
		import_export_Contacts.setOnClickListener(this);
        
        aceFavorites = (TextView) view.findViewById(R.id.aceFavorites);
        aceFavorites.setOnClickListener(this);
        
        newContact = (TextView) view.findViewById(R.id.newContact);
        newContact.setOnClickListener(this);
        newContact.setEnabled(LinphoneManager.getLc().getCallsNb() == 0);
        
        allContacts.setEnabled(onlyDisplayAceFavorites);
        aceFavorites.setEnabled(!allContacts.isEnabled());
		
        
		clearSearchField = (ImageView) view.findViewById(R.id.clearSearchField);
		clearSearchField.setOnClickListener(this);
		
		searchField = (EditText) view.findViewById(R.id.searchField);
		searchField.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				searchContacts(searchField.getText().toString());
			}
		});

		return view;
    }

	@Override
	public void onClick(View v) {
		int id = v.getId();
		
		if (id == R.id.allContacts) {
			onlyDisplayAceFavorites = false;
			if (searchField.getText().toString().length() > 0) {
				searchContacts();
			} else {
				changeContactsAdapter();
			}
		} 
		else if (id == R.id.aceFavorites) {
			onlyDisplayAceFavorites = true;
			if (searchField.getText().toString().length() > 0) {
				searchContacts();
			} else {
				changeContactsAdapter();
			}
		} 
		else if (id == R.id.newContact) {
			editConsumed = true;
			LinphoneActivity.instance().addContact(null, sipAddressToAdd);
		} 
		else if (id == R.id.clearSearchField) {
			searchField.setText("");
		}
		else if (id == R.id.import_export_Contacts) {
			Log.d("trying import export");
			Intent intent = new Intent(getActivity(), App.class);
			startActivity(intent);
		}

	}
	
	private void searchContacts() {
		searchContacts(searchField.getText().toString());
	}

	private void searchContacts(String search) {
		if (search == null || search.length() == 0) {
			changeContactsAdapter();
			return;
		}
		changeContactsToggle();
		
		if (searchCursor != null) {
			searchCursor.close();
		}
		if (onlyDisplayAceFavorites) {
			searchCursor = Compatibility.getFavoriteContactsCursor(getActivity().getContentResolver(), search, ContactsManager.getInstance().getContactsId());
			indexer = new AlphabetIndexer(searchCursor, Compatibility.getCursorDisplayNameColumnIndex(searchCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
			contactsList.setAdapter(new ContactsListAdapter(null, searchCursor));
		} else {
			searchCursor = Compatibility.getContactsCursor(getActivity().getContentResolver(), search, ContactsManager.getInstance().getContactsId());
			indexer = new AlphabetIndexer(searchCursor, Compatibility.getCursorDisplayNameColumnIndex(searchCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
			contactsList.setAdapter(new ContactsListAdapter(null, searchCursor));
		}
	}
	
	private void changeContactsAdapter() {
		changeContactsToggle();
		
		if (searchCursor != null) {
			searchCursor.close();
		}

		
		Cursor allContactsCursor = ContactsManager.getInstance().getAllContactsCursor();
		Cursor sipContactsCursor = ContactsManager.getInstance().getSIPContactsCursor();
		Cursor favoriteContactsCursor = ContactsManager.getInstance().getFavoriteContactsCursor();
		if(ContactsManager.getInstance().getAllContactsCursor()==null)
			return;

		noFavoriteContact.setVisibility(View.GONE);
		noContact.setVisibility(View.GONE);
		contactsList.setVisibility(View.VISIBLE);
		
		if (onlyDisplayAceFavorites) {
			if (favoriteContactsCursor != null && favoriteContactsCursor.getCount() == 0) {
				noFavoriteContact.setVisibility(View.VISIBLE);
				contactsList.setVisibility(View.GONE);
			} else {
				indexer = new AlphabetIndexer(favoriteContactsCursor, Compatibility.getCursorDisplayNameColumnIndex(favoriteContactsCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
				contactsList.setAdapter(new ContactsListAdapter(ContactsManager.getInstance().getFavoriteContacts(), favoriteContactsCursor));
			}
		} else {
			if (allContactsCursor != null && allContactsCursor.getCount() == 0) {
				noContact.setVisibility(View.VISIBLE);
				contactsList.setVisibility(View.GONE);
			} else {
				indexer = new AlphabetIndexer(allContactsCursor, Compatibility.getCursorDisplayNameColumnIndex(allContactsCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
				contactsList.setAdapter(new ContactsListAdapter(ContactsManager.getInstance().getAllContacts(), allContactsCursor));
			}
		}
		ContactsManager.getInstance().setLinphoneContactsPrefered(onlyDisplayAceFavorites);
	}
	
	private void changeContactsToggle() {
		if (onlyDisplayAceFavorites) {
			allContacts.setEnabled(true);
			aceFavorites.setEnabled(false);
		} else {
			allContacts.setEnabled(false);
			aceFavorites.setEnabled(true);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		Contact contact = (Contact) adapter.getItemAtPosition(position);
		if (editOnClick) {
			editConsumed = true;
			LinphoneActivity.instance().editContact(contact, sipAddressToAdd);
		} else {
			lastKnownPosition = contactsList.getFirstVisiblePosition();
			LinphoneActivity.instance().displayContact(contact, onlyDisplayChatAddress);
		}
	}
	
	@Override
	public void onResume() {
		instance = this;
		super.onResume();
		// carddav sync doesn't provide finished callback so workaround untill the callback will be providedgir
		ContactsManager.getInstance().prepareContactsInBackground();
		
		if (editConsumed) {
			editOnClick = false;
			sipAddressToAdd = null;
		}
		
		if (LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().selectMenu(FragmentsAvailable.CONTACTS);
			onlyDisplayAceFavorites = ContactsManager.getInstance().isLinphoneContactsPrefered();
			
			if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
				LinphoneActivity.instance().hideStatusBar();
			}
		}
		
		invalidate();
	}
	
	@Override
	public void onPause() {
		instance = null;
		if (searchCursor != null) {
			searchCursor.close();
		}
		super.onPause();
	}
	
	public void invalidate() {
		if (searchField != null && searchField.getText().toString().length() > 0) {
			searchContacts(searchField.getText().toString());
		} else {
			changeContactsAdapter();
		}
		contactsList.setSelectionFromTop(lastKnownPosition, 0);
	}
	
	class ContactsListAdapter extends BaseAdapter implements SectionIndexer {
		private int margin;
		private Bitmap bitmapUnknown;
		private List<Contact> contacts;
		private Cursor cursor;
		
		ContactsListAdapter(List<Contact> contactsList, Cursor c) {
			contacts = contactsList;
			cursor = c;

			margin = LinphoneUtils.pixelsToDpi(getActivity().getResources(), 10);
			bitmapUnknown = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.unknown_small);
		}
		
		public int getCount() {
			return cursor.getCount();
		}

		public Object getItem(int position) {
			if (contacts == null || position >= contacts.size()) {
				return Compatibility.getContact(getActivity().getContentResolver(), cursor, position);
			} else {
				return contacts.get(position);
			}
		}

		public long getItemId(int position) {
			return position;
		}

		private int lastPosition = -1;

		public View getView(int position, View convertView, ViewGroup parent) {
			View view = null;


			Contact contact = null;
			do {
				contact = (Contact) getItem(position);
			} while (contact == null);
			
			if (convertView != null) {
				view = convertView;
			} else {
				view = mInflater.inflate(R.layout.contact_cell, parent, false);
			}

			view.setBackgroundColor(Color.GRAY);
			TextView name = (TextView) view.findViewById(R.id.name);
			name.setText(contact.getName());
			name.setTextColor(Color.WHITE);
			TextView separator = (TextView) view.findViewById(R.id.separator);
			LinearLayout layout = (LinearLayout) view.findViewById(R.id.layout);
			if (getPositionForSection(getSectionForPosition(position)) != position) {
				separator.setVisibility(View.GONE);
				layout.setPadding(0, margin, 0, margin);
			} else {
				separator.setVisibility(View.VISIBLE);
				separator.setText(String.valueOf(contact.getName().charAt(0)));
				layout.setPadding(0, 0, 0, margin);
			}
			
			ImageView icon = (ImageView) view.findViewById(R.id.icon);
			String rawContactId = ContactsManager.getInstance().findRawContactID(getActivity().getContentResolver(), String.valueOf(contact.getID()));
			if (contact.getPhoto() != null) {
				icon.setImageBitmap(contact.getPhoto());
			} else if (contact.getPhotoUri() != null) {
				icon.setImageURI(contact.getPhotoUri());
			} else if(ContactsManager.picture_exists_in_storage_for_contact(rawContactId)) {
				icon.setImageBitmap(ContactsManager.get_bitmap_by_contact_resource_id(rawContactId));
			}else{
				icon.setImageBitmap(bitmapUnknown);
			}
			
			ImageView friendStatus = (ImageView) view.findViewById(R.id.friendStatus);
			LinphoneFriend[] friends = LinphoneManager.getLc().getFriendList();
			if (!ContactsManager.getInstance().isContactPresenceDisabled() && friends != null) {
				friendStatus.setVisibility(View.VISIBLE);
				PresenceActivityType presenceActivity = friends[0].getPresenceModel().getActivity().getType();
				if (presenceActivity == PresenceActivityType.Online) {
					friendStatus.setImageResource(R.drawable.led_connected);
				} else if (presenceActivity == PresenceActivityType.Busy) {
					friendStatus.setImageResource(R.drawable.led_error);
				} else if (presenceActivity == PresenceActivityType.Away) {
					friendStatus.setImageResource(R.drawable.led_inprogress);
				} else if (presenceActivity == PresenceActivityType.Offline) {
					friendStatus.setImageResource(R.drawable.led_disconnected);
				} else {
					friendStatus.setImageResource(R.drawable.call_quality_indicator_0);
				}
			}

			if(LinphonePreferences.instance().areAnimationsEnabled()) {
				Animation animation = AnimationUtils.loadAnimation(LinphoneActivity.ctx, (position > lastPosition) ? R.anim.slide_in_right_to_left : R.anim.slide_in_left_to_right);
				view.startAnimation(animation);
			}
			lastPosition = position;

			return view;
		}

		@Override
		public int getPositionForSection(int section) {
			return indexer.getPositionForSection(section);
		}

		@Override
		public int getSectionForPosition(int position) {
			return indexer.getSectionForPosition(position);
		}

		@Override
		public Object[] getSections() {
			return indexer.getSections();
		}
	}
}
