package com.example.phonedroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.phonedroid.ServicioSIP.LocalBinder;

/**
 * @author Diego López Herrera
 * 
 * Gestiona las llamadas salientes, así como la interacción
 * con la interfaz de usuario
 */

public class ActividadLlamadaRapida extends Activity {
	/**
	 *  CONSTANTES 
	 */
	private static final int DIALOG_ACTUALIZAR_CONFIGURACION = 1;
	
	private static final int ID_NOTIFICACION_LLAMADA_ENTRANTE=2;
	private static final int ID_NOTIFICACION_LLAMADA_PERDIDA=3;
	
	/**
	 *  VARIABLES 
	 */
	String direccion_SIP = null;
	String mensaje_error=null;
    int codigo_error;
    
    
    
    SipManager mSipManager = null;
    SipProfile mSipProfile = null;
    SipAudioCall llamada = null;
    
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
    
    ProgressBar progressBar;
    
    //NEW (service)
    ServicioSIP mSIPService;
    boolean mBound = false;
    NotificationManager mNotificationManager;
    //SipRegistrationListener mSipRegistrationListener;
    boolean incomingCallIndicator;
    boolean indicador_llamada_perdida=false;
    String mCaller="null";
    String usuarioRemoto=null;
    
    
    /**
     *  MÉTODOS 
     */
    
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
        
        //Se ocultan vistas del layout que se utilizan en las otras actividades
        spinner_opc_codec.setVisibility(View.GONE);
        spinner_modo_ilbc.setVisibility(View.GONE);
        elegir_modo_ilbc.setVisibility(View.GONE);
        
                
        
        mNotificationManager =
			    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        
        incomingCallIndicator = false;
              
                
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//actividad activa siempre
        
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
    
        Intent intent = new Intent(this, ServicioSIP.class);
        
        //bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        bindService(intent, mConnection, Context.BIND_ADJUST_WITH_ACTIVITY);
        //bindService(intent, mConnection, Context.BIND_IMPORTANT);
    }
    
   
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
        if (llamada != null) {
            llamada.close();
        }
    }

    public void initializeManager() {
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
        	
        	if (incomingCallIndicator == true){
            
        		llamada=mSIPService.getSipAudioCall();
        		contestarCall();
          	} 
               	
        }
        	
        
            
        	

            
    }


    /**
     * Realiza una llamada
     */
    public void initiateCall() {
    	try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                // Much of the client's interaction with the SIP Stack will
                // happen via listeners.  Even making an outgoing call, don't
                // forget to set up a listener to set things up once the call is established.
            	@Override
                public void onCalling(SipAudioCall call) {
            		estadoLlamando();
            	}
            	
            	@Override
                public void onCallEstablished(SipAudioCall call) {
            		
            		estadoLlamadaEstablecida();
            		call.startAudio();

            		if(call.isMuted()) {
                    	call.toggleMute();
                    }
                }

                @Override
                public void onCallEnded(SipAudioCall call) {

                	estadoEspera();
                	updateStatus(getString(R.string.Informacion_SIP_LlamadaSalienteTerminada));
                }
                              
                @Override
                public void onError (SipAudioCall call, int errorCode, String errorMessage){
                	mensaje_error=errorMessage;
                	codigo_error=errorCode;
                	estadoError();
                }
                @Override
                public void onCallBusy(SipAudioCall call){
                	estadoEspera();
                	updateStatus(getString(R.string.Informacion_SIP_LlamadaRechazada));
                }
                
            };

            llamada = mSipManager.makeAudioCall(mSipProfile.getUriString(), direccion_SIP, listener, 30);
       
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
            if (llamada != null) {
            	llamada.close();
            }
        }
    }

    /**
     * Contestar una llamada
     */
    public void contestarCall() {
        
    	estadoLlamadaEntrante();
        
    	try {
            SipAudioCall.Listener listener2 = new SipAudioCall.Listener() {
                // Much of the client's interaction with the SIP Stack will
                // happen via listeners.  Even making an outgoing call, don't
                // forget to set up a listener to set things up once the call is established.
            	@Override
				public void onRinging (SipAudioCall call, SipProfile caller) {
            	indicador_llamada_perdida=true;
            	mCaller=caller.getUriString();
            	}
            	            	
            	@Override
                public void onCallEstablished(SipAudioCall call) {
            	    
            		indicador_llamada_perdida=false;
            		
            		estadoLlamadaEstablecida();
            		            		
            		call.startAudio();
                    
            		if(call.isMuted()) {
        				call.toggleMute();
        			}
            		
            		
                 
            	}
            	
            	@Override
                public void onCallEnded(SipAudioCall call) {
            		
            		mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE);
            		
            		
            		estadoEspera();
            		updateStatus(getString(R.string.Informacion_SIP_LlamadaEntranteTerminada));
            		
            		finish();
                }
            	
            	@Override
                public void onError (SipAudioCall call, int errorCode, String errorMessage){
            		
            		mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE);
            		
            		mensaje_error=errorMessage;
            		codigo_error=errorCode;
            		estadoError();
            		
            		
            		finish();
            		
                }
		
            };
                                    
            llamada.setListener(listener2,true);
            
        }
        catch (Exception e) {
        	estadoEspera();
        	Log.i("LlamadasSIPActivity/InitiateCall", "Error al intentar crear el listener SIP", e);
        	
            if (mSipProfile != null) {
               
                	estadoEspera();
              
            }
            if (llamada != null) {
            	llamada.close();
            }
        }
    }
    
private String determinarUsuarioRemoto(String cuentaSip) {
    	/*
    	     new Thread(new Runnable() {
        public void run() {
            final Bitmap bitmap = loadImageFromNetwork("http://example.com/image.png");
            mImageView.post(new Runnable() {
                public void run() {
                    mImageView.setImageBitmap(bitmap);
                }
            });
        	}
    	}).start();

    	 */
    	String cuentaSipExtraida=cuentaSip.substring(4); //formato "sip:cuenta@.."
    	String nickname=null;
    	
    	ContentResolver mContentResolver = this.getContentResolver();
		
    	Uri uri = ContactosSIP.ContactoSIP.CONTENT_URI;
		
		String[] projection = {
		   	     //ContactosSIP.ContactoSIP._ID,
		   	     ContactosSIP.ContactoSIP.COLUMN_NAME_NICKNAME,
		   	     //ContactosSIP.ContactoSIP.COLUMN_NAME_CUENTASIP
		   	     };
		
		Cursor mCursor = mContentResolver.query(uri,
				projection, 
				"cuentasip='"+cuentaSipExtraida+"'", 
				null, 
				null);
		
		if (mCursor.getCount() != 0){
	    	mCursor.moveToFirst();
	    	nickname=mCursor.getString(0);
	    } 
		
	    return nickname;
    }
    
    
    public void agregarLlamadaPerdida(String caller){
    	
    	//INSERCIÓN EN BASE DE DATOS
    	ContentResolver mContentResolver = this.getContentResolver();
		Uri uriLlamadasPerdidas = LlamadasPerdidas.LlamadaPerdida.CONTENT_URI;
    	
		// Se registra el nickname del usuario si la dirección está en la base de datos
		String cuentasip=null;
		if (usuarioRemoto != null){
    		cuentasip=usuarioRemoto;
    	} else { // si cuenta no registrada se registra cuenta SIP
    		cuentasip=caller;
    	}
		
		String typeofcall="Normal";
		
		Time today = new Time(Time.getCurrentTimezone());
		today.setToNow();
		
		String time=String.valueOf(today.monthDay)+"/"+
				String.valueOf(today.month+1)+"/"+
				String.valueOf(today.year)+
				"\t\t"+
				today.format("%k:%M:%S")
				;
		    
		ContentValues mContentValues = new ContentValues();
		mContentValues.put(LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_CUENTASIP, cuentasip);
		mContentValues.put(LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_TYPEOFCALL, typeofcall);
		mContentValues.put(LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_TIME, time);
	    
		mContentResolver.insert(uriLlamadasPerdidas, mContentValues);
		
		
		
		//NOTIFICAR EN BARRA DE ESTADO
		Intent mIntent=new Intent(this, ActividadLlamadasPerdidas.class);
		
		mIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
		Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent mPendingIntent=PendingIntent.getActivity(this, 0,
				mIntent, 0);
		
		Notification.Builder mNotificationBuilder = 
				new Notification.Builder(this)
		.setContentTitle("Phonedroid")
        .setContentText(getString(R.string.notificacion_llamadas_perdidas))
        .setSmallIcon(R.drawable.ic_notificacion_llamadas_perdidas)
        .setContentIntent(mPendingIntent)
        .setAutoCancel(true)
		;
		
		Notification mNotification = mNotificationBuilder.getNotification();

		mNotificationManager.notify(ID_NOTIFICACION_LLAMADA_PERDIDA, mNotification);
		
		
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
		    	
		    	if (llamada != null || llamada != null) {
		            opcion_altavoz.setChecked(false);
		            opcion_silenciar.setChecked(false);
		            opcion_poner_en_espera.setChecked(false);
		    		
		            if (llamada != null){
		            	llamada.setSpeakerMode(false);	
		            }
		            if (llamada != null){
		            	llamada.setSpeakerMode(false);	
		            }
		    	    
		        }
		    	
		    	if (indicador_llamada_perdida == true){
        			agregarLlamadaPerdida(mCaller);
        			
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
		    	
		    	
		    	usuarioRemoto =
		    			determinarUsuarioRemoto(llamada.getPeerProfile().getUriString());
		    	if (usuarioRemoto != null){
		    		usuario_remoto.setText(usuarioRemoto);
		    	} else {
		    		usuario_remoto.setText(llamada.getPeerProfile().getUriString());
		    	}
		    	
		    	updateStatus(getString(R.string.Informacion_SIP_IniciandoLlamada));
		    	
		    	//ocultar teclado si está activado
		    	imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		    	imm.hideSoftInputFromWindow(edittext_cuentaSIP.getWindowToken(), 0);
            }
    	});
    }
    
    public void estadoLlamadaEntrante(){
    	this.runOnUiThread(new Runnable() {
            public void run() {
		    	cuadro_interaccion_llamada_recibida.setVisibility(View.VISIBLE);
		    	
		    	cuadro_interaccion_en_espera.setVisibility(View.INVISIBLE);
		    	gestionar_contactos.setVisibility(View.INVISIBLE);
		    	editar_configuracion_sip.setVisibility(View.INVISIBLE);
		    	error_es.setVisibility(View.INVISIBLE);
		    	error.setVisibility(View.INVISIBLE);
		    	cuadro_interaccion_llamada_establecida.setVisibility(View.INVISIBLE);
		        
		    	usuarioRemoto =
		    			determinarUsuarioRemoto(llamada.getPeerProfile().getUriString());
		    	if (usuarioRemoto != null){
		    		usuario_remoto.setText(usuarioRemoto);
		    	} else {
		    		usuario_remoto.setText(llamada.getPeerProfile().getUriString());
		    	}
		    	
		    	updateStatus(getString(R.string.Informacion_SIP_LlamadaEntrante));
		    	
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
		    	
		    	if (indicador_llamada_perdida == true){
        			agregarLlamadaPerdida(mCaller);
        		
        		}
		    	
            }
    	});
    }
    
    
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
    
    
    public void clickSIPContestar(View view){
    	try {
    		llamada.answerCall(30);
    	} catch (Exception e) {e.printStackTrace();}
    	
    	mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE);
    }
    
    public void clickSIPRechazar(View view){
    	indicador_llamada_perdida=false;     
    	
    	try {
            	llamada.endCall();
             } catch (SipException se) {
             	Log.d("LlamadasSIPActivity/clickSIPRechazar",
                         "Error terminando la llamada", se);

             	
             }
    	   
    	     mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE);
    }
    
    
    /**
     * MÉTODOS QUE DEFINEN LAS OPCIONES DEL USUARIO
     */
    
    /* OPCIONES DURANTE LA LLAMADA */ 
    public void clickSIPCancelarLlamada(View view){
    	
    	try {
        	llamada.endCall();
        } catch (SipException se) {
        	Log.d("LlamadasSIPActivity/clickSIPCancelarLlamada",
        			"Error cancelando la llamada", se);
        }
    }
    
    public void clickSIPColgar(View view){
    	if(llamada != null) {
            if (llamada.isInCall()) {
            	try {
            		llamada.endCall();
                  } catch (SipException se) {
                  	Log.d("LlamadasSIPActivity/clickSIPColgar",
                              "Error terminando la llamada", se);
                  }
            }
    	} 
        
    }
    
    public void clickSIPAltavoz(View view){
    	if (llamada != null){ 
    		if (llamada.isInCall()) {    
    			
    			if (opcion_altavoz.isChecked()){
    				llamada.setSpeakerMode(true);
    			}else {llamada.setSpeakerMode(false);};
    		}
    	}
    	
    }
    
    public void clickSIPEnEspera(View view){
    	if (llamada != null) {
    		if (llamada.isInCall()) {
    			if (opcion_poner_en_espera.isChecked()){
    				try{
    					llamada.holdCall(30);
    				}catch (SipException se) {Log.d("LlamadaSIPActivity/clickSIPEnEspera",
    						"Error poniendo En Espera", se);}
    			}else if(llamada!=null){
    				try{
    					llamada.continueCall(30);	
    				}catch (SipException se) {Log.d("LlamadaSIPActivity/clickSIPEnEspera",
                        "Error continuando llamada", se);}
    			}
    		}
    	}
    }
    
    public void clickSIPSilenciar(View view){
    	if (llamada != null) {
    		if (llamada.isInCall() && !llamada.isOnHold()) {
    			if (opcion_silenciar.isChecked()){
    				if (!llamada.isMuted()){		
    					llamada.toggleMute();
    				}
    			}else {
    				if (llamada.isMuted()){
    					llamada.toggleMute();	
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