package gregtech.tools.worldgen;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.LongBinaryTag;

/** Read-only Anvil input for both supported Minecraft versions, including oversized external chunks. */
final class AnvilChunks {
    private static final int SECTOR = 4096;
    private static final BinaryTagIO.Reader NBT = BinaryTagIO.reader(64L << 20);

    private AnvilChunks() {}

    static long seed(Path save) throws IOException {
        var data = NBT.read(save.resolve("level.dat"), BinaryTagIO.Compression.GZIP)
                .getCompound("Data")
                .getCompound("WorldGenSettings");
        if (!(data.get("seed") instanceof LongBinaryTag)) throw new IOException("Missing WorldGenSettings.seed");
        return data.getLong("seed");
    }

    static Path region(Path save, String dimension) {
        if (!dimension.matches("[a-z0-9_.-]+:[a-z0-9._/-]+")
                || dimension.contains("..")
                || dimension.contains("//")
                || dimension.contains(":/")
                || dimension.endsWith("/")) throw new IllegalArgumentException("Invalid dimension " + dimension);
        return switch (dimension) {
            case "minecraft:overworld" -> save.resolve("region");
            case "minecraft:the_nether" -> save.resolve("DIM-1/region");
            case "minecraft:the_end" -> save.resolve("DIM1/region");
            default ->
                save.resolve("dimensions").resolve(dimension.replace(':', '/')).resolve("region");
        };
    }

    static CompoundBinaryTag read(Path directory, int x, int z) throws IOException {
        var region = directory.resolve("r." + (x >> 5) + "." + (z >> 5) + ".mca");
        if (Files.notExists(region)) return null;
        try (var input = new RandomAccessFile(region.toFile(), "r")) {
            if (input.length() < SECTOR * 2) throw new IOException("Incomplete region header");
            input.seek(4L * ((x & 31) + (z & 31) * 32));
            int location = input.readInt();
            if (location == 0) return null;
            int offset = location >>> 8;
            int sectors = location & 255;
            long start = (long) offset * SECTOR;
            if (offset < 2
                    || sectors == 0
                    || start + 5 > input.length()
                    || (long) offset + sectors > (input.length() + SECTOR - 1) / SECTOR)
                throw new IOException("Invalid sector location");
            input.seek(start);
            int length = input.readInt();
            int type = input.readUnsignedByte();
            if (length < 1 || length + 4L > (long) sectors * SECTOR || start + 4L + length > input.length())
                throw new IOException("Invalid compressed chunk length");
            InputStream bytes;
            if ((type & 128) != 0) {
                if (length != 1) throw new IOException("Invalid external chunk reference");
                bytes = Files.newInputStream(directory.resolve("c." + x + "." + z + ".mcc"));
            } else {
                var payload = new byte[length - 1];
                input.readFully(payload);
                bytes = new ByteArrayInputStream(payload);
            }
            try (bytes;
                    var unpacked = decompress(bytes, type & 127);
                    var stream = new DataInputStream(new BufferedInputStream(unpacked))) {
                var tag = NBT.read((DataInput) stream);
                if (tag.getInt("xPos", Integer.MIN_VALUE) != x || tag.getInt("zPos", Integer.MIN_VALUE) != z)
                    throw new IOException("Stored chunk coordinates do not match region entry");
                return tag;
            }
        }
    }

    private static InputStream decompress(InputStream in, int type) throws IOException {
        return switch (type) {
            case 1 -> new GZIPInputStream(in);
            case 2 -> new InflaterInputStream(in);
            case 3 -> in;
            case 4 -> new LZ4BlockInputStream(in);
            default -> throw new IOException("Unsupported chunk compression " + type);
        };
    }
}
