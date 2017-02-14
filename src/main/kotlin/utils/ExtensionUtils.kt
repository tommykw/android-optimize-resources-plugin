package utils

import com.intellij.openapi.vfs.VirtualFile
import java.util.regex.Pattern

fun VirtualFile.findFilesRecursive(): List<VirtualFile> {
    val files = arrayListOf<VirtualFile>()
    children.forEach { currentChild ->
        if (currentChild.isDirectory) {
            currentChild.findFilesRecursive().forEach { files.add(it) }
        } else if (currentChild.extension == "kt" || currentChild.extension == "java" || currentChild.extension == "xml") {
            files.add(currentChild)
        } else {}
    }

    return files
}

tailrec fun VirtualFile.findChildByRelativePath(path: String): VirtualFile? {
    if (path.isEmpty() && path.split("/").size <= 1) return null
    val child = this.findChild(path.split("/")[1]) ?: return null

    if (path.split("/").size > 2 && child.isDirectory) {
        val len = path.split("/")[1].length
        val nextPath = path.substring(len + 1)
        return child.findChildByRelativePath(nextPath)
    } else {
        return child
    }
}

fun String.isMatched(regex: String) = Pattern.compile(regex).matcher(this).find()
fun String.isCommentOut() = Pattern.compile("^//").matcher(this.trim()).find()