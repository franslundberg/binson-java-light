package binson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import binson.BinsonLight;
import binson.BinsonLight.ValueType;

public class ParserTest {
    @Test
    public void testEmpty() {
        byte[] buffer = Hex.toBytes("4041");
        	// new Binson();
        BinsonLight.Parser p = new BinsonLight.Parser(buffer);
        boolean gotField = p.nextField();
        assertEquals(false, gotField);
    }
    
    @Test
    public void test0() {
        byte[] buffer = Hex.toBytes("401403636964102614017a404141");
        	// new Binson()
        	//   .put("cid", 38)
        	//   .put("z", new Binson());
        
        BinsonLight.Parser p = new BinsonLight.Parser(buffer);
        boolean gotField = p.nextField();
        assertEquals(true, gotField);
        assertEquals(BinsonLight.ValueType.INTEGER, p.getType());
        assertEquals(true, p.getName().equals(new BinsonLight.StringValue("cid")));
        assertEquals(38, p.getInteger());
    }
    
    @Test
    public void test1() {
        byte[] buffer = Hex.toBytes("401403636964102614017a404141");
        	// new Binson()
        	//   .put("cid", 38)
        	//   .put("z", new Binson());
        
        BinsonLight.Parser p = new BinsonLight.Parser(buffer);
        boolean gotField = p.nextField();
        assertEquals(true, gotField);
        assertEquals(ValueType.INTEGER, p.getType());
        assertEquals(true, p.getName().equals(new BinsonLight.StringValue("cid")));
        
        gotField = p.nextField();
        assertEquals(true, gotField);
        assertEquals(ValueType.OBJECT, p.getType());
        assertEquals(true, p.getName().equals(new BinsonLight.StringValue("z")));
    }
    
    @Test
    public void example2() {
    	// From org.binson.Examples class, ex1.
    	// {cid=4;} = 0x401403636964100441
    	
    	BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("401403636964100441"));
        boolean gotField = p.nextField();
        assertEquals(true, gotField);
        assertEquals(ValueType.INTEGER, p.getType());
        assertEquals(true, p.nameEquals("cid"));
        assertEquals(4, p.getInteger());
        
        gotField = p.nextField();
        assertEquals(false, gotField);
    }
    
    @Test
    public void exampleNested() {
    	// From binson-java, src-test/org.binson.Example, Example 3:
    	//  {a={b=2;};
    	//  401401614014016210024141

    	BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("401401614014016210024141"));
        boolean gotField = p.nextField();
        assertEquals(true, gotField);
        assertEquals(true, p.nameEquals("a"));
        assertEquals(ValueType.OBJECT, p.getType());
        
        p.goIntoObject();
        gotField = p.nextField();
        assertEquals(true, gotField);
        assertEquals(true, p.nameEquals("b"));
        assertEquals(ValueType.INTEGER, p.getType());
        assertEquals(2, p.getInteger());
        p.goUpToObject();
        
        gotField = p.nextField();
        assertEquals(false, gotField);
    }
    
    @Test
    public void example4a() {
    	// From binson-java, src-test/org.binson.Example, Example 4:
    	// {a=1; b={c=3;}; d=4}
    	// 40140161100114016240140163100341140164100441
    	// Parses through 3 fields without explicitly parsing inner object.
    	
    	BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("40140161100114016240140163100341140164100441"));
    	boolean gotField;
    	
    	gotField = p.nextField();
    	assertEquals(true, gotField);
    	assertEquals(true, p.nameEquals("a"));
        assertEquals(ValueType.INTEGER, p.getType());
        assertEquals(1, p.getInteger());
        
        gotField = p.nextField();
    	assertEquals(true, gotField);
    	assertEquals(true, p.nameEquals("b"));
        assertEquals(ValueType.OBJECT, p.getType());
        
        gotField = p.nextField();
    	assertEquals(true, gotField);
    	assertEquals(true, p.nameEquals("d"));
        assertEquals(ValueType.INTEGER, p.getType());
        assertEquals(4, p.getInteger());        
    }
    
    @Test
    public void example4b() {
    	// From binson-java, src-test/org.binson.Example, Example 4:
    	// {a=1; b={c=3;}; d=4}
    	// 40140161100114016240140163100341140164100441
    	// Parses inner object.
    	
    	BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("40140161100114016240140163100341140164100441"));
    	
    	p.nextField();
    	p.nextField();
    	
    	p.goIntoObject();
        p.nextField();
    	assertEquals(true, p.nameEquals("c"));
        assertEquals(3, p.getInteger());
        p.goUpToObject();
        
    	assertEquals(true, p.nextField());
    	assertEquals(true, p.nameEquals("d"));
        assertEquals(4, p.getInteger());
        
        assertEquals(false, p.nextField());
    }
    
    @Test
    public void example4c() {
    	// From binson-java, src-test/org.binson.Example, Example 4:
    	// {a=1; b={c=3;}; d=4}
    	// 40140161100114016240140163100341140164100441
    	// Parses value 3 and 4.
    	
    	BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("40140161100114016240140163100341140164100441"));
    	
    	p.field("b");
    	p.goIntoObject();
    	p.field("c");
    	assertEquals(3, p.getInteger());
    	p.goUpToObject();
        
    	p.field("d");
    	assertEquals(4, p.getInteger());
    }
    
    @Test
    public void testNonExistantField() {
    	// Binson.FormatException should be thrown.
    	
    	byte[] buffer = Hex.toBytes("401403636964102614017a404141");
    	// new Binson()
    	//   .put("cid", 38)
    	//   .put("z", new Binson());
    
    	Exception ex = null;
	    BinsonLight.Parser p = new BinsonLight.Parser(buffer);
	    try {
	    	p.field("height");
	    } catch (BinsonLight.FormatException e) {
	    	ex = e;
	    }
	    
	    assertEquals(true, ex != null);
    }
    
    @Test
    public void testStringBeyondInputBuffer() {
    	// String length is too large. Extends beyond the input buffer.
    	// Hand-hacked incorrect "Binson".
    	//
    	// 40                begin
    	//   14 01 61        name "a" (size 1)
    	//   14 64 62 62     string "bb" with an INCORRECT length of 100 (0x64).
    	// 41                end
    	//
    	// Together: 401401611464626241
    	//
    	
    	Exception ex = null;
	    BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("401401611464626241"));
	    try {
	    	p.field("a");
	    } catch (BinsonLight.FormatException e) {
	    	ex = e;
	    }
	    
	    assertEquals(true, ex != null);
	    assertTrue(ex.getMessage().contains("extends beyond"));
	    assertTrue(ex.getMessage().contains("100"));
    }
    
    @Test
    public void testBytesBeyondInputBuffer() {
    	// Bytes length is too large. Extends beyond the input buffer.
    	// Hand-hacked incorrect "Binson".
    	//
    	// 40                begin
    	//   14 01 61        name "a" (size 1)
    	//   18 64 07 07     bytes string 0x0707 with an INCORRECT length of 100 (0x64).
    	// 41                end
    	//
    	// Together: 401401611864070741
    	//
    	
    	Exception ex = null;
	    BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("401401611864070741"));
	    try {
	    	p.field("a");
	    } catch (BinsonLight.FormatException e) {
	    	ex = e;
	    }
	    
	    assertEquals(true, ex != null);
	    assertTrue(ex.getMessage().contains("length of bytes"));
	    assertTrue(ex.getMessage().contains("extends beyond"));
	    assertTrue("contains100", ex.getMessage().contains("100"));
    }
}
