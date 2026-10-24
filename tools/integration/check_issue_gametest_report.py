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
    'namedfluidtemperaturetests.coolant_ic2_hotkeepsauthoredtemperature',
    'namedfluidtemperaturetests.glasskeepsauthoredtemperature',
    'namedfluidtemperaturetests.matterchargedkeepsauthoredtemperature',
    'namedfluidtemperaturetests.matterneutralkeepsauthoredtemperature',
    'namedfluidtemperaturetests.oil_fryingkeepsauthoredtemperature',
    'namedfluidtemperaturetests.phaseandauthoredmoltencontrolsstayunchanged',
    'namedfluidtemperaturetests.thorium_saltkeepsauthoredtemperature',
    'namedfluidtemperaturetests.uuamplifierkeepsauthoredtemperature',
    'namedfluidtemperaturetests.uumkeepsauthoredtemperature',

    'stonelayerslabaudittests.structurecobbleslabsfollowonlytheirlocallayer',
    'waterbodybiomeboundarytests.swampoverridesgtwateronlyinsideitsowncolumns',
    'oreoverlaylifecyclecontrols.warmedsifterandstartupaddonrowssurviverepeatedoverlay',
    'oreoverlaylifecyclecontrols.sifterdatapackreplacementwinswarmlookupandremovalrestoressource',
    'oreoverlaylifecyclecontrols.exactunlinkinvalidateswarmlookupwithoutremovingindependentordynamicrows',

    'energyneighborloadednesstests.laserneighborlookupskipsabsentborderswithoutretainingwaste',
    'energyneighborloadednesstests.rotationradialpollingskipsabsentbordersandkeepsnativepushinput',
    'spikeharveststatetests.sourcematerialqualityandnativevanillatoolsfollowsecondarystate',
    'spikeharveststatetests.actualsurvivalbreakrejectslowtierandkeepssecondarydrop',

    'spikefallinglifecycletests.itemplacementschedulesfallingandemitsoriginalsound',
    'spikefallinglifecycletests.realfallingitemlandsordropswithoutlosingvariant',

    'fluidslotamountoverlaytests.inputslotbucketoverlaykeepsexactliters',
    'fluidslotamountoverlaytests.outputslotbucketoverlaykeepsexactliters',
    'barracksdooralignmenttests.generateddoorsjoinslabwallsandsurviveupdates',
    'bottlecontentsnativetests.vanillabottlesandnativecapabilitypayloadskeeptheiridentity',
    'bottlefluidresolutiontests.allgeneratedbottlesresolvetheiractualregisteredfluid',
    'bottlefluidresolutiontests.sixpreviouslywaterbottlesusesourcenamedspriteandwhitetint',
    'crankdurationaudittests.singlesurvivalclickrunsovertwentysecondsuntilanotherinteraction',
    'crankdurationaudittests.repeatedclicksdonotcreateastopdeadlineandleavingrangestops',
    'crankdurationaudittests.creativedistancebypassandremovedoperatorstops',
    'crankdurationaudittests.multiplayerlastremainingoperatorkeepscrankactive',
    'crankdurationaudittests.energystartsnexttickandcontinueseverytickpasttwentyseconds',

    'platepilehighlighttests.loosepilesneverselectcovergrid',
    'platepilehighlighttests.legitimatecoverandtoolgridsremain',
    'sedimentsurfaceaudittests.pitkeepsgrassandgeneratesbelowit',
    'sedimentsurfaceaudittests.blacksandkeepsgrassandgeneratesbelowit',
    'spikedeliveryaudittests.thirtysourcevariantitems',
    'spikeconversionaudittests.twentyconversionsmatchonlytheirsourcevariant',
    'coverremovalupdatetests.crowbarremovaldetachesnativeleveroneverythinwireface',
    'coverremovalupdatetests.crowbarremovaldistinguishesmediumandfulldiameterpipesupport',
    'coverremovalupdatetests.removingnonsolidvalvepreservesunderlyingfullhostsupport',

    'signalcoverbehaviortests.nativetorchandrepeaterstrongrelaythroughsolid',
    'signalcoverbehaviortests.nativeattachmentdustconnectionsandnotifications',
    'signalcoverbehaviortests.nativeuncoveredwirefeedbackandattachmentintakecontrols',
    'crystalchargerinteractiontests.nativechargerinsertionkeepsenergyandinteractionguards',
    'crystalchargerinteractiontests.nativecrystalinsertionusesluandpreservescharge',
    'fuelengineintaketests.exhaustventsbygasphaseandemptycollisionnotbuoyancyoraironly',
    'aluminiumweldingaudittests.aluminium_original_easy_welding_registered_costs',
    'aluminiumweldingaudittests.furnace_rotor_original_fixed_duration',
    'aluminiumweldingaudittests.welding_base_display_and_work_contract',
    'aluminiumweldingaudittests.inherited_flags_aliases_and_hard_control',
    'aluminiumweldingaudittests.source_simple_capsules_use_registered_forms_and_retained_molds',
    'fuelengineintaketests.originalenginefuelrowsproduceexactenergyandbufferedcarbondioxide',
    'fuelengineintaketests.alleightdieselsacceptknownfuelonallsixfaceswithoutsimulationmutation',
    'fuelengineintaketests.sourcefuelshaveregisteredrecipesandnativeintake',
    'fuelengineintaketests.nativepipeplacementandtransferreacheachengineface',
    'fuelengineintaketests.stoppedmodenbtandsidedcoverremainindependentoffueladmission',
    'fuelengineintaketests.finiteportablecontainerusesitsnativeitemfirsttransfer',
    'fuelengineintaketests.creativedisplayfuelcarrierfulldispatchcanrefuelengine',
    'fuelengineintaketests.unrelatedwoodenbucketandwatercarrierdonotconsumeorfill',
    'machinefluidframeworkcontroltests.registeredbathacceptsdieseldisplaycarrier',

    'coverplatesupporttests.blankcoveraddsoutlineandnativeleversupport',
    'coverplatesupporttests.coversupportisfacelocalandtorchremainsnonsolid',
    'electricwiregraphloadednesstests.wireconnectioncallbackskipsabsentsavedmaskneighbors',

    'signalwireplacementcovertests.pairedsignalwiresstaydisconnectedafternativecutter',
    'signalwireplacementcovertests.signalwireplacementhonorsexplicitneighborports',
    'signalwireplacementcovertests.signalwireportssurviveneighborpowerandremoval',
    'signalwireplacementcovertests.bareandinsulatedsignalwiresacceptnativetorchcovers',
    'signalwireplacementcovertests.signalplacementandcutterrespectneighbortorchblocker',

    'pipeplacementendpointtests.itempipeplacementconnectsclickedchestonly',
    'pipeplacementendpointtests.fluidpipeplacementconnectsclickednativetankonly',
    'pipeplacementendpointtests.electricwirenativeplacementconnectssupportreciprocally',
    'pipeplacementendpointtests.electricwireplacementrespectsnativeeufaces',
    'pipeplacementendpointtests.fluidplacementpreservesshuttersandmanualdisconnects',
    'pipeplacementendpointtests.itemplacementpreservesshuttersandmanualdisconnects',

    'turbineplungerprioritytests.nativeplungersneakingoffhandpreservessteampriority',
    'turbineplungerprioritytests.nativeplungerpurgessteambeforerealcondensate',
    'turbineplungerprioritytests.nativeplungerpurgesrealcondensateatoriginalwear',
    'turbineplungerprioritytests.emptysteamturbineplungerdoesnotspendwear',
    'turbineplungerprioritytests.nonturbinetankkeepsgenericthousandmbplunger',
    'gasturbinepartialfueltests.minimumoperationalhydrogenbatchreachesrealdynamo',
    'gasturbinepartialfueltests.fractionalpartialhydrogenbatchroundswithoutcreatingenergy',
    'gasturbinepartialfueltests.belowminimumhydrogenbatchretainsheatwithoutru',
    'gasturbinepartialfueltests.emptygasturbinecreatesnoenergyorexhaust',
    'gasturbinepartialfueltests.fullhydrogenbatchpreservesmaximumoutputrate',
    'gasturbinepartialfueltests.blockedpartialgaspulsesurvivesfreshentityloadandtransfersonce',
    'aluminafluidunitcontracttests.scaledaluminaroutesuseexactsourceunits',
    'aluminafluidunitcontracttests.sourceunittokenskeepnativeidentityandlegacysemantics',
    'aluminafluidquantitytests.specializedaluminausesoriginalfluidcoefficients',
    'materialdecompositionpolicytests.genericelectrolysisrequiresandretainsselectorzero',
    'materialdecompositionpolicytests.unflaggedaluminaandsaltdonotautodecompose',
    'materialdecompositionpolicytests.decompositionmapsfollowexplicitmaterialprocessing',
    'materialdecompositionpolicytests.specializedaluminastillconsumescarbonandbothfluids',
    'fluidmaterialmetadatatests.cryolitekeepsidandoriginalmoltenproperties',
    'fluidmaterialmetadatatests.aluminiumfluoridekeepsidandoriginalmoltenproperties',
    'fluidmaterialmetadatatests.legacymoltensaltskeepidsandoriginalproperties',
    'fluidmaterialmetadatatests.generatedmoltenusesoriginalheatandlight',
    'fluidmaterialmetadatatests.staticgeneratedmoltenusesmaterialheatandlight',
    'fluidmaterialmetadatatests.explicitfluidpropertiesremainauthoritative',
    'queuemenupermissiontests.queueguipickupcanextracteveryslot',
    'queuemenupermissiontests.queueguiquickmovecanextracteveryslot',
    'queuemenupermissiontests.queueguipickupinsertionandcapacityusemanualslots',
    'machinemenutransactiontests.basicinputandoutputquickmovesnotifyowner',
    'machinemenutransactiontests.autocrafterprogramquickmovesnotifyowner',
    'machinemenutransactiontests.behaviorquickmovesnotifyactualowners',
    'machinemenutransactiontests.bumbliaryquickmovesnotifyowner',
    'machinemenutransactiontests.advancedbumbliaryquickmovesnotifyowner',
    'machinemenutransactiontests.behaviorquickmoverejectsexistingoutputmerge',
    'machinemenutransactiontests.bumbliaryquickmovesrejectexistingoutputmerges',
    'menuslotnotificationtests.drawerquickmovesdirtynativechunk',
    'menuslotnotificationtests.metalchestquickmovesdirtynativechunk',
    'menuslotnotificationtests.safequickmovesdirtynativechunk',
    'menuslotnotificationtests.garbagebinquickmovesdirtynativechunk',
    'menuslotnotificationtests.hopperquickmovesdirtynativechunk',
    'menuslotnotificationtests.queuehopperquickmovesdirtynativechunk',
    'menuslotnotificationtests.hoppermenupartialwithdrawalwakescompaction',
    'menuslotnotificationtests.queuemenupartialwithdrawalwakescascade',
    'menulifetimecapacitytests.behaviormenurequiresliveownerandnativereach',
    'menulifetimecapacitytests.batterymenusrequirelivenearbyowners',
    'menulifetimecapacitytests.drawercapacityqueriespreservelegacyoverflow',
    'menulifetimecapacitytests.advancedcraftingcapacityqueriespreserveflush',
    'controlleroutputlifecycletests.distillationremoteitemsrespectownfilterandshutter',
    'controlleroutputlifecycletests.cryoremoteitemsrespectownfilterandshutter',
    'controlleroutputlifecycletests.implosionnativetickerclearsinvalidactivityandretainsjob',
    'bedrockdrilloutputlifecycletests.intactdrillexportscompletednativeproduct',
    'bedrockdrilloutputlifecycletests.brokendrillexportscompletedproductwithoutminingandresumesafterrepair',
    'bedrockdrilloutputlifecycletests.removeddrillcannotexportpendingnativeproduct',
    'fusioncoverlifecycletests.fusiontimerchargesacrosswindowsandretainsprocessingafterload',
    'fusioncoverlifecycletests.fusioncontrolsensorsstopandblockedoutputsconservejob',
    'fusioncoverlifecycletests.fusioncoverclockandtransfersrespectinvalidandstoppedstates',
    'addonfluididentitytests.addonbindingkeepsbuiltinfluidregistryidentity',
    'addonfluididentitytests.legacybuiltinfluidsavesurvivesaddonmaterialbinding',
    'addonfluididentitytests.generatedaddonfluidsretaintheirownnamespace',
    'itempipeshuttertests.itempipeshutterblocksnativehopperpushandpull',
    'itempipeshuttertests.itempipeshutterupdatespreviouslyobtainedsidehandlers',
    'heatsignedextractiontests.signedextractionconservesheatacrossnativeadapters',
    'heatsignedextractiontests.signedlargeboilerextractionpreservesstructuregate',
    'fluidpipeinspectiontests.fluidpipemagnifierskipsunloadedchunksandreportsloadednetwork',
    'hoppertransfercontracttests.queuehopperpullrespectsselecteditemstacklimit',
    'hoppertransfercontracttests.queuehopperpreserveslegacyoversizedcontents',
    'hoppertransfercontracttests.hopperminimumbatchrequiresdestinationcapacity',

    'machinependingstatetests.partiallydeliveredmachinejobsurvivesfreshentityload',
    'machinependingstatetests.blockedgasturbinepulsesurvivesfreshentityload',
    'behaviorstructuresafetytests.behaviormixedalternativesbindactualfixedandrepeatedparts',
    'behaviorstructuresafetytests.behaviorretainedportrejectsbrokenstructurebeforetickandrebinds',
    'behaviorstructuresafetytests.behaviorenergypollingdoesnotloadremoteborderneighbor',

    'fluidpresencerecipetests.presenceusesoriginaltankswithoutreservation',
    'fluidpresencerecipetests.addonpresencesurvivesreadaddfindroundtrip',
    'fluidpresencerecipetests.presencecollisiondoesnotbecomeconsumablestock',
    'fluidpresencerecipetests.unsupporteddirectfuelmapsfailclosed',
    'fluidpresencerecipetests.barrelskipspresencewithoutdisablingordinaryfermenter',
    'fluidpresencerecipetests.customexchangermaprejectspresencewithoutdraining',
    'fluidpresencerecipetests.textualzerooverlayretainsnativefluididentity',
    'lockeractivationtests.ordinarylockeronlyfrontmainhandswapsarmor',
    'lockeractivationtests.incompatiblestoredequipmentskipsonlyitsownslot',
    'multiblocknativechunklifecycletests.registeredmultiblocksurvivesrealunloadandstagedreload',

    'machinechunkboundarytests.formedlargemachinepollingdoesnotloadexteriorneighbor',
    'machinechunkboundarytests.unloadedautomaticioretainsstockthentransferstoloadedchest',
    'multiblockalternativebindingtests.mixedfixedalternativesbindonlyactualpartsineitherorder',
    'multiblockalternativebindingtests.mixedrepeatedalternativesignoreincompletecopyandreform',

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
FORGE_ONLY_CASES = {
    'covercapabilitylifetimetests.fallbackdrawercapabilitiesinvalidatewithsidemapping',
    'covercapabilitylifetimetests.fallbackunresolvedcapabilitiesinvalidateonrotationandremoval',
    'covercapabilitylifetimetests.fallbackcapabilitycachereplacesitemandfluidsources',
    'covercapabilitylifetimetests.fallbacksharedsourceinvalidationsupportscrossfacerequery',
    'covercapabilitylifetimetests.fallbackreentrantnewersourcesurvivesstaleouterlookup',
}
GAS_CASE = 'gasmotionregressiontests.gasusesairmotionwithoutremovinghazards'
LEGACY_CASE_NAMESPACES = {
    'coverissuetests.solarhasnowaterandcoverplatehasdepth': 'gregtech_cover_issues:test_empty',
    'coverissuetests.coretickinputanddisplaydirection': 'gregtech_cover_issues:test_empty',
    'coverissuetests.logisticsstorageandcoveredwirestaydisconnected': 'gregtech_cover_issues:test_empty',
    'coverissuetests.heldtoolsconfigurenativedetectorandremovalresets': 'gregtech_cover_issues:test_empty',
    'coverissuetests.selectorsfallbacksignalsandlatchedcoverstop': 'gregtech_cover_issues:test_empty',
    'coverissuetests.helditemworkbenchstaysopenandharvestretainscover': 'gregtech_cover_issues:test_empty',
    'coverissuetests.wireonlytorchandshuttersgatepipes': 'gregtech_cover_issues:test_empty',
    'coverissuetests.itemfiltersignorecomponentsanddecorationsuseheldchisel': 'gregtech_cover_issues:test_empty',
    'coverissuetests.drainbulkexperiencewalkandfilterconnectionboundary': 'gregtech_cover_issues:test_empty',
    'pipeautoconnecttests.fluidpipeconnectstoconnectableblockplacedafter': 'gregtech_pipe_auto:test_empty',
    'pipeautoconnecttests.fluidpipeplacedafterconnectableblockconnects': 'gregtech_pipe_auto:test_empty',
    'pipeautoconnecttests.itempipeautoconnectsinbothorders': 'gregtech_pipe_auto:test_empty',
    'pipeautoconnecttests.wrenchdisconnectsurvivesneighbourchanges': 'gregtech_pipe_auto:test_empty',
    'pipeautoconnecttests.placementscanrepairshalfconnection': 'gregtech_pipe_auto:test_empty',
    'pipeautoconnecttests.signalwireignoresunselectedsourceplacedafter': 'gregtech_pipe_auto:test_empty',
    'pipeautoconnecttests.signalwirestateplacementdoesnotinfersourceports': 'gregtech_pipe_auto:test_empty',
    'pipeautoconnecttests.signalwireretainsexplicitportwhensourceisremoved': 'gregtech_pipe_auto:test_empty',
    'pipeautoconnecttests.signalwireskeepunselectedneighborportsclosed': 'gregtech_pipe_auto:test_empty',
}


API_CONTRACT_CASES = {
    'addonenergyaccesstests.energyresultspreserveunitsandbufferownership',
    'addonenergyaccesstests.energyvalidationrunsbeforehostmutation',
    'addoninventoryaccesstests.partadapterisolateslegacyreferences',
    'addoninventoryaccesstests.snapshotaccesspreservesmachinestorage',
    'addonrecipeapitests.builderkeepsconstructorsemanticsandownsitsarguments',
    'addonrecipeapitests.dynamicproviderscannotmutateinventoryorlaterqueries',
    'addonrecipeapitests.reciperowsowntheirarraysandstacks',
}


ENERGY_STORAGE_CASE_NAMESPACES = {
    'crystalbarreltests.barrels_use_actual_small_capacity_and_persistence': 'gt6ce_crystal_barrel:test_empty',
    'crystalbarreltests.canonical_crystals_and_survival_recipes': 'gt6ce_crystal_barrel:test_empty',
    'crystalbarreltests.charger_uses_shared_lu_buffer_and_faces': 'gt6ce_crystal_barrel:test_empty',
    'dieselenginerepairtests.fuelcyclefrontonlyandheavygasexhaustinallorientations': 'gt6ce_diesel_repair:test_empty',
    'dieselenginerepairtests.taprecoversfuelafterexhaustandjadereportsdiesel': 'gt6ce_diesel_repair:test_empty',
    'legacyenergydevicetests.absorber_trophies_output_faces_and_switch': 'gt6ce_legacy_energy:test_empty',
    'legacyenergydevicetests.fuel_cell_survival_and_empty_shell': 'gt6ce_legacy_energy:test_empty',
    'legacyenergydevicetests.kilogibblometer_reads_real_large_boiler_through_shell': 'gt6ce_legacy_energy:test_empty',
    'machineperformancetests.boilerkeepsproductionandpersistentstatewithcompactclientdata': 'gt6ce_machine_performance:test_empty',
    'machineperformancetests.cachedcandidateskeepprioritycountsflagsoutputsandreloadlive': 'gt6ce_machine_performance:test_empty',
    'machineperformancetests.dynamicprovidersarenotsuppressedbycachedmisses': 'gt6ce_machine_performance:test_empty',
    'machineperformancetests.fullinputwakesimmediatelyandmachinekeepsitsprocessingrate': 'gt6ce_machine_performance:test_empty',
    'machineperformancetests.matchingreservescatalystsandsplitinputswithoutmutatingsnapshots': 'gt6ce_machine_performance:test_empty',
    'machineperformancetests.neighbourcachefollowsrotationremovalandreplacement': 'gt6ce_machine_performance:test_empty',
    'machineperformancetests.unchangedtankwritesdonotnotifybutrealtransfersdo': 'gt6ce_machine_performance:test_empty',
}


COVER_REMOVAL_CASE_NAMESPACES = {
    'coverremovalupdatetests.crowbarremovaldetachesnativeleveroneverythinwireface': 'gregtech_issue_audit:cover_removal_empty',
    'coverremovalupdatetests.crowbarremovaldistinguishesmediumandfulldiameterpipesupport': 'gregtech_issue_audit:cover_removal_empty',
    'coverremovalupdatetests.removingnonsolidvalvepreservesunderlyingfullhostsupport': 'gregtech_issue_audit:cover_removal_empty',
}


ORE_SOURCE_CASES = {
    'oresourcedifferentialtests.crusherraw',
    'oresourcedifferentialtests.sluicewater',
    'oresourcedifferentialtests.sifter',
    'oresourcedifferentialtests.centrifugepurified',
    'oresourcedifferentialtests.sluicepetrotheum',
    'oresourcedifferentialtests.steamcracking',
    'oresourcedifferentialtests.sluicewatertiny',
    'oresourcedifferentialtests.siftertiny',
    'oresourcedifferentialtests.centrifugepurifiedtiny',
    'oresourcedifferentialtests.sluicepetrotheumtiny',
    'oresourcedifferentialtests.steamcrackingtiny',
    'oresourcedifferentialtests.shreddercrushed',
    'oresourcedifferentialtests.shreddercrushedpurified',
    'oresourcedifferentialtests.shreddercrushedcentrifuged',
    'oresourcedifferentialtests.shreddercrushedtiny',
    'oresourcedifferentialtests.shreddercrushedpurifiedtiny',
    'oresourcedifferentialtests.shreddercrushedcentrifugedtiny',
    'oresourcecontroltests.furnaceusessourcefurnacenotmeltingflag',
    'oresourcecontroltests.gemlessandwoodlikematerialsdonotacquiregenericoreroutes',
    'oresourcecontroltests.magneticoreusesonlyoppositemagneticcandidates',
    'oresourcecontroltests.allregisteredsluiceskeepnineoutputsacrosssaveload',
    'oresourcecontroltests.opaquelapisgradesuseoriginalfallbackevenwhenlegacygradeitemsexist',
    'oresourcecontroltests.sluicerollseachofeightslotsandeachitemindependently',
    'oresourcecontroltests.realaddonownedmapssurvivetwooverlayrebuilds',
    'orebathsupplementtests.cinnabarblockdustcryotheum',
    'orebathsupplementtests.cinnabarcrushedcryotheum',
    'orebathsupplementtests.cinnabartinycrushedcryotheum',
}


ORE_FOLLOWON_CASE_NAMESPACES = {'orethermalsourcetests.rawanddirtyformshavenogenericthermalrows': 'gregtech_ore_thermal_audit:test_empty', 'orethermalsourcetests.dustrecyclestotargetmercurynotmoltencinnabar': 'gregtech_ore_thermal_audit:test_empty', 'orethermalsourcetests.dustfractionspreservesourcefloorrounding': 'gregtech_ore_thermal_audit:test_empty', 'orethermalsourcetests.cleanandrefinedformsremainrecyclable': 'gregtech_ore_thermal_audit:test_empty', 'orethermalsourcetests.cleanandrefinedtinyformsusesourceunits': 'gregtech_ore_thermal_audit:test_empty', 'orethermalsourcetests.submillibucketdustdoesnotcreateafluidrow': 'gregtech_ore_thermal_audit:test_empty', 'orethermalsourcetests.blockdustpreservessourceweightandtruncatedwork': 'gregtech_ore_thermal_audit:test_empty', 'orerawcraftingsourcetests.cinnabarrawgemtwobytwopositions': 'gregtech_ore_source_audit:test_empty', 'orerawcraftingsourcetests.cinnabarrawgemthreebythreepositions': 'gregtech_ore_source_audit:test_empty', 'orerawcraftingsourcetests.cinnabarrawgemoccupiedcellsnotstackcount': 'gregtech_ore_source_audit:test_empty'}


# New material phase contracts require the actual Mojang template namespace and bare method.
# Extend this map only when a new native case identity has been confirmed.
MATERIAL_PHASE_CASE_NAMESPACES = {
    'materialdecompositionpolicytests.decompositionusesfluidinputsambientphasesandoriginalwork':
        'gregtech_issue_audit:test_empty',
    'smeltingtargetremovaltests.removedsmeltingtargetretainsconsumeddustacrosshotphase':
        'gregtech_issue_audit:crucible_removal_empty',
}
FUNCTIONAL_CASES |= set(MATERIAL_PHASE_CASE_NAMESPACES)

# Separate bounded eight-owner centrifuge slice; do not expand the historical 27-case source group.
NEXT_ORE_CENTRIFUGE_CASE_NAMESPACES = {
    'solidorecentrifugetests.chalcopyritepurifiedwhole': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.chalcopyritepurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.pentlanditepurifiedwhole': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.pentlanditepurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.sphaleritepurifiedwhole': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.sphaleritepurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.tetrahedritepurifiedwhole': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.tetrahedritepurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.pyritepurifiedwhole': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.pyritepurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.nickelpurifiedwhole': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.nickelpurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.cooperitepurifiedwhole': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.cooperitepurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.yellowlimonitepurifiedwhole': 'gregtech_ore_source_audit:test_empty',
    'solidorecentrifugetests.yellowlimonitepurifiedtiny': 'gregtech_ore_source_audit:test_empty',
}
FUNCTIONAL_CASES |= set(NEXT_ORE_CENTRIFUGE_CASE_NAMESPACES)

# Separate bounded positive-ore slice; do not expand the historical 27-case source group.
POSITIVE_ORE_CENTRIFUGE_CASE_NAMESPACES = {
    'positiveorecentrifugetests.copperpurifiedwholehasthreesourcebyproducts': 'gregtech_ore_source_audit:test_empty',
    'positiveorecentrifugetests.copperpurifiedtinykeepssourcegradeandwork': 'gregtech_ore_source_audit:test_empty',
    'positiveorecentrifugetests.galenapurifiedwholehasthreesourcebyproducts': 'gregtech_ore_source_audit:test_empty',
    'positiveorecentrifugetests.galenapurifiedtinykeepssourcegradeandwork': 'gregtech_ore_source_audit:test_empty',
    'positiveorecentrifugetests.netherquartzpurifiedwholeusesqualitythreework': 'gregtech_ore_source_audit:test_empty',
    'positiveorecentrifugetests.netherquartzpurifiedtinykeepssourcegradeandwork': 'gregtech_ore_source_audit:test_empty',
}
FUNCTIONAL_CASES |= set(POSITIVE_ORE_CENTRIFUGE_CASE_NAMESPACES)


QUALITY_PHYSICS_CASE_NAMESPACES = {'orehostphysicstests.actualstatehardnessandcontextualblastfollowliteralsourcehosts': 'gregtech_ore_source_audit:ore_host_physics_owned', 'orehostphysicstests.unreviewedhostsandnonoredenseclasseskeepexistingphysics': 'gregtech_ore_source_audit:ore_host_physics_owned', 'orehostharvesttests.nativeplayerharvesthonorssmallbrokenoffsetsandhostminimum': 'gregtech_ore_source_audit:ore_host_physics_owned', 'orehostharvesttests.nativedestroyprogressuseshardnessoncewithoutlegacybrokenmultiplier': 'gregtech_ore_source_audit:ore_host_physics_owned', 'orehostharvesttests.commonitemtooltipreadshostbrokenstateandhideslowtier': 'gregtech_ore_source_audit:ore_host_physics_owned', 'orequalitymetadatatests.reviewedharvestqualitypreservesnativeorelevelsandlegacyforms': 'gregtech_ore_source_audit:test_empty', 'orequalitymetadatatests.bauxiteandscheelitequalityscaleacceptedcrusherhammerwork': 'gregtech_ore_source_audit:test_empty'}
FUNCTIONAL_CASES |= set(QUALITY_PHYSICS_CASE_NAMESPACES)

# Separate six-form Shredder slice; retain all existing source and quality/physics groups.
ORE_SHREDDER_CASE_NAMESPACES = {
    'oreshreddersourcetests.coppercrushed': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.coppercrushedpurified': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.coppercrushedcentrifuged': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.coppercrushedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.coppercrushedpurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.coppercrushedcentrifugedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.galenacrushed': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.galenacrushedpurified': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.galenacrushedcentrifuged': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.galenacrushedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.galenacrushedpurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.galenacrushedcentrifugedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.netherquartzcrushed': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.netherquartzcrushedpurified': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.netherquartzcrushedcentrifuged': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.netherquartzcrushedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.netherquartzcrushedpurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.netherquartzcrushedcentrifugedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.chalcopyritecrushed': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.chalcopyritecrushedpurified': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.chalcopyritecrushedcentrifuged': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.chalcopyritecrushedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.chalcopyritecrushedpurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.chalcopyritecrushedcentrifugedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.pentlanditecrushed': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.pentlanditecrushedpurified': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.pentlanditecrushedcentrifuged': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.pentlanditecrushedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.pentlanditecrushedpurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.pentlanditecrushedcentrifugedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.sphaleritecrushed': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.sphaleritecrushedpurified': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.sphaleritecrushedcentrifuged': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.sphaleritecrushedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.sphaleritecrushedpurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.sphaleritecrushedcentrifugedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.tetrahedritecrushed': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.tetrahedritecrushedpurified': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.tetrahedritecrushedcentrifuged': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.tetrahedritecrushedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.tetrahedritecrushedpurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.tetrahedritecrushedcentrifugedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.pyritecrushed': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.pyritecrushedpurified': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.pyritecrushedcentrifuged': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.pyritecrushedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.pyritecrushedpurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.pyritecrushedcentrifugedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.nickelcrushed': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.nickelcrushedpurified': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.nickelcrushedcentrifuged': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.nickelcrushedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.nickelcrushedpurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.nickelcrushedcentrifugedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.cooperitecrushed': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.cooperitecrushedpurified': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.cooperitecrushedcentrifuged': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.cooperitecrushedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.cooperitecrushedpurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.cooperitecrushedcentrifugedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.yellowlimonitecrushed': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.yellowlimonitecrushedpurified': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.yellowlimonitecrushedcentrifuged': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.yellowlimonitecrushedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.yellowlimonitecrushedpurifiedtiny': 'gregtech_ore_source_audit:test_empty',
    'oreshreddersourcetests.yellowlimonitecrushedcentrifugedtiny': 'gregtech_ore_source_audit:test_empty',
}
FUNCTIONAL_CASES |= set(ORE_SHREDDER_CASE_NAMESPACES)

def expected_namespace(identity):
    if identity in ORE_SHREDDER_CASE_NAMESPACES:
        return ORE_SHREDDER_CASE_NAMESPACES[identity]
    if identity in {
        "toolplacementtransactiontests.builderusesheadqualityandhitcoordinates",
        "toolplacementtransactiontests.buildercancellationrollsbackworldstockandwear",
    }:
        return "gregtech_issue_audit:builder_placement_empty"
    if identity in QUALITY_PHYSICS_CASE_NAMESPACES:
        return QUALITY_PHYSICS_CASE_NAMESPACES[identity]
    if identity == "toolplacementtransactiontests.drilltorchcancellationandcreativepreserveinventory":
        return "gregtech_issue_audit:torch_placement_empty"
    if identity in NEXT_ORE_CENTRIFUGE_CASE_NAMESPACES:
        return NEXT_ORE_CENTRIFUGE_CASE_NAMESPACES[identity]
    if identity in POSITIVE_ORE_CENTRIFUGE_CASE_NAMESPACES:
        return POSITIVE_ORE_CENTRIFUGE_CASE_NAMESPACES[identity]
    if identity in MATERIAL_PHASE_CASE_NAMESPACES:
        return MATERIAL_PHASE_CASE_NAMESPACES[identity]
    if identity.startswith('oreoverlaylifecyclecontrols.'):
        return 'gregtech_ore_source_audit:test_empty'
    if identity.startswith('stonelayerslabaudittests.'):
        return 'gregtech_issue_audit:stone_layer_slab_empty'
    if identity.startswith('waterbodybiomeboundarytests.'):
        return 'gregtech_issue_audit:water_biome_boundary_empty'
    if identity in {'sedimentsurfaceaudittests.pitkeepsgrassandgeneratesbelowit',
                    'sedimentsurfaceaudittests.blacksandkeepsgrassandgeneratesbelowit'}:
        return 'gregtech_issue_audit:sediment_columns_empty'
    if identity.startswith("spikeharveststatetests."):
        return "gregtech_issue_audit:spike_harvest_empty"
    if identity.startswith("spikefallinglifecycletests."):
        return "gregtech_issue_audit:spike_falling_empty"
    if identity == 'barracksdooralignmenttests.generateddoorsjoinslabwallsandsurviveupdates':
        return 'gt6ce_barracks_door:barracks'
    if identity in ORE_FOLLOWON_CASE_NAMESPACES:
        return ORE_FOLLOWON_CASE_NAMESPACES[identity]
    if identity.startswith('crankdurationaudittests.'):
        return 'gregtech_issue_audit:crank_duration_empty'
    if identity in ORE_SOURCE_CASES:
        return 'gregtech_ore_source_audit:test_empty'
    if identity in COVER_REMOVAL_CASE_NAMESPACES:
        return COVER_REMOVAL_CASE_NAMESPACES[identity]
    if identity in ENERGY_STORAGE_CASE_NAMESPACES:
        return ENERGY_STORAGE_CASE_NAMESPACES[identity]
    if identity in API_CONTRACT_CASES:
        return 'gt6ce_api_contracts:test_empty'
    return LEGACY_CASE_NAMESPACES.get(identity, 'gregtech_issue_audit:test_empty')


EXPECTED = {
    'forge': {'openissueregressiontests.' + method for method in FORGE_METHODS} | {GAS_CASE} | FUNCTIONAL_CASES | FORGE_ONLY_CASES | set(LEGACY_CASE_NAMESPACES) | API_CONTRACT_CASES | set(ENERGY_STORAGE_CASE_NAMESPACES) | ORE_SOURCE_CASES | set(ORE_FOLLOWON_CASE_NAMESPACES),
    'neoforge': {GAS_CASE} | FUNCTIONAL_CASES | set(LEGACY_CASE_NAMESPACES) | API_CONTRACT_CASES | set(ENERGY_STORAGE_CASE_NAMESPACES) | ORE_SOURCE_CASES | set(ORE_FOLLOWON_CASE_NAMESPACES),
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
    # Every accepted predicate requires name == method or name.endswith(identity).
    # Either implies equal final dot-separated segments; retain the predicates below.
    candidates_by_tail = {}
    for identity in expected:
        candidates_by_tail.setdefault(identity.rsplit('.', 1)[-1], []).append(identity)
    found = set()
    for case in cases:
        name = case.get('name', '').lower()
        classname = case.get('classname', '').lower()
        for identity in candidates_by_tail.get(name.rsplit('.', 1)[-1], ()):
            test_class, method = identity.split('.', 1)
            if identity in ORE_SHREDDER_CASE_NAMESPACES:
                if name == method and classname == ORE_SHREDDER_CASE_NAMESPACES[identity]:
                    found.add(identity)
                continue
            if identity in QUALITY_PHYSICS_CASE_NAMESPACES:
                if name == method and classname == QUALITY_PHYSICS_CASE_NAMESPACES[identity]:
                    found.add(identity)
                continue
            if identity in NEXT_ORE_CENTRIFUGE_CASE_NAMESPACES:
                if name == method and classname == NEXT_ORE_CENTRIFUGE_CASE_NAMESPACES[identity]:
                    found.add(identity)
                continue
            if identity in POSITIVE_ORE_CENTRIFUGE_CASE_NAMESPACES:
                if name == method and classname == POSITIVE_ORE_CENTRIFUGE_CASE_NAMESPACES[identity]:
                    found.add(identity)
                continue
            if identity in MATERIAL_PHASE_CASE_NAMESPACES:
                if name == method and classname == MATERIAL_PHASE_CASE_NAMESPACES[identity]:
                    found.add(identity)
                continue
            if identity.startswith(('namedfluidtemperaturetests.', 'oreoverlaylifecyclecontrols.', 'waterbodybiomeboundarytests.', 'stonelayerslabaudittests.')) or identity.startswith('energyneighborloadednesstests.') or identity.startswith('spikeharveststatetests.') or identity.startswith('spikefallinglifecycletests.') or identity in ORE_SOURCE_CASES or identity in ORE_FOLLOWON_CASE_NAMESPACES or identity == 'barracksdooralignmenttests.generateddoorsjoinslabwallsandsurviveupdates':
                if name == method and classname == expected_namespace(identity):
                    found.add(identity)
                continue
            # Mojang's JUnit reporter uses the template resource as classname,
            # and PrefixGameTestTemplate(false) leaves a bare method name.
            if name == method and classname == expected_namespace(identity):
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
