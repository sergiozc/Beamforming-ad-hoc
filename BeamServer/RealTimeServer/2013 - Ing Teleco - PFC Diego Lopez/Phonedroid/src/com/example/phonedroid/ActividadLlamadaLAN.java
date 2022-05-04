package com.example.phonedroid;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Enumeration;

import org.jlibrtp.Participant;
import org.jlibrtp.RTPSession;

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
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.rtp.RtpStream;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipSession;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.phonedroid.ServicioSIP.LocalBinder;
import com.example.phonedroid.SimpleSessionDescription.Media;



public class ActividadLlamadaLAN extends Activity {
		//CONSTANTES Y VARIABLES
	private static final int ID_NOTIFICACION_LLAMADA_ENTRANTE=2;
	private static final int ID_NOTIFICACION_LLAMADA_PERDIDA=3;
	
	final int FREQ=8000;
	
	
	//public final String LOG_TAG="Error al detectar IP";
	final int PROBLEMA_AL_INTERCAMBIAR=0;
	final int PROBLEMA_OBTENCION_DIRECCION_PUERTO=1;
	final int NO_COINCIDEN_SSIDS=2;
	final int INTERCAMBIO_SATISFACTORIO=3;
	final int INTERCAMBIO_SATISFACTORIO_MODO_COMPLETO=4;
	
	final long N_MS_ESPERA_CIERRE_SESION=5000;
	
	
	
	
	
	
	String mSsid=null;
	
	String DireccionLocal;
	int PuertoLocalAudioStream;
	int PuertoLocalDatagramSocket;
	TextView edittext_dir_IP_local_elegida;
	EditText edittext_direccion_IP_dest;
	EditText edittext_cuentaSIP;
	EditText direccion_destino;
	EditText puerto_destino;
	public String codec_seleccionado;
	Button boton_llamar_lan;
	
	Button boton_colgar_lan;
	ToggleButton opcion_altavoz;
	RelativeLayout opciones_llamada_LAN_establecida;
	RelativeLayout dialogo_intercambio_info;
	
	
	int mPort;
	int bufferSizeRecibido=0;
	
	private static final int DIALOG_ACTUALIZAR_CONFIGURACION = 1;
	
	TextView usuario_local;
    TextView usuario_remoto;
    TextView error_es;
    TextView error;
    
    SipManager mSipManager = null;
    SipProfile mSipProfile = null;
    SipSession mSipSession = null;
    String mPeerSd=null;
    String mensaje_error=null;
    int codigo_error;
    private long mSessionId = System.currentTimeMillis();
	Button boton_comenzar_intercambio;
	Button boton_seleccionar_contacto;
	RelativeLayout cuadro_interaccion_llamada_recibida;
    Button boton_cancelar_intercambio;
    InputMethodManager imm;
    String direccion_SIP = null;
    
    int indicador_resultado_intercambio=0;
    
    
    
	
	public Spinner spinner_opc_codec;
	public CheckBox checkbox_supresion_eco;
	public CheckBox checkbox_modo_completo;
	
	
	AudioStream mAudioStream;
	AudioGroup mAudioGroup;
	
	
	ServicioSIP mSIPService;
    boolean mBound = false;
    NotificationManager mNotificationManager;
    
    boolean incomingCallIndicator;

    private RTPSession mRTPSession_emisora=null;
	Participant mParticipant_Receiver=null;
	Participant mParticipant_Sender=null;
	DatagramSocket rtpSocket_rec = null;
	DatagramSocket rtcpSocket_rec = null;
	DatagramSocket rtpSocket = null;
	DatagramSocket rtcpSocket = null;
	DatagramSocket mDatagramSocket=null;
	private ReceptorUnicastRTP_ILBC mReceptorUnicastRTP_ILBC=null;
	byte[] trama_a_codificar;
	int tamTramaAntesCodificacion=320;
    int tamTramaCodificada=38;
	byte[] bufferAudioRecord;
	final Handler mHandler = new Handler();
	private Thread Rthread_send_mode_x = null;
    private Thread Rthread_send_mode_y = null;
	
	int mMinBufferSizeAudioRecord;
	int mMinBufferSizeAjustado;
    private boolean flag_send_packets=false;
    private AudioRecord mAudioRecord=null;
    
    int codec_selec_int;
    
    boolean indicador_llamada_perdida=false;
    String mCaller="null";
    String usuarioRemoto=null;
    
    final private Handler mHandler_terminarSesion=new Handler();
    
		//MÉTODOS
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.llamada_lan);
		
		/** Gestión SIP */
		
		usuario_local = (TextView) findViewById(R.id.usuario_local);
        usuario_remoto = (TextView) findViewById(R.id.usuario_remoto);
        error_es = (TextView) findViewById(R.id.error_es);
        error = (TextView) findViewById(R.id.error);
        boton_comenzar_intercambio = (Button) findViewById(R.id.boton_comenzar_intercambio);
        edittext_cuentaSIP=(EditText) findViewById(R.id.edittext_cuentaSIP);
        boton_seleccionar_contacto = (Button) findViewById(R.id.boton_seleccionar_contacto);
        cuadro_interaccion_llamada_recibida = (RelativeLayout) findViewById(R.id.cuadro_interaccion_llamada_recibida);
        boton_cancelar_intercambio = (Button) findViewById(R.id.boton_cancelar_intercambio);
        
       
        
        
        /** Obtención de SSID local */
        WifiManager wm= (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
     	WifiInfo wi = wm.getConnectionInfo();
     	mSsid= wi.getSSID();
        
     
      
        /** cálculo de mínimo buffer para inicializar AudioRecord en este dispositivo */
     	mMinBufferSizeAudioRecord = AudioRecord.getMinBufferSize(
        		FREQ,
	            //AudioFormat.CHANNEL_IN_STEREO,
        		AudioFormat.CHANNEL_IN_MONO,
	            AudioFormat.ENCODING_PCM_16BIT);
	    
	   
       
        
        
        mNotificationManager =
			    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        
        incomingCallIndicator = false;
		
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		/** FIN Gestión SIP */
		
		
		
		
        direccion_destino=(EditText) findViewById(R.id.direccion_destino);
        puerto_destino=(EditText) findViewById(R.id.puerto_destino);
		boton_llamar_lan = (Button) findViewById(R.id.boton_llamar_lan);
		
        boton_colgar_lan = (Button) findViewById(R.id.boton_colgar_lan);
        opcion_altavoz = (ToggleButton) findViewById(R.id.opcion_altavoz);
        opciones_llamada_LAN_establecida = (RelativeLayout) findViewById(R.id.opciones_llamada_LAN_establecida);
        dialogo_intercambio_info = (RelativeLayout) findViewById(R.id.dialogo_intercambio_info);
        
		
		DireccionLocal=getLocalIpAddress();
		
		
		
		
		spinner_opc_codec= (Spinner) findViewById(R.id.spinner_opc_codec);
		checkbox_supresion_eco= (CheckBox) findViewById(R.id.checkbox_supresion_eco);
		checkbox_modo_completo= (CheckBox) findViewById(R.id.checkbox_modo_completo);
		
		
		
		
		direccion_destino.setText(DireccionLocal);
		
		// AudioManager
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		
		/** INICIALIZACIÓN DE AUDIOSTREAM */
		inicializar_AudioStream_y_DatagramSocket();
		
	}
	
public void clickLANAltavoz(View view){
    	
	if (opcion_altavoz.isChecked()) {
		synchronized (this) {
			((AudioManager) this.getSystemService(Context.AUDIO_SERVICE)).setSpeakerphoneOn(true);
		}
	} else {
		synchronized (this) {
            ((AudioManager) this.getSystemService(Context.AUDIO_SERVICE)).setSpeakerphoneOn(false);
		}
	}
    
}

	
	
	public void inicializar_AudioStream_y_DatagramSocket(){
		/*
		 * Inicializacación de DatagramSocket
		 */
		try{
			//mDatagramSocket = new DatagramSocket(mPort);
			mDatagramSocket = new DatagramSocket();
		}catch(SocketException se) { }
		
		PuertoLocalDatagramSocket=mDatagramSocket.getLocalPort();
		
		
		/*
		 * Inicializacación de AudioStream
		 */
		try{
			InetAddress mInetAddress=InetAddress.getByName(DireccionLocal);
			try{
				mAudioStream= new AudioStream(mInetAddress);
			}catch(SocketException ex){Log.e("Error al crear AudioStream", ex.toString());}
		}catch(UnknownHostException ex){Log.e("Error al crear InetAddress", ex.toString());}
	
		mAudioStream.setMode(RtpStream.MODE_NORMAL);
		
		int codec_selec_int=spinner_opc_codec.getSelectedItemPosition();
		switch(codec_selec_int){
		case 0: mAudioStream.setCodec(AudioCodec.AMR);
		break;
		
		case 1: mAudioStream.setCodec(AudioCodec.GSM);
		break;
		
		case 2: mAudioStream.setCodec(AudioCodec.GSM_EFR);
		break;
		
		case 3: mAudioStream.setCodec(AudioCodec.PCMA);
		break;
		
		case 4: mAudioStream.setCodec(AudioCodec.PCMU);
		break;
		
		default: mAudioStream.setCodec(AudioCodec.AMR);
		};
	
		mAudioGroup=new AudioGroup();
		
		if (checkbox_supresion_eco.isChecked()){
			mAudioGroup.setMode(AudioGroup.MODE_ECHO_SUPPRESSION);
		} else mAudioGroup.setMode(AudioGroup.MODE_NORMAL);
		
		PuertoLocalAudioStream=mAudioStream.getLocalPort();
		
		TextView direccion_puerto_locales=(TextView) findViewById(R.id.direccion_puerto_locales);
		
		direccion_puerto_locales.setText(DireccionLocal+"   "+
							String.valueOf(PuertoLocalAudioStream)+"   "+
							PuertoLocalDatagramSocket+"(iLBC)");
		
	}
	
	private void inicializarDatagramSocket(){
		
		try{
			//mDatagramSocket = new DatagramSocket(mPort);
			mDatagramSocket = new DatagramSocket();
		}catch(SocketException se) {Log.d("no_crea_socket","no crea socket"+se.toString());}
		
		PuertoLocalDatagramSocket=mDatagramSocket.getLocalPort();
		
		TextView textview=(TextView) findViewById(R.id.direccion_puerto_locales);
		textview.setText(DireccionLocal+"   "+
							String.valueOf(PuertoLocalAudioStream)+"   "+
							mDatagramSocket.getLocalPort()+"(iLBC)");
		
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
            		mSipSession=mSIPService.getSipSession();
            		mPeerSd=mSIPService.getPeerSd();
            		atenderSolicitudIntercambio();
            	} 
        }
    	
        	

    }

 
	
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		// Liberar recursos AMR,GSM,GSM_EFR,PCMU,PCMA
		if (mAudioStream != null && mAudioStream.isBusy()){
			
				mAudioStream.join(null);
				mAudioStream.release();
				mAudioGroup.clear();
				mAudioStream=null;
		}
		
		// Liberar recursos iLBC
		liberarRecursosILBC();
		
		// Restablecer configuración de sonido normal 
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		audioManager.setMode(AudioManager.MODE_NORMAL);
		audioManager.setSpeakerphoneOn(false);
		
				
	}
	
	

	
	
	/**
	 * Si devuelve "null", no hay conexión a Internet disponible.
	 * Si devuelve un String no nulo, se trata de la dirección IP
	 * que posee el dispositivo (ya sea por datos móviles o a través de Wifi)  
	 */
	public String getLocalIpAddress() {
		String direccionIP="";
		try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                
	                if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
	                	direccionIP=direccionIP+inetAddress.getHostAddress().toString();
	    
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e("Error al detectar IP", ex.toString());
	    }
		return direccionIP;
	}
	
	public void clickCheckboxSupresionEco(View view){
		codec_selec_int=spinner_opc_codec.getSelectedItemPosition();
		if (codec_selec_int==5 || codec_selec_int==6){
			checkbox_supresion_eco.setChecked(false);
			Context context = getApplicationContext();
        	Toast toast_aviso_campo_nulo = Toast.makeText(context, 
 					getString(R.string.aviso_opcion_no_dispoible_ilbc), Toast.LENGTH_SHORT);
 			toast_aviso_campo_nulo.show();
			
		}
	}

	
	public void clickLANLlamar (View view) {
		llamarPorLAN(0);
	}
	
	
	public void llamarPorLAN(int bufferSize){
	
		/* Campo/s nulo/s */
		if (direccion_destino.getText().length() <= 0 ||
			puerto_destino.getText().length() <=0 ) 
		{
 			//Se avisa al usuario y no se hace nada
			Context context = getApplicationContext();
        	Toast toast_aviso_campo_nulo = Toast.makeText(context, 
 					getString(R.string.aviso_campo_nulo_llamadas_lan), Toast.LENGTH_SHORT);
 			toast_aviso_campo_nulo.show();
 		
		} 
		/* Campos no nulos */
		else {
		
 			codec_selec_int=spinner_opc_codec.getSelectedItemPosition();
 			
 			/** Caso iLBC */
 			if (codec_selec_int == 5 || codec_selec_int == 6){
 				
 				int port_d=Integer.parseInt(puerto_destino.getText().toString());
 				String addr_d=direccion_destino.getText().toString();
 				//try{
 				
 				Log.d("add and port","add and port "+addr_d+" "+port_d);
 				
 				int mode=20;
 				if (codec_selec_int==5){
 					mode=20;
 				}else if (codec_selec_int==6){
 					mode=30;
 				}
 				
 				
 				startRTPEnviarILBC(
 						addr_d, 
 						port_d, 
 						mode,
 						bufferSize);
 				
 			} 
 			/** Caso AMR,GSM,GSM_EFR,PCMU o PCMA */
 			else {
 				switch(codec_selec_int){
 				case 0: mAudioStream.setCodec(AudioCodec.AMR);
 				break;
 				
 				case 1: mAudioStream.setCodec(AudioCodec.GSM);
 				break;
 				
 				case 2: mAudioStream.setCodec(AudioCodec.GSM_EFR);
 				break;
 				
 				case 3: mAudioStream.setCodec(AudioCodec.PCMA);
 				break;
 				
 				case 4: mAudioStream.setCodec(AudioCodec.PCMU);
 				break;
 				
 				default: mAudioStream.setCodec(AudioCodec.AMR);
 				};
 				
 				if (checkbox_supresion_eco.isChecked()){
 					mAudioGroup.setMode(AudioGroup.MODE_ECHO_SUPPRESSION);
 					} else mAudioGroup.setMode(AudioGroup.MODE_NORMAL);
 				
 				int puertoDestino=Integer.parseInt(puerto_destino.getText().toString());
 				InetAddress direccionDestino;
 				
 				String addr_dest_string=direccion_destino.getText().toString();
 				
 				try{
 					direccionDestino=InetAddress.getByName(addr_dest_string);
 					mAudioStream.associate(direccionDestino, puertoDestino);
 				}catch(UnknownHostException ex){Log.e("Error al crear InetAddress", ex.toString());}
 				
 				try{
 					mAudioStream.join(mAudioGroup);
 				}catch(IllegalStateException ex){Log.e("Error al unir", ex.toString());}
 			
 			}
 			
 		//Se modifica la interfaz para que adquiera el aspecto para llamada establecida por LAN
		estadoLlamandoPorLAN();
 		
 		}//fin else (campos no nulos)
	}
	
	public void clickLANTerminar (View view) {
		/** Caso iLBC */
		if (codec_selec_int==5 || codec_selec_int==6) {
			liberarRecursosILBC();
			inicializarDatagramSocket();
		} 
		/** Caso AMR,GSM,GSM_EFR,PCMU o PCMA */
		else {
				
			if (mAudioStream != null && mAudioStream.isBusy()){
				mAudioStream.join(null);
			}
		}
		
		estadoLlamadaPorLANFinalizada();
		
	}
	
	
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
    		
    	}
    }
	
	
	
			
		
	public void clickTerminarConexion (View view) {
		
		if (mAudioStream != null && mAudioStream.isBusy()){
			
			mAudioStream.join(null);
			mAudioStream.release();
			mAudioGroup.clear();
			mAudioStream=null;
		}
		
	}
		
		
		/**
	     * Realiza una llamada
	     */
	
	public void iniciarSolicitudIntercambio() {
	    	
		try {
	            
	        /**
             * Definición del SipSession.Listener
             */
            SipSession.Listener sipSessionListener = new SipSession.Listener() {
                // Much of the client's interaction with the SIP Stack will
                // happen via listeners.  Even making an outgoing call, don't
                // forget to set up a listener to set things up once the call is established.
            	@Override
                public void onCalling(SipSession session) {
            		estadoLlamando();
            	}
            	
            	@Override
                public void onCallEstablished(SipSession session, String sessionDescription) {
            		
            		final String sD=sessionDescription;
            		estadoLlamadaEstablecida(sD);
            		
            		mHandler_terminarSesion.postDelayed(mTerminarSesion, N_MS_ESPERA_CIERRE_SESION);
                
            	}

                @Override
                public void onCallEnded(SipSession session) {

                	
                	switch(indicador_resultado_intercambio){
                	case PROBLEMA_AL_INTERCAMBIAR: 
                		updateStatus(getString(R.string.Informacion_SIP_PeticionSalienteTerminada_ProblemaIntercambio));
                		estadoEspera();
                	break;
	             	case PROBLEMA_OBTENCION_DIRECCION_PUERTO: 
	             		updateStatus(getString(R.string.Informacion_SIP_PeticionSalienteTerminada_ProblemaDirPuerto));
	             		estadoEspera();
	            	break;
	             	case NO_COINCIDEN_SSIDS: 
	             		updateStatus(getString(R.string.Informacion_SIP_PeticionSalienteTerminada_SSID_Diferentes));
	             		estadoEspera();
	            	break;
	             	case INTERCAMBIO_SATISFACTORIO:
	             			updateStatus(getString(R.string.Informacion_SIP_PeticionSalienteTerminada_OK));
	             			estadoEspera();
	             	break;
	             	
	             	case INTERCAMBIO_SATISFACTORIO_MODO_COMPLETO: 
	             		llamarPorLAN(bufferSizeRecibido);
	            	break;
	     			
	             	default: 
	             		updateStatus(getString(R.string.Informacion_SIP_PeticionSalienteTerminada_Problema));
	     			};
                	
	     			//se reinicia el valor del indicador para las siguientes peticiones
	     			indicador_resultado_intercambio=PROBLEMA_AL_INTERCAMBIAR;
	                	
	                
                }
	                              
	                
                @Override
                public void onError (SipSession session, int errorCode, String errorMessage){
                	
                	mensaje_error=errorMessage;
                	codigo_error=errorCode;
                	
                	estadoError();
                	
                }
                @Override
                public void onCallBusy(SipSession session){
                	estadoEspera();
                	
                	updateStatus(getString(R.string.Informacion_SIP_PeticionRechazada));
                }
                
	            
            };
	            /**
	             * Fin de la definición de SipSession.Listener
	             */
	            
	       
	            
	            /**
	             * Se sustituye la llamada a sip_manager.makeAudioCall por procedimientos
	             * correspondientes 
	             */
	            
            if (!SipManager.isVoipSupported(this)) {
	        	throw new SipException("VOIP API is not supported");
            }
            try {
            	SipProfile sip_profile_mio=new SipProfile.Builder(mSipProfile.getUriString()).build();
            	SipProfile sip_profile_destino=new SipProfile.Builder(direccion_SIP).build();
	            	        	
	            	        	
            	mSipSession = mSipManager.createSipSession(sip_profile_mio, null);
            	mSipSession.setListener(sipSessionListener);
	            	        	        
	            	        	
            	/**
            	 * Ahora vemos qué se hace dentro de call.makeCall y adaptamos
            	 * adecuadamente
            	 */
            	synchronized (this) {
	            	        			             
	            	        			                 
	            	        			            	  
            		/**
            		 * En siguiente paso se llama a una función interna que creamos para
            		 * crear la descripción de la sesión
            		 */            	        	        
            		/* Creación de la descrición de la sesión
            		 */
	            	        			            	 
            		mSipSession.makeCall(sip_profile_destino, createOffer().encode(), 30);        
	            	        	            	    
            	}
	            	        	
	            	        	
	            	        	
            } catch (ParseException e) {
            	throw new SipException("build SipProfile", e);
            }
	           
	    	
		}
		catch (Exception e) {
			estadoEspera();
	        Log.i("ActividadLlamadaLAN/iniciarSolicitudIntercambio", 
	        		"Error al intentar crear el listener SIP", e);
	        	
	
	        if (mSipProfile != null) {
	        	try {
	        		mSipManager.close(mSipProfile.getUriString());
	        	} catch (Exception ee) {
	        		estadoEspera();
	        		Log.i("ActividadLlamadaLAN/iniciarSolicitudIntercambio","Error al intentar cerrar el gestor SIP", ee);
	        		ee.printStackTrace();
	        	}
	        }
	            
	            
	        if (mSipSession != null) {
	        	mSipSession=null;
	        }
	            
		}
	}

	/**
	 * Crear una oferta (descripción de sesión)
	 */
	private SimpleSessionDescription createOffer() {
		SimpleSessionDescription offer =
				new SimpleSessionDescription(mSessionId, mSipSession.getLocalIp());
	    	         
	    	         
		Media media; 
	    	         
		codec_selec_int=spinner_opc_codec.getSelectedItemPosition();
	    	         
		if (codec_selec_int==5 || codec_selec_int==6){ //iLBC
			media = offer.newMedia(
					"audio", PuertoLocalDatagramSocket, 1, "RTP/AVP");
			String mode="20";
			if (codec_selec_int == 5){
				mode="20";
			} else {
				mode="30";
			}
	    	        	 
			media.setRtpPayload(97, "iLBC/8000", "mode="+mode);
	    	         
		} else { //AMR, GSM,...
			media = offer.newMedia(
					"audio", PuertoLocalAudioStream, 1, "RTP/AVP");
			int codec_selec_int=spinner_opc_codec.getSelectedItemPosition();
			switch(codec_selec_int){
			case 0: mAudioStream.setCodec(AudioCodec.AMR);
			break;
	    	     		
			case 1: mAudioStream.setCodec(AudioCodec.GSM);
			break;
	    	     		
			case 2: mAudioStream.setCodec(AudioCodec.GSM_EFR);
			break;
	    	     		
			case 3: mAudioStream.setCodec(AudioCodec.PCMA);
			break;
	    	     		
			case 4: mAudioStream.setCodec(AudioCodec.PCMU);
			break;
	    	     		
			default: mAudioStream.setCodec(AudioCodec.AMR);
			};
	    	        	
			AudioCodec codec = mAudioStream.getCodec();
			media.setRtpPayload(codec.type, codec.rtpmap, codec.fmtp);
			
		}
	    	         
	    	         
	    	         
		codec_selec_int=spinner_opc_codec.getSelectedItemPosition();
		String codec_selected;
		switch(codec_selec_int){
		case 0: codec_selected="AMR";
		break;
	    	 		
		case 1: codec_selected="GSM";
		break;
	    	 		
		case 2: codec_selected="GSM_EFR";
		break;
	    	 		
		case 3: codec_selected="PCMA";
		break;
	    	 		
		case 4: codec_selected="PCMU";
		break;
	    	 		
		case 5: codec_selected="ILBC_20";
		break;
	    	 		
		case 6: codec_selected="ILBC_30";
		break;
	    	 		 
		default: codec_selected="AMR";
		};
	    	         
		media.setAttribute("typeofcall", "lan");
		media.setAttribute("ssid", mSsid);
		media.setAttribute("localaddress", DireccionLocal);
	    	         
		if (codec_selec_int==5 || codec_selec_int==6){ //iLBC
			media.setAttribute("localport", String.valueOf(PuertoLocalDatagramSocket));
		} else { //AMR, GSM,...
			media.setAttribute("localport", String.valueOf(PuertoLocalAudioStream));
		}
	    	         
	    	         
		if (checkbox_modo_completo.isChecked()){ //si se ha elegido el modo completo
	    	        	 
			media.setAttribute("codec", codec_selected);
	    	        	
			int TAM_TRAMA_SIN_CODIFICAR=320;
                         
			if (codec_selec_int==5){
				TAM_TRAMA_SIN_CODIFICAR=320;
			} else if (codec_selec_int==6){
				TAM_TRAMA_SIN_CODIFICAR=480;
			}
                         
			mMinBufferSizeAjustado = 
					TAM_TRAMA_SIN_CODIFICAR * 
					((int) Math.ceil((double)mMinBufferSizeAudioRecord/TAM_TRAMA_SIN_CODIFICAR));
                      	 
			media.setAttribute("buffersize", String.valueOf(mMinBufferSizeAjustado));
                      	 
                      	 
		}
	    	         
	    	        
		Log.d("offer_lan","offer_lan\n"+offer.encode());
		return offer;
	}
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	/**
	 * Contestar una llamada
	 */
	public void atenderSolicitudIntercambio() {
	        
		estadoLlamadaEntrante();
	        
		try {
	    		
			/**
			 * Creación del SipSession.Listener
			 */
	            
			SipSession.Listener sipSessionListener = new SipSession.Listener() {
				// Much of the client's interaction with the SIP Stack will
				// happen via listeners.  Even making an outgoing call, don't
				// forget to set up a listener to set things up once the call is established.
				@Override
				public void onRinging (SipSession session, SipProfile caller, String sessionDescription) {
	            	
					}
	            	            	
				@Override
				public void onCallEstablished (SipSession session, String sessionDescription) {
	            	            		
					indicador_llamada_perdida=false;
					
					final String sD=sessionDescription;
					estadoLlamadaEstablecida(sD);
	            		
				}
	            	
				@Override
				public void onCallEnded (SipSession session) {
	
					mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE);
	                	
					switch(indicador_resultado_intercambio){
					case PROBLEMA_AL_INTERCAMBIAR: 
						updateStatus(getString(R.string.Informacion_SIP_PeticionSalienteTerminada_Problema));
						estadoEspera();
						break;
					case PROBLEMA_OBTENCION_DIRECCION_PUERTO: 
						updateStatus(getString(R.string.Informacion_SIP_PeticionSalienteTerminada_ProblemaDirPuerto));
						estadoEspera();
						break;
					case NO_COINCIDEN_SSIDS: 
						updateStatus(getString(R.string.Informacion_SIP_PeticionSalienteTerminada_SSID_Diferentes));
						estadoEspera();
						break;
					case INTERCAMBIO_SATISFACTORIO:
						updateStatus(getString(R.string.Informacion_SIP_PeticionSalienteTerminada_OK));
						estadoEspera();
		             	break;
		             	
					case INTERCAMBIO_SATISFACTORIO_MODO_COMPLETO: 
						llamarPorLAN(bufferSizeRecibido);
						break;
		     			
					default: 
						updateStatus(getString(R.string.Informacion_SIP_PeticionSalienteTerminada_Problema));
					};
					
					//se reinicia el valor del indicador para las siguientes peticiones
					indicador_resultado_intercambio=PROBLEMA_AL_INTERCAMBIAR;
	                	
				}
	            	
				@Override
				public void onError (SipSession session, int errorCode, String errorMessage){
					mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE);
	            		
					mensaje_error=errorMessage;
					codigo_error=errorCode;
	            		
					estadoError();
	           
				}
	            	
			
			};
	        
			//datos para notificar llamada perdida si necesario
			indicador_llamada_perdida=true;
			mCaller=mSipSession.getPeerProfile().getUriString();                     
	            
			mSipSession.setListener(sipSessionListener);// .setListener(listener2,true);
	            
		}
		catch (Exception e) {
			estadoEspera();
			Log.i("ActividadLlamadaLAN/atenderSolicitudIntercambio", "Error al intentar crear el listener SIP", e);
			if (mSipProfile != null) {
				try {
					mSipManager.close(mSipProfile.getUriString());
				} catch (Exception ee) {
					estadoEspera();
					Log.i("ActividadLlamadaLAN/atenderSolicitudIntercambio","Error al intentar cerrar el gestor SIP", ee);
	                    ee.printStackTrace();
				}
			}
			if (mSipSession != null) {
				mSipSession =null;
			}
		}
	}
	    

	    /**
	     * Crear la respuesta de descripción de sesión
	     */
	private SimpleSessionDescription createAnswer(String offerSd) {
	
		SimpleSessionDescription offer =
				new SimpleSessionDescription(offerSd);
		SimpleSessionDescription answer =
				new SimpleSessionDescription(mSessionId, mSipSession.getLocalIp());
	         
	         
		AudioCodec codec = null;
		boolean indicador_iLBC=false;
	         
		for (Media media : offer.getMedia()) {
			if ((media.getPort() > 0)
					&& "audio".equals(media.getType())
					&& "RTP/AVP".equals(media.getProtocol())) {
				
				int[] rtppayloadtypes=media.getRtpPayloadTypes();
				int rtppayloadtype_ilbc=97;
	            	 
				for (int i=0;i<rtppayloadtypes.length;i++){
					if (media.getRtpmap(rtppayloadtypes[i]) != null) {
						//comprobamos si se trata de iLBC
						if (media.getRtpmap(rtppayloadtypes[i]).equals("iLBC/8000")){
							indicador_iLBC=true;
							rtppayloadtype_ilbc=rtppayloadtypes[i];
							codec=null;
							break;
						} else {
							// devolverá el códec correspondiente, o null si no es un un AudioCodec soportado
							codec = AudioCodec.getCodec(
									rtppayloadtypes[i],
									media.getRtpmap(rtppayloadtypes[i]),
									media.getFmtp(rtppayloadtypes[i]));
						}
	            			 
					}
				}   
	            	 
				Media reply=null;
	            	 
				if ((codec!=null) || (indicador_iLBC)){
					if (codec != null) {
	                 
						reply = answer.newMedia(
								"audio", PuertoLocalAudioStream, 1, "RTP/AVP");
						reply.setRtpPayload(codec.type, codec.rtpmap, codec.fmtp);
						reply.setAttribute("localport", String.valueOf(PuertoLocalAudioStream));
	                     
					} else if (indicador_iLBC) {
	            		 	
						reply = answer.newMedia(
								"audio", PuertoLocalDatagramSocket, 1, "RTP/AVP");
						String mode=media.getFmtp(rtppayloadtype_ilbc);
						reply.setRtpPayload(rtppayloadtype_ilbc, "iLBC/8000", mode);
    	                     
						reply.setAttribute("localport", String.valueOf(PuertoLocalDatagramSocket));
     	       	    	 	
						int TAM_TRAMA_SIN_CODIFICAR=320;
						if (mode.equals("mode=20")){
     	       	    	 		TAM_TRAMA_SIN_CODIFICAR=320;
						} else if (mode.equals("mode=30")){
							TAM_TRAMA_SIN_CODIFICAR=480;
						}
						mMinBufferSizeAjustado = 
								TAM_TRAMA_SIN_CODIFICAR * 
								((int) Math.ceil((double)mMinBufferSizeAudioRecord/TAM_TRAMA_SIN_CODIFICAR));
						
						reply.setAttribute("buffersize", String.valueOf(mMinBufferSizeAjustado));
	                 
					}
	 	         	    
					reply.setAttribute("ssid", mSsid);
					reply.setAttribute("localaddress", DireccionLocal);
    	             
					if (media.getAttribute("codec") != null) {
						reply.setAttribute("codec", media.getAttribute("codec"));
					}
	         	    	 
				}
	            	     
				continue;
			} 
	             
			if ((codec==null) && (!indicador_iLBC)){    // Reject the media.
	        	 
				Media reply = answer.newMedia(
						media.getType(), 0, 1, media.getProtocol());
	             
				for (String format : media.getFormats()) {
					reply.setFormat(format, null);
				}
			}
		}
	         
		if ((codec==null) && (!indicador_iLBC)) {
			throw new IllegalStateException("Reject SDP: no suitable codecs");
		}
	         
		Log.d("answer_lan","answer_lan\n"+answer.encode());
	    return answer;
	    	     
	}

		
	private String determinarUsuarioRemoto(String cuentaSip) {
	    	
		String cuentaSipExtraida=cuentaSip.substring(4);
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
			
		String typeofcall="LAN";
			
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
	    
	    
		
	public void clickSIPStartInterchange(View view){
		//EVITAR QUE SE PUEDA LLAMAR A UN STRING EN BLANCO
		if (edittext_cuentaSIP.getText().length() <= 0) {
			Context context = getApplicationContext();
			Toast toast_aviso_campo_nulo = Toast.makeText(context, 
					getString(R.string.aviso_campo_cuenta_sip_nulo), Toast.LENGTH_SHORT);
			toast_aviso_campo_nulo.show();
		} else {
			direccion_SIP = edittext_cuentaSIP.getText().toString();
			iniciarSolicitudIntercambio();
		}
	    	
	}
	    
	public void clickSIPCancelarLlamada(View view){
	    	
		if (mSipSession != null) {
			if (mSipSession.getState() == SipSession.State.OUTGOING_CALL   ||
					mSipSession.getState() == SipSession.State.OUTGOING_CALL_RING_BACK){
				mSipSession.endCall();
			}
		}
    		
	}
	public void clickSIPContestar(View view){
	    	
		/** contestar a la llamada/sesión:    */
	    		     			
		mSipSession.answerCall(createAnswer(mPeerSd).encode(), 30);
	
		mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE);
		//mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_PERDIDA);	
	    	
	}
	    
	public void clickSIPRechazar(View view){
	    	
		if (mSipSession != null){
			if (mSipSession.getState() == SipSession.State.INCOMING_CALL){
				mSipSession.endCall();
			}
	    		
	    		
		}
	    	
		mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE);
		//mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_PERDIDA);
	    	
	    	
	}
	    
	public boolean extraerSsidDireccionPuerto(String sessionDescription){
	    	
		SimpleSessionDescription offer =
				new SimpleSessionDescription(sessionDescription);
		
		AudioCodec codec=null;
		String ssid=null;
		String DireccionDestino=null;
		String PuertoDestino=null;
		String CodecEscogido=null;
		
	    	
		for (Media media : offer.getMedia()) {
			if ((codec == null) && (media.getPort() > 0)
					&& "audio".equals(media.getType())
					&& "RTP/AVP".equals(media.getProtocol())) {
	                
				codec=AudioCodec.GSM;
	                
				ssid=media.getAttribute("ssid");
				DireccionDestino=media.getAttribute("localaddress");
				PuertoDestino=media.getAttribute("localport");
				CodecEscogido=media.getAttribute("codec");
				String buffSize=media.getAttribute("buffersize");
				if (buffSize != null){
					bufferSizeRecibido=Integer.parseInt(buffSize);
				} else { bufferSizeRecibido=0; }
			}
	            
		}
	        
	
		if (ssid==null){ //no obtuvimos ssid remota
			indicador_resultado_intercambio=
					PROBLEMA_AL_INTERCAMBIAR;
			return false;
		} else if (DireccionDestino==null || PuertoDestino==null){
			//no obtenemos dirección o puerto remoto
			indicador_resultado_intercambio=
					PROBLEMA_OBTENCION_DIRECCION_PUERTO;
			return false;
		} else if (!mSsid.equals(ssid)){//no coinciden SSID local y remoto
			indicador_resultado_intercambio=
					NO_COINCIDEN_SSIDS;
			return false;
		} else if (CodecEscogido == null) { 
			//coinciden SSID local y remoto y se han obtenido todos los datos
			//pero no es el modo_completo
			indicador_resultado_intercambio=
					INTERCAMBIO_SATISFACTORIO;
			direccion_destino.setText(DireccionDestino);
			puerto_destino.setText(PuertoDestino);
	    		
	    		return true;
	    	} else {
	    		indicador_resultado_intercambio=
	    				INTERCAMBIO_SATISFACTORIO_MODO_COMPLETO;
	    		direccion_destino.setText(DireccionDestino);
	    		puerto_destino.setText(PuertoDestino);
	    		
	    		if (CodecEscogido.equals("AMR")){
   	         		codec_selec_int=0;
   	         	}else if (CodecEscogido.equals("GSM")) {
   	         		codec_selec_int=1;
   	         	}else if (CodecEscogido.equals("GSM_EFR")) {
   	         		codec_selec_int=2;
   	         	}else if (CodecEscogido.equals("PCMA")) {
   	         		codec_selec_int=3;
   	         	}else if (CodecEscogido.equals("PCMU")) {
   	         		codec_selec_int=4;
   	         	}else if (CodecEscogido.equals("ILBC_20")) {
   	         		codec_selec_int=5;
   	         	}else if (CodecEscogido.equals("ILBC_30")) {
   	         		codec_selec_int=6;
   	         	}
   	         
   	         	this.runOnUiThread(new Runnable() {
 	            	public void run() {
 	            		spinner_opc_codec.setSelection(codec_selec_int);
 	            	}
   	         	});
	   	 				
	   	 		return true;
	    	}
	    	
	    	
	    	
	    
	    }
	    
	    private Runnable mTerminarSesion = new Runnable(){
			public void run() {
				if (mSipSession.getState() == SipSession.State.IN_CALL){
	    			mSipSession.endCall();
	    		}
			}
	    };
	    
	    
	    public void estadoEspera(){
	    	this.runOnUiThread(new Runnable() {
	            public void run() {
	     
	                boton_comenzar_intercambio.setVisibility(View.VISIBLE);
	                boton_seleccionar_contacto.setVisibility(View.VISIBLE);
	                edittext_cuentaSIP.setVisibility(View.VISIBLE);
	                boton_llamar_lan.setVisibility(View.VISIBLE);
	            	
	                error_es.setVisibility(View.INVISIBLE);
			    	error.setVisibility(View.INVISIBLE);
			    	cuadro_interaccion_llamada_recibida.setVisibility(View.INVISIBLE);
			    	boton_cancelar_intercambio.setVisibility(View.INVISIBLE);
			    	
			    	//Restablecer valor "[No disponible]" para el PeerUser
			    	usuario_remoto.setText(getString(R.string.valor_no_disponible));
			    	
	               
			    	if (indicador_llamada_perdida == true){
	        			agregarLlamadaPerdida(mCaller);
	        			finish();
	        		}
	
	            }
	    	});
	    }
	    public void estadoLlamandoPorLAN(){
	    	this.runOnUiThread(new Runnable() {
	            public void run() {
	            	boton_llamar_lan.setVisibility(View.INVISIBLE);
	            	dialogo_intercambio_info.setVisibility(View.INVISIBLE);
	            	
	            	opciones_llamada_LAN_establecida.setVisibility(View.VISIBLE);
	            	boton_colgar_lan.setVisibility(View.VISIBLE);
	    	    	 	
			    	//ocultar teclado si está activado
			    	imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			    	imm.hideSoftInputFromWindow(edittext_cuentaSIP.getWindowToken(), 0);
	            }
	    	});
	    }
	    
	    public void estadoLlamadaPorLANFinalizada(){
	    	this.runOnUiThread(new Runnable() {
	            public void run() {
	            	boton_llamar_lan.setVisibility(View.VISIBLE);
	            	dialogo_intercambio_info.setVisibility(View.VISIBLE);
	            	
	            	opciones_llamada_LAN_establecida.setVisibility(View.INVISIBLE);
	            	boton_colgar_lan.setVisibility(View.INVISIBLE);
	            	
	            	updateStatus("");
	            	estadoEspera();
	            	
			    	//ocultar teclado si está activado
			    	imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			    	imm.hideSoftInputFromWindow(edittext_cuentaSIP.getWindowToken(), 0);
	            }
	    	});
	    }
	    
	    
	    public void estadoLlamando(){
	    	this.runOnUiThread(new Runnable() {
	            public void run() {
	            	boton_cancelar_intercambio.setVisibility(View.VISIBLE);
	            	
	            	boton_comenzar_intercambio.setVisibility(View.INVISIBLE);
	                boton_seleccionar_contacto.setVisibility(View.INVISIBLE);
	                edittext_cuentaSIP.setVisibility(View.INVISIBLE);
	            	error_es.setVisibility(View.INVISIBLE);
			    	error.setVisibility(View.INVISIBLE);
			    	cuadro_interaccion_llamada_recibida.setVisibility(View.INVISIBLE);
			    	boton_llamar_lan.setVisibility(View.INVISIBLE);
		
			    	usuarioRemoto =
			    			determinarUsuarioRemoto(mSipSession.getPeerProfile().getUriString());
			    	if (usuarioRemoto != null){
			    		usuario_remoto.setText(usuarioRemoto);
			    	} else {
			    		usuario_remoto.setText(mSipSession.getPeerProfile().getUriString());
			    	}
			    	
			    	updateStatus(getString(R.string.Informacion_SIP_IniciandoPeticion));
			    	
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
	            	
	            	boton_comenzar_intercambio.setVisibility(View.INVISIBLE);
	                boton_seleccionar_contacto.setVisibility(View.INVISIBLE);
	                edittext_cuentaSIP.setVisibility(View.INVISIBLE);
	            	error_es.setVisibility(View.INVISIBLE);
			    	error.setVisibility(View.INVISIBLE);
			    	boton_cancelar_intercambio.setVisibility(View.INVISIBLE);
			    	boton_llamar_lan.setVisibility(View.INVISIBLE);
	            	
	            	
			    	usuarioRemoto =
			    			determinarUsuarioRemoto(mSipSession.getPeerProfile().getUriString());
			    	if (usuarioRemoto != null){
			    		usuario_remoto.setText(usuarioRemoto);
			    	} else {
			    		usuario_remoto.setText(mSipSession.getPeerProfile().getUriString());
			    	}
			    	
			    	
			    	updateStatus(getString(R.string.Informacion_SIP_PeticionEntrante));
			    	
			    	//ocultar teclado si está activado
			    	imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			    	imm.hideSoftInputFromWindow(edittext_cuentaSIP.getWindowToken(), 0);
	            }
	    	});
	            
	    }
	    
	    public void estadoLlamadaEstablecida(final String sessionDescription){
	    	
	    	this.runOnUiThread(new Runnable() {
	            public void run() {
	            	
	            	boton_comenzar_intercambio.setVisibility(View.INVISIBLE);
	                boton_seleccionar_contacto.setVisibility(View.INVISIBLE);
	                edittext_cuentaSIP.setVisibility(View.INVISIBLE);
	            	error_es.setVisibility(View.INVISIBLE);
			    	error.setVisibility(View.INVISIBLE);
			    	cuadro_interaccion_llamada_recibida.setVisibility(View.INVISIBLE);
			    	boton_cancelar_intercambio.setVisibility(View.INVISIBLE);
			    
			    	updateStatus(getString(R.string.Informacion_SIP_ProcesandoIntercambio));
			    	
			    	extraerSsidDireccionPuerto(sessionDescription);
			    
	            }
	    	});
	    	
	    }
	    
	    public void estadoError(){
	    	
	    	this.runOnUiThread(new Runnable() {
	            public void run() {
	            	
	            	boton_llamar_lan.setVisibility(View.VISIBLE);
	            	boton_comenzar_intercambio.setVisibility(View.VISIBLE);
	                boton_seleccionar_contacto.setVisibility(View.VISIBLE);
	                edittext_cuentaSIP.setVisibility(View.VISIBLE);
	                error_es.setVisibility(View.VISIBLE);
			    	error.setVisibility(View.VISIBLE);
	                
			    	cuadro_interaccion_llamada_recibida.setVisibility(View.INVISIBLE);
			    	boton_cancelar_intercambio.setVisibility(View.INVISIBLE);
			    	
			    	
			    	updateStatus(getString(R.string.Informacion_SIP_NotificacionError));
			    	
			    	error.setText(mensaje_error+"\t"+String.valueOf(codigo_error));
			    	
			    	if (indicador_llamada_perdida == true){
	        			agregarLlamadaPerdida(mCaller);
	        			finish();
	        		}
	            }
	    	});
	    }
		
		
		
		/**
	     * Updates the status box at the top of the UI with a message of your choice.
	     * @param status The String to display in the status box.
	     */
	    public void updateStatus(final String status) {
	        this.runOnUiThread(new Runnable() {
	            public void run() {
	                TextView labelView = (TextView) findViewById(R.id.notificacion_eventos);
	                labelView.setText(status);
	            }
	        });
	    }
	    
	    
	    @Override
	    protected Dialog onCreateDialog(int id) {
	        switch (id) {
	        
	        case DIALOG_ACTUALIZAR_CONFIGURACION:
	        	return new AlertDialog.Builder(this)
	        	    		.setMessage("Por favor, introduzca los datos de su cuenta SIP")
	                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	                            public void onClick(DialogInterface dialog, int whichButton) {
	                                updatePreferences();
	                            }
	                        })
	                        .setNegativeButton(
	                                android.R.string.cancel, new DialogInterface.OnClickListener() {
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
	        startActivity(settingsActivity);
	    }
	
	    
	    public void liberarRecursosILBC(){
			
			if (flag_send_packets==true){
			
				flag_send_packets=false;
				mAudioRecord.stop();
				mAudioRecord.release();
				mReceptorUnicastRTP_ILBC.stopAudioTrack();
				mRTPSession_emisora.removeParticipant(mParticipant_Receiver);
				mRTPSession_emisora.endSession();
				rtcpSocket.close();
	
				mDatagramSocket.close();
			}
			
		}
	    
	    
	   		
	    private ILBCCodecX mILBCCodecX;
	    private ILBCCodecY mILBCCodecY;
	    byte[] trama_codificada;
	    DatagramSocket rtpSocket_2 = null;
		DatagramSocket rtcpSocket_2 = null;
		RTPSession rtpSession=null;
		RTPSession rtpSession_2=null;
		Participant part_2=null;
	    
	    
	    
	    
	    private synchronized void startRTPEnviarILBC (
				String address_destino, 
				int port_destino, 
				int mode,
				int tam_buffer_otro_extremo){
			
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			
			// tamaño de las tramas a codificar (se tiene en cuenta que las muestras
			// son de 16 bits)
			
			if (mode==20){
				tamTramaAntesCodificacion=320;
			} else if (mode==30){
				tamTramaAntesCodificacion=480;
			}
			
			
			/** CORRECCIÓN DE BUFFERSIZE SI NECESARIO */
			// El tamaño del buffer de grabación debe ser múltiplo del tamaño de 
			// las tramas a codificar
			final int tamBufferAudioRecord = 
					tamTramaAntesCodificacion * 
					((int) Math.ceil((double)mMinBufferSizeAudioRecord/tamTramaAntesCodificacion));
			
			Log.d("tamBuffer","tamBufferAudioRecord "+tamBufferAudioRecord+
					" mMinBufferSizeAudioRecord "+mMinBufferSizeAudioRecord);
			
	        mAudioRecord = new AudioRecord(
	        		//MediaRecorder.AudioSource.MIC, 
	        		MediaRecorder.AudioSource.VOICE_COMMUNICATION,
	        		FREQ,
	        		//AudioFormat.CHANNEL_IN_STEREO,
		            AudioFormat.CHANNEL_IN_MONO,
		            AudioFormat.ENCODING_PCM_16BIT, 
		            tamBufferAudioRecord);
		    
		    bufferAudioRecord = new byte[tamBufferAudioRecord];
		    
	        //tamaño de las tramas codificadas
		    if (mode==20){
		    	tamTramaCodificada=38;
		    } else if (mode==30){
		    	tamTramaCodificada=50;
		    }
		    
		    final int N_paquetes_por_buffer=tamBufferAudioRecord/tamTramaAntesCodificacion;
		    
		    trama_a_codificar = new byte[tamTramaAntesCodificacion];
		    trama_codificada = new byte[tamTramaCodificada];
		    
		    mAudioRecord.startRecording();
		   
		    // 1. Create sockets for the RTPSession
		    try {
		    	rtcpSocket = new DatagramSocket(mDatagramSocket.getLocalPort()+1);
		    } catch (SocketException e) {}

		    // 2. Create the RTP session
		    mRTPSession_emisora = new RTPSession(mDatagramSocket, rtcpSocket);
	     				
		    // 3. Instantiate the application object
		    mReceptorUnicastRTP_ILBC =
		    		new ReceptorUnicastRTP_ILBC(mRTPSession_emisora,mHandler,mode,tam_buffer_otro_extremo);
	     				
		    // 4. Add participants we want to notify upon registration
	     	mParticipant_Receiver = new Participant(address_destino, port_destino, port_destino+1);
		    mRTPSession_emisora.addParticipant(mParticipant_Receiver);
	     				
	     				
		    // 5. Register the callback interface, this launches RTCP threads too
		    // The two null parameters are for the RTCP and debug interfaces, not use here
		    mRTPSession_emisora.RTPSessionRegister(mReceptorUnicastRTP_ILBC, null, null);
	     				
	     				
		    //Codificar y enviar
		    flag_send_packets=true;
	     				
		    if (mode==20){
		    	mILBCCodecX=ILBCCodecX.instance();
		    	mILBCCodecX.initEncode();
		    }else if(mode==30){
		    	mILBCCodecY=ILBCCodecY.instance();
		    	mILBCCodecY.initEncode();
		    }
	     				
		    Rthread_send_mode_x = new Thread(new Runnable() {
		    	public void run() {
		    		while (flag_send_packets == true) {
	     	
		    			try                    
		    			{	
		    				mAudioRecord.read(bufferAudioRecord, 0, tamBufferAudioRecord);
	     				             		
		    				for (int n=1; n <= N_paquetes_por_buffer; n++){
		    					
		    					trama_a_codificar=Arrays.copyOfRange(
		    							bufferAudioRecord, 
		    							(n-1)*bufferAudioRecord.length/N_paquetes_por_buffer
		    							, n*bufferAudioRecord.length/N_paquetes_por_buffer
		    							);
	     				        
		    					Log.d("tamBuffer","time_antesss");
		    					mILBCCodecX.encode(
		    							trama_a_codificar,
		    							0,
		    							tamTramaAntesCodificacion, 
		    							trama_codificada,
		    							0);
		    					mRTPSession_emisora.sendData(trama_codificada);
		    					Log.d("tamBuffer","time_despuesss");
		    					
		    					
		    				}
	     		                    	     	
	     		                      
	     		                   
		    			} catch (Throwable t) {
		    				Log.e("Error", "Read write failed");
		    				t.printStackTrace();
		    			}
	     						
		    		}
		    	}
		    });
	     					
	     				
		    Rthread_send_mode_y = new Thread(new Runnable() {
		    	public void run() {
		    		while (flag_send_packets == true) {
		    		
		    			try                    
		    			{	
	 		                    	
		    				mAudioRecord.read(bufferAudioRecord, 0, tamBufferAudioRecord);
	 				                   	
		    				for (int n=1; n <= N_paquetes_por_buffer; n++){
			                           	
		    					trama_a_codificar=Arrays.copyOfRange(
		    							bufferAudioRecord, 
		    							(n-1)*bufferAudioRecord.length/N_paquetes_por_buffer
		    							, n*bufferAudioRecord.length/N_paquetes_por_buffer
		    							);
		    					Log.d("tamBuffer","time_antesss");	            				
		    					mILBCCodecY.encode(
		    							trama_a_codificar,
		    							0,
		    							tamTramaAntesCodificacion, 
		    							trama_codificada,
		    							0);
		    					mRTPSession_emisora.sendData(trama_codificada);
		    					Log.d("tamBuffer","time_despuesss");			   	
		    				}
	 		                   	
		    			} catch (Throwable t) {
	    		                    
		    				Log.e("Error", "Read write failed");
	    		            t.printStackTrace();
	    		                
		    			}
	 						
		    		}
		    	}
		    });
	     				
		    if (mode==20){
		    	Rthread_send_mode_x.start();
		    } else if (mode==30){
		    	Rthread_send_mode_y.start();
		    }
	     				
	     				
	    }
	
	    
	    
}