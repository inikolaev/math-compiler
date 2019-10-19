import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by inikolaev on 05/06/16.
 */
public class CompilerTest {
    private static final Map<String, Double> CONSTANTS = ImmutableMap.<String, Double>builder()
            .put("pi", Math.PI)
            .put("e", Math.E)
            .build();

    private static final Map<String, String[]> FUNCTIONS = ImmutableMap.<String, String[]>builder()
            .put("^", new String[] {"java/lang/Math", "pow", "(DD)D"})
            .put("ln", new String[] {"java/lang/Math", "log10", "(D)D"})
            .put("lg", new String[] {"java/lang/Math", "log", "(D)D"})
            .put("sin", new String[] {"java/lang/Math", "sin", "(D)D"})
            .put("cos", new String[] {"java/lang/Math", "cos", "(D)D"})
            .put("tan", new String[] {"java/lang/Math", "tan", "(D)D"})
            .build();

    public void createDefaultConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    @Test
    public void testSimple() throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        List<String> expression = ImmutableList.of("pi", "2", "^", "1", "*");

        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_8, ACC_PUBLIC, "Formula1", null, "java/lang/Object", new String[] {"Formula"});

        createDefaultConstructor(cw);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "evaluate", "(D)D", null, null);

        int stackSize = 0;
        int maxStackSize = 0;
        for (String token: expression) {
            token = token.toLowerCase();

            if ("x".equalsIgnoreCase(token)) {
                mv.visitVarInsn(DLOAD, 1);
                stackSize += 2;
            } else if (CONSTANTS.containsKey(token)) {
                mv.visitLdcInsn(CONSTANTS.get(token));
                stackSize += 2;
            } else if ("*".equals(token)) {
                mv.visitInsn(DMUL);
                stackSize -= 2; // two doubles removed from stack and ane added
            } else if ("/".equals(token)) {
                mv.visitInsn(DDIV);
                stackSize -= 2; // two doubles removed from stack and ane added
            } else if ("+".equals(token)) {
                mv.visitInsn(DADD);
                stackSize -= 2; // two doubles removed from stack and ane added
            } else if ("-".equals(token)) {
                mv.visitInsn(DSUB);
                stackSize -= 2; // two doubles removed from stack and ane added
            } else if (FUNCTIONS.containsKey(token)) {
                String[] f = FUNCTIONS.get(token);
                mv.visitMethodInsn(INVOKESTATIC, f[0], f[1], f[2], false);
                // need to adjust stack size here, which depends on the function
            } else if ("log".equals(token)) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "log", "(D)D", false);
                mv.visitLdcInsn(2.0);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "log", "(D)D", false);
                mv.visitInsn(DDIV);
                stackSize += 2; // two doubles removed from stack and one added
            } else {
                double value = Double.valueOf(token);
                if (value == 0.0) {
                    mv.visitInsn(DCONST_0);
                } else if (value == 1.0) {
                    mv.visitInsn(DCONST_1);
                } else {
                    mv.visitLdcInsn(value);
                }
                stackSize += 2;
            }

            System.out.println("token = " + token + ", stack size = " + stackSize);

            if (stackSize > maxStackSize) {
                maxStackSize = stackSize;
            }
        }

        System.out.println("Maximum stack size: " + maxStackSize);

        mv.visitInsn(DRETURN);
        mv.visitMaxs(maxStackSize, 3);
        mv.visitEnd();

        cw.visitEnd();

        final byte[] bytes = cw.toByteArray();

        ClassLoader cl = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals("Formula1")) {
                    return defineClass(name, bytes, 0, bytes.length);
                }

                return super.loadClass(name);
            }
        };

        Class<Formula> formulaClass = (Class<Formula>) cl.loadClass("Formula1");
        Formula formula = formulaClass.newInstance();
        System.out.printf(String.valueOf(formula.evaluate(10)));

        FileOutputStream fos = new FileOutputStream("Formula1.class");
        fos.write(bytes);
        fos.close();
    }
}
