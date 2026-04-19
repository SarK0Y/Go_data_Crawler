package _lsp;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class _Server implements LanguageServer, LanguageClientAware {
    
    private LanguageClient client;
    private final TextDocumentService textDocumentService;
    private final WorkspaceService workspaceService;
    
    public _Server() {
        this.textDocumentService = new JavaTextDocumentService(this);
        this.workspaceService = new JavaWorkspaceService();
    }

    public static void main(String[] args) {
        try {
            // Option 1: Stdio (most common for LSP)
            startStdio();
            
            // Option 2: TCP Socket (alternative)
            // startSocket(5007);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void startStdio() throws Exception {
        _Server server = new _Server();
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
            server,
            System.in,
            System.out,
            Executors.newCachedThreadPool(),
            (consumer) -> {}
        );
        server.connect(launcher.getRemoteProxy());
        launcher.startListening();
    }
    
    private static void startSocket(int port) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.err.println("LSP Server listening on port " + port);
            
            while (true) {
                Socket socket = serverSocket.accept();
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                
                _Server server = new _Server();
                Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
                    server, in, out, Executors.newCachedThreadPool(), (consumer) -> {}
                );
                server.connect(launcher.getRemoteProxy());
                launcher.startListening();
            }
        }
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();
        
        // Text document sync
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        
        // Completion support
        CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setTriggerCharacters(List.of(".", "@", "#"));
        capabilities.setCompletionProvider(completionOptions);
        
        // Hover support
        capabilities.setHoverProvider(true);
        
        // Definition support
        capabilities.setDefinitionProvider(true);
        
        InitializeResult result = new InitializeResult(capabilities);
        
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setName("Java LSP Server");
        serverInfo.setVersion("1.0.0");
        result.setServerInfo(serverInfo);
        
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public void initialized(InitializedParams params) {
        // Server is ready - notify client
        client.logMessage(new MessageParams(MessageType.Info, "Java LSP Server initialized"));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(0);
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
        this.client = client;
    }
    
    public LanguageClient getClient() {
        return client;
    }
}