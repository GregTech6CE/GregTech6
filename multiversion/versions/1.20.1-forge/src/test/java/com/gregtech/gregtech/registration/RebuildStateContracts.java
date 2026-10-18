package com.gregtech.gregtech.registration;

import com.gregtech.gregtech.api.inventory.BulkStorageState;
import com.gregtech.gregtech.api.multiblock.HollowTankStructure;
import com.gregtech.gregtech.api.sensor.SensorPanelGeometry;
import net.minecraft.core.*;
import java.util.*;

public final class RebuildStateContracts {
    public static void main(String[] args) {
        recipeLayout(); partBindings(); displayOrientation(); bulk(); tanks(); panels(); anvils(); storageFace(); sensorControls(); glyphs(); boiler();
        System.out.println("Bulk storage, hollow tank and sensor panel contracts passed");
    }
    private static void recipeLayout() {
        // Exercise real recipe-map dimensions against the shared GT6 container coordinates.
        var ignored=com.gregtech.gregtech.data.MachineRecipeMaps.Crusher;
        for(var map:com.gregtech.gregtech.api.recipe.RecipeMap.RECIPE_MAP_LIST) {
            var points=new java.util.ArrayList<com.gregtech.gregtech.api.recipe.MachineGuiLayout.Point>();
            for(boolean input:new boolean[]{true,false}) {
                int count=input?map.mInputItemsCount:map.mOutputItemsCount;
                for(int i=0;i<count;i++) points.add(com.gregtech.gregtech.api.recipe.MachineGuiLayout.item(input,i,count,map.mInputFluidCount+map.mOutputFluidCount));
                int fluids=input?map.mInputFluidCount:map.mOutputFluidCount;
                for(int i=0;i<fluids;i++) points.add(com.gregtech.gregtech.api.recipe.MachineGuiLayout.fluid(input,i));
            }
            for(int i=0;i<points.size();i++) {
                var p=points.get(i);
                check(p.x()>=0&&p.x()+16<=176&&p.y()>=0&&p.y()+16<=82,"machine GUI bounds: "+map.mNameInternal);
                for(int j=0;j<i;j++) {
                    var q=points.get(j);
                    check(Math.abs(p.x()-q.x())>=18||Math.abs(p.y()-q.y())>=18,"machine GUI slot overlap: "+map.mNameInternal);
                }
            }
        }
        long unit=com.gregtech.gregtech.api.material.GTValues.U;
        check(com.gregtech.gregtech.content.tool.AnvilRecipeDefinitions.duration(unit,0)==64,"GT6 unit processing cost");
        check(com.gregtech.gregtech.content.tool.AnvilRecipeDefinitions.duration(unit/4,2)==48,"GT6 quality scaling");
    }
    private static void partBindings() {
        var bindings=new com.gregtech.gregtech.api.multiblock.PartBindings<Integer,String>();
        var claimed=new java.util.HashMap<Integer,String>();
        check(bindings.update(java.util.Map.of(1,"water",2,"water"),p->true,claimed::put,claimed::remove),"initial binding");
        check(!bindings.update(java.util.Map.of(3,"steam",4,"steam"),p->p!=4,claimed::put,claimed::remove)&&claimed.isEmpty(),"failed claim is atomic and releases old structure");
        bindings.update(java.util.Map.of(1,"water",2,"water"),p->true,claimed::put,claimed::remove);
        bindings.update(java.util.Map.of(2,"steam",3,"steam"),p->true,claimed::put,claimed::remove);
        check(!claimed.containsKey(1)&&claimed.get(2).equals("steam")&&claimed.size()==2,"rotation releases obsolete parts");
        bindings.clear(claimed::remove);
        check(claimed.isEmpty()&&!bindings.contains(2),"removal clears binding");
        for(var face:Direction.values()) {
            check(com.gregtech.gregtech.content.multiblock.TankFlowRules.canAutoOutput(face,true,1000),"heavy gas outputs in all directions");
            check(com.gregtech.gregtech.content.multiblock.TankFlowRules.canAutoOutput(face,false,1000)==(face!=Direction.UP),"liquid follows gravity");
            check(com.gregtech.gregtech.content.multiblock.TankFlowRules.canAutoOutput(face,false,-1000)==(face!=Direction.DOWN),"negative density liquid floats");
        }
    }
    private static void displayOrientation() {
        for (Direction face : Direction.values()) {
            var rotation = com.gregtech.gregtech.api.block.DisplayFaceOrientation.rotation(face);
            var normal = new org.joml.Vector3f(0,0,1).rotate(rotation);
            check(normal.distance(face.getStepX(),face.getStepY(),face.getStepZ()) < .00001f,
                    "display normal faces outward: " + face);
            var right = new org.joml.Vector3f(.25f,0,0).rotate(rotation);
            var uv = com.gregtech.gregtech.api.block.FaceCoordinates.pixels(face,.5+right.x,.5+right.y,.5+right.z);
            check(Math.abs(uv.x()-12)<.0001 && Math.abs(uv.y()-8)<.0001,
                    "display reads left to right on " + face);
        }
    }
    private static void bulk() {
        var storage = new BulkStorageState<int[]>(100, (a,b) -> a[0] == b[0], a -> a.clone());
        int[] key = {7};
        check(storage.insert(key, 120, true) == 100 && storage.count() == 0 && storage.filter() == null, "simulation must not select filter");
        check(storage.insert(key, 120, false) == 100, "capacity clipping");
        key[0] = 9;
        int[] copy = storage.filter(); copy[0] = 10;
        check(storage.filter()[0] == 7, "filter defensive copies");
        check(storage.extract(-5, false) == 0 && storage.count() == 100, "negative extraction cannot create items");
        check(storage.extract(20, true) == 20 && storage.count() == 100, "simulated extraction");
        check(storage.insert(new int[]{8}, 1, false) == 0, "wrong item rejected");
        check(storage.extract(Long.MAX_VALUE, false) == 100 && storage.filter()[0] == 7, "sticky empty filter");
        storage.setResetWhenEmpty(true);
        check(storage.filter() == null, "reset mode clears empty filter");
        storage.restore(new int[]{8}, 120);
        check(storage.count() == 120 && storage.insert(new int[]{8}, 1, false) == 0, "legacy overflow retained without additional insertion");
        storage.extract(30, false);
        check(storage.insert(new int[]{8}, Long.MAX_VALUE, false) == 10, "overflow recovery");
        storage.restore(null, 100);
        check(storage.count() == 0, "no phantom contents without identity");
        storage.restore(new int[]{8}, -1);
        check(storage.count() == 0, "corrupt negative count clamped");
    }
    private static void tanks() {
        BlockPos controller = new BlockPos(11, 50, -7);
        for (int size : new int[]{3,5}) for (Direction facing : Direction.values()) {
            int r = size / 2;
            BlockPos center = controller.relative(facing.getOpposite(), r);
            Set<BlockPos> wall = new HashSet<>(), air = new HashSet<>();
            for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
                BlockPos pos = center.offset(x,y,z);
                if (Math.abs(x) == r || Math.abs(y) == r || Math.abs(z) == r) wall.add(pos); else air.add(pos);
            }
            check(wall.remove(controller), "controller is front shell center");
            check(wall.size() == (size == 3 ? 25 : 97), "all other shell cells required");
            check(HollowTankStructure.validate(controller,facing,size,p -> true,wall::contains,air::contains), "valid tank " + size + facing);
            for (BlockPos pos : List.copyOf(wall)) {
                wall.remove(pos);
                check(!HollowTankStructure.validate(controller,facing,size,p -> true,wall::contains,air::contains), "every broken wall invalidates");
                wall.add(pos);
            }
            air.remove(center);
            check(!HollowTankStructure.validate(controller,facing,size,p -> true,wall::contains,air::contains), "obstructed interior rejected");
            air.add(center);
            check(!HollowTankStructure.validate(controller,facing,size,p -> !p.equals(center),wall::contains,air::contains), "unloaded tank rejected");
        }
    }
    private static void panels() {
        for (Direction d : Direction.values()) {
            int[] b = SensorPanelGeometry.bounds(d);
            check((b[3]-b[0])*(b[4]-b[1])*(b[5]-b[2]) == 512, "two pixel panel volume");
            int axis = d.getAxis() == Direction.Axis.X ? 0 : d.getAxis() == Direction.Axis.Y ? 1 : 2;
            check(d.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? b[axis] == 14 : b[axis+3] == 2, "panel touches target-facing boundary");
        }
    }
    private static void anvils() {
        check(com.gregtech.gregtech.content.tool.AnvilRules.wear(32, 100, 0, 0) == 10000, "anvil minimum wear");
        check(com.gregtech.gregtech.content.tool.AnvilRules.wear(-40001, 4, 0, 0) == 40001, "absolute recipe energy");
        check(com.gregtech.gregtech.content.tool.AnvilRules.wear(1, 1, 1, 0) == 20000, "fatigue doubles wear");
        check(com.gregtech.gregtech.content.tool.AnvilRules.wear(1, 1, 0, 2) == 3334, "haste rounds wear up");
        check(com.gregtech.gregtech.content.tool.AnvilRules.wear(Long.MIN_VALUE, Long.MAX_VALUE, 2, 0) == Long.MAX_VALUE, "energy overflow saturates");
        for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            check(com.gregtech.gregtech.content.tool.AnvilRules.surface(facing, Direction.UP, .25) == com.gregtech.gregtech.content.tool.AnvilRules.Surface.TOP, "top recipe map");
            check(com.gregtech.gregtech.content.tool.AnvilRules.surface(facing, facing, .25) != com.gregtech.gregtech.content.tool.AnvilRules.surface(facing, facing, .75), "two bending halves");
        }
    }
    private static void storageFace() {
        int[] expected = {8,64,4,32,1,16};
        int i = 0;
        for (var button : com.gregtech.gregtech.api.inventory.MassStorageFace.BUTTONS) {
            check(button.amount() == expected[i++], "GT6 six button ordering");
            for (Direction facing : new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
                double u = (button.left()+1)/16.0;
                double x = facing == Direction.NORTH ? 1-u : u;
                double z = facing == Direction.EAST ? 1-u : u;
                double actual = com.gregtech.gregtech.api.inventory.MassStorageFace.x(facing,x,z);
                check(com.gregtech.gregtech.api.inventory.MassStorageFace.withdrawal(actual,button.top()+1) == button.amount(), "all four fronts match rendered buttons");
            }
        }
        check(com.gregtech.gregtech.api.inventory.MassStorageFace.withdrawal(8,10) == 0, "icon does not withdraw");
        check(com.gregtech.gregtech.api.inventory.MassStorageFace.withdrawal(2,8.5) == 0, "button row gap does not withdraw");
        check(com.gregtech.gregtech.api.inventory.MassStorageFace.withdrawal(8,2) == 0, "count display does not withdraw");
        check(!com.gregtech.gregtech.api.inventory.MassStorageFace.active(.9,7), "outer border excluded");
        var storage = new BulkStorageState<String>(1000,String::equals,s -> s);
        storage.insert("item",1000,false);
        for (int count : expected) {
            long before = storage.count();
            check(storage.extract(count,false) == count && storage.count() == before-count, "button withdrawal conserves exact count");
        }
        storage.restore("item",3);
        check(storage.extract(64,false) == 3 && storage.count() == 0,"insufficient stock returns remaining amount");
    }
    private static void sensorControls() {
        var modes = com.gregtech.gregtech.api.sensor.SensorControl.Mode.values();
        int[] signal = {0,7,15,0,0,15,0,15};
        for (int i = 0; i < modes.length; i++)
            check(com.gregtech.gregtech.api.sensor.SensorControl.evaluate(modes[i],50,100,40).signal() == signal[i],"sensor mode " + modes[i]);
        check(com.gregtech.gregtech.api.sensor.SensorControl.evaluate(modes[1],1,0,0).signal() == 0,"zero maximum safe");
        check(com.gregtech.gregtech.api.sensor.SensorControl.evaluate(modes[1],Long.MAX_VALUE,Long.MAX_VALUE,0).signal() == 15,"percentage multiplication cannot overflow");
        check(com.gregtech.gregtech.api.sensor.SensorControl.thresholdChange(10,7,false) == -100,"decimal decrement");
        check(com.gregtech.gregtech.api.sensor.SensorControl.thresholdChange(13,10,true) == 16,"hex increment");
        check(com.gregtech.gregtech.api.sensor.SensorControl.thresholdChange(11.5,10,true) == 0,"threshold button gap");
        var average = new com.gregtech.gregtech.api.sensor.SensorAverage();
        average.resize(2);
        check(average.sample(100) == 50 && average.sample(200) == 150,"original zero-filled average");
        var restored = new com.gregtech.gregtech.api.sensor.SensorAverage();
        restored.restore(average.values(),average.index());
        check(restored.sample(300) == average.sample(300),"averaging survives reload");
        check(com.gregtech.gregtech.api.sensor.SensorMeasurements.timeOfDayMinutes(0) == 360,"clock starts at 6AM");
        check(com.gregtech.gregtech.api.sensor.SensorMeasurements.timeOfDayMinutes(18000) == 0,"clock midnight wrap");
    }
    private static void glyphs() {
        for (var cell : com.gregtech.gregtech.api.sensor.SixCellDisplay.storage(0,1000000))
            check(cell != null && cell.texture().equals("0"), "empty storage shows 000000");
        var cells=com.gregtech.gregtech.api.sensor.SixCellDisplay.storage(42,1000000);
        check(cells.length==6&&cells[0].texture().equals("0")&&cells[3].texture().equals("0")&&cells[4].texture().equals("4")&&cells[5].texture().equals("2"),"six-cell storage leading zeroes");
        cells=com.gregtech.gregtech.api.sensor.SixCellDisplay.storage(1000000,1000000);
        check(cells[0]==null&&cells[1].texture().equals("1")&&cells[4].texture().equals("percent")&&cells[5]==null&&cells[1].rgb()==0xFF0000,"full storage red percent display");
        cells=com.gregtech.gregtech.api.sensor.SixCellDisplay.sensor(0xabcd,com.gregtech.gregtech.api.sensor.SensorControl.Mode.DISPLAY,true,"ru",0x00FF00);
        check(cells[0].texture().equals("hex")&&cells[1].texture().equals("0xa")&&cells[4].texture().equals("0xd")&&cells[5].texture().equals("ru"),"original hexadecimal sprites and unit cell");
        check(com.gregtech.gregtech.api.sensor.SixCellDisplay.TILE_UV==.125f,"sample one tile from GT6's repeated glyph sheet");
    }
    private static void boiler() {
        var layout=com.gregtech.gregtech.content.multiblock.BoilerStructure.LAYOUT;
        var counts=new java.util.EnumMap<com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role,Integer>(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.class);
        for(var cell:layout.cells()) counts.merge(cell.role(),1,Integer::sum);
        check(layout.cells().size()==36&&counts.get(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.HEAT_INPUT)==9&&counts.get(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.FLUID_INPUT)==9&&counts.get(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.FLUID_OUTPUT)==17&&counts.get(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.AIR)==1,"GT6 boiler layer roles including controller");
        var origin=new BlockPos(10,70,-50);
        for(var face:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
            var world=new java.util.HashMap<BlockPos,com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role>();
            for(var cell:layout.cells()) world.put(cell.at(origin,face),cell.role());
            check(layout.matches(origin,face,p->true,(p,r)->world.get(p)==r),"rotated valid boiler");
            for(var cell:layout.cells()) {
                var pos=cell.at(origin,face); var role=world.remove(pos);
                check(!layout.matches(origin,face,p->true,(p,r)->world.get(p)==r),"each missing/wrong part or obstructed cavity invalidates");
                world.put(pos,role);
                check(!layout.matches(origin,face,p->!p.equals(pos),(p,r)->world.get(p)==r),"no implicit chunk loading");
            }
        }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
