package binson;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import binson.BinsonLight;

/**
 * Tests parsing arrays.
 * 
 * @author Frans Lundberg
 */
public class ParseArrayTest {

	@Test
	public void test1() {
		// From binson-java, org.binson.Examples, ex5, simple array
		//  {a=[1, "hello"];}
		//  40140161421001140568656c6c6f4341
		
		BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("40140161421001140568656c6c6f4341"));
		p.field("a");
		p.goIntoArray();
		
		boolean gotValue = p.nextArrayValue();
		assertEquals(true, gotValue);
		assertEquals(BinsonLight.TYPE_INTEGER, p.type);
		assertEquals(1, p.integerValue);
		
		gotValue = p.nextArrayValue();
		assertEquals(true, gotValue);
		assertEquals(BinsonLight.TYPE_STRING, p.type);
		assertEquals("hello", p.stringValue.toString());
	}
	
	@Test
	public void testSkipArrayField() {
		// {a=1; b=[10,20]; c=3}
		// 40140161100114016242100a101443140163100341
		//
		
		BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("40140161100114016242100a101443140163100341"));
		
		p.field("a");
		assertEquals(1, p.integerValue);
		p.field("c");
		assertEquals(3, p.integerValue);
	}
	
	@Test
	public void testArrayFieldInTheMiddle1() {
		// {a=1; b=[10,20]; c=3}
		// 40140161100114016242100a101443140163100341
		//
		
		BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("40140161100114016242100a101443140163100341"));
		
		p.field("b");
		p.goIntoArray();
		p.nextArrayValue();
		assertEquals(10, p.integerValue);
		p.nextArrayValue();
		assertEquals(20, p.integerValue);
		p.goUpToObject();
		
		p.field("c");
		assertEquals(3, p.integerValue);
	}
	
	@Test
	public void testArrayFieldInTheMiddle2() {
		// Like previous version, but p.goUpToObject() called in the middle of the array.
		
		BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("40140161100114016242100a101443140163100341"));
		
		p.field("b");
		p.goIntoArray();
		p.nextArrayValue();
		assertEquals(10, p.integerValue);
		p.goUpToObject();
		
		p.field("c");
		assertEquals(3, p.integerValue);
	}
	
	@Test
	public void testArrayInArray1() {
		//  java-binson, org.binson.Examples, ex7, array
		//  {a=1; b=[10, [100, 101], 20]; c=3}
		//  40140161100114016242100a421064106543101443140163100341
		
		BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("40140161100114016242100a421064106543101443140163100341"));
		boolean gotValue;
		
		p.field("b");
		p.goIntoArray();
		
		gotValue = p.nextArrayValue();
		assertEquals(true, gotValue);
		assertEquals(10, p.integerValue);
		
		gotValue = p.nextArrayValue();
		assertEquals(true, gotValue);
		assertEquals(BinsonLight.TYPE_ARRAY, p.type);
		
		p.goIntoArray();
		gotValue = p.nextArrayValue();
		assertEquals(true, gotValue);
		assertEquals(BinsonLight.TYPE_INTEGER, p.type);
		assertEquals(100, p.integerValue);
		
		gotValue = p.nextArrayValue();
		assertEquals(true, gotValue);
		assertEquals(BinsonLight.TYPE_INTEGER, p.type);
		assertEquals(101, p.integerValue);
		
		p.goUpToArray();
		
		gotValue = p.nextArrayValue();
		assertEquals(true, gotValue);
		assertEquals(20, p.integerValue);		
	}
}
