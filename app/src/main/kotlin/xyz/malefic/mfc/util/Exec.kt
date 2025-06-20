package xyz.malefic.mfc.util

import java.io.File

fun String.exec(workingDir: File = File(".")): Process =
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .inheritIO()
        .start()
