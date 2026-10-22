"""Require actual passing GameTest results for the focused open-issue audit."""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

EXPECTED = {
    'extrudershapesdonotsplitthemachinecreativefamily',
    'dungeonwaterplacementschedulesflowonlyaftersuccessfulwrite',
    'gtorehostsadmitsmalloreandrejectbedrockwithoutconsumption',
    'pilecovereventpreservessneakaddandemptysnapshotclearslegacycover',
}


def check(path):
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
        for method in EXPECTED:
            if name == method and classname.endswith('openissueregressiontests'):
                found.add(method)
            elif name.endswith('openissueregressiontests.' + method):
                found.add(method)
    missing = EXPECTED - found
    if missing:
        raise ValueError('Selected tests did not execute: ' + ', '.join(sorted(missing)))
    print(f'All {len(EXPECTED)} selected issue regressions executed and passed ({len(cases)} cases reported)')


if __name__ == '__main__':
    if len(sys.argv) != 2:
        raise SystemExit('Usage: check_issue_gametest_report.py REPORT.xml')
    try:
        check(Path(sys.argv[1]))
    except (OSError, ET.ParseError, ValueError) as error:
        raise SystemExit(str(error)) from error
