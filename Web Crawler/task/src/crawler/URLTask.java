package crawler;

import java.net.URL;

public class URLTask {
    URL url;
    int level;

    public URLTask(URL url, int level) {
        this.url = url;
        this.level = level;
    }
}
