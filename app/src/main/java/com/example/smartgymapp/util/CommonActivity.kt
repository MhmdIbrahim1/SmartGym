package com.example.smartgymapp.util

import android.app.Activity
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgymapp.R
import com.example.smartgymapp.mvvm.logError
import com.example.smartgymapp.util.UiHelper.toPx

object UiHelper{
    val Int.toPx: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()
    val Float.toPx: Float get() = (this * Resources.getSystem().displayMetrics.density)
    val Int.toDp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()
    val Float.toDp: Float get() = (this / Resources.getSystem().displayMetrics.density)

    fun Activity?.navigate(@IdRes navigation: Int, arguments: Bundle? = null) {
        try {
            if (this is FragmentActivity) {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as? NavHostFragment?
                navHostFragment?.navController?.navigate(navigation, arguments)
            }
        } catch (t: Throwable) {
            logError(t)
        }
    }



    fun hideKeyboard(view: View?) {
        if (view == null) return

        val inputMethodManager =
            view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

object CommonActivity {
    const val TAG = "COMPACT"
    var currentToast: Toast? = null
    @MainThread
    fun showToast(act: Activity?, message: String?, duration: Int? = null) {
        if (act == null || message == null) {
            Log.w(TAG, "invalid showToast act = $act message = $message")
            return
        }
        Log.i(TAG, "showToast = $message")

        try {
            currentToast?.cancel()
        } catch (e: Exception) {
            logError(e)
        }
        try {
            val inflater =
                act.getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            val layout: View = inflater.inflate(
                R.layout.toast,
                act.findViewById<View>(R.id.toast_layout_root) as ViewGroup?
            )

            val text = layout.findViewById(R.id.text) as TextView
            text.text = message.trim()

            val toast = Toast(act)
            toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 5.toPx)
            toast.duration = duration ?: Toast.LENGTH_SHORT
            toast.view = layout
            //https://github.com/PureWriter/ToastCompat
            toast.show()
            currentToast = toast
        } catch (e: Exception) {
            logError(e)
        }
    }

    sealed class NetworkResult<T>(
        val data: T? = null,
        val message: String? = null
    ){
        class Success<T>(data: T?): NetworkResult<T>(data)
        class Error<T>(message: String?):NetworkResult<T>(message = message)
        class Loading<T>(): NetworkResult<T>()
        class UnSpecified<T>(): NetworkResult<T>()
    }

    class ItemSpacingDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.left = space
            outRect.right = space
            outRect.top = space
            outRect.bottom = space
        }
    }
    fun View.isLtr() = this.layoutDirection == View.LAYOUT_DIRECTION_LTR

}



