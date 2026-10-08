package net.consler.librelauncherlib.auth;

import ca.weblite.webview.swing.WebViewComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Microsoft login through an embedded webview, run in a separate helper JVM
 * since the native webview library can crash the whole JVM.
 */
public class NativeWebViewFrame implements AuthCodeProvider
{
    private static final String RESULT_PREFIX = "LIBRELAUNCHER_AUTH_RESULT:";
    private static final int DEFAULT_SIZE = 600;
    private static final long TIMEOUT_MINUTES = 10;
    private static final String[] FORWARDED_PROPERTIES = {"awt.toolkit.name", "ca.weblite.webview.mode"};

    private final int width;
    private final int height;

    public NativeWebViewFrame()
    {
        this(DEFAULT_SIZE, DEFAULT_SIZE);
    }

    public NativeWebViewFrame(int width, int height)
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
        CompletableFuture<String> future = new CompletableFuture<String>().orTimeout(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        AtomicReference<Process> processRef = new AtomicReference<>();

        future.whenComplete((result, error) ->
        {
            Process process = processRef.get();
            if (process != null) process.destroyForcibly();
        });

        Thread worker = new Thread(() ->
        {
            try
            {
                Process process = new ProcessBuilder(buildCommand(url)).redirectError(ProcessBuilder.Redirect.INHERIT).start();
                processRef.set(process);

                if (future.isDone())
                {
                    process.destroyForcibly();
                    return;
                }

                boolean gotResult = false;
                String result = null;

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        if (line.startsWith(RESULT_PREFIX))
                        {
                            String value = line.substring(RESULT_PREFIX.length()).trim();
                            result = value.isEmpty() ? null : value;
                            gotResult = true;
                            break;
                        }
                    }
                }

                if (gotResult)
                {
                    future.complete(result);
                }
                else
                {
                    int exitCode = process.waitFor();
                    future.completeExceptionally(new IOException("Login window exited without a result (exit code " + exitCode + ")"));
                }
            }
            catch (Exception e)
            {
                future.completeExceptionally(e);
            }
        }, "webview-auth-launcher");

        worker.setDaemon(true);
        worker.start();

        return future;
    }

    private List<String> buildCommand(String url)
    {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String javaBin = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString();

        List<String> command = new ArrayList<>();
        command.add(javaBin);

        command.add("-XX:+IgnoreUnrecognizedVMOptions");
        command.add("-XX:-CreateCoredumpOnCrash");
        command.add("-XX:ErrorFile=" + Path.of(System.getProperty("java.io.tmpdir"), "librelauncher-webview-hs_err_pid%p.log"));

        for (String key : FORWARDED_PROPERTIES)
        {
            String value = System.getProperty(key);
            if (value != null) command.add("-D" + key + "=" + value);
        }

        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(LoginWindow.class.getName());
        command.add(url);
        command.add(String.valueOf(width));
        command.add(String.valueOf(height));
        return command;
    }

    public static final class LoginWindow
    {
        private static final int POLL_INTERVAL_MS = 250;
        private static final AtomicBoolean finished = new AtomicBoolean(false);

        static void main(String[] args)
        {
            String url = args[0];
            int width = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_SIZE;
            int height = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_SIZE;

            SwingUtilities.invokeLater(() ->
            {
                try
                {
                    show(url, width, height);
                }
                catch (Throwable t)
                {
                    Runtime.getRuntime().halt(1);
                }
            });
        }

        private static void show(String url, int width, int height)
        {
            JFrame frame = new JFrame("Microsoft Authentication");
            frame.setSize(width, height);
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e)
                {
                    finish("");
                }
            });

            WebViewComponent webView = WebViewComponent.create();
            webView.setUrl(url);

            frame.add(webView, BorderLayout.CENTER);
            frame.setVisible(true);

            AtomicBoolean evalPending = new AtomicBoolean(false);

            Timer urlChecker = new Timer(POLL_INTERVAL_MS, e ->
            {
                if (finished.get() || !evalPending.compareAndSet(false, true)) return;

                webView.evalAsync("return window.location.href;")
                        .orTimeout(3, TimeUnit.SECONDS)
                        .whenComplete((locJson, error) ->
                        {
                            evalPending.set(false);

                            if (error != null || locJson == null) return;

                            String currentLoc = locJson.replace("\"", "");
                            if (currentLoc.contains("code=")) finish(currentLoc);
                        });
            });
            urlChecker.setRepeats(true);
            urlChecker.start();
        }

        private static void finish(String value)
        {
            if (!finished.compareAndSet(false, true)) return;

            System.out.println(RESULT_PREFIX + value);
            System.out.flush();
            Runtime.getRuntime().halt(0);
        }
    }
}