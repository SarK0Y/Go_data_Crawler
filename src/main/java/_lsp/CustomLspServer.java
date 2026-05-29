package _lsp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
//import io.vavr.control.Either;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import java.util.List;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.CompletionOptions;
//import org.eclipse.lsp4j.HoverCapability;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import _lsp._Server;
// Custom interface for extension methods (beyond standard LSP)
interface CustomLanguageServer extends LanguageServer {
   // @JsonRequest("custom/getData")
    CompletableFuture<CustomResponse> getData(CustomRequest request);

    @JsonNotification("custom/notify")
    void handleNotification(CustomNotification notification);
}

// Custom interface for client methods (server calls client)
interface CustomLanguageClient extends LanguageClient {
    @JsonNotification("custom/clientNotification")
    void sendClientNotification(CustomClientNotification notification);

   // @JsonRequest("custom/askClient")
    CompletableFuture<ClientResponse> askClient(ClientQuestion question);
}

// Request/Response DTOs
class CustomRequest {
    private String query;
    private int count;

    public CustomRequest() {
    }

    public CustomRequest(String query, int count) {
        this.query = query;
        this.count = count;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

class CustomResponse {
    private String result;
    private int total;
    private double score;

    public CustomResponse() {
    }

    public CustomResponse(String result, int total, double score) {
        this.result = result;
        this.total = total;
        this.score = score;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}

class CustomNotification {
    private String message;
    private int priority;
    private long timestamp;

    public CustomNotification() {
    }

    public CustomNotification(String message, int priority, long timestamp) {
        this.message = message;
        this.priority = priority;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

class CustomClientNotification {
    private String status;
    private int code;

    public CustomClientNotification() {
    }

    public CustomClientNotification(String status, int code) {
        this.status = status;
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}

class ClientQuestion {
    private String question;
    private int timeout;

    public ClientQuestion() {
    }

    public ClientQuestion(String question, int timeout) {
        this.question = question;
        this.timeout = timeout;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}

class ClientResponse {
    private String answer;
    private boolean confirmed;
    private int retryCount;

    public ClientResponse() {
    }

    public ClientResponse(String answer, boolean confirmed, int retryCount) {
        this.answer = answer;
        this.confirmed = confirmed;
        this.retryCount = retryCount;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}

public class CustomLspServer implements CustomLanguageServer, LanguageClientAware {

    private CustomLanguageClient client;
    private final TextDocumentService textDocumentService;
    private _Server srv;
    public CustomLspServer() {
       // this.client = new CustomLanguageClient ();
        this.srv = new _Server ();
        this.textDocumentService = new _TxtDocSrv(srv);
        String [] input = {""};
        try {
            this.main (input);
        } catch (Exception e){
           // System.out.println (e.printStackTrace ());
        }
    }

    public void main(String[] args) throws Exception {
        
       JavaLspServer server = new JavaLspServer();

        // Create launcher with custom interfaces
        @SuppressWarnings("unchecked")
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
                this,
                System.in,
                System.out
            //    Executors.newCachedThreadPool(),
              //  (consumer) -> {}
        );

        server.connect((CustomLanguageClient)launcher.getRemoteProxy());
        launcher.startListening();
    }

    // ============ Custom Methods (Called by Client) ============

    @Override
    public CompletableFuture<CustomResponse> getData(CustomRequest request) {
        System.err.println("[SERVER] Received request: query=" + request.getQuery() +
                ", count=" + request.getCount());

        // Process the request
        String processedResult = "Processed: " + request.getQuery().toUpperCase();
        int totalItems = request.getCount() * 10;
        double confidence = 0.95;

        CustomResponse response = new CustomResponse(processedResult, totalItems, confidence);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public void handleNotification(CustomNotification notification) {
        System.err.println("[SERVER] Received notification: " +
                notification.getMessage() +
                " (priority=" + notification.getPriority() +
                ", timestamp=" + notification.getTimestamp() + ")");

        // Send a notification back to client after processing
        if (notification.getPriority() > 5) {
            client.sendClientNotification(
                    new CustomClientNotification("HIGH_PRIORITY_PROCESSED", 200));
        }

        // Ask client a question (request/response)
        client.askClient(new ClientQuestion("Confirm action?", 5000))
                .thenAccept(response -> {
                    System.err.println("[SERVER] Client answered: " +
                            response.getAnswer() +
                            " (confirmed=" + response.isConfirmed() +
                            ", retries=" + response.getRetryCount() + ")");
                });
    }

    // ============ Standard LSP Methods ============

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);

        InitializeResult result = new InitializeResult(capabilities);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public void initialized(InitializedParams params) {
        // Send welcome notification to client
        client.sendClientNotification(
                new CustomClientNotification("SERVER_READY", 100));

        System.err.println("[SERVER] Initialized and ready");
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
        return new JavaWorkspaceService();
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = (CustomLanguageClient) client;
    }
}

// Simple implementations for completeness
class JavaTextDocumentService implements TextDocumentService {
    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        
         List<CompletionItem> items = new ArrayList<>();    
         // populate items as needed
         //items.push 
          Either<List<CompletionItem>, CompletionList> result = Either.forLeft(items);
          //result = Either.getLeft();
         return CompletableFuture.completedFuture(result);
    }
}

class JavaWorkspaceService implements WorkspaceService {
    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    }
}
//fn
/*
    public final class Either<L,R> {
  private final L left;
  private final R right;
  private Either(L l, R r){ this.left=l; this.right=r; }
  public static <L,R> Either<L,R> left(L l){ return new Either<>(l,null); }
  public static <L,R> Either<L,R> right(R r){ return new Either<>(null,r); }
  public boolean isLeft(){ return left!=null; }
  public boolean isRight(){ return right!=null; }
  public L getLeft(){ return left; }
  public R getRight(){ return right; }
}

*/