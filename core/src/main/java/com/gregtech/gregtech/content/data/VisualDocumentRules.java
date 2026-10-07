/* GregTech-6 Team (2024) scanner/printer rules, LGPL-3.0-or-later. */
package com.gregtech.gregtech.content.data;

/** Original book/map work and paper costs; native content remains at each platform boundary. */
public final class VisualDocumentRules {
    private VisualDocumentRules() {}
    public static final String MAP_ID = "map_id";
    public static final int POWER = 16, BOOK_SCAN_TICKS = 512, MAP_TICKS = 64, MAP_DYE_MB = 16;
    public record BookPrint(String path, int sheets, int blackDye, int ticks) {}
    public static BookPrint bookPrint(int pages) {
        return pages > 50 ? new BookPrint("many_printed_pages", 6, 144, 1024)
                          : new BookPrint("printed_pages", 3, 72, 512);
    }
}
