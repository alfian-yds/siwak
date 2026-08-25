package com.mosque.prayerclock.ui.components

import com.mosque.prayerclock.data.model.Language

/**
 * Get font scale factor based on language.
 * Indonesian and English have similar (short) text, so both get larger fonts.
 */
fun getLanguageFontScale(language: Language): Float =
    when (language) {
        Language.INDONESIAN -> 1.15f // Indonesian: 15% larger
        Language.ENGLISH -> 1.2f // English: 20% larger
    }
