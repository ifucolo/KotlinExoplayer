package include.kotlinexoplayer

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.KeyEvent
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlaybackControlView
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.upstream.DataSource
import java.net.CookiePolicy.ACCEPT_ORIGINAL_SERVER
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import android.widget.Toast
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.C
import android.R.attr.mimeType
import butterknife.bindView
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.ui.SimpleExoPlayerView

class MainActivity : AppCompatActivity(), ExoPlayer.EventListener, PlaybackControlView.VisibilityListener {

    val BANDWIDTH_METER = DefaultBandwidthMeter()
    var DEFAULT_COOKIE_MANAGER: CookieManager = CookieManager()

//    companion object {
//        var DEFAULT_COOKIE_MANAGER: CookieManager = CookieManager()
//        var DEFAULT_COOKIE_MANAGER: CookiePolicy = setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
//    }


    var mediaDataSourceFactory: DataSource.Factory? = null
    var mainHandler: Handler = Handler()
    var trackSelector: DefaultTrackSelector? = DefaultTrackSelector()
    var playerNeedsSource: Boolean = false

    var resumeWindow: Int = 0
    var resumePosition: Long = 0
    var player: SimpleExoPlayer? = null

    private val simpleExoPlayerView: SimpleExoPlayerView by bindView(R.id.simpleExoplayer)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        mediaDataSourceFactory = buildDataSourceFactory(true)
        mainHandler = Handler()
        if (CookieHandler.getDefault() !== DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER)
        }

        simpleExoPlayerView.setControllerVisibilityListener(this)

        initializePlayer(true)
    }


    fun initializePlayer(shouldAutoPlay: Boolean) {

        val url = "http://storage.googleapis.com/exoplayer-test-media-0/play.mp3"

        if (player == null) {
            val preferExtensionDecoders = false

            @SimpleExoPlayer.ExtensionRendererMode val extensionRendererMode = if ((getApplication() as KotlinExoplayerApplication).useExtensionRenderers())
                if (preferExtensionDecoders)
                    SimpleExoPlayer.EXTENSION_RENDERER_MODE_PREFER
                else
                    SimpleExoPlayer.EXTENSION_RENDERER_MODE_ON
            else
                SimpleExoPlayer.EXTENSION_RENDERER_MODE_OFF

            val videoTrackSelectionFactory = AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER)
            trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, DefaultLoadControl(), null, extensionRendererMode)

            player!!.addListener(this)

            simpleExoPlayerView.player = player

            player!!.playWhenReady = shouldAutoPlay
            playerNeedsSource = true

        }

        if (playerNeedsSource && !TextUtils.isEmpty(url)) {
            val uri = Uri.parse(url)

            if (com.google.android.exoplayer2.util.Util.maybeRequestReadExternalStoragePermission(this, uri)) {
                // The player will be reinitialized if the permission is granted.
                return
            }
            val mediaSources = arrayOfNulls<MediaSource>(1)

            mediaSources[0] = buildMediaSource(uri, "")

            val mediaSource = mediaSources[0]

            val haveResumePosition = resumeWindow !== C.INDEX_UNSET
            if (haveResumePosition) {
                player!!.seekTo(resumeWindow, resumePosition)
            }
            player!!.prepare(mediaSource, !haveResumePosition, false)
            playerNeedsSource = false
            simpleExoPlayerView.requestFocus()
            simpleExoPlayerView.showController()

        }
    }

    fun buildMediaSource(uri: Uri, overrideExtension: String): MediaSource {
        val type = com.google.android.exoplayer2.util.Util.inferContentType(if (!TextUtils.isEmpty(overrideExtension))
            "." + overrideExtension
        else
            uri.lastPathSegment)
        when (type) {
            C.TYPE_SS -> return SsMediaSource(uri, buildDataSourceFactory(false),
                    DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, null)
            C.TYPE_DASH -> return DashMediaSource(uri, buildDataSourceFactory(false),
                    DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, null)
            C.TYPE_HLS -> return HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, null)
            C.TYPE_OTHER -> return ExtractorMediaSource(uri, mediaDataSourceFactory, DefaultExtractorsFactory(),
                    mainHandler, null)
            else -> {
                throw IllegalStateException("Unsupported type: " + type)
            }
        }
    }

    protected fun buildDataSourceFactory(useBandwidthMeter: Boolean): DataSource.Factory {
        return (application as KotlinExoplayerApplication)
                .buildDataSourceFactory(if (useBandwidthMeter) BANDWIDTH_METER else null!!)
    }

    override fun onTimelineChanged(timeline: Timeline, manifest: Any) {}

    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

    override fun onLoadingChanged(isLoading: Boolean) {}

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

        if (playWhenReady) {

        }
    }

    override fun onPlayerError(e: ExoPlaybackException) {
        var errorString: String? = null
        if (e.type == ExoPlaybackException.TYPE_RENDERER) {
            val cause = e.rendererException
            if (cause is MediaCodecRenderer.DecoderInitializationException) {
                // Special case for decoder initialization failures.
                val decoderInitializationException = cause
                if (decoderInitializationException.decoderName == null) {
                    if (decoderInitializationException.cause is MediaCodecUtil.DecoderQueryException) {
                        // errorString = getString(R.string.error_querying_decoders)
                    } else if (decoderInitializationException.secureDecoderRequired) {
                        // errorString = getString(R.string.error_no_secure_decoder,
                        // decoderInitializationException.mimeType)
                    } else {
                        //errorString = getString(R.string.error_no_decoder,
                        //    decoderInitializationException.mimeType)
                    }
                } else {
                    // errorString = getString(R.string.error_instantiating_decoder,
                    // decoderInitializationException.decoderName)
                }
            }
        }
        if (errorString != null) {
            showToast(errorString)
        }
        playerNeedsSource = true
        if (isBehindLiveWindow(e)) {
            clearResumePosition()
            initializePlayer(true)
        } else
            updateResumePosition()

    }

    override fun onPositionDiscontinuity() {
        if (playerNeedsSource) {
            // This will only occur if the user has performed a seek whilst in the error state. Update the
            // resume position so that if the user then retries, playback will resume from the position to
            // which they seeked.
            updateResumePosition()
        }
    }

    fun updateResumePosition() {
        resumeWindow = player!!.currentWindowIndex
        resumePosition = if (player!!.isCurrentWindowSeekable)
            Math.max(0, player!!.currentPosition)
        else
            C.TIME_UNSET
    }

    fun clearResumePosition() {
        resumeWindow = C.INDEX_UNSET
        resumePosition = C.TIME_UNSET
    }

    fun isBehindLiveWindow(e: ExoPlaybackException): Boolean {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false
        }
        var cause: Throwable? = e.sourceException
        while (cause != null) {
            if (cause is BehindLiveWindowException) {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    override fun onVisibilityChange(visibility: Int) {
        // layoutPlayer.setVisibility(View.VISIBLE);

        simpleExoPlayerView.requestFocus()
        simpleExoPlayerView.showController()
    }

    fun releasePlayer() {
        if (player != null) {
            updateResumePosition()
            player!!.release()
            player = null
            trackSelector = null


        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Show the controls on any key event.
        simpleExoPlayerView.showController()
        // If the event was not handled then see if the player view can handle it as a media key event.
        return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()

        releasePlayer()
    }
}



