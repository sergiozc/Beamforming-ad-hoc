package com.example.phonedroid;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

//import com.example.phonedroid.ContactosSIP.ContactoSIP;
import com.example.phonedroid.LlamadasPerdidas.LlamadaPerdida;

/**
 * @author Diego López Herrera
 * 
 */

public class LlamadasPerdidasProvider extends ContentProvider {
	
	//private SQLiteDatabase mDB;
	private static final String TAG = "LlamadasPerdidasProvider";
	
	//setup projection Map
	/*
	 * Configuración del "projection Map"
	 * 
	 * Los mapas de proyección son similares a "as" (la columna
	 * alias) contruyendo una sentencia sql donde se pueden
	 * renombrar las columnas
	 */
	
	//Projection maps are similar to "as" (column alias) construct
	//in an sql statement where by you can rename the columns
	private static HashMap<String,String> sContactProjectionMap;
	static
	{
		sContactProjectionMap = new HashMap<String,String>();
		sContactProjectionMap.put(LlamadaPerdida._ID, LlamadaPerdida._ID);
		sContactProjectionMap.put(LlamadaPerdida.COLUMN_NAME_ENTRY_ID, LlamadaPerdida.COLUMN_NAME_ENTRY_ID);
		sContactProjectionMap.put(LlamadaPerdida.COLUMN_NAME_CUENTASIP, LlamadaPerdida.COLUMN_NAME_CUENTASIP);
		sContactProjectionMap.put(LlamadaPerdida.COLUMN_NAME_TYPEOFCALL, LlamadaPerdida.COLUMN_NAME_TYPEOFCALL);
		sContactProjectionMap.put(LlamadaPerdida.COLUMN_NAME_TIME, LlamadaPerdida.COLUMN_NAME_TIME);
	}
	
	//setup URIs
	//Provides a mechanism to identify all the incoming uri patterns
	/*
	 * Configuración de las URIs
	 * 
	 * Ofrece un mecanismo para identificar los tipos uris
	 * que se pueden recibir
	 */
	private static final UriMatcher sUriMatcher;
	private static final int INCOMING_CONTACT_COLLECTION_URI_INDICATOR=1;
	private static final int INCOMING_SINGLE_CONTACT_URI_INDICATOR=2;
	static{
		sUriMatcher=new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(LlamadasPerdidas.AUTHORITY, "llamadasperdidas", 
				INCOMING_CONTACT_COLLECTION_URI_INDICATOR);
		sUriMatcher.addURI(LlamadasPerdidas.AUTHORITY, "llamadasperdidas/#", 
				INCOMING_SINGLE_CONTACT_URI_INDICATOR);
	}
	
	/**
	 * Configuración/Creación de la base de datos
	 * 
	 *  Esta clase se encarga de abrir, crear y actualizar el fichero
	 *  asociado a la base de datos
	 */
	
	/**
	 * Setup/Create Database
	 * This class helps open, create, and upgrade the database file
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		
		DatabaseHelper(Context context) {
			super(context,LlamadasPerdidas.DATABASE_NAME, null, LlamadasPerdidas.DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db){
			Log.d(TAG, "inner oncreate called");
			db.execSQL("CREATE TABLE "+ LlamadaPerdida.TABLE_NAME + " ("
					+ LlamadaPerdida._ID + " INTEGER PRIMARY KEY,"
					+ LlamadaPerdida.COLUMN_NAME_ENTRY_ID + " TEXT,"
					+ LlamadaPerdida.COLUMN_NAME_CUENTASIP + " TEXT,"
					+ LlamadaPerdida.COLUMN_NAME_TYPEOFCALL + " TEXT,"
					+ LlamadaPerdida.COLUMN_NAME_TIME + " TEXT"
					+");");
		}
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
			Log.d(TAG,"inner onupgrade called");
			Log.w(TAG,"Upgrading database from version "
					+oldVersion + " to"
					+newVersion +
					", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " +
					LlamadaPerdida.TABLE_NAME);
			onCreate(db);
		}
	}
	
	private DatabaseHelper mOpenHelper;
	
	//Component creation callback
	@Override
	public boolean onCreate(){
		Log.d(TAG,"main onCreate called");
		mOpenHelper=new DatabaseHelper(getContext());
		return true;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)  {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		switch (sUriMatcher.match(uri)) {
		case INCOMING_CONTACT_COLLECTION_URI_INDICATOR:
			qb.setTables(LlamadaPerdida.TABLE_NAME);
			qb.setProjectionMap(sContactProjectionMap);
			break;
			
		case INCOMING_SINGLE_CONTACT_URI_INDICATOR:
			qb.setTables(LlamadaPerdida.TABLE_NAME);
			qb.setProjectionMap(sContactProjectionMap);
			qb.appendWhere(LlamadaPerdida._ID + "="
					+ uri.getPathSegments().get(1));
			break;
			
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		
		}
		
		//If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy=LlamadaPerdida.DEFAULT_SORT_ORDER;
		}else { orderBy = sortOrder; }
		
		//Get the database and run the query
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c= qb.query(db, projection, selection, selectionArgs,
				null, null, orderBy);
		
		//example of getting a count
		int i=c.getCount();
		
		//Tell the cursor what uri to watch, so it knows when its source data changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		
		return c;
	}
	
	@Override
	public String getType(Uri uri){
		switch(sUriMatcher.match(uri)) {
		case INCOMING_CONTACT_COLLECTION_URI_INDICATOR:
			return LlamadaPerdida.CONTENT_TYPE;
			
		case INCOMING_SINGLE_CONTACT_URI_INDICATOR:
			return LlamadaPerdida.CONTENT_ITEM_TYPE;
			
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}
	
	@Override
	public Uri insert (Uri uri, ContentValues initialValues)
	{
		//Validate the requested uri
		if (sUriMatcher.match(uri) != INCOMING_CONTACT_COLLECTION_URI_INDICATOR)
		{
			throw new IllegalArgumentException("Unknown URI " + uri); 
		}
		
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		}else {
			values = new ContentValues();
		}
		
	
		
		//Make sure that the fields are all set
		
		
		if (values.containsKey(LlamadaPerdida.COLUMN_NAME_CUENTASIP) == false)
		{
			throw new SQLException("Failed to insert row because Cuenta SIP is needed " + uri);
		}
		
		if (values.containsKey(LlamadaPerdida.COLUMN_NAME_TYPEOFCALL) == false)
		{
			throw new SQLException("Failed to insert row because Nickname is needed " + uri);
		}
		
		if (values.containsKey(LlamadaPerdida.COLUMN_NAME_TIME) == false)
		{
			throw new SQLException("Failed to insert row because Nickname is needed " + uri);
		}
		
		SQLiteDatabase db=mOpenHelper.getWritableDatabase();
		long rowId=db.insert(LlamadaPerdida.TABLE_NAME, 
				LlamadaPerdida.COLUMN_NAME_CUENTASIP, values);
		
		if (rowId > 0) {
			Uri insertedContactUri=ContentUris.withAppendedId(
					LlamadaPerdida.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(insertedContactUri, null);
			
			return insertedContactUri;
		}
		
		throw new SQLException("Failed to insert row into " + uri);
	}
	
	@Override
	public int delete(Uri uri, String where, String[] whereArgs) 
	{
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case INCOMING_CONTACT_COLLECTION_URI_INDICATOR:
			count=db.delete(LlamadaPerdida.TABLE_NAME, where, whereArgs);
			break;
		
		case INCOMING_SINGLE_CONTACT_URI_INDICATOR:
			String rowId = uri.getPathSegments().get(1);
			count = db.delete(
					LlamadaPerdida.TABLE_NAME,
					LlamadaPerdida._ID + "=" + rowId
					+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs
					);
			break;
			
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
		
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs)
	{
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case INCOMING_CONTACT_COLLECTION_URI_INDICATOR : 
			count = db.update(LlamadaPerdida.TABLE_NAME, values, where, whereArgs);
			break;
		case INCOMING_SINGLE_CONTACT_URI_INDICATOR :
			String rowId = uri.getPathSegments().get(1);
			count = db.update(LlamadaPerdida.TABLE_NAME, 
					values, 
					LlamadaPerdida._ID + "=" + rowId
					+ (!TextUtils.isEmpty(where) ? " AND (" + where+ ')' : "") , 
					whereArgs
					);
			break;
			
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
		
}
