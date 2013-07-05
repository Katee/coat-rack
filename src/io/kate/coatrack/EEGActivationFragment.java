package io.kate.coatrack;

import io.kate.coatrackcontrol.R;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;

public class EEGActivationFragment extends DialogFragment {
	
	SharedPreferences prefs;
	
	SeekBar meditation;
	SeekBar attention;
	
	CheckBox meditationEnabled;
	CheckBox attentionEnabled;
	
	public static final String LOG_TAG = DialogFragment.class.getName();
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.eeg_activity, null);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
		
		getDialog().setTitle("Headset Triggers");
		
		attention = (SeekBar)view.findViewById(R.id.attention_cutoff);
		attention.setProgress(prefs.getInt(CoatRackApplication.PREF_ATTENTION_CUTOFF, 0));
		
		meditation = (SeekBar)view.findViewById(R.id.meditation_cutoff);
		meditation.setProgress(prefs.getInt(CoatRackApplication.PREF_MEDITATION_CUTOFF, 0));
		
		attentionEnabled = (CheckBox)view.findViewById(R.id.attention_enabled);
		attentionEnabled.setChecked(prefs.getBoolean(CoatRackApplication.PREF_ATTENTION_ENABLED, false));
		
	  meditationEnabled = (CheckBox)view.findViewById(R.id.meditation_enabled);
	  meditationEnabled.setChecked(prefs.getBoolean(CoatRackApplication.PREF_MEDITATION_ENABLED, false));
		
		Button button = (Button) view.findViewById(R.id.close_button);
		
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
					Editor editor = prefs.edit();
			    
					editor.putInt(CoatRackApplication.PREF_ATTENTION_CUTOFF, attention.getProgress());
					editor.putInt(CoatRackApplication.PREF_MEDITATION_CUTOFF, meditation.getProgress());
					
					editor.putBoolean(CoatRackApplication.PREF_ATTENTION_ENABLED, attentionEnabled.isChecked());
					editor.putBoolean(CoatRackApplication.PREF_MEDITATION_ENABLED, meditationEnabled.isChecked());
			    
			    editor.commit();
		      getDialog().dismiss();
			}
		});
		
		return view;
	}

}