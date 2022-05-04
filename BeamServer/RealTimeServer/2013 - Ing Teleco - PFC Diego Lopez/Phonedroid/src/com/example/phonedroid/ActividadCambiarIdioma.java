package com.example.phonedroid;


import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;



public class ActividadCambiarIdioma extends Activity {
		
	private final String[] array_languages={"es","en","de"};
	
	
	private CheckBox checkbox_espaniol;
	private CheckBox checkbox_ingles;
	
	
	private String initial_language;
	private String selected_language;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		   	
		setContentView(R.layout.cambiar_idioma);
        
        checkbox_espaniol = (CheckBox) findViewById(R.id.checkbox_espaniol);
        checkbox_ingles = (CheckBox) findViewById(R.id.checkbox_ingles);
        
        
        //Comprobamos el idioma en el que está configurada actualmente la app
        String[] array={"español","English"};
        String local_language=Locale.getDefault().getDisplayLanguage();
        
        
        if (local_language.equals(array[0])){
        	checkbox_espaniol.setChecked(true);
        	initial_language=array_languages[0];
        } else if (local_language.equals(array[1])){
        	checkbox_ingles.setChecked(true);
        	initial_language=array_languages[1];
        } else {//por defecto está en inglés la aplicación
        	checkbox_ingles.setChecked(true);
        	initial_language=array_languages[1];
        }
        
        selected_language=initial_language;
    }
	
    public void clickSpanish (View view){
    	
    	checkbox_espaniol.setChecked(true);
    	checkbox_ingles.setChecked(false);
        
    	if (selected_language != array_languages[0]){
	        selected_language=array_languages[0];
	    	Locale locale = new Locale(array_languages[0]);
	        Locale.setDefault(locale);
	        Configuration config = new Configuration();
	        config.locale = locale;
	        getBaseContext().getResources().updateConfiguration(config,getBaseContext().getResources().getDisplayMetrics());
	        
    	}
    	
     }
    
    public void clickEnglish (View view){
    	checkbox_ingles.setChecked(true);    	
    	checkbox_espaniol.setChecked(false);
        
    	if (selected_language != array_languages[1]){
    		selected_language=array_languages[1];
    		Locale locale = new Locale(array_languages[1]);
	        Locale.setDefault(locale);
	        Configuration config = new Configuration();
	        config.locale = locale;
	        getBaseContext().getResources().updateConfiguration(config,getBaseContext().getResources().getDisplayMetrics());
		}
    	
    }
    
    
    
    @Override
	public void onBackPressed() {

    	if (!initial_language.equals(selected_language)){
        	//se actualizan preferencias
        	SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	        preferencias.edit().putString("language", selected_language).commit();
        	//se notifica a la actividad llamante que ha habido un cambio de idioma
        	Intent intent = new Intent(this, this.getCallingActivity().getClass());
	    	setResult(RESULT_OK,intent);
	    }
	    
        finish();
	}
 

}