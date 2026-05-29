package _lsp;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.concurrent.CompletableFuture;

public class _WorkspaceService implements WorkspaceService {

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        // Handle configuration changes
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        // Handle file changes
    }
}
