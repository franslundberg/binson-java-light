package binson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Examples included in README.md.
 * 
 * @author Frans Lundberg
 */
public class ReadmeExamples {
    public static void main(String[] args) throws IOException {
        new ReadmeExamples().go();
    }
    
    void go() throws IOException {
        //ex1();
        //ex2();
        ex3();
    }
    
    void ex1() throws IOException {
        //
        // {
        //   a = 123;
        //   s = "Hello world!";
        // }
        //
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinsonLight.Writer w = new BinsonLight.Writer(out);
        
        w.begin()
            .name("a").integer(123)
            .name("s").string("Hello world!")
        .end().flush();
        
        byte[] bytes = out.toByteArray();
        BinsonLight.Parser p = new BinsonLight.Parser(bytes);
        
        p.field("a");
        System.out.println(p.getInteger());    // -> 123
        p.field("s");
        System.out.println(p.getString());     // -> Hello world!
    }
    
    void ex2() throws IOException {
        //
        // {
        //   a = { b = 2; };
        //   c = 3;
        // }
        //
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinsonLight.Writer w = new BinsonLight.Writer(out);
        
        w.begin()
            .name("a").begin()
                .name("b").integer(2)
            .end()
            .name("c").integer(3)
        .end();
        
        byte[] bytes = out.toByteArray();
        BinsonLight.Parser p = new BinsonLight.Parser(bytes);
        
        p.field("a");
        p.goIntoObject();
        p.field("b");
        System.out.println(p.getInteger());    // -> 2
        p.goUpToObject();
        p.field("c");
        System.out.println(p.getInteger());    // -> 3
    }
    
    void ex3() throws IOException {
        //
        // {
        //   arr = [123, "hello"];
        // }
        //
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinsonLight.Writer w = new BinsonLight.Writer(out);
        
        w.begin()
            .name("arr").beginArray().integer(123).string("Hello world!").endArray()
        .end().flush();
        
        byte[] bytes = out.toByteArray();
        BinsonLight.Parser p = new BinsonLight.Parser(bytes);
        
        p.field("arr");
        p.goIntoArray();
        
        boolean gotValue = p.nextArrayValue();
        System.out.println(gotValue);        // -> true
        System.out.println(p.getType() == BinsonLight.ValueType.INTEGER);   // -> true
        System.out.println(p.getInteger());    // -> 123
        
        gotValue = p.nextArrayValue();
        System.out.println(gotValue);        // -> true
        System.out.println(p.getType() == BinsonLight.ValueType.STRING);   // -> true
        System.out.println(p.getString());     // -> Hello world!
    }
}
