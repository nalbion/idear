package com.johnlindquist.acejump

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.util.EventDispatcher
import com.maddyhome.idea.vim.helper.EditorHelper
import java.util.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class AceFinder(val project: Project, val document: DocumentImpl, val editor: EditorImpl, val virtualFile: VirtualFile) {
    companion object AceFinder {
        val ALLOWED_CHARACTERS = "1234567890";
        val END_OF_LINE = "\\n";
        val BEGINNING_OF_LINE = "^.|\\n(?<!.\\n)";
        val CODE_INDENTS = "^\\s*\\S";
        val WHITE_SPACE = "\\s+\\S(?<!^\\s*\\S)";
        val DEFAULT = CODE_INDENTS + "|" + END_OF_LINE;
    }

    val eventDispatcher: EventDispatcher<ChangeListener>? = EventDispatcher.create(ChangeListener::class.java)

    val findManager = FindManager.getInstance(project)!!
    val findModel: FindModel = createFindModel(findManager)

    var startResult: Int = 0
    var endResult: Int = 0
    var allowedCount: Int = getAllowedCharacters()!!.length
    var results: List<Int?>? = null
    var getEndOffset: Boolean = false
    var firstChar: String = ""
    var customOffset: Int = 0
    var isTargetMode: Boolean = false

    fun createFindModel(findManager: FindManager): FindModel {
        val clone = findManager.findInFileModel.clone()
        clone.isFindAll = true
        clone.isFromCursor = true
        clone.isForward = true
        clone.isRegularExpressions = false
        clone.isWholeWordsOnly = false
        clone.isCaseSensitive = false
        clone.setSearchHighlighters(true)
        clone.isPreserveCase = false

        return clone
    }

    fun findAllVisibleSymbols() : List<Int> {
        val visualLineAtTopOfScreen = EditorHelper.getVisualLineAtTopOfScreen(editor)
        val firstLine = EditorHelper.visualLineToLogicalLine(editor, visualLineAtTopOfScreen)
        var startOffset = EditorHelper.getLineStartOffset(editor, firstLine)

        val height = EditorHelper.getScreenHeight(editor)
        val top = EditorHelper.getVisualLineAtTopOfScreen(editor)

        var lastLine = top + height
        lastLine = EditorHelper.visualLineToLogicalLine(editor, lastLine)

        var endOffset = EditorHelper.normalizeOffset(editor, lastLine, EditorHelper.getLineEndOffset(editor, lastLine, true), true)

        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)

        val offsets = ArrayList<Int>()

        psiFile!!.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement?) {
                if(element?.node?.elementType?.toString().equals("IDENTIFIER")) {
                    val offset = element?.node?.textRange?.startOffset!!;
                    if(offset in startOffset..endOffset) {
                        offsets.add(element?.node?.textRange?.startOffset!!)
                    }
                }
                if(!element?.children!!.isEmpty()) {
                    element?.acceptChildren(this)
                }
            }
        });

        return offsets
    }

    fun findText(text: String, isRegEx: Boolean) {
        findModel.stringToFind = text
        findModel.isRegularExpressions = isRegEx

        val application = ApplicationManager.getApplication()
        application?.runReadAction({ results = findAllVisibleSymbols() })

        application?.invokeLater({
            var caretOffset = editor.getCaretModel().getOffset()
            var lineNumber = document.getLineNumber(caretOffset)
            var lineStartOffset = document.getLineStartOffset(lineNumber)
            var lineEndOffset = document.getLineEndOffset(lineNumber)

            results = results!!.sortedWith(object : Comparator<Int?> {
                override fun equals(other: Any?): Boolean {
                    throw UnsupportedOperationException()
                }

                override fun compare(p0: Int?, p1: Int?): Int {
                    var i1: Int = Math.abs(caretOffset - p0!!)
                    var i2: Int = Math.abs(caretOffset - p1!!)
                    var o1OnSameLine: Boolean = p0 >= lineStartOffset && p0 <= lineEndOffset
                    var o2OnSameLine: Boolean = p1 >= lineStartOffset && p1 <= lineEndOffset
                    if (i1 > i2) {
                        if (!o2OnSameLine && o1OnSameLine) {
                            return -1
                        }
                        return 1
                    } else
                        if (i1 == i2) {
                            return 0
                        } else {
                            if (!o1OnSameLine && o2OnSameLine) {
                                return 1
                            }
                            return -1
                        }
                }
            })

            startResult = 0;
            endResult = allowedCount;

            eventDispatcher?.multicaster?.stateChanged(ChangeEvent("AceFinder"));
        });
    }

    fun expandResults() {
        startResult += allowedCount
        endResult += allowedCount
        checkForReset()
    }

    fun contractResults() {
        startResult -= allowedCount
        endResult -= allowedCount
        checkForReset()
    }

    fun checkForReset() {
        if (startResult < 0) startResult = 0
        if (endResult < allowedCount) endResult = allowedCount
    }

    private fun checkFolded(offset: Int): Int {
        val foldingModelImpl = editor.foldingModel

        var offsetResult: Int = offset;
        for (foldRegion in foldingModelImpl.fetchCollapsedAt(offset).orEmpty()) {
            offsetResult = foldRegion.endOffset + 1
        }

        return offsetResult
    }

    fun addResultsReadyListener(changeListener: ChangeListener) {
        eventDispatcher?.addListener(changeListener)
    }

    fun getAllowedCharacters(): CharSequence? {
        return ALLOWED_CHARACTERS
    }

}