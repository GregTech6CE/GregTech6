package gregtech.tools.worldgen;

/** The persisted ore counts of one material in one chunk. */
record OreSampleRow(int x, int z, String material, long normal, long poor, long bedrock, long rawPebbles) {
    static final String HEADER = "chunk_x,chunk_z,material,normal,poor,bedrock,raw_pebbles";

    OreSampleRow {
        if (!material.matches("[A-Za-z0-9_.-]+") || normal < 0 || poor < 0 || bedrock < 0 || rawPebbles < 0)
            throw new IllegalArgumentException("Invalid sample for " + material + " in chunk " + x + "," + z);
    }

    String encode() {
        return x + "," + z + "," + material + "," + normal + "," + poor + "," + bedrock + "," + rawPebbles;
    }

    static OreSampleRow decode(String row) {
        var c = row.split(",", -1);
        if (c.length != 7) throw new IllegalArgumentException("Invalid ore sample: " + row);
        return new OreSampleRow(
                Integer.parseInt(c[0]),
                Integer.parseInt(c[1]),
                c[2],
                Long.parseLong(c[3]),
                Long.parseLong(c[4]),
                Long.parseLong(c[5]),
                Long.parseLong(c[6]));
    }
}
