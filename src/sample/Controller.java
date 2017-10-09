package sample;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

public class Controller {
    @FXML
    public Button loadFileButton;
    @FXML
    public Label fileNameLabel;
    @FXML
    public ImageView imageViewContainer;

    private File selectedFile = null;

    @FXML
    public void loadFile(ActionEvent event) throws IOException {
        Node node = (Node) event.getSource();
        Scene scene = node.getScene();
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.ppm", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(scene.getWindow());
        fileNameLabel.setText(file.getName());
        selectedFile = file;
        if (file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg"))
            imageViewContainer.setImage(new Image(new FileInputStream(file)));
        if (file.getName().endsWith(".ppm"))
            displayPPM(file);
    }

    @FXML
    public void saveJPG() throws IOException {
        File compressedImageFile = new File("compressed_" + selectedFile.getName());
        InputStream inputStream = new FileInputStream(selectedFile);
        OutputStream outputStream = new FileOutputStream(compressedImageFile);

        float quality = 0.1f;
        BufferedImage image = ImageIO.read(inputStream);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
        writer.setOutput(imageOutputStream);
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        writer.write(null, new IIOImage(image, null, null), param);

        inputStream.close();
        outputStream.close();
    }

    public void displayPPM(File file) throws IOException {
        switch (getPpmFileType(file)) {
            case P3:
                displayP3(file);
                break;
            case P6:
                displayP6(file);
                break;
            default:
                break;
        }
    }


    private void displayP3(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        bufferedReader.readLine();
        String line = null;
        int width = 0, height = 0, currentWidth = 0, currentHeight = 0;
        BufferedImage img = null;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("#")) {
                line = bufferedReader.readLine();
                String[] dimensions = line.split(" ");
                width = Integer.valueOf(dimensions[0]);
                height = Integer.valueOf(dimensions[1]);
                img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                bufferedReader.readLine();
            }

            if (line.split(" ").length >= 3) {
                String currentLine = bufferedReader.readLine();
                currentLine = currentLine.replaceAll("\\s+"," ");
                if(currentLine.startsWith(" "))
                    currentLine = currentLine.substring(1);
                String[] pixels = currentLine.split(" ");
                for (int index = 0; index < pixels.length; index += 3) {
                    int r = Integer.valueOf(pixels[index]);
                    int g = Integer.valueOf(pixels[index + 1]);
                    int b = Integer.valueOf(pixels[index + 2]);
                    int rgb = (r << 16) | (g << 8) | b;
                    img.setRGB(currentWidth, currentHeight, rgb);
                    currentWidth++;
                    currentWidth %= width;
                    if (currentWidth == 0)
                        currentHeight++;
                }
            }
        }

        BufferedImage imageRGB = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.OPAQUE);
        Graphics2D graphics = imageRGB.createGraphics();

        graphics.drawImage(img, 0, 0, null);
        graphics.dispose();
        imageViewContainer.setImage(SwingFXUtils.toFXImage(imageRGB, null));
    }

    private void displayP6(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        bufferedReader.readLine();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("#"))
                continue;
        }
    }

    public PpmFileType getPpmFileType(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String type = bufferedReader.readLine();
        bufferedReader.close();
        if (type.equals("P3"))
            return PpmFileType.P3;
        if (type.equals("P6"))
            return PpmFileType.P6;
        return PpmFileType.UNRECOGNIZED;
    }
}
