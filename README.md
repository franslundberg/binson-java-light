binson-java-light
=================

A light-weight one-file Java implementation of a Binson parser and writer.
Binson is like JSON, but faster, binary and even simpler. See [binson.org](http://binson.org/).


Install
=======

Copy `src/org/binson/light/Binson.java` to your Java project.


Code examples
=============

**Example 1**. The code below first creates Binson bytes with two fields: 
one integer named 'a' and one string named 's'. Then the bytes are parsed to 
retrieve the original values.

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
    System.out.println(p.integerValue);    // -> 123
    p.field("s");
    System.out.println(p.stringValue);     // -> Hello world!
        
**Example 2**. This example demonstrates how a nested Binson object 
can be handled.

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
    System.out.println(p.integerValue);    // -> 2
    p.goUpToObject();
    p.field("c");
    System.out.println(p.integerValue);    // -> 3
 
**Example 3**. This example shows how arrays are generated and parsed.
 
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
    System.out.println(p.type == BinsonLight.TYPE_INTEGER);   // -> true
    System.out.print(p.integerValue);    // -> 123
    
    gotValue = p.nextArrayValue();
    System.out.println(gotValue);        // -> true
    System.out.println(p.type == BinsonLight.TYPE_STRING);   // -> true
    System.out.print(p.stringValue);     // -> Hello world!
        
 