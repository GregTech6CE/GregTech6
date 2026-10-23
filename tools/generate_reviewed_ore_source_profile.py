#!/usr/bin/env python3
"""Generate tri-state recipe policy from reviewed identity evidence, never from absent flags.
Default inputs/outputs are production locations after promotion; --output permits scratch staging.
"""
import argparse,hashlib,json
from pathlib import Path

def render(profile):
 rows=profile['rows'];seen=set()
 for row in rows:
  key=(row['id'],row['canonical_name'],row['rule'])
  if key in seen:raise ValueError('Duplicate reviewed identity-rule '+str(key))
  seen.add(key)
  if row['decision'] not in ('YES','NO') or row['rule'] not in ('FURNACE','GRADED_GEM'):
   raise ValueError('Unsupported reviewed decision/rule')
  if row['id']<=0 or not row['evidence']:raise ValueError('Identity and evidence required')
  if row.get('proof_status','').startswith('candidate'):raise ValueError('Unreviewed candidate cannot be promoted')
 entries='\n'.join(f'        new Entry({r["id"]}, {json.dumps(r["canonical_name"])}, Rule.{r["rule"]}, Decision.{r["decision"]}),' for r in sorted(rows,key=lambda r:(r['rule'],r['id'])))
 return '''package com.gregtech.gregtech.content.recipe;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.addon.AddonMaterials;
/** Generated from reviewed-source-policy.json. Incomplete by design; absent is UNKNOWN. */
public final class ReviewedOreSourceProfile {
    public enum Decision { YES, NO, UNKNOWN;
        public boolean orLegacy(boolean legacy) { return this == UNKNOWN ? legacy : this == YES; }
    }
    public enum Rule { FURNACE, GRADED_GEM }
    private record Entry(int id,String name,Rule rule,Decision decision) {}
    private static final Entry[] ENTRIES = {
'''+entries+'''
    };
    private ReviewedOreSourceProfile() {}
    public static Decision decision(GTMaterial material,Rule rule) {
        if(material==null||!material.isValid()) return Decision.UNKNOWN;
        material=material.resolve();
        if(AddonMaterials.owns(material)||!GTMaterialRegistry.isCoreMaterial(material)) return Decision.UNKNOWN;
        for(Entry row:ENTRIES) if(row.id==material.getId()&&row.name.equals(material.getName())&&row.rule==rule)
            return row.decision;
        return Decision.UNKNOWN;
    }
}
'''

def main():
 p=argparse.ArgumentParser();p.add_argument('--profile',type=Path,default=Path(__file__).with_name('data')/'reviewed-source-policy.json')
 p.add_argument('--output',type=Path,default=Path('core/src/main/java/com/gregtech/gregtech/content/recipe/ReviewedOreSourceProfile.java'))
 p.add_argument('--verify-source-root',type=Path);p.add_argument('--check',action='store_true');a=p.parse_args()
 profile=json.loads(a.profile.read_text())
 if a.verify_source_root:
  for source in profile['sources']:
   path=a.verify_source_root/source['path']
   if hashlib.sha256(path.read_bytes()).hexdigest()!=source['sha256']:raise ValueError('Source snapshot changed: '+str(path))
 output=render(profile)
 if a.check:
  if not a.output.exists() or a.output.read_text()!=output:raise SystemExit('Generated profile differs: '+str(a.output))
 else:
  if a.output.resolve()==a.profile.resolve():raise ValueError('Cannot overwrite evidence profile')
  a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(output)
if __name__=='__main__':main()
