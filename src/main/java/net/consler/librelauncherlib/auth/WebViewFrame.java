package net.consler.librelauncherlib.auth;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.util.concurrent.CompletableFuture;

public class WebViewFrame implements AuthCodeProvider
{
    private final CompletableFuture<String> future = new CompletableFuture<>();
    private final int width;
    private final int height;

    public WebViewFrame()
    {
        this(600, 600);
    }

    public WebViewFrame(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    @Override
    public CompletableFuture<String> getAuthCode(String url)
    {
        return start(url);
    }

    public CompletableFuture<String> start(String url)
    {
        Platform.runLater(() ->
        {
            Stage stage = new Stage();
            stage.setTitle("Microsoft Authentication");
            stage.setWidth(width);
            stage.setHeight(height);

            stage.setOnCloseRequest(e ->
            {
                if (!future.isDone()) future.complete(null);
            });

            WebView webView = new WebView();
            webView.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");

            Runnable checkUrl = () -> {
                String loc = webView.getEngine().getLocation();
                if (loc != null && loc.contains("code=") && !future.isDone()) {
                    future.complete(loc);
                    stage.close();
                }
            };

            webView.getEngine().locationProperty().addListener((obs, oldVal, newVal) -> checkUrl.run());
            webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> checkUrl.run());

            stage.setScene(new Scene(webView));
            webView.getEngine().load(url);
            stage.show();
        });

        return future;
    }
}