package org.unicode.draft;

import java.util.List;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Pair;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Block_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.text.utility.Settings;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class CompareCldrUnihanData {

    private static final IndexUnicodeProperties IUP = IndexUnicodeProperties.make(Settings.latestVersion);
    static final Splitter ONBAR = Splitter.on('|').trimResults();
    static final Splitter ONSPACE = Splitter.on(' ').trimResults();

    static UnicodeMap<Integer> UNIHAN_TOTALSTROKES_S = new UnicodeMap<>();
    static UnicodeMap<Integer> UNIHAN_TOTALSTROKES_T = new UnicodeMap<>();
    static UnicodeMap<String> UNIHAN_MANDARIN_S = new UnicodeMap<>();
    static UnicodeMap<String> UNIHAN_MANDARIN_T = new UnicodeMap<>();
    static UnicodeMap<String> CLDR_HAN_LATIN = extractCldrPinyin();
    static UnicodeMap<Integer> CLDR_STROKE_COLLATION = extractCldrTotalStrokeT();

    static {
        UnicodeMap<String> kTotalStrokes = IUP.load(UcdProperty.kTotalStrokes);
        for (final String value : kTotalStrokes.values()) {
            UnicodeSet uset = kTotalStrokes.getSet(value);
            List<String> parts = ONBAR.splitToList(value);
            Integer sValue = Integer.parseInt(parts.get(0));
            Integer tValue = parts.size() == 1 ? sValue : Integer.parseInt(parts.get(1));
            UNIHAN_TOTALSTROKES_S.putAll(uset, sValue);
            UNIHAN_TOTALSTROKES_T.putAll(uset, tValue);
        }

        final UnicodeMap<String> kMandarin = IUP.load(UcdProperty.kMandarin);
        for (final String values : kMandarin.values()) {
            UnicodeSet uset = kMandarin.getSet(values);
            List<String> parts = ONBAR.splitToList(values);
            String sValue = parts.get(0);
            String tValue = parts.size() == 1 ? sValue : parts.get(1);
            UNIHAN_MANDARIN_S.putAll(uset, sValue);
            UNIHAN_MANDARIN_T.putAll(uset, tValue);
        }

        UnicodeMap<General_Category_Values> gc = IUP.loadEnum(UcdProperty.General_Category, General_Category_Values.class);
        UnicodeMap<Block_Values> blocks = IUP.loadEnum(UcdProperty.Block, Block_Values.class);
        UnicodeSet radicals = new UnicodeSet(blocks.getSet(Block_Values.CJK_Radicals_Supplement))
        .addAll(blocks.getSet(Block_Values.Kangxi_Radicals))
        .removeAll(gc.getSet(General_Category_Values.Unassigned));

        UnicodeMap<String> cjkRadical = IUP.load(UcdProperty.CJK_Radical);
        for (String value : cjkRadical.values()) {
            UnicodeSet withRadical = cjkRadical.getSet(value);
            copyValues(withRadical, UNIHAN_MANDARIN_S);
            copyValues(withRadical, UNIHAN_MANDARIN_T);
            copyValues(withRadical, UNIHAN_TOTALSTROKES_S);
            copyValues(withRadical, UNIHAN_TOTALSTROKES_T);
        }
        
        UNIHAN_TOTALSTROKES_S.freeze();
        UNIHAN_TOTALSTROKES_T.freeze();
        System.out.println("UNIHAN_STROKES_T: " + UNIHAN_TOTALSTROKES_T.size());
        UNIHAN_MANDARIN_S.freeze();
        UNIHAN_MANDARIN_T.freeze();
        System.out.println("UNIHAN_PINYIN_S: " + UNIHAN_MANDARIN_S.size());
    }

    private static <T> boolean copyValues(UnicodeSet withRadical, UnicodeMap<T> unicodeMap) {
        for (String s : withRadical) {
            // get value if there is one
            T cjkValue = unicodeMap.get(s);
            if (cjkValue != null) {
                for (String s2 : withRadical) {
                    if (unicodeMap.get(s2) == null) {
                        unicodeMap.put(s2, cjkValue);
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Read these in from the files as text. Can do because format is generated.
     */
    private static UnicodeMap<String> extractCldrPinyin() {
        UnicodeMap<String> result = new UnicodeMap<>();

        final String prefix = "<tRule>";
        final String suffix = ";</tRule>";
        Splitter onarrow = Splitter.on('→').trimResults();
        boolean reading = false;

        for (String line : FileUtilities.in(CLDRPaths.COMMON_DIRECTORY+"/transforms/", "Han-Latin.xml")) {
            //        <comment># START AUTOGENERATED Han-Latin.xml ( Unihan kMandarin)</comment>
            //        <tRule>[呵锕阿𠼞𥥩𨉚]→ā;</tRule>
            //...
            //        <comment># END AUTOGENERATED Han-Latin.xml (Unihan kMandarin)</comment>
            if (!reading) {
                if (line.contains("START AUTOGENERATED")) {
                    reading = true;
                }
                continue;
            }
            if (line.contains("END AUTOGENERATED")) {
                break;
            }
            line = line.trim();
            // <tRule>[⺅䌾䛘人亻仁壬忈忎朲秂芢鈓銋魜鵀𡰥𢇦𦏀𧥷]→rén;</tRule>
            if (!line.startsWith(prefix) 
                    || !line.endsWith(suffix)) {
                throw new IllegalArgumentException("Internal error: " + line);
            }
            String value = line.substring(prefix.length(),line.length()-suffix.length());
            List<String> list = onarrow.splitToList(value);
            if (list.size() != 2) {
                throw new IllegalArgumentException("Internal error: " + line);
            }
            UnicodeSet uset = new UnicodeSet(list.get(0));
            String target = list.get(1);
            result.putAll(uset, target);
        }
        result.freeze();
        System.out.println("CLDR PinyinS: " + result.size());
        return result;
    }

    private static UnicodeMap<Integer> extractCldrTotalStrokeT() {
        UnicodeMap<Integer> result = new UnicodeMap<>();
        Integer index = null;
        String suffix = null;
        UnicodeSet uset = new UnicodeSet();
        boolean reading = false;

        for (String line : FileUtilities.in(CLDRPaths.COLLATION_DIRECTORY, "zh.xml")) {
            if (!reading) {
                if (line.contains("START AUTOGENERATED STROKE LONG")) {
                    reading = true;
                }
                continue;
            }
            //            <cr><![CDATA[
            //        [import zh-u-co-private-pinyin]
            //        [reorder Hani Bopo]
            //                # START AUTOGENERATED STROKE LONG                
            //               &[last regular]
            //               <'\uFDD0\u2801' # INDEX 1
            //               <*一丨丶丿乀乁⺄乙乚乛𠃉𠃊𠃋𠃌𠃍𠃎𠃑亅𠄌〆〇〡〥〻 # 1
            //               <'\uFDD0\u2802' # INDEX 2
            int indexPos = line.indexOf("INDEX");
            if (indexPos < 0) {
                if (index == null) {
                    continue;
                }
            } else {
                index = Integer.parseInt(line.substring(indexPos+5).trim());
                suffix = "# " + index;
                continue;
            }
            line = line.trim();
            if (line.startsWith("<")) {
                int hashPos = line.indexOf('#');
                if (hashPos < 0 || !line.regionMatches(hashPos, suffix, 0, suffix.length())) {
                    throw new IllegalArgumentException("Internal error: " + line);
                }
                int beginIndex = line.startsWith("<*") ? 2 : 1;
                uset.clear();
                uset.addAll(line.substring(beginIndex,hashPos).trim());
                result.putAll(uset,index);
            }
            if (line.contains("<<<")) {
                break;
            }
        }

        result.freeze();
        System.out.println("CLDR StrokeT: " + result.size());
        return result;
    }

    public static void main(String[] args) {
        System.out.println("Diff CLDR_HAN_LATIN => UNIHAN_MANDARIN_S");
        diff(CLDR_HAN_LATIN, UNIHAN_MANDARIN_S);
        System.out.println("\nDiff CLDR_STROKE_COLLATION => UNIHAN_TOTALSTROKES_T");
        diff(CLDR_STROKE_COLLATION, UNIHAN_TOTALSTROKES_T);
        //        System.out.println("\nDiff CLDR_STROKE_COLLATION => UNIHAN_TOTALSTROKES_S");
        //        diff(CLDR_STROKE_COLLATION, UNIHAN_TOTALSTROKES_S);
    }

    enum Change {removed, changed, added}

    private static <T extends Comparable<T>> UnicodeMap<Pair<T,T>> diff(UnicodeMap<T> a, UnicodeMap<T> b) {
        UnicodeMap<Pair<T,T>> result = new UnicodeMap<>();
        UnicodeSet sources = new UnicodeSet(a.keySet()).addAll(b.keySet());
        for (UnicodeSet.EntryRange range : sources.ranges()) {
            for (int i = range.codepoint; i <= range.codepointEnd; ++i) {
                T v1 = a.get(i);
                T v2 = b.get(i);
                if (!Objects.equal(v1, v2)) {
                    result.put(i, Pair.of(v1, v2));
                }
            }
        }
        for (String string : sources.strings()) {
            T v1 = a.get(string);
            T v2 = b.get(string);
            if (!Objects.equal(v1, v2)) {
                result.put(string, Pair.of(v1, v2));
            }
        }
        TreeSet<Pair<T,T>> sorted = new TreeSet<>(result.values());
        for (Change type : Change.values()) {
            boolean header = false;
            int count = 0;
            for (Pair<T, T> pair2 : sorted) {
                if ("è".equals(pair2.getFirst())) {
                    int debug = 0;
                }
                if ((pair2.getFirst() == null) != (type == Change.added)) {
                    continue;
                }
                if ((pair2.getSecond() == null) != (type == Change.removed)) {
                    continue;
                }
                if (!header) {
                    System.out.println();
                    header = true;
                }
                UnicodeSet uset = result.getSet(pair2);
                System.out.println(type + "\t" + show(pair2) + "\t" + uset.size() + "\t" + uset.toPattern(false));
                count += uset.size();
            }
            //System.out.println(type + " count:\t" + count);
        }
        return result;
    }

    static <T extends Comparable<T>> String show(Pair<T,T> pair) {
        return CldrUtility.ifNull(pair.getFirst(), "∅") + " → " + CldrUtility.ifNull(pair.getSecond(), "∅");
    }
}