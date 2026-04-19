package _lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class _TxtDocSrv implements TextDocumentService {
    
    private final JavaLspServer server;
    private final Map<String, String> documents = new ConcurrentHashMap<>();
    
    // Simple keyword completions for demo
    private static final List<String> JAVA_KEYWORDS = Arrays.asList(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch",
        "char", "class", "const", "continue", "default", "do", "double",
        "else", "enum", "extends", "final", "finally", "float", "for",
        "if", "implements", "import", "instanceof", "int", "interface",
        "long", "native", "new", "package", "private", "protected",
        "public", "return", "short", "static", "strictfp", "super",
        "switch", "synchronized", "this", "throw", "throws", "transient",
        "try", "void", "volatile", "while"
    );
    
    public JavaTextDocumentService(JavaLspServer server) {
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
        
        // Add keyword completions
        for (String keyword : JAVA_KEYWORDS) {
            CompletionItem item = new CompletionItem(keyword);
            item.setKind(CompletionItemKind.Keyword);
            item.setDetail("Java keyword");
            items.add(item);
        }
        
        // Add some snippets
        CompletionItem sysoutItem = new CompletionItem("sysout");
        sysoutItem.setKind(CompletionItemKind.Snippet);
        sysoutItem.setDetail("Print to standard output");
        sysoutItem.setInsertText("System.out.println(${1});");
        sysoutItem.setInsertTextFormat(InsertTextFormat.Snippet);
        items.add(sysoutItem);
        
        CompletionItem mainItem = new CompletionItem("main");
        mainItem.setKind(CompletionItemKind.Snippet);
        mainItem.setDetail("Main method");
        mainItem.setInsertText("public static void main(String[] args) {\n    ${1}\n}");
        mainItem.setInsertTextFormat(InsertTextFormat.Snippet);
        items.add(mainItem);
        
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
        
        if (word != null && JAVA_KEYWORDS.contains(word)) {
            MarkupContent markup = new MarkupContent();
            markup.setKind(MarkupKind.MARKDOWN);
            markup.setValue("**Java keyword**: `" + word + "`");
            return CompletableFuture.completedFuture(new Hover(markup));
        }
        
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> 
            definition(DefinitionParams params) {
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
