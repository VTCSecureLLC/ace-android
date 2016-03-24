package org.linphone;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;

import org.linphone.compatibility.Compatibility;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.Version;
import org.linphone.setup.CDNProviders;
import org.linphone.ui.AvatarWithShadow;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EditContactFragment extends Fragment {
	public static View view;
	private TextView ok;
	private EditText firstName, lastName;
	private LayoutInflater inflater;

	AvatarWithShadow contactPicture;

	private boolean isNewContact = true;
	private Contact contact;
	private int contactID;
	private List<NewOrUpdatedNumberOrAddress> numbersAndAddresses;
	private ArrayList<ContentProviderOperation> ops;
	private int firstSipAddressIndex = -1;
	private String newSipOrNumberToAdd;
	private ContactsManager contactsManager;
	private SharedPreferences sharedPreferences;
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.inflater = inflater;
		
		contact = null;
		if (getArguments() != null) {
			if (getArguments().getSerializable("Contact") != null) {
				contact = (Contact) getArguments().getSerializable("Contact");
				isNewContact = false;
				contactID = Integer.parseInt(contact.getID());
				contact.refresh(getActivity().getContentResolver());
				if (getArguments().getString("NewSipAdress") != null) {
					newSipOrNumberToAdd = getArguments().getString("NewSipAdress");
				}

			} else if (getArguments().getString("NewSipAdress") != null) {
				newSipOrNumberToAdd = getArguments().getString("NewSipAdress");
				isNewContact = true;
			}
		}

		contactsManager = ContactsManager.getInstance();
		
		view = inflater.inflate(R.layout.edit_contact, container, false);
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
		loadProviderDomainsFromCache();

		TextView cancel = (TextView) view.findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getFragmentManager().popBackStackImmediate();
			}
		});
		
		ok = (TextView) view.findViewById(R.id.ok);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isNewContact) {
					if (getResources().getBoolean(R.bool.forbid_empty_new_contact_in_editor)) {
						boolean areAllFielsEmpty = true;
						for (NewOrUpdatedNumberOrAddress nounoa : numbersAndAddresses) {
							if (nounoa.newNumberOrAddress != null && !nounoa.newNumberOrAddress.equals("")) {
								areAllFielsEmpty = false;
								break;
							}
						}
						if (areAllFielsEmpty) {
							String title= "Invalid Entry";
							String message = "Please enter at least 1 phone number or SIP address or press cancel";
							new AlertDialog.Builder(LinphoneActivity.instance())
									.setMessage(message)
									.setTitle(title)
									.setPositiveButton("OK", null)
									.show();
							//getFragmentManager().popBackStackImmediate();

							return;
						}

					}
					contactsManager.createNewContact(ops, firstName.getText().toString(), lastName.getText().toString());
				} else {
					contactsManager.updateExistingContact(ops, contact, firstName.getText().toString(), lastName.getText().toString());
				}
				
				for (NewOrUpdatedNumberOrAddress numberOrAddress : numbersAndAddresses) {
					numberOrAddress.save();
				}

		        try {
					getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
					addLinphoneFriendIfNeeded();
					removeLinphoneTagIfNeeded();
					contactsManager.prepareContactsInBackground();
		        } catch (Exception e) {
		        	e.printStackTrace();
		        }

				LinphoneActivity.contacts.performClick();

				if(LinphoneActivity.instance().getResources().getBoolean(R.bool.isTablet))
					ContactsFragment.instance().invalidate();
			}
		});
		
		lastName = (EditText) view.findViewById(R.id.contactLastName);
		// Hack to display keyboard when touching focused edittext on Nexus One
		if (Version.sdkStrictlyBelow(Version.API11_HONEYCOMB_30)) {
			lastName.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					InputMethodManager imm = (InputMethodManager) LinphoneActivity.instance().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
				}
			});
		}
		lastName.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (lastName.getText().length() > 0 || firstName.getText().length() > 0) {
					ok.setEnabled(true);
				} else {
					ok.setEnabled(false);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		
		firstName = (EditText) view.findViewById(R.id.contactFirstName);
		firstName.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (firstName.getText().length() > 0 || lastName.getText().length() > 0) {
					ok.setEnabled(true);
				} else {
					ok.setEnabled(false);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		if (!isNewContact) {
			String fn = findContactFirstName(String.valueOf(contactID));
			String ln = findContactLastName(String.valueOf(contactID));
			if (fn != null || ln != null) {
				firstName.setText(fn);
				lastName.setText(ln);
			} else {
				lastName.setText(contact.getName());
				firstName.setText("");
			}
		}
		
		contactPicture = (AvatarWithShadow) view.findViewById(R.id.contactPicture);
		String rawContactId = contactsManager.findRawContactID(getActivity().getContentResolver(),String.valueOf(contactID));
		//First check for contact image from google/device contacts
		if (contact != null && contact.getPhotoUri() != null) {
			InputStream input = Compatibility.getContactPictureInputStream(getActivity().getContentResolver(), contact.getID());
			contactPicture.setImageBitmap(BitmapFactory.decodeStream(input));
        //Then check the sdcard ace folder for a contact image
		} else if(ContactsManager.picture_exists_in_storage_for_contact(rawContactId)){
			contactPicture.setImageBitmap(ContactsManager.get_bitmap_by_contact_resource_id(rawContactId));
		} else {
        	contactPicture.setImageResource(R.drawable.unknown_small);
        }
		contactPicture.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openImageIntent();
			}
		});
		initNumbersFields((TableLayout) view.findViewById(R.id.controls), contact);
		
		ops = new ArrayList<ContentProviderOperation>();
		firstName.requestFocus();
		return view;
	}



	@Override
	public void onResume() {
		super.onResume();
		
		if (LinphoneActivity.isInstanciated()) {
			if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
				LinphoneActivity.instance().hideStatusBar();
			}
		}
	}
	public List<String> domains = new ArrayList<String>();



	protected void loadProviderDomainsFromCache(){
		if(sharedPreferences != null) {
			//Load cached providers and their domains
			String name = sharedPreferences.getString("provider0", "-1");
			domains = new ArrayList<String>();
			for (int i = 1; !name.equals("-1"); i++) {
				domains.add(name);
				name = sharedPreferences.getString("provider" + String.valueOf(i), "-1");
			}
		}
//		setProviderData(domains);
//		//Load default provider registration info if cached
//		populateRegistrationInfo("provider" + String.valueOf(0) + "domain");
	}

	protected void setProviderData(final Spinner spinner, List<String> data){
		spinner.setAdapter(new org.linphone.setup.SpinnerAdapter(LinphoneActivity.ctx, R.layout.spiner_ithem,
				new String[]{""}, new int[]{0}, false){
		});
	}
	private void initNumbersFields(final TableLayout controls, final Contact contact) {
		controls.removeAllViews();
		numbersAndAddresses = new ArrayList<NewOrUpdatedNumberOrAddress>();
		
		if (contact != null) {
			for (String numberOrAddress : contact.getNumbersOrAddresses()) {
				View view = displayNumberOrAddress(controls, numberOrAddress);
				if (view != null)
					controls.addView(view);
			}
		}
		if (newSipOrNumberToAdd != null) {
			View view = displayNumberOrAddress(controls, newSipOrNumberToAdd);
			if (view != null)
				controls.addView(view);
		}

		// Add one for phone numbers, one for SIP address
		if (!getResources().getBoolean(R.bool.hide_phone_numbers_in_editor)) {
			addEmptyRowToAllowNewNumberOrAddress(controls, false);
		}
		
		if (!getResources().getBoolean(R.bool.hide_sip_addresses_in_editor)) {
			firstSipAddressIndex = controls.getChildCount() - 2; // Update the value to always display phone numbers before SIP accounts
			addEmptyRowToAllowNewNumberOrAddress(controls, true);
		}
	}
	
	private View displayNumberOrAddress(final TableLayout controls, String numberOrAddress) {
		return displayNumberOrAddress(controls, numberOrAddress, false);
	}
	
	@SuppressLint("InflateParams")
	private View displayNumberOrAddress(final TableLayout controls, String numberOrAddress, boolean forceAddNumber) {
		boolean isSip = LinphoneUtils.isStrictSipAddress(numberOrAddress) || !LinphoneUtils.isNumberAddress(numberOrAddress);
		String domain = null;
		if (isSip) {
			domain = LinphoneUtils.getDomain(numberOrAddress);
			if (firstSipAddressIndex == -1) {
				firstSipAddressIndex = controls.getChildCount();
			}
			numberOrAddress = numberOrAddress.replace("sip:", "");
		}
		if ((getResources().getBoolean(R.bool.hide_phone_numbers_in_editor) && !isSip) || (getResources().getBoolean(R.bool.hide_sip_addresses_in_editor) && isSip)) {
			if (forceAddNumber)
				isSip = !isSip; // If number can't be displayed because we hide a sort of number, change that category
			else
				return null;
		}


		NewOrUpdatedNumberOrAddress tempNounoa;
		if (forceAddNumber) {
			tempNounoa = new NewOrUpdatedNumberOrAddress(isSip);
		} else {
			if(isNewContact || newSipOrNumberToAdd != null) {
				tempNounoa = new NewOrUpdatedNumberOrAddress(isSip, numberOrAddress);
			} else {
				tempNounoa = new NewOrUpdatedNumberOrAddress(numberOrAddress, isSip);
			}
		}
		final NewOrUpdatedNumberOrAddress nounoa = tempNounoa;
		numbersAndAddresses.add(nounoa);
		
		final View view = inflater.inflate(R.layout.contact_edit_row, null);
		
		final EditText noa = (EditText) view.findViewById(R.id.numoraddr);
		noa.setInputType(isSip ? InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS : InputType.TYPE_CLASS_PHONE);
		noa.setText(numberOrAddress);
		noa.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				nounoa.setNewNumberOrAddress(noa.getText().toString());
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		if (forceAddNumber) {
			nounoa.setNewNumberOrAddress(noa.getText().toString());
		}
		
		ImageView delete = (ImageView) view.findViewById(R.id.delete);
		delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				nounoa.delete();
				numbersAndAddresses.remove(nounoa);
				view.setVisibility(View.GONE);

			}
		});

		final Spinner sp_provider = (Spinner)view.findViewById(R.id.sp_contact_sip_provider);
		if(!isSip){
			sp_provider.setVisibility(View.INVISIBLE);
		}
		setProviderData(sp_provider, domains);
		sp_provider.setTag(-1); //Setting tag to -1 infers no item has been selected, this prevents domain being overwritten

		int pos = -1;
		if(domain!= null)
		{
			pos = CDNProviders.getInstance().getProviderPossition(domain);
		}

		if(pos!=-1)
			sp_provider.setSelection(pos);
		else
			sp_provider.setSelection(CDNProviders.getInstance().getSelectedProviderPosition());

		sp_provider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if(((Integer)sp_provider.getTag()) == -1){
					sp_provider.setTag(CDNProviders.getInstance().getSelectedProviderPosition());
					return;
				}
				update_field(noa, position, true);
				sp_provider.setTag(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		return view;
	}
	public boolean update_field(EditText noa, int position, boolean isSip){

		boolean characters_present=false;
		String oldAddr = noa.getText().toString();
		if (oldAddr.length() > 1) {
			characters_present=true;
			int domainStart = oldAddr.indexOf("@", 0);
			if (domainStart == -1) {
				domainStart = oldAddr.length();
			}
			String name = oldAddr.substring(0, domainStart);
			//String newDomain = sharedPreferences.getString("provider" + String.valueOf(position) + "domain", "");
			String newDomain = CDNProviders.getInstance().getProviders().get(position).getDomain();
			noa.setText(name + "@" + newDomain);
			if(!isSip){
				//it's a phone number, so we need to add phone parameters.
				noa.setText("+1"+name.replaceAll("-","")+"@"+newDomain+";user=phone");
			}
		}

		return characters_present;
	}

	@SuppressLint("InflateParams")
	private void addEmptyRowToAllowNewNumberOrAddress(final TableLayout controls, final boolean isSip) {
		final View view = inflater.inflate(R.layout.contact_add_row, null);
		
		final NewOrUpdatedNumberOrAddress nounoa = new NewOrUpdatedNumberOrAddress(isSip);
		
		final EditText noa = (EditText) view.findViewById(R.id.numoraddr);
		numbersAndAddresses.add(nounoa);
		noa.setHint(isSip ? getString(R.string.sip_address) : getString(R.string.phone_number));
		noa.setInputType(isSip ? InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS : InputType.TYPE_CLASS_PHONE);
		noa.requestFocus();
		noa.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				nounoa.setNewNumberOrAddress(noa.getText().toString());
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		
		final ImageView add = (ImageView) view.findViewById(R.id.add);
		add.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Add a line, and change add button for a delete button
				add.setImageResource(R.drawable.list_delete);
				ImageView delete = add;
				delete.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						nounoa.delete();
						numbersAndAddresses.remove(nounoa);
						view.setVisibility(View.GONE);
					}
				});
				if (!isSip) {
					firstSipAddressIndex++;
					addEmptyRowToAllowNewNumberOrAddress(controls, false);
				} else {
					addEmptyRowToAllowNewNumberOrAddress(controls, true);
				}
			}
		});

		if (isSip) {
			controls.addView(view, controls.getChildCount());
		} else {
			if (firstSipAddressIndex != -1) {
				controls.addView(view, firstSipAddressIndex);
			} else {
				controls.addView(view);
			}
		}
		final Spinner sp_provider = (Spinner)view.findViewById(R.id.sp_contact_sip_provider);
		if(!isSip){
			sp_provider.setVisibility(View.INVISIBLE);
		}
		setProviderData(sp_provider, domains);
		sp_provider.setSelection(CDNProviders.getInstance().getSelectedProviderPosition());
		sp_provider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				update_field(noa, position, isSip);
				return;

			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

	}
	
	private String findContactFirstName(String contactID) {
		Cursor c = getActivity().getContentResolver().query(ContactsContract.Data.CONTENT_URI,
		          new String[]{ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME},
		          ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
		          new String[]{contactID, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE}, null);
		if (c != null) {
			String result = null;
			if (c.moveToFirst()) {
				result = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
			}
			c.close();
			return result;
		}
		return null;
	}
	
	private String findContactLastName(String contactID) {
		Cursor c = getActivity().getContentResolver().query(ContactsContract.Data.CONTENT_URI,
		          new String[]{ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME},
		          ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
		          new String[]{contactID, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE}, null);
		if (c != null) {
			String result = null;
			if (c.moveToFirst()) {
				result = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
			}
			c.close();
			return result;
		}
		return null;
	}

	private void addLinphoneFriendIfNeeded(){
		for (NewOrUpdatedNumberOrAddress numberOrAddress : numbersAndAddresses) {
			if(numberOrAddress.newNumberOrAddress != null && numberOrAddress.isSipAddress) {
				if(isNewContact){
					Contact c = contactsManager.findContactWithDisplayName(ContactsManager.getInstance().getDisplayName(firstName.getText().toString(), lastName.getText().toString()));
					if (c != null && !contactsManager.isContactHasAddress(c, numberOrAddress.newNumberOrAddress)) {
						contactsManager.createNewFriend(c, numberOrAddress.newNumberOrAddress);
					}
				} else {
					if (!contactsManager.isContactHasAddress(contact, numberOrAddress.newNumberOrAddress)){
						if (numberOrAddress.oldNumberOrAddress == null) {
							contactsManager.createNewFriend(contact, numberOrAddress.newNumberOrAddress);
						} else {
							if (contact.hasFriends())
								contactsManager.updateFriend(numberOrAddress.oldNumberOrAddress, numberOrAddress.newNumberOrAddress);
						}
					}
				}
			}
		}
	}

	private void removeLinphoneTagIfNeeded(){
		if(!isNewContact) {
			boolean areAllSipFielsEmpty = true;
			for (NewOrUpdatedNumberOrAddress nounoa : numbersAndAddresses) {
				if (!nounoa.isSipAddress && (nounoa.oldNumberOrAddress != null && !nounoa.oldNumberOrAddress.equals("") || nounoa.newNumberOrAddress != null && !nounoa.newNumberOrAddress.equals(""))) {
					areAllSipFielsEmpty = false;
					break;
				}
			}
			if (areAllSipFielsEmpty && contactsManager.findRawLinphoneContactID(contact.getID()) != null) {
				contactsManager.removeLinphoneContactTag(contact);
			}
		}
	}
//Eliminated old custom spinner adapter, now using global spinneradapter class
	class NewOrUpdatedNumberOrAddress {
		private String oldNumberOrAddress;
		private String newNumberOrAddress;
		private boolean isSipAddress;
		
		public NewOrUpdatedNumberOrAddress() {
			oldNumberOrAddress = null;
			newNumberOrAddress = null;
			isSipAddress = false;
		}
		
		public NewOrUpdatedNumberOrAddress(boolean isSip) {
			oldNumberOrAddress = null;
			newNumberOrAddress = null;
			isSipAddress = isSip;
		}
		
		public NewOrUpdatedNumberOrAddress(String old, boolean isSip) {
			oldNumberOrAddress = old;
			newNumberOrAddress = null;
			isSipAddress = isSip;
		}
		
		public NewOrUpdatedNumberOrAddress(boolean isSip, String newSip) {
			oldNumberOrAddress = null;
			newNumberOrAddress = newSip;
			isSipAddress = isSip;
		}
		
		public void setNewNumberOrAddress(String newN) {
			newNumberOrAddress = newN;
		}
		
		public void save() {
			if (newNumberOrAddress == null || newNumberOrAddress.equals(oldNumberOrAddress))
				return;

			if (oldNumberOrAddress == null) {
				// New number to add
				addNewNumber();
			} else {
				// Old number to update
				updateNumber();
			}
		}
		
		public void delete() {
			if(contact != null) {
				if (isSipAddress) {
					if (contact.hasFriends()) {
						ContactsManager.getInstance().removeFriend(oldNumberOrAddress);
					} else {
						Compatibility.deleteSipAddressFromContact(ops, oldNumberOrAddress, String.valueOf(contactID));
					}
					if (getResources().getBoolean(R.bool.use_linphone_tag)) {
						Compatibility.deleteLinphoneContactTag(ops, oldNumberOrAddress, contactsManager.findRawLinphoneContactID(String.valueOf(contactID)));
					}
				} else {
					String select = ContactsContract.Data.CONTACT_ID + "=? AND "
							+ ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "' AND "
							+ ContactsContract.CommonDataKinds.Phone.NUMBER + "=?";
					String[] args = new String[]{String.valueOf(contactID), oldNumberOrAddress};

					ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
									.withSelection(select, args)
									.build()
					);
				}
			}
		}
		
		private void addNewNumber() {
			if (newNumberOrAddress == null || newNumberOrAddress.length() == 0) {
				return;
			}
			
			if (isNewContact) {
				if (isSipAddress) {
					cleanSipAddress();
					Compatibility.addSipAddressToContact(getActivity(), ops, newNumberOrAddress);
				} else {
					ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
				        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
				        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
				        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newNumberOrAddress)
				        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,  ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM)
						.withValue(ContactsContract.CommonDataKinds.Phone.LABEL, getString(R.string.addressbook_label))
				        .build()
				    );
				}
			} else {
				String rawContactId = contactsManager.findRawContactID(getActivity().getContentResolver(),String.valueOf(contactID));
				if (isSipAddress) {
					cleanSipAddress();
					Compatibility.addSipAddressToContact(getActivity(), ops, newNumberOrAddress, rawContactId);
					if (getResources().getBoolean(R.bool.use_linphone_tag)) {
						Compatibility.addLinphoneContactTag(getActivity(), ops, newNumberOrAddress, contactsManager.findRawLinphoneContactID(String.valueOf(contactID)));
					}
				} else {
					ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)         
					    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)       
				        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
				        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newNumberOrAddress)
				        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM)
				        .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, getString(R.string.addressbook_label))
				        .build()
				    );
				}
			}
		}
		
		private void updateNumber() {
			if (newNumberOrAddress == null || newNumberOrAddress.length() == 0) {
				return;
			}
			
			if (isSipAddress) {
				cleanSipAddress();
				Compatibility.updateSipAddressForContact(ops, oldNumberOrAddress, newNumberOrAddress, String.valueOf(contactID));
				if (getResources().getBoolean(R.bool.use_linphone_tag)) {
					Compatibility.updateLinphoneContactTag(getActivity(), ops, newNumberOrAddress, oldNumberOrAddress, contactsManager.findRawLinphoneContactID(String.valueOf(contactID)));
				}
			} else {
				String select = ContactsContract.Data.CONTACT_ID + "=? AND " 
						+ ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE +  "' AND " 
						+ ContactsContract.CommonDataKinds.Phone.NUMBER + "=?"; 
				String[] args = new String[] { String.valueOf(contactID), oldNumberOrAddress };   
				
	            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI) 
	        		.withSelection(select, args) 
	                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
	                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newNumberOrAddress)
	                .build()
	            );
			}
		}
	private void cleanSipAddress(){
		if (newNumberOrAddress.startsWith("sip:"))
			newNumberOrAddress = newNumberOrAddress.substring(4);
		if(!newNumberOrAddress.contains("@")) {
			//Use default proxy config domain if it exists
			LinphoneProxyConfig lpc = LinphoneManager.getLc().getDefaultProxyConfig();
			if(lpc != null){
				newNumberOrAddress = newNumberOrAddress + "@" + lpc.getDomain();
			} else {
				newNumberOrAddress = newNumberOrAddress + "@" + getResources().getString(R.string.default_domain);
			}
		}
	}
	}

	private Uri outputFileUri;
	int YOUR_SELECT_PICTURE_REQUEST_CODE=999;
	private void openImageIntent() {

// Determine Uri of camera image to save.

		String sdCard = Environment.getExternalStorageDirectory().toString()+ "/ACE/contact_images";
		String rawContactId = contactsManager.findRawContactID(getActivity().getContentResolver(),String.valueOf(contactID));
		String fname = rawContactId+ ".png";
		File image = new File(sdCard, fname);

		if (!image.exists ())
			image.getParentFile().mkdirs();
		else
			image.delete();

		outputFileUri = Uri.fromFile(image);

		Log.d("output file"+outputFileUri);
		// Camera.
		final List<Intent> cameraIntents = new ArrayList<Intent>();
		final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		final PackageManager packageManager = LinphoneActivity.instance().getPackageManager();
		final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
		for(ResolveInfo res : listCam) {
			final String packageName = res.activityInfo.packageName;
			final Intent intent = new Intent(captureIntent);
			intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
			intent.setPackage(packageName);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
			cameraIntents.add(intent);
		}

		// Filesystem.
		final Intent galleryIntent = new Intent();
		galleryIntent.setType("image/*");
		galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

		// Chooser of filesystem options.
		final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

		// Add the camera options.
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));

		startActivityForResult(chooserIntent, YOUR_SELECT_PICTURE_REQUEST_CODE);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == YOUR_SELECT_PICTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			String fileToUploadPath = null;
			String rawContactId = contactsManager.findRawContactID(getActivity().getContentResolver(),String.valueOf(contactID));
			contactPicture.setImageBitmap(ContactsManager.get_bitmap_by_contact_resource_id(rawContactId));
			Log.d(outputFileUri);
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

}