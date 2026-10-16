package com.platetectonics.geosim.world;

import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.history.WorldHistory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The MasterWorld file ({@code master.ptw}): gzip of a magic number, format version, simulation version,
 * settings and grid, then each field as name, type, length and values (big-endian). Fields are matched by
 * name, so a reader notices a missing or renamed field instead of mixing them up.
 */
public final class MasterWorldIO {
    private static final int MAGIC = 0x50544D57; // "PTMW"
    public static final int FORMAT = 1;
    private static final byte FLOAT = 1;
    private static final byte BYTE = 2;
    private static final byte INT = 3;

    private MasterWorldIO() {
    }

    /** Writes to a temporary file first, so a crash never leaves half a world behind. */
    public static void write(MasterWorld m, Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path tmp = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
                new GZIPOutputStream(Files.newOutputStream(tmp), 1 << 16), 1 << 16))) {
            out.writeInt(MAGIC);
            out.writeInt(FORMAT);
            out.writeUTF(m.simVersion);
            MasterWorld.Settings s = m.settings;
            out.writeLong(s.seed());
            out.writeInt(s.width());
            out.writeDouble(s.ageMyr());
            out.writeUTF(s.preset().name());
            out.writeDouble(s.meanOceanDepthM());
            out.writeInt(m.grid.width());
            out.writeInt(m.grid.height());
            out.writeDouble(m.grid.cellKm());
            var fields = m.fields();
            out.writeInt(fields.size());
            for (Object[] f : fields) {
                out.writeUTF((String) f[0]);
                switch (f[1]) {
                    case float[] a -> {
                        out.writeByte(FLOAT);
                        out.writeInt(a.length);
                        for (float v : a) {
                            out.writeFloat(v);
                        }
                    }
                    case byte[] a -> {
                        out.writeByte(BYTE);
                        out.writeInt(a.length);
                        out.write(a);
                    }
                    case int[] a -> {
                        out.writeByte(INT);
                        out.writeInt(a.length);
                        for (int v : a) {
                            out.writeInt(v);
                        }
                    }
                    default -> throw new IllegalStateException("field type of " + f[0]);
                }
            }
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static MasterWorld read(Path file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(Files.newInputStream(file), 1 << 16), 1 << 16))) {
            if (in.readInt() != MAGIC) {
                throw new IOException(file + " is not a PlateTectonics master world");
            }
            int format = in.readInt();
            if (format != FORMAT) {
                throw new IOException(file + ": format " + format + ", this build reads " + FORMAT);
            }
            String simVersion = in.readUTF();
            MasterWorld.Settings s = new MasterWorld.Settings(in.readLong(), in.readInt(), in.readDouble(),
                    WorldHistory.Preset.valueOf(in.readUTF()), in.readDouble());
            CylinderGrid grid = new CylinderGrid(in.readInt(), in.readInt(), in.readDouble());
            MasterWorld m = new MasterWorld(s, simVersion, grid);
            int count = in.readInt();
            var fields = m.fields();
            if (count != fields.size()) {
                throw new IOException(file + ": " + count + " fields, expected " + fields.size());
            }
            for (Object[] f : fields) {
                String name = in.readUTF();
                if (!name.equals(f[0])) {
                    throw new IOException(file + ": field " + name + " where " + f[0] + " was expected");
                }
                byte type = in.readByte();
                int length = in.readInt();
                if (length != grid.size()) {
                    throw new IOException(file + ": field " + name + " has " + length + " values");
                }
                switch (f[1]) {
                    case float[] a when type == FLOAT -> {
                        for (int k = 0; k < a.length; k++) {
                            a[k] = in.readFloat();
                        }
                    }
                    case byte[] a when type == BYTE -> in.readFully(a);
                    case int[] a when type == INT -> {
                        for (int k = 0; k < a.length; k++) {
                            a[k] = in.readInt();
                        }
                    }
                    default -> throw new IOException(file + ": field " + name + " has type " + type);
                }
            }
            return m;
        }
    }
}
