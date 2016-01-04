package binson;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import binson.BinsonLight;

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
        assertEquals(BinsonLight.TYPE_INTEGER, p.type);
        assertEquals(true, p.name.equals(new BinsonLight.StringValue("cid")));
        assertEquals(38, p.integerValue);
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
        assertEquals(BinsonLight.TYPE_INTEGER, p.type);
        assertEquals(true, p.name.equals(new BinsonLight.StringValue("cid")));
        
        gotField = p.nextField();
        assertEquals(true, gotField);
        assertEquals(BinsonLight.TYPE_OBJECT, p.type);
        assertEquals(true, p.name.equals(new BinsonLight.StringValue("z")));
    }
    
    @Test
    public void example2() {
    	// From org.binson.Examples class, ex1.
    	// {cid=4;} = 0x401403636964100441
    	
    	BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("401403636964100441"));
        boolean gotField = p.nextField();
        assertEquals(true, gotField);
        assertEquals(BinsonLight.TYPE_INTEGER, p.type);
        assertEquals(true, p.nameEquals("cid"));
        assertEquals(4, p.integerValue);
        
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
        assertEquals(BinsonLight.TYPE_OBJECT, p.type);
        
        BinsonLight.Parser pi = p.parser();
        gotField = pi.nextField();
        assertEquals(true, gotField);
        assertEquals(true, pi.nameEquals("b"));
        assertEquals(BinsonLight.TYPE_INTEGER, pi.type);
        assertEquals(2, pi.integerValue);
        gotField = pi.nextField();
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
        assertEquals(BinsonLight.TYPE_INTEGER, p.type);
        assertEquals(1, p.integerValue);
        
        gotField = p.nextField();
    	assertEquals(true, gotField);
    	assertEquals(true, p.nameEquals("b"));
        assertEquals(BinsonLight.TYPE_OBJECT, p.type);
        
        gotField = p.nextField();
    	assertEquals(true, gotField);
    	assertEquals(true, p.nameEquals("d"));
        assertEquals(BinsonLight.TYPE_INTEGER, p.type);
        assertEquals(4, p.integerValue);        
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
        assertEquals(3, p.integerValue);
        p.goUpToObject();
        
    	assertEquals(true, p.nextField());
    	assertEquals(true, p.nameEquals("d"));
        assertEquals(4, p.integerValue);
        
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
    	assertEquals(3, p.integerValue);
    	p.goUpToObject();
        
    	p.field("d");
    	assertEquals(4, p.integerValue);
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
}
