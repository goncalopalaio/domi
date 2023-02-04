package com.tri10.domi.msc

object ApplicationLogger: Logger {
    private var logger: Logger? = null
    
    fun inject(logger: Logger) {
        this.logger = logger
    }

    override fun d(text: String) {
        this.logger!!.d(text)
    }

    override fun e(text: String) {
        this.logger!!.e(text)
    }
}

fun debug(text: String) = ApplicationLogger.d(text)
fun err(text: String) = ApplicationLogger.e(text)

fun profile(group: String, name: String, startOrEnd: Boolean) {
    val time = System.currentTimeMillis()
    val thread = Thread.currentThread()
    val tId = thread.id
    val tName = thread.name
    val type = if (startOrEnd) "start" else "end"

    debug("Profile|$group|$name|$tId|$tName|$type|$time")
}

