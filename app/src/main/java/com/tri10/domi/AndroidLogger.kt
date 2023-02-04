package com.tri10.domi

import android.util.Log
import com.tri10.domi.msc.Logger

object AndroidLogger: Logger {
    override fun d(text: String) {
        Log.d("ZZZ", text)
    }

    override fun e(text: String) {
        Log.e("ZZZ", text)
    }
}