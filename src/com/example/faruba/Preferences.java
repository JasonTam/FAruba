package com.example.faruba;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;

public class Preferences extends PreferenceActivity  {

   /* @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
    }*/

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        addPreferencesFromResource(R.xml.preferences);
 
    }
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_preferences, menu);
        return true;
    }

    
}
