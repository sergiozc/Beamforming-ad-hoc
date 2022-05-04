package com.example.phonedroid;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;



/**
 * @author Diego López Herrera
 * 
 * Gestiona las llamadas salientes y entrantes, así como la interacción
 * con la interfaz de usuario
 */


public class ActividadRecepcionLlamadaLAN extends ActividadLlamadaLAN {
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
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
        //llSIP_EditarConf = (Button) findViewById(R.id.llSIP_EditarConf);
        
        //llSIP_layout_llamEstablecida = (RelativeLayout) findViewById(R.id.llSIP_layout_llamEstablecida);
        //llSIP_layout_llamEstablecida_opc= (RelativeLayout) findViewById(R.id.llSIP_layout_llamEstablecida_opc);
        
        //llSIP_layout_Colgar = (RelativeLayout) findViewById(R.id.llSIP_layout_Colgar);
        //llSIP_layout_CancelarLlamada = (RelativeLayout) findViewById(R.id.llSIP_layout_CancelarLlamada);
        //opcion_altavoz = (ToggleButton) findViewById(R.id.opcion_altavoz);
        //llSIP_EnEspera = (ToggleButton) findViewById(R.id.llSIP_EnEspera);
        //llSIP_Silenciar = (ToggleButton) findViewById(R.id.llSIP_Silenciar);
        //edittext_cuentaSIP=(EditText) findViewById(R.id.edittext_cuentaSIP);
        //spinner_opc_codec= (Spinner) findViewById(R.id.spinner_opc_codec);
        //spinner_opc_rtp_packets= (Spinner) findViewById(R.id.spinner_opc_rtp_packets);
        
        //spinner_opc_codec.setVisibility(View.GONE);
        
        
        
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
        
        incomingCallIndicator = true;
		
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        
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
}