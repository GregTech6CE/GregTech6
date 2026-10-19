import com.gregtech.gregtech.api.fluid.BarrelFermentationRules;

/** Core arithmetic observations only; this does not run barrel ticking or saved-world restoration. */
public final class BarrelFermentationAudit {
    public static void main(String[] args) {
        System.out.println("DURATION|full|" + BarrelFermentationRules.duration(32, 20, 1000, 1000));
        System.out.println("DURATION|scaled|" + BarrelFermentationRules.duration(32, 20, 1250, 1000));
        System.out.println("DURATION|small|" + BarrelFermentationRules.duration(32, 20, 1, 1000));
        System.out.println("DURATION|negative_power|" + BarrelFermentationRules.duration(-32, 20, 1000, 1000));
        System.out.println("OUTPUT|same_ratio|" + BarrelFermentationRules.output(1000, 16000, 1000));
        System.out.println("OUTPUT|expansion|" + BarrelFermentationRules.output(2000, 16000, 1000));
        System.out.println("OUTPUT|below_unit|" + BarrelFermentationRules.output(1, 1, 1000));
    }
}
