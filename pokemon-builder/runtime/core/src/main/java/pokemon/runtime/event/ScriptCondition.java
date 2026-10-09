package pokemon.runtime.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Ruby boolean expressions of Conditional Branch (code 111, type 12 "Script"): a small parser for the subset the
 * project's events use - literals (numbers, strings, symbols, nil/true/false), {@code $globals}, {@code @ivars},
 * {@code Const::NAME}, receiver calls with arguments and indexing, {@code ! && || and or not}, comparisons and
 * {@code + - * / %}, and {@code (expr rescue expr)}. The atoms are answered by an {@link Env}.
 */
final class ScriptCondition {

    /** What the atoms of an expression mean. */
    interface Env {
        /** {@code $name} (with the dollar sign) or {@code @name}. */
        Object variable(String name);

        /** {@code receiver.method(args)}; {@code receiver} is null for a bare call such as {@code pbGet(1)}. */
        Object call(Object receiver, String method, List<Object> args);

        /** {@code A::B} (for example {@code PBMoves::CUT}) or a bare constant. */
        Object constant(String path);
    }

    /** Raised for an atom the environment does not know. */
    static final class Unsupported extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Unsupported(String message) {
            super(message, null, false, false);
        }
    }

    /** The receiver of a method called on nil (Ruby raises NoMethodError, which {@code rescue} catches). */
    static final Object NIL = new Object();

    private interface Node {
        Object eval(Env env);
    }

    private final Node root;
    private final List<String> calls = new ArrayList<>();

    private ScriptCondition(Node root) {
        this.root = root;
    }

    /** The method / function names the expression calls (to tell which atoms need a screen). */
    List<String> calls() {
        return calls;
    }

    Object eval(Env env) {
        return root.eval(env);
    }

    /** Ruby truthiness: everything but nil and false. */
    static boolean truthy(Object value) {
        return value != null && !Boolean.FALSE.equals(value);
    }

    // =====================================================================
    // parsing
    // =====================================================================

    static ScriptCondition parse(String text) {
        Parser parser = new Parser(text);
        Node node = parser.parseRescue();
        parser.expectEnd();
        ScriptCondition result = new ScriptCondition(node);
        result.calls.addAll(parser.calls);
        return result;
    }

    private static final class Parser {
        private final String s;
        private int pos;
        final List<String> calls = new ArrayList<>();

        Parser(String text) {
            this.s = text;
        }

        private void skipSpace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        void expectEnd() {
            skipSpace();
            if (pos != s.length()) {
                throw new Unsupported("unexpected '" + s.substring(pos) + "'");
            }
        }

        private boolean accept(String token) {
            skipSpace();
            if (s.startsWith(token, pos)) {
                pos += token.length();
                return true;
            }
            return false;
        }

        /** A keyword operator ({@code and}, {@code or}, {@code not}, {@code rescue}) must end at a word boundary. */
        private boolean acceptWord(String word) {
            skipSpace();
            if (s.startsWith(word, pos)) {
                int end = pos + word.length();
                if (end >= s.length() || !(Character.isLetterOrDigit(s.charAt(end)) || s.charAt(end) == '_')) {
                    pos = end;
                    return true;
                }
            }
            return false;
        }

        Node parseRescue() {
            Node left = parseOr();
            if (acceptWord("rescue")) {
                Node fallback = parseOr();
                return env -> {
                    try {
                        return left.eval(env);
                    } catch (Unsupported error) {
                        throw error;
                    } catch (RuntimeException error) {
                        return fallback.eval(env);
                    }
                };
            }
            return left;
        }

        private Node parseOr() {
            Node left = parseAnd();
            while (true) {
                if (accept("||") || acceptWord("or")) {
                    Node l = left, r = parseAnd();
                    left = env -> {
                        Object value = l.eval(env);
                        return truthy(value) ? value : r.eval(env);
                    };
                } else {
                    return left;
                }
            }
        }

        private Node parseAnd() {
            Node left = parseNot();
            while (true) {
                if (accept("&&") || acceptWord("and")) {
                    Node l = left, r = parseNot();
                    left = env -> {
                        Object value = l.eval(env);
                        return truthy(value) ? r.eval(env) : value;
                    };
                } else {
                    return left;
                }
            }
        }

        private Node parseNot() {
            skipSpace();
            if (pos < s.length() && s.charAt(pos) == '!' && !s.startsWith("!=", pos)) {
                pos++;
                Node operand = parseNot();
                return env -> !truthy(operand.eval(env));
            }
            if (acceptWord("not")) {
                Node operand = parseNot();
                return env -> !truthy(operand.eval(env));
            }
            return parseComparison();
        }

        private Node parseComparison() {
            Node left = parseAdd();
            skipSpace();
            for (String op : new String[] {"==", "!=", "<=", ">=", "<", ">"}) {
                if (s.startsWith(op, pos)) {
                    pos += op.length();
                    Node l = left, r = parseAdd();
                    return env -> compare(op, l.eval(env), r.eval(env));
                }
            }
            return left;
        }

        private Node parseAdd() {
            Node left = parseMul();
            while (true) {
                skipSpace();
                if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                    char op = s.charAt(pos++);
                    Node l = left, r = parseMul();
                    left = env -> arithmetic(op, l.eval(env), r.eval(env));
                } else {
                    return left;
                }
            }
        }

        private Node parseMul() {
            Node left = parseUnary();
            while (true) {
                skipSpace();
                if (pos < s.length() && (s.charAt(pos) == '*' || s.charAt(pos) == '/' || s.charAt(pos) == '%')) {
                    char op = s.charAt(pos++);
                    Node l = left, r = parseUnary();
                    left = env -> arithmetic(op, l.eval(env), r.eval(env));
                } else {
                    return left;
                }
            }
        }

        private Node parseUnary() {
            skipSpace();
            if (pos < s.length() && s.charAt(pos) == '-') {
                pos++;
                Node operand = parseUnary();
                return env -> arithmetic('-', 0, operand.eval(env));
            }
            return parsePostfix();
        }

        private Node parsePostfix() {
            Node node = parsePrimary();
            while (true) {
                skipSpace();
                if (pos < s.length() && s.charAt(pos) == '.' && pos + 1 < s.length()
                        && (Character.isLetter(s.charAt(pos + 1)) || s.charAt(pos + 1) == '_')) {
                    pos++;
                    String name = identifier();
                    List<Node> args = parseArguments();
                    calls.add(name);
                    Node receiver = node;
                    node = env -> {
                        Object target = receiver.eval(env);
                        return env.call(target == null ? NIL : target, name, evalAll(args, env));
                    };
                } else if (pos < s.length() && s.charAt(pos) == '[') {
                    pos++;
                    List<Node> args = new ArrayList<>();
                    do {
                        args.add(parseRescue());
                    } while (accept(","));
                    if (!accept("]")) {
                        throw new Unsupported("missing ]");
                    }
                    Node receiver = node;
                    node = env -> env.call(receiver.eval(env), "[]", evalAll(args, env));
                } else {
                    return node;
                }
            }
        }

        private List<Node> parseArguments() {
            List<Node> args = new ArrayList<>();
            skipSpace();
            if (pos < s.length() && s.charAt(pos) == '(') {
                pos++;
                skipSpace();
                if (!accept(")")) {
                    do {
                        args.add(parseRescue());
                    } while (accept(","));
                    if (!accept(")")) {
                        throw new Unsupported("missing )");
                    }
                }
            }
            return args;
        }

        private String identifier() {
            int start = pos;
            while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos) == '_')) {
                pos++;
            }
            if (pos < s.length() && (s.charAt(pos) == '?' || s.charAt(pos) == '!')
                    && !(pos + 1 < s.length() && s.charAt(pos + 1) == '=')) {
                pos++;
            }
            if (start == pos) {
                throw new Unsupported("identifier expected at " + pos);
            }
            return s.substring(start, pos);
        }

        private Node parsePrimary() {
            skipSpace();
            if (pos >= s.length()) {
                throw new Unsupported("unexpected end");
            }
            char c = s.charAt(pos);
            if (c == '(') {
                pos++;
                Node inner = parseRescue();
                if (!accept(")")) {
                    throw new Unsupported("missing )");
                }
                return inner;
            }
            if (c == '[') {
                pos++;
                List<Node> items = new ArrayList<>();
                skipSpace();
                if (!accept("]")) {
                    do {
                        items.add(parseRescue());
                    } while (accept(","));
                    if (!accept("]")) {
                        throw new Unsupported("missing ]");
                    }
                }
                return env -> evalAll(items, env);
            }
            if (Character.isDigit(c)) {
                int start = pos;
                while (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                    pos++;
                }
                final Object value = Integer.valueOf(Integer.parseInt(s.substring(start, pos)));
                return env -> value;
            }
            if (c == '"' || c == '\'') {
                pos++;
                StringBuilder out = new StringBuilder();
                while (pos < s.length() && s.charAt(pos) != c) {
                    if (s.charAt(pos) == '\\' && pos + 1 < s.length()) {
                        pos++;
                    }
                    out.append(s.charAt(pos++));
                }
                pos++;
                final Object value = out.toString();
                return env -> value;
            }
            if (c == ':' && pos + 1 < s.length() && (Character.isLetter(s.charAt(pos + 1)) || s.charAt(pos + 1) == '_')) {
                pos++;
                final Object value = identifier();
                return env -> value;
            }
            if (c == '$' || c == '@') {
                int start = pos++;
                while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos) == '_')) {
                    pos++;
                }
                final String name = s.substring(start, pos);
                return env -> env.variable(name);
            }
            if (Character.isLetter(c) || c == '_') {
                String name = identifier();
                if (name.equals("nil")) {
                    return env -> null;
                }
                if (name.equals("true")) {
                    return env -> Boolean.TRUE;
                }
                if (name.equals("false")) {
                    return env -> Boolean.FALSE;
                }
                // Const::Const / Kernel.method
                StringBuilder path = new StringBuilder(name);
                while (s.startsWith("::", pos)) {
                    pos += 2;
                    path.append("::").append(identifier());
                }
                skipSpace();
                boolean hasParens = pos < s.length() && s.charAt(pos) == '(';
                boolean constant = Character.isUpperCase(name.charAt(0)) && !hasParens
                        && !(pos < s.length() && s.charAt(pos) == '.' && path.indexOf("::") < 0
                        && name.equals("Kernel"));
                if (name.equals("Kernel") && pos < s.length() && s.charAt(pos) == '.') {
                    pos++;
                    String method = identifier();                       // Kernel.pbX == pbX
                    List<Node> args = parseArguments();
                    calls.add(method);
                    return env -> env.call(null, method, evalAll(args, env));
                }
                if (constant) {
                    final String full = path.toString();
                    return env -> env.constant(full);
                }
                List<Node> args = parseArguments();
                calls.add(name);
                return env -> env.call(null, name, evalAll(args, env));
            }
            throw new Unsupported("unexpected '" + c + "'");
        }

        private static List<Object> evalAll(List<Node> nodes, Env env) {
            List<Object> values = new ArrayList<>(nodes.size());
            for (Node node : nodes) {
                values.add(node.eval(env));
            }
            return values;
        }
    }

    // =====================================================================
    // operators
    // =====================================================================

    private static Object compare(String op, Object a, Object b) {
        if (op.equals("==")) {
            return equal(a, b);
        }
        if (op.equals("!=")) {
            return !equal(a, b);
        }
        if (!(a instanceof Number) || !(b instanceof Number)) {
            throw new IllegalArgumentException("comparison of non-numbers");
        }
        double x = ((Number) a).doubleValue(), y = ((Number) b).doubleValue();
        switch (op) {
            case "<": return x < y;
            case ">": return x > y;
            case "<=": return x <= y;
            default: return x >= y;
        }
    }

    private static boolean equal(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.equals(b);
    }

    private static Object arithmetic(char op, Object a, Object b) {
        if (!(a instanceof Number) || !(b instanceof Number)) {
            if (op == '+' && a instanceof String && b instanceof String) {
                return (String) a + b;
            }
            throw new IllegalArgumentException("arithmetic on non-numbers");
        }
        int x = ((Number) a).intValue(), y = ((Number) b).intValue();
        switch (op) {
            case '+': return x + y;
            case '-': return x - y;
            case '*': return x * y;
            case '/': return Math.floorDiv(x, y);
            default: return Math.floorMod(x, y);
        }
    }

    @Override
    public String toString() {
        return "ScriptCondition" + Arrays.toString(calls.toArray());
    }
}
