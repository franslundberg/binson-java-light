package binson;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import binson.BinsonLight;
import binson.BinsonLight.ValueType;

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
		assertEquals(ValueType.INTEGER, p.getType());
		assertEquals(1, p.getInteger());
		
		gotValue = p.nextArrayValue();
		assertEquals(true, gotValue);
		assertEquals(ValueType.STRING, p.getType());
		assertEquals("hello", p.getString().toString());
	}
	
	@Test
	public void testSkipArrayField() {
		// {a=1; b=[10,20]; c=3}
		// 40140161100114016242100a101443140163100341
		//
		
		BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("40140161100114016242100a101443140163100341"));
		
		p.field("a");
		assertEquals(1, p.getInteger());
		p.field("c");
		assertEquals(3, p.getInteger());
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
		assertEquals(10, p.getInteger());
		p.nextArrayValue();
		assertEquals(20, p.getInteger());
		p.goUpToObject();
		
		p.field("c");
		assertEquals(3, p.getInteger());
	}
	
	@Test
	public void testArrayFieldInTheMiddle2() {
		// Like previous version, but p.goUpToObject() called in the middle of the array.
		
		BinsonLight.Parser p = new BinsonLight.Parser(Hex.toBytes("40140161100114016242100a101443140163100341"));
		
		p.field("b");
		p.goIntoArray();
		p.nextArrayValue();
		assertEquals(10, p.getInteger());
		p.goUpToObject();
		
		p.field("c");
		assertEquals(3, p.getInteger());
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
		assertEquals(10, p.getInteger());
		
		gotValue = p.nextArrayValue();
		assertEquals(true, gotValue);
		assertEquals(ValueType.ARRAY, p.getType());
		
		p.goIntoArray();
		gotValue = p.nextArrayValue();
		assertEquals(true, gotValue);
		assertEquals(ValueType.INTEGER, p.getType());
		assertEquals(100, p.getInteger());
		
		gotValue = p.nextArrayValue();
		assertEquals(true, gotValue);
		assertEquals(ValueType.INTEGER, p.getType());
		assertEquals(101, p.getInteger());
		
		p.goUpToArray();
		
		gotValue = p.nextArrayValue();
		assertEquals(true, gotValue);
		assertEquals(20, p.getInteger());		
	}
}
