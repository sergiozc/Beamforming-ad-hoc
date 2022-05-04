package com.example.phonedroid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipSession;

import com.example.phonedroid.SimpleSessionDescription.Media;

/**
 * Realiza una espera activa de las llamadas SIP entrantes, las atiende
 * cuando se producen y las gestiona para pasárselas a la actividad principal
 */
public class ReceptorLlamada extends BroadcastReceiver {

	/**
	 * Gestiona la llamada entrante, la contesta y se la pasa a la actividad
	 * principal
	 * @param context El contexto bajo el cual el receptor se ejecuta
	 * @param intent El "intent" que se recibe
	 */
	
	private static final int ID_NOTIFICACION_LLAMADA_ENTRANTE=2;
	
	private final long[] VIBRATION_PATTERN = { 100, 500, 1000, 500, 1000, 500, 1000
			, 500, 1000, 500, 1000, 500, 1000, 500, 1000
			, 500, 1000, 500, 1000, 500, 1000, 500, 1000
			, 500, 1000, 500, 1000, 500, 1000, 500, 1000};
	
	
	
	
	boolean indicador_llamada_iLBC = false;
	boolean indicador_llamada_LAN = false;
	String atributo_typeofcall= null;
	
	ServicioSIP mSIPService;
	
	SipAudioCall mSipAudioCall = null;
	String session;
	SimpleSessionDescription offer;
	
	NotificationManager mNotificationManager;
	Intent mIntent;
	PendingIntent mPendingIntent;
	Notification.Builder mNotificationBuilder;
	Notification mNotification;
	
	SipSession mSipSession = null;
	
	
	@Override
	public void onReceive(Context context, Intent incomingCallIntent) {
		
	
		mSIPService = (ServicioSIP) context;
		
		session = SipManager.getOfferSessionDescription(incomingCallIntent);
		offer = new SimpleSessionDescription(session);
		
		mNotificationManager =
			    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
		/* Características comunes de la notificación a crear */
        mNotificationBuilder = 
				new Notification.Builder(context)
		.setContentTitle(context.getString(R.string.notificacion_llamada_entrante))
        .setSmallIcon(R.drawable.ic_notificacion_llamada_entrante)
        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), AudioManager.RINGER_MODE_NORMAL)
        .setVibrate(VIBRATION_PATTERN)
        ;
		
		/* Determinar si se trata de llamada iLBC o LAN */
        for (Media media : offer.getMedia()) {
            
        	if ((media.getPort() > 0)
                    && "audio".equals(media.getType())
                    	&& "RTP/AVP".equals(media.getProtocol())) {
                
        		atributo_typeofcall=media.getAttribute("typeofcall");
                
                int[] rtppayloadtypes=media.getRtpPayloadTypes();
           	 	
           	 	for (int i=0;i<rtppayloadtypes.length;i++){
           	 		if (media.getRtpmap(rtppayloadtypes[i]) != null){
           	 			if (media.getRtpmap(rtppayloadtypes[i]).equals("iLBC/8000")){
           	 				indicador_llamada_iLBC=true;
           	 			}
           	 		}
           	 	}
            }
        }
		
        
        if (atributo_typeofcall == null){
        	indicador_llamada_LAN=false;
        } else if (atributo_typeofcall.equals("lan")){
        	indicador_llamada_LAN=true;
        	indicador_llamada_iLBC=false;
        }
        
        
       
		// Atender según el tipo de llamada detectado
		if (!indicador_llamada_iLBC && !indicador_llamada_LAN) { // Atender llamada "normal"
		
			try{
				mSipAudioCall = mSIPService.mSipManager.takeAudioCall(incomingCallIntent, null);
				/** Descubrir alguna forma de obtener la sessionDescription y poder decidir
				 * si se trata de llamada iLBC o normal */
				mSIPService.mSipAudioCall = mSipAudioCall;
				
				
				/* Agregar características específicas de la notificación */
				mIntent=new Intent(context, ActividadRecepcionLlamadaRapida.class);
        		
				mIntent.setFlags(
        				Intent.FLAG_ACTIVITY_CLEAR_TOP
        				|
        				//Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        				//Intent.FLAG_ACTIVITY_CLEAR_TASK		
        				//	|
        		Intent.FLAG_ACTIVITY_SINGLE_TOP
        		);
        		//i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        		
        		mPendingIntent=PendingIntent.getActivity(context, 0,
        				mIntent, 0);
        		
				mNotificationBuilder.setContentText(mSipAudioCall.getPeerProfile().getUriString());
				mNotificationBuilder.setFullScreenIntent(mPendingIntent, true);
				
				mNotification = mNotificationBuilder.getNotification();
				
				// mId allows you to update the notification later on.
				mNotificationManager.notify(ID_NOTIFICACION_LLAMADA_ENTRANTE, mNotification);
		
			}catch (SipException se) {}
			
		}//fin atender llamada "normal"
		
		else if (indicador_llamada_iLBC) {// Atender llamada iLBC
					
			try{
						
				if (incomingCallIntent == null) {
					throw new SipException("Cannot retrieve session with null intent");
				}
			
				mSipSession= mSIPService.mSipManager.getSessionFor(incomingCallIntent);	
						
				String offerSd = SipManager.getOfferSessionDescription(incomingCallIntent);
				if (offerSd == null) {
					throw new SipException("Session description missing in incoming "
							+ "call intent");
				}
				
				try {
					synchronized (this) {
						mSIPService.mSipSession = mSipSession;
						mSIPService.mPeerSd = offerSd;
					}
				} catch (Throwable t) {
					
					throw new SipException("takeAudioCall()", t);
				}
			
			}catch (SipException se) {}
				
			
			/* Agregar características específicas de la notificación */
			mIntent=new Intent(context, ActividadRecepcionLlamadaILBC.class);
    		
			mIntent.setFlags(
    				Intent.FLAG_ACTIVITY_CLEAR_TOP
    				|
    				//Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
    				//Intent.FLAG_ACTIVITY_CLEAR_TASK		
    				//	|
    				Intent.FLAG_ACTIVITY_SINGLE_TOP
    				);
    				//i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		
    		mPendingIntent=PendingIntent.getActivity(context, 0,
    				mIntent, 0);
    		
    		mNotificationBuilder.setContentText(mSipSession.getPeerProfile().getUriString());
    		mNotificationBuilder.setFullScreenIntent(mPendingIntent, true)
			;
			
			mNotification = mNotificationBuilder.getNotification();
			
			// mId allows you to update the notification later on.
			mNotificationManager.notify(ID_NOTIFICACION_LLAMADA_ENTRANTE, mNotification);
			
			
		}//fin atender llamada iLBC
		
		else if (indicador_llamada_LAN) {// Atender llamada LAN
			
			try{
				if (incomingCallIntent == null) {
					throw new SipException("Cannot retrieve session with null intent");
				}
		
				mSipSession = mSIPService.mSipManager.getSessionFor(incomingCallIntent);	
				String offerSd = SipManager.getOfferSessionDescription(incomingCallIntent);
				
				if (offerSd == null) {
					throw new SipException("Session description missing in incoming "
							+ "call intent");
				}
				
				try {
					synchronized (this) {
						mSIPService.mSipSession = mSipSession;
						mSIPService.mPeerSd = offerSd;
					}
				} catch (Throwable t) {
					throw new SipException("takeAudioCall()", t);
				}
			}catch (SipException se) {}
			
			
			/* Agregar características específicas de la notificación */
			mIntent=new Intent(context, ActividadRecepcionLlamadaLAN.class);
    		
			mIntent.setFlags(
    				Intent.FLAG_ACTIVITY_CLEAR_TOP
    				|
    				//Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
    				//Intent.FLAG_ACTIVITY_CLEAR_TASK		
    				//	|
    				Intent.FLAG_ACTIVITY_SINGLE_TOP
					);
    				//i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		
    		mPendingIntent=PendingIntent.getActivity(context, 0,
    				mIntent, 0);
    		
    		mNotificationBuilder.setContentText(mSipSession.getPeerProfile().getUriString());
	        mNotificationBuilder.setFullScreenIntent(mPendingIntent, true);
			
			mNotification = mNotificationBuilder.getNotification();
			
			// mId allows you to update the notification later on.
			mNotificationManager.notify(ID_NOTIFICACION_LLAMADA_ENTRANTE, mNotification);
			
		}//fin atender llamada LAN
	
	}//fin de onReceive()

}