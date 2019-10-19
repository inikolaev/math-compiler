import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by inikolaev on 05/06/16.
 */
public class Parser {
    private static final int START = 0;
    private static final int NUMBER = 1;
    private static final int FUNCTION = 2;

    public List<String> parse(String expression) {
        final char[] tokens = expression.toCharArray();

        List<String> result = new ArrayList<String>();
        List<String> stack = new ArrayList<String>();

        int state = START;
        String value = "";

        for (char token: tokens) {
            if (Character.isDigit(token)) {
                if (state != NUMBER) {
                    System.out.println("State changed");
                    value = "";
                }

                state = NUMBER;
                value += String.valueOf(token);
            }
        }

        return Collections.emptyList();
    }

    public static void main(String[] args) {
        Parser parser = new Parser();
        parser.parse("11+22+32*22");
    }
}
