package com.example.phonedroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * @author Diego
 * 
 * Gestiona la configuración para la autentificación SIP
 */

/**
 * Handles SIP authentication settings.
 */
public class ActividadConfiguracionSIP extends PreferenceActivity {
	
	Intent intent;
	
	OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener;
	
	SharedPreferences mSharedPreferencias;
	String nombre_usuario;
	String dominio;
	String nuevo_valor;
	
	EditTextPreference SIP_nombre_usuario;
	EditTextPreference SIP_dominio;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Note that none of the preferences are actually defines here
		// They´re all in the XML file res/xml/preferences.xml
		super.onCreate(savedInstanceState);
		intent=new Intent(this, ServicioSIP.class);
		addPreferencesFromResource(R.xml.preferences);
		
		
		mSharedPreferencias = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        nombre_usuario = mSharedPreferencias.getString("nombrePref", "");
        dominio = mSharedPreferencias.getString("dominioPref", "");
        
        if (nombre_usuario.length() != 0) {  
        	SIP_nombre_usuario = (EditTextPreference) findPreference("nombrePref");
        	SIP_nombre_usuario.setSummary(nombre_usuario);
        }
        if (dominio.length() != 0) {
        	SIP_dominio = (EditTextPreference) findPreference("dominioPref");
        	SIP_dominio.setSummary(dominio);
        }
		
        
        mOnSharedPreferenceChangeListener=new OnSharedPreferenceChangeListener() {
			
        	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String key) 
        	{
        		nuevo_valor = mSharedPreferencias.getString(key, "");
                
                if (key.equals("nombrePref"))  {
                	SIP_nombre_usuario = (EditTextPreference) findPreference(key);
                	SIP_nombre_usuario.setSummary(nuevo_valor);
                } else if  (key.equals("dominioPref"))  {
                	SIP_dominio = (EditTextPreference) findPreference(key);
                	SIP_dominio.setSummary(nuevo_valor);
                }
        		  	
        	}
			
        };
        
        
        getPreferenceScreen().getSharedPreferences()
        .registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
		
	
	}
	
	
	
	@Override
	public void onBackPressed() {
		//Note that none of the preferences are actually defines here
		// They´re all in the XML file res/xml/preferences.xml
		
		
		/** Restart SIPService (applying new configuration) */
		startService(intent);
		
		Intent intent_2 = new Intent(this, this.getCallingActivity().getClass());
	    setResult(RESULT_OK,intent_2);
	    finish();
	}
}