package com.example.pruebasprint0;

import com.example.pruebasprint0.LOGIC.Utilidades;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilidadesUnitTest {

    @Test
    public void bytesToInt_smallArray() {
        byte[] bytes = new byte[]{0x00, 0x05};
        int v = Utilidades.bytesToInt(bytes);
        assertEquals(5, v);
    }

    @Test
    public void bytesToHexString_nonNull() {
        byte[] bytes = new byte[]{0x01, 0x02, 0x0A};
        String hex = Utilidades.bytesToHexString(bytes);
        assertTrue(hex.contains("01:02:0a") || hex.contains("01:02:0A"));
    }

    @Test
    public void bytesToInt_nullReturnsZeroOrExceptionHandled() {
        try {
            int v = Utilidades.bytesToInt(null);
            // BigInteger on null would throw NPE; ensure method either handles or we catch it.
            assertEquals(0, v);
        } catch (Exception e) {
            // acceptable if implementation throws; test ensures predictable behavior
            assertTrue(e instanceof NullPointerException || e instanceof IllegalArgumentException);
        }
    }

    @Test(expected = Error.class)
    public void bytesToIntOK_tooManyBytesThrows() {
        byte[] bytes = new byte[]{1,2,3,4,5};
        Utilidades.bytesToIntOK(bytes);
    }
}
