package com.example.phonedroid;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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


public class ActividadRecepcionLlamadaRapida extends ActividadLlamadaRapida {
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
    	setContentView(R.layout.llamada_sip);
        
        //puede que vengan bien unas hebras aquí
        
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
        
        spinner_opc_codec.setVisibility(View.GONE);
        spinner_modo_ilbc.setVisibility(View.GONE);
        elegir_modo_ilbc.setVisibility(View.GONE);
 
        
        incomingCallIndicator = true;
        
        
        /** FLAGS DE LA PANTALLA */
        // se permite que se abra la actividad cuando se reciban llamadas,
        // a pesar de que esté bloqueado el dispositivo 
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        // se mantiene la pantalla activa
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
       
	}
}