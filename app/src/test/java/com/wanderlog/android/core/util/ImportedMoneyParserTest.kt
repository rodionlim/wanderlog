package com.wanderlog.android.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ImportedMoneyParserTest {

    @Test
    fun `parseAmount keeps four digit totals with decimals`() {
        val parsed = ImportedMoneyParser.parseAmount("AUD 1962.20")
        assertNotNull(parsed)
        assertEquals(1962.20, parsed!!, 0.0001)
    }

    @Test
    fun `parseAmount handles thousands separators`() {
        val parsed = ImportedMoneyParser.parseAmount("Total: 1,962.20")
        assertNotNull(parsed)
        assertEquals(1962.20, parsed!!, 0.0001)
    }

    @Test
    fun `parseAmount returns null when no amount exists`() {
        assertNull(ImportedMoneyParser.parseAmount("price unavailable"))
    }
}
