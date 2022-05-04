package com.example.phonedroid;

import java.text.ParseException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipSession;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.phonedroid.ServicioSIP.LocalBinder;

/**
 * @author Diego
 * 
 * Gestiona las llamadas salientes y entrantes, así como la interacción
 * con la interfaz de usuario
 */

public class ActividadLlamadaEligiendoCodec extends Activity {
	/**  CONSTANTES	 */
	private static final int DIALOG_ACTUALIZAR_CONFIGURACION = 1;
	
	
	/**  VARIABLES  */
	String direccion_SIP = null;
	String mensaje_error=null;
    int codigo_error;
    
    SipManager mSipManager = null;
    SipProfile mSipProfile = null;
    SipAudioCallModified llamada_saliente = null;
    SipAudioCall llamada_entrante = null;
    
    TextView usuario_local;
    TextView usuario_remoto;
    TextView error_es;
    TextView error;
    
    RelativeLayout cuadro_interaccion_en_espera;
    EditText edittext_cuentaSIP;
    Spinner spinner_opc_codec;
    Spinner spinner_modo_ilbc;
    TextView elegir_modo_ilbc;
    Button gestionar_contactos;
    Button editar_configuracion_sip;
    //public Button llSIP_CrearCuenta;
    
    RelativeLayout layout_cancelar_llamada;
    
    RelativeLayout cuadro_interaccion_llamada_recibida;
    
    RelativeLayout cuadro_interaccion_llamada_establecida;
    RelativeLayout opciones_durante_llamada;
    ToggleButton opcion_altavoz;
    ToggleButton opcion_poner_en_espera;
    ToggleButton opcion_silenciar;
    RelativeLayout layout_colgar;
    
    InputMethodManager imm;
    
    ServicioSIP mSIPService;
    boolean mBound = false;
    //SipRegistrationListener mSipRegistrationListener;
    
    /**  MÉTODOS  */
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.llamada_sip);
    
        
        usuario_local = (TextView) findViewById(R.id.usuario_local);
        usuario_remoto = (TextView) findViewById(R.id.usuario_remoto);
        error_es = (TextView) findViewById(R.id.error_es);
        error = (TextView) findViewById(R.id.error);
        cuadro_interaccion_llamada_recibida = (RelativeLayout) findViewById(R.id.cuadro_interaccion_llamada_recibida);
        editar_configuracion_sip = (Button) findViewById(R.id.editar_configuracion_sip);
        gestionar_contactos = (Button) findViewById(R.id.gestionar_contactos);
        cuadro_interaccion_llamada_establecida = (RelativeLayout) findViewById(R.id.cuadro_interaccion_llamada_establecida);
        opciones_durante_llamada= (RelativeLayout) findViewById(R.id.opciones_durante_llamada);
        cuadro_interaccion_en_espera = (RelativeLayout) findViewById(R.id.cuadro_interaccion_en_espera);
        layout_colgar = (RelativeLayout) findViewById(R.id.layout_colgar);
        layout_cancelar_llamada = (RelativeLayout) findViewById(R.id.layout_cancelar_llamada);
        opcion_altavoz = (ToggleButton) findViewById(R.id.opcion_altavoz);
        opcion_poner_en_espera = (ToggleButton) findViewById(R.id.opcion_poner_en_espera);
        opcion_silenciar = (ToggleButton) findViewById(R.id.opcion_silenciar);
        edittext_cuentaSIP=(EditText) findViewById(R.id.edittext_cuentaSIP);
        spinner_opc_codec= (Spinner) findViewById(R.id.spinner_opc_codec);
        spinner_modo_ilbc= (Spinner) findViewById(R.id.spinner_modo_ilbc);
        elegir_modo_ilbc=(TextView) findViewById(R.id.elegir_modo_ilbc);
        
        spinner_modo_ilbc.setVisibility(View.GONE);
        elegir_modo_ilbc.setVisibility(View.GONE);
        
        /** por defecto se escoge GSM como códec */
        spinner_opc_codec.setSelection(1); 
        
        /**
         * Se configura el intent filter. Este se usará para lanzar el BroadcastReceiver
         * cuando alguien contacte con la dirección SIP del usuario 
         */
        
        /*
        mSipRegistrationListener=new SipRegistrationListener() {
            public void onRegistering(String localProfileUri) {
                updateStatus(getString(R.string.SIPStatus_Registering));
            }

            public void onRegistrationDone(String localProfileUri, long expiryTime) {
            	updateStatus(getString(R.string.SIPStatus_Registered));
            }

            public void onRegistrationFailed(String localProfileUri, int errorCode,
                    String errorMessage) {
            	updateStatus(getString(R.string.SIPStatus_FailedRegistration));
            	//llamar a EstadoError()?? estaría bien
            }
        };
		*/


        /** Se mantiene la pantalla activa */
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
          
    }
    
    
    /** Define llamadas para vinculación al servicio pasadas a bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        //@Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mSIPService = binder.getService();
            mBound = true;
            if (mSipManager == null){
            	initializeManager();
            }
            
        }

        //@Override
        public void onServiceDisconnected(ComponentName arg0) {
        	mBound = false;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        // When we get back from the preference setting Activity, assume
        // settings have changed, and re-login with new auth info.
        //estadoEspera();
        
        /** 
         * Cuando se vuelve de alguna actividad "child" se vuelve a
         * realizar el registro en el servidor SIP
         */
        //NEW (service)
         Intent intent = new Intent(this, ServicioSIP.class);
        //bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        bindService(intent, mConnection, Context.BIND_ADJUST_WITH_ACTIVITY);
        //bindService(intent, mConnection, Context.BIND_IMPORTANT);
    }
    
    //NEW (service)
    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
		// AudioManager
		AudioManager audio_manager = (AudioManager) getSystemService(AUDIO_SERVICE);
		audio_manager.setSpeakerphoneOn(false);
        if (llamada_saliente != null) {
            llamada_saliente.close();
        }
        
        //if (llamada_entrante != null) {
        	//llamada_entrante.close();
        //}

      
    }

    public void initializeManager() {
        //if(mSipManager == null) {
        	//mSipManager = SipManager.newInstance(this);
        //}

        initializeLocalProfile();
    }

    /**
     * Registro en el servidor SIP para poder iniciar y recibir llamadas
     */
    public void initializeLocalProfile() {
    	

		SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String nombre_usuario = preferencias.getString("nombrePref", "");
        String dominio = preferencias.getString("dominioPref", "");
        String contraseña = preferencias.getString("contraseniaPref", "");
        
		if (nombre_usuario.length() == 0 || dominio.length() == 0 || contraseña.length() == 0) {
			showDialog(DIALOG_ACTUALIZAR_CONFIGURACION);
			return;
		}  
    	
		if (mBound){	
    		mSipManager=mSIPService.getSipManager();
    	}
		
        if (mBound && (mSipManager!=null)){
        
        	mSipProfile=mSIPService.getSipProfile();
        	
        	this.runOnUiThread(new Runnable() {
                public void run() {
                	usuario_local.setText(mSipProfile.getUriString());
                }
            });
        }
        	
        
            
        	

      
        }
        
 

    /**
     * Realiza una llamada
     */
    public void initiateCall() {
    	try {
            SipAudioCallModified.Listener listener = new SipAudioCallModified.Listener() {
                // Much of the client's interaction with the SIP Stack will
                // happen via listeners.  Even making an outgoing call, don't
                // forget to set up a listener to set things up once the call is established.
            	@Override
                public void onCalling(SipAudioCallModified call) {
            		estadoLlamando();
            	}
            	
            	@Override
                public void onCallEstablished(SipAudioCallModified call) {
            		
            		estadoLlamadaEstablecida();
            		call.startAudio();

                    if(call.isMuted()) {
                    call.toggleMute();
                    }
                    
                }

                @Override
                public void onCallEnded(SipAudioCallModified call) {

                	estadoEspera();

                	updateStatus(getString(R.string.Informacion_SIP_LlamadaSalienteTerminada));
                }
                              
                @Override
                public void onError (SipAudioCallModified call, int errorCode, String errorMessage){
                	mensaje_error=errorMessage;
                	codigo_error=errorCode;
                	estadoError();
                }
                @Override
                public void onCallBusy(SipAudioCallModified call){
                	estadoEspera();
                	//updateStatus("Llamada rechazada");
                	updateStatus(getString(R.string.Informacion_SIP_LlamadaRechazada));
                }
                
            };

                    

            
            String localProfileUri=mSipProfile.getUriString();
            String peerProfileUri=direccion_SIP;
            int timeout=30;
            
            SipProfile localProfile;
            SipProfile peerProfile;
            
            try {
            localProfile = new SipProfile.Builder(localProfileUri).build();
            peerProfile = new SipProfile.Builder(peerProfileUri).build();
            } catch (ParseException e) {
            	throw new SipException("build SipProfile", e);
            	}
            

            llamada_saliente = new SipAudioCallModified(this, localProfile);
            llamada_saliente.setListener(listener);
            SipSession s = mSipManager.createSipSession(localProfile, null);
            
            if (s == null) {
                throw new SipException(
                        "Failed to create SipSession; network available?");
            }
            
            
            

            int chosen_codec=spinner_opc_codec.getSelectedItemPosition();
            llamada_saliente.makeCall(peerProfile, s, timeout, chosen_codec);

            
           
    	}
        catch (Exception e) {
        	estadoEspera();
        	Log.i("LlamadasSIPActivity/InitiateCall", "Error al intentar crear el listener SIP", e);
        	

            if (mSipProfile != null) {
                try {
                	mSipManager.close(mSipProfile.getUriString());
                } catch (Exception ee) {
                	estadoEspera();
                	Log.i("LlamadasSIPActivity/InitiateCall","Error al intentar cerrar el gestor SIP", ee);

                    ee.printStackTrace();
                }
            }
            if (llamada_saliente != null) {
            	llamada_saliente.close();
            }
        }
    }

   
    /**
     * DEFINICIÓN DE ESTADOS
     * 
     * Se definen una serie de estados que determinarán cómo puede
     * interactuar el usuario con la interfaz gráfica en funcion de
     * la situación que se presente, así como la información que
     * se le ofrecerá
     */
    public void estadoEspera(){
    	this.runOnUiThread(new Runnable() {
            public void run() {
		    	cuadro_interaccion_en_espera.setVisibility(View.VISIBLE);
		    	gestionar_contactos.setVisibility(View.VISIBLE);
		    	editar_configuracion_sip.setVisibility(View.VISIBLE);
		    	
		    	error_es.setVisibility(View.INVISIBLE);
		    	error.setVisibility(View.INVISIBLE);
		    	cuadro_interaccion_llamada_recibida.setVisibility(View.INVISIBLE);
		    	cuadro_interaccion_llamada_establecida.setVisibility(View.INVISIBLE);
		    	
		    	//Reestablecer valor "[No disponible]" para el PeerUser
		    	usuario_remoto.setText(getString(R.string.valor_no_disponible));
		    	
		    	if (llamada_saliente != null || llamada_entrante != null) {
		            opcion_altavoz.setChecked(false);
		            opcion_silenciar.setChecked(false);
		            opcion_poner_en_espera.setChecked(false);
		    		
		            if (llamada_saliente != null){
		            	llamada_saliente.setSpeakerMode(false);	
		            }
		            if (llamada_entrante != null){
		            	llamada_entrante.setSpeakerMode(false);	
		            }
		    		
		        }
		    	
		    	
		    	
            }
    	});
    }
    
    public void estadoLlamando(){
    	this.runOnUiThread(new Runnable() {
            public void run() {
            	cuadro_interaccion_llamada_establecida.setVisibility(View.VISIBLE);
            	layout_cancelar_llamada.setVisibility(View.VISIBLE);
            	
            	gestionar_contactos.setVisibility(View.INVISIBLE);
		    	cuadro_interaccion_en_espera.setVisibility(View.INVISIBLE);
		    	error_es.setVisibility(View.INVISIBLE);
		    	error.setVisibility(View.INVISIBLE);
		    	cuadro_interaccion_llamada_recibida.setVisibility(View.INVISIBLE);
		    	opciones_durante_llamada.setVisibility(View.INVISIBLE);
		    	layout_colgar.setVisibility(View.INVISIBLE);
		    	editar_configuracion_sip.setVisibility(View.INVISIBLE);
		    	
		    	
		    	usuario_remoto.setText(llamada_saliente.getPeerProfile().getUriString());
		    	
		    	//updateStatus("Iniciando Llamada...");
		    	updateStatus(getString(R.string.Informacion_SIP_IniciandoLlamada));
		    	
		    	//ocultar teclado si está activado
		    	imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		    	imm.hideSoftInputFromWindow(edittext_cuentaSIP.getWindowToken(), 0);
            }
    	});
    }
    

    
    public void estadoLlamadaEstablecida(){
    	this.runOnUiThread(new Runnable() {
            public void run() {
		    	cuadro_interaccion_llamada_establecida.setVisibility(View.VISIBLE);
		    	opciones_durante_llamada.setVisibility(View.VISIBLE);
		    	layout_colgar.setVisibility(View.VISIBLE);
		    	
		    	
		    	gestionar_contactos.setVisibility(View.INVISIBLE);
		    	cuadro_interaccion_en_espera.setVisibility(View.INVISIBLE);
		    	error_es.setVisibility(View.INVISIBLE);
		    	error.setVisibility(View.INVISIBLE);
		    	cuadro_interaccion_llamada_recibida.setVisibility(View.INVISIBLE);
		    	editar_configuracion_sip.setVisibility(View.INVISIBLE);
		    	layout_cancelar_llamada.setVisibility(View.INVISIBLE);
		    	

		    	updateStatus(getString(R.string.Informacion_SIP_LlamadaEstablecida));
		    	
		    	
		    	//call.startAudio();
            }
    	});
    }
    public void estadoError(){
    	
    	this.runOnUiThread(new Runnable() {
            public void run() {
		        cuadro_interaccion_en_espera.setVisibility(View.VISIBLE);
		            
		        gestionar_contactos.setVisibility(View.VISIBLE);
		    	editar_configuracion_sip.setVisibility(View.VISIBLE);
		    	
		    	error_es.setVisibility(View.VISIBLE);
		    	error.setVisibility(View.VISIBLE);
		    	cuadro_interaccion_llamada_recibida.setVisibility(View.INVISIBLE);
		    	cuadro_interaccion_llamada_establecida.setVisibility(View.INVISIBLE);
            

		    	updateStatus(getString(R.string.Informacion_SIP_NotificacionError));
		    	
		    	error.setText(mensaje_error+"\t"+String.valueOf(codigo_error));
            }
    	});
    }
    

  
    
    
    /**
     * MÉTODOS QUE DEFINEN LAS OPCIONES DEL USUARIO
     */
    
    public void clickSIPLlamar(View view){
    	//EVITAR QUE SE PUEDA LLAMAR A UN STRING EN BLANCO
    	if (edittext_cuentaSIP.getText().length() <= 0) {
 			Context context = getApplicationContext();
        	Toast toast_aviso_campo_nulo = Toast.makeText(context, 
 					getString(R.string.aviso_campo_cuenta_sip_nulo), Toast.LENGTH_SHORT);
        	toast_aviso_campo_nulo.show();
 		} else {
 		
 		    direccion_SIP = edittext_cuentaSIP.getText().toString();
 	        initiateCall();
 	    
 		}
    	
    }
    
    
    /* OPCIONES DURANTE LA LLAMADA */ 
    public void clickSIPCancelarLlamada(View view){
    	//if(call != null) {
            //if (call.isInCall()) {
            	try {
            		llamada_saliente.endCall();
            		

                  } catch (SipException se) {
                  	Log.d("LlamadasSIPActivity/clickSIPCancelarLlamada",
                              "Error cancelando la llamada", se);
            }
            //}
    }
    public void clickSIPColgar(View view){
    	if(llamada_saliente != null && llamada_saliente.isInCall()) {
            
            	try {
            		llamada_saliente.endCall();

                  } catch (SipException se) {
                  	Log.d("LlamadasSIPActivity/clickSIPColgar",
                              "Error terminando la llamada", se);
                  }
    	} else if (llamada_entrante != null && llamada_entrante.isInCall()) {
            		try {
            			llamada_entrante.endCall();

            			} catch (SipException se) {
            				Log.d("LlamadasSIPActivity/clickSIPColgar",
            						"Error terminando la llamada", se);
            			}
            	} 	
        
    	}
    
    
    public void clickSIPAltavoz(View view){
    	if (llamada_saliente != null) {
    	
    	if (llamada_saliente.isInCall()){
    			//&& !llamada_saliente.isOnHold()) {
    		if (opcion_altavoz.isChecked()){
    			llamada_saliente.setSpeakerMode(true);
        	}else {llamada_saliente.setSpeakerMode(false);};
    	}
    	}
    	 if (llamada_entrante!=null) {
    		if (llamada_entrante.isInCall()){ 
    				//&& !llamada_entrante.isOnHold()) {
        		if (opcion_altavoz.isChecked()){
        			llamada_entrante.setSpeakerMode(true);
            	}else {llamada_entrante.setSpeakerMode(false);};
        	}
    	}
    		
    	
    }
    
    public void clickSIPEnEspera(View view){
    	if (llamada_saliente != null) {
    	if (llamada_saliente.isInCall()) {
    		if (opcion_poner_en_espera.isChecked()){
        		try{
        			llamada_saliente.holdCall(30);
        		}catch (SipException se) {Log.d("LlamadaSIPActivity/clickSIPEnEspera",
                            "Error poniendo En Espera", se);}
        		
        	}else {
        		try{
        			llamada_saliente.continueCall(30);	
        		}catch (SipException se) {Log.d("LlamadaSIPActivity/clickSIPEnEspera",
                        "Error continuando llamada", se);}
        		}
    	}
    	}
    	if (llamada_entrante != null) {
    		//if (llamada_entrante.isInCall()) {
    		if (llamada_entrante.isInCall() && opcion_poner_en_espera.isChecked()){
        		try{
        			llamada_entrante.holdCall(30);	
        		}catch (SipException se) {Log.d("LlamadaSIPActivity/clickSIPEnEspera",
                            "Error poniendo En Espera", se);}
        		
        	}else if (llamada_entrante.isOnHold() && !opcion_poner_en_espera.isChecked())  {
        		try{
        			llamada_entrante.continueCall(30);	
        		}catch (SipException se) {Log.d("LlamadaSIPActivity/clickSIPEnEspera",
                        "Error continuando llamada", se);}
        		
        		}
    		//}
    	}
    	
    }
    
    public void clickSIPSilenciar(View view){
    	if (llamada_saliente != null) {
    		
    	
    	if (llamada_saliente.isInCall()) {
    		if (opcion_silenciar.isChecked()){
        		if (!llamada_saliente.isMuted()){		
        		llamada_saliente.toggleMute();
        		}
        	}else {
        		if (llamada_saliente.isMuted()){
        		llamada_saliente.toggleMute();	
        		}
        	}
    	}
    	}
    	if (llamada_entrante!=null) {
    		if (llamada_entrante.isInCall()){
        		if (opcion_silenciar.isChecked()){
            		if (!llamada_entrante.isMuted()){		
            			llamada_entrante.toggleMute();
            		}
            	}else {
            		if (llamada_entrante.isMuted()){
            		llamada_entrante.toggleMute();	
            		}
            	}
        	}
    	}
    		
    		
    	
    }

    
    
    /* OTRAS OPCIONES */
    public void clickSIPSelecContacto(View view) {
    	Intent intent = new Intent(this, ActividadEleccionContacto.class);
    	startActivityForResult(intent,1);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data){
    	if (requestCode==1 && resultCode==RESULT_OK) {
    		String cuentasip=data.getStringExtra(ActividadEleccionContacto.EXTRA_MESSAGE_CUENTA_SIP);
    		
    		if (cuentasip != null){
    	    	edittext_cuentaSIP.setText(cuentasip);
    	    }
    		
    	} else if (requestCode==2 && resultCode==RESULT_OK) {
    		
    		mSipManager=null;
    	}
    
    }
    
    public void clickSIPGestContactos(View view) {
    	Intent intent = new Intent(this, ActividadGestionContactos.class);
    	startActivity(intent);
    	
    }
    
    
    public void clickSIPEditarConf(View view){
    	updatePreferences();
    }
  
    
    
    /**
     * Updates the status box at the top of the UI with a message of your choice.
     * @param status The String to display in the status box.
     */
    public void updateStatus(final String status) {
        // Be a good citizen.  Make sure UI changes fire on the UI thread.
        this.runOnUiThread(new Runnable() {
            public void run() {
                TextView labelView = (TextView) findViewById(R.id.notificacion_eventos);
                labelView.setText(status);
            }
        });
    }

    /**
     * Updates the status box with the SIP address of the current call.
     * @param call The current, active call.
     */
    public void updateStatus(SipAudioCall call) {
        String useName = call.getPeerProfile().getDisplayName();
        if(useName == null) {
          useName = call.getPeerProfile().getUserName();
        }
        updateStatus(useName);

    }
    
    public void updateStatus(SipAudioCallModified call) {
        String useName = call.getPeerProfile().getDisplayName();
        if(useName == null) {
          useName = call.getPeerProfile().getUserName();
        }
        updateStatus(useName);

    }
    
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        
        case DIALOG_ACTUALIZAR_CONFIGURACION:
        	return new AlertDialog.Builder(this)
        	    		.setMessage(getString(R.string.aviso_configurar_SIP))
                        .setPositiveButton(R.string.aceptar, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                updatePreferences();
                            }
                        })
                        .setNegativeButton(
                                R.string.cancelar, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                        })
                        .create();
        }
        return null;
    }
    
    public void updatePreferences() {
    	Intent settingsActivity = new Intent(getBaseContext(),
    			ActividadConfiguracionSIP.class);

    	startActivityForResult(settingsActivity,2);
    }
    
    
}