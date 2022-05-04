package com.example.phonedroid;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * @author Diego López Herrera
 * 
 * Contiene la definición de los datos. Define cómo es la estructura de
 * LlamadasPerdidas y permite al "Content Provider" (LlamadasPerdidasProvider)
 * acceder a ella correctamente.
 */

 /*
 * Hold the data definition. It will define what a ContactosSIP 
 * data structure looks like and let your Content Provider access
 * it correctly. (Because the provider will be a class that sits in
 * your project, there is no need to build a corresponding .xml
 * layout file)
 */
public class LlamadasPerdidas {
	public static final String AUTHORITY="com.example.phonedroid.provider.LlamadasPerdidasProvider";
	
	public static final String DATABASE_NAME= "llamadaperdida.db";
	public static final int DATABASE_VERSION= 1;
	
	private LlamadasPerdidas() {}
	
	/** 
	 * Esta subclase representa un contacto dentro de la
	 * base de datos ContactosSIP
	 */
	
	 /* 
	 * Name this subclass ContactoSIP because it will represent one contact
	 * from the ContactosSIP dataset
	 */
	//Se define le Schema
	public static final class LlamadaPerdida implements BaseColumns {
		private LlamadaPerdida() {}
		
		/*
		 * Se usa una URI de contenido para identificar el contenido que
		 * se manejará. Este valor debe ser único
		 */
		
		/*
		 * A Content URI is used to identify the content that you will handle.
		 * This value must be unique
		 */
		public static final Uri CONTENT_URI=Uri.parse(
				"content://" + AUTHORITY + "/llamadasperdidas");
		
		public static final String CONTENT_TYPE=
				"vnd.android.cursor.dir/vnd.phonedroid.llamadaperdida";
		
		public static final String CONTENT_ITEM_TYPE=
				"vnd.android.cursor.item/vnd.phonedroid.llamadaperdida";
		
		// Ordena por orden inverso de llegada (las lamadas perdidas
		// más recientes se muestran en primer lugar)
		public static final String DEFAULT_SORT_ORDER="_ID DESC"; 
		
        public static final String TABLE_NAME = "llamadaPerdida";
        public static final String COLUMN_NAME_ENTRY_ID = "entryid";
        public static final String COLUMN_NAME_CUENTASIP = "cuentasip";
        public static final String COLUMN_NAME_TYPEOFCALL = "typeofcall";
        public static final String COLUMN_NAME_TIME = "time";
        
        //private LlamadasPerdidasContract () {}
	}
}
	
