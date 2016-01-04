package binson;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * A small, high-performance implementation of Binson, see binson.org.
 * 
 * Binson.Parser is used to parse a byte array to a sequence of Binson fields.
 * The parser uses a fixed and small amount of memory, no memory is allocated 
 * dynamically while parsing.
 * 
 * Binson.Writer is used to write a Binson object to an OutputStream.
 * 
 * In general, this implementation is indented to be small and high performance.
 * It is suitable for applications on small devices, for high-performance implementations, 
 * and for cases when a single public domain java source file is all that is needed instead
 * of a library dependency.
 * 
 * @author Frans Lundberg
 */
public class BinsonLight {
    public static final int TYPE_OBJECT = 100;
    public static final int TYPE_ARRAY = 101;
    public static final int TYPE_BOOLEAN = 0x44;
    public static final int TYPE_INTEGER = 0x10;
    public static final int TYPE_STRING = 0x14;
    public static final int TYPE_BYTES  = 0x18;
    
    private static final int BEGIN=0x40, END=0x41, BEGIN_ARRAY=0x42, END_ARRAY=0x43, 
        TRUE=0x44, FALSE=0x45, DOUBLE=0x46,
        INTEGER1=0x10, INTEGER2=0x11, INTEGER4=0x12, INTEGER8=0x13,
        STRING1=0x14, STRING2=0x15, STRING4=0x16, BYTES1=0x18, BYTES2=0x19, BYTES4=0x1a;
    private static final int INT_LENGTH_MASK = 0x03;
    private static final int ONE_BYTE = 0x00, TWO_BYTES = 0x01, FOUR_BYTES = 0x02, EIGHT_BYTES = 0x03;
    private static final long TWO_TO_7 = 128, TWO_TO_15 = 32768, TWO_TO_31 = 2147483648L;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	
	/**
	 * Parses a Binson object in a byte array (byte[]) to a sequence of Binson 
	 * fields. A "stream parser"; no in-memory representation is built.
	 */
	public static class Parser {
	    /** Name of last field parsed. */
		public StringValue name;
	    
		/** Type of last value parsed. One of TYPE_BOOLEAN, TYPE_INTEGER, ... */
		public int type;
		
		/** Last boolean value parsed. */
	    public boolean booleanValue;
	    
	    /** Last integer value parsed. */
	    public long integerValue;
	    
	    /** Last double value parsed. */
	    public double doubleValue;
	    
	    /** Last string value parsed. */
	    public StringValue stringValue;
	    
	    /** Last bytes value parsed. */
	    public BytesValue bytesValue;
	    
	    private static final int STATE_ZERO = 200;
	    private static final int STATE_BEFORE_FIELD = 201;
	    private static final int STATE_BEFORE_ARRAY_VALUE = 202;
	    private static final int STATE_BEFORE_ARRAY = 203;
	    private static final int STATE_END_OF_ARRAY = 204;
	    private static final int STATE_BEFORE_OBJECT = 205;
	    private static final int STATE_END_OF_OBJECT = 206;
	    
	    int state = STATE_ZERO;
	    private byte[] buffer;
	    int offset;
	    
	    public Parser(byte[] buffer) {
	        this(buffer, 0);
	    }
	    
	    public Parser(byte[] buffer, int offset) {
	        this.stringValue = new StringValue();
	        this.bytesValue = new BytesValue();
	        this.name = new StringValue();
	        this.buffer = buffer;
	        this.offset = offset;
	    }
	    
	    /**
	     * Parses until an expected field with the given name is found
	     * (without considering fields of inner objects).
	     * 
	     * @throws FormatException 
	     * 		If a field with the expected name was not found.
	     */
	    public void field(String name) {
	    	while (nextField()) {
	    		if (nameEquals(name)) {
	    			return;
	    		}
	    	}
	    	
	    	throw new FormatException("no field named '" + name + "'");
	    }
	    
	    /**
	     * Reads next field, returns true if a field was found and false
	     * if end-of-object was reached.
	     * If boolean/integer/double/bytes/string was found, the value is also read
	     * and is available in one of the fields:
	     * field booleanValue, integerValue, doubleValue, bytesValue, stringValue.
	     * 
	     * @throws IllegalStateException if end-of-object was reached already.
	     */
	    public boolean nextField() {
	        if (state == STATE_ZERO) {
	            parseBegin();
	        } else if (state == STATE_END_OF_OBJECT) {
	            throw new IllegalStateException("reached end-of-object");
	        } else if (state == STATE_BEFORE_OBJECT) {
	        	state = STATE_BEFORE_FIELD;
	            while (nextField()) {}
	            state = STATE_BEFORE_FIELD;
	        } else if (state == STATE_BEFORE_ARRAY) {
	        	state = STATE_BEFORE_ARRAY_VALUE;
	        	while (nextArrayValue()) {}	     
	        	state = STATE_BEFORE_FIELD;
	        }
	        
	        if (state != STATE_BEFORE_FIELD) {
	            throw new IllegalStateException("not ready to read a field, state: " + state);
	        }
	        
	        int typeBeforeName = readOne();
	        if (typeBeforeName == END) {
	        	state = STATE_END_OF_OBJECT;
	        	return false;
	        }
	        parseFieldName(typeBeforeName);
	        
	        int typeBeforeValue = readOne();
	        parseValue(typeBeforeValue, STATE_BEFORE_FIELD);
	        
	        return true;
	    }

		private void parseValue(int typeByte, int afterValueState) {
			switch (typeByte) {
	        case BEGIN:
	            type = TYPE_OBJECT;
	            state = STATE_BEFORE_OBJECT;
	            break;
	        case BEGIN_ARRAY:
	            type = TYPE_ARRAY;
	            state = STATE_BEFORE_ARRAY;
	            break;
	            
	        case FALSE:
	            type = TYPE_BOOLEAN;
	            booleanValue = false;
	            state = afterValueState;
	            break;
	            
	        case TRUE:
	            type = TYPE_BOOLEAN;
	            booleanValue = true;
	            state = afterValueState;
	            break;
	            
	        case DOUBLE:
	            type = DOUBLE;
	            parseDouble();
	            state = afterValueState;
	            break;
	            
	        case INTEGER1:
	        case INTEGER2:
	        case INTEGER4:
	        case INTEGER8:
	            type = TYPE_INTEGER;
	            integerValue = parseInteger(typeByte);            
	            state = afterValueState;
	            break;
	            
	        case STRING1:
	        case STRING2:
	        case STRING4:
	            type = TYPE_STRING;
	            parseString(typeByte, stringValue);
	            state = afterValueState;
	            break;
	            
	        case BYTES1:
	        case BYTES2:
	        case BYTES4:
	            type = TYPE_BYTES;
	            parseBytes(type);
	            state = afterValueState;
	            break;
	            
	        default:
	            throw new FormatException("Unexpected type byte: " + typeByte + ".");
	        }
		}

		private void parseFieldName(int typeBeforeName) {
			switch (typeBeforeName) {
	        case STRING1:
	        case STRING2:
	        case STRING4:
	            parseString(typeBeforeName, name);
	            break;
	        default:
	            throw new FormatException("unexpected type: " + typeBeforeName);
	        }
		}
	    
	    public boolean nextArrayValue() {
	    	if (state == STATE_BEFORE_ARRAY) {
	    		state = STATE_BEFORE_ARRAY_VALUE;
	    		while (nextArrayValue()) {}
	    		state = STATE_BEFORE_ARRAY_VALUE;
	    	}
	    	
	    	if (state == STATE_BEFORE_OBJECT) {
	    		state = STATE_BEFORE_FIELD;
	    		while (nextField()) {}
	    		state = STATE_BEFORE_ARRAY_VALUE;
	    	}
	    	
	    	if (state != STATE_BEFORE_ARRAY_VALUE) {
	    		throw new IllegalStateException("not before array value, " + state);
	    	}
	    	
	    	int typeByte = readOne();
	    	if (typeByte == END_ARRAY) {
	    		state = STATE_END_OF_ARRAY;
	    		return false;
	    	}
	    	
	    	parseValue(typeByte, STATE_BEFORE_ARRAY_VALUE);
	    	return true;
	    }

	    public void goIntoObject() {
	    	if (state != STATE_BEFORE_OBJECT) {
	    		throw new IllegalStateException("unexpected parser state, not an object field");
	    	}
	    	state = STATE_BEFORE_FIELD;
	    }
	    
	    public void goIntoArray() {
	    	if (state != STATE_BEFORE_ARRAY) {
	    		throw new IllegalStateException("unexpected parser state, not an array field");
	    	}
	    	state = STATE_BEFORE_ARRAY_VALUE;
	    }
	    
	    public void goUpToObject() {
	    	if (state == STATE_BEFORE_ARRAY_VALUE) {
	    		while (nextArrayValue()) {}
	    	}
	    	
	    	if (state == STATE_BEFORE_FIELD) {
	    		while (nextField()) {}
	    	}
	    	
	    	if (state != STATE_END_OF_OBJECT && state != STATE_END_OF_ARRAY) {
	    		throw new IllegalStateException("unexpected parser state, " + state);
	    	}
	    	
	    	state = STATE_BEFORE_FIELD;
	    }
	    
	    public void goUpToArray() {
	    	if (state == STATE_BEFORE_ARRAY_VALUE) {
	    		while (nextArrayValue()) {}
	    	}
	    	
	    	if (state == STATE_BEFORE_FIELD) {
	    		while (nextField()) {}
	    	}
	    	
	    	if (state != STATE_END_OF_OBJECT && state != STATE_END_OF_ARRAY) {
	    		throw new IllegalStateException("unexpected parser state, " + state);
	    	}
	    	
	    	state = STATE_BEFORE_ARRAY_VALUE;
	    }
	    
	    /**
	     * Checks whether current field name equals a provided one.
	     */
	    public boolean nameEquals(StringValue name) {
	        return this.name == null ? false : this.name.equals(name);
	    }
	    
	    public boolean nameEquals(String name) {
	    	return this.name == null ? false : this.name.toString().equals(name);
	    }
	    
	    /**
	     * Creates a parser to parse current field value.
	     * Last read field must be of type TYPE_OBJECT.
	     * 
	     * @throws IllegalStateException if the parser is not about to parse an object value.
	     */
	    public Parser parser() {
	    	if (state != STATE_BEFORE_OBJECT) {
	    		throw new IllegalStateException("not before object value");
	    	}
	    	
	    	return new Parser(buffer, offset - 1);
	    }
	    
	    private void parseDouble() {
	        doubleValue = Util.bytesToDoubleLE(buffer, offset);
	        offset += 8;
	    }
	    
	    private void parseBegin() {
	        int type = readOne();
	        if (type != BEGIN) {
	            throw new FormatException("Expected BEGIN, got " + type + ".");
	        }
	        state = STATE_BEFORE_FIELD;
	    }
	    
	    private void parseString(int type, StringValue s) {
	        int len = (int) parseInteger(type);
	        if (len < 0) throw new FormatException("Bad len, " + len + ".");
	        s.set(buffer, offset, len);
	        this.offset += len;
	    }
	    
	    private void parseBytes(int type) {
	        int len = (int) parseInteger(type);
	        if (len < 0) throw new FormatException("Bad len, " + len + ".");
	        bytesValue.set(buffer, offset, len);
	        this.offset += len;
	    }
	    
	    private long parseInteger(int type) {
	        int intType = type & INT_LENGTH_MASK;
	        long integer;
	        
	        switch (intType) {
	        case ONE_BYTE:
	            integer = buffer[offset];
	            offset++;
	            break;
	            
	        case TWO_BYTES:
	            integer = Util.bytesToShortLE(buffer, offset);
	            offset += 2;
	            break;
	            
	        case FOUR_BYTES:
	            integer = Util.bytesToIntLE(buffer, offset);
	            offset += 4;
	            break;
	            
	        case EIGHT_BYTES:
	            integer = Util.bytesToLongLE(buffer, offset);
	            offset += 8;
	            break;
	            
	        default:
	            throw new Error("never happens, intType: " + intType);
	        }
	        
	        return integer;
	    }
	    
	    private int readOne() {
	        int result = unsigned(buffer[offset]);
	        offset++;
	        return result;
	    }
	    
	
	    /**
	     * Returns an int in range [0, 255] given a byte.
	     * The byte is considered unsigned instead of the ordinary signed 
	     * interpretation in Java.
	     */
	    private static int unsigned(byte b) {
	        return b & 0x000000ff;
	    }
	}

	/**
	 * Thrown to indicate that the parsed bytes do not adhere to the expected 
	 * byte format.
	 */
	public static class FormatException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public FormatException(String message) { super(message); }
	}
	
	/**
	 * Writes Binson tokens to an OutputStream. The writer is low-level and very simple.
	 * There is no validation, an instance of this class can generate invalid Binson bytes.
	 * Make sure fields are written in alphabetical order. This is required to produce valid Binson.
	 */
	public static class Writer {
	    private OutputStream out;
	    
	    public Writer(OutputStream out) {
	        this.out = out;
	    }
	    
	    public Writer begin() throws IOException {
	        out.write(BEGIN);
	        return this;
	    }
	    
	    public Writer end() throws IOException {
	        out.write(END);
            return this;
	    }
	    
	    public Writer beginArray() throws IOException {
	        out.write(BEGIN_ARRAY);
	        return this;
	    }
	    
	    public Writer endArray() throws IOException {
	        out.write(END_ARRAY);
	        return this;
	    }
	    
	    public Writer bool(boolean value) throws IOException {
	        out.write(value == true ? TRUE : FALSE);
	        return this;
	    }
	    
	    public Writer integer(long value) throws IOException {
	        writeIntegerOrLength(TYPE_INTEGER, value);
	        return this;
	    }
	    
	    public Writer doubl(double value) throws IOException {
	        byte[] bytes = new byte[9];
	        bytes[0] = DOUBLE;
	        Util.doubleToBytesLE(value, bytes, 1);
	        out.write(bytes);
	        return this;
	    }
	    
	    public Writer string(String string) throws IOException {
	        return string(string.getBytes("UTF-8"));
	    }
	    
	    public Writer string(byte[] utf8Bytes) throws IOException {
	        writeIntegerOrLength(TYPE_STRING, utf8Bytes.length);
            out.write(utf8Bytes);
            return this;
	    }
	    
	    public Writer bytes(byte[] value) throws IOException {
	        writeIntegerOrLength(TYPE_BYTES, value.length);
	        out.write(value);
	        return this;
	    }
	    
	    public Writer name(String name) throws IOException {
	        string(name);
	        return this;
	    }
	    
	    /** Calls flush() on the OutputStream. */
	    public void flush() throws IOException {
	        out.flush();
	    }
	    
	    private void writeIntegerOrLength(int baseType, long value) throws IOException {
	        int type;
	        byte[] buffer;
	        
	        if (value >= -TWO_TO_7 && value < TWO_TO_7) {
	            type = baseType | ONE_BYTE;
	            buffer = new byte[1];
	            buffer[0] = (byte) value;
	        } else if (value >= -TWO_TO_15 && value < TWO_TO_15) {
	            type = baseType | TWO_BYTES;
	            buffer = new byte[2];
	            Util.shortToBytesLE((int) value, buffer, 0);
	        } else if (value >= -TWO_TO_31 && value < TWO_TO_31) {
	            type = baseType | FOUR_BYTES;
	            buffer = new byte[4];
	            Util.intToBytesLE((int) value, buffer, 0);
	        } else {
	            type = baseType | EIGHT_BYTES;
	            buffer = new byte[8];
	            Util.longToBytesLE(value, buffer, 0);
	        }
	        
	        out.write(type);
	        out.write(buffer);
	    }
	}
	
	/**
	 * A String represented as UTF-8 bytes. Mutable to allow memory reuse.
	 */
	public static class StringValue {
	    public byte[] buffer;
	    public int offset;
	    public int size;
	    
	    public StringValue() {
	        set(EMPTY_BYTE_ARRAY, 0, 0);
	    }
	    
	    public void set(byte[] buffer, int offset, int size) {
	        this.buffer = buffer;
	        this.offset = offset;
	        this.size = size;
	    }
	    
	    public StringValue(String s) {
	        this.offset = 0;
	        
	        try {
	            this.buffer = s.getBytes("UTF-8");
	        } catch (UnsupportedEncodingException e) {
	            throw new Error(e);
	        }
	        
	        this.size = buffer.length;
	    }
	    
	    public boolean equals(Object that) {
	        if (that == null || !(that instanceof StringValue)) {
	            return false;
	        }
	        return equals((StringValue) that);
	    }
	    
	    public boolean equals(StringValue that) {
	        if (this.size != that.size) {
	            return false;
	        }
	        
	        for (int i = 0; i < this.size; i++) {
	            if (this.buffer[this.offset + i] != that.buffer[that.offset + i]) {
	                return false;
	            }
	        }
	        
	        return true;
	    }
	    
	    public String toString() {
	        try {
	            return new String(buffer, offset, size, "UTF-8");
	        } catch (UnsupportedEncodingException e) {
	            throw new Error(e);
	        }
	    }
	}
	
	public static class BytesValue {
	    public byte[] buffer;
	    public int offset;
	    public int size;
	    
	    public BytesValue() {
	        set(EMPTY_BYTE_ARRAY, 0, 0);
	    }
	    
	    public void set(byte[] buffer, int offset, int size) {
	        this.buffer = buffer;
	        this.offset = offset;
	        this.size = size;
	    }
	}
	
	public static class Util {
	    public static final short bytesToShortLE(byte[] arr, int offset) {
	        int result = (arr[offset++] & 0x00ff);
	        result |= (arr[offset++] & 0x00ff) << 8;
	        return (short) result;
	    }
	    
	    public static final int bytesToIntLE(byte[] arr, int offset) {
	        int i = offset;
	        int result = (arr[i++] & 0x00ff);
	        result |= (arr[i++] & 0x00ff) << 8;
	        result |= (arr[i++] & 0x00ff) << 16;
	        result |= (arr[i] & 0x00ff) << 24;
	        return result;
	    }
	    
	    public static final long bytesToLongLE(byte[] arr, int offset) {
	        int i = offset;
	        long result = (arr[i++] & 0x000000ffL);
	        result |= (arr[i++] & 0x000000ffL) << 8;
	        result |= (arr[i++] & 0x000000ffL) << 16;
	        result |= (arr[i++] & 0x000000ffL) << 24;
	        result |= (arr[i++] & 0x000000ffL) << 32;
	        result |= (arr[i++] & 0x000000ffL) << 40;
	        result |= (arr[i++] & 0x000000ffL) << 48;
	        result |= (arr[i]   & 0x000000ffL) << 56;
	        return result;
	    }
	    
	    public static double bytesToDoubleLE(byte[] arr, int offset) {
	        long myLong = bytesToLongLE(arr, offset);
	        return Double.longBitsToDouble(myLong);
	    }

	    public static final void shortToBytesLE(int value, byte[] arr, int offset) {
	        int i = offset;
	        arr[i++] = (byte) value;
	        arr[i++] = (byte) (value >>> 8);
	    }

	    public static final void intToBytesLE(int value, byte[] arr, int offset) {
	        arr[offset++] = (byte) value;
	        arr[offset++] = (byte) (value >>> 8);
	        arr[offset++] = (byte) (value >>> 16);
	        arr[offset] = (byte) (value >>> 24);
	    }

	    public static final void longToBytesLE(final long value, final byte[] arr, int offset) {
	        int i = offset;
	        arr[i++] = (byte) value;
	        arr[i++] = (byte) (value >>> 8);
	        arr[i++] = (byte) (value >>> 16);
	        arr[i++] = (byte) (value >>> 24);
	        arr[i++] = (byte) (value >>> 32);
	        arr[i++] = (byte) (value >>> 40);
	        arr[i++] = (byte) (value >>> 48);
	        arr[i]   = (byte) (value >>> 56);
	    }
	    
	    public static final void doubleToBytesLE(double value, byte[] arr, int offset) {
	        long bits = Double.doubleToRawLongBits(value);
	        longToBytesLE(bits, arr, 1);
	    }
	}
}
