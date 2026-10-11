package com.gregtech.gregtech.content.tool;
import java.util.List;
/** Original finite open-vessel profiles and fill/rain gates, independent of loader and storage. */
public final class OpenVesselRules {private OpenVesselRules(){}public record Profile(String id,boolean wooden,boolean bowl,boolean juicer,float hardness,float resistance){}
 public static final List<Profile> ALL=List.of(new Profile("mixing_bowl",false,true,false,1,5),new Profile("mixing_bowl_table",false,true,false,1,5),new Profile("juicer",false,false,true,1,5),new Profile("bathing_pot",false,false,false,1,6),new Profile("bathing_pot_table",false,false,false,1,6),new Profile("bathing_pot_wood",true,false,false,1,5),new Profile("bathing_pot_table_wood",true,false,false,1,5));
 public static Profile profile(String id){return ALL.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow();}
 public static int inputCapacity(boolean wooden){return wooden?4000:8000;}public static int outputCapacity(boolean juicer,boolean wooden){return juicer?1000000:inputCapacity(wooden);}
 public static boolean rainDue(long time){return time%600==10;}
 public static int rainfallAmount(float downfall,float temperature,boolean thunder){if(!Float.isFinite(downfall)||!Float.isFinite(temperature)||downfall<=0||temperature<.2F)return 0;return (int)Math.min(8000,Math.max(1,(long)(downfall*200))*(thunder?2L:1L));}
 public static boolean accepts(boolean wooden,boolean bowl,long temperature,long limit,int density,boolean simple,boolean gas,boolean acid,boolean magic){return density>0&&(wooden?temperature<=limit:temperature<limit)&&(!bowl||simple)&&(!wooden||simple&&!gas&&!acid&&!magic);}
 public static boolean recipePower(long power){return power>=0&&power<=ManualWorkRules.MAX_GU;}
 public static double exhaustionDivisor(boolean juicer,boolean bath){return juicer?10000:bath?1000:250;}
}
