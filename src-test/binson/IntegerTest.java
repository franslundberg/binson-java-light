package binson;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import binson.BinsonLight;

public class IntegerTest {
	@Test
	public void test0() {
		// binson-java, org.binson.Examples, ex7, array
		//  {a=1; b=-1; c=250; d=Integer.MAX_VALUE, f=Integer.MIN_VALUE
		//  40140161100114016210ff14016311fa0014016412ffffff7f14016613ffffffffffffff7f41
		
		BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("40140161100114016210ff14016311fa0014016412ffffff7f14016613ffffffffffffff7f41"));
		
		p.field("a");
		assertEquals(1, p.getInteger());
		
		p.field("b");
		assertEquals(-1, p.getInteger());

		p.field("c");
		assertEquals(250, p.getInteger());

		p.field("d");
		assertEquals(Integer.MAX_VALUE, p.getInteger());

		p.field("f");
		assertEquals(Long.MAX_VALUE, p.getInteger());
	}
	
	@Test
	public void testInt250() {
		// ex9, int value = 250
		// {aaaa=250}
		// 4014046161616111fa0041
		
		BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("4014046161616111fa0041"));
		p.nextField();
		assertEquals(250, p.getInteger());
	}
	
	@Test
	public void testWriteAndParseInt1() throws IOException {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    BinsonLight.Writer w = new BinsonLight.Writer(out);
	    
	    w.begin()
	        .name("i1").integer(12)
	        .name("i2").integer(-1)
	        .name("i3").integer(127)
	        .name("i4").integer(-128)
	    .end().flush();
	    
	    BinsonLight.Parser p = new BinsonLight.Parser(out.toByteArray());
	    
	    p.field("i1");
	    assertEquals(12, p.getInteger());
	    p.field("i2");
        assertEquals(-1, p.getInteger());
        p.field("i3");
        assertEquals(127, p.getInteger());
        p.field("i4");
        assertEquals(-128, p.getInteger());
	}
	
    @Test
    public void testWriteAndParseInt2() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinsonLight.Writer w = new BinsonLight.Writer(out);
        
        w.begin()
            .name("i1").integer(12345)
            .name("i2").integer(-2000)
            .name("i3").integer(32767)
            .name("i4").integer(-32768)
        .end().flush();
        
        BinsonLight.Parser p = new BinsonLight.Parser(out.toByteArray());
        
        p.field("i1");
        assertEquals(12345, p.getInteger());
        p.field("i2");
        assertEquals(-2000, p.getInteger());
        p.field("i3");
        assertEquals(32767, p.getInteger());
        p.field("i4");
        assertEquals(-32768, p.getInteger());
    }
    
    @Test
    public void testWriteAndParseInt4() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinsonLight.Writer w = new BinsonLight.Writer(out);
        
        w.begin()
            .name("i1").integer(123456)
            .name("i2").integer(-2000000)
            .name("i3").integer(2147483647)
            .name("i4").integer(-2147483648)
        .end().flush();
        
        BinsonLight.Parser p = new BinsonLight.Parser(out.toByteArray());
        
        p.field("i1");
        assertEquals(123456, p.getInteger());
        p.field("i2");
        assertEquals(-2000000, p.getInteger());
        p.field("i3");
        assertEquals(2147483647, p.getInteger());
        p.field("i4");
        assertEquals(-2147483648, p.getInteger());
    }
    

    @Test
    public void testWriteAndParseInt8() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinsonLight.Writer w = new BinsonLight.Writer(out);
        
        w.begin()
            .name("i1").integer(123456789012L)
            .name("i2").integer(-20000000000L)
            .name("i3").integer(Long.MAX_VALUE)
            .name("i4").integer(Long.MIN_VALUE)
        .end().flush();
        
        BinsonLight.Parser p = new BinsonLight.Parser(out.toByteArray());
        
        p.field("i1");
        assertEquals(123456789012L, p.getInteger());
        p.field("i2");
        assertEquals(-20000000000L, p.getInteger());
        p.field("i3");
        assertEquals(Long.MAX_VALUE, p.getInteger());
        p.field("i4");
        assertEquals(Long.MIN_VALUE, p.getInteger());
    }
}
