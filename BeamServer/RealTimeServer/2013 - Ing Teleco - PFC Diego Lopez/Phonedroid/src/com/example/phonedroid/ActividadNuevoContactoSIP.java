package com.example.phonedroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * @author Diego López Herrera
 * 
 */
public class ActividadNuevoContactoSIP extends Activity {
	
	/** CONSTANTES */
	public final static String EXTRA_MESSAGE_NICKNAME_EDITADO = "com.example.phonedroid.MESSAGE_NICKNAME_EDITADO";
	public final static String EXTRA_MESSAGE_CUENTASIP_EDITADO = "com.example.phonedroid.MESSAGE_CUENTASIP_EDITADO";
	public final CharSequence AVISO_CAMPO_NULO = "Rellene Nombre y Cuenta SIP";
	
	/** VARIABLES */
	EditText edittext_cuentasip;
	EditText edittext_nickname;
	
	/** MÉTODOS */
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nuevo_contacto_sip);
		
		edittext_nickname=(EditText) findViewById(R.id.nombre_contacto);
		edittext_cuentasip=(EditText) findViewById(R.id.cuenta_sip);
		
        Intent intent_cuentasip = getIntent();
    	String nickname= intent_cuentasip.getStringExtra(ActividadGestionContactos.EXTRA_MESSAGE_NICKNAME);
        
    	/* CASO EDITAR */
        if (nickname != null){
    		String cuentasip = intent_cuentasip.getStringExtra(ActividadGestionContactos.EXTRA_MESSAGE_CUENTASIP);
        	
        	edittext_nickname.setText(nickname);
        	edittext_cuentasip.setText(cuentasip);
    	}
    }
	
	public void clickSIPAceptarContacto (View view) {
		/* Se muestra aviso si alguno de los campos obligatorios no está cumplimentado */
		if (edittext_nickname.getText().length() <= 0 || edittext_cuentasip.getText().length() <= 0) {
 			Context context = getApplicationContext();
        	Toast toast_aviso_campo_nulo = Toast.makeText(context, 
 					getString(R.string.aviso_campo_nulo_nuevo_contacto), Toast.LENGTH_SHORT);
 			toast_aviso_campo_nulo.show();
 		} else {/* si campos obligatorios cumplimentados... */
			String nickname=edittext_nickname.getText().toString();
 			String cuentasip=edittext_cuentasip.getText().toString();
		    
		    Intent intent = new Intent(this, ActividadGestionContactos.class);
		    intent.putExtra(EXTRA_MESSAGE_NICKNAME_EDITADO, nickname);
		    intent.putExtra(EXTRA_MESSAGE_CUENTASIP_EDITADO, cuentasip);
		    
		    setResult(RESULT_OK,intent);
		    finish();
 		}
	}
}