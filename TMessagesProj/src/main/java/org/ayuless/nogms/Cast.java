/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.nogms;

import android.content.Context;
import android.net.Uri;
import androidx.mediarouter.media.MediaRouteSelector;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Inert Cast compatibility surface. No discovery, sessions, playback or network services. */
public final class Cast {
    private Cast() {}
    public static final class CastContext {
        private static final CastContext INSTANCE=new CastContext();
        private final SessionManager manager=new SessionManager();
        public static CastContext getSharedInstance(Context c) { return INSTANCE; }
        public SessionManager getSessionManager() { return manager; }
        public MediaRouteSelector getMergedSelector() { return MediaRouteSelector.EMPTY; }
        public void addCastStateListener(Consumer<Integer> listener) {}
    }
    public static final class SessionManager {
        public CastSession getCurrentCastSession() { return null; }
        public <T> void addSessionManagerListener(SessionManagerListener<T> listener, Class<T> type) {}
        public void endCurrentSession(boolean stop) {}
    }
    public interface SessionManagerListener<T> {
        void onSessionStarting(T session);
        void onSessionStarted(T session, String id);
        void onSessionStartFailed(T session, int error);
        void onSessionEnding(T session);
        void onSessionEnded(T session, int error);
        void onSessionResuming(T session, String id);
        void onSessionResumed(T session, boolean suspended);
        void onSessionResumeFailed(T session, int error);
        void onSessionSuspended(T session, int reason);
    }
    public static final class CastSession {
        private CastSession() {}
        public RemoteMediaClient getRemoteMediaClient() { return null; }
        public String getSessionId() { return null; }
        public CastDevice getCastDevice() { return null; }
        public boolean isConnecting() { return false; }
        public boolean isConnected() { return false; }
    }
    public static final class CastDevice { public String getFriendlyName() { return ""; } }
    public static final class MediaError {
        public static final class DetailedErrorCode { public static final int MEDIA_SRC_NOT_SUPPORTED=104, MEDIA_DECODE=102; }
        public Integer getDetailedErrorCode() { return null; }
        public long getRequestId() { return 0; }
    }
    public static final class MediaStatus {
        public static final int REPEAT_MODE_REPEAT_SINGLE=1, IDLE_REASON_CANCELED=2, IDLE_REASON_ERROR=4;
        public double getStreamVolume() { return 0; }
        public double getPlaybackRate() { return 1; }
    }
    public static final class Operation {
        public void addStatusListener(Consumer<Google.Status> listener) { listener.accept(new Google.Status()); }
    }
    public static final class RemoteMediaClient {
        private RemoteMediaClient() {}
        public static class Callback {
            public void onStatusUpdated() {}
            public void onMediaError(MediaError error) {}
            public void onAdBreakStatusUpdated() {}
            public void onMetadataUpdated() {}
            public void onPreloadStatusUpdated() {}
            public void onQueueStatusUpdated() {}
            public void onSendingRemoteMediaRequest() {}
        }
        public void registerCallback(Callback c) {}
        public void unregisterCallback(Callback c) {}
        public void queueSetRepeatMode(int mode, Object data) {}
        public long getApproximateStreamPosition() { return -1; }
        public MediaStatus getMediaStatus() { return null; }
        public int getIdleReason() { return 0; }
        public boolean isPaused() { return true; }
        public boolean isPlaying() { return false; }
        public Operation seek(MediaSeekOptions options) { return new Operation(); }
        public Operation setStreamVolume(double volume) { return new Operation(); }
        public Operation setPlaybackRate(double rate) { return new Operation(); }
        public Operation play() { return new Operation(); }
        public Operation pause() { return new Operation(); }
        public Operation load(MediaInfo info, MediaLoadOptions options) { return new Operation(); }
    }
    public static final class MediaSeekOptions {
        public static final class Builder {
            public Builder setPosition(long position) { return this; }
            public MediaSeekOptions build() { return new MediaSeekOptions(); }
        }
    }
    public static final class MediaLoadOptions {
        public static final class Builder {
            public Builder setAutoplay(boolean autoplay) { return this; }
            public MediaLoadOptions build() { return new MediaLoadOptions(); }
        }
    }
    public static final class MediaInfo {
        public static final int STREAM_TYPE_BUFFERED=1;
        public static final class Builder {
            public Builder(String uri) {}
            public Builder setContentType(String type) { return this; }
            public Builder setMetadata(MediaMetadata metadata) { return this; }
            public Builder setStreamType(int type) { return this; }
            public MediaInfo build() { return new MediaInfo(); }
        }
    }
    public static final class MediaMetadata {
        public static final int MEDIA_TYPE_GENERIC=0, MEDIA_TYPE_MOVIE=1, MEDIA_TYPE_MUSIC_TRACK=3, MEDIA_TYPE_PHOTO=4;
        public static final String KEY_TITLE="title", KEY_SUBTITLE="subtitle", KEY_ARTIST="artist", KEY_ALBUM_TITLE="album", KEY_ALBUM_ARTIST="albumArtist", KEY_COMPOSER="composer", KEY_DISC_NUMBER="disc", KEY_TRACK_NUMBER="track", KEY_WIDTH="width", KEY_HEIGHT="height";
        private final Map<String,Object> values=new HashMap<>();
        public MediaMetadata() {}
        public MediaMetadata(int type) {}
        public void putString(String key, String value) { values.put(key,value); }
        public void putInt(String key, int value) { values.put(key,value); }
        public String getString(String key) { Object v=values.get(key); return v instanceof String ? (String)v : null; }
        public void addImage(WebImage image) {}
    }
    public static final class WebImage { public WebImage(Uri uri) {} }
}
