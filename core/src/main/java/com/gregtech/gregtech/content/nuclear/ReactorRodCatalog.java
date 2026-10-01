package com.gregtech.gregtech.content.nuclear;
import java.util.*;
/** Extracted from GT6 Loader_MultiTileEntities reactor registrations 9201–9441. */
public final class ReactorRodCatalog {
    public enum Kind {BASE, NUCLEAR, DEPLETED, ABSORBER, REFLECTOR, MODERATOR, BREEDER, PRODUCT}
    public record Rod(int originalId,String id,String name,String material,Kind kind,long life,int self,int emission,int divisor,int maximum,int product,int loss) {}
    public static final List<Rod> ALL=List.of(
        new Rod(9201,"reactor_rod_9201","Empty Reactor Rod","Zirconium",Kind.BASE,0L,0,0,1,1,0,0),
        new Rod(9202,"reactor_rod_9202","Neutron Absorber Rod","CdInAgAlloy",Kind.ABSORBER,0L,0,0,1,1,0,0),
        new Rod(9203,"reactor_rod_9203","Neutron Reflector Rod","Beryllium",Kind.REFLECTOR,0L,0,0,1,1,0,0),
        new Rod(9204,"reactor_rod_9204","Neutron Moderator Rod","Graphite",Kind.MODERATOR,0L,0,0,1,1,0,0),
        new Rod(9210,"fuel_rod_th_232","Thorium-232 Fuel Rod","Thorium",Kind.NUCLEAR,12000000000L,2,2,32,128,9310,0),
        new Rod(9219,"reactor_rod_9219","Cyanite Fuel Rod","Cyanite",Kind.NUCLEAR,12000000000L,2,2,32,64,9319,0),
        new Rod(9220,"fuel_rod_u_238","Uranium-238 Fuel Rod","Uranium",Kind.NUCLEAR,6000000000L,4,4,16,512,9320,0),
        new Rod(9221,"fuel_rod_u_235","Uranium-235 Fuel Rod","Uranium235",Kind.NUCLEAR,1200000000L,32,32,4,2048,9321,0),
        new Rod(9222,"reactor_rod_9222","Uranium-233 Fuel Rod","Uranium233",Kind.NUCLEAR,6000000000L,32,32,4,2048,9322,0),
        new Rod(9229,"reactor_rod_9229","Yellorium Fuel Rod","Yellorium",Kind.NUCLEAR,6000000000L,4,4,16,256,9329,0),
        new Rod(9230,"reactor_rod_9230","Plutonium-244 Fuel Rod","Plutonium",Kind.NUCLEAR,1200000000L,64,64,4,2048,9330,0),
        new Rod(9231,"reactor_rod_9231","Plutonium-241 Fuel Rod","Plutonium241",Kind.NUCLEAR,1200000000L,128,128,3,3072,9331,0),
        new Rod(9232,"reactor_rod_9232","Plutonium-243 Fuel Rod","Plutonium243",Kind.NUCLEAR,1200000000L,128,128,3,4096,9332,0),
        new Rod(9233,"fuel_rod_pu_239","Plutonium-239 Fuel Rod","Plutonium239",Kind.NUCLEAR,2400000000L,128,128,3,4096,9333,0),
        new Rod(9239,"reactor_rod_9239","Blutonium Fuel Rod","Blutonium",Kind.NUCLEAR,1200000000L,64,64,4,1024,9339,0),
        new Rod(9240,"reactor_rod_9240","Americium-245 Fuel Rod","Americium",Kind.NUCLEAR,1200000000L,64,64,4,4096,9340,0),
        new Rod(9241,"fuel_rod_am_241","Americium-241 Fuel Rod","Americium241",Kind.NUCLEAR,1200000000L,128,128,3,4096,9341,0),
        new Rod(9249,"reactor_rod_9249","Ludicrite Fuel Rod","Ludicrite",Kind.NUCLEAR,1200000000L,128,128,3,3072,9349,0),
        new Rod(9250,"fuel_rod_co_60","Cobalt-60 Fuel Rod","Cobalt60",Kind.NUCLEAR,120000000L,8,0,16,256,9350,0),
        new Rod(9260,"reactor_rod_9260","Enriched Naquadah Fuel Rod","NaquadahEnriched",Kind.NUCLEAR,12000000000L,128,128,4,8192,9360,0),
        new Rod(9261,"reactor_rod_9261","Naquadria Fuel Rod","Naquadria",Kind.NUCLEAR,12000000000L,512,512,3,16384,9361,0),
        new Rod(9310,"reactor_rod_9310","Depleted Thorium-232 Fuel Rod","Thorium",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9319,"reactor_rod_9319","Depleted Cyanite Fuel Rod","Cyanite",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9320,"reactor_rod_9320","Depleted Uranium-238 Fuel Rod","Uranium",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9321,"reactor_rod_9321","Depleted Uranium-235 Fuel Rod","Uranium235",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9322,"reactor_rod_9322","Depleted Uranium-233 Fuel Rod","Uranium233",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9329,"reactor_rod_9329","Depleted Yellorium Fuel Rod","Yellorium",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9330,"reactor_rod_9330","Depleted Plutonium-244 Fuel Rod","Plutonium",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9331,"reactor_rod_9331","Depleted Plutonium-241 Fuel Rod","Plutonium241",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9332,"reactor_rod_9332","Depleted Plutonium-243 Fuel Rod","Plutonium243",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9333,"reactor_rod_9333","Depleted Plutonium-239 Fuel Rod","Plutonium239",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9339,"reactor_rod_9339","Depleted Blutonium Fuel Rod","Blutonium",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9340,"reactor_rod_9340","Depleted Americium-245 Fuel Rod","Americium",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9341,"reactor_rod_9341","Depleted Americium-241 Fuel Rod","Americium241",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9349,"reactor_rod_9349","Depleted Ludicrite Fuel Rod","Ludicrite",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9350,"reactor_rod_9350","Depleted Cobalt-60 Fuel Rod","Cobalt60",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9360,"reactor_rod_9360","Depleted Enriched Naquadah Fuel Rod","NaquadahEnriched",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9361,"reactor_rod_9361","Depleted Naquadria Fuel Rod","Naquadria",Kind.DEPLETED,0L,0,0,1,1,0,0),
        new Rod(9410,"reactor_rod_9410","Thorium-232 Breeder Rod","Thorium",Kind.BREEDER,64000000L,0,0,1,1,9411,1000),
        new Rod(9420,"reactor_rod_9420","Uranium-238 Breeder Rod","Uranium",Kind.BREEDER,256000000L,0,0,1,1,9421,2500),
        new Rod(9430,"reactor_rod_9430","Lithium Breeder Rod","Lithium",Kind.BREEDER,640000L,0,0,1,1,9431,250),
        new Rod(9440,"reactor_rod_9440","Naquadah Breeder Rod","Naquadah",Kind.BREEDER,4096000000L,0,0,1,1,9441,10000),
        new Rod(9411,"reactor_rod_9411","Uranium-233 Enriched Rod","Uranium233",Kind.PRODUCT,0L,0,0,1,1,9410,0),
        new Rod(9421,"reactor_rod_9421","Plutonium-239 Enriched Rod","Plutonium239",Kind.PRODUCT,0L,0,0,1,1,9420,0),
        new Rod(9431,"reactor_rod_9431","Tritium Enriched Rod","Tritium",Kind.PRODUCT,0L,0,0,1,1,9430,0),
        new Rod(9441,"reactor_rod_9441","Enriched Naquadah Enriched Rod","NaquadahEnriched",Kind.PRODUCT,0L,0,0,1,1,9440,0));
    public static Rod byOriginal(int id){return ALL.stream().filter(r->r.originalId()==id).findFirst().orElse(null);}
    private ReactorRodCatalog() {}
}
