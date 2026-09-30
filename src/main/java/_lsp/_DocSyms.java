package _lsp;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import basix_funx.loggy;

/**
 * Breadcrumb / document-symbol scanner. This is a port of the extension's
 * show_doc_symbs.ts + fn_body_n_head.ts + _block_head (faav.ts) so that the
 * scanning can run in the jar instead of in the client.
 *
 * The port is meant to reproduce the typescript behaviour exactly, odd corners
 * included: the class/function names are whole source lines (trimmed for c/cpp,
 * untouched for d/rust), the ranges run line-to-line, and the language comes off
 * the *active* editor rather than the document being asked about. Do not
 * "clean this up" without diffing against the typescript first.
 */
public class _DocSyms {
    /** stands in for the typescript `number | null` so that null and 0 differ */
    private static final int NONE = -1;

    private static final Pattern P_EXCLUDE_STRNS = Pattern.compile("(\"[\\s\\S]*?\")");
    private static final Pattern P_EXCLUDE_COMMENTS = Pattern.compile("(//.*)|(/\\*.*(/)?)");
    private static final Pattern P_ONE_LINE_COMMENT = Pattern.compile("^//");
    private static final Pattern P_OPEN_COMMENT = Pattern.compile("^/\\*");
    private static final Pattern P_CLOSE_COMMENT = Pattern.compile("\\*/$");
    private static final Pattern P_ONE_LINE_BLOCK = Pattern.compile(".*\\{.*\\}.*");
    private static final Pattern P_OPEN_BLOCK = Pattern.compile("\\{");
    private static final Pattern P_CLOSE_BLOCK = Pattern.compile("\\}");
    private static final Pattern P_QUOTES = Pattern.compile("[\"'`]+");
    private static final Pattern P_FN_HEAD = Pattern.compile("(fn\\s.*\\{?)|(\\sfn\\s.*\\{?)");
    private static final Pattern P_ANY_HEAD = Pattern.compile(".*");

    public static List<SymbolInformation> symbols(DocSymsParams params) {
        List<SymbolInformation> ret = new ArrayList<>();
        if (params == null || params.getText() == null) {
            return ret;
        }
        String uri = params.getUri();
        if (uri == null || uri.isEmpty()) {
            return ret;
        }
        // the original picked the language off the active editor, not off the
        // document it was handed, so activeUri carries that decision over
        String active = params.getActiveUri() != null ? params.getActiveUri() : uri;
        String lang = langOf(active);
        String doc = params.getText();
        if ("c".equals(lang) || "cpp".equals(lang)) {
            c_fn_body(doc, uri, ret);
        } else if ("d".equals(lang)) {
            _c_fn_body(doc, uri, ret);
        } else if ("rs".equals(lang)) {
            _rust_fn_body(doc, uri, ret);
        }
        return ret;
    }

    // /rs$|c$|cpp$|d$/g.exec(path) - every branch is $-anchored, so this is
    // just the same endings in the same order
    private static String langOf(String path) {
        if (path.endsWith("rs")) {
            return "rs";
        }
        if (path.endsWith("c")) {
            return "c";
        }
        if (path.endsWith("cpp")) {
            return "cpp";
        }
        if (path.endsWith("d")) {
            return "d";
        }
        return "";
    }

    // ------------------------------------------------------------------ c/cpp

    private static void c_fn_body(String doc, String uri, List<SymbolInformation> symbols) {
        String[] lines = split(doc);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = js_trim(lines[i]);
        }
        // note: no bee_ep here, the quote counter below plays that role
        final Pattern tst_class = Pattern.compile(".*(\\sclass|\\sstruct)\\s", Pattern.CASE_INSENSITIVE);
        QuoteCursor quotes = new QuoteCursor();
        BlockHead block_head = new BlockHead();
        int quote_state = 0;
        int block_state = 0;
        int sav_block_state = 0;
        int start_class = NONE;
        int start_block = NONE;
        boolean opened_class = false;
        boolean within_comment = false;
        String fn_head = "";

        for (int i = 0; i < lines.length; i++) {
            String ln = lines[i];
            if (P_ONE_LINE_COMMENT.matcher(ln).find()) {
                continue;
            }
            if (!within_comment) {
                within_comment = P_OPEN_COMMENT.matcher(lines[i]).find();
            }
            if (within_comment) {
                if (P_CLOSE_COMMENT.matcher(ln).find()) {
                    within_comment = false;
                }
                continue;
            }
            if (quote_state == 0) {
                quote_state = quotes.next(lines[i]);
            } // not complete covering
            if (quote_state > 0) {
                int tmp_quote_state = quotes.next(lines[i]);
                if (tmp_quote_state > 0) {
                    quote_state -= tmp_quote_state;
                }
                continue;
            }
            boolean one_line_block = P_ONE_LINE_BLOCK.matcher(ln).find();
            if (one_line_block && block_state == 0) {
                add_symb(symbols, i, i, ln, SymbolKind.Function, uri);
                continue;
            }
            if (one_line_block && block_state != 0) {
                continue;
            }
            if (start_class == NONE && block_state == 0 && tst_class.matcher(lines[i]).find()) {
                if (P_OPEN_BLOCK.matcher(lines[i]).find()) {
                    opened_class = true;
                }
                start_class = i;
                sav_block_state = block_state;
                continue;
            }
            if (start_class != NONE && block_state == 0 && P_CLOSE_BLOCK.matcher(lines[i]).find()) {
                add_symb(symbols, start_class, i, lines[start_class], SymbolKind.Class, uri);
                start_class = NONE;
                opened_class = false;
                continue;
            }
            String no_comments = P_EXCLUDE_COMMENTS.matcher(lines[i]).replaceAll("");
            block_state -= P_OPEN_BLOCK.matcher(no_comments).find() ? 1 : 0;
            if (!opened_class && start_class != NONE && sav_block_state != block_state) {
                block_state += 1;
                opened_class = true;
            }
            block_state += P_CLOSE_BLOCK.matcher(no_comments).find() ? 1 : 0;
            block_head.set_info(lines[i], i);
            if (start_block == NONE && block_state == -1) {
                fn_head = get_c_fn_head(lines, i);
                start_block = i;
            }
            if (start_block != NONE && block_state == 0) {
                add_symb(symbols, start_block, i, fn_head, SymbolKind.Function, uri);
                start_block = NONE;
            }
        }
    }

    // the enclosing head line: walk back to the nearest line holding a (
    private static String get_c_fn_head(String[] lines, int lnum) {
        for (int i = lnum; i > -1; i--) {
            if (lines[i].indexOf('(') >= 0) {
                return lines[i];
            }
        }
        return lines[0];
    }

    // -------------------------------------------------------------------- d

    private static void _c_fn_body(String doc, String uri, List<SymbolInformation> symbols) {
        LangElement d = new LangElement(uri);
        d.tst_class = Pattern.compile("^(class|struct|enum)\\s|\\s(class|struct|enum)\\s", Pattern.CASE_INSENSITIVE);
        d.any_head = true; // mark_fn_head = /.*/
        d.run(doc, bee_ep(doc, P_EXCLUDE_STRNS, "#"), symbols);
    }

    // ----------------------------------------------------------------- rust

    private static void _rust_fn_body(String doc, String uri, List<SymbolInformation> symbols) {
        LangElement rust = new LangElement(uri);
        rust.tst_class = Pattern.compile("^(trait<?|struct<?|(impl<?)|enum<?)|\\s(trait<?|struct<?|(impl<?)|enum<?)",
                Pattern.CASE_INSENSITIVE);
        rust.run(doc, bee_ep(doc, P_EXCLUDE_STRNS, "#"), symbols);
    }

    /**
     * The scanner d and rust share: tracks classes, blocks and the head of the
     * block that is currently open.
     */
    private static class LangElement {
        private final String uri;
        Pattern tst_class;
        boolean any_head;
        private final BlockHead block_head = new BlockHead();
        private final List<SymbolInformation> symbols = new ArrayList<>();
        private final JsGlobalRegex open_block = new JsGlobalRegex(P_OPEN_BLOCK);
        private final JsGlobalRegex close_block = new JsGlobalRegex(P_CLOSE_BLOCK);
        private int opened_class = NONE;
        private boolean within_comment = false;
        private int start_class = NONE;
        private int start_block = NONE;
        private int block_state = 0;
        private int sav_block_state = 0;
        private String no_comments = "";
        private String[] lines = new String[0];
        private String[] orig_lines = new String[0];

        LangElement(String uri) {
            this.uri = uri;
        }

        void run(String orig_doc, String beeped_doc, List<SymbolInformation> into) {
            this.lines = split(beeped_doc);
            this.orig_lines = split(orig_doc);
            for (int i = 0; i < lines.length; i++) {
                lines[i] = js_trim(lines[i]);
            }
            if (uri == null) {
                return; // set_lines() bails out, symbols stay empty
            }
            if (any_head) {
                block_head.mark_fn_head = P_ANY_HEAD;
            }
            for (int i = 0; i < lines.length; i++) {
                if (one_line_comment7(i)) {
                    continue;
                }
                if (within_comment7(i)) {
                    continue;
                }
                if (class_entry7(i)) {
                    continue;
                }
                head7(i);
                if (one_line_block7(i)) {
                    continue;
                }
                block_entry7(i);
                if (close_class7(i)) {
                    continue;
                }
                update_block_state(i);
                if (close_block7(i)) {
                    continue;
                }
            }
            into.addAll(symbols);
        }

        private boolean one_line_comment7(int lnum) {
            return P_ONE_LINE_COMMENT.matcher(lines[lnum]).find();
        }

        private boolean within_comment7(int lnum) {
            if (!within_comment) {
                within_comment = P_OPEN_COMMENT.matcher(js_trim(lines[lnum])).find();
            }
            if (within_comment) {
                if (P_CLOSE_COMMENT.matcher(js_trim(lines[lnum])).find()) {
                    within_comment = false;
                    return true;
                }
            }
            return within_comment;
        }

        private boolean class_entry7(int i) {
            if (start_class != NONE && opened_class == NONE && open_block.test(lines[i])) {
                opened_class = i;
                return true;
            }
            if (start_class == NONE && block_state == 0) {
                if (tst_class.matcher(lines[i]).find()) {
                    if (open_block.matchCount(lines[i]) > 0) {
                        opened_class = i;
                    }
                    start_class = i;
                    sav_block_state = block_state;
                }
            }
            return start_class == i || opened_class == i;
        }

        // the original also stashed block_head.name into a #fn_head here, but
        // nothing ever read it back, so there is nothing to keep
        private boolean block_entry7(int i) {
            if (start_block == NONE && block_state == -1) {
                start_block = i;
            }
            return start_block == i;
        }

        private boolean one_line_block7(int i) {
            if (uri == null) {
                return false;
            }
            if (P_ONE_LINE_BLOCK.matcher(lines[i]).find() && block_state == 0 && !block_head.name.isEmpty()) {
                add_symb(symbols, block_head.lnum, i, orig_lines[i], SymbolKind.Function, uri);
                block_head.name = "";
                return true;
            }
            return false;
        }

        private void head7(int i) {
            no_comments = P_EXCLUDE_COMMENTS.matcher(lines[i]).replaceAll("");
            if (block_state != 0) {
                return;
            }
            block_head.try_set_info(no_comments, i);
        }

        private boolean close_block7(int i) {
            if (uri == null) {
                return false;
            }
            if (start_block != NONE && block_state == 0 && !block_head.name.isEmpty()) {
                int head = get_head_lnum(orig_lines, block_head.lnum);
                add_symb(symbols, head, i + 1, orig_lines[head], SymbolKind.Function, uri);
                block_head.name = "";
                start_block = NONE;
                return true;
            }
            return false;
        }

        private boolean close_class7(int i) {
            if (uri == null) {
                return false;
            }
            if (start_class != NONE && opened_class != NONE && block_state == 0
                    && close_block.test(lines[i])) {
                add_symb(symbols, start_class, i + 1, orig_lines[start_class], SymbolKind.Class, uri);
                block_head.name = "";
                start_class = NONE;
                opened_class = NONE;
                return true;
            }
            return false;
        }

        private void update_block_state(int i) {
            block_state -= open_block.matchCount(no_comments);
            // `!this.#opened_class` is falsy for both null and line 0
            if ((opened_class == NONE || opened_class == 0) && start_class != NONE
                    && sav_block_state != block_state) {
                block_state += 1;
                opened_class = i;
            }
            block_state += close_block.matchCount(no_comments);
        }
    }

    // ---------------------------------------------------------------- shared

    /**
     * The line a function symbol should be named after, and the line its range
     * should start on. block_head lands on the last line seen while outside any
     * block, which for a signature spread over several lines is the closing
     * paren rather than the declaration, so walk back to the line that opens
     * the argument list.
     */
    private static int get_head_lnum(String[] orig_lines, int lnum) {
        for (int i = lnum; i > 0; i--) {
            String ln = P_EXCLUDE_COMMENTS.matcher(orig_lines[i]).replaceAll("").trim();
            if (ln.indexOf('(') >= 0) {
                // a line that opens with "(" is itself a continuation
                return ln.startsWith("(") ? Math.max(0, i - 1) : i;
            }
            // a blank line, a statement end or a closing brace starts a new
            // construct, so stop rather than wander into unrelated code
            if (ln.isEmpty() || ln.endsWith(";") || ln.equals("}")) {
                break;
            }
        }
        return lnum;
    }

    /** _block_head out of faav.ts: remembers the line that opened the block */
    private static class BlockHead {
        String name = "";
        int lnum = 0;
        Pattern mark_fn_head = P_FN_HEAD;

        void set_info(String strn, int i) {
            if (strn.length() <= 1) {
                return;
            }
            this.name = strn;
            this.lnum = i;
        }

        void try_set_info(String strn, int i) {
            if (strn.length() <= 1) {
                return;
            }
            if (!mark_fn_head.matcher(strn).find()) {
                return;
            }
            this.name = strn;
            this.lnum = i;
        }
    }

    /** bee_ep(): beep out the contents of string literals, newlines kept */
    private static String bee_ep(String txt0, Pattern rgx, String symb) {
        String txt = txt0;
        List<String> for_beep = new ArrayList<>();
        Matcher m = rgx.matcher(txt0);
        while (m.find()) {
            for_beep.add(m.group());
        }
        if (for_beep.isEmpty()) {
            return txt;
        }
        for (String strn : for_beep) {
            StringBuilder new_strn = new StringBuilder();
            for (int x = 0; x < strn.length(); x++) {
                new_strn.append(strn.charAt(x) == '\n' ? "\n" : symb);
            }
            String rep = new_strn.toString();
            int at = txt.indexOf(strn);
            if (at >= 0) { // js replaces the first occurrence only
                txt = txt.substring(0, at) + rep + txt.substring(at + strn.length());
            }
        }
        return txt;
    }

    /**
     * The /["'`]+/g cursor of c_fn_body, sticky lastIndex and all. Note the
     * original reads exec(...)?.length, which is the size of the match array -
     * 1 for a hit and 0 for a miss - not the length of the matched run, so
     * quote_state only ever holds 0 or 1 here.
     */
    private static class QuoteCursor {
        private int last_index = 0;

        int next(String line) {
            Matcher m = P_QUOTES.matcher(line);
            m.region(Math.min(last_index, line.length()), line.length());
            if (!m.find()) { // js searches forward from lastIndex, then resets it
                last_index = 0;
                return 0;
            }
            last_index = m.end();
            return 1;
        }
    }

    private static void add_symb(List<SymbolInformation> symbols, int startLine, int endLine, String objName,
            SymbolKind kind, String uri) {
        Range rng = new Range(new Position(startLine, 0), new Position(endLine, 0));
        // lsp4j takes containerName last, vscode's own ctor takes it third
        symbols.add(new SymbolInformation(objName, kind, new Location(uri, rng), ""));
    }

    private static int count(String strn, Pattern rgx) {
        int ret = 0;
        Matcher m = rgx.matcher(strn);
        while (m.find()) {
            ret++;
            if (m.end() == m.start()) {
                // a zero width match would spin forever otherwise
                if (m.end() >= strn.length()) {
                    break;
                }
                m.region(m.end() + 1, strn.length());
            }
        }
        return ret;
    }

    /**
     * Emulates a javascript global regex ({@code /.../g}) closely enough to
     * reproduce what the original scanner actually did: {@code test()} carries
     * {@code lastIndex} over from one call to the next, and searches
     * {@code strn.substring(lastIndex)} like {@code exec()} does, while
     * {@code String.match()} resets {@code lastIndex} to zero and reports every
     * match. The stateless patterns used by the c scanner do not need this.
     */
    private static class JsGlobalRegex {
        private final Pattern pat;
        private int last_index = 0;

        JsGlobalRegex(Pattern pat) {
            this.pat = pat;
        }

        boolean test(String strn) {
            if (last_index > strn.length()) {
                last_index = 0;
                return false;
            }
            Matcher m = pat.matcher(strn);
            // anchors must see the sliced string, not the whole one
            m.useAnchoringBounds(false);
            m.useTransparentBounds(false);
            m.region(last_index, strn.length());
            if (m.find()) {
                last_index = m.end();
                return true;
            }
            last_index = 0;
            return false;
        }

        int matchCount(String strn) {
            last_index = 0;
            return count(strn, pat);
        }
    }

    /** String.split("\n") keeps trailing empty fields, java's drops them */
    private static String[] split(String strn) {
        return strn.split("\n", -1);
    }

    /** String.trim() only strips <= 0x20, js also strips the unicode spaces */
    private static String js_trim(String strn) {
        int a = 0;
        int b = strn.length();
        while (a < b && js_space(strn.charAt(a))) {
            a++;
        }
        while (b > a && js_space(strn.charAt(b - 1))) {
            b--;
        }
        return strn.substring(a, b);
    }

    private static boolean js_space(char c) {
        return c <= ' ' || c == '\u00A0' || c == '\u1680' || (c >= '\u2000' && c <= '\u200A')
                || c == '\u2028' || c == '\u2029' || c == '\u202F' || c == '\u205F' || c == '\u3000' || c == '\uFEFF';
    }
}
