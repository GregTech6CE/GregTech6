package com.gregtech.gregtech.api.inventory;
/** Original exact hopper/battery-box/queue inventory coordinates, shared by both native menus. */
public final class InventorySlotLayout {
 private InventorySlotLayout(){}
 public record Position(int x,int y){}
 public record Layout(java.util.List<Position> positions,int playerInventoryY){}
 public static int[] supportedSizes(){return new int[]{1,2,3,4,5,6,7,8,9,12,14,15,16,18,27,36,54};}
    public static Layout layout(int slotCount) {
        java.util.List<Position> positions=new java.util.ArrayList<>();
        switch (slotCount) {
            case 1:
                positions.add(new Position(80,35));
                return new Layout(java.util.List.copyOf(positions),84);
            case 2:
                positions.add(new Position(71,35));
                positions.add(new Position(89,35));
                return new Layout(java.util.List.copyOf(positions),84);
            case 3:
                positions.add(new Position(62,35));
                positions.add(new Position(80,35));
                positions.add(new Position(98,35));
                return new Layout(java.util.List.copyOf(positions),84);
            case 4:
                positions.add(new Position(71,26));
                positions.add(new Position(89,26));
                positions.add(new Position(71,44));
                positions.add(new Position(89,44));
                return new Layout(java.util.List.copyOf(positions),84);
            case 5:
                positions.add(new Position(44,35));
                positions.add(new Position(62,35));
                positions.add(new Position(80,35));
                positions.add(new Position(98,35));
                positions.add(new Position(116,35));
                return new Layout(java.util.List.copyOf(positions),84);
            case 6:
                positions.add(new Position(62,26));
                positions.add(new Position(80,26));
                positions.add(new Position(98,26));
                positions.add(new Position(62,44));
                positions.add(new Position(80,44));
                positions.add(new Position(98,44));
                return new Layout(java.util.List.copyOf(positions),84);
            case 7:
                positions.add(new Position(26,35));
                positions.add(new Position(44,35));
                positions.add(new Position(62,35));
                positions.add(new Position(80,35));
                positions.add(new Position(98,35));
                positions.add(new Position(116,35));
                positions.add(new Position(134,35));
                return new Layout(java.util.List.copyOf(positions),84);
            case 8:
                positions.add(new Position(53,26));
                positions.add(new Position(71,26));
                positions.add(new Position(89,26));
                positions.add(new Position(107,26));
                positions.add(new Position(53,44));
                positions.add(new Position(71,44));
                positions.add(new Position(89,44));
                positions.add(new Position(107,44));
                return new Layout(java.util.List.copyOf(positions),84);
            case 9:
                positions.add(new Position(62,17));
                positions.add(new Position(80,17));
                positions.add(new Position(98,17));
                positions.add(new Position(62,35));
                positions.add(new Position(80,35));
                positions.add(new Position(98,35));
                positions.add(new Position(62,53));
                positions.add(new Position(80,53));
                positions.add(new Position(98,53));
                return new Layout(java.util.List.copyOf(positions),84);
            case 12:
                positions.add(new Position(35,26));
                positions.add(new Position(53,26));
                positions.add(new Position(71,26));
                positions.add(new Position(89,26));
                positions.add(new Position(107,26));
                positions.add(new Position(125,26));
                positions.add(new Position(35,44));
                positions.add(new Position(53,44));
                positions.add(new Position(71,44));
                positions.add(new Position(89,44));
                positions.add(new Position(107,44));
                positions.add(new Position(125,44));
                return new Layout(java.util.List.copyOf(positions),84);
            case 14:
                positions.add(new Position(26,26));
                positions.add(new Position(44,26));
                positions.add(new Position(62,26));
                positions.add(new Position(80,26));
                positions.add(new Position(98,26));
                positions.add(new Position(116,26));
                positions.add(new Position(134,26));
                positions.add(new Position(26,44));
                positions.add(new Position(44,44));
                positions.add(new Position(62,44));
                positions.add(new Position(80,44));
                positions.add(new Position(98,44));
                positions.add(new Position(116,44));
                positions.add(new Position(134,44));
                return new Layout(java.util.List.copyOf(positions),84);
            case 15:
                positions.add(new Position(44,17));
                positions.add(new Position(62,17));
                positions.add(new Position(80,17));
                positions.add(new Position(98,17));
                positions.add(new Position(116,17));
                positions.add(new Position(44,35));
                positions.add(new Position(62,35));
                positions.add(new Position(80,35));
                positions.add(new Position(98,35));
                positions.add(new Position(116,35));
                positions.add(new Position(44,53));
                positions.add(new Position(62,53));
                positions.add(new Position(80,53));
                positions.add(new Position(98,53));
                positions.add(new Position(116,53));
                return new Layout(java.util.List.copyOf(positions),96);
            case 16:
                positions.add(new Position(53,8));
                positions.add(new Position(71,8));
                positions.add(new Position(89,8));
                positions.add(new Position(107,8));
                positions.add(new Position(53,26));
                positions.add(new Position(71,26));
                positions.add(new Position(89,26));
                positions.add(new Position(107,26));
                positions.add(new Position(53,44));
                positions.add(new Position(71,44));
                positions.add(new Position(89,44));
                positions.add(new Position(107,44));
                positions.add(new Position(53,62));
                positions.add(new Position(71,62));
                positions.add(new Position(89,62));
                positions.add(new Position(107,62));
                return new Layout(java.util.List.copyOf(positions),84);
            case 18:
                positions.add(new Position(8,26));
                positions.add(new Position(26,26));
                positions.add(new Position(44,26));
                positions.add(new Position(62,26));
                positions.add(new Position(80,26));
                positions.add(new Position(98,26));
                positions.add(new Position(116,26));
                positions.add(new Position(134,26));
                positions.add(new Position(152,26));
                positions.add(new Position(8,44));
                positions.add(new Position(26,44));
                positions.add(new Position(44,44));
                positions.add(new Position(62,44));
                positions.add(new Position(80,44));
                positions.add(new Position(98,44));
                positions.add(new Position(116,44));
                positions.add(new Position(134,44));
                positions.add(new Position(152,44));
                return new Layout(java.util.List.copyOf(positions),84);
            case 27:
                positions.add(new Position(8,17));
                positions.add(new Position(26,17));
                positions.add(new Position(44,17));
                positions.add(new Position(62,17));
                positions.add(new Position(80,17));
                positions.add(new Position(98,17));
                positions.add(new Position(116,17));
                positions.add(new Position(134,17));
                positions.add(new Position(152,17));
                positions.add(new Position(8,35));
                positions.add(new Position(26,35));
                positions.add(new Position(44,35));
                positions.add(new Position(62,35));
                positions.add(new Position(80,35));
                positions.add(new Position(98,35));
                positions.add(new Position(116,35));
                positions.add(new Position(134,35));
                positions.add(new Position(152,35));
                positions.add(new Position(8,53));
                positions.add(new Position(26,53));
                positions.add(new Position(44,53));
                positions.add(new Position(62,53));
                positions.add(new Position(80,53));
                positions.add(new Position(98,53));
                positions.add(new Position(116,53));
                positions.add(new Position(134,53));
                positions.add(new Position(152,53));
                return new Layout(java.util.List.copyOf(positions),84);
            case 36:
                positions.add(new Position(8,8));
                positions.add(new Position(26,8));
                positions.add(new Position(44,8));
                positions.add(new Position(62,8));
                positions.add(new Position(80,8));
                positions.add(new Position(98,8));
                positions.add(new Position(116,8));
                positions.add(new Position(134,8));
                positions.add(new Position(152,8));
                positions.add(new Position(8,26));
                positions.add(new Position(26,26));
                positions.add(new Position(44,26));
                positions.add(new Position(62,26));
                positions.add(new Position(80,26));
                positions.add(new Position(98,26));
                positions.add(new Position(116,26));
                positions.add(new Position(134,26));
                positions.add(new Position(152,26));
                positions.add(new Position(8,44));
                positions.add(new Position(26,44));
                positions.add(new Position(44,44));
                positions.add(new Position(62,44));
                positions.add(new Position(80,44));
                positions.add(new Position(98,44));
                positions.add(new Position(116,44));
                positions.add(new Position(134,44));
                positions.add(new Position(152,44));
                positions.add(new Position(8,62));
                positions.add(new Position(26,62));
                positions.add(new Position(44,62));
                positions.add(new Position(62,62));
                positions.add(new Position(80,62));
                positions.add(new Position(98,62));
                positions.add(new Position(116,62));
                positions.add(new Position(134,62));
                positions.add(new Position(152,62));
                return new Layout(java.util.List.copyOf(positions),84);
            case 54:
                // Original chests/54.png: first row at 18, player inventory at 140.
                for (int row = 0; row < 6; row++)
                    for (int col = 0; col < 9; col++)
                        positions.add(new Position(8 + col * 18,18 + row * 18));
                return new Layout(java.util.List.copyOf(positions),140);
            default:
                // Fallback: auto-layout in rows of 9
                int cols = Math.min(slotCount, 9);
                for (int idx = 0; idx < slotCount; idx++) {
                    int row = idx / cols;
                    int col = idx % cols;
                    int x = 8 + col * 18;
                    int y = 17 + row * 18;
                    positions.add(new Position(x,y));
                }
                int rows = (slotCount + cols - 1) / cols;
                return new Layout(java.util.List.copyOf(positions),17 + rows * 18 + 14);
        }
    }

}
