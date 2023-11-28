import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

open class OnSwipeTouchListener(context: Context?) : View.OnTouchListener {
    private val gestureDetector: GestureDetector

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }

    open fun onSwiperVertical() {}
    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val distanceY = abs(e2.y - e1.y)
            if (
                abs(distanceY) > Companion.SWIPE_VELOCITY_THRESHOLD
            ) {
                onSwiperVertical()
                return true
            }
            return false
        }


    }

    companion object {
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

}