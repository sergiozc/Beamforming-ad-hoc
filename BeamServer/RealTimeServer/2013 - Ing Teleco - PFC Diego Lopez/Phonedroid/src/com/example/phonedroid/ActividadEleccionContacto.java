package com.example.phonedroid;


import android.app.ActionBar.LayoutParams;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentResolver;
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
import android.widget.SimpleCursorAdapter;

/**
 * @author Diego López Herrera
 * 
 */

public class ActividadEleccionContacto extends ListActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
	
	/** CONSTANTES */
	public final static String EXTRA_MESSAGE_CUENTA_SIP = "com.example.phonedroid.MESSAGE";
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
	ProgressBar progressBar;
	// This is the Adapter being used to display the list's data
    SimpleCursorAdapter mAdapter;

   
    /** MÉTODOS */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.eleccion_contacto);
        
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
	    Uri queryUri = Uri.withAppendedPath(uri, Long.toString(id));
	    
	    String[] projection = {
	   	     ContactosSIP.ContactoSIP.COLUMN_NAME_CUENTASIP
	   	     };
	    
	    Cursor cursor;
	    cursor=cr.query(queryUri, projection, null, null, null);
	    
	    cursor.moveToFirst();
	    String cuentaSIP=cursor.getString(0);
	    
	    Intent intent = new Intent(this, this.getCallingActivity().getClass());
	    intent.putExtra(EXTRA_MESSAGE_CUENTA_SIP, cuentaSIP);
	    setResult(RESULT_OK,intent);
	    finish();
    }
    
}