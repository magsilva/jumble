package jumble.mutation;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.BIPUSH;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DADD;
import org.apache.bcel.generic.DCONST;
import org.apache.bcel.generic.DDIV;
import org.apache.bcel.generic.DMUL;
import org.apache.bcel.generic.DNEG;
import org.apache.bcel.generic.DREM;
import org.apache.bcel.generic.DRETURN;
import org.apache.bcel.generic.DSUB;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.FADD;
import org.apache.bcel.generic.FCONST;
import org.apache.bcel.generic.FDIV;
import org.apache.bcel.generic.FMUL;
import org.apache.bcel.generic.FNEG;
import org.apache.bcel.generic.FREM;
import org.apache.bcel.generic.FRETURN;
import org.apache.bcel.generic.FSUB;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.IADD;
import org.apache.bcel.generic.IAND;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.IDIV;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.IFNONNULL;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.IMUL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.IOR;
import org.apache.bcel.generic.IREM;
import org.apache.bcel.generic.IRETURN;
import org.apache.bcel.generic.ISHL;
import org.apache.bcel.generic.ISHR;
import org.apache.bcel.generic.ISUB;
import org.apache.bcel.generic.IUSHR;
import org.apache.bcel.generic.IXOR;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LADD;
import org.apache.bcel.generic.LAND;
import org.apache.bcel.generic.LCONST;
import org.apache.bcel.generic.LDIV;
import org.apache.bcel.generic.LMUL;
import org.apache.bcel.generic.LOR;
import org.apache.bcel.generic.LREM;
import org.apache.bcel.generic.LRETURN;
import org.apache.bcel.generic.LSHL;
import org.apache.bcel.generic.LSHR;
import org.apache.bcel.generic.LSUB;
import org.apache.bcel.generic.LUSHR;
import org.apache.bcel.generic.LXOR;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.SIPUSH;
import org.apache.bcel.generic.Type;

/**
 * Mutation tester. Given a class file can either count the number of possible
 * mutation points or perform a mutations. Mutations can be specified by number
 * or selected at random.
 * 
 * @author Sean A. Irvine
 * @version $Revision$
 */
public class Mutater {

  /**
   * Lop off .class or .java from a string.
   * 
   * @param className
   *          name of the class
   * @return class name without extension
   */
  public static String fixName(final String className) {
    if (className.endsWith(".class")) {
      return className.substring(0, className.length() - 6).replace('/', '.');
    } else if (className.endsWith(".java")) {
      return className.substring(0, className.length() - 5).replace('/', '.');
    } else {
      return className.replace('/', '.');
    }
  }

  /** Should ICONST instructions be changed. */
  private boolean mMutateInlineConstants = false;

  /** Should return instructions be changed. */
  private boolean mMutateReturns = false;

  /** Should IINC instructions be changed. */
  private boolean mMutateIncrements = false;

  public void setMutateIncrements(final boolean v) {
    mMutateIncrements = v;
    if (mMutateIncrements) {
      mMutatable[Constants.IINC] = new NOP();
    } else {
      mMutatable[Constants.IINC] = null;
    }
  }

  /**
   * Set whether or not inline constants should be mutated.
   * 
   * @param v
   *          true for mutation of inline constants
   */
  public void setMutateInlineConstants(final boolean v) {
    mMutateInlineConstants = v;
    if (mMutateInlineConstants) {
      mMutatable[Constants.ICONST_0] = new NOP();
      mMutatable[Constants.ICONST_1] = new NOP();
      mMutatable[Constants.ICONST_2] = new NOP();
      mMutatable[Constants.ICONST_3] = new NOP();
      mMutatable[Constants.ICONST_4] = new NOP();
      mMutatable[Constants.ICONST_5] = new NOP();
      mMutatable[Constants.ICONST_M1] = new NOP();
      mMutatable[Constants.FCONST_0] = new NOP();
      mMutatable[Constants.FCONST_1] = new NOP();
      mMutatable[Constants.FCONST_2] = new NOP();
      mMutatable[Constants.DCONST_0] = new NOP();
      mMutatable[Constants.DCONST_1] = new NOP();
      mMutatable[Constants.LCONST_0] = new NOP();
      mMutatable[Constants.LCONST_1] = new NOP();
      mMutatable[Constants.BIPUSH] = new NOP();
      mMutatable[Constants.SIPUSH] = new NOP();
    } else {
      mMutatable[Constants.ICONST_0] = null;
      mMutatable[Constants.ICONST_1] = null;
      mMutatable[Constants.ICONST_2] = null;
      mMutatable[Constants.ICONST_3] = null;
      mMutatable[Constants.ICONST_4] = null;
      mMutatable[Constants.ICONST_5] = null;
      mMutatable[Constants.ICONST_M1] = null;
      mMutatable[Constants.FCONST_0] = null;
      mMutatable[Constants.FCONST_1] = null;
      mMutatable[Constants.FCONST_2] = null;
      mMutatable[Constants.DCONST_0] = null;
      mMutatable[Constants.DCONST_1] = null;
      mMutatable[Constants.LCONST_0] = null;
      mMutatable[Constants.LCONST_1] = null;
      mMutatable[Constants.BIPUSH] = null;
      mMutatable[Constants.SIPUSH] = null;
    }
  }

  /**
   * Set whether or not return values should be mutated.
   * 
   * @param v
   *          true to mutate return values
   */
  public void setMutateReturnValues(final boolean v) {
    mMutateReturns = v;
    if (mMutateReturns) {
      mMutatable[Constants.ARETURN] = new NOP();
      mMutatable[Constants.DRETURN] = new NOP();
      mMutatable[Constants.FRETURN] = new NOP();
      mMutatable[Constants.IRETURN] = new NOP();
      mMutatable[Constants.LRETURN] = new NOP();
    } else {
      mMutatable[Constants.ARETURN] = null;
      mMutatable[Constants.DRETURN] = null;
      mMutatable[Constants.FRETURN] = null;
      mMutatable[Constants.IRETURN] = null;
      mMutatable[Constants.LRETURN] = null;
    }
  }

  /**
   * Table of mutatable instructions. If defined and not a NOP this gives the
   * mutated instruction to use.
   */
  private final Instruction[] mMutatable = new Instruction[256];
  {
    mMutatable[Constants.IADD] = new ISUB();
    mMutatable[Constants.ISUB] = new IADD();
    mMutatable[Constants.IMUL] = new IDIV();
    mMutatable[Constants.IDIV] = new IMUL();
    mMutatable[Constants.IREM] = new IMUL();
    mMutatable[Constants.IAND] = new IOR();
    mMutatable[Constants.IOR] = new IAND();
    mMutatable[Constants.IXOR] = new IAND();
    mMutatable[Constants.ISHL] = new ISHR();
    mMutatable[Constants.ISHR] = new ISHL();
    mMutatable[Constants.IUSHR] = new ISHL();
    mMutatable[Constants.LADD] = new LSUB();
    mMutatable[Constants.LSUB] = new LADD();
    mMutatable[Constants.LMUL] = new LDIV();
    mMutatable[Constants.LDIV] = new LMUL();
    mMutatable[Constants.LREM] = new LMUL();
    mMutatable[Constants.LAND] = new LOR();
    mMutatable[Constants.LOR] = new LAND();
    mMutatable[Constants.LXOR] = new LAND();
    mMutatable[Constants.LSHL] = new LSHR();
    mMutatable[Constants.LSHR] = new LSHL();
    mMutatable[Constants.LUSHR] = new LSHL();
    mMutatable[Constants.FADD] = new FSUB();
    mMutatable[Constants.FSUB] = new FADD();
    mMutatable[Constants.FMUL] = new FDIV();
    mMutatable[Constants.FDIV] = new FMUL();
    mMutatable[Constants.FREM] = new FMUL();
    mMutatable[Constants.DADD] = new DSUB();
    mMutatable[Constants.DSUB] = new DADD();
    mMutatable[Constants.DMUL] = new DDIV();
    mMutatable[Constants.DDIV] = new DMUL();
    mMutatable[Constants.DREM] = new DMUL();
    mMutatable[Constants.IF_ACMPEQ] = new NOP();
    mMutatable[Constants.IF_ACMPNE] = new NOP();
    mMutatable[Constants.IF_ICMPEQ] = new NOP();
    mMutatable[Constants.IF_ICMPGE] = new NOP();
    mMutatable[Constants.IF_ICMPGT] = new NOP();
    mMutatable[Constants.IF_ICMPLE] = new NOP();
    mMutatable[Constants.IF_ICMPLT] = new NOP();
    mMutatable[Constants.IF_ICMPNE] = new NOP();
    mMutatable[Constants.IFEQ] = new NOP();
    mMutatable[Constants.IFGE] = new NOP();
    mMutatable[Constants.IFGT] = new NOP();
    mMutatable[Constants.IFLE] = new NOP();
    mMutatable[Constants.IFLT] = new NOP();
    mMutatable[Constants.IFNE] = new NOP();
    mMutatable[Constants.IFNONNULL] = new NOP();
    mMutatable[Constants.IFNULL] = new NOP();
  }

  /**
   * Is this an instruction we know how to mutate? Needs the entire chain since
   * in rare cases we need to examine context to see if mutation is allowable.
   * 
   * @param ihs
   *          current instruction chain
   * @param offset
   *          position in chain
   * @param cpg
   *          constant pool
   * @return true if instruction can be mutated
   */
  private boolean isMutatable(final InstructionHandle[] ihs, final int offset,
      final ConstantPoolGen cpg) {
    final Instruction i = ihs[offset].getInstruction();

    final boolean canMutate = mMutatable[i.getOpcode()] != null;

    // handle special situation of .class invocations
    if (canMutate && i instanceof ICONST && offset < ihs.length - 1) {
      final Instruction context = ihs[offset + 1].getInstruction();
      if (context instanceof INVOKESTATIC
          && "class".equals(((INVOKESTATIC) context).getMethodName(cpg))) {
        return false;
      }
    }

    return canMutate;
  }

  /**
   * Skip to the next valid instruction to examine. The primary reason for this
   * function is to attempt to skip over assertions.
   */
  private static int skipAhead(final InstructionHandle[] ihs,
      final ConstantPoolGen cp, int j) {
    final Instruction i = ihs[j++].getInstruction();
    if (i instanceof GETSTATIC) {
      final GETSTATIC gs = (GETSTATIC) i;
      if (gs.getFieldName(cp).equals("$noassert")
          || gs.getFieldName(cp).equals("assert")) {
        // attempt to skip over a java 1.4 assert() statement
        // this works for code generated by jikes
        // skip forwards to a ATHROW instruction, most likely it ends the assert
        while (!(ihs[j++].getInstruction() instanceof ATHROW)) {
          ; // do nothing
        }
      } else if (gs.getFieldName(cp).indexOf("class$") != -1) {
        // attempt to skip a ".class" reference (because it has a ifnonnull
        // test)
        if (ihs[j + 1].getInstruction() instanceof IFNONNULL) {
          j += 2;
        }
      }
    }

    return j;
  }

  /** Set of methods to be ignored (i.e. never mutated). */
  private Set mIgnored;

  /**
   * Set the names of all the methods to be ignored during mutation. Any method
   * named by a member of the given set will not be subject to mutation.
   * 
   * @param ignore
   *          Set a value of type 'final'
   */
  public void setIgnoredMethods(final Set ignore) {
    mIgnored = ignore == null ? new HashSet() : ignore;
  }

  private boolean checkNormalMethod(final Method m) {
    /*
     * if(m == null) System.out.println("null"); else if(m.isNative())
     * System.out.println("native"); else if(m.isAbstract())
     * System.out.println("abstract"); else if(mIgnored.contains(m.getName()))
     * System.out.println("ignored"); else if(m.getName().indexOf("access$")!=
     * -1) System.out.println("access"); else if(m.getLineNumberTable() == null)
     * System.out.println("linenumber"); else if(m.getCode() == null)
     * System.out.println("code"); else
     * if(m.getLineNumberTable().getSourceLine(0) <= 0)
     * System.out.println("invalid line number");
     */
    return m != null && !m.isNative() && !m.isAbstract()
        && !mIgnored.contains(m.getName())
        && m.getName().indexOf("access$") == -1
        && m.getLineNumberTable() != null && m.getCode() != null
        && m.getLineNumberTable().getSourceLine(0) > 0;
  }

  /**
   * Count number of mutation points in a method.
   */
  private int countMutationPoints(final Method m, final String className,
      final ConstantPoolGen cp) {

    // check this is a method that it makes sense to mutate
    if (!checkNormalMethod(m)) {
      return 0;
    }
    final InstructionList il = new MethodGen(m, className, cp)
        .getInstructionList();
    final InstructionHandle[] ihs = il.getInstructionHandles();
    int count = 0;
    for (int j = skipAhead(ihs, cp, 0); j < ihs.length; j = skipAhead(ihs, cp,
        j)) {
      if (isMutatable(ihs, j, cp)) {
        count += 1;
      }
    }
    il.dispose();
    return count;
  }

  /**
   * Compute the total number of possible mutation points in the class.
   */
  public int countMutationPoints(final String cl) {
    final String className = fixName(cl);
    int count = 0;
    final JavaClass clazz = Repository.lookupClass(className);

    if (clazz == null) {
      System.out.println("Error: could not retrieve " + className);
      return -1;
    }
    // if is an interface, return -1 to distinguish from 0 point classes
    if (clazz.isInterface()) {
      return -1;
    }

    final Method[] methods = clazz.getMethods();
    final ConstantPool cpool = clazz.getConstantPool();
    /*
     * if (mConstants) { count = countMutationPoints(cpool.getConstantPool()); }
     */
    final ConstantPoolGen cp = new ConstantPoolGen(cpool);
    for (int i = 0; i < methods.length; i++) {
      count += countMutationPoints(methods[i], className, cp);
    }
    return count;
  }

  /**
   * Maps -1 -&gt; 1; 0 -&gt; 1; 1 -&gt; 0; 2 -&gt; 3; 3 -&gt; 4; 4 -&gt; 5; 5
   * -&gt; -1. This mapping is careful to handle use as boolean cases correctly.
   */
  private static final int[] ICONST_MAP = new int[] {1, 1, 0, 3, 4, 5, -1};

  /** Mutate an ICONST instruction. */
  private static Instruction mutateICONST(final ICONST i,
      final ConstantPoolGen cp) {
    return new ICONST(ICONST_MAP[i.getValue().intValue() + 1]);
  }

  /** Mutate a FCONST instruction. */
  private static Instruction mutateFCONST(final FCONST i,
      final ConstantPoolGen cp) {
    final float v = i.getValue().floatValue();
    if (v == 0.0F) {
      return new FCONST(1.0F);
    } else {
      return new FCONST(0.0F);
    }
  }

  /** Mutate a DCONST instruction. */
  private static Instruction mutateDCONST(final DCONST i,
      final ConstantPoolGen cp) {
    final double v = i.getValue().doubleValue();
    if (v == 0.0) {
      return new DCONST(1.0);
    } else {
      return new DCONST(0.0);
    }
  }

  /** Mutate a LCONST instruction. */
  private static Instruction mutateLCONST(final LCONST i,
      final ConstantPoolGen cp) {
    final long v = i.getValue().longValue();
    if (v == 0L) {
      return new LCONST(1L);
    } else {
      return new LCONST(0L);
    }
  }

  /** Mutate a BIPUSH instruction. */
  private static Instruction mutateBIPUSH(final BIPUSH i,
      final ConstantPoolGen cp) {
    return new BIPUSH((byte) (i.getValue().byteValue() + 1));
  }

  /** Mutate a SIPUSH instruction. */
  private static Instruction mutateSIPUSH(final SIPUSH i,
      final ConstantPoolGen cp) {
    return new SIPUSH((byte) (i.getValue().shortValue() + 1));
  }

  /** Mutate an IINC instruction */
  private static Instruction mutateIINC(final IINC i, final ConstantPoolGen cp) {
    return new IINC(i.getIndex(), -i.getIncrement());
  }

  /**
   * Return a new integer instruction with the same parameter, but which differs
   * from the current instruction.
   */
  private Instruction mutateIntegerArithmetic(
      final ArithmeticInstruction current, final ConstantPoolGen cp) {
    return mMutatable[current.getOpcode()];
  }

  private InstructionList mutateRETURN(final ReturnInstruction ret,
      final InstructionFactory ifactory) {
    final InstructionList il = new InstructionList();
    if (ret instanceof IRETURN) {
      // maps 0->1 and anything else to 0, this is done without need
      // of any more stack space.
      final IFEQ ifeq = new IFEQ(null);
      il.append(ifeq);
      il.append(new ICONST(0));
      il.append(new IRETURN());
      il.append(new ICONST(1));
      ifeq.setTarget(il.getEnd());
    } else if (ret instanceof LRETURN) {
      // +1L
      il.append(new LCONST(1));
      il.append(new LADD());
    } else if (ret instanceof FRETURN) {
      // +1.0, negate
      il.append(new FCONST(1.0F));
      il.append(new FNEG());
    } else if (ret instanceof DRETURN) {
      // +1.0, negate
      il.append(new DCONST(1.0));
      il.append(new DNEG());
    } else if (ret instanceof ARETURN) {
      // if result is non-null make it null, otherwise hard case
      // for moment throw runtime exception
      final IFNONNULL ifnonnull = new IFNONNULL(null);
      il.append(ifnonnull);
      il.append(ifactory.createNew("java.lang.RuntimeException"));
      il.append(new DUP());
      il.append(ifactory.createInvoke("java.lang.RuntimeException", "<init>",
          Type.VOID, new Type[0], Constants.INVOKESPECIAL));
      il.append(new ATHROW());
      il.append(new ACONST_NULL());
      ifnonnull.setTarget(il.getEnd());
    }
    return il;
  }

  /**
   * Produce a human description of an instruction.
   * 
   * @param i
   *          the instruction
   * @return description
   */
  private String describe(final Instruction i) {
    if (i instanceof IADD || i instanceof LADD || i instanceof FADD
        || i instanceof DADD) {
      return "+";
    }
    if (i instanceof ISUB || i instanceof LSUB || i instanceof FSUB
        || i instanceof DSUB) {
      return "-";
    }
    if (i instanceof IMUL || i instanceof LMUL || i instanceof FMUL
        || i instanceof DMUL) {
      return "*";
    }
    if (i instanceof IDIV || i instanceof LDIV || i instanceof FDIV
        || i instanceof DDIV) {
      return "/";
    }
    if (i instanceof IREM || i instanceof LREM || i instanceof FREM
        || i instanceof DREM) {
      return "%";
    }
    if (i instanceof IOR || i instanceof LOR) {
      return "|";
    }
    if (i instanceof IXOR || i instanceof LXOR) {
      return "^";
    }
    if (i instanceof IAND || i instanceof LAND) {
      return "&";
    }
    if (i instanceof ISHL || i instanceof LSHL) {
      return "<<";
    }
    if (i instanceof ISHR || i instanceof LSHR) {
      return ">>";
    }
    if (i instanceof IUSHR || i instanceof LUSHR) {
      return ">>>";
    }
    if (i instanceof ICONST) {
      return ((ICONST) i).getValue().toString();
    }
    if (i instanceof FCONST) {
      return ((FCONST) i).getValue().toString() + "F";
    }
    if (i instanceof DCONST) {
      return ((DCONST) i).getValue().toString() + "D";
    }
    if (i instanceof LCONST) {
      return ((LCONST) i).getValue().toString() + "L";
    }
    if (i instanceof BIPUSH) {
      final byte b = ((BIPUSH) i).getValue().byteValue();
      if (b >= ' ' && b <= '~') {
        return (b + " (" + (char) b + ")");
      }
      return "" + b;
    }
    if (i instanceof SIPUSH) {
      return ((SIPUSH) i).getValue().toString();
    }
    if (i instanceof ReturnInstruction) {
      return "changed return value (" + i.getName() + ")";
    }

    if (i instanceof IINC) {
      if (((IINC) i).getIncrement() >= 0) {
        return "+=";
      } else {
        return "-=";
      }
    }
    return "unknown";
  }

  /** The most recent modification. */
  private String mModification = null;

  /**
   * Return the most recent modification.
   * 
   * @return description of modification
   */
  public String getModification() {
    return mModification;
  }

  /** Count down for mutation to apply. */
  private int mCount = 0;

  public Mutater(final int count) {
    mCount = count;
    setIgnoredMethods(null);
  }

  private Method jumble(Method m, final String className,
      final ConstantPoolGen cp) {

    // check if modification is appropriate
    if (mCount < 0 || !checkNormalMethod(m)) {
      return m;
    }

    final MethodGen mg = new MethodGen(m, className, cp);
    final InstructionList il = mg.getInstructionList();
    final InstructionHandle[] ihs = il.getInstructionHandles();
    final InstructionFactory ifactory = new InstructionFactory(cp);

    for (int j = skipAhead(ihs, cp, 0); j < ihs.length; j = skipAhead(ihs, cp,
        j)) {
      final Instruction i = ihs[j].getInstruction();
      if (isMutatable(ihs, j, cp) && mCount-- == 0) {
        String mod = className + ":"
            + m.getLineNumberTable().getSourceLine(ihs[j].getPosition()) + ": ";
        if (i instanceof IfInstruction) {
          mod += "negated conditional";
          ihs[j].setInstruction(((IfInstruction) i).negate());
        } else if (i instanceof ArithmeticInstruction) {
          // binary operand integer instruction
          final Instruction inew = mutateIntegerArithmetic(
              (ArithmeticInstruction) i, cp);
          ihs[j].setInstruction(inew);
          mod += describe(i) + " -> " + describe(inew);
        } else if (i instanceof ReturnInstruction) {
          mod += describe(i);
          il.insert(ihs[j], mutateRETURN((ReturnInstruction) i, ifactory));
        } else {
          final Instruction inew;
          if (i instanceof ICONST) {
            inew = mutateICONST((ICONST) i, cp);
          } else if (i instanceof FCONST) {
            inew = mutateFCONST((FCONST) i, cp);
          } else if (i instanceof DCONST) {
            inew = mutateDCONST((DCONST) i, cp);
          } else if (i instanceof LCONST) {
            inew = mutateLCONST((LCONST) i, cp);
          } else if (i instanceof BIPUSH) {
            inew = mutateBIPUSH((BIPUSH) i, cp);
          } else if (i instanceof SIPUSH) {
            inew = mutateSIPUSH((SIPUSH) i, cp);
          } else if (i instanceof IINC) {
            // added by tin
            inew = mutateIINC((IINC) i, cp);
          } else {
            inew = null;
          }
          if (inew != null) {
            ihs[j].setInstruction(inew);
            mod += describe(i) + " -> " + describe(inew);
          }
        }
        mModification = mod;
        // System.err.println("Made modification: " + mModification);
        break;
      }
    }

    mg.setMaxStack(); // this is needed for the return mods
    m = mg.getMethod();
    il.dispose();
    return m;
  }

  public JavaClass jumbler(String cn) throws IOException {
    JavaClass clazz = Repository.lookupClass(cn);
    if (clazz == null) {
      throw new IOException("Class did not exist");
    }
    return jumbler(clazz);
//    Method[] methods = clazz.getMethods();
//    ConstantPoolGen cp = new ConstantPoolGen(clazz.getConstantPool());
//    for (int i = 0; i < methods.length; i++) {
//      methods[i] = jumble(methods[i], cn, cp);
//    }
//    clazz.setConstantPool(cp.getFinalConstantPool());
//    return clazz;
  }

  public JavaClass jumbler(final JavaClass clazz) {
    JavaClass ret = clazz.copy();

    Method[] methods = ret.getMethods();
    ConstantPoolGen cp = new ConstantPoolGen(ret.getConstantPool());
    for (int i = 0; i < methods.length; i++) {
      methods[i] = jumble(methods[i], ret.getClassName(), cp);
    }
    ret.setConstantPool(cp.getFinalConstantPool());
    return ret;
  }

  /**
   * Gets the name of the method currently being mutated for the given class.
   * 
   * @param cl
   *          the name of the class to mutate
   * 
   * @return mutated method name
   */
  public String getMutatedMethodName(String cl) {
    final String className = fixName(cl);
    int count = 0;
    final JavaClass clazz = Repository.lookupClass(className);

    if (clazz == null) {
      System.out.println("Error: could not retrieve " + className);
      return null;
    }

    final Method[] methods = clazz.getMethods();
    final ConstantPool cpool = clazz.getConstantPool();
    /*
     * if (mConstants) { count = countMutationPoints(cpool.getConstantPool()); }
     */
    final ConstantPoolGen cp = new ConstantPoolGen(cpool);
    for (int i = 0; i < methods.length; i++) {
      count += countMutationPoints(methods[i], className, cp);

      // Once we have gone past the mutation point,
      // then we have found the mutated method
      if (mCount < count) {
        return methods[i].getName() + methods[i].getSignature();
      }
    }

    // If we get here, then something went wrong
    throw new RuntimeException("Invalid mutation point");
  }

  /**
   * Gets the mutation point, relative to the method being mutated. The method
   * is specified by <CODE>getMutatedMethodName(cl)</CODE>.
   * 
   * @param cl
   *          the class to to mutate.
   * @return the mutation point, relative to the mutated method.
   */
  public int getMethodRelativeMutationPoint(String cl) {
    final String className = fixName(cl);
    int count = 0;
    final JavaClass clazz = Repository.lookupClass(className);

    if (clazz == null) {
      System.out.println("Error: could not retrieve " + className);
      return -1;
    }

    final Method[] methods = clazz.getMethods();
    final ConstantPool cpool = clazz.getConstantPool();
    /*
     * if (mConstants) { count = countMutationPoints(cpool.getConstantPool()); }
     */
    final ConstantPoolGen cp = new ConstantPoolGen(cpool);
    for (int i = 0; i < methods.length; i++) {
      int oldCount = count;
      count += countMutationPoints(methods[i], className, cp);
      // Once we have gone past the mutation point,
      // then we have found the mutated method
      if (mCount < count) {
        return mCount - oldCount;
      }
    }

    // If we get here, then something went wrong
    throw new RuntimeException("Invalid mutation point");
  }

}
