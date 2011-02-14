package org.unicode.draft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.draft.UnicodeDataInput.StringReader;
import org.unicode.draft.UnicodeDataOutput.StringWriter;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.ICUPropertyFactory;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class TestCompressed extends TestFmwk {
  public static void main(String[] args) {
    new TestCompressed().run(args);
  }

  public void TestSetStreamer () throws IOException {
    String[] tests = {"[a-c]", "[]", "[a-ce-fq-z]", "[{abc}]", "[a-cmq-z{abc}]"};
    UnicodeDataOutput unicodeDataOutput = new UnicodeDataOutput();
    UnicodeDataInput unicodeDataInput = new UnicodeDataInput();
    for (String test : tests) {
      for (boolean compressed : new boolean[] {false, true}) {
        UnicodeSet item = new UnicodeSet(test);
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();

        DataOutputStream out = new DataOutputStream(outBytes);
        unicodeDataOutput.set(out, compressed).writeUnicodeSet(item);
        out.close();

        byte[] bytes = outBytes.toByteArray();
        ByteArrayInputStream inBytes = new ByteArrayInputStream(bytes);
        DataInputStream in = new DataInputStream(inBytes);
        UnicodeSet newItem = unicodeDataInput.set(in, compressed).readUnicodeSet();
        checkAtEnd("Stream not read completely: " + test, in);
        in.close();
        assertEquals("Original: " + test + " => " + showBytes(bytes, 20), item, newItem);
      }
    }
  }

  public void TestMapStreamer () throws IOException {
    String[] tests = {"[a-c]", "[]", "[a-ce-fq-zA-BD-F]", "[{abc}]", "[a-cmq-z{abc}{huh?}]"};
    String[] values = {"The", "Quick", "Brown"};
    StringReader stringReader = new StringReader();
    StringWriter stringWriter = new StringWriter();
    UnicodeDataOutput unicodeDataOutput = new UnicodeDataOutput();
    UnicodeDataInput unicodeDataInput = new UnicodeDataInput();

    for (String test : tests) {
      // make a testing map
      UnicodeSet set = new UnicodeSet(test);
      UnicodeMap<String> item = new UnicodeMap();
      int count = 0;
      for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.nextRange();) {
        if (it.codepoint == it.IS_STRING) {
          item.put(it.string, values[count++]);
        } else {
          item.putAll(it.codepoint, it.codepointEnd, values[count++]);
        }
        if (count >= values.length){
          count = 0;
        }
      }

      verifyMap(stringReader, stringWriter, unicodeDataOutput, unicodeDataInput, test, item);

    }
  }

  int total;
  int totalCompressed;

  private void verifyMap(StringReader stringReader, StringWriter stringWriter, UnicodeDataOutput unicodeDataOutput, UnicodeDataInput unicodeDataInput,
          String test, UnicodeMap<String> item) throws IOException {
    int totalLocal = 0;
    for (boolean compressed : new boolean[] {false, true}) {
      ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(outBytes);
      unicodeDataOutput.set(out, compressed).writeUnicodeMap(item, stringWriter);
      out.close();
      byte[] bytes = outBytes.toByteArray();
      if (compressed) {
        totalCompressed += bytes.length;
      } else {
        total += bytes.length;
        totalLocal = bytes.length;
      }
      ByteArrayInputStream inBytes = new ByteArrayInputStream(bytes);
      DataInputStream in = new DataInputStream(inBytes);
      UnicodeMap<String> newItem = unicodeDataInput.set(in, compressed).readUnicodeMap(stringReader);
      checkAtEnd("Stream not read completely: " + test, in);
      in.close();
      String comp = compressed ? "\t" + percent.format(bytes.length/(double)totalLocal - 1) : "";
      if (!item.equals(newItem)) {
        errln("Original: " + test + " =>\t" + showBytes(bytes, 20) + comp + ":\t" + item + "\t!=\t" + newItem);
      } else {
        logln("Original: " + test + " =>\t" + showBytes(bytes, 20) + comp);
      }
    }
  }


  private void checkAtEnd(String msg, DataInput in) throws IOException {
    try {
      byte nextByte = in.readByte();
      errln(msg);
    } catch (EOFException e) {
      // ok
    }
  }

  public void TestXBasic() throws IOException {
    Object[] tests =
    { 
            "a",
            "Mark Davis",
            "abc", "xyz", "\uFFFF\u0000", "\ud800\udc00\ud800\udc02",
            "Εκτός αυτού",
            "ユニコードとは何か",
            "\ud800\udc00",
            "abc",
            0,
            -1,
            1,
            100,
            -100,
            -1000,
            1000,
            Byte.MAX_VALUE, Byte.MIN_VALUE, 
            Short.MAX_VALUE, Byte.MIN_VALUE,
            Character.MAX_VALUE, Character.MIN_VALUE,
            Integer.MAX_VALUE, Integer.MIN_VALUE,
            Long.MAX_VALUE, Long.MIN_VALUE, 
            1.0,
            -5.0,
            0.0,
            0.0d,
            1.0d,
            -1.0,
            -1.0d,
            Float.MIN_VALUE, Float.MAX_VALUE,
            Double.MIN_VALUE, Double.MAX_VALUE,
            Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
    };
    for (Object item : tests) {
      checkObject(item);
    }
  }

  static final DecimalFormat nf = (DecimalFormat) NumberFormat.getIntegerInstance();
  static final DecimalFormat percent = (DecimalFormat) NumberFormat.getPercentInstance();
  static {
    percent.setPositivePrefix("+");
  }

  private void checkObject(Object item) throws IOException {
    ByteArrayOutputStream oldOutBytes = new ByteArrayOutputStream();
    DataOutputStream oldOut = new DataOutputStream(oldOutBytes);
    writeObject(item, oldOut);
    oldOut.close();
    byte[] oldBytes = oldOutBytes.toByteArray();

    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(outBytes);
    CompressedDataOutput compressed = new CompressedDataOutput().set(out);
    writeObject(item, compressed);
    out.close();
    byte[] bytes = outBytes.toByteArray();
    ByteArrayInputStream inBytes = new ByteArrayInputStream(bytes);
    DataInputStream in = new DataInputStream(inBytes);
    CompressedDataInput compressedIn = new CompressedDataInput().set(in);
    Object newItem = readObject(compressedIn, item);
    checkAtEnd("Stream not read completely: " + item, in);
    in.close();

    if (item.equals(newItem)) {
      logln(item + "\t" + item.getClass().getName()
              + "\t" + percent.format(bytes.length/(double)oldBytes.length - 1) + "\t" + showBytes(bytes, 20) + "\tOld\t" + showBytes(oldBytes, 20)
      );
    } else {
      errln("No round trip " 
              + "\t" + item + "\t" + item.getClass().getName()
              + "\t" + percent.format(bytes.length/(double)oldBytes.length - 1) + "\t" + showBytes(bytes, 20) + "\tOld\t" + showBytes(oldBytes, 20)
              + "\t" + newItem + "\t" + newItem.getClass().getName());
    }
  }

  private Object readObject(CompressedDataInput in, Object item) throws IOException {
    if (item instanceof Byte) {
      return in.readByte();
    } else if (item instanceof Short) {
      return in.readShort();
    } else if (item instanceof Character) {
      return in.readChar();
    } else if (item instanceof Integer) {
      return in.readInt();
    } else if (item instanceof Long) {
      return in.readLong();
    } else if (item instanceof Float) {
      return in.readFloat();
    } else if (item instanceof Double) {
      return in.readDouble();
    } else if (item instanceof CharSequence) {
      return in.readUTF();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private String showBytes(byte[] bytes, int limit) {
    StringBuilder result = new StringBuilder();
    result.append('[').append(nf.format(bytes.length)).append(']');
    for (byte byteValue : bytes) {
      //      if (result.length() != 0) {
      result.append(' ');
      //      }
      result.append(Utility.hex(byteValue & 0xFF, 2));
      if (limit-- < 0) {
        result.append("...");
        break;
      }
    }
    return result.toString();
  }

  private void writeObject(Object item, DataOutput out) throws IOException {
    if (item instanceof Byte) {
      out.writeByte((Byte)item);
    } else if (item instanceof Short) {
      out.writeShort((Short)item);
    } else if (item instanceof Character) {
      out.writeChar((Character)item);
    } else if (item instanceof Integer) {
      out.writeInt((Integer)item);
    } else if (item instanceof Long) {
      out.writeLong((Long)item);
    } else if (item instanceof Float) {
      out.writeFloat((Float)item);
    } else if (item instanceof Double) {
      out.writeDouble((Double)item);
    } else if (item instanceof CharSequence) {
      out.writeUTF(item.toString());
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private Map<String,UnicodeMap<String>> cache = new LinkedHashMap();
  
  /**
   * Do this just to isolate the time.
   * @throws IOException
   */
  public void TestUnicodePropAccess () throws IOException {
    UnicodeProperty.Factory f = ICUPropertyFactory.make();
    for (String property : (Iterable<String>) f.getAvailableNames()) {
      UnicodeProperty prop = f.getProperty(property);
      if (prop.isType(UnicodeProperty.STRING_OR_MISC_MASK)) {
        continue;
      }
      UnicodeMap<String> map = prop.getUnicodeMap();
      cache.put(property, map);
    }
  }
  
  public void TestUnicodeProperties () throws IOException {
    StringReader stringReader = new StringReader();
    StringWriter stringWriter = new StringWriter();
    UnicodeDataOutput unicodeDataOutput = new UnicodeDataOutput();
    UnicodeDataInput unicodeDataInput = new UnicodeDataInput();

    for (Entry<String, UnicodeMap<String>> propertyPair : cache.entrySet()) {
      String property = propertyPair.getKey();
      UnicodeMap<String> map = propertyPair.getValue();
      verifyMap(stringReader, stringWriter, unicodeDataOutput, unicodeDataInput, property, map);
    }
    logln("Total:\t" + total);
    logln("Total-Compressed:\t" + nf.format(totalCompressed) + "\t" + percent.format(totalCompressed/(double)total - 1));
  }
}