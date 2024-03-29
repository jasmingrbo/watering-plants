package ba.grbo.wateringplants.util

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class CustomImageView : AppCompatImageView {
    private var onVisibilityChangedListener: ((Int) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet?, deffStyleAttr: Int) : super(
        context,
        attributeSet,
        deffStyleAttr
    )

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (id == changedView.id) onVisibilityChangedListener?.invoke(visibility)
        super.onVisibilityChanged(changedView, visibility)
    }

    fun setOnVisibilityChangedListener(listener: ((Int) -> Unit)?) {
        onVisibilityChangedListener = listener
    }
}