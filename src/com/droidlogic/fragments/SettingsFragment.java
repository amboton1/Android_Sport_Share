package com.droidlogic.fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.droidlogic.others.EndomondoFunctions;
import com.droidlogic.others.MyFunctions;
import com.droidlogic.others.RuntasticFunctions;
import com.droidlogic.pfdeporte.MainMenuActivity;
import com.droidlogic.pfdeporte.R;

public class SettingsFragment extends Fragment
{
	private MainMenuActivity activity;
	private EditText edEndoUser, edEndoPass, edRunUser, edRunPass;
	private SharedPreferences sp;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.settingsfragment, container, false);
		Button b=(Button)v.findViewById(R.id.btSaveSettings);
		edEndoUser=(EditText)v.findViewById(R.id.edEndomUsername);
		edEndoUser.setText(sp.getString("endoUser", "").trim());
		edEndoPass=(EditText)v.findViewById(R.id.edEndomPassword);
		edEndoPass.setText(sp.getString("endoPass", "").trim());
		edRunUser=(EditText)v.findViewById(R.id.edRuntUsername);
		edRunUser.setText(sp.getString("runUser", "").trim());
		edRunPass=(EditText)v.findViewById(R.id.edRuntPassword);
		edRunPass.setText(sp.getString("runPass", "").trim());
		
		b.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String endoUser=edEndoUser.getText().toString().trim(),
						endoPass=edEndoPass.getText().toString().trim(),
						runUser=edRunUser.getText().toString().trim(),
						runPass=edRunPass.getText().toString().trim();
				new checkLogin().execute(endoUser, endoPass, runUser, runPass);
			}
		});
		return v;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		activity = (MainMenuActivity) getActivity();
		sp=activity.getSharedPreferences(MyFunctions.PREFS_NAME, 0);
	}
	
	private class checkLogin extends AsyncTask<String, Void, Void>
	{
		private boolean isRuntasticOk=false, isEndomondoOK=false;
		
		@Override
		protected void onPostExecute(Void result)
		{
			super.onPostExecute(result);
			SharedPreferences.Editor editor=sp.edit();
			if (!isEndomondoOK && !isRuntasticOk)
			{
				Toast.makeText(SettingsFragment.this.getActivity(), getResources().getString(R.string.bothLoginError), Toast.LENGTH_SHORT).show();
				editor.putBoolean("loggedRuntastic", false);
				editor.putBoolean("loggedEndomondo", false);
			}
			else if (!isEndomondoOK)
			{
				Toast.makeText(SettingsFragment.this.getActivity(), getResources().getString(R.string.endomondoLoginError), Toast.LENGTH_SHORT).show();
				editor.putBoolean("loggedRuntastic", true);
				editor.putBoolean("loggedEndomondo", false);
			}
			else if (!isRuntasticOk)
			{
				Toast.makeText(SettingsFragment.this.getActivity(), getResources().getString(R.string.runtasticLoginError), Toast.LENGTH_SHORT).show();
				editor.putBoolean("loggedRuntastic", false);
				editor.putBoolean("loggedEndomondo", true);
			}
			else
			{
				//Both logins are OK
				Toast.makeText(SettingsFragment.this.getActivity(), getResources().getString(R.string.saved), Toast.LENGTH_SHORT).show();
				editor.putBoolean("loggedRuntastic", true);
				editor.putBoolean("loggedEndomondo", true);
			}
			editor.commit();
		}

		@Override
		protected Void doInBackground(String... params)
		{
			String endoUser=params[0], endoPass=params[1], runUser=params[2], runPass=params[3];
			SharedPreferences.Editor editor;
			
			if (!endoUser.equals("") && !endoPass.equals(""))
			{
				editor=sp.edit();
				editor.putString("endoUser", endoUser);
				editor.putString("endoPass", endoPass);
				editor.putString("userId", "");
				editor.commit();
				if (EndomondoFunctions.connectEndomondo(SettingsFragment.this.getActivity())!=null)
				{
					isEndomondoOK=true;
				}
			}
			
			if (!runUser.equals("") && !runPass.equals(""))
			{
				editor=sp.edit();
				editor.putString("runUser", runUser);
				editor.putString("runPass", runPass);
				editor.commit();
				if (RuntasticFunctions.connectRuntastic(SettingsFragment.this.getActivity())!=null)
				{
					isRuntasticOk=true;
				}
			}
			return null;
		}
		
	}
}
