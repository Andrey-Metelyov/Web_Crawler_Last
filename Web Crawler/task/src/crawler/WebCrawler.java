package crawler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawler extends JFrame {
    JTextField textField;
    JLabel titleValueLabel;
    JTable table;
    JTextField exportFilePath;
    private JTextField workersTextField;
    private JTextField depthTextField;
    private JTextField timeLimitTextField;
    private JTextField exportUrlTextField;
    private JToggleButton runButton;
    private JTextField urlTextField;
    private JCheckBox depthCheckBox;
    private JCheckBox timeLimitCheckBox;
    private JLabel parsedLabel;
//    private ConcurrentLinkedQueue<URLTask> taskQueue = new ConcurrentLinkedQueue<>();
//    private ConcurrentLinkedQueue<URLRecord> resultQueue = new ConcurrentLinkedQueue<>();
    private ConcurrentHashMap<String, String> resultMap = new ConcurrentHashMap<>();
    private ConcurrentSkipListSet<String> processedLinks = new ConcurrentSkipListSet<>();
    private ExecutorService executor;
    private JLabel elapsedTimeValueLabel;
    private Timer timer;

    class URLRecord {
        String url;
        String title;

        public URLRecord(String url, String title) {
            this.url = url;
            this.title = title;
        }

        @Override
        public String toString() {
            return "URLRecord{" +
                    "url='" + url + '\'' +
                    ", title='" + title + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            URLRecord urlRecord = (URLRecord) o;

            if (!url.equals(urlRecord.url)) return false;
            return title.equals(urlRecord.title);
        }

        @Override
        public int hashCode() {
            int result = url.hashCode();
            result = 31 * result + title.hashCode();
            return result;
        }
    }

    public WebCrawler() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 225);
        setTitle("Web Crawler");

        createInterface();

//        pack();

        setVisible(true);
    }

    private void createInterface() {
        JPanel panel = new JPanel();

        JButton exportButton = new JButton("Save");
        depthCheckBox = new JCheckBox("Enabled");
        timeLimitCheckBox = new JCheckBox("Enabled");
        parsedLabel = new JLabel("0");
        urlTextField = new JTextField("https://hi.hyperskill.org/", 60);
        workersTextField = new JTextField("5");
        depthTextField = new JTextField("50");
        timeLimitTextField = new JTextField("120");
        exportUrlTextField = new JTextField("d:\\temp\\lastCrawl.txt");
        runButton = new JToggleButton("Run");

        depthCheckBox.setName("DepthCheckBox");
        depthCheckBox.setSelected(true);
        depthTextField.setName("DepthTextField");
        exportButton.setName("ExportButton");
        exportUrlTextField.setName("ExportUrlTextField");
        parsedLabel.setName("ParsedLabel");
        runButton.setName("RunButton");
        urlTextField.setName("UrlTextField");

        runButton.addActionListener(e -> runButtonClicked());
        exportButton.addActionListener(e -> exportButtonClicked());

        JLabel elapsedTimeLabel = new JLabel("Elapsed time:");
        elapsedTimeValueLabel = new JLabel("0:00");
        JLabel exportLabel = new JLabel("Export:");
        JLabel maximumDepthLabel = new JLabel("Maximum depth:");
        JLabel parsedPagesLabel = new JLabel("Parsed pages:");
        JLabel secondsLabel = new JLabel("seconds");
        JLabel startUrlLabel = new JLabel("Start URL:");
        JLabel timeLimitLabel = new JLabel("Time limit:");
        JLabel workersLabel = new JLabel("Workers:");

        SpringLayout layout = new SpringLayout();
        panel.setLayout(layout);
        panel.add(startUrlLabel);
        panel.add(urlTextField);
        panel.add(runButton);
        panel.add(workersLabel);
        panel.add(workersTextField);
        panel.add(maximumDepthLabel);
        panel.add(depthTextField);
        panel.add(depthCheckBox);
        panel.add(timeLimitLabel);
        panel.add(timeLimitTextField);
        panel.add(secondsLabel);
        panel.add(timeLimitCheckBox);
        panel.add(elapsedTimeLabel);
        panel.add(elapsedTimeValueLabel);
        panel.add(parsedPagesLabel);
        panel.add(parsedLabel);
        panel.add(exportLabel);
        panel.add(exportUrlTextField);
        panel.add(exportButton);

        // labels
        layout.putConstraint(SpringLayout.WEST, startUrlLabel, 5, SpringLayout.WEST, panel);
        layout.putConstraint(SpringLayout.WEST, workersLabel, 5, SpringLayout.WEST, panel);
        layout.putConstraint(SpringLayout.WEST, maximumDepthLabel, 5, SpringLayout.WEST, panel);
        layout.putConstraint(SpringLayout.WEST, timeLimitLabel, 5, SpringLayout.WEST, panel);
        layout.putConstraint(SpringLayout.WEST, elapsedTimeLabel, 5, SpringLayout.WEST, panel);
        layout.putConstraint(SpringLayout.WEST, parsedPagesLabel, 5, SpringLayout.WEST, panel);
        layout.putConstraint(SpringLayout.WEST, exportLabel, 5, SpringLayout.WEST, panel);

        layout.putConstraint(SpringLayout.EAST, startUrlLabel, 0, SpringLayout.EAST, maximumDepthLabel);
        layout.putConstraint(SpringLayout.EAST, workersLabel, 0, SpringLayout.EAST, maximumDepthLabel);
        layout.putConstraint(SpringLayout.EAST, timeLimitLabel, 0, SpringLayout.EAST, maximumDepthLabel);
        layout.putConstraint(SpringLayout.EAST, elapsedTimeLabel, 0, SpringLayout.EAST, maximumDepthLabel);
        layout.putConstraint(SpringLayout.EAST, parsedPagesLabel, 0, SpringLayout.EAST, maximumDepthLabel);
        layout.putConstraint(SpringLayout.EAST, exportLabel, 0, SpringLayout.EAST, maximumDepthLabel);

        // Run button
        layout.putConstraint(SpringLayout.NORTH, runButton, 5, SpringLayout.NORTH, panel);
        layout.putConstraint(SpringLayout.EAST, runButton, -5, SpringLayout.EAST, panel);
        // Start URL
        layout.putConstraint(SpringLayout.EAST, urlTextField, -5, SpringLayout.WEST, runButton);
        layout.putConstraint(SpringLayout.WEST, urlTextField, 5, SpringLayout.EAST, startUrlLabel);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, urlTextField, 0, SpringLayout.VERTICAL_CENTER, runButton);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, startUrlLabel, 0, SpringLayout.VERTICAL_CENTER, runButton);
        // Workers text field
        layout.putConstraint(SpringLayout.NORTH, workersTextField, 5, SpringLayout.SOUTH, runButton);
        layout.putConstraint(SpringLayout.EAST, workersTextField, -5, SpringLayout.EAST, panel);
        layout.putConstraint(SpringLayout.WEST, workersTextField, 5, SpringLayout.EAST, workersLabel);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, workersLabel, 0, SpringLayout.VERTICAL_CENTER, workersTextField);
        // Maximum depth text field
        layout.putConstraint(SpringLayout.NORTH, depthTextField, 5, SpringLayout.SOUTH, workersTextField);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, depthCheckBox, 0, SpringLayout.VERTICAL_CENTER, depthTextField);
        layout.putConstraint(SpringLayout.EAST, depthCheckBox, -5, SpringLayout.EAST, panel);
        layout.putConstraint(SpringLayout.EAST, depthTextField, -5, SpringLayout.WEST, depthCheckBox);
        layout.putConstraint(SpringLayout.WEST, depthTextField, 5, SpringLayout.EAST, maximumDepthLabel);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, maximumDepthLabel, 0, SpringLayout.VERTICAL_CENTER, depthTextField);
        // Time limit text field
        layout.putConstraint(SpringLayout.NORTH, timeLimitTextField, 5, SpringLayout.SOUTH, depthTextField);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, timeLimitCheckBox, 0, SpringLayout.VERTICAL_CENTER, timeLimitTextField);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, secondsLabel, 0, SpringLayout.VERTICAL_CENTER, timeLimitTextField);
        layout.putConstraint(SpringLayout.EAST, timeLimitCheckBox, -5, SpringLayout.EAST, panel);
        layout.putConstraint(SpringLayout.EAST, secondsLabel, 0, SpringLayout.EAST, depthTextField);
        layout.putConstraint(SpringLayout.EAST, timeLimitTextField, -5, SpringLayout.WEST, secondsLabel);
        layout.putConstraint(SpringLayout.WEST, timeLimitTextField, 5, SpringLayout.EAST, timeLimitLabel);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, timeLimitLabel, 0, SpringLayout.VERTICAL_CENTER, timeLimitTextField);
        // Time label
        layout.putConstraint(SpringLayout.NORTH, elapsedTimeValueLabel, 5, SpringLayout.SOUTH, timeLimitTextField);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, elapsedTimeLabel, 0, SpringLayout.VERTICAL_CENTER, elapsedTimeValueLabel);
        layout.putConstraint(SpringLayout.WEST, elapsedTimeValueLabel, 0, SpringLayout.WEST, timeLimitTextField);
        // Parsed pages label
        layout.putConstraint(SpringLayout.NORTH, parsedLabel, 5, SpringLayout.SOUTH, elapsedTimeValueLabel);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, parsedPagesLabel, 0, SpringLayout.VERTICAL_CENTER, parsedLabel);
        layout.putConstraint(SpringLayout.WEST, parsedLabel, 0, SpringLayout.WEST, elapsedTimeValueLabel);
        // Save button
        layout.putConstraint(SpringLayout.NORTH, exportButton, 5, SpringLayout.SOUTH, parsedLabel);
        layout.putConstraint(SpringLayout.EAST, exportButton, -5, SpringLayout.EAST, panel);
        // Export text field
        layout.putConstraint(SpringLayout.EAST, exportUrlTextField, -5, SpringLayout.WEST, exportButton);
        layout.putConstraint(SpringLayout.WEST, exportUrlTextField, 5, SpringLayout.EAST, exportLabel);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, exportUrlTextField, 0, SpringLayout.VERTICAL_CENTER, exportButton);
        layout.putConstraint(SpringLayout.VERTICAL_CENTER, exportLabel, 0, SpringLayout.VERTICAL_CENTER, exportButton);
        // bottom
        layout.putConstraint(SpringLayout.SOUTH, panel, 5, SpringLayout.SOUTH, exportUrlTextField);
//        add(panel, BorderLayout.CENTER);
        add(panel);

        setPreferredSize(layout.preferredLayoutSize(this));
    }

    private void exportButtonClicked() {
        System.out.println("Export clicked!");
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : resultMap.entrySet()) {
            sb.append(entry.getKey());
            sb.append(System.lineSeparator());
            sb.append(entry.getValue());
            sb.append(System.lineSeparator());
        }
        sb.delete(sb.length() - System.lineSeparator().length(), sb.length());
        saveToFile(exportUrlTextField.getText(), sb.toString());
    }

    private void runButtonClicked() {
        System.out.println("Run clicked!");
        resultMap.clear();
        processedLinks.clear();

        URL startUrl = null;
        try {
            startUrl = new URL(urlTextField.getText());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        boolean maxDepthEnabled = depthCheckBox.isSelected();
        boolean timeLimitEnabled = timeLimitCheckBox.isSelected();
        int workers = Integer.parseInt(workersTextField.getText());
        int maxDepth = maxDepthEnabled ? Integer.parseInt(depthTextField.getText()) : Integer.MAX_VALUE;
        int timeLimit = Integer.parseInt(timeLimitTextField.getText());
        long startTime = System.currentTimeMillis();

        executor = Executors.newFixedThreadPool(workers);

        timer = new Timer(1000, e -> {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            long minutes = elapsed / 60;
            int seconds = (int) (elapsed % 60);
            elapsedTimeValueLabel.setText(minutes + ":" + (seconds < 10 ? "0" + seconds : seconds));
            parsedLabel.setText(String.valueOf(resultMap.size()));
            if (timeLimitEnabled && elapsed > timeLimit) {
                System.out.println("time limit reached!");
                executor.shutdownNow();
                timer.stop();
            }
        });

        timer.start();


//        URL finalStartUrl = startUrl;
//        executor.submit(() -> {
//            processUrl(finalStartUrl, 0);
//        });
        startTask(startUrl, 0, maxDepth);
//        processUrl(startUrl);
//        System.out.println(visitPage(startUrl).get());
    }

    private void startTask(URL url, int level, int maxDepth) {
        if (!processedLinks.contains(url.toString())) {
            processedLinks.add(url.toString());
            System.out.println("Start task: " + url + ", level: " + level);
            executor.submit(() -> {
                processUrl(url, level, maxDepth);
            });
        } else {
            System.out.println("Already done: " + url);
        }
    }

    private void processUrl(URL url, int level, int maxDepth) {
        try {
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
            String contentType = connection.getContentType();
//        System.out.println(url + " content type: " + contentType);
            if (contentType != null && contentType.startsWith("text/html")) {
                InputStream inputStream = connection.getInputStream();
                String siteText = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                String siteTitle = findTitle(siteText);
                List<URL> siteUrls = findUrls(url, siteText);
                if (level < maxDepth) {
                    List<URLTask> tasks = makeURLTasks(siteUrls, level);
                    for (URLTask task : tasks) {
                        startTask(task.url, task.level, maxDepth);
                    }
                } else {
                    System.out.println("depth " + maxDepth + " reached");
                }
//                System.out.println(url + siteTitle + siteUrls);

//                resultQueue.add(new URLRecord(url.toString(), siteTitle));
                resultMap.put(url.toString(), siteTitle);
                parsedLabel.setText(String.valueOf(resultMap.size()));
//                taskQueue.addAll(tasks);
            }
        } catch (IOException e) {
            System.out.println("bad url: " + url);
//            e.printStackTrace();
        }
    }

    private List<URLTask> makeURLTasks(List<URL> siteUrls, int level) {
        List<URLTask> result = new ArrayList<>();
        for (URL url : siteUrls) {
            result.add(new URLTask(url, level + 1));
        }
        return result;
    }

    private List<URL> findUrls(URL context, String siteText) {
        List<URL> result = new ArrayList<>();
        List<String> aTags = getATags(siteText);
        List<String> hrefAttributes = getHrefAttributes(aTags);
//        System.out.println("hrefAttributes: " + hrefAttributes);
//        List<String> links = fixLinks(address, hrefAttributes);
//        System.out.println("links: " + links);
        for (String hrefAttribute : hrefAttributes) {
            try {
                result.add(new URL(context, hrefAttribute));
            } catch (MalformedURLException e) {
                System.out.println("bad link: " + context + " " + hrefAttribute);
//                e.printStackTrace();
            }
        }
//        System.out.println("links: " + result);
        return result;
    }

//    private void createInterface() {
//        JPanel panel = new JPanel();
//
//        JLabel urlLabel = new JLabel("URL:");
//        JLabel exportLabel = new JLabel("Export:");
//        textField = new JTextField("https://www.wikipedia.org", 20);
//        textField.setName("UrlTextField");
//
//        exportFilePath = new JTextField("d:\\temp\\last.txt");
//        exportFilePath.setName("ExportUrlTextField");
//
//        JButton button = new JButton("Parse");
//        button.setName("RunButton");
//        button.addActionListener(e -> parseClicked());
//
//        JButton exportButton = new JButton("Save");
//        exportButton.setName("ExportButton");
//        exportButton.addActionListener(e -> exportClicked());
//
//        JLabel titleLabel = new JLabel("Title:");
//
//        titleValueLabel = new JLabel("Example text");
//        titleValueLabel.setName("TitleLabel");
//
//        table = createTable();
//
//        JScrollPane scrollPane = new JScrollPane(table);
//        table.setFillsViewportHeight(true);
//        table.setEnabled(false);
//
//        SpringLayout layout = new SpringLayout();
//        panel.setLayout(layout);
//        panel.add(urlLabel);
//        panel.add(textField);
//        panel.add(button);
//        panel.add(titleLabel);
//        panel.add(titleValueLabel);
//        panel.add(scrollPane);
//        panel.add(exportLabel);
//        panel.add(exportFilePath);
//        panel.add(exportButton);
//
//        layout.putConstraint(SpringLayout.NORTH, button, 5, SpringLayout.NORTH, panel);
//        layout.putConstraint(SpringLayout.EAST, button, -5, SpringLayout.EAST, panel);
//        layout.putConstraint(SpringLayout.VERTICAL_CENTER, textField, 0, SpringLayout.VERTICAL_CENTER, button);
//        layout.putConstraint(SpringLayout.EAST, textField, -5, SpringLayout.WEST, button);
//        layout.putConstraint(SpringLayout.WEST, urlLabel, 5, SpringLayout.WEST, panel);
//        layout.putConstraint(SpringLayout.WEST, textField, 5, SpringLayout.EAST, urlLabel);
//        layout.putConstraint(SpringLayout.VERTICAL_CENTER, urlLabel, 0, SpringLayout.VERTICAL_CENTER, button);
//
//        layout.putConstraint(SpringLayout.WEST, titleLabel, 5, SpringLayout.WEST, panel);
//        layout.putConstraint(SpringLayout.NORTH, titleLabel, 5, SpringLayout.SOUTH, textField);
//        layout.putConstraint(SpringLayout.WEST, titleValueLabel, 5, SpringLayout.EAST, titleLabel);
//        layout.putConstraint(SpringLayout.NORTH, titleValueLabel, 5, SpringLayout.SOUTH, textField);
//
//        layout.putConstraint(SpringLayout.WEST, scrollPane, 5, SpringLayout.WEST, panel);
//        layout.putConstraint(SpringLayout.EAST, scrollPane, -5, SpringLayout.EAST, panel);
//        layout.putConstraint(SpringLayout.NORTH, scrollPane, 5, SpringLayout.SOUTH, titleLabel);
//        layout.putConstraint(SpringLayout.SOUTH, scrollPane, -5, SpringLayout.NORTH, exportButton);
//
//        layout.putConstraint(SpringLayout.SOUTH, exportButton, -5, SpringLayout.SOUTH, panel);
//        layout.putConstraint(SpringLayout.EAST, exportButton, -5, SpringLayout.EAST, panel);
//        layout.putConstraint(SpringLayout.VERTICAL_CENTER, exportFilePath, 0, SpringLayout.VERTICAL_CENTER, exportButton);
//        layout.putConstraint(SpringLayout.EAST, exportFilePath, -5, SpringLayout.WEST, button);
//        layout.putConstraint(SpringLayout.WEST, exportLabel, 5, SpringLayout.WEST, panel);
//        layout.putConstraint(SpringLayout.WEST, exportFilePath, 5, SpringLayout.EAST, exportLabel);
//        layout.putConstraint(SpringLayout.VERTICAL_CENTER, exportLabel, 0, SpringLayout.VERTICAL_CENTER, exportButton);
//
//        add(panel, BorderLayout.CENTER);
//    }

    private void exportClicked() {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        int columns = tableModel.getColumnCount();
        int rows = tableModel.getRowCount();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                sb.append(tableModel.getValueAt(i, j)).append(System.lineSeparator());
            }
        }
        System.out.println(sb);
        saveToFile(exportFilePath.getText(), sb.toString());
    }

    private void saveToFile(String fileName, String content) {
        System.out.println("Saving to file: " + fileName + System.lineSeparator() + content);
        File file = new File(fileName);
        try (FileWriter writer = new FileWriter(file);) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseClicked() {
        String address = textField.getText();
        String siteText = getWebPage(address);
//        System.out.println(siteText);
//        System.out.println("siteText:\n" + siteText);
        String title = findTitle(siteText);
//        System.out.println("Title: " + title);
        titleValueLabel.setText(title);
        List<String> aTags = getATags(siteText);
        List<String> hrefAttributes = getHrefAttributes(aTags);
        System.out.println("hrefAttributes: " + hrefAttributes);
        List<String> links = fixLinks(address, hrefAttributes);
        System.out.println("links: " + links);
        List<URLRecord> urls = crawlLinks(links);
        URLRecord current = new URLRecord(address, title);
        if (!urls.contains(current)) {
            urls.add(current);
        }
        clearTable();
        fillTable(urls);
    }

    private List<URLRecord> crawlLinks(List<String> links) {
        List<URLRecord> result = new ArrayList<>();
        for (String link : links) {
            Optional<URLRecord> res = visitPage(link);
            if (res.isPresent()) {
                result.add(res.get());
                System.out.println(res.get());
            }
        }
        return result;
    }

    private Optional<URLRecord> visitPage(String link) {
        try {
            URL url = new URL(link);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:63.0) Gecko/20100101 Firefox/63.0");
            String contentType = connection.getContentType();
            System.out.println(link + " content type: " + contentType);
            if (contentType != null && contentType.startsWith("text/html")) {
                InputStream inputStream = connection.getInputStream();
                String siteText = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                return Optional.of(new URLRecord(link, findTitle(siteText)));
            }
        } catch (MalformedURLException e) {
            System.out.println("bad link: " + link);
//            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("unavailable page: " + link);
//            e.printStackTrace();
        }
        return Optional.empty();
    }

    private List<String> fixLinks(String address, List<String> hrefAttributes) {
        String protocol = getProtocol(address);
        String rootAddress = getRootAddress(address);
        System.out.println(protocol);
        System.out.println(rootAddress);
        List<String> result = new ArrayList<>();
        for (String hrefAttribute : hrefAttributes) {
            String fixedLink = "";
            if (!hrefAttribute.contains("/")) {
                fixedLink = rootAddress + "/" + hrefAttribute;
            } else {
                String hrefProtocol = getProtocol(hrefAttribute);
                if (hrefProtocol.isEmpty()) {
                    if (hrefAttribute.startsWith("//")) {
                        fixedLink = protocol + hrefAttribute;
                    } else if (hrefAttribute.equals("/")) {
                        fixedLink = rootAddress;
                    } else {
                        fixedLink = protocol + "//" + hrefAttribute;
                    }
                } else {
                    fixedLink = hrefAttribute;
                }
            }
            System.out.println(hrefAttribute + " -> " + fixedLink);
            result.add(fixedLink);
        }
        return result;
    }

    private String getRootAddress(String address) {
        String protocol = getProtocol(address);
        int lastSlashPosition = address.lastIndexOf("/");
        System.out.println(lastSlashPosition);
        return address.substring(0, (lastSlashPosition > protocol.length() + 2) ? lastSlashPosition : address.length());
    }

    private String getProtocol(String address) {
        return address.substring(0, address.indexOf(":") + 1);
    }

    private List<String> getHrefAttributes(List<String> aTags) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\s*href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))");
        for (String aTag : aTags) {
//            System.out.println(aTag);
            Matcher matcher = pattern.matcher(aTag);
            if (matcher.find()) {
                String url = matcher.group(1);
                result.add(url.substring(1, url.length() - 1));
//                System.out.println(matcher.group(1));
            } else {
//                System.out.println("not found");
            }
        }
        return result;
    }

    private void clearTable() {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        tableModel.setRowCount(0);
    }

    private void fillTable(List<URLRecord> urls) {
        final String baseUrl = textField.getText();
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        for (URLRecord url : urls) {
            if (validUrl(url.url)) {
                tableModel.addRow(new String[]{url.url, url.title});
            }
            System.out.println("url: " + url);
        }
    }

    private boolean validUrl(String url) {
//        URLConnection =
        return true;
    }

    private List<String> getATags(String siteText) {
        List<String> result = new ArrayList<>();

        Pattern pattern = Pattern.compile("<a([^>]+)>(.+?)</a>");
        Matcher matcher = pattern.matcher(siteText);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }

        return result;
    }

    private JTable createTable() {
        DefaultTableModel tableModel = new DefaultTableModel();
        tableModel.setColumnIdentifiers(new String[]{"URL", "Title"});
        JTable table = new JTable(tableModel);
        table.setName("TitlesTable");

        return table;
    }

    private String getWebPage(String url) {
//        final String url = textField.getText();
        System.out.println(url);
        try (InputStream inputStream = new BufferedInputStream(new URL(url).openStream())) {
            String siteText = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
//            textArea.setText(siteText);
//            System.out.println(siteText);
            return siteText;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private String findTitle(String siteText) {
//        <a\s+href=['\"](.+)['\"]>(.+)</a>
        Pattern pattern = Pattern.compile("<title>(.+)</title>");
        Matcher matcher = pattern.matcher(siteText);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }

    public static void main(String[] args) {
        try {
            URL context = new URL("https://www.wikipedia.org/");
//            URL url = new URL(context, "//ast.wikipedia.org/");
            URL url = new URL(context, "//www.wikipedia.ru");
            System.out.println(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
