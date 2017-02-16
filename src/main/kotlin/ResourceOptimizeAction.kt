import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import utils.findChildByRelativePath
import utils.findFilesRecursive
import utils.isCommentOut
import utils.isMatched
import java.io.BufferedReader
import java.io.InputStreamReader
import com.intellij.openapi.ui.Messages

class ResourceOptimizeAction : AnAction() {
    private val allowValuesXmlList = listOf("colors.xml", "dimens.xml", "strings.xml", "integers.xml")

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        optimizeValuesXmlList(project)
        optimizeDrawablePngList(project)
        Messages.showMessageDialog(project, "Optimize Resources has successfully completed", "Optimize Resource Result", Messages.getInformationIcon())
    }

    private fun optimizeDrawablePngList(project: Project) {
        val appName = findAppName(project) ?: return
        findDrawablePngList(project).forEach { png ->
            val resourceName = png.name.replace(".png", "")
            val child = project.baseDir.findChildByRelativePath("/$appName/src/main") ?: return
            val files = child.findFilesRecursive()

            var isDeletable = true
            files.forEach FILES_LOOP@ { targetFile ->
                val reader = BufferedReader(InputStreamReader(targetFile.inputStream, "UTF-8"))
                var line = reader.readLine()
                while (line != null) {
                    val appResName = "R.drawable.${resourceName}"
                    val assetResName = "@drawable/${resourceName}"
                    if (line.isMatched(appResName) || line.isMatched(assetResName)) {
                        if (line.isCommentOut().not()) {
                            isDeletable = false
                            break
                        }
                    }

                    line = reader.readLine()
                }
                reader.close()
                if (isDeletable) return@FILES_LOOP
            }

            PsiManager.getInstance(project).findFile(png)?.let { file ->
                WriteCommandAction.runWriteCommandAction(project, { file.delete() })
            }
        }
    }

    private fun optimizeValuesXmlList(project: Project) {
        val appName = findAppName(project) ?: return

        findValuesXmlList(project).forEach { xml ->
            if (allowValuesXmlList.contains(xml.name).not()) return@forEach
            val resourceName = xml.name.replace("s.xml", "")
            (PsiManager.getInstance(project).findFile(xml) as XmlFile).rootTag?.findSubTags(resourceName)?.forEach {
                it.attributes.forEach { attr ->
                    val appResName = "R.$resourceName.${attr.displayValue}"
                    val assetResName = "@$resourceName/${attr.displayValue}"
                    val child = project.baseDir.findChildByRelativePath("/$appName/src/main") ?: return
                    val files = child.findFilesRecursive()

                    var isDeletable = true
                    files.forEach FILES_LOOP@ { targetFile ->
                        val reader = BufferedReader(InputStreamReader(targetFile.inputStream, "UTF-8"))
                        var line = reader.readLine()
                        while (line != null) {
                            if (line.isMatched(appResName) || line.isMatched(assetResName)) {
                                if (line.isCommentOut().not()) {
                                    isDeletable = false
                                    break
                                }
                            }

                            line = reader.readLine()
                        }
                        reader.close()
                        if (isDeletable.not()) return@FILES_LOOP
                    }

                    if (isDeletable) WriteCommandAction.runWriteCommandAction(project, { attr.parent.delete() })
                }
            }
        }
    }

    private fun findAppName(project: Project): String? {
        project.baseDir.children.forEach {
            if (it.isDirectory) {
                val child = project.baseDir.findChildByRelativePath("/${it.name}/src/main")
                if (child != null) return it.name
            }
        }

        return null
    }

    private fun findValuesXmlList(project: Project): Array<VirtualFile> {
        val appName = findAppName(project) ?: return arrayOf()
        val child = project.baseDir.findChildByRelativePath("/${appName}/src/main/res/values") ?: return arrayOf()
        return child.children
    }

    private fun findDrawablePngList(project: Project): List<VirtualFile> {
        val pngList = arrayListOf<VirtualFile>()
        val appName = findAppName(project) ?: return pngList.toList()
        val child = project.baseDir.findChildByRelativePath("/${appName}/src/main/res/drawable") ?: return pngList.toList()
        child.children.forEach { if (it.extension == "png") pngList.add(it) }
        return pngList.toList()
    }
}