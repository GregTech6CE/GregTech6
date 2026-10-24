"""Bounded source-guarded slurry rows; never reinterpret all FL.waters or legacy omd tokens."""
from pathlib import Path
import re
from .migrate_alumina_fluid_units import source_constants, uncomment, compact

U9 = 72_072_000
WATERS = ('Water', 'MnWtr', 'DistW', 'SpDew')
OLD = (
 '        r("Mixer", 16, 16, null, sp("omd:SluiceSand:111"), sp("w:100"), sp("f:Sluice:100"), NONE);',
 '        r("Mixer", 16, 16, null, sp("omd:SluiceSand:1000"), sp("w:900"), sp("f:Sluice:900"), NONE);',
 '        r("Drying", 16, 100, null, NONE, sp("f:Sluice:100"), sp("f:DistW:50"), sp("omd:SluiceSand:111"));')
NEW = tuple(line for water in WATERS for line in (
 f'        r("Mixer", 16, 16, null, sp("omdu:SluiceSand:{U9}"), sp("'+('w:100' if water=='Water' else f'f:{water}:100')+'"), sp("f:Sluice:100"), NONE);',
 f'        r("Mixer", 16, 16, null, sp("omd:SluiceSand:1000"), sp("'+('w:900' if water=='Water' else f'f:{water}:900')+'"), sp("f:Sluice:900"), NONE);')) + (
 f'        r("Drying", 16, 100, null, NONE, sp("f:Sluice:100"), sp("f:DistW:50"), sp("omdu:SluiceSand:{U9}"));',)
SOURCE = '''for (FluidStack tWater : FL.waters(1000)) {
RM.Mixer.addRecipe1(T,16,16,OM.dust(MT.SluiceSand,U9),FL.mul(tWater,1,10,T),FL.Sluice.make(100),ZL_IS);
RM.Mixer.addRecipe1(T,16,16,OM.dust(MT.SluiceSand),FL.mul(tWater,9,10,T),FL.Sluice.make(900),ZL_IS);
}
RM.Drying.addRecipe0(T,16,100,FL.Sluice.make(100),FL.DistW.make(50),OM.dust(MT.SluiceSand,U9));'''
FL_SOURCE = 'public static FluidStack[] waters(long aWater, long aDistilled, long aSpectralDew) {return FL.array(FL.Water.make(aWater), FL.MnWtr.make(aWater), FL.DistW.make(aDistilled), FL.SpDew.make(aSpectralDew));}'

def migrate_ores(content: str, ores: Path, constants: Path, fluids: Path) -> str:
    units = source_constants(constants)
    if units['U'] != 648_648_000 or units['U9'] != U9:
        raise ValueError('Reviewed source material units changed')
    if compact(uncomment(ores.read_text())).count(compact(SOURCE)) != 1:
        raise ValueError('Expected exact original slurry water loop and Drying rule')
    fluid_source = compact(uncomment(fluids.read_text()))
    if compact(FL_SOURCE) not in fluid_source or compact('public static FluidStack[] waters(long aWater) {return waters(aWater, aWater, aWater);}') not in fluid_source:
        raise ValueError('Reviewed four-water expansion changed')
    old='\n'.join(OLD)+'\n';new='\n'.join(NEW)+'\n'
    if content.count(new)==1 and content.count(OLD[0])==0 and content.count(OLD[2])==0:return content
    if content.count(old)!=1 or any(content.count(line)!=1 for line in OLD) or any(line in content for line in NEW if line!=OLD[1]):
        raise ValueError('Expected one unmigrated or one fully migrated slurry row group')
    result=content.replace(old,new,1)
    # The source call count stays the same; only six reviewed fluid alternatives are expanded.
    return re.sub(r'(RM calls translated into )(\d+)( recipes;)',lambda m:m[1]+str(int(m[2])+6)+m[3],result,count=1)
