package org.unicode.bidi;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;

import com.ibm.icu.lang.UCharacterDirection;

public class BidiTestIcu4jConformance {
  static com.ibm.icu.text.Bidi bidi;
  
  public static void main(String[] args) throws Exception {
    final String file = "/Users/markdavis/Desktop/BidiConformance.txt";
    System.out.println("Reading: " + file);
    String result = "";
    BufferedReader in = new BufferedReader(new FileReader(file));
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      line = line.trim();
      if (line.length() == 0) continue;
      if (line.startsWith("@Result:")) {
        result = line.substring(8).trim();
      } else {
        // R AL ... ; 7
        String[] source = line.split("\\s+");
        int paragraphDirections = 0;
        for (String item : source) {
          int type = getBidiType(item);
          if (type < 0) {
            if (item.equals(";")) continue;
            paragraphDirections = Integer.parseInt(item);
          } else {
            
          }
        }
      }
    }
    System.out.println("Done");
  }
  
  int getBidiType(String input) {
    return UCharacterDirection.LEFT_TO_RIGHT;
  }
  
  for 
}