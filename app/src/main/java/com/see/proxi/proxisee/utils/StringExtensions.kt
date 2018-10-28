package com.see.proxi.proxisee.utils

fun String.firstWordCaps(): String {
    val words = this.trim().split(" ")
    val ret = StringBuilder()
    for (i in 0 until words.size) {
        if (words[i].trim().isNotEmpty()) {
            ret.append(Character.toUpperCase(words[i].trim()[0]))
            ret.append(words[i].trim().substring(1))
            if(i < words.size - 1){
                ret.append(' ')
            }
        }
    }

    return ret.toString()
}