package com.example.tosh.recorder;

import android.app.ProgressDialog;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.os.Environment;
import android.widget.TextView;

import java.io.*;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    private File noisy, clean;
    private short[] input, out, output, aux, spectralsubtraction, shortArr, output_frame, buffer1, buffer2, buffer3, buffer4;
    final int fs = 8000;
    private RecordAudio recordTask;
    private PlayAudio playTask;
    private Thread recordPlayThread, recordProcessThread;
    private static AudioRecord record = null;
    final int source = MediaRecorder.AudioSource.MIC;
    final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    final int channelOut = AudioFormat.CHANNEL_OUT_MONO;
    final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private boolean recording, playing;
    private ProgressDialog dialog;
    private TextView statusText;
    private static SpectralSubtraction ss;
    private double noiseAverage;
    private double[] noise, signal;
    private boolean stop = true;
    private boolean isStart = false;
    private boolean lastIsOne = false;
    private boolean lastIsTwo = false;
    private boolean isOne = false;
    private boolean isTwo = false;
    private boolean isThree = false;
    private static FileOutputStream os = null;
    private static FileOutputStream os1 = null;
    private int loop = 0;
    private static final String AUDIO_FOLDER = "SuperRecorder";
    private static String AUDIO_TEMPFILE = "record_temp";
    private double recordTime = 0.0;
    private double realTime = 0.0;
    private static String fullTmpFilename;
    private static String originalFullTmpFilename;
    private String recFileName;
    private String fileName;
    private static int minBufSize;
    private AudioManager am = null;
    AudioTrack at = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setButtonHandlers();
        enableButtons(false);
        File path = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/");
        path.mkdir();
        statusText = (TextView) this.findViewById(R.id.textView);
        statusText.setText(getResources().getString(R.string.preparado));


        try {
            am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.setMode(AudioManager.STREAM_VOICE_CALL);
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);


            minBufSize = AudioRecord.getMinBufferSize(fs,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);


            //Log.e("ss", String.valueOf(minBufSize));
            //minBufSize=2048;


            record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    fs,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufSize);
            prepareTempFile();
            fullTmpFilename=getTempFilename();
            originalFullTmpFilename = getOriginalTempFilename();
            final File recordFile = configRecFileName(".wav");
            final File originalFile = configRecFileName("_original.wav");
            recFileName = recordFile.getAbsolutePath();
            fileName = originalFile.getAbsolutePath();
            minBufSize = AudioRecord.getMinBufferSize(fs,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            noisy = File.createTempFile("noisy", ".pcm", path);
            clean = File.createTempFile("clean", ".pcm", path);
            int minBufSize = AudioRecord.getMinBufferSize(fs,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create file on SD card", e);

        }

    }

    private void setButtonHandlers() {
        findViewById(R.id.buttonStart).setOnClickListener(btnClick);
        findViewById(R.id.buttonStop).setOnClickListener(btnClick);
        findViewById(R.id.buttonPlay).setOnClickListener(btnClick);
        findViewById(R.id.buttonPlay2).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        findViewById(id).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.buttonStart, !isRecording);
        enableButton(R.id.buttonStop, isRecording);
        enableButton(R.id.buttonPlay, !isRecording);
        enableButton(R.id.buttonPlay2, !isRecording);
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.buttonStart: {
                    enableButtons(true);
                    statusText.setText(getResources().getString(R.string.grabando));
                    if (stop) {
                        stop = false;                                    // Avoid user pressing the Play key multiple times very quickly
                        try {
//                            os = new FileOutputStream(clean); //LA PUTA CLAVE
//                            os1 = new FileOutputStream(noisy);
                            os = new FileOutputStream(fullTmpFilename);
                            os1 = new FileOutputStream(originalFullTmpFilename);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        //curDate = new Date(System.currentTimeMillis());
                        //beginTime = curDate.getTime();
                        //recordTime = 0.0;

                        //thread for recording
                        recordPlayThread = new Thread() {
                            @Override
                            public void run() {
                                recordAndPlay();
                            }
                        };
                        recordPlayThread.start();

                        //thread for processing
                        recordProcessThread = new Thread() {
                            @Override
                            public void run() {
                                recordAndProcess();
                            }

                        };
                        recordProcessThread.start();

                    }


                    break;
                }
                case R.id.buttonStop: {
                    enableButtons(false);
                    statusText.setText(getResources().getString(R.string.preparado));
                    recording = false;
                    playing = false;
                    stopRecordPlay();
                    dialog = new ProgressDialog(MainActivity.this);
                    dialog.setTitle("Por favor espere");
                    dialog.setMessage(getResources().getString(R.string.filtrando));

                    //dialog.show();
                    break;
                }
                case R.id.buttonPlay: {
                    enableButtons(true);
                    statusText.setText(getResources().getString(R.string.reproduciendo));
                    playing = true;
                    try {
                        PlayAudioTrack(fullTmpFilename, originalFullTmpFilename);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    enableButtons(false);
                    statusText.setText(getResources().getString(R.string.preparado));
                    //playTask = new PlayAudio();
                    //playTask.execute(noisy);

                    break;
                }
                case R.id.buttonPlay2: {
                    enableButtons(true);
                    statusText.setText(getResources().getString(R.string.reproduciendo));
                    playing = true;
                    try {
                        PlayAudioTrack(originalFullTmpFilename, fullTmpFilename);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    enableButtons(false);
                    statusText.setText(getResources().getString(R.string.preparado));
                    //playTask = new PlayAudio();
                    //playTask.execute(noisy);

                    break;
                }

            }

        }


    };

    private void recordAndPlay() {
        int bufferSize = AudioRecord.getMinBufferSize(fs, channelConfiguration, audioEncoding);
        int bufSize = bufferSize / 2;
        buffer1 = new short[bufSize];
        buffer2 = new short[bufSize];
        buffer3 = new short[bufSize];
        int num = 0;

        record.startRecording();
        isOne = true;
        isTwo = false;
        isStart = true;
        while (stop == false) {
            if (isOne) {

                num = record.read(buffer1, 0, bufSize);
                isOne = false;
                isTwo = true;

            } else if (isTwo) {

                num = record.read(buffer2, 0, bufSize);
                isTwo = false;
                isOne = true;
            }
        }
    }

    private void recordAndProcess() {
        //Wave.deleteTempFile(fullTmpFilename);
        //Wave.deleteTempFile(originalFullTmpFilename);// Delete the temp file
        int bufferSize = AudioRecord.getMinBufferSize(fs, channelConfiguration, audioEncoding);
        int bufSize = bufferSize / 2;
        shortArr = new short[bufferSize];
        output_frame = new short[160];
        buffer3 = new short[bufSize / 2];
        buffer4 = new short[bufSize / 2];
        noise = new double[bufSize * 8];
        spectralsubtraction = new short[bufSize * 8];
        signal = new double[bufSize];
        lastIsOne = false;
        lastIsTwo = true;

        while (stop == false) {
            while (isStart) {
                if (isTwo && lastIsOne) {

                    Wave.writeAudioBufToFile(originalFullTmpFilename, buffer1, os1);

                    if (loop < 7) {
                        for (int i = 0; i < bufSize; i++) {
                            signal[i] = (double) buffer1[i];
                            signal[i] = signal[i] / 32768.0;
                        }
                        System.arraycopy(signal, 0, noise, loop * bufSize, bufSize);
                        System.arraycopy(buffer1, 0, spectralsubtraction, loop * bufSize, bufSize);
                        buffer1 = new short[bufSize];
                    } else if (loop == 7) {
                        System.arraycopy(signal, 0, noise, loop * bufSize, bufSize);
                        System.arraycopy(buffer1, 0, spectralsubtraction, loop * bufSize, bufSize);
                        ss = new SpectralSubtraction(spectralsubtraction, bufSize);
                        noiseAverage = ss.noiseAverage(noise);
                        buffer1 = new short[bufSize];
                    } else {
                        //Log.e("ss","start to modify");
                        System.arraycopy(buffer3, 0, shortArr, 0, bufSize / 2);
                        System.arraycopy(buffer1, 0, shortArr, bufSize / 2, bufSize);
                        System.arraycopy(buffer3, 0, shortArr, bufSize / 2 + bufSize, bufSize / 2);
                        ss.setSignal(shortArr);
                        System.arraycopy(ss.noiseSubtraction(noiseAverage), bufSize / 2, buffer1, 0, bufSize);

                    }
                    Wave.writeAudioBufToFile(fullTmpFilename, buffer1, os);
                    lastIsOne = false;
                    lastIsTwo = true;
                    realTime += (double) (bufSize) / (double) fs;
                    loop++;
                } else if(isOne&&lastIsTwo){

                    Wave.writeAudioBufToFile(originalFullTmpFilename, buffer2, os1);

                    if(loop<7){
                        for(int i=0;i<bufSize;i++){
                            signal[i]=(double) buffer2[i];
                            signal[i]= signal[i]/32768.0;
                        }
                        System.arraycopy(signal,0,noise,loop*bufSize,bufSize);
                        System.arraycopy(buffer2,0,spectralsubtraction,loop*bufSize,bufSize);
                        buffer2 = new short[bufSize];
                    }else if(loop==7){
                        System.arraycopy(signal,0,noise,loop*bufSize,bufSize);
                        System.arraycopy(buffer2,0,spectralsubtraction,loop*bufSize,bufSize);
                        ss = new SpectralSubtraction(spectralsubtraction,bufSize);
                        noiseAverage = ss.noiseAverage(noise);
                        buffer2 = new short[bufSize];
                    }
                    else{
                        //Log.e("ss","start to modify");
                        System.arraycopy(buffer3,0,shortArr,0,bufSize/2);
                        System.arraycopy(buffer2,0,shortArr,bufSize/2,bufSize);
                        System.arraycopy(buffer3, 0, shortArr, bufSize/2+bufSize, bufSize/2);
                        ss.setSignal(shortArr);
                        System.arraycopy(ss.noiseSubtraction(noiseAverage),bufSize/2,buffer2,0,bufSize);
                    }

                    //transform data from short to byte and save to temp file
                    Wave.writeAudioBufToFile(fullTmpFilename, buffer2, os);



                    //Log.e("ss", "write2");
                    lastIsTwo=false;
                    lastIsOne = true;

                    realTime +=(double)(bufSize)/(double)fs;


                    loop++;

                }
            }
        }
    }
    public void stopRecordPlay() {
        stop = true;
        isStart = false;
        record.stop();
        //am.stopBluetoothSco();
        //am.setBluetoothScoOn(false);
        recordPlayThread = null;
        recordProcessThread = null;
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        os = null;
        os1=null;
        String fullname = getFullname();
        String originalFullname = getOriginalFullname();
        //Log.e("save",fullname);
        //System.out.println("Debugging: " + fullTmpFilename + " to " + fullname + ".wav");
        Wave.copyTmpfileToWavfile(fullTmpFilename, fullname, (long) fs, minBufSize);
        Wave.copyTmpfileToWavfile(originalFullTmpFilename, originalFullname, (long) fs, minBufSize);
        //Log.e("save", fullTmpFilename);
        //Wave.deleteTempFile(fullTmpFilename);
        //Wave.deleteTempFile(originalFullTmpFilename);// Delete the temp file
    }

    private class RecordAudio extends AsyncTask<Void, Integer, Void> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            /*int bufferSize = AudioRecord.getMinBufferSize(fs, channelConfiguration, audioEncoding);
            short[] buffer=new short[bufferSize];
            try{
                DataOutputStream oStream= new DataOutputStream(new BufferedOutputStream(new FileOutputStream(noisy)));
                AudioRecord audioRecord=new AudioRecord(source,fs,channelConfiguration,audioEncoding,bufferSize);
                audioRecord.startRecording();
                while (recording){
                    int bufferRead=audioRecord.read(buffer, 0, bufferSize);
                    for (int i = 0; i < bufferRead; i++) {
                        oStream.writeShort(buffer[i]);
                    }
                }
                audioRecord.stop();
                audioRecord.release();
                oStream.close();
            }
            catch (Throwable e) {
                e.printStackTrace();;
            }*/
            int bufferSize = AudioRecord.getMinBufferSize(fs, channelConfiguration, audioEncoding);
            int bufSize = bufferSize / 2;
            buffer1 = new short[bufSize];
            buffer2 = new short[bufSize];
            buffer3 = new short[bufSize];
            int num = 0;

            record.startRecording();
            isOne = true;
            isTwo = false;
            isStart = true;
            while (stop == false) {
                if (isOne) {

                    num = record.read(buffer1, 0, bufSize);
                    isOne = false;
                    isTwo = true;

                } else if (isTwo) {

                    num = record.read(buffer2, 0, bufSize);
                    isTwo = false;
                    isOne = true;
                }


            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            enableButtons(false);
            //output = segmentar(200, .4);
            //salida();
            DataOutputStream oStream = null;
            try {
                oStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(clean)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                for (int i = 0; i < output.length - 1; i++) {
                    oStream.writeShort(output[i]);
                }
                oStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //WS Ws = new WS();
            //Ws.preproceso(output);
        }
    }

    private class PlayAudio extends AsyncTask<File, Integer, Void> {
        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(File... fileObj) {
            int bufferSize = AudioTrack.getMinBufferSize(fs, channelOut, audioEncoding);
            short[] audioFragment = new short[bufferSize];
            try {
                //DataInputStream iStream = new DataInputStream(new BufferedInputStream(new FileInputStream(fileObj[0])));
                DataInputStream iStream = new DataInputStream(new BufferedInputStream(new FileInputStream(fileObj[0])));
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, fs, channelOut,
                        audioEncoding, bufferSize, AudioTrack.MODE_STREAM);
                audioTrack.play();
                while (playing && iStream.available() > 0) {
                    int i = 0;
                    while (iStream.available() > 0 && i < audioFragment.length) {
                        audioFragment[i] = iStream.readShort();
                        i++;
                    }
                    audioTrack.write(audioFragment, 0, audioFragment.length);
                }

                audioTrack.stop();
                audioTrack.release();
                iStream.close();
                statusText.setText(getResources().getString(R.string.preparado));
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            enableButtons(false);

        }
    }

private short[] salida(int W, double SP){


    DataInputStream inStream = null;
    try {
        inStream = new DataInputStream(new BufferedInputStream(new FileInputStream(fullTmpFilename)));


    } catch (FileNotFoundException e) {

    }
    try {

        int L = inStream.available();
        int N = (int) ((L - W) / (SP * W) + 1);
        input = new short[400];
        out = new short[N * W];
        boolean continuar = true;
        int k = 0;
        int ntrama = 0;
        while (inStream.available() > 0 && continuar) {
            input[k] = inStream.readShort();
            k = k + 1;
            if (k == 400) {
                k = 0;
                if (inStream.available() < 400) {
                    aux = input;//Aquí va Antonio, s.processing(input)
                    continuar = false;
                } else {

                    aux = input;
                }
                ntrama = ntrama + 5;
            }
        }
        inStream.close();


    } catch (IOException e) {

    }
    return aux;
}
    public short[] segmentar(int W, double SP) {


        // Parte la señal para solapar los segmentos enventanados.
        // Devuelve una matriz cuyas columnas son tramas segmentadas y enventanadas
        // de la señal de entrada

        DataInputStream inStream = null;
        try {
            inStream = new DataInputStream(new BufferedInputStream(new FileInputStream(noisy)));
        } catch (FileNotFoundException e) {

        }
        try {
            int L = inStream.available();
            int N = (int) ((L - W) / (SP * W) + 1);
            input = new short[400];
            out = new short[N * W];
            spectralsubtraction = new short[N * W];
            boolean continuar = true;
            int k = 0;
            int ntrama = 0;
            while (inStream.available() > 0 && continuar) {
                input[k] = inStream.readShort();
                k = k + 1;
                if (k == 400) {
                    k = 0;
                    if (inStream.available() < 400) {
                        aux = input;//Aquí va Antonio, s.processing(input)
                        continuar = false;
                    } else {

                        aux = input;
                    }
                    /*if(ntrama==0) {

                        System.arraycopy(aux, 0, out, ntrama * 200, W); //aOrign,aInicioOrigen,aDestino,InicioaDestino,nElementosACopiar
                        System.arraycopy(aux, 80, out, (ntrama + 1) * 200, W);
                        System.arraycopy(aux, 160, out, (ntrama + 2) * 200, W);
                        System.arraycopy(aux, 240, out, (ntrama + 3) * 200, W - 40);
                        System.arraycopy(aux, 320, out, (ntrama + 4) * 200, W - 120);
                    }
                    else{

                        System.arraycopy(aux, 0, out, ntrama * 200 - 240, W-160);
                        System.arraycopy(aux, 0, out, ntrama * 200 - 120, W - 40);
                        System.arraycopy(aux, 0, out, ntrama * 200, W);
                        System.arraycopy(aux, 80, out, (ntrama + 1) * 200, W);
                        System.arraycopy(aux, 160, out, (ntrama + 2) * 200, W);
                        System.arraycopy(aux, 240, out, (ntrama + 3) * 200, W - 40);
                        System.arraycopy(aux, 320, out, (ntrama + 4) * 200, W - 120);

                    }*/
                    ntrama = ntrama + 5;
                }
            }
            inStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return aux;
    }

    private String getOriginalTempFilename() {

        String filepath = Environment.getExternalStorageDirectory().getPath();

        File file = new File(filepath, FileUtil.APP_DIR);
        String path = file.getAbsolutePath() + "/" + AUDIO_TEMPFILE + "1";

        return (file.getAbsolutePath() + "/" + AUDIO_TEMPFILE + "1");

    }

    private String getTempFilename() {

        String filepath = Environment.getExternalStorageDirectory().getPath();

        File file = new File(filepath, FileUtil.APP_DIR);
        String path = file.getAbsolutePath() + "/" + AUDIO_TEMPFILE;

        return (file.getAbsolutePath() + "/" + AUDIO_TEMPFILE);
    }
    private File configRecFileName( String extension) {

        String path;
        path = FileUtil.getUniqueFileName();


        return new File(FileUtil.getExternalStorageDir(FileUtil.APP_DIR),
                path + extension);
    }
    private String getFullname() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, FileUtil.APP_DIR);
        String eventID=recFileName;
        if (!file.exists()) {
            file.mkdirs();
        }


        return eventID;
    }
    private String getOriginalFullname() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, FileUtil.APP_DIR);
        String eventID=fileName;
        if (!file.exists()) {
            file.mkdirs();
        }


        return eventID;
    }
    private void prepareTempFile() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, FileUtil.APP_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
        File tempFile = new File(filepath, AUDIO_TEMPFILE);
        if (tempFile.exists()) {
            tempFile.delete();
        }
        File tempFile1 = new File(filepath, AUDIO_TEMPFILE+"1");
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }
    public void PlayAudioTrack(String filePath, String recFilePath)
            throws IOException {
        // We keep temporarily filePath globally as we have only two sample
        // sounds now..Ω
        //Log.e("ss","start1");
        if (filePath == null)
            return;
        //Log.e("ss","start2");
        // Reading the file..



        byte[] byteData = null;

        File originalFile = null;
        originalFile = new File(filePath); // for ex. path= "/sdcard/samplesound.pcm" or// "/sdcard/samplesound.wav"
        byteData = new byte[(int) originalFile.length()];
        //Log.e("ss",originalFile.getAbsolutePath());
        //short [] history = new short[byteData.length/2-22];
        FileInputStream in1 = null;
        /*try {
            in1 = new FileInputStream(originalFile);
            in1.read(byteData);

            for (int i = 22; i <byteData.length/2 ; i++)
            {
                history[i-22] = ( (short)( ( byteData[i*2] & 0xff )|( byteData[i*2 + 1] << 8 ) ) );
                //history[i-22] = shortArr[i-22];
            }

            //Log.e("ss",String.valueOf(shortArr.length));
            in1.close();




        } catch (FileNotFoundException e) {

            e.printStackTrace();
        }
*/
        File file = null;
        file = new File(recFilePath); // for ex. path= "/sdcard/samplesound.pcm" or// "/sdcard/samplesound.wav"
        byteData = new byte[(int) file.length()];
        short[] shortAr = new short[byteData.length/2-22];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(byteData);

            for (int i = 22; i <byteData.length/2 ; i++)
            {
                shortAr[i-22] = ( (short)( ( byteData[i*2] & 0xff )|( byteData[i*2 + 1] << 8 ) ) );
                //history[i-22] = shortArr[i-22];
            }

            //Log.e("ss",String.valueOf(shortArr.length));
            in.close();




        } catch (FileNotFoundException e) {

            e.printStackTrace();
        }

        int maxJitter = AudioTrack.getMinBufferSize(fs, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);


        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
                fs, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxJitter, AudioTrack.MODE_STREAM);


        if (track != null) {
            //Log.e("ss","update the wave");
            //waveformAudio.printAudioData(shortArr);
            //waveformCompare.printAudioData(history);
            //Log.e("ss","play the wave");
            track.play();

            // Write the byte array to the track
            track.write(byteData, 0, byteData.length);
            track.stop();
            track.release();
            track = null;
        } else
            Log.e("ss", "audio track is not initialised ");

    }
}

