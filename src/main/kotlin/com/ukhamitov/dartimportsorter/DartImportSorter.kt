package com.ukhamitov.dartimportsorter

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.ukhamitov.dartimportsorter.model.ImportInfo
import java.io.IOException

open class DartImportSorter : AnAction() {

    private val regex = Regex("""import\s+(.+?)(?:\s+as\s+(.+?))?(?:\s+show\s+(.+?))?(?:\s+hide\s+(.+?))?;""")

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        (project != null).also { e.presentation.isEnabledAndVisible = it }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val editor: Editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val document: Document = editor.document
        val file: VirtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
        if (file.extension != "dart") return

        try {
            val local: String = DartImportSorterSettingsConfigurable.getOptionTextString(project, DartImportSorterOptionsForm.LOCAL_PREFIX)
            val parsedImports = parseDartFile(document.text)
            val (start, finish) = importBlockPosition(document.text)
            val formattedImports = formatImports(parsedImports, local)
            val sortedImportStr = formattedImports.map { importInfo ->
                if (importInfo.library.isNotEmpty())
                    buildImportString(importInfo)
                else
                    ""
            }
            val newFileContents = document.text.replaceRange(start, finish, sortedImportStr.joinToString("\n"))
            val cmd = Runnable {
                document.setReadOnly(false)
                document.setText(newFileContents)
            }
            WriteCommandAction.runWriteCommandAction(project, cmd)
        } catch (err : IOException) {
            err.printStackTrace()
        }
    }

    internal fun parseDartFile(dartString: String): List<ImportInfo> {
        return dartString.lines().mapNotNull { line -> parseImport(line) }
    }

    private fun parseImport(line: String): ImportInfo? {
        val match = regex.find(line) ?: return null
        val (library, alias, show, hide) = match.destructured
        return ImportInfo(library, alias, show.split(",").map { it.trim() }, hide.split(",").map { it.trim() })
    }

    internal fun importBlockPosition(dartString: String): Pair<Int, Int> {
        var start = -1
        var finish = -1
        regex.findAll(dartString).forEach { match ->
            if (start == -1)
                start = match.range.first
            finish = match.range.last + 1
        }
        return Pair(start, finish)
    }

    internal fun formatImports(imports: List<ImportInfo>, local: String): List<ImportInfo> {
        val results = ArrayList<ImportInfo>()
        var needEmptyLine = false
        val groups: MutableMap<Int, ArrayList<ImportInfo>> = HashMap()

        val dartLib = ArrayList<ImportInfo>()
        val flutterLib = ArrayList<ImportInfo>()
        val extLib = ArrayList<ImportInfo>()
        val locLib = ArrayList<ImportInfo>()
        val relLib = ArrayList<ImportInfo>()

        for (imp in imports) {
            when (group(imp.library, local)) {
                DART_LIB -> dartLib.add(imp)
                FLUTTER_LIB -> flutterLib.add(imp)
                EXTERNAL_LIB -> extLib.add(imp)
                LOCAL_LIB -> locLib.add(imp)
                RELATIVE_LIB -> relLib.add(imp)
            }
        }

        groups[DART_LIB] = dartLib
        groups[FLUTTER_LIB] = flutterLib
        groups[EXTERNAL_LIB] = extLib
        groups[LOCAL_LIB] = locLib
        groups[RELATIVE_LIB] = relLib

        val sortedGroups = groups.toSortedMap()

        for ((_: Int, groupImports: ArrayList<ImportInfo>) in sortedGroups) {
            if (groupImports.isEmpty()) {
                continue
            }
            groupImports.sortWith { a, b -> a.library.compareTo(b.library) }
            if (needEmptyLine) {
                results.add(ImportInfo(""))
            }
            results.addAll(groupImports)
            needEmptyLine = true
        }

        return results
    }

    private fun group(importString: String, localPrefix: String): Int {
        val path: String = importPath(importString)

        return if (importString.contains(Regex("^'dart:.*\$"))) {
            DART_LIB
        } else  if (importString.contains(Regex("^'package:flutter/.*\$"))) {
            FLUTTER_LIB
        } else if (path.isNotEmpty() && path.contains(localPrefix, false)) {
            LOCAL_LIB
        } else {
            EXTERNAL_LIB
        }
    }

    internal fun buildImportString(importInfo: ImportInfo): String {
        val strBuilder = StringBuilder()
        strBuilder.append("import")
        strBuilder.append(" ")
        strBuilder.append(importInfo.library)
        val aliasStr = importInfo.alias
        if (aliasStr != null && aliasStr != "") {
            strBuilder.append(" as $aliasStr")
        }
        val showStr = importInfo.show?.joinToString(", ")
        if (showStr != null && showStr != "") {
            strBuilder.append(" show $showStr")
        }
        val hideStr = importInfo.hide?.joinToString(", ")
        if (hideStr != null && hideStr != "") {
            strBuilder.append(" hide $hideStr")
        }
        strBuilder.append(";")
        return  strBuilder.toString()
    }

    private fun importPath(str: String): String {
        val groups = str.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return groups[groups.size - 1]
    }

    companion object {
        const val DART_LIB = 10
        const val FLUTTER_LIB = 20
        const val EXTERNAL_LIB = 30
        const val LOCAL_LIB = 40
        const val RELATIVE_LIB = 50
    }
}