package com.princess.royalscepter.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.card.MaterialCardView
import com.princess.royalscepter.R

fun Context.copyToClipboard(label: String, value: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(this, getString(R.string.copied_to_clipboard, label), Toast.LENGTH_SHORT).show()
}

fun Context.materialListCard(contentBuilder: LinearLayout.() -> Unit): MaterialCardView {
    val card = MaterialCardView(this).apply {
        radius = resources.getDimension(R.dimen.release_card_corner_radius)
        cardElevation = resources.getDimension(R.dimen.release_card_elevation)
        useCompatPadding = true
    }
    val content = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(28, 24, 28, 24)
        contentBuilder()
    }
    card.addView(content)
    return card
}

fun LinearLayout.addBodyText(textValue: CharSequence): TextView {
    val textView = TextView(context).apply {
        text = textValue
        setTextAppearance(android.R.style.TextAppearance_Material_Body1)
    }
    addView(textView)
    return textView
}

fun View.visibleWhen(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}
