package com.ukhamitov.dartimportsorter

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import java.util.*
import javax.swing.JComponent

class DartImportSorterSettingsConfigurable(project: Project) : Configurable {

    private var dartImportSorterOptionsForm: DartImportSorterOptionsForm? = null
    var propertiesComponent: PropertiesComponent = PropertiesComponent.getInstance(project)

    init {
        instance = this
    }

    override fun getDisplayName(): String {
        return "Dart Import Sorter"
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun createComponent(): JComponent? {
        DartImportSorterOptionsForm().also { dartImportSorterOptionsForm = it }
        return dartImportSorterOptionsForm!!.contentPane
    }

    override fun isModified(): Boolean {
        val textInForm = dartImportSorterOptionsForm!!.getOptionText(DartImportSorterOptionsForm.LOCAL_PREFIX)
        val savedOptionText = propertiesComponent.getValue(DartImportSorterOptionsForm.LOCAL_PREFIX)
        return textInForm != savedOptionText
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        propertiesComponent.setValue(
            DartImportSorterOptionsForm.LOCAL_PREFIX,
            dartImportSorterOptionsForm!!.getOptionText(DartImportSorterOptionsForm.LOCAL_PREFIX)
        )
    }

    override fun reset() {
        val optionId = DartImportSorterOptionsForm.LOCAL_PREFIX
        val savedOptionText = propertiesComponent.getValue(optionId)
        dartImportSorterOptionsForm!!.setOptionText(optionId, Objects.requireNonNullElse(savedOptionText, ""))
    }

    override fun disposeUIResources() {
        dartImportSorterOptionsForm = null
    }

    companion object {
        lateinit var instance: DartImportSorterSettingsConfigurable

        @JvmStatic
        fun getOptionTextString(
            project: Project?,
            optionId: String?
        ): String {
            return PropertiesComponent.getInstance(project!!).getValue(optionId!!) ?: return ""
        }
    }
}