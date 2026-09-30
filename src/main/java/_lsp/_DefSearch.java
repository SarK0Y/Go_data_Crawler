package _lsp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import basix_funx.loggy;

/**
 * Resolves F12 style definitions: reads the //rgx:// commands from the
 * i_c_fn_head.opts file and searches the workspace with them.
 */
public class _DefSearch {
    private static final String PLACEHOLDER = "@663@";
    private static final String CLOSE_RGX = ":::";
    private static final int MAX_FILES = 2000;
    private static final int MAX_RESULTS = 2000;

    public static List<Location> find(DefinitionsParams params) {
        List<Location> ret = new ArrayList<>();
        if (params == null || params.getWord() == null || params.getWord().isEmpty()) {
            return ret;
        }
        Path opts = toPath(params.getUri());
        if (opts == null || !Files.isRegularFile(opts)) {
            loggy.w.error("definitions: bad opts uri: " + params.getUri());
            return ret;
        }
        String txt;
        try {
            txt = new String(Files.readAllBytes(opts), StandardCharsets.UTF_8);
        } catch (IOException e) {
            loggy.w.error("definitions: cannot read " + opts + ": " + e.getMessage(), e);
            return ret;
        }

        List<Pattern> rgxs = rgxs(txt, esc(params.getWord()));
        boolean defaulted = rgxs.isEmpty();
        if (defaulted) {
            // no //rgx:// command, fall back to a plain identifier search so
            // F12 still works on a project that never wrote any
            rgxs = default_rgxs(params.getWord());
        }
        List<Pattern> excluded = excluded(txt);
        List<Path> files = files(txt, opts.getParent());
        loggy.w.info("definitions: word=" + params.getWord() + " rgxs=" + rgxs.size()
                + (defaulted ? " (defaulted)" : "") + " files=" + files.size()
                + " excluded=" + excluded.size());

        for (Path file : files) {
            if (ret.size() >= MAX_RESULTS) {
                break;
            }
            String fpath = file.toString();
            if (isExcluded(fpath, excluded)) {
                continue;
            }
            String content;
            try {
                content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            } catch (IOException e) {
                continue;
            }
            List<Integer> line_starts = lineStarts(content);
            for (Pattern rgx : rgxs) {
                Matcher m = rgx.matcher(content);
                while (m.find() && ret.size() < MAX_RESULTS) {
                    ret.add(loc(file, line_starts, content, m.start(), m.end()));
                }
            }
        }
        loggy.w.info("definitions: found " + ret.size() + " locations for " + params.getWord());
        return ret;
    }

    private static Path toPath(String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }
        String p = uri.startsWith("file://") ? Paths.get(java.net.URI.create(uri)).toString() : uri;
        return Paths.get(p);
    }

    private static Location loc(Path file, List<Integer> line_starts, String content, int from, int to) {
        return new Location(
                file.toUri().toString(),
                new Range(pos(line_starts, content, from), pos(line_starts, content, to)));
    }

    private static Position pos(List<Integer> line_starts, String content, int off) {
        if (off < 0) {
            off = 0;
        }
        if (off > content.length()) {
            off = content.length();
        }
        int lo = 0;
        int hi = line_starts.size() - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (line_starts.get(mid) <= off) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return new Position(lo, off - line_starts.get(lo));
    }

    private static List<Integer> lineStarts(String content) {
        List<Integer> ret = new ArrayList<>();
        ret.add(0);
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                ret.add(i + 1);
            }
        }
        return ret;
    }

    // the //rgx:// commands, with the word put in place of the @663@ placeholder
    private static List<Pattern> rgxs(String txt, String word) {
        List<Pattern> ret = new ArrayList<>();
        Matcher m = line("rgx").matcher(txt);
        while (m.find()) {
            Pattern p = toRgx(m.group(1), word);
            if (p != null) {
                ret.add(p);
            }
        }
        return ret;
    }

    /**
     * The search used when the opts file carries no //rgx:// command: the word
     * as a whole identifier. \b is no good around $ or @, which d and rust
     * identifiers use, so the lookarounds spell the boundary out instead.
     */
    private static List<Pattern> default_rgxs(String word) {
        List<Pattern> ret = new ArrayList<>();
        if (word == null || word.isEmpty()) {
            return ret;
        }
        try {
            ret.add(Pattern.compile("(?<![\\w$@])" + Pattern.quote(word) + "(?![\\w$@])"));
        } catch (PatternSyntaxException e) {
            loggy.w.error("definitions: bad default rgx for " + word + ": " + e.getMessage(), e);
        }
        return ret;
    }

    private static List<Pattern> excluded(String txt) {
        List<Pattern> ret = new ArrayList<>();
        Matcher m = line("exclude_path").matcher(txt);
        while (m.find()) {
            Pattern p = toRgx(m.group(1), "");
            if (p != null) {
                ret.add(p);
            }
        }
        return ret;
    }

    private static boolean isExcluded(String path, List<Pattern> excluded) {
        for (Pattern p : excluded) {
            if (p.matcher(path).find()) {
                return true;
            }
        }
        return false;
    }

    // one directive per line: //key: value// , so the closing // is pinned to the
    // end of the line and the value may hold any number of //
    private static Pattern line(String key) {
        return Pattern.compile(
                "^[ \\t]*//[ \\t]*" + key + "[ \\t]*:[ \\t]*(/.*?(?:" + CLOSE_RGX + "[gims]*)?)[ \\t]*//[ \\t\\r]*$",
                Pattern.MULTILINE);
    }

    // /re:::flags or /re/ -> compiled pattern; @663@ is replaced by the searched word
    private static Pattern toRgx(String strn, String word) {
        if (strn == null || strn.isEmpty()) {
            return null;
        }
        String body = strn.startsWith("/") ? strn.substring(1) : strn;
        String flags = "";
        Matcher fm = Pattern.compile("(?s)" + CLOSE_RGX + "([gims]*)$").matcher(body);
        if (fm.find()) {
            flags = fm.group(1);
            body = body.substring(0, fm.start());
        }
        if (body.length() > 1 && body.endsWith("/")) {
            body = body.substring(0, body.length() - 1);
        }
        body = body.replace(PLACEHOLDER, word);
        try {
            return Pattern.compile(body, flagsToJava(flags));
        } catch (Exception e) {
            loggy.w.error("definitions: bad rgx /" + body + "/" + flags + ": " + e.getMessage(), e);
            return null;
        }
    }

    // g has no java counterpart, find() already walks every match
    private static int flagsToJava(String flags) {
        int ret = 0;
        if (flags.contains("i")) { ret |= Pattern.CASE_INSENSITIVE; }
        if (flags.contains("m")) { ret |= Pattern.MULTILINE; }
        if (flags.contains("s")) { ret |= Pattern.DOTALL; }
        return ret;
    }

    private static String esc(String word) {
        return word.replaceAll("([\\\\.^$|?*+()\\[\\]{}])", "\\\\$1");
    }

    // //include_dir:// walks one level, //include_subdir:// walks recursively
    private static List<Path> files(String txt, Path fallback_root) {
        List<Path> dirs = new ArrayList<>();
        List<Path> subdirs = new ArrayList<>();
        collect(txt, "include_dir", dirs);
        collect(txt, "include_subdir", subdirs);
        collect(txt, "include_subdirs", subdirs);

        List<Path> ret = new ArrayList<>();
        for (Path dir : dirs) {
            ret.addAll(walk(dir, false));
        }
        for (Path dir : subdirs) {
            ret.addAll(walk(dir, true));
        }
        if (ret.isEmpty() && fallback_root != null) {
            ret.addAll(walk(fallback_root, true));
        }
        return ret;
    }

    private static void collect(String txt, String key, List<Path> into) {
        Matcher m = Pattern.compile(
                "^[ \\t]*//[ \\t]*" + key + "[ \\t]*:[ \\t]*(/.*?)(?:" + CLOSE_RGX + "[gims]*)?[ \\t]*//[ \\t\\r]*$",
                Pattern.MULTILINE).matcher(txt);
        while (m.find()) {
            into.add(Paths.get(m.group(1)));
        }
    }

    private static List<Path> walk(Path dir, boolean recursive) {
        List<Path> ret = new ArrayList<>();
        if (dir == null || !Files.isDirectory(dir)) {
            return ret;
        }
        try (Stream<Path> s = recursive ? Files.walk(dir) : Files.list(dir)) {
            ret = s.filter(Files::isRegularFile).limit(MAX_FILES).sorted().collect(java.util.stream.Collectors.toList());
        } catch (IOException e) {
            loggy.w.error("definitions: cannot walk " + dir + ": " + e.getMessage(), e);
        }
        return ret;
    }
}
