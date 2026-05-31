package com.juzabel.mdfinder

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform