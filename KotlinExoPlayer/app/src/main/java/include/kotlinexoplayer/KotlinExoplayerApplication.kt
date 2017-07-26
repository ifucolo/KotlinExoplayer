package include.kotlinexoplayer

import android.app.Application
import android.support.multidex.MultiDex
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.source.MediaSource
import android.text.TextUtils
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource


/**
 * Created by iagomendesfucolo on 06/06/17.
 */
class KotlinExoplayerApplication: Application(){

    var userAgent: String? = null

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
        userAgent = com.google.android.exoplayer2.util.Util.getUserAgent(this, "ExoPlayerDemo");

    }

    fun useExtensionRenderers(): Boolean {
        return BuildConfig.FLAVOR == "withExtensions"
    }

    fun buildDataSourceFactory(bandwidthMeter: DefaultBandwidthMeter): DataSource.Factory {
        return DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter))
    }

    fun buildHttpDataSourceFactory(bandwidthMeter: DefaultBandwidthMeter): HttpDataSource.Factory {
        return DefaultHttpDataSourceFactory(userAgent, bandwidthMeter)
    }



}