package _lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import _lsp._Server;
import basix_funx.loggy;
public class _TxtDocSrv implements TextDocumentService {
    
    private final _Server server;
    private final Map<String, String> documents = new ConcurrentHashMap<>();
    
    private static final List<String> DEFAULT_KEYWORDS = Arrays.asList(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch",
        "char", "class", "const", "continue", "default", "do", "double",
        "else", "enum", "extends", "final", "finally", "float", "for",
        "if", "implements", "import", "instanceof", "int", "interface",
        "long", "native", "new", "package", "private", "protected",
        "public", "return", "short", "static", "strictfp", "super",
        "switch", "synchronized", "this", "throw", "throws", "transient",
        "try", "void", "volatile", "while"
    );
    private static final String DEFAULT_LANG = "java";
    private static volatile String cur_lang = DEFAULT_LANG;
    private static volatile List<String> keywords = loadKeywords(null, DEFAULT_KEYWORDS);

    private static List<String> loadKeywords(String file, List<String> fallback) {
        String keywords_file = file != null ? file : System.getProperty("keywordsFile");
        if (keywords_file == null || keywords_file.isEmpty()) {
            return fallback;
        }
        try (Stream<String> lines = Files.lines(Paths.get(keywords_file))) {
            return lines.map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            loggy.w.error("cannot load keywords from " + keywords_file + ": " + e.getMessage(), e);
            return fallback;
        }
    }

    public void setLanguage(SetLangParams params) {
        if (params == null) { return; }
        String lang = params.getLang() == null ? DEFAULT_LANG : params.getLang().toLowerCase();
        String file = params.getFile();
        keywords = loadKeywords(file, DEFAULT_KEYWORDS);
        cur_lang = lang;
        loggy.w.info("lang set to " + lang + ", keywords: " + keywords.size());
    }

    private static void addSnippet(List<CompletionItem> items, String label, String detail, String insert) {
        CompletionItem item = new CompletionItem(label);
        item.setKind(CompletionItemKind.Snippet);
        item.setDetail(detail);
        item.setInsertText(insert);
        item.setInsertTextFormat(InsertTextFormat.Snippet);
        items.add(item);
    }
    
    public _TxtDocSrv(_Server server) {
        this.server = server;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String content = params.getTextDocument().getText();
        documents.put(uri, content);
        
        server.getClient().logMessage(
            new MessageParams(MessageType.Info, "Opened: " + uri)
        );
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        String newContent = params.getContentChanges().get(0).getText();
        documents.put(uri, newContent);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        documents.remove(uri);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        server.getClient().logMessage(
            new MessageParams(MessageType.Info, "Saved: " + params.getTextDocument().getUri())
        );
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            CompletionParams params) {
        
        List<CompletionItem> items = new ArrayList<>();
        String lang = cur_lang;

        // Add keyword completions
        for (String keyword : keywords) {
            CompletionItem item = new CompletionItem(keyword);
            item.setKind(CompletionItemKind.Keyword);
            item.setDetail(lang + " keyword");
            items.add(item);
        }

        // Add some snippets
        if (lang.equals("java")) {
            addSnippet(items, "sysout", "Print to standard output", "System.out.println(${1});");
            addSnippet(items, "main", "Main method", "public static void main(String[] args) {\n    ${1}\n}");
        } else if (lang.equals("c") || lang.equals("cpp")) {
            addSnippet(items, "printf", "Print to standard output", "printf(${1});");
            addSnippet(items, "main", "Main function", "int main(int argc, char** argv) {\n    ${1}\n    return 0;\n}");
        } else if (lang.equals("rust")) {
            addSnippet(items, "println", "Print to standard output", "println!(\"${1}\");");
            addSnippet(items, "main", "Main function", "fn main() {\n    ${1}\n}");
        } else if (lang.equals("d")) {
            addSnippet(items, "writeln", "Print to standard output", "writeln(\"${1}\");");
            addSnippet(items, "main", "Main function", "void main() {\n    ${1}\n}");
        }

        return CompletableFuture.completedFuture(Either.forLeft(items));
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        String uri = params.getTextDocument().getUri();
        Position pos = params.getPosition();
        
        String content = documents.get(uri);
        if (content == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Simple word detection at position (very basic)
        String word = getWordAtPosition(content, pos);
        
        if (word != null && keywords.contains(word)) {
            MarkupContent markup = new MarkupContent();
            markup.setKind(MarkupKind.MARKDOWN);
            markup.setValue("**" + cur_lang + " keyword**: `" + word + "`");
            return CompletableFuture.completedFuture(new Hover(markup));
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> 
            definition(DefinitionParams params) {
        server.printActiveDocument(params);
        // Simplified - just return empty for demo
        return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
    }
    
    private String getWordAtPosition(String content, Position pos) {
        String[] lines = content.split("\n");
        if (pos.getLine() >= lines.length) return null;
        
        String line = lines[pos.getLine()];
        int character = pos.getCharacter();
        if (character >= line.length()) return null;
        
        // Find word boundaries
        int start = character;
        while (start > 0 && Character.isJavaIdentifierPart(line.charAt(start - 1))) {
            start--;
        }
        
        int end = character;
        while (end < line.length() && Character.isJavaIdentifierPart(line.charAt(end))) {
            end++;
        }
        
        if (start < end) {
            return line.substring(start, end);
        }
        
        return null;
    }
}
