package com.example.phonedroid;


import android.app.ActionBar.LayoutParams;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;

/**
 * @author Diego López Herrera
 * 
 */

public class ActividadLlamadasPerdidas extends ListActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
	
	/** CONSTANTES */
	
	// These are the Contacts rows that we will retrieve
    static final String[] PROJECTION = new String[] {
    	LlamadasPerdidas.LlamadaPerdida._ID ,
    	LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_ENTRY_ID ,
    	LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_CUENTASIP ,
    	LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_TYPEOFCALL ,
    	LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_TIME
    	};

    // This is the select criteria
    static final String SELECTION = "((" + 
            LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_CUENTASIP + " NOTNULL) AND (" +
            LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_CUENTASIP + " != '' ) AND (" +
            LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_TYPEOFCALL + " NOTNULL) AND (" +
            LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_TYPEOFCALL + " != '' ) AND (" +
            LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_TIME + " NOTNULL) AND (" +
            LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_TIME + " != '' ))";
	
	/** VARIABLES */
	ProgressBar progressBar;
	// This is the Adapter being used to display the list's data
    SimpleCursorAdapter mAdapter;

    
    /** MÉTODOS */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.llamadas_perdidas);
        
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
        
        String[] fromColumns = {LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_CUENTASIP, 
        		//LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_TYPEOFCALL,
        		LlamadasPerdidas.LlamadaPerdida.COLUMN_NAME_TIME};
        
        int[] toViews = {android.R.id.text1, android.R.id.text2}; // The TextView in simple_list_item_1

        // Create an empty adapter we will use to display the loaded data.
        // We pass null for the cursor, then update it in onLoadFinished()
        mAdapter = new SimpleCursorAdapter(this, 
                android.R.layout.simple_list_item_2, 
                null,
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
        return new CursorLoader(this, LlamadasPerdidas.LlamadaPerdida.CONTENT_URI,
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
    
    public void clickLimpiarLista(View view){
    	ContentResolver cr = this.getContentResolver();
    	Uri uri = LlamadasPerdidas.LlamadaPerdida.CONTENT_URI;
    	//ELIMINAR TODAS LAS LLAMADAS
    	cr.delete(uri, null, null);
    }
    
 // En principio no se ofrecen opciones cuando se clique en algún elemento de la lista 
    /*
    @Override 
    public void onListItemClick(ListView l, View v, int position, long id) {
        
    }
    */
    
}