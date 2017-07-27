package include.kotlinexoplayer

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.KeyEvent
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.ui.PlaybackControlView
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.MediaSource
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
import com.google.android.exoplayer2.drm.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.*
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util
import java.util.*

class MainActivity : AppCompatActivity(), ExoPlayer.EventListener, PlaybackControlView.VisibilityListener {

    val BANDWIDTH_METER = DefaultBandwidthMeter()
    var DEFAULT_COOKIE_MANAGER: CookieManager = CookieManager()

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


        mediaDataSourceFactory = buildDataSourceFactory()
        mainHandler = Handler()
        if (CookieHandler.getDefault() !== DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER)
        }

        simpleExoPlayerView.setControllerVisibilityListener(this)
        simpleExoPlayerView.controllerShowTimeoutMs = 500000000


        initializePlayer(true)
    }


    fun initializePlayer(shouldAutoPlay: Boolean) {

        val url = "http://storage.googleapis.com/exoplayer-test-media-0/play.mp3"

        if (player == null) {
            @DefaultRenderersFactory.ExtensionRendererMode val extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON

            val adaptiveTrackSelectionFactory: TrackSelection.Factory = AdaptiveTrackSelection.Factory(BANDWIDTH_METER)
            val drmSessionManager : DrmSessionManager<FrameworkMediaCrypto> = buildDrmSessionManager(C.CLEARKEY_UUID, url, null.toString())
            val renderersFactory : DefaultRenderersFactory = DefaultRenderersFactory(this, drmSessionManager, extensionRendererMode)

            trackSelector = DefaultTrackSelector(adaptiveTrackSelectionFactory)

            player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector)
            simpleExoPlayerView.player = player
            player!!.playWhenReady = shouldAutoPlay

            playerNeedsSource = true
        }

        if (playerNeedsSource && !TextUtils.isEmpty(url)) {
            val uri = Uri.parse(url)

            if (com.google.android.exoplayer2.util.Util.maybeRequestReadExternalStoragePermission(this, uri)) {
                return
            }

            val mediaSources = arrayOfNulls<MediaSource>(1)

            mediaSources[0] = buildMediaSource(uri, "")

            val mediaSource = mediaSources[0]

            val haveResumePosition =  resumeWindow != C.INDEX_UNSET
            if (haveResumePosition) {
                player!!.seekTo(resumeWindow, resumePosition)
            }
            player!!.prepare(mediaSource, !haveResumePosition, false)
            playerNeedsSource = false
            simpleExoPlayerView.requestFocus()
            simpleExoPlayerView.showController()

        }
    }


    fun buildDrmSessionManager(uuid: UUID , licenseUrl: String , keyRequestPropertiesArray: String): DrmSessionManager<FrameworkMediaCrypto> {
        val drmCallback: HttpMediaDrmCallback  =  HttpMediaDrmCallback(licenseUrl, buildHttpDataSourceFactory())

        for((i) in keyRequestPropertiesArray.withIndex()) {
            drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i].toString(), keyRequestPropertiesArray[i].toString())
        }

        return  DefaultDrmSessionManager(uuid, FrameworkMediaDrm.newInstance(uuid), drmCallback, null, mainHandler, null)
    }



    fun buildMediaSource(uri: Uri, overrideExtension: String): MediaSource {
        val type = if (TextUtils.isEmpty(overrideExtension))
            Util.inferContentType(uri)
        else
            Util.inferContentType("." + overrideExtension)
        when (type) {
            C.TYPE_SS -> return SsMediaSource(uri, buildDataSourceFactory(),
                    DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, null)
            C.TYPE_DASH -> return DashMediaSource(uri, buildDataSourceFactory(),
                    DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, null)
            C.TYPE_HLS -> return HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, null)
            C.TYPE_OTHER -> return ExtractorMediaSource(uri, mediaDataSourceFactory, DefaultExtractorsFactory(),
                    mainHandler, null)
            else -> {
                throw IllegalStateException("Unsupported type: " + type)
            }
        }
    }

    fun buildHttpDataSourceFactory (): HttpDataSource.Factory {
        return ((application as KotlinExoplayerApplication)).buildHttpDataSourceFactory(BANDWIDTH_METER)
    }

    fun buildDataSourceFactory(): DataSource.Factory {
        return (application as KotlinExoplayerApplication).buildDataSourceFactory(BANDWIDTH_METER)
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}


    override fun onTimelineChanged(timeline: Timeline, manifest: Any) {}

    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

    override fun onLoadingChanged(isLoading: Boolean) {}

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
    }

    override fun onPlayerError(e: ExoPlaybackException) {
        playerNeedsSource = true
        if (isBehindLiveWindow(e)) {
            clearResumePosition()
            initializePlayer(true)
        } else
            updateResumePosition()
    }

    override fun onPositionDiscontinuity() {
        if (playerNeedsSource)
            updateResumePosition()
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

    override fun onVisibilityChange(visibility: Int) {
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
        simpleExoPlayerView.showController()
        return super.dispatchKeyEvent(event) || simpleExoPlayerView.dispatchMediaKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()

        releasePlayer()
    }
}



