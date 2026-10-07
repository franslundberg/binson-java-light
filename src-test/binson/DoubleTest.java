package binson;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import binson.BinsonLight.ValueType;

/**
 * Tests doubles, in particular BINSON-SPEC-1.1 recommendation 6:
 * a writer should store NaN as the bit pattern 0x7ff8000000000000.
 * A reader must accept all NaN bit patterns; they are all valid Binson.
 */
public class DoubleTest {
    private static final long CANONICAL_NAN = 0x7ff8000000000000L;

    // Not final, so javac cannot fold zero/zero to the constant Double.NaN.
    private static double zero = 0.0;
    private static double minusOne = -1.0;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    BinsonLight.Writer w = new BinsonLight.Writer(out);

    // ======== Writer, NaN ========

    @Test
    public void testWriteNaN() throws IOException {
        assertWritesBits(CANONICAL_NAN, Double.NaN);
    }

    @Test
    public void testWriteNegativeQuietNaN() throws IOException {
        // x86 hardware result of 0.0/0.0.
        assertWritesBits(CANONICAL_NAN, nan(0xfff8000000000000L));
    }

    @Test
    public void testWriteNaNWithPayload() throws IOException {
        // Go's math.NaN().
        assertWritesBits(CANONICAL_NAN, nan(0x7ff8000000000001L));
    }

    @Test
    public void testWriteSignalingNaN() throws IOException {
        assertWritesBits(CANONICAL_NAN, nan(0x7ff0000000000001L));
    }

    @Test
    public void testWriteAllOnesNaN() throws IOException {
        assertWritesBits(CANONICAL_NAN, nan(0xffffffffffffffffL));
    }

    @Test
    public void testWriteRuntimeNaN() throws IOException {
        // Bit pattern of these depends on the hardware.
        assertWritesBits(CANONICAL_NAN, zero / zero);
        assertWritesBits(CANONICAL_NAN, Math.sqrt(minusOne));
    }

    @Test
    public void testWriteNaNInArrayAndFields() throws IOException {
        // {a=NaN; b=[NaN, NaN];}
        w.begin()
            .name("a").doubl(nan(0xfff8000000000000L))
            .name("b").beginArray()
                .doubl(nan(0x7ff8000000000001L))
                .doubl(Double.NaN)
            .endArray()
        .end().flush();

        assertOutput("40"
            + "140161" + "46000000000000f87f"
            + "140162" + "42" + "46000000000000f87f" + "46000000000000f87f" + "43"
            + "41");
    }

    // ======== Writer, non-NaN values must be unchanged ========

    @Test
    public void testWriteNonNaN() throws IOException {
        assertWritesBits(0x0000000000000000L, 0.0);
        assertWritesBits(0x8000000000000000L, -0.0);
        assertWritesBits(0x7ff0000000000000L, Double.POSITIVE_INFINITY);
        assertWritesBits(0xfff0000000000000L, Double.NEGATIVE_INFINITY);
        assertWritesBits(0x3ff0000000000000L, 1.0);
        assertWritesBits(0x0000000000000001L, Double.MIN_VALUE);
        assertWritesBits(0x7fefffffffffffffL, Double.MAX_VALUE);
    }

    @Test
    public void testWriteOne() throws IOException {
        // {d=1.0;}
        w.begin().name("d").doubl(1.0).end().flush();
        assertOutput("40140164" + "46000000000000f03f" + "41");
    }

    // ======== Parser, all NaN patterns are valid ========

    @Test
    public void testParseCanonicalNaN() {
        BinsonLight.Parser p = parserForDouble(CANONICAL_NAN);
        p.field("d");
        assertEquals(ValueType.DOUBLE, p.getType());
        assertTrue(Double.isNaN(p.getDouble()));
        assertEquals(CANONICAL_NAN, Double.doubleToRawLongBits(p.getDouble()));
    }

    @Test
    public void testParseNonCanonicalQuietNaNs() {
        long[] patterns = {0xfff8000000000000L, 0x7ff8000000000001L, 0xffffffffffffffffL};
        for (long bits : patterns) {
            BinsonLight.Parser p = parserForDouble(bits);
            p.field("d");
            assertEquals(ValueType.DOUBLE, p.getType());
            assertTrue(Double.isNaN(p.getDouble()));
            assertEquals(bits, Double.doubleToRawLongBits(p.getDouble()));
        }
    }

    @Test
    public void testParseSignalingNaN() {
        // Double.longBitsToDouble need not preserve the bits of a signaling NaN,
        // so only check that it is accepted as a NaN.
        BinsonLight.Parser p = parserForDouble(0x7ff0000000000001L);
        p.field("d");
        assertEquals(ValueType.DOUBLE, p.getType());
        assertTrue(Double.isNaN(p.getDouble()));
    }

    @Test
    public void testParseThenWriteGivesCanonicalNaN() throws IOException {
        BinsonLight.Parser p = parserForDouble(0xfff8000000000000L);
        p.field("d");
        assertWritesBits(CANONICAL_NAN, p.getDouble());
    }

    @Test
    public void testParseFieldAfterNaN() {
        // {a=NaN; b=1;} with a non-canonical NaN.
        BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("40"
            + "140161" + "46000000000000f8ff"
            + "140162" + "1001"
            + "41"));
        p.field("a");
        assertTrue(Double.isNaN(p.getDouble()));
        p.field("b");
        assertEquals(ValueType.INTEGER, p.getType());
        assertEquals(1, p.getInteger());
    }

    // ======== Helpers ========

    private static double nan(long bits) {
        double d = Double.longBitsToDouble(bits);
        assertTrue(Double.isNaN(d));
        return d;
    }

    /** Writes {d=value;} and checks the 8 bytes of the double. */
    private static void assertWritesBits(long expectedBits, double value) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new BinsonLight.Writer(out).begin().name("d").doubl(value).end().flush();
        assertArrayEquals(objectWithDouble(expectedBits), out.toByteArray());
    }

    private static BinsonLight.Parser parserForDouble(long bits) {
        return new BinsonLight.Parser(objectWithDouble(bits));
    }

    /** Returns the bytes of {d=X;}, where X is a double with the given bits. */
    private static byte[] objectWithDouble(long bits) {
        byte[] bytes = Hex.toBytes("40140164" + "460000000000000000" + "41");
        for (int i = 0; i < 8; i++) {
            bytes[5 + i] = (byte) (bits >>> (8 * i));
        }
        return bytes;
    }

    private void assertOutput(String hex) {
        assertArrayEquals(Hex.toBytes(hex), out.toByteArray());
    }
}
