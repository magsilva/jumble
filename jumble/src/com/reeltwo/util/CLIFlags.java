package com.reeltwo.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is a handy utility for dealing with command line
 * flags. It allows syntactic and type-checking of flags and supports
 * java generics. Here is some example usage: <p>
 * 
 * <code><pre>
 * public static void main(String[] args) {
 *   // Create the CLIFlags
 *   CLIFlags flags = new CLIFlags(&quot;java Example&quot;);
 * 
 *   // Simple register, a boolean option, no description
 *   flags.registerOptional(&quot;verbose&quot;);
 *   // Register a required integer flag with usage and long description.
 *   flags.registerRequired(&quot;port&quot;, Integer.class, &quot;NUMBER&quot;, &quot;The port to connect to&quot;); // does type-checking!
 *   Flag&lt;Integer&gt; fallbackTimes = flags.registerRequired(&quot;retries&quot;, Integer.class, &quot;NUMBER&quot;, &quot; How long to wait before retry.&quot;, Integer.valueOf(5)); // A similar thing, supplying a default value and supporting generics.
 *   fallbackTimes.setMaxCount(Integer.MAX_VALUE);   // And let it be supplied multiple times
 *   flags.setRemainderUsage(&quot; &lt; FILE&quot;);
 * 
 *   // Set the user-supplied flags with main's String[] args
 *   // This will attempt to parse the flags.
 *   // If it cannot it will print out an appropriate message and call System.exit.
 *   // To override this behaviour, see setInvalidFlagHandler()
 *   flags.setFlags(args);
 * 
 *   // Read the flags
 *   if (flags.isSet(&quot;port&quot;)) {
 *     Integer port = (Integer) flags.getValue(&quot;port&quot;);
 *   }
 *   for (Integer fallback : fallbackFlag.getValues()) { // See, using generics to iterate over the values.
 *      if (connect(port)) {
 *        // So something
 *        break;
 *      }
 *      Thread.currentThread.sleep(fallback * 1000);
 *   }
 *   // More code...
 * 
 * }
 * </pre></code>
 * 
 * @author <a href="mailto:pablo@reeltwo.com">Pablo Mayrgundter</a>
 * @author <a href="mailto:len@netvalue.net.nz">Len Trigg</a>
 * @author <a href="mailto:sean@netvalue.net.nz">Sean Irvine</a>
 * @author <a href="mailto:jcleary@netvalue.net.nz">John Cleary</a>
 * @version $Revision: 1.123 $
 */
public final class CLIFlags {

  static final String LS = System.getProperty("line.separator");

  /**
   * An interface for objects that perform custom flag validation across a set
   * of flags.
   */
  public interface Validator {

    /**
     * Returns false if the current flag settings are not valid. An error
     * message should be supplied by calling <code>flags.setParseMessage()</code>
     * 
     * @param flags a <code>CLIFlags</code>.
     * @return true if the current flag settings are valid.
     */
    boolean isValid(CLIFlags flags);
  }

  /**
   * An interface for objects that handle invalid flag settings.
   */
  public interface InvalidFlagHandler {

    /**
     * Perform whatever action you want to take if flags are determined to be
     * invalid.
     * 
     * @param flags a <code>CLIFlags</code>.
     */
    void handleInvalidFlags(CLIFlags flags);
  }

  private static class FlagCountException extends IllegalArgumentException {
    public FlagCountException(final String message) {
      super(message);
    }
  }

  /**
   * Encapsulates a single flag.
   */
  public static class Flag<T> implements Comparable {

    private final Character mFlagChar;

    private final String mFlagName;

    private final String mFlagDescription;

    /** The maximum number of times the flag can occur. */
    private int mMaxCount;

    /** The minimum number of times the flag can occur. */
    private int mMinCount;

    private final Class<T> mParameterType;

    private String mParameterDescription;

    private final T mParameterDefault;

    /** Optional list of valid values for the parameter. */
    private List<String> mParameterRange;

    /** Values supplied by the user */
    private List<T> mParameter = new ArrayList<T>();

    /**
     * Creates a new <code>Flag</code> for which the name must be supplied on
     * the command line.
     * 
     * @param flagChar a <code>Character</code> which can be supplied by the
     * user as an abbreviation for flagName. May be null.
     * @param flagName a <code>String</code> which is the name that the user
     * specifies on the command line to denote the flag.
     * @param flagDescription a name used when printing help messages.
     * @param minCount the minimum number of times the flag must be specified.
     * @param maxCount the maximum number of times the flag can be specified.
     * @param paramDescription a description of the meaning of the flag.
     * @param paramDefault a default value that can be used for optional flags.
     */
    public Flag(Character flagChar, final String flagName, final String flagDescription,
                final int minCount, final int maxCount, Class<T> paramType, final String paramDescription,
                T paramDefault) {
      if (flagDescription == null) {
        throw new NullPointerException();
      }
      if ((flagName == null) && paramType == null) {
        throw new IllegalArgumentException();
      }

      mFlagName = flagName;
      mFlagChar = flagChar;
      mFlagDescription = flagDescription;
      mParameterType = paramType;
      mParameterDescription = ((paramDescription == null) || (paramDescription.length() == 0)) 
        ? autoDescription(mParameterType)
        : paramDescription.toUpperCase();
      mParameterDefault = paramDefault;

      mMinCount = minCount;
      mMaxCount = maxCount;

      // For enums set up the limited set of values message
      final String[] range = values(mParameterType);
      if (range != null) {
        setParameterRange(range);
      }
    }

    /**
     * Gets the object specified by str.
     * @param type of object.
     * @param str string to specify object.
     * @return object of class type (null if the type does not look sufficiently like an Enum).
     */
    //%internal static object ValueOf(Type type, string str) {
    static Object valueOf(final Class<?> type, final String str) {
      if (!isValidEnum(type)) {
        return null;
      }
      try {
        //%string valueOfMethod = "ValueOf";
        final String valueOfMethod = "valueOf";
        final Method m = type.getMethod(valueOfMethod, new Class[] {String.class});
        if (!isStatic(m)) {
          return null;
        }
        //%Type returnType = m.ReturnType;
        final Class<?> returnType = m.getReturnType();
        if (!type.isAssignableFrom(returnType)) {
          return null;
        }
        //System.err.println("m=" + m + " parameters=" + Arrays.toString(m.getParameterTypes()));
        final Object ret = m.invoke(null, new Object[] {str});
        return ret;
        //%catch (NullReferenceException e) { //getMethod returns null in c# in this case
      } catch (final NoSuchMethodException e) {
        //Should never happen
        throw new RuntimeException(e);
      } catch (final InvocationTargetException e) {
        // Should never happen
        throw new RuntimeException(e);
      } catch (final IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Gets the range of values for an Enum (or at least something that looks like an Enum).
     * @param type from which values to be extracted.
     * @return the allowed values for the specified type (null if does not look sufficiently like an Enum).
     */
    //%internal static string[] Values(Type type) {
    static String[] values(final Class<?> type) {
      if (type == null) {
        return null;
      }
      if (!isValidEnum(type)) {
        return null;
      }
      try {
        //%string valuesMethod = "Values";
        final String valuesMethod = "values";
        final Method m = type.getMethod(valuesMethod, new Class[0]);
        //%Type returnType = m.ReturnType;
        final Class<?> returnType = m.getReturnType();
        if (returnType.isArray()) {
          final Object[] ret = (Object[]) m.invoke(null, new Object[0]);
          final String[] res = new String[ret.length];

          for (int i = 0; i < ret.length; i++) {
            res[i] = ret[i].toString();
          }
          return res;
        }
        return null;
        //%catch (NullReferenceException e) { //getMethod returns null instead of exception in c#
      } catch (final NoSuchMethodException e) {
        // Should never happen
        throw new RuntimeException(e);
      } catch (final InvocationTargetException e) {
        // Should never happen
        throw new RuntimeException(e);
      } catch (final IllegalAccessException e) {
        throw new RuntimeException(e);
      }

    }

    /**
     * Check if looks sufficiently like an Enum to be treated as one.
     * Must implement 
     *    static T[] values()
     *    static T value(String)
     * @param type
     * @return true iff is an Enum or looks sufficiently like one.
     */
    //%internal static bool IsValidEnum(Type type) {
    static boolean isValidEnum(final Class<?> type) {
      if (type == null) {
        return false;
      }
      if (type.isEnum()) {
        return true;
      }

      final Method m;
      try {
        //      final Method[] declaredMethods = type.getDeclaredMethods();
        //System.err.println(Arrays.toString(declaredMethods));
        //%string valuesMethod = "Values";
        final String valuesMethod = "values";
        m = type.getDeclaredMethod(valuesMethod, new Class[0]);
      } catch (final SecurityException e) {
        return false;
        //%catch (Exception e) {
      } catch (final NoSuchMethodException e) {
        return false;
      }
      if (!isStatic(m)) {
        return false;
      }
      //%Type returnType = m.ReturnType;
      final Class<?> returnType = m.getReturnType();
      if (!returnType.isArray()) {
        return false;
      }

      final Method v;
      try {
        //%string valueOfMethod = "ValueOf";
        final String valueOfMethod = "valueOf";
        v = type.getMethod(valueOfMethod, new Class[] {String.class});
      } catch (final SecurityException e) {
        return false;
        //%catch (Exception e) {
      } catch (final NoSuchMethodException e) {
        return false;
      }
      if (!isStatic(v)) {
        return false;
      }
      //%Type returnTypev = v.ReturnType;
      final Class<?> returnTypev = v.getReturnType();
      if (!type.isAssignableFrom(returnTypev)) {
        return false;
      }
      return true;
    }

    static boolean isStatic(final Method method) {
      //%return method.IsStatic;
      return Modifier.isStatic(method.getModifiers());
    }

    /**
     * Sets the maximum number of times the flag can be specified.
     * 
     * @param count the maximum number of times the flag can be specified.
     */
    public void setMaxCount(final int count) {
      if ((count < 1) || (count < mMinCount)) {
        throw new IllegalArgumentException("MaxCount (" + count
            + ") must not be 0 or less than MinCount (" + mMinCount + ")");
      }
      mMaxCount = count;
    }

    /**
     * Gets the maximum number of times the flag can be specified.
     * 
     * @return the maximum number of times the flag can be specified.
     */
    public int getMaxCount() {
      return mMaxCount;
    }

    /**
     * Sets the minimum number of times the flag can be specified.
     * 
     * @param count the minimum number of times the flag can be specified.
     */
    public void setMinCount(final int count) {
      if (count > mMaxCount) {
        throw new IllegalArgumentException("MinCount (" + count
            + ") must not be greater than MaxCount (" + mMaxCount + ")");
      }
      if (count == Integer.MAX_VALUE) {
        throw new IllegalArgumentException(
            "You're crazy man -- MinCount cannot be Integer.MAX_VALUE");
      }
      mMinCount = count;
    }

    /**
     * Gets the minimum number of times the flag can be specified.
     * 
     * @return the minimum number of times the flag can be specified.
     */
    public int getMinCount() {
      return mMinCount;
    }

    /**
     * Return the number of times the flag has been set.
     * 
     * @return the number of times the flag has been set.
     */
    public int getCount() {
      return mParameter.size();
    }

    /**
     * Return true if the flag has been set.
     * 
     * @return true if the flag has been set.
     */
    public boolean isSet() {
      return mParameter.size() > 0;
    }

    /**
     * Gets the character name of this flag, if set.
     * 
     * @return the character name of this flag, or null if no character name has
     * been set.
     */
    public Character getChar() {
      return mFlagChar;
    }

    /**
     * Gets the name of the flag.
     * 
     * @return the name of the flag.
     */
    public String getName() {
      return mFlagName;
    }

    /**
     * Gets the description of the flag's purpose.
     * 
     * @return the description.
     */
    public String getDescription() {
      return mFlagDescription;
    }

    /**
     * Gets the description of the flag parameter. This is usually a single word
     * that indicates a little more than the parameter type.
     * 
     * @return the parameter description, or null for untyped flags.
     */
    public String getParameterDescription() {
      return mParameterDescription;
    }
    protected void setParameterDescription(String desc) {
      mParameterDescription = desc;
    }
    /**
     * Gets the type of the parameter. This will return null for untyped
     * (switch) flags. Parameters will be checked that they are of the specified
     * type.
     * 
     * @return the parameter type, or null if the flag is untyped.
     */
    public Class<T> getParameterType() {
      return mParameterType;
    }

    /**
     * Gets the default value of the parameter.
     * 
     * @return the default value, or null if no default has been specified.
     */
    public T getParameterDefault() {
      return mParameterDefault;
    }

    /**
     * Defines the set of strings that are valid for this flag.
     * 
     * @param range a collection of Strings.
     */
    public void setParameterRange(Collection<String> range) {
      setParameterRange(range.toArray(new String[range.size()]));
    }

    /**
     * Defines the set of strings that are valid for this flag.
     * 
     * @param range an array of Strings.
     */
    public void setParameterRange(String[] range) {
      if (range == null) {
        mParameterRange = null;
      } else {
        if (range.length < 2) {
          throw new IllegalArgumentException("Must specify at least two values in parameter range.");
        }
        List<String> l = new ArrayList<String>();
        for (int i = 0; i < range.length; i++) {
          final String s = range[i];
          try {
            parseValue(s);
          } catch (Exception e) {
            throw new IllegalArgumentException("Range value " + s + " could not be parsed.");
          }
          l.add(s);
        }
        mParameterRange = Collections.unmodifiableList(l);
      }
    }

    /**
     * Gets the list of valid parameter values, if these have been specified.
     * 
     * @return a <code>List</code> containing the permitted values, or null if
     * this has not been set.
     */
    public List<String> getParameterRange() {
      return mParameterRange;
    }

    /**
     * Get the value for this flag. If the flag was not user-set, then the
     * default value is returned (if defined). The value will have been checked
     * to comply with any parameter typing. If called on an untyped flag, this
     * will return Boolean.TRUE or Boolean.FALSE appropriately.
     * 
     * @return a value for this flag.
     */
    public T getValue() {
      return (isSet()) ? mParameter.get(0) : mParameterDefault;
    }

    /**
     * Get a collection of all values set for this flag. This is for flags that
     * can be set multiple times. If the flag was not user-set, then the
     * collection contains only the default value (if defined).
     * 
     * @return a <code>Collection</code> of the supplied values.
     */
    public Collection<T> getValues() {
      final Collection<T> result;
      if (isSet()) {
        result = mParameter;
      } else {
        result = new ArrayList<T>();
        if (mParameterDefault != null) {
          result.add(mParameterDefault);
        }
      }
      return Collections.unmodifiableCollection(result);
    }

    private void reset() {
      mParameter = new ArrayList<T>();
    }

    private FlagValue setValue(final String valueStr) {
      //System.err.println("SetValue: " + valueStr + " type: " + mParameterType);
      if (mParameter.size() >= mMaxCount) {
        throw new FlagCountException("Value cannot be set more than once for flag: " + mFlagName);
      }
      if ((mParameterRange != null) && !mParameterRange.contains(valueStr)) {
        throw new IllegalArgumentException("Value supplied is not in the set of allowed values.");
      }
      T value = parseValue(valueStr);
      mParameter.add(value);
      return new FlagValue<T>(this, value);
    }

    /**
     * Converts the string representation of a parameter value into the
     * appropriate Object. This default implementation knows how to convert
     * based on the parameter type for several common types. Override for custom
     * parsing.
     */
    protected T parseValue(final String valueStr) {
      //System.err.println("ParseValue: " + valueStr + " type: " + mParameterType);
      return Flag.instanceHelper(mParameterType, valueStr);
    }

    /** {@inheritDoc} */
    public boolean equals(final Object other) {
      return other instanceof Flag && getName().equals(((Flag) other).getName());
    }

    /** {@inheritDoc} */
    public int hashCode() {
      return getName().hashCode();
    }

    /** {@inheritDoc} */
    public int compareTo(Object other) {
      if ((other == null) || !(other instanceof Flag)) {
        return -1;
      }
      Flag f2 = (Flag) other;
      if (f2.getName() != null) {
        return (getName().compareTo(f2.getName()));
      }
      return -1;
    }

    /** Make a compact usage string (prefers char name if present). */
    protected String getCompactFlagUsage() {
      StringBuffer sb = new StringBuffer();
      if (getChar() != null) {
        sb.append(SHORT_FLAG_PREFIX).append(getChar());
      } else {
        sb.append(LONG_FLAG_PREFIX).append(getName());
      }
      final String usage = getParameterDescription();
      if (usage.length() > 0) {
        sb.append(' ').append(usage);
      }
      return sb.toString();
    }

    /** Make a usage string. */
    protected String getFlagUsage() {
      StringBuffer sb = new StringBuffer();
      sb.append(LONG_FLAG_PREFIX).append(getName());
      if (getParameterType() != null) {
        sb.append('=').append(getParameterDescription());
      }
      return sb.toString();
    }

    private void appendLongFlagUsage(WrappingStringBuffer wb, final int longestUsageLength) {
      wb.append("  ");
      if (getChar() == null) {
        wb.append("    ");
      } else {
        wb.append(SHORT_FLAG_PREFIX).append(getChar().charValue()).append(", ");
      }

      final String usageStr = getFlagUsage();
      wb.append(getFlagUsage());
      for (int i = 0; i < longestUsageLength - usageStr.length(); i++) {
        wb.append(" ");
      }
      wb.append(" ");

      StringBuffer description = new StringBuffer(getDescription());

      List range = getParameterRange();
      if (range != null) {
        description.append(" Must be one of ").append(range).append(".");
      }

      if (getMinCount() > 1) {
        description.append(" Must be specified at least ").append(getMinCount()).append(" times.");
      }

      if (getMaxCount() > 1) {
        if (getMaxCount() == Integer.MAX_VALUE) {
          description.append(" May be specified multiple times.");
        } else {
          description.append(" May be specified up to ").append(getMaxCount()).append(" times.");
        }
      }

      if (getParameterDefault() != null) {
        description.append(" (Default is ").append(getParameterDefault()).append(")");
      }

      wb.wrapText(description.toString());
      wb.append(LS);
    }

    private static String autoDescription(Class type) {
      final String result = type.getName();
      return result.substring(result.lastIndexOf('.') + 1).toUpperCase();
    }

    private static final Set<String> BOOLEAN_AFFIRMATIVE = new HashSet<String>();
    {
      BOOLEAN_AFFIRMATIVE.add("yes");
      BOOLEAN_AFFIRMATIVE.add("y");
      BOOLEAN_AFFIRMATIVE.add("t");
      BOOLEAN_AFFIRMATIVE.add("1");
      BOOLEAN_AFFIRMATIVE.add("on");
      BOOLEAN_AFFIRMATIVE.add("aye");
      BOOLEAN_AFFIRMATIVE.add("hai");
      BOOLEAN_AFFIRMATIVE.add("ja");
      BOOLEAN_AFFIRMATIVE.add("da");
      BOOLEAN_AFFIRMATIVE.add("ya");
      BOOLEAN_AFFIRMATIVE.add("positive");
      BOOLEAN_AFFIRMATIVE.add("fer-shure");
      BOOLEAN_AFFIRMATIVE.add("totally");
      BOOLEAN_AFFIRMATIVE.add("affirmative");
      BOOLEAN_AFFIRMATIVE.add("+5v");
    }

    @SuppressWarnings("unchecked")
    private static <T> T instanceHelper(Class<T> type, final String stringRep) {
      try {
        if (type == Boolean.class) {
          Boolean result = Boolean.valueOf(stringRep);
          if (!result.booleanValue() && BOOLEAN_AFFIRMATIVE.contains(stringRep.toLowerCase())) {
            result = Boolean.TRUE;
          }
          return (T) result;
        } else if (type == Byte.class) {
          return (T) Byte.valueOf(stringRep);
        } else if (type == Character.class) {
          return (T) Character.valueOf(stringRep.charAt(0));
        } else if (type == Float.class) {
          return (T) Float.valueOf(stringRep);
        } else if (type == Double.class) {
          return (T) Double.valueOf(stringRep);
        } else if (type == Integer.class) {
          return (T) Integer.valueOf(stringRep);
        } else if (type == Long.class) {
          return (T) Long.valueOf(stringRep);
        } else if (type == Short.class) {
          return (T) new Short(stringRep);
        } else if (type == File.class) {
          return (T) new File(stringRep);
        } else if (type == URL.class) {
          return (T) new URL(stringRep);
        } else if (type == String.class) {
          return (T) stringRep;
        } else if (isValidEnum(type)) {
          return (T) valueOf(type, stringRep.toUpperCase(Locale.getDefault()));
        } else if (type == Class.class) {
          return (T) Class.forName(stringRep);
        }
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("");
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException("Badly formatted URL: " + stringRep);
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException("Class not found: " + stringRep);
      }
      throw new IllegalArgumentException("Unknown parameter type: " + type);
    }
  }

  /**
   * Switch is an untyped flag, where no value needs to be specified by the user. 
   */
  public static class Switch extends Flag<Boolean> {
    /**
     * Constructor for a <code>Switch</code>. These are boolean valued
     * flags where the value does not need to be supplied -- if the
     * flag is specified the value is true, otherwise the flag is
     * false.
     * 
     * @param flagChar a <code>Character</code> which can be supplied by the
     * user as an abbreviation for flagName. May be null.
     * @param flagName a <code>String</code> which is the name that the user
     * specifies on the command line to denote the flag.
     * @param flagDescription a description of the meaning of the flag.
     */
    public Switch(Character flagChar, final String flagName, final String flagDescription) {
      super(flagChar, flagName, flagDescription, 0, 1, Boolean.class, null, null);
      if (flagName == null) {
        throw new IllegalArgumentException();
      }
      setParameterDescription(null);
    }

    /** {@inheritDoc} */
    public Class<Boolean> getParameterType() {
      return null;
    }

    /** {@inheritDoc} */
    protected Boolean parseValue(final String valueStr) {
      return Boolean.TRUE;
    }

    /** {@inheritDoc} */
    public void setParameterRange(String[] range) {
      throw new IllegalArgumentException("Cannot set parameter range for no-arg flags.");
    }

    /** {@inheritDoc} */
    public Boolean getValue() {
      return isSet();
    }

    /** {@inheritDoc} */
    public Collection<Boolean> getValues() {
      if (isSet()) {
        return super.getValues();
      } else {
        Collection<Boolean> result = new ArrayList<Boolean>();
        result.add(Boolean.FALSE);
        return result;
      }
    }
  }

  /**
   * <code>AnonymousFlag</code> is a flag with no name.
   */
  public static class AnonymousFlag<T> extends Flag<T> {

    private static int sAnonCounter = 0;

    private static synchronized int nextAnonCounter() {
      return ++sAnonCounter;
    }


    /** This specifies the ordering. */
    private final int mFlagRank;

    /**
     * Constructor for an anonymous <code>Flag</code>. These flags aren't
     * referred to by name on the command line -- their values are assigned
     * based on their position in the command line.
     * 
     * @param flagDescription a name used when printing help messages.
     * @param paramType a <code>Class</code> denoting the type of values to be
     * accepted.
     * @param paramDescription a description of the meaning of the flag.
     */
    public AnonymousFlag(final String flagDescription, Class<T> paramType,
        final String paramDescription) {
      super(null, null, flagDescription, 1, 1, paramType, paramDescription, null);
      mFlagRank = nextAnonCounter();
    }

    /** {@inheritDoc} */
    protected String getFlagUsage() {
      StringBuffer sb = new StringBuffer();
      sb.append(getParameterDescription());
      if (getMaxCount() > 1) {
        sb.append('+');
      }
      return sb.toString();
    }

    /** {@inheritDoc} */
    protected String getCompactFlagUsage() {
      return getFlagUsage();
    }

    /** {@inheritDoc} */
    public boolean equals(final Object other) {
      return other instanceof Flag && getName().equals(((Flag) other).getName());
    }

    /** {@inheritDoc} */
    public int hashCode() {
      return getName().hashCode();
    }

    /** {@inheritDoc} */
    public int compareTo(Object other) {
      if (other instanceof AnonymousFlag) {
        return mFlagRank - ((AnonymousFlag) other).mFlagRank;
      }
      return 1;
    }
  }

  /**
   * Encapsulates a flag and value pairing. This is used when retrieving the set
   * of flags in the order they were set.
   */
  public static class FlagValue<T> {
    private Flag<T> mFlag;

    private T mValue;

    private FlagValue(Flag<T> flag, T value) {
      mFlag = flag;
      mValue = value;
    }

    /**
     * Gets the Flag that this value was supplied to.
     * 
     * @return the Flag that this value was supplied to.
     */
    public Flag<T> getFlag() {
      return mFlag;
    }

    /**
     * Gets the value supplied to the flag.
     * 
     * @return the value supplied to the flag.
     */
    public T getValue() {
      return mValue;
    }

    /**
     * Gets a human-readable description of the flag value.
     * 
     * @return a human-readable description of the flag value.
     */
    public String toString() {
      String name = mFlag.getName();
      if (name == null) {
        name = mFlag.getParameterDescription();
      }
      return name + "=" + mValue;
    }
  }

  /**
   * True if the default InvalidFlagHandler is permitted to call System.exit.
   * This will be set to false if this class is loaded as part of JUnit tests.
   * (To prevent any tests of main from exiting the tests).
   */
  public static final boolean EXIT_OK;
  static {
    Throwable t = new Throwable();
    StackTraceElement[] trace = t.getStackTrace();
    boolean exitOk = true;
    for (int i = 0; i < trace.length && exitOk; i++) {
      if (trace[i].getClassName().startsWith("junit")) {
        exitOk = false;
      }
    }
    EXIT_OK = exitOk;
  }

  /** The default invalid flag handler. */
  public static final InvalidFlagHandler DEFAULT_INVALID_FLAG_HANDLER = new InvalidFlagHandler() {
    public void handleInvalidFlags(CLIFlags flags) {
      final int errorCode;
      if (flags.isSet(HELP_FLAG)) {
        flags.printUsage();
        errorCode = 0;
      } else {
        final WrappingStringBuffer wb = new WrappingStringBuffer();
        wb.setWrapWidth(DEFAULT_WIDTH);
        flags.appendUsageHeader(wb);
        flags.appendParseMessage(wb);
        wb.append(LS + "Try '" + LONG_FLAG_PREFIX + HELP_FLAG + "' for more information");
        flags.error(wb.toString());
        errorCode = 1;
      }
      if (!EXIT_OK) {
        throw new IllegalArgumentException("Exit with: " + errorCode);
      }
      System.exit(errorCode);
    }
  };

  static final String SHORT_FLAG_PREFIX = "-";

  static final String LONG_FLAG_PREFIX = "--";

  /** The built-in flag that signals wants help about flag usage. */
  public static final String HELP_FLAG = "help";

  static final String USAGE_SUMMARY_PREFIX = "Usage: ";

  static final String PARSE_ERROR_PREFIX = "Error: ";

  static final String REQUIRED_FLAG_USAGE_PREFIX = "Required flags: ";

  static final String OPTIONAL_FLAG_USAGE_PREFIX = "Optional flags: ";

  /**
   * Default width to which help is printed. This is semi-intelligent in that it
   * attempts to look at environment variables to determine the terminal width.
   */
  static final int DEFAULT_WIDTH;
  static {
    int defaultWidth = 80;
    try { // Have a crack at working out a larger default value
      final Process p = Runtime.getRuntime().exec("printenv TERMCAP");
      try {
        p.waitFor();
        final InputStream is = p.getInputStream();
        final byte[] b = new byte[is.available()];
        if (is.read(b) == b.length) {
          final String columnsEnv = new String(b).toUpperCase();
          final Matcher m = Pattern.compile(":CO#([0-9]+):").matcher(columnsEnv);
          if (m.find()) {
            defaultWidth = Integer.parseInt(m.group(1));
          }
        }
      } finally {
        p.getInputStream().close();
        p.getOutputStream().close();
        p.getErrorStream().close();
      }
    } catch (Throwable t) {
      ; // We really don't care, just fall back on the default
    }
    // System.err.println("Default width is " + defaultWidth);
    DEFAULT_WIDTH = defaultWidth;
  }

  /** The set of all (named) registered flags */
  private Set<Flag> mRegisteredFlags;

  /** The list of anonymous flags */
  private List<Flag> mAnonymousFlags;

  /** Maps from long names to all registered flags. */
  private Map<String, Flag> mLongNames;

  /** Maps from short char names to flags (only for those that have short names). */
  private Map<Character, Flag> mShortNames;

  /** Where error messages are written. */
  private final Appendable mErr;

  /** Custom text to tack on to the usage header. */
  private String mRemainderHeaderString = "";

  /** Typically a description of what the program does. */
  private String mProgramDescription = "";

  /** The name of the program accepting flags. */
  private String mProgramName;

  /** Optional validator for overall consistency between flags. */
  private Validator mValidator;

  /** Optional handler to deal with invalid flags. */
  private InvalidFlagHandler mInvalidFlagHandler = DEFAULT_INVALID_FLAG_HANDLER;

  // Set during setFlags()

  /** Stores all the read flags and their values, in the order they were seen. */
  private List<FlagValue> mReceivedFlags;

  /** The parse error string. */
  private String mParseMessageString = "";

  /**
   * Creates a new <code>CLIFlags</code> instance.
   * 
   * @param programName the name of the program.
   * @param programDescription a description of what the program does.
   * @param err TODO
   */
  public CLIFlags(final String programName, final String programDescription, final Appendable err) {
    mErr = err;
    mAnonymousFlags = new ArrayList<Flag>();
    mRegisteredFlags = new TreeSet<Flag>();
    mLongNames = new TreeMap<String, Flag>();
    mShortNames = new TreeMap<Character, Flag>();
    registerOptional('h', HELP_FLAG, "Print help on command-line flag usage.");
    setName(programName);
    setDescription(programDescription);
  }

  /**
   * Creates a new <code>CLIFlags</code> instance.
   * 
   * @param programName the name of the program.
   * @param programDescription a description of what the program does.
   */
  public CLIFlags(final String programName, final String programDescription) {
    this(programName, programDescription, System.err);
  }

  /**
   * Creates a new <code>CLIFlags</code> instance.
   * 
   * @param programName the name of the program.
   * @param err where error messages are written.
   */
  public CLIFlags(final String programName, final Appendable err) {
    this(programName, null, err);
  }

  /**
   * Creates a new <code>CLIFlags</code> instance.
   * 
   * @param programName the name of the program.
   */
  public CLIFlags(final String programName) {
    this(programName, System.err);
  }

  /**
   * Creates a new <code>CLIFlags</code> instance.
   */
  public CLIFlags() {
    this(null);
  }


  // Switch flags -- those that have a name but don't take a parameter
  // These can only be optional

  /**
   * Registers an option. This option is not required to be specified, and has
   * no usage info and no type associated with it. This method is a shortcut for
   * simple boolean flags.
   * 
   * @param name the option name (without the prefix string).
   * @param description the option description.
   * @return the flag.
   */
  public Flag registerOptional(final String name, final String description) {
    return register(new Switch(null, name, description));
  }

  /**
   * Registers an option. This option is not required to be specified, and has
   * no usage info and no type associated with it. This method is a shortcut for
   * simple boolean flags.
   * 
   * @param nameChar single letter name.
   * @param name the option name (without the prefix string).
   * @param description the option description.
   * @return the flag.
   */
  public Flag registerOptional(final char nameChar, final String name, final String description) {
    return register(new Switch(nameChar, name, description));
  }

  // Required flags

  /**
   * Register an anonymous flag. An anonymous flag has no name. Any anonymous
   * flags are processed in the order they are encountered.
   * 
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter.
   * @param description the option description.
   * @return the flag.
   */
  public <T> Flag<T> registerRequired(Class<T> type, final String usage, final String description) {
    Flag<T> flag = new AnonymousFlag<T>(description, type, usage);
    register(flag);
    return flag;
  }

  /**
   * Registers a required flag. This flag requires a parameter of a specified
   * type.
   * 
   * @param name the option name (without the prefix string).
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter type.
   * For example this might be <code>FILE</code>, <code>DIR</code>.
   * @param description the option description.
   * @return the flag.
   */
  public <T> Flag<T> registerRequired(final String name, Class<T> type, final String usage,
      final String description) {
    return registerRequired(null, name, type, usage, description);
  }

  /**
   * Registers a required flag. This flag requires a parameter of a specified
   * type.
   * 
   * @param nameChar single letter name.
   * @param name the option name (without the prefix string).
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter type.
   * For example this might be <code>FILE</code>, <code>DIR</code>.
   * @param description the option description.
   * @return the flag.
   */
  public <T> Flag<T> registerRequired(final char nameChar, final String name, Class<T> type,
      final String usage, final String description) {
    return registerRequired(Character.valueOf(nameChar), name, type, usage, description);
  }

  // (currently) internal method that uses Character instead of (easier) char
  private <T> Flag<T> registerRequired(Character nameChar, final String name, Class<T> type,
      final String usage, final String description) {
    return register(new Flag<T>(nameChar, name, description, 1, 1, type, usage, null));
  }

  // Optional flags

  /**
   * Registers an option. When provided, this option requires a parameter of a
   * specified type.
   * 
   * @param name the option name (without the prefix string).
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter type.
   * For example this might be <code>FILE</code>, <code>DIR</code>.
   * @param description the option description.
   * @return the flag.
   */
  public <T> Flag<T> registerOptional(final String name, Class<T> type, final String usage,
      final String description) {
    return registerOptional(name, type, usage, description, null);
  }

  /**
   * Registers an option. This option requires a parameter of a specified type.
   * 
   * @param name the option name (without the prefix string).
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter type.
   * For example this might be <code>FILE</code>, <code>DIR</code>.
   * @param description the option description.
   * @return the flag.
   */
  public <T> Flag<T> registerOptional(final String name, Class<T> type, final String usage,
      final String description, T defaultValue) {
    return registerOptional(null, name, type, usage, description, defaultValue);
  }

  /**
   * Registers an option. This option requires a parameter of a specified type.
   * 
   * @param nameChar single letter name.
   * @param name the option name (without the prefix string).
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter type.
   * For example this might be <code>FILE</code>, <code>DIR</code>.
   * @param description the option description.
   * @return the flag.
   */
  public <T> Flag<T> registerOptional(final char nameChar, final String name, Class<T> type,
      final String usage, final String description) {
    return registerOptional(nameChar, name, type, usage, description, null);
  }

  /**
   * Registers an option. This option requires a parameter of a specified type.
   * 
   * @param nameChar single letter name.
   * @param name the option name (without the prefix string).
   * @param type the <code>Class</code> of the expected parameter. Supported
   * classes are: <code>Boolean</code>, <code>Byte</code>,
   * <code>Character</code>, <code>Float</code>, <code>Double</code>,
   * <code>Integer</code>, <code>Long</code>, <code>Short</code>,
   * <code>File</code>, <code>URL</code> and <code>String</code>.
   * @param usage a one-word usage description of the expected parameter type.
   * For example this might be <code>FILE</code>, <code>DIR</code>.
   * @param description the option description.
   * @param defaultValue default value.
   * @return the flag.
   */
  public <T> Flag<T> registerOptional(final char nameChar, final String name, Class<T> type,
      final String usage, final String description, T defaultValue) {
    return registerOptional(Character.valueOf(nameChar), name, type, usage, description, defaultValue);
  }

  // (currently) internal method that uses Character instead of (easier) char
  private <T> Flag<T> registerOptional(Character nameChar, final String name, Class<T> type,
      final String usage, final String description, T defaultValue) {
    return register(new Flag<T>(nameChar, name, description, 0, 1, type, usage, defaultValue));
  }

  /**
   * Register a flag.
   * 
   * @param flag flag to register
   * @return registered instance
   */
  public <T> Flag<T> register(Flag<T> flag) {
    if (flag instanceof AnonymousFlag) {
      mAnonymousFlags.add(flag);
      mRegisteredFlags.add(flag);
    } else {
      if (mLongNames.containsKey(flag.getName())) {
        throw new IllegalArgumentException("A flag named " + flag.getName() + " already exists.");
      }
      if (flag.getChar() != null) {
        if (mShortNames.containsKey(flag.getChar())) {
          throw new IllegalArgumentException("A flag with short name " + flag.getChar()
              + " already exists.");
        }
        mShortNames.put(flag.getChar(), flag);
      }
      mRegisteredFlags.add(flag);
      mLongNames.put(flag.getName(), flag);
    }
    return flag;
  }

  /**
   * Returns a set of the required flags that have not been fully set during
   * <code>setFlags</code>.
   * 
   * @return a set of flags.
   */
  private Collection<Flag> getPendingRequired() {
    Collection<Flag> results = new ArrayList<Flag>();
    for (Flag f : mRegisteredFlags) {
      if (f.getCount() < f.getMinCount()) {
        results.add(f);
      }
    }
    return results;
  }

  /**
   * Returns a collection of the required flags.
   * 
   * @return a collection of flags.
   */
  public Collection<Flag> getRequired() {
    Collection<Flag> results = new ArrayList<Flag>();
    for (Flag f : mRegisteredFlags) {
      if (f.getMinCount() > 0) {
        results.add(f);
      }
    }
    return results;
  }

  /**
   * Returns a collection of the optional flags.
   * 
   * @return a collection of flags.
   */
  public Collection<Flag> getOptional() {
    Collection<Flag> results = new ArrayList<Flag>();
    for (Flag f : mRegisteredFlags) {
      if (f.getMinCount() == 0) {
        results.add(f);
      }
    }
    return results;
  }

  public String getParseMessage() {
    return mParseMessageString;
  }

  public void setParseMessage(final String parseString) {
    mParseMessageString = parseString;
  }

  private void setRemainingParseMessage(Collection remaining) {
    StringBuffer usage = new StringBuffer();
    if (remaining != null) {
      if (remaining.size() == 1) {
        usage.append("unexpected argument");
      } else {
        usage.append("unexpected arguments");
      }
      Iterator remItr = remaining.iterator();
      while (remItr.hasNext()) {
        final String s = (String) remItr.next();
        usage.append(' ').append(s);
      }
    }
    setParseMessage(usage.toString());
  }

  private void setPendingParseMessage(Collection pendingRequired) {
    StringBuffer usage = new StringBuffer();
    if ((pendingRequired != null) && !pendingRequired.isEmpty()) {
      if (pendingRequired.size() == 1) {
        usage.append("you must provide a value for");
      } else {
        usage.append("you must provide values for");
      }
      Iterator reqItr = pendingRequired.iterator();
      while (reqItr.hasNext()) {
        Flag f = (Flag) reqItr.next();
        usage.append(' ').append(f.getCompactFlagUsage());
        if (f.getMinCount() > 1) {
          final int count = f.getMinCount() - f.getCount();
          usage.append(" (").append(count).append((count == 1) ? " more time)" : " more times)");
        }
      }
    }
    setParseMessage(usage.toString());
  }

  /**
   * Sets the header text giving usage regarding standard input and output.
   * 
   * @param usageString a short description to append to the header text.
   */
  public void setRemainderHeader(final String usageString) {
    mRemainderHeaderString = usageString;
  }

  /**
   * Sets the name of the program reading the arguments. Used when printing
   * usage.
   * 
   * @param progName the name of the program.
   */
  public void setName(final String progName) {
    mProgramName = progName;
  }

  /**
   * Sets the description of the program reading the arguments. Used when
   * printing usage.
   * 
   * @param description the description.
   */
  public void setDescription(final String description) {
    mProgramDescription = description;
  }

  private void setFlag(Flag flag, final String strValue) {
    // System.err.println("Setting flag " + flag + " to " + strValue);
    mReceivedFlags.add(flag.setValue(strValue));
  }

  public void setInvalidFlagHandler(InvalidFlagHandler handler) {
    mInvalidFlagHandler = handler;
  }

  public void setValidator(Validator validator) {
    mValidator = validator;
  }

  /** Resets the list of flags received and their values. */
  private void reset() {
    mReceivedFlags = new ArrayList<FlagValue>();
    for (Flag f : mRegisteredFlags) {
      f.reset();
    }
    for (Flag f : mAnonymousFlags) {
      f.reset();
    }
    setParseMessage("");
  }

  /**
   * This method allows the setting of flags via a Properties instance. This
   * should be a convenient way of setting the values consistently. However,
   * since the Properties type has no fixed convention for expressing
   * multi-valued property values (e.g. names="joe","bob"), only single-valued
   * properties should be submitted to this method. Multi-valued properties will
   * likely cause an exception in the parsing of the value.
   * 
   * @param properties A Properties instance with single-valued properties only.
   * @return True if properties were successfully used to set flags.
   */
  public boolean setFlags(final Properties properties) {
    reset();
    boolean success = true;
    final Collection pendingRequired = getRequired();
    final Enumeration keys = properties.propertyNames();
    while (keys.hasMoreElements()) {
      final String name = (String) keys.nextElement();
      if (!mLongNames.containsKey(name)) {
        success = false;
        break;
      }
      try {
        final Flag flag = getFlag(name);
        pendingRequired.remove(flag);
        setFlag(flag, properties.getProperty(name));
      } catch (RuntimeException e) {
        return false;
      }
    }
    if (success && !pendingRequired.isEmpty()) {
      setPendingParseMessage(pendingRequired);
      success = false;
    }
    return success;
  }

  /**
   * Parses the command line flags for later use by the
   * <code>getValue(final String flagname)</code> method.
   * 
   * @param args The new flags value.
   * @return True iff all required flags were seen and all seen flags were of
   * set with expected type.
   */
  public boolean setFlags(String[] args) {
    reset();
    List<String> remaining = new ArrayList<String>();
    boolean success = true;
    int anonymousCount = 0;
    int i = 0;
    for (; i < args.length && success; i++) {
      final String nameArg = args[i];
      Flag flag = null;
      String value = null;

      if (nameArg.startsWith(SHORT_FLAG_PREFIX)) {
        if (nameArg.startsWith(LONG_FLAG_PREFIX)) {
          String name = nameArg.substring(LONG_FLAG_PREFIX.length());
          final int splitpos = name.indexOf('=');
          if (splitpos != -1) {
            value = name.substring(splitpos + 1);
            name = name.substring(0, splitpos);
          }
          flag = getFlag(name);
        } else if (nameArg.length() == SHORT_FLAG_PREFIX.length() + 1) {
          Character nameChar = Character.valueOf(nameArg.charAt(SHORT_FLAG_PREFIX.length()));
          flag = mShortNames.get(nameChar);
        }
        if (flag == null) {
          setParseMessage("Unknown flag " + nameArg);
          success = false;
        }
      }

      if (flag != null) {
        if ((flag.getParameterType() != null) && (value == null)) {
          i++;
          if (i == args.length) {
            setParseMessage("Expecting value for flag " + nameArg);
            success = false;
            break;
          } else {
            value = args[i];
          }
        }

        try {
          setFlag(flag, value);
        } catch (FlagCountException ie) {
          setParseMessage("Attempt to set flag " + nameArg + " too many times.");
          success = false;
        } catch (IllegalArgumentException e) {
          setParseMessage("Invalid value " + value + " for " + nameArg + ". " + e.getMessage());
          success = false;
        }
      } else if (anonymousCount < mAnonymousFlags.size()) {
        flag = getAnonymousFlag(anonymousCount);
        setFlag(flag, args[i]);
        if (flag.getCount() == flag.getMaxCount()) {
          anonymousCount++;
        }
      } else {
        remaining.add(args[i]);
      }
    }

    if (!success) {
      // Quickly scan unparsed args to see if it looks like they tried to ask
      // for help
      for (; i < args.length; i++) {
        if ((LONG_FLAG_PREFIX + HELP_FLAG).equals(args[i])
            || (SHORT_FLAG_PREFIX + "h").equals(args[i])) {
          setFlag(getFlag(HELP_FLAG), null);
        }
      }
    }

    if (isSet(HELP_FLAG)) {
      success = false;
    }

    if (success && !remaining.isEmpty()) {
      setRemainingParseMessage(remaining);
      success = false;
    }

    Collection pendingRequired = getPendingRequired();
    // System.err.println(pendingRequired);
    if (success && !pendingRequired.isEmpty()) {
      setPendingParseMessage(pendingRequired);
      success = false;
    }

    if (success && (mValidator != null) && (!mValidator.isValid(this))) {
      success = false;
    }

    if (!success && (mInvalidFlagHandler != null)) {
      mInvalidFlagHandler.handleInvalidFlags(this);
    }

    return success;
  }

  /**
   * Get an iterator over anonymous flags.
   * 
   * @return iterator over anonymous flags.
   */
  public Iterator<Flag> getAnonymousFlags() {
    return mAnonymousFlags.iterator();
  }

  /**
   * Get an anonymous flag by index.
   * 
   * @param index the index
   * @return the flag
   */
  public Flag getAnonymousFlag(final int index) {
    return mAnonymousFlags.get(index);
  }

  /**
   * Get a flag from its name.
   * 
   * @param flag flag name
   * @return the flag.
   */
  public Flag getFlag(final String flag) {
    return mLongNames.get(flag);
  }

  /**
   * Gets the value supplied with a flag.
   * 
   * @param flag the name of the flag (without the prefix).
   * @return an <code>Object</code> value. This object will be of type
   * configured during the option registering. You can also get the value of
   * no-type flags as a boolean, which indicates whether the no-type flag
   * occurred.
   */
  public Object getValue(final String flag) {
    return getFlag(flag).getValue();
  }

  /**
   * Get the values of a flag.
   * 
   * @param flag the flag
   * @return values of the flag
   */
  public Collection<?> getValues(final String flag) {
    return getFlag(flag).getValues();
  }

  /**
   * Get an anonymous value.
   * 
   * @param index the index
   * @return the anonymous value
   */
  public Object getAnonymousValue(final int index) {
    return getAnonymousFlag(index).getValue();
  }

  /**
   * Get the anonymous values.
   * 
   * @param index the index
   * @return anonymous values
   */
  public Collection<?> getAnonymousValues(final int index) {
    return getAnonymousFlag(index).getValues();
  }

  /**
   * Returns an iterator over the values that the user supplied, in the order
   * that they were supplied. Each element of the Iterator is a FlagValue.
   * 
   * @return an <code>Iterator</code> of <code>FlagValue</code>s.
   */
  public Iterator<FlagValue> getReceivedValues() {
    return mReceivedFlags.iterator();
  }

  /**
   * Returns true if a particular flag was provided in the arguments.
   * 
   * @param flag the name of the option.
   * @return true if the option was provided in the arguments.
   */
  public boolean isSet(final String flag) {
    final Flag aFlag = mLongNames.get(flag);
    return (aFlag == null) ? false : aFlag.isSet();
  }

  /**
   * Gets a compact description of the required and optional flags. This
   * contains only the names of the options with their usage parameters (i.e.:
   * not their individual descriptions).
   * 
   * @return a short <code>String</code> listing the options.
   */
  public String getCompactFlagUsage() {
    final WrappingStringBuffer sb = new WrappingStringBuffer();
    appendCompactFlagUsage(sb);
    return sb.toString().trim();
  }

  void appendUsageHeader(WrappingStringBuffer wb) {
    if (mProgramName != null) {
      wb.append(USAGE_SUMMARY_PREFIX);
      wb.append(mProgramName);
      wb.append(' ');
      wb.setWrapIndent();
      appendCompactFlagUsage(wb);
      if (!mRemainderHeaderString.equals("")) {
        wb.append(' ');
        wb.wrapText(mRemainderHeaderString);
      }
      wb.append(LS);
    }
  }

  /**
   * Adds compact flag usage information to the given WrappingStringBuffer,
   * wrapping at appropriate places.
   */
  void appendCompactFlagUsage(WrappingStringBuffer wb) {
    boolean first = true;
    if (getOptional().size() > 0) {
      wb.wrapWord("[OPTION]...");
      first = false;
    }
    Iterator it = getRequired().iterator();
    while (it.hasNext()) {
      wb.wrapWord((first ? "" : " ") + ((Flag) it.next()).getCompactFlagUsage());
      first = false;
    }
  }

  void appendParseMessage(WrappingStringBuffer wb) {
    if (!mParseMessageString.equals("")) {
      wb.append(LS).append(PARSE_ERROR_PREFIX);
      wb.setWrapIndent(PARSE_ERROR_PREFIX.length());
      wb.wrapText(mParseMessageString).append(LS);
    }
  }

  /**
   * @return a <code>String</code> containing the full usage information. This
   * contains the usage header, usage for each flag and usage footer.
   */
  public String getUsageString() {
    return getUsageString(DEFAULT_WIDTH);
  }

  /**
   * Get the usage string.
   * 
   * @param width width of output
   * @return usage wrapped to given width
   */
  public String getUsageString(final int width) {
    final WrappingStringBuffer usage = new WrappingStringBuffer();
    usage.setWrapWidth(width);

    appendUsageHeader(usage);

    appendParseMessage(usage);

    appendLongFlagUsage(usage);

    if (mProgramDescription != null && !mProgramDescription.equals("")) {
      usage.append(LS);
      usage.setWrapIndent(0);
      usage.wrapText(mProgramDescription);
    }
    return usage.toString();
  }

  void appendLongFlagUsage(WrappingStringBuffer wb) {
    int longestUsageLength = 0;
    // Get longest string lengths for use below in pretty-printing.
    final Iterator flagItr = mRegisteredFlags.iterator();
    while (flagItr.hasNext()) {
      Flag flag = (Flag) flagItr.next();
      final String usageStr = flag.getFlagUsage();
      if (usageStr.length() > longestUsageLength) {
        longestUsageLength = usageStr.length();
      }
    }

    // We do all the required flags first
    final Collection required = getRequired();
    if (required.size() > 0) {
      wb.append(LS);
      wb.append(REQUIRED_FLAG_USAGE_PREFIX).append(LS);
      wb.setWrapIndent(longestUsageLength + 7);
      for (Iterator flagItr2 = required.iterator(); flagItr2.hasNext();) {
        ((Flag) flagItr2.next()).appendLongFlagUsage(wb, longestUsageLength);
      }
    }

    // Then all the optional flags
    final Collection optional = getOptional();
    if (optional.size() > 0) {
      wb.setWrapIndent(0);
      wb.append(LS);
      wb.append(OPTIONAL_FLAG_USAGE_PREFIX).append(LS);
      wb.setWrapIndent(longestUsageLength + 7);
      for (Iterator flagItr2 = optional.iterator(); flagItr2.hasNext();) {
        ((Flag) flagItr2.next()).appendLongFlagUsage(wb, longestUsageLength);
      }
    }
  }

  /**
   * Prints the full usage information.
   */
  public void printUsage() {
    error(getUsageString());
  }

  /**
   * Prints message to the specified error output.
   * @param msg message to be written to error output.
   */
  public void error(final String msg) {
    try {
      mErr.append(msg + LS);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Just used for testing purposes
   * 
   * @param args The command line arguments.
   */
  public static void main(String[] args) {
    CLIFlags cli = new CLIFlags("CLIFlags");

    // cli.setRemainderHeader("ARGS");
    // Maybe support remaining args like this:
    cli.registerRequired(String.class, "ARGS", "These are some extra required args");
    cli.registerRequired(String.class, "BARGS", "These are some extra required args");

    Flag<Integer> intFlag = cli.registerRequired('i', "int", Integer.class, "my_int",
        "This sets an int value.");
    intFlag.setMaxCount(5);
    // intFlag.setMaxCount(Integer.MAX_VALUE);
    intFlag.setMinCount(2);

    cli.registerOptional('s', "switch", "This is a toggle flag.");

    cli.registerOptional('b', "boolean", Boolean.class, "true/false", "This sets a boolean value.", Boolean.TRUE);

    Flag<Float> f = cli.registerOptional('f', "float", Float.class, null, "This sets a float value.", Float.valueOf(20));
    f.setParameterRange(new String[] {"0.2", "0.4", "0.6" });
    cli.registerOptional("string", String.class, null,
            "This sets a string value. and for this one I'm going to have quite a long description. Possibly long enough to need wrapping.",
            "myDefault");
    cli.setFlags(args);

    // XXX System.out.println("--switch: " + cli.getValue("switch"));
    System.out.println("--boolean: " + cli.getValue("boolean"));
    System.out.println("--int: " + cli.getValue("int"));
    for (Integer anInt : intFlag.getValues()) {
      System.out.println("AnInt: " + anInt);
    }
    
    System.out.println("--float: " + cli.getValue("float"));
    System.out.println("--float infinite: " + f.getValue().isInfinite());

    System.out.println("--string: " + cli.getValue("string"));

    System.out.println(LS + "Multi-occurrence flag:");
    System.out.println("--int: " + cli.getValues("int"));

    System.out.println(LS + "Received values in order:");
    for (Iterator it = cli.getReceivedValues(); it.hasNext();) {
      System.out.println(it.next());
    }

  }
}
