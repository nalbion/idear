package com.jetbrains.idear.settings

import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent


// TODO: http://corochann.com/intellij-plugin-development-introduction-applicationconfigurable-projectconfigurable-873.html
class IdearConfigurable : SearchableConfigurable {
    private var gui: RecognitionSettingsForm? = null;

    override fun getId() = "preferences.IdearConfigurable"

    override fun getDisplayName() = "Recognition"

    override fun apply() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createComponent(): JComponent? {
        gui = RecognitionSettingsForm()
        return gui?.rootPanel
    }

    override fun disposeUIResources() {
        gui = null
    }

    override fun isModified(): Boolean {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return false
    }
}