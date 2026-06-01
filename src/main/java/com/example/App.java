package com.example;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class App {

    public static void main(String[] args) throws Exception {
        run();
    }

    private static void run() throws Exception {
        List<String> lines = Files.readAllLines(Paths.get("data/data.txt"));
        String band = lines.get(0).trim();
        String album = lines.get(1).trim();
        List<String> trackList = new ArrayList<>();
        for (int i = 2; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) trackList.add(line);
        }

        File outputDir = new File("result").getAbsoluteFile();
        outputDir.mkdirs();

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", outputDir.getAbsolutePath());
        prefs.put("download.prompt_for_download", false);
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("download.directory_upgrade", true);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            driver.get("http://www.papercdcase.com");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.name("artist")));

            fillFormData(driver, band, album, trackList);

            ((ChromeDriver) driver).executeCdpCommand("Browser.setDownloadBehavior",
                    Map.of("behavior", "allow", "downloadPath", outputDir.getAbsolutePath()));

            WebElement btn = driver.findElement(By.name("submit"));
            btn.submit();

            File pdfFile = null;
            for (int i = 0; i < 30; i++) {
                Thread.sleep(1000);
                File[] found = outputDir.listFiles((d, name) ->
                        name.endsWith(".pdf") && !name.endsWith(".crdownload"));
                if (found != null && found.length > 0) {
                    pdfFile = found[0];
                    break;
                }
            }

            if (pdfFile != null) {
                File dest = new File(outputDir, "cd.pdf");
                if (!pdfFile.getName().equals("cd.pdf")) {
                    Files.move(pdfFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("Сохранено: " + dest.getAbsolutePath());
            } else {
                System.out.println("PDF не найден");
            }
        } finally {
            driver.quit();
        }
    }

    private static void fillFormData(WebDriver driver, String band,
                                     String album, List<String> trackList) {
        driver.findElement(By.name("artist")).sendKeys(band);
        driver.findElement(By.name("title")).sendKeys(album);

        for (int i = 0; i < Math.min(trackList.size(), 16); i++) {
            driver.findElement(By.name("track" + (i + 1))).sendKeys(trackList.get(i));
        }

        WebElement jewel = driver.findElement(
                By.xpath("//input[@name='template' and @value='jewel']"));
        if (!jewel.isSelected()) jewel.click();

        WebElement a4 = driver.findElement(
                By.xpath("//input[@name='size' and @value='a4']"));
        if (!a4.isSelected()) a4.click();

        WebElement forceSave = driver.findElement(By.name("force_saveas"));
        if (!forceSave.isSelected()) forceSave.click();
    }
}
