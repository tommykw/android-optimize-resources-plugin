import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import utils.findChildByRelativePath
import utils.findFilesRecursive
import utils.isCommentOut
import utils.isMatched
import java.io.BufferedReader
import java.io.InputStreamReader

class ResourceOptimizeIntention : IntentionAction {
    private val valuesXmlList = listOf("colors.xml", "dimens.xml", "strings.xml", "styles.xml")
    private var assetType = ""
    override fun getFamilyName() = text
    override fun getText() = "Optimize resources"
    override fun startInWriteAction() = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file is XmlFile
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file !is XmlFile) return
        if (valuesXmlList.contains(file.containingFile.name).not()) return
        assetType = file.containingFile.name.replace("s.xml", "")
        val appDirIndex = project.baseDir.canonicalPath.toString().split("/").size
        val appDirName = file.containingDirectory.toString().split("/")[appDirIndex]

        file.rootTag?.findSubTags(assetType)?.map {
            it.attributes.map { attr ->
                val appResName = "R.$assetType.${attr.displayValue}"
                val assetResName = "@$assetType/${attr.displayValue}"
                val child = project.baseDir.findChildByRelativePath("/$appDirName/src/main") ?: return
                val files = child.findFilesRecursive()

                var isDeletable = true
                files.forEach READ_WHOLE_FILES@ { targetFile ->
                    val reader = BufferedReader(InputStreamReader(targetFile.inputStream, "UTF-8"))
                    var line = reader.readLine()
                    while (line != null) {
                        println(line)
                        if (line.isMatched(appResName) || line.isMatched(assetResName)) {
                            if (line.isCommentOut().not()) {
                                isDeletable = false
                                break
                            }
                        }

                        line = reader.readLine()
                    }
                    reader.close()

                    if (isDeletable) return@READ_WHOLE_FILES
                }

                if (isDeletable) attr.parent.delete()
            }
        }
    }
}