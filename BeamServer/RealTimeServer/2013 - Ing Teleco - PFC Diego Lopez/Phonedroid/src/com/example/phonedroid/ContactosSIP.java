package com.example.phonedroid;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * @author Diego López Herrera
 * 
 * Contiene la definición de los datos. Define cómo es la estructura de
 * ContactosSIP y permite al "Content Provider" (ContactosSIPProvider)
 * acceder a ella correctamente.
 */

 /**
 * 
 * Hold the data definition. It will define what a ContactosSIP 
 * data structure looks like and let your Content Provider access
 * it correctly. (Because the provider will be a class that sits in
 * your project, there is no need to build a corresponding .xml
 * layout file)
 */
public class ContactosSIP {
	public static final String AUTHORITY="com.example.phonedroid.provider.ContactosSIPProvider";
	
	public static final String DATABASE_NAME= "contactosip.db";
	public static final int DATABASE_VERSION= 1;
	//public static final String CONTACTOS_TABLE_NAME="contactossip";
	
	private ContactosSIP() {}
	
	/** 
	 * Esta subclase representa un contacto dentro de la
	 * base de datos ContactosSIP
	 */
	
	 /* 
	 * Name this subclass ContactoSIP because it will represent one contact
	 * from the ContactosSIP dataset
	 */
	//Se define el Schema
	public static final class ContactoSIP implements BaseColumns {
		private ContactoSIP() {}
		
		/*
		 * Se usa una URI de contenido para identificar el contenido que
		 * se manejará. Este valor debe ser único
		 */
		
		/*
		 * A Content URI is used to identify the content that you will handle.
		 * This value must be unique
		 */
		public static final Uri CONTENT_URI=Uri.parse(
				"content://" + AUTHORITY + "/contactossip");
		
		public static final String CONTENT_TYPE=
				"vnd.android.cursor.dir/vnd.phonedroid.contactosip";
		
		public static final String CONTENT_ITEM_TYPE=
				"vnd.android.cursor.item/vnd.phonedroid.contactosip";
		
		//ordena por orden alfabético ascendente  y no es sensible a mayúsculas
		public static final String DEFAULT_SORT_ORDER="nickname COLLATE NOCASE ASC"; 
		
        public static final String TABLE_NAME = "contactoSIP";
        public static final String COLUMN_NAME_ENTRY_ID = "entryid";
        public static final String COLUMN_NAME_NICKNAME = "nickname";
        public static final String COLUMN_NAME_CUENTASIP = "cuentasip";
        
        //private ContactoSIPContract () {}
	}
}
	
