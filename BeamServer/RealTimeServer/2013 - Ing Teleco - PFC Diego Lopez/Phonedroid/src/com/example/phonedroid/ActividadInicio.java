package com.example.phonedroid;

import java.util.Locale;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ActividadInicio extends Activity {
	
	private final int duration = Toast.LENGTH_SHORT;
	
	
	private ProgressBar progressBar = null;
	private CheckBox mantener_registro_sip;
	private boolean flag_mantener_registro_sip=true;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 
    	//se comprueba la conexión a Internet
    	ConnectivityManager connMgr = (ConnectivityManager) 
        getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        
        if (networkInfo == null || !networkInfo.isConnected()) {
        	//no hay conexión: se muestra aviso correspondiente al usuario
        	Context context = getApplicationContext();
        	Toast toast_aviso_no_conexion = Toast.makeText(context, getString(R.string.aviso_no_internet), duration);
        	toast_aviso_no_conexion.show();
        }
        
        //comprobación del lenguaje elegido por el usuario y configuración 
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String language = preferencias.getString("language", "");
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,getBaseContext().getResources().getDisplayMetrics());
        
        //se inicia el servicio SIP para poder realizar y recibir llamadas vía SIP 
        Intent i=new Intent(this, ServicioSIP.class);
        startService(i);
        
        //carga de la interfaz gráfica
        setContentView(R.layout.actividad_inicio);
       
    }
	
	@Override 
	public void onStart() {
		super.onStart();
		
		if (progressBar != null) {
			progressBar.setVisibility(View.GONE);
			setContentView(R.layout.actividad_inicio);
		}
		
	}
	
	@Override
	public void onBackPressed(){
		super.onBackPressed();
		//se comprueba opción de mantener servicio SIP ejecutándose
		if ( ! flag_mantener_registro_sip ){
			Intent i=new Intent(this, ServicioSIP.class);
	        stopService(i);
		}
	}
	
	/**
	 * Lanza una interfaz de espera para actividades que requieren
	 * un tiempo medio-alto de carga
	 */
	public void cargandoActividad() {
    	
		
		setContentView(R.layout.cargando_actividad);
        
		// Se crea una barra animada que indica que la carga está en proceso  
        progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        progressBar.setIndeterminate(true);
        
        // Must add the progress bar to the root of the layout
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        root.addView(progressBar);
    }
  
    public void clickAccesoLlamadaRapida (View view) {
    	Intent intent = new Intent(this, ActividadLlamadaRapida.class);
    	startActivity(intent);
    	
    	cargandoActividad();
    }
    
    public void clickAccesoElegirCodec (View view) {
    	Intent intent = new Intent(this, ActividadLlamadaEligiendoCodec.class);
    	startActivity(intent);
        
        cargandoActividad();
    }
    
    
    public void clickAccesoLlamadaILBC (View view) {
    	Intent intent = new Intent(this, ActividadLlamadaILBC.class);
    	startActivity(intent);
    	
    	cargandoActividad();
    }
    
    public void clickAccesoLlamadasRedLocal (View view) {
    	Intent intent = new Intent(this, ActividadLlamadaLAN.class);
    	startActivity(intent);
    	
    	cargandoActividad();
    }
    
    
    public void clickAccesoLlamadasPerdidas (View view){
    	Intent intent = new Intent(this, ActividadLlamadasPerdidas.class);
    	startActivity(intent);
    	
    }
    
    public void clickAccesoSIPConf(View view){
    	updatePreferences();
    }
    
    public void updatePreferences() {
    	Intent settingsActivity = new Intent(getBaseContext(),
    			ActividadConfiguracionSIP.class);
        startActivityForResult(settingsActivity,2);
    }
    
    public void clickAdministrarContactos(View view){
    	Intent intent = new Intent(this, ActividadGestionContactos.class);
    	startActivity(intent);
    }
    
        
    public void onActivityResult(int requestCode, int resultCode, Intent data){
    	if (requestCode==1 && resultCode==RESULT_OK) {
    		// Al detectar cambio de idioma se refresca la interfaz (actividad)
    		SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    		String language = preferencias.getString("language", "");
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,getBaseContext().getResources().getDisplayMetrics());
    		
    		setContentView(R.layout.actividad_inicio);
    		
   	 }else 	if (requestCode==2 && resultCode==RESULT_OK) {
   		 //se reinicia el servicio SIP al volver de las preferencias
   		 //el reinicio se lanza desde la propia actividad de configuración SIP
    	 }
    }
    
    public void clickAccesoCambiarLenguaje (View view){
    	Intent intent = new Intent(this, ActividadCambiarIdioma.class);
    	startActivityForResult(intent,1);
    }
    
    /**
     * Almacena la opción que quiere el usuario al salir
     * de la aplicación (mantener servicio SIP en ejecución o no)
     * @param view
     */
    public void clickKeepApp (View view) {
    	this.runOnUiThread(new Runnable() {
            public void run() {
            	mantener_registro_sip=(CheckBox) findViewById(R.id.mantener_registro_sip);
    	    	if (mantener_registro_sip.isChecked()){
    	    		flag_mantener_registro_sip=true;
    	    	} else {
    	    		flag_mantener_registro_sip=false;
    	    	}
            }
    	});
    	
    }
    
    
    /*
    public void clickBotonPruebasVarias (View view) {
    	Intent intent = new Intent(this, MainActivity.class);
    	startActivity(intent);
    }
    */
    
    

}