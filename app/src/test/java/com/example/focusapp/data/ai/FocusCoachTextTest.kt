package com.example.focusapp.data.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusCoachTextTest {

    @Test
    fun `markdown bold and star bullets become plain text`() {
        val answer = "**Great week!**\n* Focus in the morning.\n  * Keep your phone away.\n- Already plain."

        assertEquals(
            "Great week!\n- Focus in the morning.\n- Keep your phone away.\n- Already plain.",
            toPlainText(answer)
        )
    }
}
