package com.example.phonedroid;

import java.util.Arrays;

import org.jlibrtp.DataFrame;
import org.jlibrtp.Participant;
import org.jlibrtp.RTPAppIntf;
import org.jlibrtp.RTPSession;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

/**
 * <p>This is an example of how to set up a Unicast session.</p>
 * <p>It does not accept any input arguments and is therefore of limited practical value, but it shows
 * the basics.</p>
 *
 * <p> The class has to implement RTPAppIntf.</p>
 * @author Arne Kepp
 */
public class ReceptorUnicastRTP_ILBC implements RTPAppIntf {
	
	private final int FREQ=8000;
	
	private final int TAM_SUPER_BUFFER_INICIAL=2000;
	
	
	
	private final int RETARDO_TOTAL_DESEADO=180;
	private final int RETARDO_RECEPTOR_SIN_DATOS=150;
	
	private int retardo_total_provisional;
	private int RETARDO_TOTAL;
	private int RETARDO_EMISOR;
	private int retardo_receptor_provisional;
	private int RETARDO_RECEPTOR;
	
	
	private int TAM_BUFFER=0;
	private int TAM_SUPER_BUFFER=2000;
	private int TAM_BUFFER_AUDIOTRACK=0;
	
	
	private int ilbc_mode=20;
	
	private int N_MS_TRAMA=20;
	private int TAM_TRAMA_CODIFICADA=38;
	private int TAM_TRAMA_DECODIFICADA=320;
	
	
	private int N_TRAMAS_BUFFER=0;
	private long N_MS_BUFFER=0;
	private int N_TRAMAS_SUPER_BUFFER=0;
	
	
	private ILBCCodecX mILBCCodecX;
	private ILBCCodecY mILBCCodecY;
	
	private AudioTrack mAudioTrack;
	
	
	//recepción de paquetes
	private byte[] trama_codificada;
	//private byte[] buffer;
	private byte[] super_buffer;
	private byte[] buffer_a_decodificar;
	private byte[] buffer_a_reproducir;
	
	
	//procesado de paquetes recibidos
	private boolean indicador_recepcion;//=false;
	Handler mHandler;// = new Handler();
	
	
	private boolean flag_primer_paquete=true;
	private int n_sec_inicial=0;
	private int n_sec_recibido=0;
	private int posicion=0;
	private int n_recibidos_de_mas=0;
	private int n_adelantados=0;
	private int n_buffers_a_eliminar=0;
	
	/** Holds a RTPSession instance */
	RTPSession rtpSession = null;


	/** A minimal constructor */
	public ReceptorUnicastRTP_ILBC(
			RTPSession rtpSession, 
			Handler mHandler, 
			int mode, 
			int tamBufferEmisor)
	{  
		
		this.rtpSession = rtpSession;

		this.mHandler=mHandler;
		
		
		ilbc_mode=mode;
		
		if (ilbc_mode==20){
			//TAM_AUDIO=320;
			mILBCCodecX=ILBCCodecX.instance();
			mILBCCodecX.initDecode();
			
			N_MS_TRAMA=20;
			TAM_TRAMA_CODIFICADA=38;
			TAM_TRAMA_DECODIFICADA=320;
			
			//mILBCCodecX=ILBCCodecX.instance();
			//ms_por_ronda=20*n_paq_por_ronda;
		} else if (ilbc_mode==30){
			//TAM_AUDIO=480;
			mILBCCodecY=ILBCCodecY.instance();
			mILBCCodecY.initDecode();
			
			N_MS_TRAMA=30;
			TAM_TRAMA_CODIFICADA=50;
			TAM_TRAMA_DECODIFICADA=480;
			//mILBCCodecY=ILBCCodecY.instance();
			//ms_por_ronda=30*n_paq_por_ronda;
		}
		
		
		//Mínimo buffer para Audiotrack
		final int minBufferSizeInicialSinAjuste=AudioTrack.getMinBufferSize(
				FREQ, 
				AudioFormat.CHANNEL_OUT_MONO, 
				AudioFormat.ENCODING_PCM_16BIT);
		
		Log.d("iniciando Unicast_RTP", "tam_buffer_audiotrack_inicial"
	    		+minBufferSizeInicialSinAjuste);
		
		final int minBufferSizeInicial = 
				TAM_TRAMA_DECODIFICADA * 
				((int) Math.ceil((double)minBufferSizeInicialSinAjuste/TAM_TRAMA_DECODIFICADA));
		//Asegurar múltiplo de 480 bytes (modo 30 ms)
		
		Log.d("iniciando Unicast_RTP", "sin ajuste "
	    		+minBufferSizeInicialSinAjuste + " con "+minBufferSizeInicial);
		
		
		/** Establecimiento del retardo en recepción */
		RETARDO_EMISOR=N_MS_TRAMA * (tamBufferEmisor/TAM_TRAMA_DECODIFICADA);
		retardo_receptor_provisional = N_MS_TRAMA * (minBufferSizeInicial/TAM_TRAMA_DECODIFICADA);
		retardo_total_provisional=RETARDO_EMISOR + retardo_receptor_provisional;
		
		
		if (RETARDO_EMISOR != 0) { ////sí se ha indicado tamaño del buffer del emisor
			
			if (retardo_total_provisional >= RETARDO_TOTAL_DESEADO) {
				RETARDO_RECEPTOR=
						RETARDO_EMISOR *
						((int) Math.ceil((double)retardo_receptor_provisional/RETARDO_EMISOR));
			} else if (retardo_total_provisional < RETARDO_TOTAL_DESEADO) {
				RETARDO_TOTAL=
						RETARDO_EMISOR *
						((int) Math.ceil((double)RETARDO_TOTAL_DESEADO/RETARDO_EMISOR));
				
				RETARDO_RECEPTOR=RETARDO_TOTAL-RETARDO_EMISOR;
			}
					
		} else { //no se ha indicado tamaño del buffer del emisor
			if (RETARDO_RECEPTOR_SIN_DATOS >= retardo_receptor_provisional) {
				//no hay limitación hardware para nuestro propósito
				RETARDO_RECEPTOR =
						N_MS_TRAMA *
						((int) Math.ceil((double)RETARDO_RECEPTOR_SIN_DATOS/N_MS_TRAMA))
						;
			} else { //limitación de hardware
				RETARDO_RECEPTOR = retardo_receptor_provisional;
			}
			
		}
		
		Log.d("retardos","RR " + RETARDO_RECEPTOR + " RE " + RETARDO_EMISOR );
		
		RETARDO_TOTAL= RETARDO_EMISOR + RETARDO_RECEPTOR;
		
		TAM_BUFFER_AUDIOTRACK = (RETARDO_RECEPTOR/N_MS_TRAMA) * TAM_TRAMA_DECODIFICADA;
		
		
		Log.d("tam_buffer_audiotrack","tam_buffer_audiotrack " + TAM_BUFFER_AUDIOTRACK);
		
		
	    
	    Log.d("iniciando Unicast_RTP", "tam_buffer_audiotrack_ajustado"
	    		+TAM_BUFFER_AUDIOTRACK+"\tla"+(minBufferSizeInicial/TAM_TRAMA_DECODIFICADA));
	    
		//Inicialización de variables
				
	    N_TRAMAS_BUFFER=TAM_BUFFER_AUDIOTRACK/TAM_TRAMA_DECODIFICADA;
	    TAM_BUFFER = N_TRAMAS_BUFFER*TAM_TRAMA_CODIFICADA;
		
	    TAM_SUPER_BUFFER = 
	    		((int) Math.ceil((double)TAM_SUPER_BUFFER_INICIAL/TAM_TRAMA_CODIFICADA))*TAM_TRAMA_CODIFICADA;
	    N_TRAMAS_SUPER_BUFFER=TAM_SUPER_BUFFER/TAM_TRAMA_CODIFICADA;
	    
	    Log.d("tramas","tramas_super"+N_TRAMAS_SUPER_BUFFER);
	    
		
		indicador_recepcion=true;
		
		
		trama_codificada=new byte[TAM_TRAMA_CODIFICADA];
		//buffer=new byte[TAM_BUFFER];
		super_buffer=new byte[TAM_SUPER_BUFFER];
		buffer_a_decodificar=new byte[TAM_BUFFER];
		buffer_a_reproducir=new byte[TAM_BUFFER_AUDIOTRACK];
		
		//Log.d("recepcion","buffer="+buffer+" "+"super_buffer="+super_buffer);
		
		Arrays.fill(super_buffer, (byte) 0);
		
		N_MS_BUFFER=N_MS_TRAMA*N_TRAMAS_BUFFER;
		
	    mAudioTrack = new AudioTrack(
	    		AudioManager.STREAM_VOICE_CALL, 
	    		FREQ,
	    		//AudioFormat.CHANNEL_OUT_STEREO,
	    		AudioFormat.CHANNEL_OUT_MONO,
	    		AudioFormat.ENCODING_PCM_16BIT,
	    		TAM_BUFFER_AUDIOTRACK,
	    		AudioTrack.MODE_STREAM);

	    mAudioTrack.setPlaybackRate(FREQ);
	    
		mAudioTrack.play();
	
		
	}
	

	
	boolean indicador_primer_paquete;
	int n_sec;
	int n_paq;
	int n_nulos=0;
	int n_sec_primer_paq=0;
	
	
	private Runnable mProcesarPaquetes = new Runnable(){
		public void run() {
			
			Log.d("ModuloAlmacDec","finaliza temporizador");
			
			//cambio4
			Log.d("adelantados","adelantados" + n_adelantados+" "+n_recibidos_de_mas+" "+n_sec_inicial);
			
			
			
			//se corrige en función de los que se hayan adelantado en esta ronda al recibir
			n_sec_inicial = n_sec_inicial + N_TRAMAS_BUFFER;
			
			// Ajuste de n_sec_inicial si > 65535
			if (n_sec_inicial > 65535) {
				n_sec_inicial = n_sec_inicial - 65536;
			}
			
			
			if (indicador_recepcion == true) {
	    		boolean exito_handler=mHandler.postDelayed(this,N_MS_BUFFER);
	    		Log.d("handler", "handler"+exito_handler);
	    		
	    	}
			
			/** Adquisición de buffer a decodificar */
			
			buffer_a_decodificar=Arrays.copyOfRange(super_buffer, 0, TAM_BUFFER);
			
						
			//eliminación de primer buffer y adición de buffer de ceros en super_buffer
			super_buffer=Arrays.copyOfRange(super_buffer, TAM_BUFFER, (TAM_SUPER_BUFFER + TAM_BUFFER));
			
			Log.d("prueba","prueba_dec antes");
			
			n_recibidos_de_mas = 0;
			
			
			/** Decodificación */
			if (ilbc_mode==20){
				
			
				mILBCCodecX.decode(buffer_a_decodificar,
						0, 
        				TAM_BUFFER, 
        				buffer_a_reproducir,
        				0);
				
				
			} else if (ilbc_mode==30){
				
				mILBCCodecY.decode(buffer_a_decodificar,
						0, 
        				TAM_BUFFER, 
        				buffer_a_reproducir, 
        				0);
			}
			
			Log.d("prueba","prueba_dec despues");
			
			mAudioTrack.write(
					buffer_a_reproducir, 
					0, 
					TAM_BUFFER_AUDIOTRACK);
			
		
	
		}
	};
	
	
	
		/** Used to receive data from the RTP Library */
		public void receiveData(DataFrame frame, Participant p) {
			/**
			 * This concatenates all received packets for a single timestamp
			 * into a single byte[]
			 */
			
			n_sec_recibido=frame.sequenceNumbers()[0];
			
			if (flag_primer_paquete){ //primer paquete de todos 
				flag_primer_paquete=false;
				n_sec_inicial=frame.sequenceNumbers()[0];
				mHandler.postDelayed(mProcesarPaquetes, N_MS_BUFFER);
			} 
			
			posicion = n_sec_recibido - n_sec_inicial;
			
			Log.d("recibiendo","recibiendo pos="+posicion+"n_s_r"+n_sec_recibido+"n_s_i"+n_sec_inicial);
			
			
			
			
			if (posicion >= N_TRAMAS_BUFFER) {
			
				// en funcion de 16 o el numero que se fije, añadir tantos
				//a n_sec_inicial como corresponda y adelantar super_buffer
				//tanto como corresponda
		
				if ( posicion >= (2*N_TRAMAS_BUFFER) ) {
					n_buffers_a_eliminar = ((int) (posicion / N_TRAMAS_BUFFER)) - 1;
					n_sec_inicial=n_sec_inicial + (n_buffers_a_eliminar * N_TRAMAS_BUFFER);
					
					// Ajuste de n_sec_inicial si > 65535
					if (n_sec_inicial > 65535) {
						n_sec_inicial = n_sec_inicial - 65536;
					}
					
					posicion = n_sec_recibido - n_sec_inicial; //nueva posicion (ya que nuevo n_sec_inicial)
				}
				
				
				Log.d("adelantados","adelantados___" +"  n_rec_de_m "+ n_recibidos_de_mas+" pos "+posicion+" n_sec_inicial "+n_sec_inicial);
			
				
			}
			
			if (posicion >= 0 && posicion < (N_TRAMAS_SUPER_BUFFER-1)){ // se incluye el 0 porque puede tratarse del primer paquete recibido en cada ronda
				
				//trama_codificada=frame.getConcatenatedData();
				trama_codificada=Arrays.copyOfRange(frame.getConcatenatedData(), 0, TAM_TRAMA_CODIFICADA);
				System.arraycopy(
						trama_codificada, 
						0,
						super_buffer, 
						posicion*TAM_TRAMA_CODIFICADA, 
						TAM_TRAMA_CODIFICADA);
			}
			// si (posicion < 0) se descarta el paquete (se considera fuera de tiempo)
			
			
			
			
		
		}
	

	public void stopAudioTrack() {
		indicador_recepcion=false;
		mHandler.removeCallbacks(mProcesarPaquetes);
		mAudioTrack.pause();
		mAudioTrack.flush();
		mAudioTrack.release();
	}
	
	

	/** Used to communicate updates to the user database through RTCP */
	public void userEvent(int type, Participant[] participant) {
		//Do nothing
	}

	/** How many packets make up a complete frame for the payload type? */
	public int frameSize(int payloadType) {
		return 1;
	}
	// RTPAppIntf/
}