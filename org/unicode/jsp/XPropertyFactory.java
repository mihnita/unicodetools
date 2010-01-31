package org.unicode.jsp;

import java.util.ArrayList;
import java.util.List;

import org.unicode.jsp.Idna.IdnaType;

import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.util.VersionInfo;

public class XPropertyFactory extends UnicodeProperty.Factory {
  private static XPropertyFactory singleton = null;
  public static synchronized XPropertyFactory make() {
    if (singleton != null) return singleton;
    singleton = new XPropertyFactory();
    return singleton;
  }
  

  {
    ICUPropertyFactory base = ICUPropertyFactory.make();
    for (String propertyAlias : (List<String>)base.getInternalAvailablePropertyAliases(new ArrayList())) {
      add(base.getProperty(propertyAlias));
    }
    for (int i = UnicodeUtilities.XSTRING_START; i < UnicodeUtilities.XSTRING_LIMIT; ++i) {
      XUnicodeProperty property = new XUnicodeProperty(i);
      add(property);
    }
    add(new IDNA2003());
    add(new UTS46());
    add(new IDNA2008());
    add(new Usage());
    add(new HanType());
    add(new UnicodeProperty.UnicodeMapProperty().set(XIDModifications.getReasons()).setMain("identifier-restriction", "idr", UnicodeProperty.ENUMERATED, "1.1"));
    add(new UnicodeProperty.UnicodeMapProperty().set(Confusables.getMap()).setMain("confusable", "confusable", UnicodeProperty.ENUMERATED, "1.1"));
  }

//  public UnicodeProperty getInternalProperty(String propertyAlias) {
//    UnicodeProperty result = props.get(propertyAlias.toLowerCase(Locale.ENGLISH));
//    if (result != null) {
//      return result;
//    }
//    return base.getInternalProperty(propertyAlias);
//  }
//
//  public List getInternalAvailablePropertyAliases(List result) {
//    base.getInternalAvailablePropertyAliases(result);
//    result.addAll(UnicodeUtilities.XPROPERTY_NAMES);
//    return result;
//  }

  private static class XUnicodeProperty extends UnicodeProperty {
    int fakeEnumValue;

    public XUnicodeProperty(int i) {
      setName(UnicodeUtilities.XPROPERTY_NAMES.get(i - UnicodeUtilities.XSTRING_START));
      fakeEnumValue = i;
      setType(UnicodeProperty.EXTENDED_STRING);
    }

    @Override
    protected List _getAvailableValues(List result) {
      addUnique("<string>", result);
      return result;
    }

    @Override
    protected List _getNameAliases(List result) {
      addUnique(getName(), result);
      return result;
    }

    @Override
    protected String _getValue(int codepoint) {
      return UnicodeUtilities.getXStringPropertyValue(fakeEnumValue, codepoint, NameChoice.LONG);
    }

    @Override
    protected List _getValueAliases(String valueAlias, List result) {
      addUnique("<string>", result);
      return result;
    }

    @Override
    protected String _getVersion() {
      return VersionInfo.ICU_VERSION.toString();
    }

  }
  
  private static abstract class XEnumUnicodeProperty extends UnicodeProperty {
    List<String> values = new ArrayList();

    public XEnumUnicodeProperty(String name, Object[] values) {
      setName(name);
      for (Object item : values) {
        this.values.add(item.toString());
      }
      setType(UnicodeProperty.ENUMERATED);
    }

    @Override
    protected List _getAvailableValues(List result) {
      for (String s : values) addUnique(s, result);
      return result;
    }

    @Override
    protected List _getNameAliases(List result) {
      addUnique(getName(), result);
      return result;
    }

    @Override
    protected List _getValueAliases(String valueAlias, List result) {
      if (values.contains(valueAlias)) {
        addUnique(valueAlias, result);
      }
      return result;
    }

    @Override
    protected String _getVersion() {
      return VersionInfo.ICU_VERSION.toString();
    }

  }
  
  private static class IDNA2003 extends XEnumUnicodeProperty {
    public IDNA2003() {
      super("idna", IdnaType.values());
    }

    @Override
    protected String _getValue(int codepoint) {
      return Idna2003.getIDNA2003Type(codepoint).toString();
    }
    @Override
    protected List _getNameAliases(List result) {
      super._getNameAliases(result);
      result.add("idna2003");
      return result;
    }
  }
  private static class UTS46 extends XEnumUnicodeProperty {
    public UTS46() {
      super("uts46", IdnaType.values());
    }

    @Override
    protected String _getValue(int codepoint) {
      return Uts46.SINGLETON.getType(codepoint).toString();
    }
  }
  
  private static class IDNA2008 extends XEnumUnicodeProperty {
    public IDNA2008() {
      super("idna2008", Idna2008.Idna2008Type.values());
    }

    @Override
    protected String _getValue(int codepoint) {
      return Idna2008.getTypeMapping().get(codepoint).toString();
    }
  }
  
  private static class Usage extends XEnumUnicodeProperty {
    enum UsageValues {common, historic, deprecated, liturgical, limited, symbol, punctuation, na;
    public static UsageValues getValue(int codepoint) {
      if (UnicodeProperty.SPECIALS.contains(codepoint)) return na;
      if (UnicodeUtilities.DEPRECATED.contains(codepoint)) return deprecated;
      if (UnicodeUtilities.LITURGICAL.contains(codepoint)) return liturgical;
      if (ScriptCategoriesCopy.ARCHAIC.contains(codepoint)) return historic;
      //if (UnicodeUtilities.LIM.contains(codepoint)) return archaic;
      if (UnicodeUtilities.COMMON_USE_SCRIPTS.contains(codepoint)) {
        if (UnicodeUtilities.SYMBOL.contains(codepoint)) return symbol;
        if (UnicodeUtilities.PUNCTUATION.contains(codepoint)) return punctuation;
        return common;
      }
      return limited;
    }
    }
    public Usage() {
      super("Usage", UsageValues.values());
      setType(UnicodeProperty.EXTENDED_ENUMERATED);
    }

    @Override
    protected String _getValue(int codepoint) {
      return UsageValues.getValue(codepoint).toString();
    }
  }

  private static class HanType extends XEnumUnicodeProperty {
    enum HanTypeValues {na, simplified_only, traditional_only, both;
    public static HanTypeValues getValue(int codepoint) {
      if (UnicodeSetUtilities.simpOnly.contains(codepoint)) return simplified_only;
      if (UnicodeSetUtilities.tradOnly.contains(codepoint)) return traditional_only;
      if (UnicodeSetUtilities.bothSimpTrad.contains(codepoint)) return both;
      return na;
    }
    }
    public HanType() {
      super("HanType", HanTypeValues.values());
      setType(UnicodeProperty.EXTENDED_ENUMERATED);
    }

    @Override
    protected String _getValue(int codepoint) {
      return HanTypeValues.getValue(codepoint).toString();
    }
  }
}
