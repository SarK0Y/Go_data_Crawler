package _lsp;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import com.google.gson.*;
import _lsp.CustomLanguageClient;
import _lsp.JavaTextDocumentService;
import _lsp.JavaWorkspaceService;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
//import io.vavr.control.Either;
// Configuration holder
class ServerConfig {
    boolean enableDiagnostics = true;
    int maxProblems = 100;
    String javaHome = "/usr/lib/jvm/default";
    List<String> classpath = new ArrayList<>();
    Map<String, Object> customSettings = new HashMap<>();
    FormattingOptions formattingOptions = new FormattingOptions(4, false);

    @Override
    public String toString() {
        return "ServerConfig{" +
                "enableDiagnostics=" + enableDiagnostics +
                ", maxProblems=" + maxProblems +
                ", javaHome='" + javaHome + '\'' +
                ", classpath=" + classpath +
                ", formattingOptions=" + formattingOptions +
                '}';
    }
}

class FormattingOptions {
    int indentSize;
    boolean useTabs;
    int maxLineLength;

    public FormattingOptions(int indentSize, boolean useTabs) {
        this.indentSize = indentSize;
        this.useTabs = useTabs;
        this.maxLineLength = 120;
    }

    @Override
    public String toString() {
        return "{indentSize=" + indentSize +
                ", useTabs=" + useTabs +
                ", maxLineLength=" + maxLineLength + "}";
    }
}

interface CustomLanguageClient_ extends CustomLanguageClient {
    @JsonNotification("custom/configurationChanged")
    void configurationChanged(String message);
}

public class JavaLspServer implements LanguageServer, LanguageClientAware {

    private CustomLanguageClient_ client;
    private final JavaTextDocumentService_ textDocumentService;
    private final JavaWorkspaceService_ workspaceService;
    private ServerConfig config = new ServerConfig();
    private final Gson gson = new GsonBuilder().create();

    public JavaLspServer() {
        this.textDocumentService = new JavaTextDocumentService_(this);
        this.workspaceService = new JavaWorkspaceService_(this);
    }

    public static void main(String[] args) throws Exception {
        JavaLspServer server = new JavaLspServer();

        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
                server,
                System.in,
                System.out
        );
          //      Executors.newCachedThreadPool(),
            //    (consumer) -> {
              //  });*/

        server.connect((CustomLanguageClient_)launcher.getRemoteProxy());
        launcher.startListening();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        System.err.println("[SERVER] Initializing...");

        // Request specific settings during initialization
        if (params.getInitializationOptions() != null) {
            parseInitializationOptions(params.getInitializationOptions());
        }

        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Incremental);
        capabilities.setCompletionProvider(new CompletionOptions(true, Arrays.asList(".", "@")));
        capabilities.setHoverProvider(true);
        capabilities.setDefinitionProvider(true);
        capabilities.setReferencesProvider(true);
        capabilities.setDocumentFormattingProvider(true);
        capabilities.setCodeActionProvider(true);

        InitializeResult result = new InitializeResult(capabilities);
        result.setServerInfo(new ServerInfo("Java LSP Server", "1.0.0"));

        return CompletableFuture.completedFuture(result);
    }

    @Override
    public void initialized(InitializedParams params) {
        System.err.println("[SERVER] Initialized. Current config: " + config);
        client.configurationChanged("Server initialized with config: " + config);
    }

    private void parseInitializationOptions(Object options) {
        if (options instanceof JsonObject) {
            JsonObject json = (JsonObject) options;

            if (json.has("javaHome") && json.get("javaHome").isJsonPrimitive()) {
                config.javaHome = json.get("javaHome").getAsString();
            }

            if (json.has("maxProblems") && json.get("maxProblems").isJsonPrimitive()) {
                config.maxProblems = json.get("maxProblems").getAsInt();
            }

            System.err.println("[SERVER] Parsed initial options: javaHome=" + config.javaHome);
        }
    }

    public ServerConfig getConfig() {
        return config;
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = (CustomLanguageClient_) client;
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(0);
    }
}

class JavaWorkspaceService_ extends JavaWorkspaceService {

    private final JavaLspServer server;

    public JavaWorkspaceService_(JavaLspServer server) {
        this.server = server;
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        String command = params.getCommand();
        List<Object> args = params.getArguments();

        System.err.println("[SERVER] Executing command: " + command + " with args: " + args);

        switch (command) {
            case "java.server.reloadConfig":
                System.err.println("[SERVER] Reloading configuration...");
                return CompletableFuture.completedFuture("Configuration reloaded");

            case "java.server.clearCache":
                System.err.println("[SERVER] Clearing cache...");
                return CompletableFuture.completedFuture("Cache cleared");

            default:
                return CompletableFuture.completedFuture("Unknown command: " + command);
        }
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        System.err.println("[SERVER] Configuration changed!");
        ServerConfig config = server.getConfig();

        // Parse the new configuration
        Object settings = params.getSettings();

        if (settings instanceof JsonObject) {
            JsonObject root = (JsonObject) settings;

            // Typical VS Code structure: settings.java.server.*
            if (root.has("java")) {
                JsonObject javaSettings = root.getAsJsonObject("java");

                if (javaSettings.has("server")) {
                    JsonObject serverSettings = javaSettings.getAsJsonObject("server");

                    // Parse each setting
                    if (serverSettings.has("enableDiagnostics")) {
                        config.enableDiagnostics = serverSettings.get("enableDiagnostics").getAsBoolean();
                        System.err.println("[SERVER] enableDiagnostics = " + config.enableDiagnostics);
                    }

                    if (serverSettings.has("maxProblems")) {
                        config.maxProblems = serverSettings.get("maxProblems").getAsInt();
                        System.err.println("[SERVER] maxProblems = " + config.maxProblems);
                    }

                    if (serverSettings.has("javaHome")) {
                        config.javaHome = serverSettings.get("javaHome").getAsString();
                        System.err.println("[SERVER] javaHome = " + config.javaHome);
                    }

                    if (serverSettings.has("classpath")) {
                        config.classpath.clear();
                        JsonArray cp = serverSettings.getAsJsonArray("classpath");
                        for (JsonElement element : cp) {
                            config.classpath.add(element.getAsString());
                        }
                        System.err.println("[SERVER] classpath = " + config.classpath);
                    }

                    if (serverSettings.has("formatting")) {
                        JsonObject fmt = serverSettings.getAsJsonObject("formatting");

                        if (fmt.has("indentSize")) {
                            config.formattingOptions.indentSize = fmt.get("indentSize").getAsInt();
                        }

                        if (fmt.has("useTabs")) {
                            config.formattingOptions.useTabs = fmt.get("useTabs").getAsBoolean();
                        }

                        if (fmt.has("maxLineLength")) {
                            config.formattingOptions.maxLineLength = fmt.get("maxLineLength").getAsInt();
                        }

                        System.err.println("[SERVER] formatting = " + config.formattingOptions);
                    }

                    // Handle nested objects
                    if (serverSettings.has("customSettings")) {
                        JsonObject custom = serverSettings.getAsJsonObject("customSettings");
                        config.customSettings.clear();

                        for (Map.Entry<String, JsonElement> entry : custom.entrySet()) {
                            if (entry.getValue().isJsonPrimitive()) {
                                JsonPrimitive primitive = entry.getValue().getAsJsonPrimitive();
                                if (primitive.isString()) {
                                    config.customSettings.put(entry.getKey(), primitive.getAsString());
                                } else if (primitive.isNumber()) {
                                    config.customSettings.put(entry.getKey(), primitive.getAsNumber());
                                } else if (primitive.isBoolean()) {
                                    config.customSettings.put(entry.getKey(), primitive.getAsBoolean());
                                }
                            }
                        }
                        System.err.println("[SERVER] customSettings = " + config.customSettings);
                    }
                }
            }

            // Notify client about config change
            CustomLanguageClient_ client = (CustomLanguageClient_) server;
            client.configurationChanged("Configuration updated: " + config);
        }

        System.err.println("[SERVER] New config: " + config);
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        System.err.println("[SERVER] Watched files changed:");
        for (FileEvent event : params.getChanges()) {
            System.err.println("  " + event.getType() + ": " + event.getUri());
        }
    }
}

class JavaTextDocumentService_ extends JavaTextDocumentService {

    private final JavaLspServer server;
    private final Map<String, TextDocumentItem> documents = new HashMap<>();

    public JavaTextDocumentService_(JavaLspServer server) {
        this.server = server;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        documents.put(params.getTextDocument().getUri(), params.getTextDocument());
        System.err.println("[SERVER] Opened: " + params.getTextDocument().getUri());

        // Use config to determine diagnostics behavior
        ServerConfig config = server.getConfig();
        if (config.enableDiagnostics) {
            System.err.println("[SERVER] Diagnostics enabled, max problems: " + config.maxProblems);
        }
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        TextDocumentItem doc = documents.get(uri);

        if (doc != null && !params.getContentChanges().isEmpty()) {
            String newText = params.getContentChanges().get(0).getText();
            documents.put(uri, new TextDocumentItem(
                    doc.getUri(), doc.getLanguageId(), doc.getVersion(), newText));
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        documents.remove(params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        System.err.println("[SERVER] Saved: " + params.getTextDocument().getUri());
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            CompletionParams params) {

        ServerConfig config = server.getConfig();

        List<CompletionItem> items = new ArrayList<>();

        // Add completion items based on config
        if (config.enableDiagnostics) {
            CompletionItem item = new CompletionItem("sysout");
            item.setKind(CompletionItemKind.Snippet);
            item.setDetail("System.out.println");
            item.setInsertText("System.out.println(${1});");
            item.setInsertTextFormat(InsertTextFormat.Snippet);
            items.add(item);
        }

        return CompletableFuture.completedFuture(Either.forLeft(items));
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
            DefinitionParams params) {
        return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        ServerConfig config = server.getConfig();
        FormattingOptions options = config.formattingOptions;

        System.err.println("[SERVER] Formatting with: indentSize=" + options.indentSize +
                ", useTabs=" + options.useTabs);

        // Apply formatting based on config...
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}
