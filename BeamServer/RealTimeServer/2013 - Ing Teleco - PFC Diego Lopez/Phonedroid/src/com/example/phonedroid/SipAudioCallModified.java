/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.phonedroid;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.Context;
import android.media.AudioManager;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.rtp.RtpStream;
import android.net.sip.SipErrorCode;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipSession;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.util.Log;

import com.example.phonedroid.SimpleSessionDescription.Media;

/**
 * Handles an Internet audio call over SIP. You can instantiate this class with {@link SipManager},
 * using {@link SipManager#makeAudioCall makeAudioCall()} and  {@link SipManager#takeAudioCall
 * takeAudioCall()}.
 *
 * <p class="note"><strong>Note:</strong> Using this class require the
 *   {@link android.Manifest.permission#INTERNET} and
 *   {@link android.Manifest.permission#USE_SIP} permissions.<br/><br/>In addition, {@link
 *   #startAudio} requires the
 *   {@link android.Manifest.permission#RECORD_AUDIO},
 *   {@link android.Manifest.permission#ACCESS_WIFI_STATE}, and
 *   {@link android.Manifest.permission#WAKE_LOCK} permissions; and {@link #setSpeakerMode
 *   setSpeakerMode()} requires the
 *   {@link android.Manifest.permission#MODIFY_AUDIO_SETTINGS} permission.</p>
 */
public class SipAudioCallModified {
    private static final String TAG = SipAudioCallModified.class.getSimpleName();
    private static final boolean RELEASE_SOCKET = true;
    private static final boolean DONT_RELEASE_SOCKET = false;
    private static final int SESSION_TIMEOUT = 5; // in seconds

    /** Listener for events relating to a SIP call, such as when a call is being
     * recieved ("on ringing") or a call is outgoing ("on calling").
     * <p>Many of these events are also received by {@link SipSession.Listener}.</p>
     */
    public static class Listener {
        /**
         * Called when the call object is ready to make another call.
         * The default implementation calls {@link #onChanged}.
         *
         * @param call the call object that is ready to make another call
         */
        public void onReadyToCall(SipAudioCallModified call) {
            onChanged(call);
        }

        /**
         * Called when a request is sent out to initiate a new call.
         * The default implementation calls {@link #onChanged}.
         *
         * @param call the call object that carries out the audio call
         */
        public void onCalling(SipAudioCallModified call) {
            onChanged(call);
        }

        /**
         * Called when a new call comes in.
         * The default implementation calls {@link #onChanged}.
         *
         * @param call the call object that carries out the audio call
         * @param caller the SIP profile of the caller
         */
        public void onRinging(SipAudioCallModified call, SipProfile caller) {
            onChanged(call);
        }

        /**
         * Called when a RINGING response is received for the INVITE request
         * sent. The default implementation calls {@link #onChanged}.
         *
         * @param call the call object that carries out the audio call
         */
        public void onRingingBack(SipAudioCallModified call) {
            onChanged(call);
        }

        /**
         * Called when the session is established.
         * The default implementation calls {@link #onChanged}.
         *
         * @param call the call object that carries out the audio call
         */
        public void onCallEstablished(SipAudioCallModified call) {
            onChanged(call);
        }

        /**
         * Called when the session is terminated.
         * The default implementation calls {@link #onChanged}.
         *
         * @param call the call object that carries out the audio call
         */
        public void onCallEnded(SipAudioCallModified call) {
            onChanged(call);
        }

        /**
         * Called when the peer is busy during session initialization.
         * The default implementation calls {@link #onChanged}.
         *
         * @param call the call object that carries out the audio call
         */
        public void onCallBusy(SipAudioCallModified call) {
            onChanged(call);
        }

        /**
         * Called when the call is on hold.
         * The default implementation calls {@link #onChanged}.
         *
         * @param call the call object that carries out the audio call
         */
        public void onCallHeld(SipAudioCallModified call) {
            onChanged(call);
        }

        /**
         * Called when an error occurs. The default implementation is no op.
         *
         * @param call the call object that carries out the audio call
         * @param errorCode error code of this error
         * @param errorMessage error message
         * @see SipErrorCode
         */
        public void onError(SipAudioCallModified call, int errorCode,
                String errorMessage) {
            // no-op
        }

        /**
         * Called when an event occurs and the corresponding callback is not
         * overridden. The default implementation is no op. Error events are
         * not re-directed to this callback and are handled in {@link #onError}.
         */
        public void onChanged(SipAudioCallModified call) {
            // no-op
        }
    }

    //CAMBIO
    //public String direccion=null;
    //public String primera_offer=null;
    
    private Context mContext;
    private SipProfile mLocalProfile;
    private SipAudioCallModified.Listener mListener;
    private SipSession mSipSession;

    private long mSessionId = System.currentTimeMillis();
    private String mPeerSd;

    private AudioStream mAudioStream;
    private AudioGroup mAudioGroup;

    private boolean mInCall = false;
    private boolean mMuted = false;
    private boolean mHold = false;

    private SipProfile mPendingCallRequest;
    private WifiManager mWm;
    private WifiManager.WifiLock mWifiHighPerfLock;

    private int mErrorCode = SipErrorCode.NO_ERROR;
    private String mErrorMessage;

    /**
     * Creates a call object with the local SIP profile.
     * @param context the context for accessing system services such as
     *        ringtone, audio, WIFI etc
     */
    public SipAudioCallModified(Context context, SipProfile localProfile) {
        mContext = context;
        mLocalProfile = localProfile;
        mWm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Sets the listener to listen to the audio call events. The method calls
     * {@link #setListener setListener(listener, false)}.
     *
     * @param listener to listen to the audio call events of this object
     * @see #setListener(Listener, boolean)
     */
    public void setListener(SipAudioCallModified.Listener listener) {
        setListener(listener, false);
    }

    /**
     * Sets the listener to listen to the audio call events. A
     * {@link SipAudioCallModified} can only hold one listener at a time. Subsequent
     * calls to this method override the previous listener.
     *
     * @param listener to listen to the audio call events of this object
     * @param callbackImmediately set to true if the caller wants to be called
     *      back immediately on the current state
     */
    public void setListener(SipAudioCallModified.Listener listener,
            boolean callbackImmediately) {
        mListener = listener;
        try {
            if ((listener == null) || !callbackImmediately) {
                // do nothing
            } else if (mErrorCode != SipErrorCode.NO_ERROR) {
                listener.onError(this, mErrorCode, mErrorMessage);
            } else if (mInCall) {
                if (mHold) {
                    listener.onCallHeld(this);
                } else {
                    listener.onCallEstablished(this);
                }
            } else {
                int state = getState();
                switch (state) {
                    case SipSession.State.READY_TO_CALL:
                        listener.onReadyToCall(this);
                        break;
                    case SipSession.State.INCOMING_CALL:
                        listener.onRinging(this, getPeerProfile());
                        break;
                    case SipSession.State.OUTGOING_CALL:
                        listener.onCalling(this);
                        break;
                    case SipSession.State.OUTGOING_CALL_RING_BACK:
                        listener.onRingingBack(this);
                        break;
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "setListener()", t);
        }
    }

    /**
     * Checks if the call is established.
     *
     * @return true if the call is established
     */
    public boolean isInCall() {
        synchronized (this) {
            return mInCall;
        }
    }

    /**
     * Checks if the call is on hold.
     *
     * @return true if the call is on hold
     */
    public boolean isOnHold() {
        synchronized (this) {
            return mHold;
        }
    }

    /**
     * Closes this object. This object is not usable after being closed.
     */
    public void close() {
        close(true);
    }

    private synchronized void close(boolean closeRtp) {
        if (closeRtp) stopCall(RELEASE_SOCKET);

        mInCall = false;
        mHold = false;
        mSessionId = System.currentTimeMillis();
        mErrorCode = SipErrorCode.NO_ERROR;
        mErrorMessage = null;

        if (mSipSession != null) {
            mSipSession.setListener(null);
            mSipSession = null;
        }
    }

    /**
     * Gets the local SIP profile.
     *
     * @return the local SIP profile
     */
    public SipProfile getLocalProfile() {
        synchronized (this) {
            return mLocalProfile;
        }
    }

    /**
     * Gets the peer's SIP profile.
     *
     * @return the peer's SIP profile
     */
    public SipProfile getPeerProfile() {
        synchronized (this) {
            return (mSipSession == null) ? null : mSipSession.getPeerProfile();
        }
    }

    /**
     * Gets the state of the {@link SipSession} that carries this call.
     * The value returned must be one of the states in {@link SipSession.State}.
     *
     * @return the session state
     */
    public int getState() {
        synchronized (this) {
            if (mSipSession == null) return SipSession.State.READY_TO_CALL;
            return mSipSession.getState();
        }
    }


    /**
     * Gets the {@link SipSession} that carries this call.
     *
     * @return the session object that carries this call
     * @hide
     */
    public SipSession getSipSession() {
        synchronized (this) {
            return mSipSession;
        }
    }

    private SipSession.Listener createListener() {
        return new SipSession.Listener() {
            @Override
            public void onCalling(SipSession session) {
                Log.d(TAG, "calling... " + session);
                Listener listener = mListener;
                if (listener != null) {
                    try {
                        listener.onCalling(SipAudioCallModified.this);
                    } catch (Throwable t) {
                        Log.i(TAG, "onCalling(): " + t);
                    }
                }
            }

            @Override
            public void onRingingBack(SipSession session) {
                Log.d(TAG, "sip call ringing back: " + session);
                Listener listener = mListener;
                if (listener != null) {
                    try {
                        listener.onRingingBack(SipAudioCallModified.this);
                    } catch (Throwable t) {
                        Log.i(TAG, "onRingingBack(): " + t);
                    }
                }
            }

            @Override
            public void onRinging(SipSession session,
                    SipProfile peerProfile, String sessionDescription) {
                synchronized (SipAudioCallModified.this) {
                    if ((mSipSession == null) || !mInCall
                            || !session.getCallId().equals(
                                    mSipSession.getCallId())) {
                        // should not happen
                        session.endCall();
                        return;
                    }

                    // session changing request
                    try {
                        String answer = createAnswer(sessionDescription).encode();
                        mSipSession.answerCall(answer, SESSION_TIMEOUT);
                    } catch (Throwable e) {
                        Log.e(TAG, "onRinging()", e);
                        session.endCall();
                    }
                }
            }

            @Override
            public void onCallEstablished(SipSession session,
                    String sessionDescription) {
                mPeerSd = sessionDescription;
                Log.v(TAG, "onCallEstablished()" + mPeerSd);

                Listener listener = mListener;
                if (listener != null) {
                    try {
                        if (mHold) {
                            listener.onCallHeld(SipAudioCallModified.this);
                        } else {
                            listener.onCallEstablished(SipAudioCallModified.this);
                        }
                    } catch (Throwable t) {
                        Log.i(TAG, "onCallEstablished(): " + t);
                    }
                }
            }

            @Override
            public void onCallEnded(SipSession session) {
                Log.d(TAG, "sip call ended: " + session);
                Listener listener = mListener;
                if (listener != null) {
                    try {
                        listener.onCallEnded(SipAudioCallModified.this);
                    } catch (Throwable t) {
                        Log.i(TAG, "onCallEnded(): " + t);
                    }
                }
                close();
            }

            @Override
            public void onCallBusy(SipSession session) {
                Log.d(TAG, "sip call busy: " + session);
                Listener listener = mListener;
                if (listener != null) {
                    try {
                        listener.onCallBusy(SipAudioCallModified.this);
                    } catch (Throwable t) {
                        Log.i(TAG, "onCallBusy(): " + t);
                    }
                }
                close(false);
            }

            @Override
            public void onCallChangeFailed(SipSession session, int errorCode,
                    String message) {
                Log.d(TAG, "sip call change failed: " + message);
                mErrorCode = errorCode;
                mErrorMessage = message;
                Listener listener = mListener;
                if (listener != null) {
                    try {
                        listener.onError(SipAudioCallModified.this, mErrorCode,
                                message);
                    } catch (Throwable t) {
                        Log.i(TAG, "onCallBusy(): " + t);
                    }
                }
            }

            @Override
            public void onError(SipSession session, int errorCode,
                    String message) {
                SipAudioCallModified.this.onError(errorCode, message);
            }

            @Override
            public void onRegistering(SipSession session) {
                // irrelevant
            }

            @Override
            public void onRegistrationTimeout(SipSession session) {
                // irrelevant
            }

            @Override
            public void onRegistrationFailed(SipSession session, int errorCode,
                    String message) {
                // irrelevant
            }

            @Override
            public void onRegistrationDone(SipSession session, int duration) {
                // irrelevant
            }
        };
    }

    private void onError(int errorCode, String message) {
        Log.d(TAG, "sip session error: "
                + SipErrorCode.toString(errorCode) + ": " + message);
        mErrorCode = errorCode;
        mErrorMessage = message;
        Listener listener = mListener;
        if (listener != null) {
            try {
                listener.onError(this, errorCode, message);
            } catch (Throwable t) {
                Log.i(TAG, "onError(): " + t);
            }
        }
        synchronized (this) {
            if ((errorCode == SipErrorCode.DATA_CONNECTION_LOST)
                    || !isInCall()) {
                close(true);
            }
        }
    }

    /**
     * Attaches an incoming call to this call object.
     *
     * @param session the session that receives the incoming call
     * @param sessionDescription the session description of the incoming call
     * @throws SipException if the SIP service fails to attach this object to
     *        the session
     */
    public void attachCall(SipSession session, String sessionDescription)
            throws SipException {
        synchronized (this) {
            mSipSession = session;
            mPeerSd = sessionDescription;
            Log.v(TAG, "attachCall()" + mPeerSd);
            try {
                session.setListener(createListener());
            } catch (Throwable e) {
                Log.e(TAG, "attachCall()", e);
                throwSipException(e);
            }
        }
    }

    /**
     * Initiates an audio call to the specified profile. The attempt will be
     * timed out if the call is not established within {@code timeout} seconds
     * and {@link Listener#onError onError(SipAudioCallModified, SipErrorCode.TIME_OUT, String)}
     * will be called.
     *
     * @param peerProfile the SIP profile to make the call to
     * @param sipSession the {@link SipSession} for carrying out the call
     * @param timeout the timeout value in seconds. Default value (defined by
     *        SIP protocol) is used if {@code timeout} is zero or negative.
     * @see Listener#onError
     * @throws SipException if the SIP service fails to create a session for the
     *        call
     */
    public void makeCall(SipProfile peerProfile, SipSession sipSession,
            int timeout, int chosen_codec) throws SipException {
        synchronized (this) {
            mSipSession = sipSession;
            try {
                mAudioStream = new AudioStream(InetAddress.getByName(
                        getLocalIp()));
                sipSession.setListener(createListener());
                sipSession.makeCall(peerProfile, createOffer(chosen_codec).encode(),
                        timeout);
                //CAMBIO
                //primera_offer=createOffer(chosen_codec).encode();
            } catch (IOException e) {
                throw new SipException("makeCall()", e);
            }
        }
    }

    /**
     * Ends a call.
     * @throws SipException if the SIP service fails to end the call
     */
    public void endCall() throws SipException {
        synchronized (this) {
            stopCall(RELEASE_SOCKET);
            mInCall = false;

            // perform the above local ops first and then network op
            if (mSipSession != null) mSipSession.endCall();
        }
    }

    /**
     * Puts a call on hold.  When succeeds, {@link Listener#onCallHeld} is
     * called. The attempt will be timed out if the call is not established
     * within {@code timeout} seconds and
     * {@link Listener#onError onError(SipAudioCallModified, SipErrorCode.TIME_OUT, String)}
     * will be called.
     *
     * @param timeout the timeout value in seconds. Default value (defined by
     *        SIP protocol) is used if {@code timeout} is zero or negative.
     * @see Listener#onError
     * @throws SipException if the SIP service fails to hold the call
     */
    public void holdCall(int timeout) throws SipException {
        synchronized (this) {
        if (mHold) return;
            mSipSession.changeCall(createHoldOffer().encode(), timeout);
            mHold = true;

            AudioGroup audioGroup = getAudioGroup();
            if (audioGroup != null) audioGroup.setMode(AudioGroup.MODE_ON_HOLD);
        }
    }

    /**
     * Answers a call. The attempt will be timed out if the call is not
     * established within {@code timeout} seconds and
     * {@link Listener#onError onError(SipAudioCallModified, SipErrorCode.TIME_OUT, String)}
     * will be called.
     *
     * @param timeout the timeout value in seconds. Default value (defined by
     *        SIP protocol) is used if {@code timeout} is zero or negative.
     * @see Listener#onError
     * @throws SipException if the SIP service fails to answer the call
     */
    public void answerCall(int timeout) throws SipException {
        synchronized (this) {
            try {
                mAudioStream = new AudioStream(InetAddress.getByName(
                        getLocalIp()));
                mSipSession.answerCall(createAnswer(mPeerSd).encode(), timeout);
            } catch (IOException e) {
                throw new SipException("answerCall()", e);
            }
        }
    }

    /**
     * Continues a call that's on hold. When succeeds,
     * {@link Listener#onCallEstablished} is called. The attempt will be timed
     * out if the call is not established within {@code timeout} seconds and
     * {@link Listener#onError onError(SipAudioCallModified, SipErrorCode.TIME_OUT, String)}
     * will be called.
     *
     * @param timeout the timeout value in seconds. Default value (defined by
     *        SIP protocol) is used if {@code timeout} is zero or negative.
     * @see Listener#onError
     * @throws SipException if the SIP service fails to unhold the call
     */
    public void continueCall(int timeout) throws SipException {
        synchronized (this) {
            if (!mHold) return;
            mSipSession.changeCall(createContinueOffer().encode(), timeout);
            mHold = false;
            AudioGroup audioGroup = getAudioGroup();
            if (audioGroup != null) audioGroup.setMode(AudioGroup.MODE_NORMAL);
        }
    }

    /**
     * 
     * @param chosen_codec
     * @return
     * 
     * Se introducen una serie de cambios para poder elegir c?dec
     */
    
    private SimpleSessionDescription createOffer(int chosen_codec) {
        SimpleSessionDescription offer =
                new SimpleSessionDescription(mSessionId, getLocalIp());
        
        /* Se establece el c?dec a utilizar en funci?n de la opci?n escogida */
        
        //AudioCodec[] codecs = AudioCodec.getCodecs();
        //int codec_selec_int=spinner_opc_codec.getSelectedItemPosition();
        AudioCodec codec;
        
			switch(chosen_codec){
			case 0: //stream_emisor.setCodec(AudioCodec.AMR);
				codec=AudioCodec.AMR;
			break;
			
			case 1: //stream_emisor.setCodec(AudioCodec.GSM);
				codec=AudioCodec.GSM;
			break;
			
			case 2: //stream_emisor.setCodec(AudioCodec.GSM_EFR);
				codec=AudioCodec.GSM_EFR;
			break;
			
			case 3: //stream_emisor.setCodec(AudioCodec.PCMA);
				codec=AudioCodec.PCMA;
			break;
			
			case 4: //stream_emisor.setCodec(AudioCodec.PCMU);
				codec=AudioCodec.PCMU;
			break;
			
			default: //stream_emisor.setCodec(AudioCodec.PCMU);
				codec=AudioCodec.GSM;
			};
        Media media = offer.newMedia(
                "audio", mAudioStream.getLocalPort(), 1, "RTP/AVP");
        //for (AudioCodec codec : AudioCodec.getCodecs()) {
            media.setRtpPayload(codec.type, codec.rtpmap, codec.fmtp);
        //}
        media.setRtpPayload(127, "telephone-event/8000", "0-15");
        return offer;
    }

    private SimpleSessionDescription createAnswer(String offerSd) {
        SimpleSessionDescription offer =
                new SimpleSessionDescription(offerSd);
        SimpleSessionDescription answer =
                new SimpleSessionDescription(mSessionId, getLocalIp());
        AudioCodec codec = null;
        for (Media media : offer.getMedia()) {
            if ((codec == null) && (media.getPort() > 0)
                    && "audio".equals(media.getType())
                    && "RTP/AVP".equals(media.getProtocol())) {
                // Find the first audio codec we supported.
                for (int type : media.getRtpPayloadTypes()) {
                    codec = AudioCodec.getCodec(type, media.getRtpmap(type),
                            media.getFmtp(type));
                    if (codec != null) {
                        break;
                    }
                }
                if (codec != null) {
                    Media reply = answer.newMedia(
                            "audio", mAudioStream.getLocalPort(), 1, "RTP/AVP");
                    reply.setRtpPayload(codec.type, codec.rtpmap, codec.fmtp);

                    // Check if DTMF is supported in the same media.
                    for (int type : media.getRtpPayloadTypes()) {
                        String rtpmap = media.getRtpmap(type);
                        if ((type != codec.type) && (rtpmap != null)
                                && rtpmap.startsWith("telephone-event")) {
                            reply.setRtpPayload(
                                    type, rtpmap, media.getFmtp(type));
                        }
                    }

                    // Handle recvonly and sendonly.
                    if (media.getAttribute("recvonly") != null) {
                        answer.setAttribute("sendonly", "");
                    } else if(media.getAttribute("sendonly") != null) {
                        answer.setAttribute("recvonly", "");
                    } else if(offer.getAttribute("recvonly") != null) {
                        answer.setAttribute("sendonly", "");
                    } else if(offer.getAttribute("sendonly") != null) {
                        answer.setAttribute("recvonly", "");
                    }
                    continue;
                }
            }
            // Reject the media.
            Media reply = answer.newMedia(
                    media.getType(), 0, 1, media.getProtocol());
            for (String format : media.getFormats()) {
                reply.setFormat(format, null);
            }
        }
        if (codec == null) {
            throw new IllegalStateException("Reject SDP: no suitable codecs");
        }
        return answer;
    }

    private SimpleSessionDescription createHoldOffer() {
        SimpleSessionDescription offer = createContinueOffer();
        offer.setAttribute("sendonly", "");
        return offer;
    }

    private SimpleSessionDescription createContinueOffer() {
        SimpleSessionDescription offer =
                new SimpleSessionDescription(mSessionId, getLocalIp());
        Media media = offer.newMedia(
                "audio", mAudioStream.getLocalPort(), 1, "RTP/AVP");
        AudioCodec codec = mAudioStream.getCodec();
        media.setRtpPayload(codec.type, codec.rtpmap, codec.fmtp);
        int dtmfType = mAudioStream.getDtmfType();
        if (dtmfType != -1) {
            media.setRtpPayload(dtmfType, "telephone-event/8000", "0-15");
        }
        return offer;
    }

    private void grabWifiHighPerfLock() {
        if (mWifiHighPerfLock == null) {
            Log.v(TAG, "acquire wifi high perf lock");
            mWifiHighPerfLock = ((WifiManager)
                    mContext.getSystemService(Context.WIFI_SERVICE))
                    .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG);
            mWifiHighPerfLock.acquire();
        }
    }

    private void releaseWifiHighPerfLock() {
        if (mWifiHighPerfLock != null) {
            Log.v(TAG, "release wifi high perf lock");
            mWifiHighPerfLock.release();
            mWifiHighPerfLock = null;
        }
    }

    private boolean isWifiOn() {
        return (mWm.getConnectionInfo().getBSSID() == null) ? false : true;
    }

    /** Toggles mute. */
    public void toggleMute() {
        synchronized (this) {
            AudioGroup audioGroup = getAudioGroup();
            if (audioGroup != null) {
                audioGroup.setMode(mMuted
                        ? AudioGroup.MODE_NORMAL
                        : AudioGroup.MODE_MUTED);
                mMuted = !mMuted;
            }
        }
    }

    /**
     * Checks if the call is muted.
     *
     * @return true if the call is muted
     */
    public boolean isMuted() {
        synchronized (this) {
            return mMuted;
        }
    }

    /**
     * Puts the device to speaker mode.
     * <p class="note"><strong>Note:</strong> Requires the
     *   {@link android.Manifest.permission#MODIFY_AUDIO_SETTINGS} permission.</p>
     */
    public void setSpeakerMode(boolean speakerMode) {
        synchronized (this) {
            ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE))
                    .setSpeakerphoneOn(speakerMode);
        }
    }

    /**
     * Sends a DTMF code. According to <a href="http://tools.ietf.org/html/rfc2833">RFC 2883</a>,
     * event 0--9 maps to decimal
     * value 0--9, '*' to 10, '#' to 11, event 'A'--'D' to 12--15, and event
     * flash to 16. Currently, event flash is not supported.
     *
     * @param code the DTMF code to send. Value 0 to 15 (inclusive) are valid
     *        inputs.
     */
    public void sendDtmf(int code) {
        sendDtmf(code, null);
    }

    /**
     * Sends a DTMF code. According to <a href="http://tools.ietf.org/html/rfc2833">RFC 2883</a>,
     * event 0--9 maps to decimal
     * value 0--9, '*' to 10, '#' to 11, event 'A'--'D' to 12--15, and event
     * flash to 16. Currently, event flash is not supported.
     *
     * @param code the DTMF code to send. Value 0 to 15 (inclusive) are valid
     *        inputs.
     * @param result the result message to send when done
     */
    public void sendDtmf(int code, Message result) {
        synchronized (this) {
            AudioGroup audioGroup = getAudioGroup();
            if ((audioGroup != null) && (mSipSession != null)
                    && (SipSession.State.IN_CALL == getState())) {
                Log.v(TAG, "send DTMF: " + code);
                audioGroup.sendDtmf(code);
            }
            if (result != null) result.sendToTarget();
        }
    }

    /**
     * Gets the {@link AudioStream} object used in this call. The object
     * represents the RTP stream that carries the audio data to and from the
     * peer. The object may not be created before the call is established. And
     * it is undefined after the call ends or the {@link #close} method is
     * called.
     *
     * @return the {@link AudioStream} object or null if the RTP stream has not
     *      yet been set up
     * @hide
     */
    public AudioStream getAudioStream() {
        synchronized (this) {
            return mAudioStream;
        }
    }

    /**
     * Gets the {@link AudioGroup} object which the {@link AudioStream} object
     * joins. The group object may not exist before the call is established.
     * Also, the {@code AudioStream} may change its group during a call (e.g.,
     * after the call is held/un-held). Finally, the {@code AudioGroup} object
     * returned by this method is undefined after the call ends or the
     * {@link #close} method is called. If a group object is set by
     * {@link #setAudioGroup(AudioGroup)}, then this method returns that object.
     *
     * @return the {@link AudioGroup} object or null if the RTP stream has not
     *      yet been set up
     * @see #getAudioStream
     * @hide
     */
    public AudioGroup getAudioGroup() {
        synchronized (this) {
            if (mAudioGroup != null) return mAudioGroup;
            return ((mAudioStream == null) ? null : mAudioStream.getGroup());
        }
    }

    /**
     * Sets the {@link AudioGroup} object which the {@link AudioStream} object
     * joins. If {@code audioGroup} is null, then the {@code AudioGroup} object
     * will be dynamically created when needed.
     *
     * @see #getAudioStream
     * @hide
     */
    public void setAudioGroup(AudioGroup group) {
        synchronized (this) {
            if ((mAudioStream != null) && (mAudioStream.getGroup() != null)) {
                mAudioStream.join(group);
            }
            mAudioGroup = group;
        }
    }

    /**
     * Starts the audio for the established call. This method should be called
     * after {@link Listener#onCallEstablished} is called.
     * <p class="note"><strong>Note:</strong> Requires the
     *   {@link android.Manifest.permission#RECORD_AUDIO},
     *   {@link android.Manifest.permission#ACCESS_WIFI_STATE} and
     *   {@link android.Manifest.permission#WAKE_LOCK} permissions.</p>
     */
    public void startAudio() {
        try {
            startAudioInternal();
        } catch (UnknownHostException e) {
            onError(SipErrorCode.PEER_NOT_REACHABLE, e.getMessage());
        } catch (Throwable e) {
            onError(SipErrorCode.CLIENT_ERROR, e.getMessage());
        }
    }

    private synchronized void startAudioInternal() throws UnknownHostException {
        if (mPeerSd == null) {
            Log.v(TAG, "startAudioInternal() mPeerSd = null");
            throw new IllegalStateException("mPeerSd = null");
        }

        stopCall(DONT_RELEASE_SOCKET);
        mInCall = true;

        // Run exact the same logic in createAnswer() to setup mAudioStream.
        SimpleSessionDescription offer =
                new SimpleSessionDescription(mPeerSd);
        AudioStream stream = mAudioStream;
        AudioCodec codec = null;
        for (Media media : offer.getMedia()) {
            if ((codec == null) && (media.getPort() > 0)
                    && "audio".equals(media.getType())
                    && "RTP/AVP".equals(media.getProtocol())) {
                // Find the first audio codec we supported.
                for (int type : media.getRtpPayloadTypes()) {
                    codec = AudioCodec.getCodec(
                            type, media.getRtpmap(type), media.getFmtp(type));
                    if (codec != null) {
                        break;
                    }
                }

                if (codec != null) {
                    // Associate with the remote host.
                    String address = media.getAddress();
                    //CAMBIO
                    
                    //direccion=media.getAddress();
                    
                    if (address == null) {
                        address = offer.getAddress();
                    }
                    //CAMBIO
                    //direccion="media_getAd"+media.getAddress()+" offer_getAd:"+offer.getAddress()+"|"+media.getPort();
                    //direccion="dir:"+InetAddress.getByName(address).toString()+" port:"+String.valueOf(media.getPort());
                    //stream.associate(InetAddress.getByName("217.9.36.145"),
                    //media.getPort());
                    stream.associate(InetAddress.getByName(address),
                           media.getPort());

                    stream.setDtmfType(-1);
                    stream.setCodec(codec);
                    // Check if DTMF is supported in the same media.
                    for (int type : media.getRtpPayloadTypes()) {
                        String rtpmap = media.getRtpmap(type);
                        if ((type != codec.type) && (rtpmap != null)
                                && rtpmap.startsWith("telephone-event")) {
                            stream.setDtmfType(type);
                        }
                    }

                    // Handle recvonly and sendonly.
                    if (mHold) {
                        stream.setMode(RtpStream.MODE_NORMAL);
                    } else if (media.getAttribute("recvonly") != null) {
                        stream.setMode(RtpStream.MODE_SEND_ONLY);
                    } else if(media.getAttribute("sendonly") != null) {
                        stream.setMode(RtpStream.MODE_RECEIVE_ONLY);
                    } else if(offer.getAttribute("recvonly") != null) {
                        stream.setMode(RtpStream.MODE_SEND_ONLY);
                    } else if(offer.getAttribute("sendonly") != null) {
                        stream.setMode(RtpStream.MODE_RECEIVE_ONLY);
                    } else {
                        stream.setMode(RtpStream.MODE_NORMAL);
                    }
                    break;
                }
            }
        }
        if (codec == null) {
            throw new IllegalStateException("Reject SDP: no suitable codecs");
        }

        if (isWifiOn()) grabWifiHighPerfLock();

        if (!mHold) {
            /* The recorder volume will be very low if the device is in
             * IN_CALL mode. Therefore, we have to set the mode to NORMAL
             * in order to have the normal microphone level.
             */
            ((AudioManager) mContext.getSystemService
                    (Context.AUDIO_SERVICE))
                    .setMode(AudioManager.MODE_NORMAL);
        }

        // AudioGroup logic:
        AudioGroup audioGroup = getAudioGroup();
        if (mHold) {
            if (audioGroup != null) {
                audioGroup.setMode(AudioGroup.MODE_ON_HOLD);
            }
            // don't create an AudioGroup here; doing so will fail if
            // there's another AudioGroup out there that's active
        } else {
            if (audioGroup == null) audioGroup = new AudioGroup();
            stream.join(audioGroup);
            if (mMuted) {
                audioGroup.setMode(AudioGroup.MODE_MUTED);
            } else {
                audioGroup.setMode(AudioGroup.MODE_NORMAL);
            	//audioGroup.setMode(AudioGroup.MODE_ECHO_SUPPRESSION);
            }
        }
    }

    private void stopCall(boolean releaseSocket) {
        Log.d(TAG, "stop audiocall");
        releaseWifiHighPerfLock();
        if (mAudioStream != null) {
            mAudioStream.join(null);

            if (releaseSocket) {
                mAudioStream.release();
                mAudioStream = null;
            }
        }
    }

    private String getLocalIp() {
        return mSipSession.getLocalIp();
    }

    private void throwSipException(Throwable throwable) throws SipException {
        if (throwable instanceof SipException) {
            throw (SipException) throwable;
        } else {
            throw new SipException("", throwable);
        }
    }

    private SipProfile getPeerProfile(SipSession session) {
        return session.getPeerProfile();
    }
}
