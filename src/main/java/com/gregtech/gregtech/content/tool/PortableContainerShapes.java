package com.gregtech.gregtech.content.tool;
public final class PortableContainerShapes {
 private PortableContainerShapes() {}
 public static int[][] bounds(String id) { return switch (id) {
 case "cell" -> new int[][] {{5,1,6,11,11,10},{6,1,5,10,11,11},{6,0,6,10,12,10}};
 case "jug" -> new int[][] {{5,10,6,6,14,10}, {6,10,5,10,14,6}, {10,10,6,11,14,10}, {6,10,10,10,14,11}, {3,0,3,13,10,13}};
 case "cup" -> new int[][] {{5,1,6,6,5,10}, {6,1,5,10,5,6}, {10,1,6,11,5,10}, {6,1,10,10,5,11}, {6,0,6,10,1,10}};
 case "measuring_pot" -> new int[][] {{4,1,5,5,8,11}, {5,1,4,11,8,5}, {11,1,5,12,8,11}, {5,1,11,11,8,12}, {5,0,5,11,1,11}};
 case "thermos" -> new int[][] {{4,0,4,12,16,12}};
 case "barometer_gas_cylinder" -> new int[][] {{4,0,5,12,8,11}, {5,0,4,11,8,12}, {5,8,5,11,9,11}, {7,9,7,9,16,9}, {6,10,6,10,14,10}};
 default -> throw new IllegalArgumentException(id);
 }; }
}
