package com.platetectonics.geosim.io;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Streams frames into a looping animated GIF, so long timelines never sit in memory at once. */
public final class GifWriter implements Closeable {
    private final ImageWriter writer;
    private final ImageOutputStream out;
    private final int delayCentiseconds;
    private boolean started;

    public GifWriter(Path file, int delayMs) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        this.writer = ImageIO.getImageWritersBySuffix("gif").next();
        this.out = ImageIO.createImageOutputStream(file.toFile());
        this.delayCentiseconds = Math.max(1, delayMs / 10);
        writer.setOutput(out);
    }

    public void write(BufferedImage frame) throws IOException {
        write(frame, delayCentiseconds);
    }

    public void write(BufferedImage frame, int delayCs) throws IOException {
        if (!started) {
            writer.prepareWriteSequence(null);
            started = true;
        }
        ImageWriteParam param = writer.getDefaultWriteParam();
        ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(frame);
        IIOMetadata metadata = writer.getDefaultImageMetadata(type, param);
        String format = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);

        IIOMetadataNode gce = child(root, "GraphicControlExtension");
        gce.setAttribute("disposalMethod", "none");
        gce.setAttribute("userInputFlag", "FALSE");
        gce.setAttribute("transparentColorFlag", "FALSE");
        gce.setAttribute("delayTime", Integer.toString(delayCs));
        gce.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode apps = child(root, "ApplicationExtensions");
        IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
        app.setAttribute("applicationID", "NETSCAPE");
        app.setAttribute("authenticationCode", "2.0");
        app.setUserObject(new byte[]{0x1, 0x0, 0x0}); // loop forever
        apps.appendChild(app);

        metadata.setFromTree(format, root);
        writer.writeToSequence(new IIOImage(frame, null, metadata), param);
    }

    private static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int k = 0; k < root.getLength(); k++) {
            if (root.item(k).getNodeName().equalsIgnoreCase(name)) {
                return (IIOMetadataNode) root.item(k);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }

    @Override
    public void close() throws IOException {
        if (started) {
            writer.endWriteSequence();
        }
        out.close();
        writer.dispose();
    }
}
