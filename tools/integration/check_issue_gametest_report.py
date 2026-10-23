"""Require actual passing GameTest results for the focused open-issue audit."""
import argparse
import xml.etree.ElementTree as ET
from pathlib import Path

FORGE_METHODS = {
    'extrudershapesdonotsplitthemachinecreativefamily',
    'dungeonwaterplacementschedulesflowonlyaftersuccessfulwrite',
    'gtorehostsadmitsmalloreandrejectbedrockwithoutconsumption',
    'pilecovereventpreservessneakaddandemptysnapshotclearslegacycover',
}


FUNCTIONAL_CASES = {
    'machinemenuvaliditytests.basicmachinemenurequireslivenearbyowner',
    'machinemenuvaliditytests.bumbliarymenurequireslivenearbyowner',
    'machinemenuvaliditytests.advancedbumbliarymenurequireslivenearbyowner',
    'machinemenuvaliditytests.hoppermenurequireslivenearbyowner',
    'machinemenuvaliditytests.queuehoppermenurequireslivenearbyowner',

    'machineauxiliaryroutingtests.sametypeauxiliaryusesdisjointfacesandconservesbuffers',
    'machineauxiliaryroutingtests.differenttypeauxiliaryretainstypeandfaceisolation',
    'builderspikevarianttests.spikematerialsskipmismatchedstock',
    'builderspikevarianttests.spikemodesanddefaultpreserveplacementfacing',
    'builderspikevarianttests.spikenomatchingidentitypreservesstockandwear',
    'builderorevarianttests.orehostsskipmismatchedstock',
    'builderorevarianttests.orebrokenformskipsmismatchedstock',
    'builderorevarianttests.orenomatchingvariantpreservesinventory',
    'buildercontainertransactiontests.powderbucketsurvivalmainhand',
    'buildercontainertransactiontests.powderbucketsurvivaloffhand',
    'buildercontainertransactiontests.powderbucketcreativemainhand',
    'buildercontainertransactiontests.powderbucketcreativeoffhand',

    'reactorcapacityordertests.fluidsensitivefillpreservesdefaultandmapcontracts',
    'reactorcapacityordertests.reactorsteamexpansioncommitswholebatchesandsurvivessaveload',
    'reactorcapacityordertests.reactorexportsoldhotcoolantbeforequeuedcoolingexactlyonce',
    'reactorcapacityordertests.blockedreactoroutputsstilldestroyrodswithoutconsumingcoolant',
    'worldgenwritecontracttests.nativeoriginwoodclassification',
    'worldgenwritecontracttests.waterfaultinjectionpreservessurfacelevels',
    'worldgenwritecontracttests.pitfaultinjectionpreservesattemptdepth',
    'garbagemenuvaliditytests.garbagebinmenurequireslivenearbyowner',
    'garbagemenuvaliditytests.garbagerecyclermenurequireslivenearbyowner',

    'handheldmetertests.electrometerreadsacceptedwirepowerthroughregistereditem',
    'handheldmetertests.tachometerreadsrealaxlestartuptransfer',
    'handheldmetertests.tachometerreportsgearboxsidesandtransferredpower',
    'handheldmetertests.handheldmetersrejectunsupportedtargetswithoutconsumption',

    'foodcloneimmunitytests.nondeathrestorecopiesfoodstatswithoutaliasing',
    'foodcloneimmunitytests.emptyoriginalclearsstaleclonefoodstats',
    'foodcloneimmunitytests.deathrestorestillclearsfoodstatsanddose',
    'foodcloneimmunitytests.reactorradiationrespectscreativeandarmorprotection',
    'foodcloneimmunitytests.reactorradiationpreservesspeciesexclusionsandfallback',

    'machineioreservationtests.distinctfluidreservationsblockstartwithoutconsuminginputs',
    'machineioreservationtests.fluidreservationsmergesplitandrespectfullidentity',
    'machineioreservationtests.activeiteminputrespectsownwhitelistandblacklist',
    'machineioreservationtests.activeitemoutputandairejectionrespectownfilter',
    'reciperemaindertests.registered_machining_remainders_and_blocking',
    'reciperemaindertests.pulverized_remainder_ratios_and_floor',
    'toolplacementtransactiontests.buildersixfacescommitexactlyonce',
    'toolplacementtransactiontests.builderusesheadqualityandhitcoordinates',
    'toolplacementtransactiontests.buildercancellationrollsbackworldstockandwear',
    'toolplacementtransactiontests.drilltorchcancellationandcreativepreserveinventory',

    'bumblelifecycleissuetests.ordinarylifecyclepromotesloneprincess',
    'bumblelifecycleissuetests.advancedlifecyclepromotesloneprincess',
    'bumblelifecycleissuetests.deathclearsoldbeesbutpreservesunrelateditems',
    'bumblelifecycleissuetests.occupiedmainandblockedspareslotskeeporiginalrules',
    'bumblelifecycleissuetests.unsuitablebrooddiesinsteadofpromoting',
    'bumblelifecycleissuetests.pureprogressticksdirtynativechunkandserialize',

    'storagecovertransactiontests.garbageswapcommitsexactlyonce',
    'storagecovertransactiontests.garbagequickmovepreserveseveryremainder',
    'storagecovertransactiontests.usbnonselectorremovalpreservesselectedfile',
    'radawayconsumptiontests.lastradawaypillappliesonedose',
    'radawayconsumptiontests.stackedradawayappliesonceperpill',
    'radawayconsumptiontests.ordinaryfoodstillappliesstatisticsonce',
    'woodenbuckettransactiontests.topupconsumesexactlyonebucketandkeepsband',
    'woodenbuckettransactiontests.waterdoesnotreplacelavaorpowdersnow',
}
GAS_CASE = 'gasmotionregressiontests.gasusesairmotionwithoutremovinghazards'
EXPECTED = {
    'forge': {'openissueregressiontests.' + method for method in FORGE_METHODS} | {GAS_CASE} | FUNCTIONAL_CASES,
    'neoforge': {GAS_CASE} | FUNCTIONAL_CASES,
}


def check(path, platform='forge'):
    expected = EXPECTED[platform]
    root = ET.parse(path).getroot()
    cases = list(root.iter('testcase'))
    failures = [node for node in root.iter() if node.tag in ('failure', 'error', 'skipped')]
    if failures:
        raise ValueError(f'Report contains {len(failures)} failure/error/skipped results')
    for suite in root.iter():
        if suite.tag in ('testsuite', 'testsuites'):
            for field in ('failures', 'errors', 'skipped'):
                if float(suite.get(field, '0')) != 0:
                    raise ValueError(f'Report {field} count is nonzero')
    found = set()
    for case in cases:
        name = case.get('name', '').lower()
        classname = case.get('classname', '').lower()
        for identity in expected:
            test_class, method = identity.split('.', 1)
            # Mojang's JUnit reporter uses the template resource as classname,
            # and PrefixGameTestTemplate(false) leaves a bare method name.
            if name == method and classname == 'gregtech_issue_audit:test_empty':
                found.add(identity)
            elif name == method and classname.endswith(test_class):
                found.add(identity)
            elif name.endswith(identity):
                found.add(identity)
    missing = expected - found
    if missing:
        raise ValueError('Selected tests did not execute: ' + ', '.join(sorted(missing)))
    print(f'All {len(expected)} selected issue regressions executed and passed ({len(cases)} cases reported)')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report', type=Path)
    parser.add_argument('--platform', choices=sorted(EXPECTED), default='forge')
    args = parser.parse_args()
    try:
        check(args.report, args.platform)
    except (OSError, ET.ParseError, ValueError) as error:
        raise SystemExit(str(error)) from error
