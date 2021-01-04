binson-java-light
=================

A light-weight one-file Java implementation of a Binson parser and writer.
Binson is like JSON, but faster, binary and even simpler. See [binson.org](http://binson.org/).

This library is low-level and light-weight; if a more complete feature set
is needed, consider using 
[github.com/franslundberg/binson-java](https://github.com/franslundberg/binson-java) instead.


Install
=======

Copy `src/org/binson/light/Binson.java` to your Java project. That's all! The code is in the public domain, so no need
to follow specific license requirements.


Code examples
=============

Useful code examples. The source code is also available from 
`src-test/binson/ReadmeExamples.java`. NOTE: fields must be sorted on alphabetical order
(see binson.org for exact sort order) to be real Binson objects. This light-weight implementation does not check this. Invalid Binson bytes can be produced with 
this library.

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
    System.out.println(p.getInteger());    // -> 123
    p.field("s");
    System.out.println(p.getString());     // -> Hello world!
        
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
    System.out.println(p.getInteger());    // -> 2
    p.goUpToObject();
    p.field("c");
    System.out.println(p.getInteger());    // -> 3
 
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
    System.out.println(p.getType() == BinsonLight.ValueType.INTEGER);   // -> true
    System.out.println(p.getInteger());    // -> 123
    
    gotValue = p.nextArrayValue();
    System.out.println(gotValue);        // -> true
    System.out.println(p.getType() == BinsonLight.ValueType.STRING);   // -> true
    System.out.println(p.getString());     // -> Hello world!



Build
=====

Use any tool, build from source in src. No dependencies expect the standard JVM.
To run the tests, use JUnit 5 and the source files in src-test.



Versions and history
====================

* 2021 January 4. Improved error handling and documentation. Release v1.1.0.

* 2016 January 29. First complete version.

* 2016 January 4. First commit.

* 2014 May. The Binson specification (BINSON-SPEC-1) was written by Frans Lundberg. 
  See binson.org. This code follows the specification.
