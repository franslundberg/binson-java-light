package binson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import binson.BinsonLight;

public class WriterTest {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    BinsonLight.Writer w = new BinsonLight.Writer(out);
    
    @Test
    public void testEmptyBinsonObject() throws IOException {
        // {}  -- empty Binson object
        // 0x4041    

        w.begin().end().flush();
        
        byte[] bytes = out.toByteArray();
        assertEquals(2, bytes.length);
        assertEquals(0x40, bytes[0]);
        assertEquals(0x41, bytes[1]);
    }
    
    @Test
    public void testEx1() throws IOException {
        // From org.binson.Examples class, ex1.
        // {cid=4;} = 0x401403636964100441
        
        w.begin().name("cid").integer(4).end().flush();
        assertOutput("401403636964100441");
    }
    
    @Test
    public void testEx3() throws IOException {
        // From binson-java, src-test/org.binson.Example, Example 3:
        //  {a={b=2};};
        //  401401614014016210024141
        
        w.begin().name("a").begin().name("b").integer(2).end().end().flush();
        assertOutput("401401614014016210024141");
    }
    
    @Test
    public void testEx4() throws IOException {
        // From binson-java, src-test/org.binson.Example, Example 4:
        // {a=1; b={c=3;}; d=4}
        // 40140161100114016240140163100341140164100441
        
        w.begin()
            .name("a").integer(1)
            .name("b").begin()
                .name("c").integer(3)
            .end()
            .name("d").integer(4)
        .end().flush();
        
        assertOutput("40140161100114016240140163100341140164100441");
    }
    
    
    @Test
    public void testArrayInArray() throws IOException {
        //  java-binson, org.binson.Examples, ex7, array
        //  {a=1; b=[10, [100, 101], 20]; c=3}
        //  40140161100114016242100a421064106543101443140163100341
        
        w.begin()
            .name("a").integer(1)
            .name("b").beginArray()
                .integer(10)
                .beginArray().integer(100).integer(101).endArray()
                .integer(20)
            .endArray()
            .name("c").integer(3)
        .end().flush();
        
        assertOutput("40140161100114016242100a421064106543101443140163100341");
    }
    
    private void assertOutput(String hex) {
        assertArrayEquals(Hex.toBytes(hex), out.toByteArray());
    }
}
