package com.ukhamitov.dartimportsorter

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.guessProjectForFile
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import java.util.*

class DartImportSorterSaveFileListener : FileDocumentManagerListener {

    private val myDocumentsToStripLater: MutableSet<Document> = HashSet()

    override fun beforeAllDocumentsSaving() {
        val documentsToStrip: Set<Document> = HashSet(myDocumentsToStripLater)
        myDocumentsToStripLater.clear()
        for (document in documentsToStrip) {
            sortImports(document)
        }
    }

    override fun beforeDocumentSaving(document: Document) {
        sortImports(document)
    }

    private fun sortImports(document: Document) {
        val file: VirtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
        val project = guessProjectForFile(file)?: return
        val dartImportSorter = DartImportSorter()
        val local = DartImportSorterSettingsConfigurable.getOptionTextString(project, DartImportSorterOptionsForm.LOCAL_PREFIX)
        try {
            val parsedImports = dartImportSorter.parseDartFile(document.text)
            val formattedImports = dartImportSorter.formatImports(parsedImports, local)
            val (start, finish) = dartImportSorter.importBlockPosition(document.text)
            val sortedImportStr = formattedImports.map { importInfo ->
                if (importInfo.library.isNotEmpty())
                    dartImportSorter.buildImportString(importInfo)
                else
                    ""
            }
            val newFileContents = document.text.replaceRange(start, finish, sortedImportStr.joinToString("\n"))
            CommandProcessor.getInstance().runUndoTransparentAction {
                document.setText(newFileContents)
            }
        } catch (e: IOException) {
            LOG.debug(e.message)
        }
    }

    override fun unsavedDocumentsDropped() {
        myDocumentsToStripLater.clear()
    }

    companion object {
        private val LOG = Logger.getInstance(
            DartImportSorterSaveFileListener::class.java
        )
    }
}