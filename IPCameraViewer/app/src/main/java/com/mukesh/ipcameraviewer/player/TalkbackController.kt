package com.mukesh.ipcameraviewer.player

interface TalkbackController {
    val isSupported: Boolean
    fun startTalkback()
    fun stopTalkback()
}
