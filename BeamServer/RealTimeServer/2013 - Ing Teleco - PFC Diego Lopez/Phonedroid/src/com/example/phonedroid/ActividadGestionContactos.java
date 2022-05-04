package com.example.phonedroid;


import android.app.ActionBar.LayoutParams;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

/**
 * @author Diego López Herrera
 * 
 */

public class ActividadGestionContactos extends ListActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
	
	/** CONSTANTES */
	
	public final static String EXTRA_MESSAGE_NICKNAME = "com.example.phonedroid.MESSAGE_NICKNAME";
	public final static String EXTRA_MESSAGE_CUENTASIP = "com.example.phonedroid.MESSAGE_CUENTASIP";
	
	public final int REQ_CODE_EDITAR=2;
	public final int REQ_CODE_AGREGAR=3;
	
	// These are the Contacts rows that we will retrieve
    static final String[] PROJECTION = new String[] {
    	ContactosSIP.ContactoSIP._ID ,
    	ContactosSIP.ContactoSIP.COLUMN_NAME_ENTRY_ID ,
    	ContactosSIP.ContactoSIP.COLUMN_NAME_NICKNAME ,
    	ContactosSIP.ContactoSIP.COLUMN_NAME_CUENTASIP
    	};

    // This is the select criteria
    static final String SELECTION = "((" + 
            ContactosSIP.ContactoSIP.COLUMN_NAME_NICKNAME + " NOTNULL) AND (" +
            ContactosSIP.ContactoSIP.COLUMN_NAME_NICKNAME + " != '' ) AND (" +
        	ContactosSIP.ContactoSIP.COLUMN_NAME_CUENTASIP + " NOTNULL) AND (" +
        	ContactosSIP.ContactoSIP.COLUMN_NAME_CUENTASIP + " != '' ))";
	
	/** VARIABLES */
	RadioButton rb_eliminar;
	RadioButton rb_editar;
	
	ProgressBar progressBar;
	
	String id_editar=null;
	
	// This is the Adapter being used to display the list's data
    SimpleCursorAdapter mAdapter;

    
    /** MÉTODOS */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.gestion_contactos);
        
        rb_eliminar = (RadioButton) findViewById(R.id.radiobutton_eliminar_contacto);
        rb_editar = (RadioButton) findViewById(R.id.radiobutton_editar_contacto);
    	
        // Create a progress bar to display while the list loads
        progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        progressBar.setIndeterminate(true);
        getListView().setEmptyView(progressBar);
        

        // Must add the progress bar to the root of the layout
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        root.addView(progressBar);

        // For the cursor adapter, specify which columns go into which views
        
        String[] fromColumns = {ContactosSIP.ContactoSIP.COLUMN_NAME_NICKNAME, 
        		ContactosSIP.ContactoSIP.COLUMN_NAME_CUENTASIP};
        
        int[] toViews = {android.R.id.text1,android.R.id.text2}; // The TextView in simple_list_item_1

        // Create an empty adapter we will use to display the loaded data.
        // We pass null for the cursor, then update it in onLoadFinished()
        mAdapter = new SimpleCursorAdapter(this, 
                android.R.layout.simple_list_item_2, null,
                fromColumns, toViews, 0);
        setListAdapter(mAdapter);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    // Called when a new Loader needs to be created
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, ContactosSIP.ContactoSIP.CONTENT_URI,
                PROJECTION, SELECTION, null, null); //poner a null SELECTION si da problemas
    }

    // Called when a previously created loader has finished loading
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
        progressBar.setVisibility(View.INVISIBLE);
    }

    // Called when a previously created loader is reset, making the data unavailable
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }
    
    @Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Do something when a list item is clicked
    	
	    ContentResolver cr = this.getContentResolver();
	    Uri uri = ContactosSIP.ContactoSIP.CONTENT_URI;
	    
	    if (!rb_eliminar.isChecked() && !rb_editar.isChecked()) {
	    	Context context = getApplicationContext();
        	Toast toast_aviso_campo_nulo = Toast.makeText(context, 
	    			getString(R.string.aviso_ninguna_opcion_elegida), Toast.LENGTH_SHORT);
        	toast_aviso_campo_nulo.show();
	    } else {
	    	//ELIMINAR CONTACTO
		    if (rb_eliminar.isChecked()) {
		    	//DELETE INFO FROM DATABASE
		    	Uri deleteUri = Uri.withAppendedPath(uri, Long.toString(id));
			    cr.delete(deleteUri, null, null);
		    }
		    
		    //EDITAR CONTACTO
		    else if (rb_editar.isChecked()){
		    	//UPDATE INFO FROM DATABASE
		    	id_editar=Long.toString(id);
		    	
		    	Uri queryUri = Uri.withAppendedPath(uri, id_editar);
			    
			    String[] projection = {
			   	     ContactosSIP.ContactoSIP.COLUMN_NAME_NICKNAME,
			   	     ContactosSIP.ContactoSIP.COLUMN_NAME_CUENTASIP
			   	     };
			    Cursor cursor;
			    cursor=cr.query(queryUri, projection, null, null, null);
			    
			    cursor.moveToFirst();
			    String nickname=cursor.getString(0);
			    String cuentasip=cursor.getString(1);
			    
			    Intent intent = new Intent(this, ActividadNuevoContactoSIP.class);
			    intent.putExtra(EXTRA_MESSAGE_NICKNAME, nickname);
			    intent.putExtra(EXTRA_MESSAGE_CUENTASIP, cuentasip);
			    
			    startActivityForResult(intent,REQ_CODE_EDITAR);
			        
		    }
	    }
    }
    
    public void clickSIPAgregarContacto (View view) {
    	Intent intent = new Intent(this, ActividadNuevoContactoSIP.class);
    	startActivityForResult(intent,REQ_CODE_AGREGAR);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data){
    	//CASO EDITAR
    	if (requestCode==REQ_CODE_EDITAR && resultCode==RESULT_OK) {
    		String nickname=data.getStringExtra(ActividadNuevoContactoSIP.EXTRA_MESSAGE_NICKNAME_EDITADO);
    		String cuentasip=data.getStringExtra(ActividadNuevoContactoSIP.EXTRA_MESSAGE_CUENTASIP_EDITADO);
    		
		    ContentResolver cr = this.getContentResolver();
		    Uri uri = ContactosSIP.ContactoSIP.CONTENT_URI;
		    Uri updUri = Uri.withAppendedPath(uri, id_editar);
		    
		    ContentValues cv = new ContentValues();
	        cv.put(ContactosSIP.ContactoSIP.COLUMN_NAME_NICKNAME, nickname);
		    cv.put(ContactosSIP.ContactoSIP.COLUMN_NAME_CUENTASIP, cuentasip);
		    cr.update(updUri, cv, null, null);
		    
    	}
    	
    	//CASO AGREGAR 
    	 else if (requestCode == REQ_CODE_AGREGAR && resultCode == RESULT_OK  ) {
    		 
    		 String nickname=data.getStringExtra(ActividadNuevoContactoSIP.EXTRA_MESSAGE_NICKNAME_EDITADO);
     		 String cuentasip=data.getStringExtra(ActividadNuevoContactoSIP.EXTRA_MESSAGE_CUENTASIP_EDITADO);
     		    
     		 ContentResolver cr = this.getContentResolver();
     		 Uri uri = ContactosSIP.ContactoSIP.CONTENT_URI;
     		    
     		 ContentValues cv = new ContentValues();
     	     cv.put(ContactosSIP.ContactoSIP.COLUMN_NAME_NICKNAME, nickname);
     		 cv.put(ContactosSIP.ContactoSIP.COLUMN_NAME_CUENTASIP, cuentasip);
     	    
     		 cr.insert(uri, cv);
     		
    	 }   
   }

}