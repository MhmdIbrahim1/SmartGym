package com.example.smartgymapp.util

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgymapp.R
import com.example.smartgymapp.SmartGymApp
import com.example.smartgymapp.model.BookingStatus
import com.example.smartgymapp.mvvm.logError
import com.example.smartgymapp.model.UserModel
import com.example.smartgymapp.util.UiHelper.toPx
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.Locale
import kotlin.math.roundToInt

object UiHelper{
    val Int.toPx: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()
    val Float.toPx: Float get() = (this * Resources.getSystem().displayMetrics.density)
    val Int.toDp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()
    val Float.toDp: Float get() = (this / Resources.getSystem().displayMetrics.density)

    fun Activity?.navigate(@IdRes navigation: Int, arguments: Bundle? = null) {
        try {
            if (this is FragmentActivity) {
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NavHostFragment?
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

    private var _activity: WeakReference<Activity>? = null
    var activity
        get() = _activity?.get()
        private set(value) {
            _activity = WeakReference(value)
        }

    @MainThread
    fun setActivityInstance(newActivity: Activity?) {
        activity = newActivity
    }

    val SERVER_KEY = "AAAA9ko5xrQ:APA91bGj2TeIUwq4v9jloJ2sOxwBBfIdI-WWduF7lWBxnYvrf7dsuZXcDBdXt1nGHwGeIBq9yGDk4hnIHZEa0q78KGGnxi6qQv7IpwovRR6PyUDSAcMYFdxrF1S-uqwqUiDfapHZQzj7"
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

    @ColorInt
    fun Context.getResourceColor(@AttrRes resource: Int, alphaFactor: Float = 1f): Int {
        val typedArray = obtainStyledAttributes(intArrayOf(resource))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()

        if (alphaFactor < 1f) {
            val alpha = (color.alpha * alphaFactor).roundToInt()
            return Color.argb(alpha, color.red, color.green, color.blue)
        }

        return color
    }

    fun passUserModelAsIntent(intent: Intent,userModel: UserModel) {
        intent.apply {
            putExtra("userId", userModel.userId)
            putExtra("firstName", userModel.firstName)
            putExtra("lastName", userModel.lastName)
            putExtra("email", userModel.email)
            putExtra("userType", userModel.userType)
            putExtra("profile_picture", userModel.profile_picture)
            putExtra("userBookedIdsAccepted", userModel.userBookedIdsAccepted.toTypedArray())
            putExtra("userBookedIdsPending", userModel.userBookedIdsPending.toTypedArray())
            putExtra("userBookedIdsRejected", userModel.userBookedIdsRejected.toTypedArray())
            putExtra("fcmToken", userModel.fcmToken)
        }
    }

    fun getUserModelFromIntent(intent: Intent): UserModel {
        return UserModel(
            intent.getStringExtra("userId") ?: "",
            intent.getStringExtra("firstName") ?: "",
            intent.getStringExtra("lastName") ?: "",
            intent.getStringExtra("email") ?: "",
            intent.getStringExtra("userType") ?: "",
            intent.getStringExtra("profile_picture") ?: "",
            intent.getStringArrayExtra("userBookedIdsAccepted")?.toList() ?: emptyList(),
            intent.getStringArrayExtra("userBookedIdsPending")?.toList() ?: emptyList(),
            intent.getStringArrayExtra("userBookedIdsRejected")?.toList() ?: emptyList(),
            intent.getStringExtra("fcmToken") ?: ""
        )
    }

    fun showBottomNav(requireActivity: FragmentActivity) {
        requireActivity.findViewById<View>(R.id.nav_view).visibility = View.VISIBLE
    }

    fun hideBottomNav(requireActivity: FragmentActivity) {
        requireActivity.findViewById<View>(R.id.nav_view).visibility = View.GONE
    }

    fun validateEmail(email: String): RegisterValidation {
        if (email.isEmpty()) {
            return RegisterValidation.Failed("Email cannot be empty")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return RegisterValidation.Failed("Wrong email format")

        return RegisterValidation.Success
    }

    fun validatePassword(password: String): RegisterValidation {
        if (password.isEmpty()) {
            return RegisterValidation.Failed("Password cannot be empty")
        }
        if (password.length < 6) {
            return RegisterValidation.Failed("Password must be at least 6 characters")
        }
        return RegisterValidation.Success
    }

    fun callApi(jsonObject: JSONObject) {
        val json = "application/json; charset=utf-8".toMediaType()
        val client = OkHttpClient()
        val url = "https://fcm.googleapis.com/fcm/send"
        val body = RequestBody.create(json, jsonObject.toString())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer $SERVER_KEY")
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        Log.d("FCM Response", responseBody ?: "Empty response")
    }


    fun Dialog?.dismissSafe(activity: Activity?) {
        if (this?.isShowing == true && activity?.isFinishing == false) {
            this.dismiss()
        }
    }

    fun Dialog?.dismissSafe() {
        if (this?.isShowing == true && activity?.isFinishing != true) {
            this.dismiss()
        }
    }

    sealed class RegisterValidation{
        data object Success: RegisterValidation()
        data class Failed(val message: String): RegisterValidation()
    }

    data class RegisterFailedState(
        val email: RegisterValidation,
        val password: RegisterValidation
    )


    val appLanguageExceptions = hashMapOf(
        "zh-rTW" to Locale.TRADITIONAL_CHINESE
    )

    fun setLocale(context: Context?, languageCode: String?) {
        if (context == null || languageCode == null) return
        val locale = appLanguageExceptions[languageCode] ?: Locale(languageCode)
        val resources: Resources = context.resources
        val config = resources.configuration
        Locale.setDefault(locale)
        config.setLocale(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            context.createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun Context.updateLocale() {
        val settingsManager = PreferenceManager.getDefaultSharedPreferences(this)
        val localeCode = settingsManager.getString(getString(R.string.locale_key), null)
        setLocale(this, localeCode)
    }

    fun init(act: Activity) {
        setActivityInstance(act)

        val componentActivity = activity as? ComponentActivity ?: return
        componentActivity.updateLocale()
        // Ask for notification permissions on Android 13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                componentActivity,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val requestPermissionLauncher = componentActivity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                Log.d(TAG, "Notification permission: $isGranted")
            }
            requestPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }
    fun setSubArrowImageBasedOnLayoutDirection(resources: Resources, imageView: ImageView) {
        val layoutDirection = resources.configuration.layoutDirection
        val arrowDrawableResId = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            R.drawable.left_arrow_
        } else {
            R.drawable.ic_arrow
        }
        imageView.setImageResource(arrowDrawableResId)
    }

    fun Context.colorFromAttribute(attribute: Int): Int {
        val attributes = obtainStyledAttributes(intArrayOf(attribute))
        val color = attributes.getColor(0, 0)
        attributes.recycle()
        return color
    }


    fun getCurrentLocale(context: Context): String {
        val res = context.resources
        val conf = res.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            conf?.locales?.get(0)?.toString() ?: "en"
        } else {
            @Suppress("DEPRECATION")
            conf?.locale?.toString() ?: "en"
        }
    }


    val appLanguages = arrayListOf(
        /* begin language list */
        Triple("", "العربية", "ar"),
        Triple("", "English", "en")
    ).sortedBy { it.second.lowercase() }

}



