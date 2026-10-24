"""Strict one-phase report/process/save check; does not run Minecraft or modify worlds."""
import argparse, hashlib, json, re
from pathlib import Path
from xml.etree import ElementTree as ET

def check(phase, report, log, exit_code, directory):
    if exit_code != 0: raise ValueError('JVM/Gradle command did not exit zero')
    root=ET.parse(report).getroot(); cases=list(root.iter('testcase'))
    if len(cases)!=1: raise ValueError(f'Expected exactly one testcase, got {len(cases)}')
    for node in root.iter():
        if node.tag in ('failure','error','skipped'): raise ValueError('Failed/errored/skipped testcase')
        if node.tag in ('testsuite','testsuites'):
            for key in ('failures','errors','skipped'):
                if float(node.get(key,'0'))!=0: raise ValueError('Nonzero '+key)
    case=cases[0]
    if case.get('name','').lower()!=phase+'disposablefixture' or case.get('classname','').lower()!=f'gregtech_restart_{phase}:test_empty':
        raise ValueError('Wrong phase testcase or namespace')
    text=log.read_text()
    for token in ('All 1 required tests passed', 'Stopping server', 'Saving worlds', 'All dimensions are saved', 'Game test server shutting down', 'BUILD SUCCESSFUL'):
        if token not in text: raise ValueError('Missing terminal evidence: '+token)
    matches=re.findall(r'GT6_RESTART_'+phase.upper()+r'(?: warm=false)? world=(\S+) pid=(\d+)(?: writerPid=(\d+))? nonce=(\S+)',text)
    if len(matches)!=1: raise ValueError('Expected exactly one fixture phase log')
    world,pid,writer_pid,nonce=matches[0]; world=Path(world).resolve(); directory=directory.resolve()
    if not world.is_relative_to(directory) or world==directory or not directory.name.startswith('gt6-restart-'):
        raise ValueError('Not the explicit disposable world')
    if phase=='read' and (pid==writer_pid or 'GT6_RESTART_READ warm=false ' not in text): raise ValueError('Missing distinct cold reader JVM')
    if Path('/proc',pid).exists(): raise ValueError('Recorded fixture JVM PID still exists; confirm exit/PID reuse manually')
    files=[world/'level.dat',world/'region/r.39062.39062.mca',world/'gt6-restart-phase1.properties']
    hashes={}
    for f in files:
        if not f.is_file() or not f.stat().st_size: raise ValueError('Missing/nonempty disk artifact: '+str(f))
        hashes[str(f)]={'bytes':f.stat().st_size,'sha256':hashlib.sha256(f.read_bytes()).hexdigest()}
    return {'phase':phase,'world':str(world),'pid':pid,'writer_pid':writer_pid,'nonce':nonce,'artifacts':hashes,'scope':'orderly development GameTest chunk/BE persistence; NOT ordinary-server metadata'}

def check_pair(writer, reader):
    """Compare two already validated phase receipts; never read or repair BE state."""
    if writer.get('phase') != 'write' or reader.get('phase') != 'read':
        raise ValueError('Expected writer then reader receipts')
    for key in ('world', 'nonce'):
        if not writer.get(key) or writer[key] != reader.get(key):
            raise ValueError('Pair differs in ' + key)
    if (not writer.get('pid') or not reader.get('pid')
            or str(writer['pid']) == str(reader['pid'])
            or str(reader.get('writer_pid')) != str(writer['pid'])):
        raise ValueError('Pair does not identify distinct matching JVMs')
    def marker(receipt):
        entries = [(path, value) for path, value in receipt.get('artifacts', {}).items()
                   if path.replace('\\', '/').endswith('/gt6-restart-phase1.properties')]
        if len(entries) != 1:
            raise ValueError('Missing unique nonempty marker evidence')
        path, value = entries[0]
        expected = Path(receipt['world']).resolve() / 'gt6-restart-phase1.properties'
        if Path(path).resolve() != expected:
            raise ValueError('Marker evidence is outside the recorded world')
        if not value.get('sha256') or value.get('bytes', 0) <= 0:
            raise ValueError('Missing unique nonempty marker evidence')
        return value['sha256'], value['bytes']
    if marker(writer) != marker(reader):
        raise ValueError('Marker changed between phases')
    return {'world': writer['world'], 'nonce': writer['nonce'],
            'writer_pid': writer['pid'], 'reader_pid': reader['pid'],
            'marker_sha256': marker(writer)[0],
            'scope': 'orderly development GameTest chunk/BE persistence; NOT ordinary-server metadata'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='phase', required=True)
    for phase in ('write', 'read'):
        command = commands.add_parser(phase)
        command.add_argument('report', type=Path)
        command.add_argument('log', type=Path)
        command.add_argument('exit_code', type=int)
        command.add_argument('directory', type=Path)
    pair = commands.add_parser('pair')
    pair.add_argument('writer_receipt', type=Path)
    pair.add_argument('reader_receipt', type=Path)
    args = parser.parse_args()
    if args.phase == 'pair':
        result = check_pair(json.loads(args.writer_receipt.read_text()),
                            json.loads(args.reader_receipt.read_text()))
    else:
        result = check(args.phase, args.report, args.log, args.exit_code, args.directory)
    print(json.dumps(result, indent=2))
