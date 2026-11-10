/*
 * SPDX-FileCopyrightText: WitAqua
 * SPDX-License-Identifier: Apache-2.0
 */

package org.witaqua.setupwizard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.insets.ProtectionLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.google.android.setupcompat.view.StatusBarBackgroundLayout
import org.lineageos.setupwizard.R

/**
 * Full screen welcome background (raw/welcome_bg_material_you).
 *
 * It starts on pure black - where the boot animation fades out to - the WitAqua logo emerges, the
 * screen warms up to a deep teal, then the logo "drips", the surface is revealed and Material
 * shapes pop out. The welcome UI comes in once the surface is there.
 *
 * The frame numbers below must match generate.py of the animation.
 */
object WelcomeBackground {

    private const val TAG = "WelcomeBackground"

    /** First frame of the endless ambient part (marker "ambient"). */
    private const val AMBIENT_START_FRAME = 214

    /** The surface has been revealed around here: time to show the actual UI. */
    private const val CONTENT_FADE_DELAY_MS = 1800L
    private const val CONTENT_FADE_DURATION_MS = 400L

    fun attach(activity: Activity, savedInstanceState: Bundle?) {
        val view = activity.findViewById<LottieAnimationView>(R.id.welcome_background) ?: return

        // Load synchronously so that the first frame on screen is already black, not the theme
        // background.
        val config = activity.resources.configuration
        val night = config.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val composition =
            LottieCompositionFactory.fromRawResSync(
                    activity,
                    R.raw.welcome_bg_material_you,
                    "welcome_bg_${night}_${config.orientation}",
                )
                .value
        if (composition == null) {
            Log.e(TAG, "Could not load the welcome background")
            return
        }

        // The setup layout paints a strip over the status bar; let the animation show through.
        activity
            .findViewById<StatusBarBackgroundLayout>(R.id.suc_layout_status)
            ?.statusBarBackground = ColorDrawable(Color.TRANSPARENT)
        activity
            .findViewById<ProtectionLayout>(R.id.sud_layout_protection)
            ?.setProtections(emptyList())

        view.setComposition(composition)
        view.repeatCount = LottieDrawable.INFINITE
        // Never replay the intro when the (very long) ambient part wraps around.
        view.addAnimatorListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationRepeat(animation: Animator) {
                    view.setMinFrame(AMBIENT_START_FRAME)
                }
            }
        )

        if (savedInstanceState != null) {
            // Re-created (language change, rotation...): the intro has been seen already.
            view.setMinFrame(AMBIENT_START_FRAME)
            view.playAnimation()
            return
        }

        activity.findViewById<View>(R.id.sud_layout_protection)?.apply {
            alpha = 0f
            animate()
                .alpha(1f)
                .setStartDelay(CONTENT_FADE_DELAY_MS)
                .setDuration(CONTENT_FADE_DURATION_MS)
        }
        view.playAnimation()
    }
}
