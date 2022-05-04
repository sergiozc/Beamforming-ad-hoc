package com.example.phonedroid;



import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Arrays;

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
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipSession;
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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.phonedroid.ServicioSIP.LocalBinder;
import com.example.phonedroid.SimpleSessionDescription.Media;


/**
 * @author Diego López Herrera
 * 
 * Gestiona las llamadas salientes y entrantes, así como la interacción
 * con la interfaz de usuario
 */

public class ActividadLlamadaILBC extends Activity {
	/**
	 *  CONSTANTES 
	 */
	private static final int DIALOG_ACTUALIZAR_CONFIGURACION = 1;
	
	private static final int ID_NOTIFICACION_LLAMADA_ENTRANTE=2;
	private static final int ID_NOTIFICACION_LLAMADA_PERDIDA=3;
	
	final int INCOMING_CALL=1;
	final int OUTGOING_CALL=0;
	
	private final int FREQ=8000;
	
	
	/**
	 *  VARIABLES 
	 */
	
	//NUEVAS RESPECTO A SIPLlamadasRapidas.java
	private long mSessionId = System.currentTimeMillis();
	SipSession mSipSession = null;
	DatagramSocket mDatagramSocket;
	String mPeerSd=null;
	String usuarioRemoto=null;
	
	
	int mMinBufferSizeAudioRecord;
	int mMinBufferSizeAjustado;
    private Thread Rthread_send = null;
    private Thread Rthread_send_mode_x = null;
    private Thread Rthread_send_mode_y = null;
	private boolean flag_send_packets=false;
	private Thread Rthread_receive =null;
	private Thread Rthread_send2 = null;
	
	ILBCCodecX mILBCCodecX;
	ILBCCodecY mILBCCodecY;
	
	private AudioRecord mAudioRecord=null; 
	
	private RTPSession mRTPSession_emisora=null;
	private RTPSession mRTPSession_receptora=null;
	//private UnicastRTP_iLBC_Receiver mUnicastRTP_iLBC_Receiver=null;
	//private UnicastRTP_iLBC_Receiver_2 mUnicastRTP_iLBC_Receiver_2=null;
	private ReceptorUnicastRTP_ILBC mReceptorUnicastRTP_ILBC=null;
	Participant mParticipant_Receiver=null;
	Participant mParticipant_Sender=null;
	DatagramSocket rtpSocket_rec = null;
	DatagramSocket rtcpSocket_rec = null;
	DatagramSocket rtpSocket = null;
	DatagramSocket rtcpSocket = null;
	byte[] trama_a_codificar;
    int tamTramaAntesCodificacion=320;
    int tamTramaCodificada=38;
	byte[] bufferAudioRecord;
	byte[] trama_codificada;
	final Handler mHandler = new Handler();
	
	String ad=null;
	int po=-1;
	int busi=0;
	
	
	//FIN NUEVAS RESPECTO A SIPLlamadasRapidas.java
	
	
	String direccion_SIP = null;
	String mensaje_error=null;
    int codigo_error;
    
    SipManager mSipManager = null;
    SipProfile mSipProfile = null;
    
    
    TextView usuario_local;
    TextView usuario_remoto;
    TextView error_es;
    TextView error;
    
    RelativeLayout cuadro_interaccion_en_espera;
    EditText edittext_cuentaSIP;
    Spinner spinner_opc_codec;
    Spinner spinner_modo_ilbc;
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
    
    
  
    ServicioSIP mSIPService;
    boolean mBound = false;
    NotificationManager mNotificationManager;
    //SipRegistrationListener mSipRegistrationListener;
    boolean incomingCallIndicator;
    
    boolean indicador_llamada_perdida=false;
    String mCaller="null";
    
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
        
        // Se eliminan componentes de la interfaz gráfica que no se utilizan
        // (se utiliza una misma interfaz para varias actividades distintas)
        spinner_opc_codec.setVisibility(View.GONE);
        opcion_poner_en_espera.setVisibility(View.GONE);
        opcion_silenciar.setVisibility(View.GONE);
        
        /** por defecto se escoge MINIMUM como tamaño de paquete RTP */
        spinner_modo_ilbc.setSelection(0); 
        
        
        /** cálculo de mínimo buffer para inicializar AudioRecord en este dispositivo */
        // Se debe hacer un ajuste posterior para hacer que sea múltiplo del tamaño
        // de trama a codificar (depende del modo de iLBC escogido)
        mMinBufferSizeAudioRecord = AudioRecord.getMinBufferSize(
        		FREQ,
	            //AudioFormat.CHANNEL_IN_STEREO,
        		AudioFormat.CHANNEL_IN_MONO,
	            AudioFormat.ENCODING_PCM_16BIT);
	    
	   
        mNotificationManager =
			    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        
        incomingCallIndicator = false;
        
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
	    // Liberar recursos iLBC
        liberarRecursosILBC();
        // Restablecer configuración de sonido normal
		AudioManager audio_manager = (AudioManager) getSystemService(AUDIO_SERVICE);
		audio_manager.setSpeakerphoneOn(false);
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
            		atenderLlamada();
            	} 
            }
    	
    }

    

    /**
     * Realiza una llamada
     */
    public void iniciarLlamada() {
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
            		
            		mPeerSd=sessionDescription;
            		
            		Log.d("sesionRecibida", "sesionRecibida\n"+mPeerSd);
            		
            		estadoLlamadaEstablecida();
            		
            		/**
            		 * Ahora en vez de call.startAudio lanzaremos las hebras emisora
            		 * y receptora de UDP, que también se encargan de la codificación/
            		 * decodificación del audio
            		 */
            		
            		startAudioILBC_5(OUTGOING_CALL);
            		
                }

                @Override
                public void onCallEnded(SipSession session) {
                	
                	estadoEspera();
                	
                	updateStatus(getString(R.string.Informacion_SIP_LlamadaSalienteTerminada));
                	
                	//PROVISIONAL (puede que lo suyo sea meterlo en Estado de Espera)
                	//liberar recursos de sockets y RTPSession
                	//mDatagramSocket.close();
                	
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
                	
                	updateStatus(getString(R.string.Informacion_SIP_LlamadaRechazada));
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
            	        	
            	        	/*
            	        	 * Supuestamente se le debería pasar un listener null a mSipSession,
            	        	 * pero le pasamos el listener de SipSession que hemos creado
            	        	 */
            	        	
            	        	mSipSession = mSipManager.createSipSession(sip_profile_mio, null);
            	        	mSipSession.setListener(sipSessionListener);
            	        	        
            	        	/**
            	        	 * Ahora vemos qué se hace dentro de call.makeCall y adaptamos
            	        	 * adecuadamente
            	        	 */
            	        	     	         
            	        	synchronized (this) {
            	        		try {
            	        			  
            	        			/*
        			            	  * aquí deberíamos inicializar nuestro DatagramSocket
        			            	  * de manera similar a como se define el AudioStream
        			            	  */
            	        			mDatagramSocket = new DatagramSocket();
            	        			                 
            	        			            	  
        			            	 /**
        	            	        	 * En siguiente paso se llama a una función interna que creamos para
        	            	        	 * crear la descripción de la sesión
        	            	        	 */            	        	        
        			            	 /* Creación de la descrición de la sesión
        			            	  */
        			            	 
            	        	        
            	        			mSipSession.makeCall(sip_profile_destino, createOffer().encode(), 30);        
            	        	 	} catch (IOException e) {
            	        	 		throw new SipException("makeCall()", e);
            	        	 	}
            	        	}
            	        	
            	        	
            	        	
            	        } catch (ParseException e) {
            	            throw new SipException("build SipProfile", e);
            	        }
           
    	
    	
    	
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
            
            
            if (mSipSession != null) {
            	mSipSession=null;
            }
            
        }
    }

    /**
     * Crear una descripción de sesión oferta
     */
    private SimpleSessionDescription createOffer() {
    	
    	SimpleSessionDescription offer =
    			new SimpleSessionDescription(mSessionId, mSipSession.getLocalIp());
    	Media media = offer.newMedia(
    			"audio", mDatagramSocket.getLocalPort(), 1, "RTP/AVP");
        
    	
    	int TAM_TRAMA_SIN_CODIFICAR=320;
        String mode="20";
    	int mode_selected=spinner_modo_ilbc.getSelectedItemPosition();
    	 
        switch(mode_selected){
    	case 0: 
     		mode="20";
     		TAM_TRAMA_SIN_CODIFICAR=320;
     		break;
        case 1: 
        	mode="30";
        	TAM_TRAMA_SIN_CODIFICAR=480;
        	break;
        default: 
        	mode="20";
        	TAM_TRAMA_SIN_CODIFICAR=320;
     	};
    	 
     	mMinBufferSizeAjustado = 
				TAM_TRAMA_SIN_CODIFICAR * 
				((int) Math.ceil((double)mMinBufferSizeAudioRecord/TAM_TRAMA_SIN_CODIFICAR));
     	 
     	 
     	media.setRtpPayload(97, "iLBC/8000", "mode="+mode);
     	media.setAttribute("buffersize", String.valueOf(mMinBufferSizeAjustado));
    	 
     	Log.d("offer","offer\n"+offer.encode());
     	
    	return offer;
    }
    
    
    /**
     * Contestar una llamada
     */
    public void atenderLlamada() {
        
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
            		
            		
            		mPeerSd=sessionDescription;
            		estadoLlamadaEstablecida();
            		
            		startAudioILBC_5(INCOMING_CALL);
            	
            	}
            	
            	@Override
                public void onCallEnded (SipSession session) {

            		mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE);
            	
            		estadoEspera();
            		updateStatus(getString(R.string.Informacion_SIP_LlamadaEntranteTerminada));
            		
            		finish();
            		
                	
                }
            	
            	@Override
                public void onError (SipSession session, int errorCode, String errorMessage){

            		mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE);
            		
            		mensaje_error=errorMessage;
            		codigo_error=errorCode;
            		estadoError();
            		
            		finish();
            		
                }
            	
		
            };
            
            //datos para notificar llamda perdida si necesario
            indicador_llamada_perdida=true;
            mCaller=mSipSession.getPeerProfile().getUriString();
            
            mSipSession.setListener(sipSessionListener);
            
            
        }
        catch (Exception e) {
        	estadoEspera();
        	Log.i("LlamadasSIPActivity/InitiateCall", "Error al intentar crear el listener SIP", e);

            if (mSipProfile != null) {
                try {
                	mSipManager.close(mSipProfile.getUriString());
                } catch (Exception ee) {
                	estadoEspera();
                	Log.i("LlamadasSIPActivity/ContestarCall","Error al intentar cerrar el gestor SIP", ee);

                    ee.printStackTrace();
                }
            }
            if (mSipSession != null) {
            	mSipSession =null;
            }
        }
    }
    

    /**
     * Crear una descripción de sesión repuesta
     */
    private SimpleSessionDescription createAnswer(String offerSd) {
    	         
    	SimpleSessionDescription offer =
                 new SimpleSessionDescription(offerSd);
         SimpleSessionDescription answer =
                 new SimpleSessionDescription(mSessionId, mSipSession.getLocalIp());
         
         boolean indicador_iLBC=false;
         
         for (Media media : offer.getMedia()) {
             if ((media.getPort() > 0)
                    && "audio".equals(media.getType())
                       && "RTP/AVP".equals(media.getProtocol())) {
                 
                    
            	 int[] rtppayloadtypes=media.getRtpPayloadTypes();
            	 int rtppayloadtype_ilbc=97;
            	 
    	 
            	 
            	 for (int i=0;i<rtppayloadtypes.length;i++){
            		 if (media.getRtpmap(rtppayloadtypes[i]) != null) {
	            		 if (media.getRtpmap(rtppayloadtypes[i]).equals("iLBC/8000")){
	            			 indicador_iLBC=true;
	            			 rtppayloadtype_ilbc=rtppayloadtypes[i];
	            		 }
            		 }

            	 }
            	 
                 
            	 if (indicador_iLBC){
            		 
            		 Media reply = answer.newMedia(
                     		 "audio", mDatagramSocket.getLocalPort(), 1, "RTP/AVP");
                     
                     String mode=media.getFmtp(rtppayloadtype_ilbc);
                     
                     int TAM_TRAMA_SIN_CODIFICAR=320;
                     
                     if (mode != null) {
	                     if (mode.equals("mode=20")){
	                            	 TAM_TRAMA_SIN_CODIFICAR=320;
	                         	 } else if (mode.equals("mode=30")){
	                         		 TAM_TRAMA_SIN_CODIFICAR=480;
	                         	 }
                     } else {mode="mode=20";}
                             
                     mMinBufferSizeAjustado = 
                     		 TAM_TRAMA_SIN_CODIFICAR * 
                     		 ((int) Math.ceil((double)mMinBufferSizeAudioRecord/TAM_TRAMA_SIN_CODIFICAR));
                     
                     
                     reply.setRtpPayload(rtppayloadtype_ilbc, "iLBC/8000", mode);
                     reply.setAttribute("buffersize", String.valueOf(mMinBufferSizeAjustado));
                             
                 }
            	 
            	 continue;
             }
             if (!indicador_iLBC) {
	             // Reject the media.
	             Media reply = answer.newMedia(
	                     media.getType(), 0, 1, media.getProtocol());
	            		 
	             for (String format : media.getFormats()) {
	                 reply.setFormat(format, null);
	             }
             }
         }
        
         if (!indicador_iLBC){
         	throw new IllegalStateException("Reject SDP: no suitable codecs");
         }
         
         Log.d("answer","answer\n"+answer.encode());
         
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
		
		String typeofcall="iLBC";
		
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
	
            	
		    	liberarRecursosILBC();
		    	
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
		    			determinarUsuarioRemoto(mSipSession.getPeerProfile().getUriString());
		    	if (usuarioRemoto != null){
		    		usuario_remoto.setText(usuarioRemoto);
		    	} else {
		    		usuario_remoto.setText(mSipSession.getPeerProfile().getUriString());
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
		    			determinarUsuarioRemoto(mSipSession.getPeerProfile().getUriString());
		    	if (usuarioRemoto != null){
		    		usuario_remoto.setText(usuarioRemoto);
		    	} else {
		    		usuario_remoto.setText(mSipSession.getPeerProfile().getUriString());
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
		    	
		    	liberarRecursosILBC();
		    	
		    	if (indicador_llamada_perdida == true){
        			agregarLlamadaPerdida(mCaller);
        			
        		}
		    	
            }
    	});
    }
    
    
    public void clickSIPContestar(View view){
    	try {
    		/** contestar a la llamada/sesión:    */
    		synchronized (this) {
    			try {
    			            	 
    				mDatagramSocket= new DatagramSocket();
    			    
    				mSipSession.answerCall(createAnswer(mPeerSd).encode(), 30);
	        	    
    			} catch (IOException e) {
    				throw new SipException("answerCall()", e);
    			}
    			             
    			mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE);
    		}
    		
    	} catch (Exception e) {e.printStackTrace();}
    }
    
    public void clickSIPRechazar(View view){
    	
    	indicador_llamada_perdida=false;
    	
    	if (mSipSession != null){
    		if (mSipSession.getState() == SipSession.State.INCOMING_CALL){
    			mSipSession.endCall();
    		}
    	}
    	
    	mNotificationManager.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE);
    	
    }
    
    
    /**
     * MÉTODOS QUE DEFINEN LAS OPCIONES DEL USUARIO
     */
    
    /* OPCIONES DURANTE LA LLAMADA */ 
 
    public void clickSIPCancelarLlamada(View view){
    	
    				//liberamos los recursos reservados para mDatagramSocket
            		mDatagramSocket.close();
            		if (mSipSession != null) {
            			if (mSipSession.getState() == SipSession.State.OUTGOING_CALL   ||
            					 mSipSession.getState() == SipSession.State.OUTGOING_CALL_RING_BACK){
        
            				mSipSession.endCall();
            			}
            		}
    }
    
    
    
    public void clickSIPColgar(View view){
    	if (mSipSession != null){
    		if (mSipSession.getState() == SipSession.State.IN_CALL){
    			mSipSession.endCall();
    		}
    	}
    }
    
    
    public void clickSIPAltavoz(View view){
    	
    	if (mSipSession != null) {
    		if (mSipSession.getState() == SipSession.State.IN_CALL){
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
    
    public void clickSIPLlamar(View view){
    	//EVITAR QUE SE PUEDA LLAMAR A UN STRING EN BLANCO
    	if (edittext_cuentaSIP.getText().length() <= 0) {
 			Context context = getApplicationContext();
        	Toast toast_aviso_campo_nulo = Toast.makeText(context, 
 					getString(R.string.aviso_campo_cuenta_sip_nulo), Toast.LENGTH_SHORT);
        	toast_aviso_campo_nulo.show();
 		} else {
 			direccion_SIP = edittext_cuentaSIP.getText().toString();
 		    iniciarLlamada();
 		}
    	
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
    
    
    /**
     * Starts the audio for the established call
     */
    public void startAudioILBC_5(int flag_incoming_call) {
    	try {
            startAudioInternalILBC_5(flag_incoming_call);
        } catch (UnknownHostException e) { }
    }

    
    private synchronized void startAudioInternalILBC_5 (int flag_incoming_call) throws UnknownHostException {
    
        if (mPeerSd == null) {
            throw new IllegalStateException("mPeerSd = null");
        }
        
        int mode=20;//valor para inicializar, se cambia después
        int bufferSize=0;//valor para inicializar, se cambia después
        int port=-1;
        String address=null;
      
        SimpleSessionDescription offer =
                new SimpleSessionDescription(mPeerSd);
   
        for (Media media : offer.getMedia()) {
            if ((media.getPort() > 0)
            		&& "audio".equals(media.getType())
            		&& "RTP/AVP".equals(media.getProtocol())) {
               
            	String buffSize=media.getAttribute("buffersize");
            	if (buffSize != null){
                	bufferSize=Integer.parseInt(buffSize);
            	} else { bufferSize=0; }
                
                if (flag_incoming_call == INCOMING_CALL){
                	
                	int[] rtppayloadtypes=media.getRtpPayloadTypes();
	            	int rtppayloadtype_ilbc=97;
	            	 
	            	for (int i=0;i<rtppayloadtypes.length;i++){
	            		if (media.getRtpmap(rtppayloadtypes[i]) != null) {
	            			
	            			if (media.getRtpmap(rtppayloadtypes[i]).equals("iLBC/8000")){
   	            				rtppayloadtype_ilbc=rtppayloadtypes[i];
   	            			}
	            			
	            		}
	            	}
                	
                	
                	
	            	String ftmp=media.getFmtp(rtppayloadtype_ilbc);//(media.getRtpPayloadTypes()[0]);
                	
                	
                	if (ftmp.equals("mode=20")){
                		mode=20;
                	} else if (ftmp.equals("mode=30")){
                		mode=30;
                	}
                	
                
                } else {//si estamos realizando nosotros la llamada
                	int mode_selected=spinner_modo_ilbc.getSelectedItemPosition();
                	switch(mode_selected){
                	case 0: 
        				mode=20;
        			break;
                	case 1: 
        				mode=30;
        			break;
                   	default: mode=20;
        			};
                }
                
                address = media.getAddress();
                if (address == null) {
                	address = offer.getAddress();
                }
                port=media.getPort();
        
                break;
            }
        }
        
        // introducir alguna restricción en caso de que no se tengan todos los datos imprescindibles
        // y se finalice la sesión (por ejemplo, el buffersize es prescindible, pero no la address)
        //con lo cual se rechazaría la llamada
        startRTPEnviarILBC(address,port,mode,bufferSize);
               
        
        /*   
        if (codec == null) {
            throw new IllegalStateException("Reject SDP: no suitable codecs");
        }
        */
            
    }
    
    
    
	

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
	    	//rtpSocket = new DatagramSocket();
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
     				            				
     						mILBCCodecX.encode(
     								trama_a_codificar,
     								0,
     								tamTramaAntesCodificacion, 
     								trama_codificada,
     								0);
     						mRTPSession_emisora.sendData(trama_codificada);
     				            		   	
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
 				            				
	    					mILBCCodecY.encode(
	    							trama_a_codificar,
	    							0,
	    							tamTramaAntesCodificacion, 
	    							trama_codificada,
	    							0);
 				            			
	    					mRTPSession_emisora.sendData(trama_codificada);
	 		                    
		                           	
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
		
    
}