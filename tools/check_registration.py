"""Compile and run isolated registration contracts (Java 17 and Python 3 required).

The ModList fixture models only mod presence; this is not a Forge launch test.
Usage: python tools/check_registration.py [--offline]
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

from pathlib import Path
import os
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    with tempfile.TemporaryDirectory(prefix='gt6-registration-') as directory:
        temp = Path(directory)
        classpath_file = temp / 'classpath.txt'
        init = temp / 'check.gradle'
        init.write_text("allprojects { tasks.register('registrationContractClasspath') { doLast { "
                        "new File('" + classpath_file.as_posix().replace("'", "\\'") + "').text = "
                        "sourceSets.main.output.classesDirs.asPath + File.pathSeparator + "
                        "sourceSets.main.compileClasspath.asPath } } }", encoding='utf-8')
        wrapper = ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew')
        subprocess.run([str(wrapper), 'compileJava', 'registrationContractClasspath',
                        '--console=plain', '-I', str(init), *sys.argv[1:]], cwd=ROOT, check=True)
        cp = classpath_file.read_text(encoding='utf-8')
        anvil_test = native_path(ROOT / 'src/test/java/com/gregtech/gregtech/client/AnvilRenderContracts.java')
        fixture = temp / 'ModList.java'
        fixture.write_text('''package net.minecraftforge.fml;
public final class ModList {
    private static final ModList INSTANCE = new ModList();
    public static ModList get() { return INSTANCE; }
    public boolean isLoaded(String name) {
        return java.util.Set.of("minecraft", "forge", "gt6ce").contains(name);
    }
}
''', encoding='utf-8')
        test = native_path(ROOT / 'src/test/java/com/gregtech/gregtech/registration/RegistrationContracts.java')
        client_test = test.with_name('ClientModelContracts.java')
        tool_test = test.with_name('ToolInteractionContracts.java')
        piston_test = test.with_name('PistonEngineContracts.java')
        container_test = test.with_name('ContainerSensorContracts.java')
        rebuild_test = test.with_name('RebuildStateContracts.java')
        subprocess.run(['javac', '-proc:none', '-cp', cp, '-d', str(temp), str(fixture), str(test), str(client_test), str(tool_test), str(piston_test), str(container_test), str(rebuild_test), str(anvil_test)], check=True)
        subprocess.run(['java', '-cp', str(temp) + os.pathsep + cp, 'com.gregtech.gregtech.client.AnvilRenderContracts'], check=True)
        for args in ([], ['references-first'], ['catalog', str(ROOT / 'build/registration-material-catalog.json')], ['material-memory']):
            subprocess.run(['java', '-cp', str(temp) + os.pathsep + cp,
                            'com.gregtech.gregtech.registration.RegistrationContracts', *args], check=True)
        subprocess.run(['java', '-cp', str(temp) + os.pathsep + cp,
                        'com.gregtech.gregtech.registration.ClientModelContracts'], check=True)
        subprocess.run(['java', '-cp', str(temp) + os.pathsep + cp,
                        'com.gregtech.gregtech.registration.ToolInteractionContracts'], check=True)
        subprocess.run(['java', '-cp', str(temp) + os.pathsep + cp,
                        'com.gregtech.gregtech.registration.PistonEngineContracts'], check=True)
        subprocess.run(['java', '-cp', str(temp) + os.pathsep + cp,
                        'com.gregtech.gregtech.registration.ContainerSensorContracts'], check=True)
        subprocess.run(['java', '-cp', str(temp) + os.pathsep + cp,
                        'com.gregtech.gregtech.registration.RebuildStateContracts'], check=True)


if __name__ == '__main__':
    main()
