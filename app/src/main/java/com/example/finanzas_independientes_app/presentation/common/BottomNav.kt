package com.example.finanzas_independientes_app.presentation.common

import android.content.Intent
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.finanzas_independientes_app.databinding.LayoutBottomNavigationBinding
import com.example.finanzas_independientes_app.presentation.analytics.AnalyticsActivity
import com.example.finanzas_independientes_app.presentation.calendar.CalendarActivity
import com.example.finanzas_independientes_app.presentation.dashboard.DashboardActivity
import com.example.finanzas_independientes_app.presentation.profile.ProfileActivity
import com.google.android.material.color.MaterialColors

/**
 * Single wiring point for the shared floating bottom navigation. Every primary
 * screen includes `layout_bottom_navigation` and calls [setup] with the tab it
 * represents and what the central FAB should do. This keeps routing, active-tab
 * tinting, the frosted pill and the gesture-bar inset in one place instead of
 * duplicated per Activity.
 *
 * The View system has no real backdrop blur — [frostPill] is an alpha scrim
 * (translucent surface colour), not a blur. Real blur lands in a later phase.
 */
object BottomNav {

    enum class Tab { HOME, CALENDAR, STATS, PROFILE }

    // Opacity of the frosted pill — matches the top-bar scrim used across screens.
    private const val FROSTED_ALPHA = 0.9f

    fun setup(
        activity: AppCompatActivity,
        nav: LayoutBottomNavigationBinding,
        active: Tab,
        onFab: () -> Unit
    ) {
        frostPill(nav)
        liftAboveGestureBar(nav)
        tintActiveTab(nav, active)

        nav.navHome.setOnClickListener { navigate(activity, active, Tab.HOME) }
        nav.navCalendar.setOnClickListener { navigate(activity, active, Tab.CALENDAR) }
        nav.navStats.setOnClickListener { navigate(activity, active, Tab.STATS) }
        nav.navUser.setOnClickListener { navigate(activity, active, Tab.PROFILE) }
        nav.cardFabAdd.setOnClickListener { onFab() }
    }

    private fun destination(tab: Tab): Class<out AppCompatActivity> = when (tab) {
        Tab.HOME -> DashboardActivity::class.java
        Tab.CALENDAR -> CalendarActivity::class.java
        Tab.STATS -> AnalyticsActivity::class.java
        Tab.PROFILE -> ProfileActivity::class.java
    }

    private fun navigate(activity: AppCompatActivity, active: Tab, target: Tab) {
        if (active == target) return
        // REORDER_TO_FRONT + SINGLE_TOP: switch tabs without stacking a fresh copy
        // of a screen that's already alive in the task.
        val intent = Intent(activity, destination(target)).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        activity.startActivity(intent)
    }

    private fun frostPill(nav: LayoutBottomNavigationBinding) {
        val card = nav.bottomNavCard
        val surface = MaterialColors.getColor(card, com.google.android.material.R.attr.colorSurface)
        card.setCardBackgroundColor(
            ColorUtils.setAlphaComponent(surface, (FROSTED_ALPHA * 255).toInt())
        )
    }

    private fun tintActiveTab(nav: LayoutBottomNavigationBinding, active: Tab) {
        val primary = MaterialColors.getColor(nav.navHome, com.google.android.material.R.attr.colorPrimary)
        val inactive = MaterialColors.getColor(nav.navHome, com.google.android.material.R.attr.colorOnSurfaceVariant)
        fun paint(view: ImageView, tab: Tab) = view.setColorFilter(if (active == tab) primary else inactive)
        paint(nav.navHome, Tab.HOME)
        paint(nav.navCalendar, Tab.CALENDAR)
        paint(nav.navStats, Tab.STATS)
        paint(nav.navUser, Tab.PROFILE)
    }

    // Lift the pill clear of the system gesture bar. The docked FAB is anchored to
    // the pill, so CoordinatorLayout moves it along automatically.
    private fun liftAboveGestureBar(nav: LayoutBottomNavigationBinding) {
        val card = nav.bottomNavCard
        val baseMarginBottom = (card.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(card) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            (v.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = baseMarginBottom + bars.bottom
            v.requestLayout()
            insets
        }
    }
}
