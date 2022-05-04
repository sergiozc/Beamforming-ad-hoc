package com.example.phonedroid;

import java.text.ParseException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.net.sip.SipSession;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

//imports

public class ServicioSIP extends Service {
	
	/**
	 * CONSTANTES
	 */
	// Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    
    private static final int ID_NOTIFICACION_APLICACION_EJECUTANDOSE=1;
    
    /**
     * VARIABLES
     */
	SipManager mSipManager = null;
	SipProfile mSipProfile = null;
	SipAudioCall mSipAudioCall = null;
	SipSession mSipSession = null;
	String mPeerSd = null;
	ReceptorLlamada mReceptorLlamada;
	SipRegistrationListener mSipRegistrationListener=null;
	Thread Rthread_onCreate=null;
	Context mContext;
	
	PendingIntent pendInt;
	String username="";
	String dominio="";
	String cuentaSipUsuario="";
	
	NotificationManager mNotificationManager; 
	
	Notification mNotification_Inicial;
	Notification mNotification_Conectado;
	Notification mNotification_Conectando;
	Notification mNotification_NoConectado;
	
	Notification.Builder mNotificationBuilder_Conectado;
	Notification.Builder mNotificationBuilder_Conectando;
	Notification.Builder mNotificationBuilder_NoConectado;
	
	
	/**
	 * MÉTODOS
	 */
	
	/**
     * Clase que usa el Binder del cliente. Como se sabe que este servicio
     * siempre se ejecuta en el mismo proceso que su cliente, no se 
     * requiere utilizar IPC
     * 
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        ServicioSIP getService() {
            // Return this instance of LocalService so clients can call public methods
            return ServicioSIP.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
        
    @Override
    public void onRebind(Intent intent){
    	onBind(intent);
    }

    /** methods for clients */
    public SipManager getSipManager() {
      return mSipManager;
    }
    
    public SipProfile getSipProfile() {
        return mSipProfile;
      }
    
    public SipRegistrationListener getSipRegistrationListener() {
        return mSipRegistrationListener;
      }
    public void restartSipRegistrationListener() {
    	try{
    		mSipManager.setRegistrationListener(mSipProfile.getUriString(), mSipRegistrationListener);
    	}catch (SipException se){}
    }
	
    public SipAudioCall getSipAudioCall() {
        return mSipAudioCall;
    }
    public SipSession getSipSession() {
        return mSipSession;
      }
    public String getPeerSd() {
        return mPeerSd;
     }
	
	@Override
    public void onCreate() {
		
		mContext=this;
		
		mNotificationManager =
			    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		
		
		mNotification_Inicial=new Notification(
				R.drawable.ic_notificacion_aplicacion,
		getString(R.string.notificacion_phonedroid_iniciado),
		System.currentTimeMillis());
		
		Intent i=new Intent(mContext, ActividadInicio.class);
		
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
		Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi=PendingIntent.getActivity(mContext, 0,
		i, 0);
		
		mNotification_Inicial.setLatestEventInfo(mContext,
				"Phonedroid",
	    		getString(R.string.aviso_no_cuenta_sip),
				pi);
		
		mNotification_Inicial.flags|=Notification.FLAG_NO_CLEAR;
		
		startForeground(ID_NOTIFICACION_APLICACION_EJECUTANDOSE, mNotification_Inicial);
		
		
		Intent in=new Intent(mContext, ActividadInicio.class);
		
		in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
		Intent.FLAG_ACTIVITY_SINGLE_TOP);
		pendInt=PendingIntent.getActivity(mContext, 0,
		in, 0);
		
		mNotificationBuilder_Conectado = 
				new Notification.Builder(mContext)
        //.setContentTitle(getString(R.string.SIPStatus_Registering_brief))
		//.setContentTitle(mSipProfile.getUriString())
		.setContentTitle(cuentaSipUsuario)
		.setContentText(getString(R.string.Registro_SIP_Registrandose))
        .setContentIntent(pendInt)
        .setSmallIcon(R.drawable.animacion_notificacion_registrandose);
        

		mNotification_Conectado = mNotificationBuilder_Conectado.getNotification();
		
		mNotificationBuilder_Conectando = 
					new Notification.Builder(mContext)
		//.setContentTitle(getString(R.string.SIPStatus_Registered_brief))
		//.setContentTitle(mSipProfile.getUriString())    
		.setContentTitle(cuentaSipUsuario)
		.setContentText(getString(R.string.Registro_SIP_Registrado)) 
		.setContentIntent(pendInt)
	    .setSmallIcon(R.drawable.ic_notificacion_registrado);
		
			
		mNotification_Conectando = mNotificationBuilder_Conectando.getNotification();
				
		mNotificationBuilder_NoConectado = 
				new Notification.Builder(mContext)
		//.setContentTitle(getString(R.string.SIPStatus_FailedRegistration_2_brief))
		//.setContentTitle(mSipProfile.getUriString())
		.setContentTitle(cuentaSipUsuario)
		.setContentText(getString(R.string.Registro_SIP_FalloRegistro))
		.setContentIntent(pendInt)
		.setSmallIcon(R.drawable.ic_notificacion_registro_fallido);
		
		mNotification_NoConectado = mNotificationBuilder_NoConectado.getNotification();
				
		
		mSipRegistrationListener=new SipRegistrationListener() {
        			
			public void onRegistering(String localProfileUri) {
        		mNotificationManager.notify(ID_NOTIFICACION_APLICACION_EJECUTANDOSE, mNotification_Conectado);
			}

			
			public void onRegistrationDone(String localProfileUri, long expiryTime) {
        		mNotificationManager.notify(ID_NOTIFICACION_APLICACION_EJECUTANDOSE, mNotification_Conectando);
        	}

			
			public void onRegistrationFailed(String localProfileUri, int errorCode,
        			String errorMessage) {
    			
				mNotificationManager.notify(ID_NOTIFICACION_APLICACION_EJECUTANDOSE, mNotification_NoConectado);
    		}
    		
		};
		
		
		
		Rthread_onCreate = new Thread(new Runnable() {
		        public void run() {
		    		initializeManager();
		        }
		});
		Rthread_onCreate.start();
    	
    }
    
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        username = preferencias.getString("nombrePref", "");
        dominio = preferencias.getString("dominioPref", "");
        cuentaSipUsuario=username+"@"+dominio;
        
		initializeManager();
		
		//return(START_REDELIVER_INTENT);
		return(START_NOT_STICKY);//default
	}
	
	@Override
	public void onDestroy() {
		
		if (mReceptorLlamada != null) {
            this.unregisterReceiver(mReceptorLlamada);
        }
		if (mSipManager != null){
			try{
				//mSipManager.unregister(mSipProfile, mSipRegistrationListener);
				mSipManager.setRegistrationListener(mSipProfile.getUriString(), null);
			} catch (SipException se) {}
			
		}
		
		closeLocalProfile();
		
		mSipManager=null;
		
        
        
        mNotificationManager.cancel(ID_NOTIFICACION_APLICACION_EJECUTANDOSE);
        
        stopForeground(true);
	}
	
	public void initializeManager() {
        if(mSipManager == null) {
        	mSipManager = SipManager.newInstance(this);
        }

        inicializarPerfilLocal();
    }
	
	/**
     * Se configura el intent filter. Este se usará para lanzar el BroadcastReceiver
     * cuando alguien contacte con la dirección SIP del usuario 
     */
	public void configurarReceptor() {
		
		if (mReceptorLlamada != null) {
			this.unregisterReceiver(mReceptorLlamada);
        }
				
		IntentFilter intent_filter = new IntentFilter();
		intent_filter.addAction("com.example.phonedroid.action.INCOMING_CALL_LLAMADA");
    				 
		mReceptorLlamada = new ReceptorLlamada();
		this.registerReceiver(mReceptorLlamada, intent_filter);
	}
	/**
     * Registro en el servidor SIP para poder iniciar y recibir llamadas
     */
    public void inicializarPerfilLocal() {
        configurarReceptor();
    	
    	if (mSipManager == null) {
            return;
        }

        if (mSipProfile != null) {
            closeLocalProfile();
        }

        //setUpReceiver();
        
        SharedPreferences preferencias = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String nombre_usuario = preferencias.getString("nombrePref", "");
        String dominio = preferencias.getString("dominioPref", "");
        String password = preferencias.getString("contraseniaPref", "");

        if (nombre_usuario.length() == 0 || dominio.length() == 0 || password.length() == 0) {
        	mSipManager=null;
        	mNotificationManager.notify(ID_NOTIFICACION_APLICACION_EJECUTANDOSE, mNotification_Inicial);
            return;
        }
        
        mNotificationBuilder_Conectado.setContentTitle(cuentaSipUsuario);
        mNotificationBuilder_Conectando.setContentTitle(cuentaSipUsuario);
        mNotificationBuilder_NoConectado.setContentTitle(cuentaSipUsuario);
		
        mNotification_Conectado = mNotificationBuilder_Conectado.getNotification();
        mNotification_Conectando = mNotificationBuilder_Conectando.getNotification();
        mNotification_NoConectado = mNotificationBuilder_NoConectado.getNotification();
        
        try {
            SipProfile.Builder builder = new SipProfile.Builder(nombre_usuario, dominio);
            builder.setPassword(password);
            
            mSipProfile = builder.build();

            Intent intent = new Intent();
            
            intent.setAction("com.example.phonedroid.action.INCOMING_CALL_LLAMADA");
                        
            PendingIntent pending_intent = PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA);
            mSipManager.open(mSipProfile, pending_intent, null);
            
            
            // This listener must be added AFTER manager.open is called,
            // Otherwise the methods aren't guaranteed to fire.
			mSipManager.setRegistrationListener(mSipProfile.getUriString(), mSipRegistrationListener);
            	
        } catch (ParseException pe) {
        	
        	

        } catch (SipException se) {
            
            

        }
    }

    /**
     * Cierre del perfil local, liberando los objetos asociados en la memoria
     * y cancelando el registo en el servidor SIP
     */
    public void closeLocalProfile() {
        if (mSipManager == null) {
            return;
        }
        try {
            if (mSipProfile != null) {
            	mSipManager.close(mSipProfile.getUriString());
            }
        } catch (Exception ee) { }
        
    }
		
}