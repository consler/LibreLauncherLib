module librelauncherlib {
    requires com.google.gson;
    requires java.desktop;
    requires java.net.http;
    requires java.xml;

    exports net.consler.librelauncherlib.auth;
    exports net.consler.librelauncherlib.download;
    exports net.consler.librelauncherlib.exception;
    exports net.consler.librelauncherlib.launch;
    exports net.consler.librelauncherlib.utill;
    exports net.consler.librelauncherlib.versions;
}
