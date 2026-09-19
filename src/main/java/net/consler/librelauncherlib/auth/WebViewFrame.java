package net.consler.librelauncherlib.auth;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CompletableFuture;

public class WebViewFrame extends JFrame implements AuthCodeProvider
{
    private final CompletableFuture<String> future = new CompletableFuture<>();

    /**
     * An authentication code provider, opens a JavaFX webview with a Microsoft login page
     */

    public WebViewFrame()
    {
        this(600, 600);
    }

    /**
     * An authentication code provider, opens a JavaFX webview with a Microsoft login page
     * @param width Window width
     * @param height Window height
     */
    public WebViewFrame(int width, int height)
    {
        setTitle("Microsoft Authentication");
        setSize(width, height);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setContentPane(new JFXPanel());

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if (!future.isDone()) future.complete(null);
            }
        });
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
            WebView webView = new WebView();
            webView.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");

            Runnable checkUrl = () ->
            {
                String loc = webView.getEngine().getLocation();
                if (loc != null && loc.contains("code=") && !future.isDone())
                {
                    future.complete(loc);
                    SwingUtilities.invokeLater(this::dispose);
                }
            };

            webView.getEngine().locationProperty().addListener((obs, oldVal, newVal) -> checkUrl.run());
            webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> checkUrl.run());

            ((JFXPanel) getContentPane()).setScene(new Scene(webView));
            webView.getEngine().load(url);

            SwingUtilities.invokeLater(() -> setVisible(true));
        });

        return future;
    }
}