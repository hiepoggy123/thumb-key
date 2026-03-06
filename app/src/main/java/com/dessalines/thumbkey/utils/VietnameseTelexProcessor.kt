package com.dessalines.thumbkey.utils

import android.view.KeyEvent
import com.dessalines.thumbkey.IMEService
import com.dessalines.thumbkey.textprocessors.TextProcessor

class VietnameseTelexProcessor : TextProcessor {

    override fun handleCommitText(ime: IMEService, input: CharSequence) {
        val ic = ime.currentInputConnection
        val inputChar = input[0]

        val extracted = ic.getTextBeforeCursor(20, 0)?.toString() ?: ""
        val wordRegex = Regex("[a-zA-ZÀ-ỹ]+\$")
        val match = wordRegex.find(extracted)
        
        if (match != null) {
            val oldWord = match.value
            val newWord = processTelex(oldWord + inputChar)
            if (newWord != oldWord + inputChar) {
                ic.deleteSurroundingText(oldWord.length, 0)
                ic.commitText(newWord, 1)
                return
            }
        }
        
        ic.commitText(input, 1)
    }

    override fun handleKeyEvent(ime: IMEService, ev: KeyEvent) {
        ime.currentInputConnection.sendKeyEvent(ev)
    }

    override fun handleFinishInput(ime: IMEService) {}

    override fun handleCursorUpdate(
        ime: IMEService,
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int
    ) {}

    override fun updateCursorPosition(ime: IMEService) {}

    private val VOWELS = "aAăĂâÂeEêÊiIoOôÔơƠuUưƯyY"
    private val TONES = listOf(
        "aáàảãạ", "AÁÀẢÃẠ",
        "ăắằẳẵặ", "ĂẮẰẲẴẶ",
        "âấầẩẫậ", "ÂẤẦẨẪẬ",
        "eéèẻẽẹ", "EÉÈẺẼẸ",
        "êếềểễệ", "ÊẾỀỂỄỆ",
        "iíìỉĩị", "IÍÌỈĨỊ",
        "oóòỏõọ", "OÓÒỎÕỌ",
        "ôốồổỗộ", "ÔỐỒỔỖỘ",
        "ơớờởỡợ", "ƠỚỜỞỠỢ",
        "uúùủũụ", "UÚÙỦŨỤ",
        "ưứừửữự", "ƯỨỪỬỮỰ",
        "yýỳỷỹỵ", "YÝỲỶỸỴ"
    )

    private fun getTone(c: Char): Int {
        for (row in TONES) {
            val idx = row.indexOf(c)
            if (idx != -1) return idx
        }
        return 0
    }

    private fun setTone(c: Char, tone: Int): Char {
        for (row in TONES) {
            val idx = row.indexOf(c)
            if (idx != -1) return row[tone]
        }
        return c
    }

    private fun getBase(c: Char): Char {
        for (row in TONES) {
            val idx = row.indexOf(c)
            if (idx != -1) return row[0]
        }
        return c
    }

    private fun isUpper(c: Char): Boolean {
        for (row in TONES) {
            if (row.indexOf(c) != -1) return row[0].isUpperCase()
        }
        if (c in 'A'..'Z') return true
        return c.isUpperCase()
    }

    private fun processTelex(word: String): String {
        if (word.isEmpty()) return word
        val lastChar = word.last()
        val prefix = word.dropLast(1)
        if (prefix.isEmpty()) return word
        
        val pLast = prefix.last()
        val lc = lastChar.lowercaseChar()
        val pBase = getBase(pLast).lowercaseChar()
        
        fun replaceLast(newBase: Char): String {
            val isUp = isUpper(pLast)
            val c = if (isUp) newBase.uppercaseChar() else newBase.lowercaseChar()
            val tone = getTone(pLast)
            return prefix.dropLast(1) + setTone(c, tone)
        }

        if (lc == 'a' && pBase == 'a') {
            if (pLast.lowercaseChar() == 'â' || pLast.lowercaseChar() == 'ă') return replaceLast('a') + lastChar
            return replaceLast('â')
        }
        if (lc == 'w' && pBase == 'a') {
            if (pLast.lowercaseChar() == 'â' || pLast.lowercaseChar() == 'ă') return replaceLast('a') + lastChar
            return replaceLast('ă')
        }
        if (lc == 'e' && pBase == 'e') {
            if (pLast.lowercaseChar() == 'ê') return replaceLast('e') + lastChar
            return replaceLast('ê')
        }
        if (lc == 'o' && pBase == 'o') {
            if (pLast.lowercaseChar() == 'ô' || pLast.lowercaseChar() == 'ơ') return replaceLast('o') + lastChar
            return replaceLast('ô')
        }
        if (lc == 'w' && pBase == 'o') {
            if (pLast.lowercaseChar() == 'ô' || pLast.lowercaseChar() == 'ơ') return replaceLast('o') + lastChar
            return replaceLast('ơ')
        }
        if (lc == 'w' && pBase == 'u') {
            if (pLast.lowercaseChar() == 'ư') return replaceLast('u') + lastChar
            return replaceLast('ư')
        }
        if (lc == 'd' && pLast.lowercaseChar() == 'đ') {
            val isUp = pLast.isUpperCase()
            return prefix.dropLast(1) + (if (isUp) 'D' else 'd') + lastChar
        }
        if (lc == 'd' && pLast.lowercaseChar() == 'd') {
            val isUp = pLast.isUpperCase()
            return prefix.dropLast(1) + (if (isUp) 'Đ' else 'đ')
        }
        
        if (lc == 'w' && !VOWELS.contains(pBase) && prefix.none { VOWELS.contains(it) }) {
            val isUpLast = lastChar.isUpperCase()
            return prefix + (if (isUpLast) 'Ư' else 'ư')
        }

        val toneMap = mapOf('s' to 1, 'f' to 2, 'r' to 3, 'x' to 4, 'j' to 5, 'z' to 0)
        if (toneMap.containsKey(lc)) {
            val targetTone = toneMap[lc]!!
            val mainVowelIdx = findMainVowel(prefix)
            if (mainVowelIdx != -1) {
                val c = prefix[mainVowelIdx]
                if (getTone(c) != targetTone) {
                    return prefix.substring(0, mainVowelIdx) + setTone(c, targetTone) + prefix.substring(mainVowelIdx + 1)
                } else if (targetTone == 0) {
                    return prefix.substring(0, mainVowelIdx) + getBase(c) + prefix.substring(mainVowelIdx + 1)
                } else {
                    return prefix.substring(0, mainVowelIdx) + getBase(c) + prefix.substring(mainVowelIdx + 1) + lastChar
                }
            }
        }
        
        return word
    }

    private fun findMainVowel(word: String): Int {
        val vowelsInWord = mutableListOf<Int>()
        for (i in word.indices) {
            if (VOWELS.contains(word[i])) vowelsInWord.add(i)
        }
        if (vowelsInWord.isEmpty()) return -1
        if (vowelsInWord.size == 1) return vowelsInWord[0]
        
        val v1 = getBase(word[vowelsInWord[0]]).lowercaseChar()
        val v2 = getBase(word[vowelsInWord[1]]).lowercaseChar()
        
        if (vowelsInWord[0] > 0 && (word[vowelsInWord[0]-1].lowercaseChar() == 'q' || word[vowelsInWord[0]-1].lowercaseChar() == 'g')) {
            return vowelsInWord[1]
        }
        if ("$v1$v2" in listOf("oa", "oe", "uy", "ue", "ua")) return vowelsInWord[1]
        return vowelsInWord[0]
    }
}
